package com.avgtechie.glsurfaceview;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.io.IOException;

/**
 * Created by ashish on 10/26/14.
 */
public class DemoGLSurfaceView extends GLSurfaceView {

    private DemoSurfaceViewRenderer mRenderer;

    public DemoGLSurfaceView(Context context) {
        super(context);
        //step 1 : setup GLSurfaceView
        setupGLSurfaceView();
    }

    public DemoGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupGLSurfaceView();
    }

    private void setupGLSurfaceView() {
        setEGLContextClientVersion(2);
        try {
            mRenderer = new DemoSurfaceViewRenderer(getHolder(), getContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        Log.d(VideoSurfaceViewActivity.TAG, String.format("onTouchEvent() X = %f, Y = %f", event.getX(), event.getY()));
        queueEvent(new Runnable() {
            @Override
            public void run() {
                Log.d(VideoSurfaceViewActivity.TAG, "run()");
                mRenderer.setColor(event.getX() / getWidth(), event.getY() / getHeight(), 1.0f);
            }
        });
        return true;
    }
}
