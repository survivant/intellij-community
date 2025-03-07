// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.history.integration.ui.models

import com.intellij.history.core.LocalHistoryFacade
import com.intellij.history.core.RevisionsCollector
import com.intellij.history.core.changes.ChangeSet
import com.intellij.history.core.changes.ChangeVisitor
import com.intellij.history.core.changes.StructuralChange
import com.intellij.history.core.revisions.CurrentRevision
import com.intellij.history.core.revisions.Revision
import com.intellij.history.integration.IdeaGateway
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PairProcessor
import java.util.*

data class RevisionData(val currentRevision: Revision, val revisions: List<RevisionItem>)

val RevisionData.allRevisions get() = listOf(currentRevision) + revisions.map { it.revision }

internal fun collectRevisionData(project: Project,
                                 gateway: IdeaGateway,
                                 facade: LocalHistoryFacade,
                                 file: VirtualFile,
                                 filter: String? = null,
                                 before: Boolean = true): RevisionData {
  return runReadAction {
    gateway.registerUnsavedDocuments(facade)

    val root = gateway.createTransientRootEntry()
    val path = gateway.getPathOrUrl(file)

    val revisionItems = mergeLabelsWithRevisions(RevisionsCollector.collect(facade, root, path, project.getLocationHash(), filter, before))
    RevisionData(CurrentRevision(root, path), revisionItems)
  }
}

private fun mergeLabelsWithRevisions(revisions: List<Revision>): List<RevisionItem> {
  val result = mutableListOf<RevisionItem>()

  for (revision in revisions.asReversed()) {
    if (revision.isLabel && !result.isEmpty()) {
      result.last().labels.addFirst(revision)
    }
    else {
      result.add(RevisionItem(revision))
    }
  }

  return result.asReversed()
}

fun LocalHistoryFacade.filterContents(gateway: IdeaGateway, file: VirtualFile, revisions: List<Revision>, filter: String,
                                      before: Boolean): Set<Long> {
  val result = mutableSetOf<Long>()
  processContents(gateway, file, revisions, before) { revision, content ->
    if (Thread.currentThread().isInterrupted) return@processContents false
    if (content?.contains(filter, true) == true) {
      val id = revision.changeSetId
      if (id != null) result.add(id)
    }
    true
  }
  return result
}

internal fun LocalHistoryFacade.processContents(gateway: IdeaGateway, file: VirtualFile, revisions: List<Revision>, before: Boolean,
                                                processor: PairProcessor<in Revision, in String?>) {
  val revisionMap = revisions.filter { !it.isLabel }.associateBy { it.changeSetId }
  if (revisionMap.isEmpty()) return

  val root = revisionMap.values.first().root.copy()
  var path: String? = gateway.getPathOrUrl(file)

  accept(object : ChangeVisitor() {
    init {
      processContent(revisionMap[null])
    }

    private fun processContent(revision: Revision?): Boolean {
      if (revision == null) return true
      val entry = root.findEntry(path)
      val content = entry?.content
      val text = content?.getString(entry, gateway)
      return processor.process(revision, text)
    }

    override fun begin(c: ChangeSet) {
      ProgressManager.checkCanceled()
      if (Thread.currentThread().isInterrupted) {
        throw ProcessCanceledException()
      }
      if (!before && !processContent(revisionMap[c.id])) stop()
    }

    @Throws(StopVisitingException::class)
    override fun end(c: ChangeSet) {
      if (before && !processContent(revisionMap[c.id])) stop()
    }

    override fun visit(c: StructuralChange) {
      if (c.affectsPath(path)) {
        c.revertOn(root, false)
        path = c.revertPath(path)
      }
    }
  })
}