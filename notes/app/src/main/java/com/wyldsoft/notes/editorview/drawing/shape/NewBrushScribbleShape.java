package com.wyldsoft.notes.editorview.drawing.shape;

import com.wyldsoft.notes.render.RendererHelper;
import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.NeoBrushPen;
import com.onyx.android.sdk.pen.PenUtils;

import java.util.List;
import android.util.Log;

public class NewBrushScribbleShape extends DrawingShape {

    @Override
    public void render(RendererHelper.RenderContext renderContext) {
        List<TouchPoint> points = touchPointList.getPoints();
        applyStrokeStyle(renderContext);

        List<TouchPoint> NeoBrushPoints = NeoBrushPen.computeStrokePoints(points,
                strokeWidth, EpdController.getMaxTouchPressure());
        PenUtils.drawStrokeByPointSize(renderContext.canvas, renderContext.paint, NeoBrushPoints, isTransparent());
        Log.d("Shape", "neoBrushPoints.size()" + NeoBrushPoints.size());
    }
}
