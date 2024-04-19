package com.fevly.kasuaristream.file;

/*============================================
Author : Fevly pallar
Contact : fevly.pallar@gmail.com
============================================== */
import android.content.Context;
import android.content.ContextWrapper;
import androidx.annotation.NonNull;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtil {
    private ContextWrapper contextWrapper;
    private final String prefix = "VID_";
    private final String ext = ".mp4";
    private final String timeFormat = "yyyy_MM_dd_HH_mm_ss_SSS" ;

    public File getFile (){
        /*===================18042024========================================
       Extraks dir. via contextwrapper berfungsi di Android 10 ,
       Tapi test di atasnya.
       ==============================================================*/
        return new File(contextWrapper.getExternalFilesDir(null).toString() +"/"+generateFileName());
    }
    public String generateFileName (){;
        SimpleDateFormat formatter =  new SimpleDateFormat(timeFormat, Locale.US);
        String fileName = prefix+formatter.format(new Date())+ext;
        return fileName;
    }
    public FileUtil( ContextWrapper contextWrapper) {
        this.contextWrapper = contextWrapper;
    }
}
