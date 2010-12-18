package com.misgod.pdbreader.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.Log;

import com.misgod.pdbreader.NoSDCardActivity;


public class SDCardUtil {
    private static String TAG = "SDCardUtil";
    private static Activity mActivity;
    private static BroadcastReceiver mMountReceiver;

    public static void addListener(final Activity activity) {
        if (mActivity == null) {
            mActivity = activity;
            mMountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                        Intent sdIntent = new Intent(activity,
                                NoSDCardActivity.class);
                      
                        activity.startActivity(sdIntent);
                    }
                }
            };

            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_UNMOUNTED);
            intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
            intentFilter.addDataScheme("file");
            activity.registerReceiver(mMountReceiver, intentFilter);

            if (!SDCardUtil.hasExternalStorage()) {

                Intent sdIntent = new Intent(activity, NoSDCardActivity.class);
                activity.startActivity(sdIntent);
            }
        }
    }

    public static void removeListener(final Activity activity) {
        if (activity == mActivity) {
            mActivity.unregisterReceiver(mMountReceiver);
            mActivity = null;
            mMountReceiver = null;
        }
    }



    public synchronized static boolean hasExternalStorage() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }



    public static boolean isMediaShared() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_SHARED);
    }
    
    
    public static boolean copyFile(Context context,String source, File target) {
        /* force write to sd */

        BufferedInputStream in = null;
        BufferedOutputStream out = null;

        try {
            in = new BufferedInputStream(context.getAssets().open(source),8192);
            out = new BufferedOutputStream(new FileOutputStream(target));
            byte[] tmpData = new byte[8192];
            int c;
            while ((c = in.read(tmpData)) != -1) {
                out.write(tmpData, 0, c);
            }

        } catch (Exception e) {
            Log.e(TAG,e.getMessage(),e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {   
                    e.printStackTrace();
                }
                in = null;
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out = null;
            }

        }
        return true;
    }

    
    
}
