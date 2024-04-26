package com.fevly.kasuaristream.cam;
/*============================================
Author : Fevly pallar
Contact : fevly.pallar@gmail.com
============================================== */
import android.content.ContextWrapper;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import android.util.Size;

import com.fevly.kasuaristream.file.FileUtil;

import java.io.IOException;
/*==============================================
Note : Kelas cadangan untuk migrasi ke Camera API v2.
sekarang msh pake config untuk camera api yg lama
Karena porting Camera API 2 ke struktur yg sudah ada sungguh ,aduhai, bohay, sulit...
============================================== */
// berfungsi setelah instance dari CameraInspector tercipta
public class CamUtility extends MediaRecorder {
    private int vidSurface = 2; // surface
    private int frRate = 60  ;
    private FileUtil fileUtil;

    private MediaRecorder medRecord;
    private ContextWrapper contextWrapper;

    public void setGeneratedFile() {
        // ini harusnya disana (FileUtil)
        try {
            fileUtil = new FileUtil(this.contextWrapper);
        } catch (Exception ex) {
            Log.d("KasuariStream", ex.getMessage());
        }

        this.medRecord.setOutputFile(fileUtil.getFile().getAbsolutePath());
    }


    public CamUtility(ContextWrapper contextWrapper, MediaRecorder medRecord) throws IOException {
        this.medRecord = medRecord;
        this.contextWrapper = contextWrapper;
        medRecord.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        medRecord.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        medRecord.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        medRecord.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));// issue, disable
        medRecord.setVideoSize(640, 480);
        medRecord.setVideoFrameRate(30);
        // output file
        setGeneratedFile();
        medRecord.setVideoEncodingBitRate(10_000_000);
        medRecord.prepare();
    }

}
