// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.idea.maven.project

import com.intellij.openapi.application.readAction
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.util.progress.RawProgressReporter
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.idea.maven.model.MavenConstants
import org.jetbrains.idea.maven.model.MavenExplicitProfiles
import org.jetbrains.idea.maven.project.MavenProjectChangesBuilder.Companion.merged
import org.jetbrains.idea.maven.utils.MavenLog
import org.jetbrains.idea.maven.utils.MavenUtil
import org.jetbrains.idea.maven.utils.ParallelRunner
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@ApiStatus.Internal
internal class MavenProjectsTreeUpdater(private val tree: MavenProjectsTree,
                                        private val explicitProfiles: MavenExplicitProfiles,
                                        private val updateContext: MavenProjectsTreeUpdateContext,
                                        private val reader: MavenProjectReader,
                                        private val generalSettings: MavenGeneralSettings,
                                        private val process: RawProgressReporter?,
                                        private val updateModules: Boolean) {
  private val updated = ConcurrentHashMap<VirtualFile, Boolean>()
  private val userSettingsFile = generalSettings.effectiveUserSettingsIoFile
  private val globalSettingsFile = generalSettings.effectiveGlobalSettingsIoFile

  private fun startUpdate(mavenProjectFile: VirtualFile, forceRead: Boolean): Boolean {
    val projectPath = mavenProjectFile.path

    if (tree.isIgnored(projectPath)) return false

    val previousUpdateRef = Ref<Boolean>()
    updated.compute(mavenProjectFile) { file: VirtualFile?, value: Boolean? ->
      previousUpdateRef.set(value)
      java.lang.Boolean.TRUE == value || forceRead
    }
    val previousUpdate = previousUpdateRef.get()

    if ((null != previousUpdate && !forceRead) || (java.lang.Boolean.TRUE == previousUpdate)) {
      // we already updated this file
      MavenLog.LOG.trace("Has already been updated ($previousUpdate): $mavenProjectFile; forceRead: $forceRead")
      return false
    }
    process?.text(MavenProjectBundle.message("maven.reading.pom", projectPath))
    return true
  }

  private suspend fun readPomIfNeeded(mavenProject: MavenProject, forceRead: Boolean): Boolean {
    val timestamp = calculateTimestamp(mavenProject)
    val timeStampChanged = timestamp != tree.getTimeStamp(mavenProject)
    val readPom = forceRead || timeStampChanged

    if (readPom) {
      val oldProjectId = if (mavenProject.isNew) null else mavenProject.mavenId
      val oldParentId = mavenProject.parentId
      val readerResult = reader.readProjectAsync(generalSettings, mavenProject.file, explicitProfiles, tree.projectLocator)
      val readChanges = mavenProject.set(readerResult, generalSettings, true, false, true)

      tree.putVirtualFileToProjectMapping(mavenProject, oldProjectId)

      if (Comparing.equal(oldParentId, mavenProject.parentId)) {
        tree.putTimestamp(mavenProject, timestamp)
      }
      else {
        // ensure timestamp reflects actual parent's timestamp
        val newTimestamp = calculateTimestamp(mavenProject)
        tree.putTimestamp(mavenProject, newTimestamp)
      }

      val forcedChanges = if (forceRead) MavenProjectChanges.ALL else MavenProjectChanges.NONE
      val changes = merged(forcedChanges, readChanges)
      updateContext.updated(mavenProject, changes)
    }

    return readPom
  }

  private suspend fun calculateTimestamp(mavenProject: MavenProject): MavenProjectTimestamp {
    return readAction {
      val pomTimestamp = getFileTimestamp(mavenProject.file)
      val parent = tree.findParent(mavenProject)
      val parentLastReadStamp = parent?.lastReadStamp ?: -1
      val profilesXmlFile = mavenProject.profilesXmlFile
      val profilesTimestamp = getFileTimestamp(profilesXmlFile)
      val jvmConfigFile = MavenUtil.getConfigFile(mavenProject, MavenConstants.JVM_CONFIG_RELATIVE_PATH)
      val jvmConfigTimestamp = getFileTimestamp(jvmConfigFile)
      val mavenConfigFile = MavenUtil.getConfigFile(mavenProject, MavenConstants.MAVEN_CONFIG_RELATIVE_PATH)
      val mavenConfigTimestamp = getFileTimestamp(mavenConfigFile)
      val userSettingsTimestamp = getFileTimestamp(userSettingsFile)
      val globalSettingsTimestamp = getFileTimestamp(globalSettingsFile)

      val profilesHashCode = explicitProfiles.hashCode()
      MavenProjectTimestamp(pomTimestamp,
                            parentLastReadStamp,
                            profilesTimestamp,
                            userSettingsTimestamp,
                            globalSettingsTimestamp,
                            profilesHashCode.toLong(),
                            jvmConfigTimestamp,
                            mavenConfigTimestamp)
    }
  }

  private fun handleRemovedModules(mavenProject: MavenProject, prevModules: List<MavenProject>, existingModuleFiles: List<VirtualFile>) {
    val removedModules = prevModules.filter { !existingModuleFiles.contains(it.file) }
    for (module in removedModules) {
      val moduleFile = module.file
      if (tree.isManagedFile(moduleFile)) {
        if (tree.reconnectRoot(module)) {
          updateContext.updated(module, MavenProjectChanges.NONE)
        }
      }
      else {
        tree.removeModule(mavenProject, module)
        tree.doDelete(mavenProject, module, updateContext)
      }
    }
  }

  private fun reconnectModuleFiles(mavenProject: MavenProject, modulesFilesToReconnect: List<VirtualFile>) {
    for (file in modulesFilesToReconnect) {
      val module = tree.findProject(file)
      if (null != module) {
        if (tree.reconnect(mavenProject, module)) {
          updateContext.updated(module, MavenProjectChanges.NONE)
        }
      }
    }
  }

  private fun collectModuleFilesToReconnect(mavenProject: MavenProject, existingModuleFiles: List<VirtualFile>): List<VirtualFile> {
    val modulesFilesToReconnect = ArrayList<VirtualFile>()
    for (moduleFile in existingModuleFiles) {
      val foundModule = tree.findProject(moduleFile)
      val isNewModule = foundModule == null
      if (!isNewModule) {
        val currentAggregator = tree.findAggregator(foundModule!!)
        if (currentAggregator != null && currentAggregator !== mavenProject) {
          MavenLog.LOG.info("Module $moduleFile is already included into ${mavenProject.file}")
          continue
        }
      }

      modulesFilesToReconnect.add(moduleFile)
    }
    return modulesFilesToReconnect
  }

  private fun collectModuleFilesToUpdate(moduleFilesToReconnect: List<VirtualFile>, updateExistingModules: Boolean): List<VirtualFile> {
    if (updateExistingModules) {
      return moduleFilesToReconnect
    }

    // update only new modules
    return moduleFilesToReconnect.filter { null == tree.findProject(it) }
  }

  private fun collectChildFilesToUpdate(mavenProject: MavenProject, prevChildren: Collection<MavenProject>): List<VirtualFile> {
    val children = HashSet(prevChildren)
    children.removeAll(updateContext.deletedProjects.toSet())
    children.addAll(tree.findInheritors(mavenProject))
    return children.map { it.file }
  }

  private fun findOrCreateProject(f: VirtualFile): MavenProject {
    val mavenProject = tree.findProject(f)
    return mavenProject ?: MavenProject(f)
  }

  private suspend fun update(mavenProjectFile: VirtualFile, forceRead: Boolean) {
    // if the file has already been updated, skip subsequent updates
    if (!startUpdate(mavenProjectFile, forceRead)) return

    val mavenProject = findOrCreateProject(mavenProjectFile)

    // we will compare modules and children before and after reading the pom.xml file
    val prevModules = tree.getModules(mavenProject)
    val prevChildren = tree.findInheritors(mavenProject)

    // read pom.xml if something has changed since the last reading or reading is forced
    val readPom = readPomIfNeeded(mavenProject, forceRead)

    val existingModuleFiles = mavenProject.existingModuleFiles

    // some modules might have been removed
    handleRemovedModules(mavenProject, prevModules, existingModuleFiles)

    // collect new and existing modules to reconnect to the tree
    val modulesFilesToReconnect = collectModuleFilesToReconnect(mavenProject, existingModuleFiles)
    val updateExistingModules = readPom || updateModules

    // collect modules to update recursively
    val modulesFilesToUpdate = collectModuleFilesToUpdate(modulesFilesToReconnect, updateExistingModules)

    // do not force update modules if only this project was requested to be updated
    val forceReadModules = updateModules && forceRead
    val moduleUpdates = modulesFilesToUpdate.map {
      UpdateSpec(
        it,
        forceReadModules
      )
    }
    updateProjects(moduleUpdates)
    reconnectModuleFiles(mavenProject, modulesFilesToReconnect)

    // collect children (inheritors) to update recursively
    val childFilesToUpdate = collectChildFilesToUpdate(mavenProject, prevChildren)
    val childUpdates = childFilesToUpdate.map {
      UpdateSpec(
        it,
        readPom // if parent was read, force read children
      )
    }
    updateProjects(childUpdates)
  }

  suspend fun updateProjects(specs: List<UpdateSpec>) {
    if (specs.isEmpty()) return

    ParallelRunner.getInstance(tree.project).runInParallel(specs) {
      update(it.mavenProjectFile, it.forceRead)
    }
  }

  private fun getFileTimestamp(file: VirtualFile?): Long {
    if (file == null || !file.isValid) return -1
    return file.timeStamp
  }

  private fun getFileTimestamp(file: File?): Long {
    return getFileTimestamp(if (file == null) null else LocalFileSystem.getInstance().findFileByIoFile(file))
  }

  @ApiStatus.Internal
  @JvmRecord
  internal data class UpdateSpec(val mavenProjectFile: VirtualFile, val forceRead: Boolean)
}
