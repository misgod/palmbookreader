package com.misgod.pdbreader.util;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.mozilla.universalchardet.UniversalDetector;

import android.util.Log;

public class ConvertUtil {

    public static long readDWORD(byte[] bytes) {
        long b0 = (bytes[0] & 0xff);
        long b1 = ((long) (bytes[1] & 0xff)) << 8;
        long b2 = ((long) (bytes[2] & 0xff)) << 16;
        long b3 = ((long) (bytes[3] & 0xff)) << 24;
        return (b0 | b1 | b2 | b3);
    }
    
    public static Date getFileTime(long dwordH, long dwordL) {
        Calendar c1601 = Calendar.getInstance();
        c1601.set(Calendar.YEAR, 1601);
        c1601.set(Calendar.MONTH, Calendar.JANUARY);
        c1601.set(Calendar.DAY_OF_MONTH, 1);
        c1601.set(Calendar.HOUR, 0);
        c1601.set(Calendar.MINUTE, 0);
        c1601.set(Calendar.SECOND, 0);
        c1601.set(Calendar.MILLISECOND, 0);
        long interval = 0 - c1601.getTimeInMillis();

        long time1 = dwordH << 32;
        time1 += dwordL;
        time1 /= 10000;
        return new Date(time1 - interval);
    }
    
    
    
    public static String guessCharset(String fileName) throws IOException{
    	 byte[] buf = new byte[4096];
    	    java.io.FileInputStream fis = new java.io.FileInputStream(fileName);


    	    UniversalDetector detector = new UniversalDetector(null);


    	    int nread;
    	    while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
    	      detector.handleData(buf, 0, nread);
    	    }
    	    detector.dataEnd();

    	    String encoding = detector.getDetectedCharset();
    	    if (encoding != null) {
    	      Log.d("ConvertUtil",fileName+" detected encoding = " + encoding);
    	    } else {
    	    	   Log.d("ConvertUtil","No encoding detected = " + encoding);
    	    }

    	    detector.reset();
    	    return encoding;
    }
    
    
}
