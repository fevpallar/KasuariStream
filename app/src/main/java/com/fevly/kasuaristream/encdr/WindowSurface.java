

package com.fevly.kasuaristream.encdr;

import android.view.Surface;

public class WindowSurface extends EglSurfaceInit {
    private Surface mSurface;
    private boolean mReleaseSurface;


    public WindowSurface(EglConfigUtil eglCore, Surface surface, boolean releaseSurface) {
        super(eglCore);
        createWindowSurface(surface);
        mSurface = surface;
        mReleaseSurface = releaseSurface;
    }

    public void release() {
        releaseEglSurface();
        if (mSurface != null) {
            if (mReleaseSurface) {
                mSurface.release();
            }
            mSurface = null;
        }
    }

    public void recreate(EglConfigUtil newEglCore) {
        mEglCore = newEglCore;          // pindah ke konteks baru
        createWindowSurface(mSurface);  // buat surface baru
    }
}
