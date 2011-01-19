package com.misgod.pdbreader.util;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;

public class ColorUtil {
    private Context mContext;
    private static final ArrayList<Integer[]> ColorList = new ArrayList<Integer[]>();
    
    static{
    	//black background
    	ColorList.add(new Integer[]{0xffffffff,0xff000000});
    	ColorList.add(new Integer[]{0xffeeeeee,0xff000000});
    	ColorList.add(new Integer[]{0xffcccccc,0xff000000});
    	ColorList.add(new Integer[]{0xffaaaaaa,0xff000000});
    	ColorList.add(new Integer[]{0xff888888,0xff000000});
    	ColorList.add(new Integer[]{0xff666666,0xff000000});
    	ColorList.add(new Integer[]{0xff444444,0xff000000});   	
    	//gray background
    	ColorList.add(new Integer[]{0xffffffff,0xff333333});
    	ColorList.add(new Integer[]{0xffeeeeee,0xff333333});
    	ColorList.add(new Integer[]{0xffcccccc,0xff333333});
    	ColorList.add(new Integer[]{0xffaaaaaa,0xff333333});
    	ColorList.add(new Integer[]{0xff888888,0xff333333});
    	ColorList.add(new Integer[]{0xff666666,0xff333333});
    	ColorList.add(new Integer[]{0xff444444,0xff333333});   	
    	
    	//light gray background
    	ColorList.add(new Integer[]{0xff666666,0xffcccccc});
    	ColorList.add(new Integer[]{0xff444444,0xffcccccc});
    	ColorList.add(new Integer[]{0xff222222,0xffcccccc});
    	ColorList.add(new Integer[]{0xff111111,0xffcccccc});
      	ColorList.add(new Integer[]{0xff000000,0xffcccccc});
    	
    	
    	//white background
    	ColorList.add(new Integer[]{0xff666666,0xffffffff});
    	ColorList.add(new Integer[]{0xff444444,0xffffffff});
    	ColorList.add(new Integer[]{0xff222222,0xffffffff});
    	ColorList.add(new Integer[]{0xff111111,0xffffffff});
      	ColorList.add(new Integer[]{0xff000000,0xffffffff});
    }
    

    
    public ColorUtil(Context context){
        mContext = context;
    }
    
    
    public int getColorSize(){
        return ColorList.size();
    }
    
    /**
     * colorIndex: -1, read from share pref
     * @return 2-length, 0 foreground, 1 background
     */
    public Integer[] getColor(int colorIndex){
            int index=0;
            if(colorIndex<0){
                SharedPreferences pref =mContext.getSharedPreferences(Constatnts.PREF_TAG, Context.MODE_PRIVATE);
                index = pref.getInt(Constatnts.TEXT_COLOR, 0);
                index = Math.max(0, index);
            }else{
                index = colorIndex;
            }
            

            
            
            Integer[] result = ColorList.get(index);
            return result;
    }
    
}
