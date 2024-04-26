package com.fevly.kasuaristream.encdr;

import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;


import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class VideoEncoder implements Runnable {

    private static WindowSurface mInputWindowSurface;
    private EglConfigUtil mEglCore;
    private static FrameRect mFullScreen;
    private static int mTextureId;
    private int mFrameNum;
    private static VideoEncoderCore mVideoEncoder;

    private volatile EncoderHandler mHandler;

    private Object mReadyLock = new Object();
    private boolean mReady;
    private boolean mRunning;
    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_QUIT = 5;

    public static class EncoderConfig {
        final File mOutputFile;
        final int mWidth;
        final int mHeight;
        final int mBitRate;
        final EGLContext mEglContext;

        public EncoderConfig(File outputFile, int width, int height, int bitRate,
                EGLContext sharedEglContext) {
            mOutputFile = outputFile;
            mWidth = width;
            mHeight = height;
            mBitRate = bitRate;
            mEglContext = sharedEglContext;
        }

        @Override
        public String toString() {
            return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate +
                    " to '" + mOutputFile.toString() + "' ctxt=" + mEglContext;
        }
    }

    public void startRecording(EncoderConfig config) throws InterruptedException {

        synchronized (mReadyLock) {
            if (mRunning) {
                return;
            }
            mRunning = true;
            new Thread(this, "").start();
            while (!mReady) {
                    mReadyLock.wait();

            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }

    public void stopRecording() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
    }

    /*
     true kalau recording sudah starts
     */
    public boolean isRecording() {
        synchronized (mReadyLock) {
            return mRunning;
        }
    }

    /*
     * refresh EGL context
     */
    public void refreshEGLSurface(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    /*

    * Kasih tahun VideoRecord kalau frame baru tersedia
     */
    public void frameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (mReadyLock) {
            if (!mReady) {

                // Masuk sini #7
                // langsung return kalau tombol record belum ditekan
                return;
            }
        }


        // LINE DIBAWAH INI SKIP KALAU TOMBOL BELUM DITEKAN
        float[] transform = new float[16];
        //used to adjust the position, orientation, or size of the texture
        surfaceTexture.getTransformMatrix(transform);
        long timestamp = surfaceTexture.getTimestamp();

        int upperBits = (int) (timestamp >> 32);  // Extract upper 32 bits
        int lowerBits = (int) timestamp;           // Extract lower 32 bits

        mHandler.sendMessage(
                mHandler.obtainMessage(MSG_FRAME_AVAILABLE, upperBits, lowerBits, transform)
        );;
    }


    public void setTextureId(int id) {
        synchronized (mReadyLock) {
            if (!mReady) {
                return;
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null));
    }

    @Override
    public void run() {
        Looper.prepare(); // looper milik thread ini
        synchronized (mReadyLock) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyLock.notify();
        }
        Looper.loop();
        synchronized (mReadyLock) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }


    private static class EncoderHandler extends Handler {
        private WeakReference<VideoEncoder> mWeakEncoder;

        public EncoderHandler(VideoEncoder encoder) {
            mWeakEncoder = new WeakReference<VideoEncoder>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            VideoEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                return;
            }

            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                case MSG_FRAME_AVAILABLE:

                /*    long timestamp = (((long) inputMessage.arg1) << 32) |
                            (((long) inputMessage.arg2) & 0xffffffffL);*/
                    long timestamp = ((long) inputMessage.arg1 << 32) | ((long) inputMessage.arg2 & 0xffffffffL);


                    //Handles notification of an available frame.
                    mVideoEncoder.drainEncoder(false);
                    mFullScreen.drawFrame(mTextureId, (float[]) obj);

                    mInputWindowSurface.setPresentationTime(timestamp);
                    mInputWindowSurface.swapBuffers();

                    break;
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(inputMessage.arg1);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    private void handleStartRecording(EncoderConfig config) {
        mFrameNum = 0;
        prepareEncoder(config.mEglContext, config.mWidth, config.mHeight, config.mBitRate,
                config.mOutputFile);
    }


    private void handleStopRecording() {

        mVideoEncoder.drainEncoder(true);
        releaseEncoder();
    }

    private void handleSetTexture(int id) {
        mTextureId = id;
    }

    /*
         Trigger setelah balik ke foreground
     */
    private void handleUpdateSharedContext(EGLContext newSharedContext) {

        mInputWindowSurface.releaseEglSurface();
        mFullScreen.release(false);
        mEglCore.release();

        // buat  EGLContext yg baru dan recreate window surface.
        mEglCore = new EglConfigUtil(newSharedContext, EglConfigUtil.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        mFullScreen = new FrameRect(
                new TextureUtility(TextureUtility.ProgramType.TEXTURE_EXT));
    }

    private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate,
            File outputFile) {
        try {
            mVideoEncoder = new VideoEncoderCore(width, height, bitRate, outputFile);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mEglCore = new EglConfigUtil(sharedContext, EglConfigUtil.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();

        mFullScreen = new FrameRect(
                new TextureUtility(TextureUtility.ProgramType.TEXTURE_EXT));
    }

    private void releaseEncoder() {
        mVideoEncoder.release();
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release(false);
            mFullScreen = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }


}
