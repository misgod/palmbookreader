package com.misgod.pdbreader.pdb;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.zip.DataFormatException;
import java.util.zip.InflaterInputStream;

import org.WeaselReader.PalmIO.PalmDocDB;
import org.WeaselReader.PalmIO.ZtxtDB;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.misgod.pdbreader.util.FormatNotSupportException;

/*
 * #define dmDBNameLength 32/* 31 chars + 1 null terminator
 * 
 * struct pdb_header { // 78 bytes total char name[ dmDBNameLength ]; DWord
 * attributes; Word version; DWord create_time; DWord modify_time; DWord
 * backup_time; DWord modificationNumber; DWord appInfoID; DWord sortInfoID;
 * char type[4]; char creator[4]; DWord id_seed; DWord nextRecordList; Word
 * numRecords; };
 */
public class PDBBookInfo extends AbstractBookInfo {
    public int mCount;
    public int[] mRecodeOffset;
    public boolean isProgressing;
    private int mType = TYPE_NORMAL;
    
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_Hodoo = 1;
    private static final int TYPE_EREADER = 2;
    public PDBBookInfo(long id){
        super(id);
    }

    @Override
    public void setFile(File pdb,boolean headerOnly) throws IOException {
        mFile = pdb;

        mPage = 0;
        FileChannel channel = new FileInputStream(pdb).getChannel();

        byte[] nameByte = new byte[32];
        channel.map(MapMode.READ_ONLY, 0, 32).get(nameByte);
        mName = new String(nameByte, mEncode).replace('_',' ').trim();
        
        byte[] fourBytes = new byte[4];
        channel.map(MapMode.READ_ONLY, 60, 4).get(fourBytes);
        String type = new String(fourBytes);
        channel.map(MapMode.READ_ONLY, 64, 4).get(fourBytes);
        String creatorID = new String(fourBytes);
        if(type.equals(PalmDocDB.PALMDOC_TYPE_ID) && creatorID.equals(PalmDocDB.PALMDOC_CREATOR_ID)){
        	setFormat(2);
        }else if(type.equals("PNRd") && creatorID.equals("PPrs")){
        	mType = TYPE_EREADER;
        	setFormat(1);
        	setEncode("US-ASCII");
    	}else if(type.equals("zTXT") && creatorID.equals("GPlm")){
    		setFormat(3);
    		setEncode("UTF-8");
    	}else if(creatorID.equals("MTIT")){
        	setEncode("Big5");
        	mType = TYPE_Hodoo;
        }else if(creatorID.equals("MTIU")){
        	setEncode("UTF-16LE");
        	mType = TYPE_Hodoo;
        }else if(creatorID.equals("SilX")){
        	throw new FormatNotSupportException("iSilo");
        }
        
       	 mCount = channel.map(MapMode.READ_ONLY, 76, 2).asCharBuffer().get();

       	 
       	int offset = 78;
       	
         if(mType == TYPE_EREADER){
//        	 StringBuilder sb = new StringBuilder(Integer.toHexString(mCount));
//        	 mCount = Integer.parseInt(sb.toString());
        	 offset +=8;
         }
        
        
        mRecodeOffset = new int[mCount];
        for (int i = 0; i < mCount; i++) {
            mRecodeOffset[i] = channel.map(MapMode.READ_ONLY, offset, 4)
                    .asIntBuffer().get();
            offset += 8;
        }
        
        if(mType == TYPE_Hodoo){
        	byte[] fifityBytes = new byte[50];
        	channel.map(MapMode.READ_ONLY, mRecodeOffset[0],
        			fifityBytes.length).order(ByteOrder.BIG_ENDIAN).get(fifityBytes);
        	String str = new String(fifityBytes, mEncode); 
        	mName = str.substring(0, str.indexOf(27, 0)).trim(); //escape
        }
        
        
        channel.close();
        

    }
   
    @Override
    public int getPageCount() {
        return mCount;
    }
    
    boolean isStop;
    public  void stop(){
        isStop = true;
    }
    
    public  boolean isProgressing(){
        return isProgressing;
    }
    
    
    public String getText() throws IOException, DataFormatException {
        isProgressing = true;
        try{
        	if(mFormat ==2){ 
                return getPalmDoc();
            }else if(mFormat ==3){ 
            	return getZTXT();
            }else{
            	  return getMyText();
            }
        }finally{
            isProgressing = false;
        }
    }
    
