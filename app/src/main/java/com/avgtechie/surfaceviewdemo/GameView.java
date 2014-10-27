package com.avgtechie.surfaceviewdemo;

import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import java.io.File;

public class GameView extends SurfaceView implements Callback {

    private static final String TAG = "GameView";
    private GameRunner runner;
    private Game game;

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("AP", "changed");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        game.onTouchEvent(event);
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("AP", "created");
        game = new Game(getContext(), getWidth(), getHeight(), holder, getResources());
        File outputFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES), "fbo-gl-recording.mp4");
        runner = new GameRunner(game);
        runner.start();
        Log.d(TAG, "outputFile = " + outputFile.getAbsolutePath());

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("AP", "destroyed");
        if (runner != null) {
            runner.shutDown();
            while (runner != null) {
                try {
                    runner.join();
                    runner = null;
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
