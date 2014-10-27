package com.avgtechie.surfaceviewdemo;

public class GameRunner extends Thread {


    private static final String TAG = "GameRunner";

    private volatile boolean running = true;
    private Game game;


    public GameRunner(Game game) {
        this.game = game;
    }

    @Override
    public void run() {
        game.init();
        long lastTime = System.currentTimeMillis();

        // Game loop
        while (running) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastTime;
            if (elapsed < 100) {
                game.update(elapsed);
                game.draw();
            }
            lastTime = now;
        }
    }

    public void shutDown() {
        running = false;
    }
}
