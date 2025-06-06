package com.wyldsoft.notes.editorview.drawing.onyx

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.view.SurfaceView
import androidx.lifecycle.lifecycleScope
import com.onyx.android.sdk.pen.TouchHelper
import com.wyldsoft.notes.editorview.editor.EditorState
import com.wyldsoft.notes.GlobalDeviceReceiver
import com.wyldsoft.notes.TouchUtils
import com.wyldsoft.notes.base.BaseDeviceReceiver
import com.wyldsoft.notes.editorview.drawing.base.BaseDrawingActivity
import com.wyldsoft.notes.editorview.drawing.base.BaseTouchHelper
import com.wyldsoft.notes.backend.database.entities.Note
import kotlinx.coroutines.launch

/**
 * Refactored Onyx-specific implementation of BaseDrawingActivity
 * This class now focuses only on core drawing activity functionality and coordination
 * Delegates specific responsibilities to specialized manager classes
 */
open class OnyxDrawingActivity : BaseDrawingActivity() {
    companion object {
        private const val TAG = "OnyxDrawingActivity"
    }

    // Onyx-specific components
    private var onyxTouchHelper: TouchHelper? = null
    private var onyxDeviceReceiver: GlobalDeviceReceiver? = null

    // Specialized managers for different responsibilities
    private lateinit var shapeManager: OnyxShapeManager
    private lateinit var databaseManager: OnyxDatabaseManager
    private lateinit var eraserManager: OnyxEraserManager
    private lateinit var renderingManager: OnyxRenderingManager
    private lateinit var navigationHandler: OnyxNavigationHandler

    override fun initializeSDK() {
        // Initialize all manager components
        initializeManagers()

        // Set up eraser mode listener
        setupEraserModeListener()
    }

    private fun initializeManagers() {
        renderingManager = OnyxRenderingManager()
        shapeManager = OnyxShapeManager(renderingManager)
        databaseManager = OnyxDatabaseManager(this)
        eraserManager = OnyxEraserManager(shapeManager, renderingManager)
        navigationHandler = OnyxNavigationHandler(databaseManager)
    }

    private fun setupEraserModeListener() {
        lifecycleScope.launch {
            EditorState.eraserModeChanged.collect { enabled ->
                eraserManager.setEraserMode(enabled)
                updateTouchHelperForEraserMode()
                Log.d(TAG, "Eraser mode changed to: $enabled")
            }
        }
    }

    override fun createTouchHelper(surfaceView: SurfaceView): BaseTouchHelper {
        val callback = OnyxTouchCallbackFactory.create(
            shapeManager = shapeManager,
            eraserManager = eraserManager,
            databaseManager = databaseManager,
            renderingManager = renderingManager,
            navigationHandler = navigationHandler,
            activity = this // Pass activity for access to pen profile and surface view
        )

        onyxTouchHelper = TouchHelper.create(surfaceView, callback)
        return OnyxTouchHelperWrapper(onyxTouchHelper!!)
    }

    override fun createDeviceReceiver(): BaseDeviceReceiver {
        onyxDeviceReceiver = GlobalDeviceReceiver()
        return OnyxDeviceReceiverWrapper(onyxDeviceReceiver!!)
    }

    override fun enableFingerTouch() {
        TouchUtils.enableFingerTouch(applicationContext)
    }

    override fun disableFingerTouch() {
        TouchUtils.disableFingerTouch(applicationContext)
    }

    override fun cleanSurfaceView(surfaceView: SurfaceView): Boolean {
        return renderingManager.cleanSurfaceView(surfaceView)
    }

    override fun renderToScreen(surfaceView: SurfaceView, bitmap: Bitmap?) {
        renderingManager.renderToScreen(surfaceView, bitmap)
    }

    override fun onResumeDrawing() {
        onyxTouchHelper?.setRawDrawingEnabled(true)
        enableFingerTouch()
    }

    override fun onPauseDrawing() {
        onyxTouchHelper?.setRawDrawingEnabled(false)
        databaseManager.saveAllShapesToDatabase(shapeManager.getAllShapes(), currentPenProfile)
        enableFingerTouch()
    }

