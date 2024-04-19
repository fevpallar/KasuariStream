package com.fevly.kasuaristream.cam;

/*============================================
Author : Fevly pallar
Contact : fevly.pallar@gmail.com
============================================== */

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Size;

public class CameraInspector {
    private Activity activity;
    private CameraManager cameraManager;

    // Cari element size yg paling besar
    /*==================================
     Note : ukuran video tidak boleh pakai ukuran max disini,
     front camera tidak bisa buffer gambar kalau terlalu high quality.

     Yang aneh meskipun internal support dari hardware bisa
     capai 1080 px , malah error
     sementara masih ukuran 640 x 480 untuk keperluan testing,,,,
     ====================================*/
    private Size getIndexDariMaximumElementSize(Size size[]) {
        int max = 0;
        Size targetSize = null;
        for (Size sz : size) {
            int dimension = sz.getWidth() * sz.getHeight();
            if (dimension > max) {
                max = dimension;
                targetSize = sz;
            }
        }
        return targetSize;
    }
    public CameraInspector(Activity activity) {
        this.activity = activity;
        cameraManager = (CameraManager) getSystemServc();
    }
    public Object getSystemServc() {
        return activity.getSystemService(Context.CAMERA_SERVICE);
    }




    public  void prepareCameraSetting(Handler backgroundHandler) throws CameraAccessException {
        /*==========================
         Kamera depan dulu. Besok2 klo ada waktu baru extend ke belakang..
         =============================*/
        String listCameraId[] = getListCameraID();

        for (String id : listCameraId) {
            CameraCharacteristics cameraCharacs = this.cameraManager.getCameraCharacteristics(id);
            if (cameraCharacs.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                StreamConfigurationMap mapKonfigStream = cameraCharacs.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (mapKonfigStream != null) {

                    Size previewSize[] = cameraCharacs.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            .getOutputSizes(ImageFormat.JPEG);
                    preview = getIndexDariMaximumElementSize(previewSize);


                    Size vidSize[] = cameraCharacs.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            .getOutputSizes(MediaRecorder.class);
                    videoSize = getIndexDariMaximumElementSize(vidSize);

                    imageReader = ImageReader.newInstance(preview.getWidth(), preview.getHeight(), ImageFormat.JPEG, 1);

                    imageReader.setOnImageAvailableListener(
                            new ImageReader.OnImageAvailableListener() {
                                @Override
                                public void onImageAvailable(ImageReader imageReader) {
                                   imageReader.acquireLatestImage().close();
                                }
                            },
                           backgroundHandler
                    );
                }
                cameraId = id;
            }
        }
    } // ends Iterate camera
    private String[] getListCameraID() throws CameraAccessException {
        return cameraManager.getCameraIdList();
    }


    public ImageReader getImageReader() {
        return imageReader;
    }

    public String getCameraId() {
        return cameraId;
    }

    public Size getPreview() {
        return preview;
    }

    public Size getVideoSize() {
        return videoSize;
    }
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    private ImageReader imageReader;
    private String cameraId = "";
    private Size preview, videoSize;

}
