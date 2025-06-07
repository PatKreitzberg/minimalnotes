package com.wyldsoft.notes.render;

import android.graphics.Bitmap;
import android.view.SurfaceView;

import com.wyldsoft.notes.editorview.drawing.shape.DrawingShape;

import java.util.List;

/**
 * Created by lxm on 2018/2/8.
 */

public interface Renderer {

    void renderToBitmap(SurfaceView surfaceView,
                        RendererHelper.RenderContext renderContext);

    void renderToBitmap(final List<DrawingShape> shapes,
                        final RendererHelper.RenderContext renderContext);

    void renderToScreen(final SurfaceView surfaceView, final Bitmap bitmap);

    void renderToScreen(final SurfaceView surfaceView,
                        RendererHelper.RenderContext renderContext);

    void onDeactivate(final SurfaceView surfaceView);

    void onActive(final SurfaceView surfaceView);
}