    override fun onCleanupSDK() {
        onyxTouchHelper?.closeRawDrawing()
        databaseManager.saveAllShapesToDatabase(shapeManager.getAllShapes(), currentPenProfile)
        shapeManager.clearShapes()
    }

    override fun updateActiveSurface() {
        updateTouchHelperWithProfile()
    }

    override fun updateTouchHelperWithProfile() {
        onyxTouchHelper?.let { helper ->
            helper.setRawDrawingEnabled(false)
            helper.closeRawDrawing()

            val limit = Rect()
            surfaceView?.getLocalVisibleRect(limit)
            val excludeRects = EditorState.getCurrentExclusionRects()

            helper.setStrokeWidth(currentPenProfile.strokeWidth)
                .setStrokeColor(currentPenProfile.getColorAsInt())
                .setLimitRect(limit, ArrayList(excludeRects))
                .openRawDrawing()

            helper.setStrokeStyle(currentPenProfile.getOnyxStrokeStyle())
            helper.setRawDrawingEnabled(true)
            helper.setRawDrawingRenderEnabled(!eraserManager.isEraserModeEnabled())
        }
    }

    private fun updateTouchHelperForEraserMode() {
        onyxTouchHelper?.let { helper ->
            if (eraserManager.isEraserModeEnabled()) {
                helper.setRawDrawingEnabled(true)
                helper.setRawDrawingRenderEnabled(false)
            } else {
                helper.setRawDrawingRenderEnabled(true)
            }
        }
    }

    override fun updateTouchHelperExclusionZones(excludeRects: List<Rect>) {
        onyxTouchHelper?.let { helper ->
            helper.setRawDrawingEnabled(false)
            helper.closeRawDrawing()

            val limit = Rect()
            surfaceView?.getLocalVisibleRect(limit)

            helper.setStrokeWidth(currentPenProfile.strokeWidth)
                .setLimitRect(limit, ArrayList(excludeRects))
                .openRawDrawing()
            helper.setStrokeStyle(currentPenProfile.getOnyxStrokeStyle())

            helper.setRawDrawingEnabled(true)
            helper.setRawDrawingRenderEnabled(!eraserManager.isEraserModeEnabled())
        }
    }

    override fun initializeDeviceReceiver() {
        val deviceReceiver = createDeviceReceiver() as OnyxDeviceReceiverWrapper
        deviceReceiver.enable(this, true)
        deviceReceiver.setSystemNotificationPanelChangeListener { open ->
            onyxTouchHelper?.setRawDrawingEnabled(!open)
            surfaceView?.let { sv ->
                renderToScreen(sv, bitmap)
            }
        }.setSystemScreenOnListener {
            surfaceView?.let { sv ->
                renderToScreen(sv, bitmap)
            }
        }
    }

    override fun onCleanupDeviceReceiver() {
        onyxDeviceReceiver?.enable(this, false)
    }

    override fun handleSurfaceViewCreated(sv: SurfaceView) {
        surfaceView = sv

        // Initialize bitmap in rendering manager
        renderingManager.initializeSurfaceView(sv)

        initializeTouchHelper(sv)
    }

    public override fun forceScreenRefresh() {
        Log.d(TAG, "forceScreenRefresh() called")

        // Create/update bitmap if needed
        surfaceView?.let { sv ->
            renderingManager.initializeSurfaceView(sv)
            renderingManager.forceScreenRefresh(sv, shapeManager.getAllShapes())
        }
    }

    // Public API for external components
    fun setCurrentNote(note: Note) {
        navigationHandler.setCurrentNote(note)
        databaseManager.setCurrentNote(note, shapeManager)
        enableFingerTouch()
    }

    fun prepareForHomeView() {
        onyxTouchHelper?.setRawDrawingEnabled(false)
        enableFingerTouch()
        databaseManager.saveAllShapesToDatabase(shapeManager.getAllShapes(), currentPenProfile)
    }

    fun clearDrawing() {
        shapeManager.clearShapes()
        navigationHandler.getCurrentNote()?.let { note ->
            databaseManager.clearAllShapesForNote(note.id)
        }
        renderingManager.clearSurface(surfaceView)
    }

    open fun updateEraserMode(enabled: Boolean) {
        eraserManager.setEraserMode(enabled)
    }
}