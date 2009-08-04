package com.android.lee.pdbreader.util;

import java.util.Calendar;
import java.util.Date;

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
}
