// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.lightEdit

import com.apple.eawt.event.FullScreenEvent
import com.intellij.diagnostic.IdeMessagePanel
import com.intellij.ide.lightEdit.menuBar.getLightEditMainMenuActionGroup
import com.intellij.ide.lightEdit.project.LightEditFileEditorManagerImpl
import com.intellij.ide.lightEdit.statusBar.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.impl.ProjectManagerImpl
import com.intellij.openapi.project.impl.applyBoundsOrDefault
import com.intellij.openapi.project.impl.createNewProjectFrameProducer
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.wm.*
import com.intellij.openapi.wm.impl.*
import com.intellij.openapi.wm.impl.FrameInfoHelper.Companion.isFullScreenSupportedInCurrentOs
import com.intellij.openapi.wm.impl.ProjectFrameBounds.Companion.getInstance
import com.intellij.openapi.wm.impl.status.IdeStatusBarImpl
import com.intellij.openapi.wm.impl.status.adaptV2Widget
import com.intellij.platform.ide.menu.installAppMenuIfNeeded
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.toolWindow.ToolWindowPane
import com.intellij.ui.mac.MacFullScreenControlsManager
import com.intellij.ui.mac.MacMainFrameDecorator
import com.intellij.ui.mac.MacMainFrameDecorator.FSAdapter
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Component
import java.awt.Dimension
import javax.swing.JFrame

@RequiresEdt
internal fun allocateLightEditFrame(project: Project, frameInfo: FrameInfo?): LightEditFrameWrapper {
  return runWithModalProgressBlocking(project, "") {
    withContext(Dispatchers.EDT) {
      val wrapper = allocateLightEditFrame(project) { frame ->
        LightEditFrameWrapper(project = project, frame = frame ?: createNewProjectFrameProducer(frameInfo).create())
      } as LightEditFrameWrapper
      (project.serviceAsync<FileEditorManager>() as LightEditFileEditorManagerImpl).internalInit()
      wrapper
    }
  }
}

internal class LightEditFrameWrapper(
  private val project: Project,
  frame: IdeFrameImpl
) : ProjectFrameHelper(frame = frame), Disposable, LightEditFrame {
  private var editPanel: LightEditPanel? = null
  private var frameTitleUpdateEnabled = true

  override fun getProject(): Project = project

  val lightEditPanel: LightEditPanel
    get() = editPanel!!

  override fun createIdeRootPane(loadingState: FrameLoadingState?): IdeRootPane = LightEditRootPane(frame = frame)

  override suspend fun installDefaultProjectStatusBarWidgets(project: Project) {
    val editorManager = LightEditService.getInstance().editorManager
    val statusBar = statusBar!!

    @Suppress("DEPRECATION")
    val coroutineScope = project.coroutineScope
    statusBar.addWidgetToLeft(LightEditModeNotificationWidget())

    val dataContext = object : WidgetPresentationDataContext {
      override val project: Project
        get() = project

      override val currentFileEditor: StateFlow<FileEditor?> by lazy {
        val flow = MutableStateFlow<FileEditor?>(null)
        editorManager.addListener(object : LightEditorListener {
          override fun afterSelect(editorInfo: LightEditorInfo?) {
            flow.value = editorInfo?.fileEditor
          }
        })
        flow
      }
    }

    statusBar.init(
      project = project,
      frame = frame,
      extraItems = listOf(
        LightEditAutosaveWidget(editorManager) to LoadingOrder.before(IdeMessagePanel.FATAL_ERROR),
        LightEditEncodingWidgetWrapper(project, coroutineScope) to LoadingOrder.after(StatusBar.StandardWidgets.POSITION_PANEL),
        LightEditLineSeparatorWidgetWrapper(project, coroutineScope) to LoadingOrder.before(LightEditEncodingWidgetWrapper.WIDGET_ID),
        adaptV2Widget(StatusBar.StandardWidgets.POSITION_PANEL, dataContext) { scope ->
          LightEditPositionWidget(dataContext = dataContext, scope = scope, editorManager = editorManager)
        } to LoadingOrder.before(IdeMessagePanel.FATAL_ERROR),
      ),
    )
  }

  override fun getTitleInfoProviders(): List<TitleInfoProvider> = emptyList()

  override fun dispose() {
    val frameInfo = getInstance(project).getActualFrameInfoInDeviceSpace(
      frameHelper = this,
      frame = frame,
      windowManager = (WindowManager.getInstance() as WindowManagerImpl)
    )
    val lightEditServiceImpl = LightEditService.getInstance() as LightEditServiceImpl
    lightEditServiceImpl.setFrameInfo(frameInfo)
    lightEditServiceImpl.frameDisposed()
    Disposer.dispose(editPanel!!)
    super.dispose()
  }

  private inner class LightEditRootPane(frame: IdeFrameImpl)
    : IdeRootPane(frame = frame,
                  loadingState = null,
                  mainMenuActionGroup = getLightEditMainMenuActionGroup()), LightEditCompatible {
    override val isLightEdit: Boolean
      get() = true

    override fun createCenterComponent(frame: JFrame): Component {
      val panel = LightEditPanel(LightEditUtil.requireProject())
      editPanel = panel
      return panel
    }

    override fun getToolWindowPane(): ToolWindowPane {
      throw IllegalStateException("Tool windows are unavailable in LightEdit")
    }

    @Suppress("DEPRECATION")
    override fun createStatusBar(frameHelper: ProjectFrameHelper): IdeStatusBarImpl {
      return object : IdeStatusBarImpl(frameHelper = frameHelper, addToolWindowWidget = false, coroutineScope = project.coroutineScope) {
        override fun updateUI() {
          setUI(LightEditStatusBarUI())
        }

        override fun getPreferredSize(): Dimension = LightEditStatusBarUI.withHeight(super.getPreferredSize())
      }
    }
  }

  fun setFrameTitleUpdateEnabled(frameTitleUpdateEnabled: Boolean) {
    this.frameTitleUpdateEnabled = frameTitleUpdateEnabled
  }

  override fun setFrameTitle(text: String) {
    if (frameTitleUpdateEnabled) {
      super.setFrameTitle(text)
    }
  }
}

