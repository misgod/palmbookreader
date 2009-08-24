package com.android.lee.pdbreader.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.lee.pdbreader.R;
import com.android.lee.pdbreader.R.array;
import com.android.lee.pdbreader.pdb.PDBBookInfo;
import com.android.lee.pdbreader.provider.BookColumn;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;

public class SyncAgent {
    private static final String TAG = "SyncAgent";
    private ArrayList<File> pdbFileList = new  ArrayList<File>();
    
    public  void syncSD(Context context,File path){
        scanFile(path);
        SharedPreferences pref = context.getSharedPreferences(Constatnts.PREF_TAG, Context.MODE_PRIVATE);
        int charset = pref.getInt(Constatnts.DEFAULT_ENCODE, 0);
        
        String encode = context.getResources().getStringArray(R.array.charset)[charset];
        for(File f:pdbFileList){
            if(DBUtil.isExits(context, f)){
                continue;
            }
            PDBBookInfo book = new PDBBookInfo(-1);
            try {
                book.setFile(f, encode);
            } catch (IOException e) {
                Log.d(TAG,e.getMessage(),e);
            }
            ContentValues values = new ContentValues();
            values.put(BookColumn.NAME, book.mName);
            values.put(BookColumn.PATH, f.getAbsolutePath());
            values.put(BookColumn.ENDCODE, book.mEncode);
            context.getContentResolver().insert(BookColumn.CONTENT_URI,
                    values);
        }
        DBUtil.clearFileNoFound(context);    
    }
    
    
    
        

        private void scanFile(File dir){
            
            File[] pdbList = dir.listFiles(new FileFilter(){
                @Override
                public boolean accept(File file) {
                    return file.isDirectory() ||file.getName().toLowerCase().endsWith(".pdb");
                }
            });

            for(File p:pdbList){
                if(p.isDirectory()){
                    scanFile(p);
                }else{
                    pdbFileList.add(p);
                }


            }

        }    
}