    public String getMyText() throws IOException {
        /* Record Header */
       // int recordBegin = 78 + 8 * mCount;

        FileChannel channel = new FileInputStream(mFile).getChannel();

        channel.position(mRecodeOffset[mPage]);
        StringBuilder body = new StringBuilder();
        ByteBuffer bodyBuffer;
        if (mPage + 1 < mCount) {
            int length = mRecodeOffset[mPage + 1] - mRecodeOffset[mPage];
            bodyBuffer = channel.map(MapMode.READ_ONLY, mRecodeOffset[mPage],
                    length).order(ByteOrder.BIG_ENDIAN);
            byte[] tmpCache = new byte[bodyBuffer.capacity()];
            bodyBuffer.get(tmpCache);
            if(mFormat==1){
                byte[] ttt = new byte[8192];
                InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(tmpCache));
                int c=0;
                while((c = input.read(ttt))>0){
                    String str = new String(ttt,0,c, mEncode);
                    body.append(replaceString(str));
                    if(isStop){
                        isStop = false;
                        break;
                    }
                    
                }
                input.close();
            }else{
                String str = new String(tmpCache,mEncode);
                body.append(str);
            }
        } else {
            bodyBuffer = ByteBuffer.wrap(new byte[8192]);
            int idx;
            while ((idx = channel.read(bodyBuffer)) > 0) {
                String str = new String(bodyBuffer.array(), mEncode);
                body.append(str);
                if(isStop){
                    isStop = false;
                    break;
                }
            }
        }
        channel.close();

        
        return filter(body);

    }
    
    
    
    public String getPalmDoc() throws IOException, DataFormatException {
        PalmDocDB palmDoc = new PalmDocDB(mFile,mEncode);
        mCount = palmDoc.getNumDataRecords();
        String result = palmDoc.readTextRecord(mPage);
        palmDoc.close();
        return result;
    }
    
    public String getZTXT() throws IOException, DataFormatException {
    	ZtxtDB palmDoc = new ZtxtDB(mFile,mEncode);
    	palmDoc.initializeDecompression();
        mCount = palmDoc.getNumDataRecords();
        String result = palmDoc.readTextRecord(mPage);
        palmDoc.close();
        return result;
    }
    
    
    /**
     * filter palm doc tag
     */
    private String filter(StringBuilder body){
        int begin=-1;
        int c=-1;

        while((c = body.indexOf("\\v",c+1)) >0 ){
            if(begin>-1){
                body.delete(begin, c);
                begin = -1;
            }else{
                begin = c;
            }
        }
       
        c=-1;
        while((c = body.indexOf("\\a"))>-1){
            char myChar = (char)Integer.parseInt(body.substring(c+2, c+5));
            body.replace(c, c+5,  String.valueOf(myChar));
        }
        String result = body.toString();
        
        result = result.replaceAll("\\\\Sd=\\\".*\\\"|\\\\(Sd|Fn|Cn|[TwQq])=\".*\"|\\\\((Sp|Sb|Sd|Fn)|[pxcriuovtnsbqlBkI\\-])", "")
        .replace("\\\\", "\\");

        if(mType == TYPE_Hodoo){
        	result = replaceString(result);
        }
        
        return result;
        
    }
    
    
    
    private String replaceString(String str){
       return str.replace("\r", "").replace("　", " ").replace('﹁', '「')
     .replace('﹂', '」').replace('﹃', '『').replace('﹄', '』').replace('︽','《').replace('︾', '》')
     .replace('︱', '–').replace('︵', '(').replace('︶', ')').replace('︷', '{').replace('︸', '}')
     .replace('︻', '【').replace('︼', '】').replace('︿', '〈').replace('﹀', '〉').replace('︸', '}')
     .replace((char) 0x1B, '\t').replace('｜', '—').replace('︹', '〔').replace('︺','〕').replace("\0","");

    }

    public Bitmap getImage() throws IOException {
        /* Record Header */
        int recordBegin = 78 + 8 * mCount;
        Bitmap result =null;
        FileChannel channel = new FileInputStream(mFile).getChannel();

        channel.position(mRecodeOffset[mPage]);

        ByteBuffer bodyBuffer;
        if (mPage + 1 < mCount) {
            int length = mRecodeOffset[mPage + 1] - mRecodeOffset[mPage];
            bodyBuffer = channel.map(MapMode.READ_ONLY, mRecodeOffset[mPage],
                    length);
            byte[] tmpCache = new byte[bodyBuffer.capacity()];
            bodyBuffer.get(tmpCache);
            FileOutputStream o = new FileOutputStream("/sdcard/test.bmp");
            o.write(tmpCache);
            o.flush();
            o.getFD().sync();
            o.close();

            
            result = BitmapFactory.decodeByteArray(tmpCache, 0, length);
        } else {
//            bodyBuffer = ByteBuffer.wrap(new byte[8192]);
//            int idx;
//            while ((idx = channel.read(bodyBuffer)) > 0) {
//                String str = new String(bodyBuffer.array(), mEncode);
//                body.append(replaceString(str));
//            }
        }
        
        
        channel.close();
        return result;
    }

    @Override
    public boolean supportFormat() {
        return true;
    }


}
