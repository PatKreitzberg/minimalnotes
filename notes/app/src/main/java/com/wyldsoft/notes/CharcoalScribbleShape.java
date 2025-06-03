package com.wyldsoft.notes.shape;

import com.wyldsoft.notes.data.ShapeFactory;
import com.wyldsoft.notes.helper.RendererHelper;
import com.wyldsoft.notes.util.RendererUtils;
import com.onyx.android.sdk.data.PenConstant;
import com.onyx.android.sdk.data.note.ShapeCreateArgs;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.NeoCharcoalPenV2;
import com.onyx.android.sdk.pen.PenRenderArgs;

import java.util.List;

public class CharcoalScribbleShape extends Shape {

    @Override
    public void render(RendererHelper.RenderContext renderContext) {
        List<TouchPoint> points = touchPointList.getPoints();
        applyStrokeStyle(renderContext);

        PenRenderArgs renderArgs = new PenRenderArgs()
                .setCreateArgs(new ShapeCreateArgs())
                .setCanvas(renderContext.canvas)
                .setPenType(ShapeFactory.getCharcoalPenType(texture))
                .setColor(strokeColor)
                .setErase(isTransparent())
                .setPaint(renderContext.paint)
                .setScreenMatrix(RendererUtils.getPointMatrix(renderContext));
        if (strokeWidth <= PenConstant.CHARCOAL_SHAPE_DRAW_NORMAL_SCALE_WIDTH_THRESHOLD) {
            renderArgs.setStrokeWidth(strokeWidth)
                    .setPoints(points);
            NeoCharcoalPenV2.drawNormalStroke(renderArgs);
        } else {
            renderArgs.setStrokeWidth(strokeWidth)
                    .setPoints(points)
                    .setRenderMatrix(RendererUtils.getPointMatrix(renderContext));
            NeoCharcoalPenV2.drawBigStroke(renderArgs);
        }

    }
}
