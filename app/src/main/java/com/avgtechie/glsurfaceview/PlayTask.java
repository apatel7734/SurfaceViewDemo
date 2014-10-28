package com.avgtechie.glsurfaceview;

/**
 * Created by ashish on 10/28/14.
 */

import android.os.Handler;
import android.os.Message;

import java.io.IOException;

/**
 * Thread helper for video playback.
 * <p/>
 * The PlayerFeedback callbacks will execute on the thread that creates the object,
 * assuming that thread has a looper.  Otherwise, they will execute on the main looper.
 */
public class PlayTask implements Runnable {
    private static final int MSG_PLAY_STOPPED = 0;

    private MoviePlayer mPlayer;
    private MoviePlayer.PlayerFeedback mFeedback;
    private boolean mDoLoop;
    private Thread mThread;
    private LocalHandler mLocalHandler;

    private final Object mStopLock = new Object();
    private boolean mStopped = false;

    /**
     * Prepares new PlayTask.
     *
     * @param player   The player object, configured with control and output.
     * @param feedback UI feedback object.
     */
    public PlayTask(MoviePlayer player, MoviePlayer.PlayerFeedback feedback) {
        mPlayer = player;
        mFeedback = feedback;

        mLocalHandler = new LocalHandler();
    }

    /**
     * Sets the loop mode.  If true, playback will loop forever.
     */
    public void setLoopMode(boolean loopMode) {
        mDoLoop = loopMode;
    }

    /**
     * Creates a new thread, and starts execution of the player.
     */
    public void execute() {
        mPlayer.setLoopMode(mDoLoop);
        mThread = new Thread(this, "Movie Player");
        mThread.start();
    }

    /**
     * Requests that the player stop.
     * <p/>
     * Called from arbitrary thread.
     */
    public void requestStop() {
        mPlayer.requestStop();
    }

    /**
     * Wait for the player to stop.
     * <p/>
     * Called from any thread other than the PlayTask thread.
     */
    public void waitForStop() {
        synchronized (mStopLock) {
            while (!mStopped) {
                try {
                    mStopLock.wait();
                } catch (InterruptedException ie) {
                    // discard
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            mPlayer.play();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            // tell anybody waiting on us that we're done
            synchronized (mStopLock) {
                mStopped = true;
                mStopLock.notifyAll();
            }

            // Send message through Handler so it runs on the right thread.
            mLocalHandler.sendMessage(
                    mLocalHandler.obtainMessage(MSG_PLAY_STOPPED, mFeedback));
        }
    }

    private static class LocalHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;

            switch (what) {
                case MSG_PLAY_STOPPED:
                    MoviePlayer.PlayerFeedback fb = (MoviePlayer.PlayerFeedback) msg.obj;
                    fb.playbackStopped();
                    break;
                default:
                    throw new RuntimeException("Unknown msg " + what);
            }
        }
    }
}
