package com.wyldsoft.notes.editorview.drawing.onyx

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.SurfaceView
import androidx.lifecycle.lifecycleScope
import com.onyx.android.sdk.api.device.epd.EpdController
import com.onyx.android.sdk.pen.TouchHelper
import com.wyldsoft.notes.editorview.editor.EditorState
import com.wyldsoft.notes.GlobalDeviceReceiver
import com.wyldsoft.notes.TouchUtils
import com.wyldsoft.notes.base.BaseDeviceReceiver
import com.wyldsoft.notes.editorview.drawing.base.BaseDrawingActivity
import com.wyldsoft.notes.editorview.drawing.base.BaseTouchHelper
import com.wyldsoft.notes.backend.database.entities.Note
import com.wyldsoft.notes.editorview.drawing.shape.DrawingShape
import kotlinx.coroutines.launch

/**
 * Refactored Onyx-specific implementation with consolidated touch helper update function
 * REFACTORED: Combined updateTouchHelperWithProfile and updateTouchHelperExclusionZones
 * into a single updateTouchHelper function to eliminate code duplication
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

    // Viewport management
    private var currentViewportController: com.wyldsoft.notes.editorview.viewport.ViewportController? = null
    private var viewportChangeListener: com.wyldsoft.notes.editorview.viewport.ViewportController.ViewportChangeListener? = null

    // Public access to surface view and pen profile for managers
    override var surfaceView: SurfaceView? = null

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
        eraserManager = OnyxEraserManager(
            shapeManager, 
            renderingManager, 
            databaseManager,
            { currentPenProfile } // Provide current pen profile
        )
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
        
        // Clean up viewport listener
        currentViewportController?.let { controller ->
            viewportChangeListener?.let { listener ->
                controller.removeViewportChangeListener(listener)
                Log.d(TAG, "Cleaned up viewport change listener")
            }
        }
    }

    override fun updateActiveSurface() {
        updateTouchHelper()
    }

    /**
     * REFACTORED: Single consolidated function to update touch helper configuration
     * Replaces both updateTouchHelperWithProfile and updateTouchHelperExclusionZones
     *
     * This function handles all touch helper reconfiguration needs:
     * - Pen profile changes (stroke width, color, style)
     * - Exclusion zone updates
     * - Surface changes
     * - Eraser mode changes
     */
    private fun updateTouchHelper() {
        onyxTouchHelper?.let { helper ->
            Log.d(TAG, "Updating touch helper configuration")

            // Step 1: Disable and close current drawing session
            helper.setRawDrawingEnabled(false)
            helper.closeRawDrawing()

            // Step 2: Get current surface dimensions and exclusion zones
            val limit = Rect()
            surfaceView?.getLocalVisibleRect(limit)
            val excludeRects = EditorState.getCurrentExclusionRects()

            Log.d(TAG, "Touch helper update - Surface: ${limit.width()}x${limit.height()} top: ${limit.top} bot: ${limit.bottom}, Exclusions: ${excludeRects.size}")

            // Step 3: Apply pen profile settings
            helper.setStrokeWidth(currentPenProfile.strokeWidth)
                .setStrokeColor(currentPenProfile.getColorAsInt())
                .setLimitRect(limit, ArrayList(excludeRects))
                .openRawDrawing()

            // Step 4: Set pen-specific stroke style
            helper.setStrokeStyle(currentPenProfile.getOnyxStrokeStyle())

            // Step 5: Configure for current mode (drawing vs erasing)
            helper.setRawDrawingEnabled(true)
            helper.setRawDrawingRenderEnabled(!eraserManager.isEraserModeEnabled())

            Log.d(TAG, "Touch helper configuration complete - Eraser mode: ${eraserManager.isEraserModeEnabled()}")
        }
    }

    /**
     * Handle eraser mode-specific touch helper updates
     * Called when eraser mode changes to update rendering behavior
     */
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

    /**
     * REFACTORED: Now delegates to the consolidated updateTouchHelper function
     */
    override fun updateTouchHelperWithProfile() {
        updateTouchHelper()
    }

    /**
     * REFACTORED: Now delegates to the consolidated updateTouchHelper function
     */
    override fun updateTouchHelperExclusionZones(excludeRects: List<Rect>) {
        Log.d(TAG, "updateTouchHelperExclusionZones called with ${excludeRects.size} rects")

        // The exclusion rects are already managed by EditorState.getCurrentExclusionRects()
        // so we just need to trigger a full touch helper update
        updateTouchHelper()
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

        initializeTouchHelper(sv)

        // If we have a current note, load its shapes now that surface is ready
        navigationHandler.getCurrentNote()?.let { note ->
            databaseManager.loadShapesFromDatabase(note.id, shapeManager)
        }
    }

    public override fun forceScreenRefresh() {
        Log.d(TAG, "forceScreenRefresh() called")

        // Create/update bitmap if needed
        surfaceView?.let { sv ->
            renderingManager.createOrGetDrawingBitmap(sv)
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

    /**
     * Set the viewport controller for rendering transformations
     */
    fun setViewportController(controller: com.wyldsoft.notes.editorview.viewport.ViewportController?) {
        // Remove previous listener if exists
        currentViewportController?.let { oldController ->
            viewportChangeListener?.let { listener ->
                oldController.removeViewportChangeListener(listener)
                Log.d(TAG, "Removed previous viewport change listener")
            }
        }
        
        // Update managers with new controller
        renderingManager.setViewportController(controller)
        shapeManager.setViewportController(controller)
        eraserManager.setViewportController(controller)
        
        // Set up new viewport change listener for automatic refresh
        controller?.let { viewportController ->
            val listener = object : com.wyldsoft.notes.editorview.viewport.ViewportController.ViewportChangeListener {
                override fun onViewportChanged(viewport: android.graphics.RectF, zoomLevel: Float) {
                    Log.d(TAG, "Viewport changed - zoom: ${(zoomLevel * 100).toInt()}%, bounds: $viewport")
                }
                
                override fun onVisibleShapesChanged(visibleShapes: List<com.wyldsoft.notes.editorview.drawing.shape.DrawingShape>) {
                    Log.d(TAG, "Visible shapes changed: ${visibleShapes.size} shapes")
                }
                
                override fun onViewportRefreshRequired() {
                    Log.d(TAG, "Viewport refresh required - forcing full screen refresh")
                    EpdController.enablePost(surfaceView, 1)
                    forceScreenRefresh()
                }
            }
            
            viewportController.addViewportChangeListener(listener)
            viewportChangeListener = listener
            Log.d(TAG, "Registered viewport change listener for automatic refresh")
        }
        
        currentViewportController = controller
        
        // Set up gesture-based viewport control
        setGestureViewportController(controller)
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

    /**
     * ENHANCED: Handle pen profile updates
     * Now triggers the consolidated touch helper update
     */
    override fun updatePenProfile(penProfile: com.wyldsoft.notes.pen.PenProfile) {
        Log.d(TAG, "Updating pen profile: $penProfile")
        currentPenProfile = penProfile
        updatePaintFromProfile()
        updateTouchHelper() // Use consolidated function
    }

    /**
     * ENHANCED: Handle exclusion zone updates
     * Now triggers the consolidated touch helper update
     */
    override fun updateExclusionZones(excludeRects: List<Rect>) {
        updateTouchHelperExclusionZones(excludeRects)
        Log.d(TAG, "Updated exclusion zones, forcing screen refresh")
        forceScreenRefresh()
    }

    /**
     * Handle zoom to fit functionality
     * Resets zoom to 100%
     */
    override fun handleZoomToFit() {
        Log.d(TAG, "Resetting zoom to 100%")
        currentViewportController?.resetZoomToFit()
    }
}