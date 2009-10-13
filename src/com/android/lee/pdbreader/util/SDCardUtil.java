package com.android.lee.pdbreader.util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;

import com.android.lee.pdbreader.NoSDCardActivity;


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
    
    
}
