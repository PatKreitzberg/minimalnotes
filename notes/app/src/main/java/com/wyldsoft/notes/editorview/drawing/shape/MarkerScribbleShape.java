package com.wyldsoft.notes.editorview.drawing.shape;

import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.NeoMarkerPen;
import com.wyldsoft.notes.render.RendererHelper;
import android.util.Log;
import java.util.List;

public class MarkerScribbleShape extends DrawingShape {

    @Override
    public void render(RendererHelper.RenderContext renderContext) {

        List<TouchPoint> points = touchPointList.getPoints();
        applyStrokeStyle(renderContext);
        List<TouchPoint> markerPoints = NeoMarkerPen.computeStrokePoints(points, strokeWidth,
                EpdController.getMaxTouchPressure());
        NeoMarkerPen.drawStroke(renderContext.canvas, renderContext.paint, markerPoints, strokeWidth, isTransparent());
        Log.d("Shape", "markerPoints" + markerPoints);
    }
}
