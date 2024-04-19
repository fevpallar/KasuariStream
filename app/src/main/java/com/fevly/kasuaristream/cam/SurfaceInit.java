package com.fevly.kasuaristream.cam;
/*============================================
Author : Fevly pallar
Contact : fevly.pallar@gmail.com
============================================== */
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Handler;
import android.view.Surface;
import android.view.TextureView;

import java.util.ArrayList;

// konsentrasi surface disini
public class SurfaceInit {
    private SurfaceTexture surfaceTexture;
    private Surface livePreview, recordSurface;
    private TextureView textureView;
    private CaptureRequest.Builder cameraRequestBuilder;
    private CameraDevice cameraDevice;
    private MediaRecorder mediaRecorder;

    public SurfaceInit(TextureView textureView,
                       int bufferWidth,
                       int bufferHeight,
                       MediaRecorder mediaRecorder,
                       CameraDevice cameraDevice,
                       CameraCaptureSession.StateCallback stateCallback,
                       Handler handler
    ) throws CameraAccessException {
        this.textureView = textureView;
        this.mediaRecorder = mediaRecorder;
        this.cameraDevice = cameraDevice;

        surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(bufferWidth, bufferHeight);

        livePreview = new Surface(surfaceTexture);
        recordSurface = mediaRecorder.getSurface();

        cameraRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

        cameraRequestBuilder.addTarget(livePreview);
        cameraRequestBuilder.addTarget(recordSurface);

//        cameraDevice.createCaptureSession(listOf(previewSurface, recordingSurface), captureStateVideoCallback, backgroundHandler)

        ArrayList<Surface> listSurface = new ArrayList();
        listSurface.add(livePreview);
        listSurface.add(recordSurface);

        cameraDevice.createCaptureSession(listSurface,stateCallback, handler);
        mediaRecorder.start();
    }


}
