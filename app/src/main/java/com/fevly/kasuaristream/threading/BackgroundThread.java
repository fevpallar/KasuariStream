package com.fevly.kasuaristream.threading;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class BackgroundThread extends HandlerThread {
    private Handler backgroundHandler ;

    public BackgroundThread(String name) {
        super(name);
        this.backgroundHandler = new Handler();
    }

    public Handler getBackgroundHandler() {
        return backgroundHandler;
    }

}
