package com.wyldsoft.notes.onyx

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import android.view.SurfaceView
import androidx.lifecycle.lifecycleScope
import com.onyx.android.sdk.data.note.TouchPoint
import com.onyx.android.sdk.pen.TouchHelper
import com.onyx.android.sdk.pen.data.TouchPointList
import com.onyx.android.sdk.rx.RxManager
import com.wyldsoft.notes.editor.EditorState
import com.wyldsoft.notes.GlobalDeviceReceiver
import com.wyldsoft.notes.render.RendererToScreenRequest
import com.wyldsoft.notes.render.RendererHelper
import com.wyldsoft.notes.TouchUtils
import com.wyldsoft.notes.base.BaseDeviceReceiver
import com.wyldsoft.notes.base.BaseDrawingActivity
import com.wyldsoft.notes.base.BaseTouchHelper
import com.wyldsoft.notes.data.ShapeFactory
import com.wyldsoft.notes.shapepkg.Shape
import com.wyldsoft.notes.pen.PenType
import com.wyldsoft.notes.database.DatabaseManager
import com.wyldsoft.notes.database.ShapeUtils
import com.wyldsoft.notes.database.entities.Note
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap

open class OnyxDrawingActivity : BaseDrawingActivity() {
    private var rxManager: RxManager? = null
    private var onyxTouchHelper: TouchHelper? = null
    private var onyxDeviceReceiver: GlobalDeviceReceiver? = null

    // Store all drawn shapes for re-rendering
    private val drawnShapes = mutableListOf<Shape>()

    // Renderer helper for shape rendering
    private var rendererHelper: RendererHelper? = null

    // Database integration
    private lateinit var databaseManager: DatabaseManager
    private var currentNote: Note? = null

    // Add this property to track if we're loading from database
    private var isLoadingFromDatabase = false

    override fun initializeSDK() {
        // Onyx-specific initialization
        rendererHelper = RendererHelper()

        // Initialize database
        databaseManager = DatabaseManager.getInstance(this)

        // Load current note if available
        loadCurrentNote()
    }

    private fun loadCurrentNote() {
        // This will be called from the navigation system when opening a note
        // For now, we'll create a default note for testing
        currentNote?.let { note ->
            loadShapesFromDatabase(note.id)
        }
    }

    fun setCurrentNote(note: Note) {
        currentNote = note
        loadShapesFromDatabase(note.id)
    }

    private fun loadShapesFromDatabase(noteId: String) {
        lifecycleScope.launch {
            try {
                isLoadingFromDatabase = true
                val databaseShapes = databaseManager.repository.getShapesInNoteSync(noteId)

                // Clear current shapes
                drawnShapes.clear()

                // Convert database shapes to drawing shapes
                databaseShapes.forEach { dbShape ->
                    val drawingShape = ShapeUtils.convertToDrawing(dbShape)
                    drawnShapes.add(drawingShape)
                }

                // Recreate bitmap with loaded shapes
                recreateBitmapFromShapes()

                // Render to screen
                surfaceView?.let { sv ->
                    bitmap?.let { renderToScreen(sv, it) }
                }

                Log.d(TAG, "Loaded ${drawnShapes.size} shapes from database for note $noteId")
                isLoadingFromDatabase = false

            } catch (e: Exception) {
                Log.e(TAG, "Error loading shapes from database", e)
                isLoadingFromDatabase = false
            }
        }
    }

    override fun createTouchHelper(surfaceView: SurfaceView): BaseTouchHelper {
        val callback = createOnyxCallback()
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
        val holder = surfaceView.holder ?: return false
        val canvas = holder.lockCanvas() ?: return false
        canvas.drawColor(Color.WHITE)
        holder.unlockCanvasAndPost(canvas)
        return true
    }

    override fun renderToScreen(surfaceView: SurfaceView, bitmap: Bitmap?) {
        if (bitmap != null) {
            getRxManager().enqueue(
                RendererToScreenRequest(
                    surfaceView,
                    bitmap
                ), null)
        }
    }

