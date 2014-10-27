package com.avgtechie.surfaceviewdemo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.avgtechie.surfaceviewdemo.Bat.Position;

public class Game {

    private static final String TAG = "Game";

    public enum State {
        PAUSED, WON, LOST, RUNNING
    }

    private SoundPool soundPool;

    private State state = State.PAUSED;

    private SurfaceHolder surfaceHolder;
    private Resources resources;

    private Ball ball;

    private Bat player;
    private Bat computer;

    private Paint textPaint;
    private Context context;

    private int[] sounds = new int[5];

    public Game(Context context, int screenWidth, int screenHeight, SurfaceHolder surfaceHolder, Resources resources) {
        this.surfaceHolder = surfaceHolder;
        this.resources = resources;
        this.context = context;

        ball = new Ball(screenWidth, screenHeight);
        player = new Bat(screenWidth, screenHeight, Position.LEFT);
        computer = new Bat(screenWidth, screenHeight, Position.RIGHT);
    }

    public void initGamePositions() {
        ball.initPosition();
        player.initPosition();
        computer.initPosition();
    }

    public void init() {
        Bitmap ballImage = BitmapFactory.decodeResource(resources, R.drawable.cropped_orig);
        Bitmap ballShadow = BitmapFactory.decodeResource(resources, R.drawable.cropped_orig);
        // Bitmap ballImage = BitmapFactory.decodeResource(resources,
        // R.drawable.button);
        // Bitmap ballShadow = BitmapFactory.decodeResource(resources,
        // R.drawable.buttonshadow);
        Bitmap batImage = BitmapFactory.decodeResource(resources, R.drawable.bat);
        Bitmap batShadow = BitmapFactory.decodeResource(resources, R.drawable.bat);

        ball.init(ballImage, ballShadow);
        player.init(batImage, batShadow);
        computer.init(batImage, batShadow);

        textPaint = new Paint();
        textPaint.setTextAlign(Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.BLUE);
        textPaint.setTextSize(70);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        sounds[Sound.START] = soundPool.load(context, R.raw.start, 1);
        sounds[Sound.BOUNCE1] = soundPool.load(context, R.raw.bounce1, 1);
        sounds[Sound.BOUNCE2] = soundPool.load(context, R.raw.bounce2, 1);
        sounds[Sound.WON] = soundPool.load(context, R.raw.win, 1);
        sounds[Sound.LOST] = soundPool.load(context, R.raw.lose, 1);

        soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                if (sounds[Sound.START] == sampleId) {
                    Log.d("Sounds", "START :" + sounds[Sound.START]);
                    soundPool.play(sampleId, 1, 1, 1, 0, 1);
                }
            }
        });

    }

    public void update(long elapsed) {

        if (state == State.RUNNING) {
            updateGame(elapsed);
        }
    }

    private void updateGame(long elapsed) {
        if (player.getScreenRect().contains(ball.getScreenRect().left, ball.getScreenRect().centerY())) {
            Log.d("Sounds", "Bounce1 :" + sounds[Sound.BOUNCE1]);
            soundPool.play(sounds[Sound.BOUNCE1], 1, 1, 1, 0, 1);
            ball.moveRight();
        } else if (computer.getScreenRect().contains(ball.getScreenRect().right, ball.getScreenRect().centerY())) {
            Log.d("Sounds", "Bounce2 :" + sounds[Sound.BOUNCE2]);
            soundPool.play(sounds[Sound.BOUNCE2], 1, 1, 1, 0, 1);
            ball.moveLeft();
        } else if (ball.getScreenRect().left < player.getScreenRect().right) {
            state = State.LOST;
            Log.d("Sounds", "LOST :" + sounds[Sound.LOST]);
            soundPool.play(sounds[Sound.LOST], 1, 1, 1, 0, 1);
            initGamePositions();
        } else if (ball.getScreenRect().right > computer.getScreenRect().left) {
            state = State.WON;
            Log.d("Sounds", "WON :" + sounds[Sound.WON]);
            soundPool.play(sounds[Sound.WON], 1, 1, 1, 0, 1);
            initGamePositions();
        }

        ball.update(elapsed);
        computer.update(elapsed, ball);
    }

    private void drawText(Canvas canvas, String text) {
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;
        canvas.drawText(text, centerX, centerY, textPaint);
    }

    private void drawGame(Canvas canvas) {

        ball.draw(canvas);
        player.draw(canvas);
        computer.draw(canvas);
    }

    public void draw() {
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas != null) {
            canvas.drawColor(Color.WHITE);

            switch (state) {
                case LOST:
                    drawText(canvas, "You Lost...");
                    break;
                case PAUSED:
                    drawText(canvas, "Tap Screen to start...");
                    break;
                case RUNNING:
                    drawGame(canvas);
                    break;
                case WON:
                    drawText(canvas, "You Won...");
                    break;
            }
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    public SurfaceHolder getSurfaceHolder() {
        return surfaceHolder;
    }

    public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    public Resources getResources() {
        return resources;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    public void onTouchEvent(MotionEvent event) {

        if (state == State.RUNNING) {
            if (event.getX() < 100) {
                player.setPosition(event.getY());
            }

        } else if (MotionEvent.ACTION_DOWN == event.getAction()) {
            state = State.RUNNING;
        }
    }
}
