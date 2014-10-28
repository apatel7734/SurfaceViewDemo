package com.avgtechie.glsurfaceview;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.avgtechie.surfaceviewdemo.R;

import java.io.File;

public class VideoSurfaceViewActivity extends Activity implements SurfaceHolder.Callback, View.OnTouchListener, Choreographer.FrameCallback {

    public static final String TAG = "GLSurfaceViewActivity";
    private SurfaceView surfaceView;
    private SurfaceView movieSurfaceView;
    private RenderThread mRenderThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_glsurface_view);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(this);
        surfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);

        movieSurfaceView = (SurfaceView) findViewById(R.id.movie_surface_view);
        movieSurfaceView.getHolder().addCallback(this);

        surfaceView.setOnTouchListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    public void stopRecording(View view) {
        Log.d(TAG, "stopRecording");
        mRenderThread.stopEncoder();
        Toast.makeText(this, "Recording Stopped!", Toast.LENGTH_SHORT).show();
    }

    public void startRecording(View view) {
        Log.d(TAG, "startRecording");
        mRenderThread.startEncoder();
        Toast.makeText(this, "Recording Started!", Toast.LENGTH_SHORT).show();
    }


    //SurfaceHolder.Callback methods
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated()");

        if (holder.equals(surfaceView.getHolder())) {
            File outputFile = FileUtil.getInstance().getMemeFilePath();
            mRenderThread = new RenderThread(holder, outputFile, this);
            mRenderThread.start();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            RenderHandler rh = mRenderThread.getHandler();
            if (rh != null) {
                rh.sendSurfaceCreated();
            }
            // start the draw events
            Choreographer.getInstance().postFrameCallback(this);
        } else {
            Surface surface = holder.getSurface();
            MoviePlayerUtil.getInstance().clearSurface(surface);
            MoviePlayerUtil.getInstance().playAndStopMovie(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.equals(surfaceView.getHolder())) {
            RenderHandler rh = mRenderThread.getHandler();
            if (rh != null) {
                rh.sendSurfaceChanged(format, width, height);
            }
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed holder=" + holder);

        if (holder.equals(surfaceView.getHolder())) {
            // We need to wait for the render thread to shut down before continuing because we
            // don't want the Surface to disappear out from under it mid-render.  The frame
            // notifications will have been stopped back in onPause(), but there might have
            // been one in progress.
            //
            // TODO: the RenderThread doesn't currently wait for the encoder / muxer to stop,
            //       so we can't use this as an indication that the .mp4 file is complete.

            RenderHandler rh = mRenderThread.getHandler();
            if (rh != null) {
                rh.sendShutdown();
                try {
                    mRenderThread.join();
                } catch (InterruptedException ie) {
                    // not expected
                    throw new RuntimeException("join was interrupted", ie);
                }
            }
            mRenderThread = null;
            //mRecordingEnabled = false;

            // If the callback was posted, remove it.  Without this, we could get one more
            // call on doFrame().
            Choreographer.getInstance().removeFrameCallback(this);
        }
        Log.d(TAG, "surfaceDestroyed complete");
    }

    //OnTouchListenerMethod
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(TAG, String.format("onTouch X = %f, Y = %f", event.getX(), event.getY()));
        return true;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        //Log.d(TAG, "doFrame : " + frameTimeNanos);
        RenderHandler rh = mRenderThread.getHandler();
        if (rh != null) {
            Choreographer.getInstance().postFrameCallback(this);
            rh.sendDoFrame(frameTimeNanos);
        }
    }
}
