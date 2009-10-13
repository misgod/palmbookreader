package com.android.lee.pdbreader.pdb;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;

public class TxtBookInfo extends AbstractBookInfo {
    private boolean isProgressing;
    public TxtBookInfo(long id) {
        super(id);
    }

    @Override
    public void setFile(File pdb) throws IOException {
        mFile = pdb;
        String name = pdb.getName();
        int end = name.indexOf(".");
        if(end >0){
            mName = name.substring(0,name.lastIndexOf(".")); 
        }else{
            mName = name;
        }

    }

    @Override
    public int getPageCount() {
        return 1;
    }

    boolean isStop;
    public  void stop(){
        isStop = true;
    }
    

    public  boolean isProgressing(){
        return isProgressing;
    }
    
    
    
    
    public CharSequence getText() throws IOException {
        isProgressing = true;
        StringBuilder body = new StringBuilder();
        try{
            BufferedReader input = new BufferedReader(new InputStreamReader( new FileInputStream(mFile),mEncode));
            String lineStr;
            while ((lineStr = input.readLine() )!=null) {
                body.append(lineStr.replace("    ", " ").replace("\t", "  ")).append("\n");
                if(isStop){
                    isStop = false;
                    break;
                }
            }

            input.close();
        }finally{
            isProgressing = false;
        }
       
        return body;

    }

    @Override
    public boolean supportFormat() {
        return false;
    }

}
