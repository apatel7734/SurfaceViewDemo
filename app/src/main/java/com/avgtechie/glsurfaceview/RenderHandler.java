package com.avgtechie.glsurfaceview;


import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by ashish on 10/26/14.
 */
public class RenderHandler extends Handler {


    private static final int MSG_SURFACE_CREATED = 0;
    private static final int MSG_SURFACE_CHANGED = 1;
    private static final int MSG_DO_FRAME = 2;
    private static final int MSG_RECORDING_ENABLED = 3;
    private static final int MSG_RECORD_METHOD = 4;
    private static final int MSG_SHUTDOWN = 5;
    private static final String TAG = "RenderHandler";

    // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
    // but no real harm in it.
    private WeakReference<RenderThread> mWeakRenderThread;


    /**
     * Call from render thread.
     */
    public RenderHandler(RenderThread rt) {
        mWeakRenderThread = new WeakReference<RenderThread>(rt);
    }


    @Override
    public void handleMessage(Message msg) {
        //Log.d(TAG, "handleMessage : " + msg.what);
        int what = msg.what;
        RenderThread renderThread = mWeakRenderThread.get();

        if (renderThread == null) {
            Log.w(TAG, "RenderHandler.handleMessage: weak ref is null");
            return;
        }


        switch (what) {
            case MSG_SURFACE_CREATED:
                renderThread.surfaceCreated();
                break;
            case MSG_SURFACE_CHANGED:
                renderThread.surfaceChanged(msg.arg1, msg.arg2);
                break;
            case MSG_DO_FRAME:
                long timestamp = (((long) msg.arg1) << 32) | (((long) msg.arg2) & 0xffffffffL);
                renderThread.doFrame(timestamp);
                break;
            case MSG_RECORDING_ENABLED:
                //renderThread.setRecordingEnabled(msg.arg1 != 0);
                break;
            case MSG_RECORD_METHOD:
                //renderThread.setRecordMethod(msg.arg1);
                break;
            case MSG_SHUTDOWN:
                //renderThread.shutdown();
                break;
            default:
                throw new RuntimeException("unknown message " + what);
        }


    }

    public void sendSurfaceCreated() {
        sendMessage(obtainMessage(MSG_SURFACE_CREATED));
    }

    public void sendSurfaceChanged(int format, int width, int height) {
        sendMessage(obtainMessage(MSG_SURFACE_CHANGED, width, height));
    }

    public void sendDoFrame(long frameTimeNanos) {
        sendMessage(obtainMessage(MSG_DO_FRAME, (int) (frameTimeNanos >> 32), (int) frameTimeNanos));
    }

    public void setRecordingEnabled(boolean enabled) {
        sendMessage(obtainMessage(MSG_RECORDING_ENABLED, enabled ? 1 : 0, 0));
    }

    public void sendShutdown() {
        sendMessage(obtainMessage(RenderHandler.MSG_SHUTDOWN));
    }


}