    override fun onResumeDrawing() {
        onyxTouchHelper?.setRawDrawingEnabled(true)
    }

    override fun onPauseDrawing() {
        onyxTouchHelper?.setRawDrawingEnabled(false)

        // Save current state to database
        saveCurrentStateToDatabase()
    }

    override fun onCleanupSDK() {
        onyxTouchHelper?.closeRawDrawing()

        // Save final state before cleanup
        saveCurrentStateToDatabase()

        drawnShapes.clear()
    }

    private fun saveCurrentStateToDatabase() {
        currentNote?.let { note ->
            if (!isLoadingFromDatabase) {
                saveShapesToDatabase(note.id)
            }
        }
    }

    private fun saveShapesToDatabase(noteId: String) {
        lifecycleScope.launch {
            try {
                // Clear existing shapes for this note
                databaseManager.repository.deleteAllShapesInNote(noteId)

                // Convert and save current shapes
                val databaseShapes = drawnShapes.map { drawingShape ->
                    ShapeUtils.convertToDatabase(drawingShape, noteId, currentPenProfile)
                }

                databaseManager.repository.saveShapes(databaseShapes)

                Log.d(TAG, "Saved ${databaseShapes.size} shapes to database for note $noteId")

            } catch (e: Exception) {
                Log.e(TAG, "Error saving shapes to database", e)
            }
        }
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
            helper.setRawDrawingRenderEnabled(true)
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
            helper.setRawDrawingRenderEnabled(true)
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

    override fun forceScreenRefresh() {
        Log.d("Onyx", "forceScreenRefresh() called")
        surfaceView?.let { sv ->
            cleanSurfaceView(sv)
            // Recreate bitmap from all stored shapes
            recreateBitmapFromShapes()
            bitmap?.let { renderToScreen(sv, it) }
        }
    }

    private fun getRxManager(): RxManager {
        if (rxManager == null) {
            rxManager = RxManager.Builder.sharedSingleThreadManager()
        }
        return rxManager!!
    }

    private fun createOnyxCallback() = object : com.onyx.android.sdk.pen.RawInputCallback() {
        override fun onBeginRawDrawing(b: Boolean, touchPoint: TouchPoint?) {
            isDrawingInProgress = true
            disableFingerTouch()
            EditorState.notifyDrawingStarted()
        }

        override fun onEndRawDrawing(b: Boolean, touchPoint: TouchPoint?) {
            isDrawingInProgress = false
            enableFingerTouch()

            // Save the new shape to database immediately
            currentNote?.let { note ->
                if (!isLoadingFromDatabase && drawnShapes.isNotEmpty()) {
                    saveShapeToDatabase(drawnShapes.last(), note.id)
                }
            }

            forceScreenRefresh()
            EditorState.notifyDrawingEnded()
        }

        override fun onRawDrawingTouchPointMoveReceived(touchPoint: TouchPoint?) {
            // Handle move events if needed
        }

        override fun onRawDrawingTouchPointListReceived(touchPointList: TouchPointList?) {
            touchPointList?.points?.let { points ->
                if (!isDrawingInProgress) {
                    isDrawingInProgress = true
                }
                drawScribbleToBitmap(points, touchPointList)
            }
        }

        override fun onBeginRawErasing(b: Boolean, touchPoint: TouchPoint?) {
            // Handle erasing start
        }

        override fun onEndRawErasing(b: Boolean, touchPoint: TouchPoint?) {
            // Handle erasing end
        }

        override fun onRawErasingTouchPointMoveReceived(touchPoint: TouchPoint?) {
            // Handle erase move
        }

        override fun onRawErasingTouchPointListReceived(touchPointList: TouchPointList?) {
            // Handle erase list
        }
    }

    private fun saveShapeToDatabase(shape: Shape, noteId: String) {
        lifecycleScope.launch {
            try {
                val databaseShape = ShapeUtils.convertToDatabase(shape, noteId, currentPenProfile)
                databaseManager.repository.saveShape(databaseShape)
                Log.d(TAG, "Saved shape to database for note $noteId")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving shape to database", e)
            }
        }
    }

    private fun drawScribbleToBitmap(points: List<TouchPoint>, touchPointList: TouchPointList) {
        Log.d(TAG, "drawScribbleToBitmap called list size " + touchPointList.size())
        surfaceView?.let { sv ->
            createDrawingBitmap()

            // Create and store the shape based on current pen type
            val shape = createShapeFromPenType(touchPointList)
            drawnShapes.add(shape)

            // Render the new shape to the bitmap
            renderShapeToBitmap(shape)
            renderToScreen(sv, bitmap)
        }
    }

    private fun createShapeFromPenType(touchPointList: TouchPointList): Shape {
        // Map pen type to shape type
        val shapeType = when (currentPenProfile.penType) {
            PenType.BALLPEN, PenType.PENCIL -> ShapeFactory.SHAPE_PENCIL_SCRIBBLE
            PenType.FOUNTAIN -> ShapeFactory.SHAPE_BRUSH_SCRIBBLE
            PenType.MARKER -> ShapeFactory.SHAPE_MARKER_SCRIBBLE
            PenType.CHARCOAL, PenType.CHARCOAL_V2 -> ShapeFactory.SHAPE_CHARCOAL_SCRIBBLE
            PenType.NEO_BRUSH -> ShapeFactory.SHAPE_NEO_BRUSH_SCRIBBLE
            PenType.DASH -> ShapeFactory.SHAPE_PENCIL_SCRIBBLE // Default to pencil for dash
        }

        // Create the shape
        val shape = ShapeFactory.createShape(shapeType)
        shape.setTouchPointList(touchPointList)
            .setStrokeColor(currentPenProfile.getColorAsInt())
            .setStrokeWidth(currentPenProfile.strokeWidth)
            .setShapeType(shapeType)

        // Set texture for charcoal if needed
        if (currentPenProfile.penType == PenType.CHARCOAL_V2) {
            shape.setTexture(com.onyx.android.sdk.data.note.PenTexture.CHARCOAL_SHAPE_V2)
        } else if (currentPenProfile.penType == PenType.CHARCOAL) {
            shape.setTexture(com.onyx.android.sdk.data.note.PenTexture.CHARCOAL_SHAPE_V1)
        }

        return shape
    }

    private fun renderShapeToBitmap(shape: Shape) {
        bitmap?.let { bmp ->
            val renderContext = rendererHelper?.getRenderContext() ?: return
            renderContext.bitmap = bmp
            renderContext.canvas = Canvas(bmp)
            renderContext.paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            // Initialize viewPoint for shapes that need it (like CharcoalScribbleShape)
            renderContext.viewPoint = android.graphics.Point(0, 0)

            shape.render(renderContext)
        }
    }

    private fun recreateBitmapFromShapes() {
        surfaceView?.let { sv ->
            // Create a fresh bitmap
            bitmap?.recycle()
            bitmap = createBitmap(sv.width, sv.height)
            bitmapCanvas = Canvas(bitmap!!)
            bitmapCanvas?.drawColor(Color.WHITE)

            // Get render context
            val renderContext = rendererHelper?.getRenderContext() ?: return
            renderContext.bitmap = bitmap
            renderContext.canvas = bitmapCanvas!!
            renderContext.paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            // Initialize viewPoint for shapes that need it (like CharcoalScribbleShape)
            renderContext.viewPoint = android.graphics.Point(0, 0)

            // Render all shapes
            for (shape in drawnShapes) {
                shape.render(renderContext)
            }
        }
    }

    // Add method to clear all drawings
    fun clearDrawing() {
        drawnShapes.clear()

        // Clear from database too
        currentNote?.let { note ->
            lifecycleScope.launch {
                databaseManager.repository.deleteAllShapesInNote(note.id)
            }
        }

        surfaceView?.let { sv ->
            bitmap?.recycle()
            bitmap = null
            bitmapCanvas = null
            cleanSurfaceView(sv)
        }
    }
}