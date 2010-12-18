package com.misgod.pdbreader.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class ColorUtil {
    private Context mContext;
    public static final int[] foregroungColor = new int[]{
        0xffcccccc, 0xffEEEEEE,0xffffffff,0xff000000,0xffcc0000,0xffFFFFBB,0xffFF0084,0xffB0E2FF,0xffB02B2C,0xff330000,
        0xff00CC00,0xff000033,0xff001100,0xffCCFFFF,0xffC3C3B4,0xff3C0F00
    };
    
    public static final int[] backgroungColor = new int[]{
        0xff000000,0xffffffff,0xffFFF0F0,0xffEEEEEE,0xffdddddd,0xff36393D,0xfff9f7ed,0xffFFFFB4,
        0xff4096EE,0xffC3D9FF, 0xffF0FFF0,0xffFFFF88,0xffE1F0B4,0xff330000,0xffCCCCFF,0xffCCFFFF, 
        0xffFF96F0
    };
    
    public ColorUtil(Context context){
        mContext = context;
    }
    
    
    public int getColorSize(){
        return foregroungColor.length * backgroungColor.length;
    }
    
    /**
     * colorIndex: -1, read from share pref
     * @return 2-length, 0 foreground, 1 background
     */
    public int[] getColor(int colorIndex){
            int index=0;
            if(colorIndex<0){
                SharedPreferences pref =mContext.getSharedPreferences(Constatnts.PREF_TAG, Context.MODE_PRIVATE);
                index = pref.getInt(Constatnts.TEXT_COLOR, 0);
                index = Math.max(0, index);
            }else{
                index = colorIndex;
            }
            
            int bgIndex = index /foregroungColor.length;
            int fgIndex = index % foregroungColor.length;
            if(fgIndex == foregroungColor.length){
                fgIndex=0;
            }
            
            
            int[] result = new int[2];
            result[0]  = foregroungColor[fgIndex];
            result[1] = backgroungColor[bgIndex];
            return result;
    }
    
}