/**
 * This method is not used in a normal conditions. Only for light edit.
 */
private suspend fun allocateLightEditFrame(project: Project,
                                           projectFrameHelperFactory: (IdeFrameImpl?) -> ProjectFrameHelper): ProjectFrameHelper {
  val windowManager = WindowManager.getInstance() as WindowManagerImpl
  windowManager.getFrameHelper(project)?.let {
    return it
  }

  windowManager.removeAndGetRootFrame()?.let { frame ->
    val frameHelper = projectFrameHelperFactory(frame)
    windowManager.lightFrameAssign(project, frameHelper)
    return frameHelper
  }

  val frame = projectFrameHelperFactory(null)
  frame.init()
  var frameInfo: FrameInfo? = null
  val lastFocusedProjectFrame = IdeFocusManager.getGlobalInstance().lastFocusedFrame?.project?.let(windowManager::getFrameHelper)
  if (lastFocusedProjectFrame != null) {
    frameInfo = getFrameInfoByFrameHelper(lastFocusedProjectFrame)
    if (frameInfo.bounds == null) {
      frameInfo = windowManager.defaultFrameInfoHelper.info
    }
  }

  if (frameInfo?.bounds != null) {
    // update default frame info - newly opened project frame should be the same as last opened
    if (frameInfo !== windowManager.defaultFrameInfoHelper.info) {
      windowManager.defaultFrameInfoHelper.copyFrom(frameInfo)
    }
    frameInfo.bounds?.let {
      applyBoundsOrDefault(frame.frame, FrameBoundsConverter.convertFromDeviceSpaceAndFitToScreen(it)?.first)
    }
  }

  if (SystemInfoRt.isMac) {
    val decorator = frame.getDecorator()
    if (decorator is MacMainFrameDecorator) {
      decorator.dispatcher.addListener(object : FSAdapter() {
        override fun windowEnteringFullScreen(e: FullScreenEvent?) {
          MacFullScreenControlsManager.configureForLightEdit(true)
        }

        override fun windowExitedFullScreen(e: FullScreenEvent?) {
          MacFullScreenControlsManager.configureForLightEdit(false)
        }
      })
    }
  }

  windowManager.lightFrameAssign(project, frame)
  val uiFrame = frame.frame
  if (frameInfo != null) {
    uiFrame.extendedState = frameInfo.extendedState
  }
  uiFrame.isVisible = true
  if (isFullScreenSupportedInCurrentOs() && frameInfo != null && frameInfo.fullScreen) {
    frame.toggleFullScreen(true)
  }
  uiFrame.addComponentListener(FrameStateListener(windowManager.defaultFrameInfoHelper))
  installAppMenuIfNeeded(uiFrame)

  @Suppress("DEPRECATION")
  project.coroutineScope.launch {
    ProjectManagerImpl.dispatchEarlyNotifications()
  }
  return frame
}