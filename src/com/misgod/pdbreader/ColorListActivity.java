
package com.misgod.pdbreader;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.misgod.pdbreader.util.ColorUtil;
import com.misgod.pdbreader.util.Constatnts;

/**
 * This example shows how to use choice mode on a list. This list is in
 * CHOICE_MODE_SINGLE mode, which means the items behave like checkboxes.
 */
public class ColorListActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setListAdapter(new MyArrayAdapter(this,
                android.R.layout.simple_list_item_single_choice));

        final ListView listView = getListView();
        listView.setItemsCanFocus(false);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        SharedPreferences pref = getSharedPreferences(Constatnts.PREF_TAG,
                Context.MODE_PRIVATE);
        listView.setItemChecked(pref.getInt(Constatnts.TEXT_COLOR, 0), true);
        
        setResult(Activity.RESULT_CANCELED);

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                Intent data = new Intent();
                data.putExtra("DATA", position);
                setResult(Activity.RESULT_OK, data);
                SharedPreferences pref = getSharedPreferences(
                        Constatnts.PREF_TAG, Context.MODE_PRIVATE);
                Editor editor = pref.edit();
                editor.putInt(Constatnts.TEXT_COLOR, getListView()
                        .getCheckedItemPosition());
                
                
                
                editor.commit();
                finish();
            }
        });

    }



    private class MyArrayAdapter extends ArrayAdapter<String> {
        private FBColor[] mColorList;
        private final String mString;

        public MyArrayAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            mString = getResources().getString(R.string.choose_color);
            ColorUtil colorUtil = new ColorUtil(context);
            int size = colorUtil.getColorSize();
            mColorList = new  FBColor[size];
            
            for(int i=0;i<size;i++){
                Integer[] color  = colorUtil.getColor(i);
                mColorList[i] = new FBColor(color[0], color[1]);
            }
        }

        @Override
        public int getCount() {
            return mColorList.length;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = super.getView(position, convertView, parent);
            }
            if (convertView instanceof TextView) {
                TextView tv = (TextView) convertView;
                tv.setTextColor(mColorList[position].foreground);
                tv.setBackgroundColor(mColorList[position].background);
            }

            return convertView;
        }

        @Override
        public String getItem(int position) {
            return mString;
        }

    };


    private final class FBColor {
        public int foreground;
        public int background;

        FBColor(int f, int b) {
            foreground = f;
            background = b;
        }

    }


}
