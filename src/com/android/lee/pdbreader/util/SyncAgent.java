package com.android.lee.pdbreader.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.lee.pdbreader.R;
import com.android.lee.pdbreader.pdb.AbstractBookInfo;
import com.android.lee.pdbreader.provider.BookColumn;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;

public class SyncAgent {
    private static final String TAG = "SyncAgent";
    private ArrayList<File> pdbFileList = new ArrayList<File>();

    public void syncSD(Context context, File path, boolean otherType) {
        scanFile(path, otherType);
        SharedPreferences pref = context.getSharedPreferences(
                Constatnts.PREF_TAG, Context.MODE_PRIVATE);
        int charset = pref.getInt(Constatnts.DEFAULT_ENCODE, 0);

        String encode = context.getResources().getStringArray(R.array.charset)[charset];
        for (File f : pdbFileList) {
            try {
                if (DBUtil.isExits(context, f)) {
                    continue;
                }
                AbstractBookInfo book = AbstractBookInfo.newBookInfo(f, -1);
                try {
                    book.setEncode(encode);
                    book.setFile(f);
                    ContentValues values = new ContentValues();
                    values.put(BookColumn.NAME, book.mName);
                    values.put(BookColumn.PATH, f.getAbsolutePath());
                    values.put(BookColumn.ENDCODE, book.mEncode);
                    context.getContentResolver().insert(BookColumn.CONTENT_URI,
                            values);
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage(), e);
                    // skip
                }
            } catch (RuntimeException e) {
                Log.e(TAG, e.getMessage(), e);
                // ignore...
            }
        }
        DBUtil.clearFileNoFound(context);
    }



    private void scanFile(File dir, final boolean otherType) {

        File[] pdbList = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                try {
                    if (otherType) {
                        return file.isDirectory()
                                || file.getName().toLowerCase()
                                        .endsWith(".pdb")
                                || file.getName().toLowerCase()
                                        .endsWith(".txt");
                        // || file.getName().toLowerCase().endsWith(".htm")
                        // || file.getName().toLowerCase().endsWith(".html");
                    } else {
                        return file.isDirectory()
                                || file.getName().toLowerCase()
                                        .endsWith(".pdb");
                    }

                } catch (RuntimeException e) {
                    Log.e(TAG, e.getMessage(), e);
                    return false;
                }
            }
        });

        for (File p : pdbList) {
            if (p.isDirectory()) {
                scanFile(p, otherType);
            } else {
                pdbFileList.add(p);
            }


        }

    }
}
