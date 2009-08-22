package com.android.lee.pdbreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ZoomControls;

import com.android.lee.pdbreader.pdb.PDBBookInfo;
import com.android.lee.pdbreader.provider.BookColumn;
import com.android.lee.pdbreader.util.ColorUtil;
import com.android.lee.pdbreader.util.Constatnts;

import java.io.File;
import java.io.IOException;

public class PalmBookReaderActivity extends Activity implements
        View.OnClickListener {
    protected static final String TAG = "PilotBookReaderActivity";
    private static final int MAX_TEXT_SIZE = 36;
    private static final int MIN_TEXT_SIZE = 8;
    private static final int REQUEST_COLOR = 0x123;

    private ZoomControls zoomControl;
    private PDBBookInfo mBook;


    private TextView mBody;
    private View topPanel;
    private ScrollView scrollview;

    private ColorUtil colorUtil;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.bookreader);



        long id = getIntent().getExtras().getLong("ID");
        Uri pdbUri = Uri.parse(BookColumn.CONTENT_URI + "/" + id);
        Cursor cursor = getContentResolver().query(
                pdbUri,
                new String[] {
                        BookColumn._ID, BookColumn.NAME, BookColumn.AUTHOR,
                        BookColumn.ENDCODE, BookColumn.PATH,
                        BookColumn.LAST_PAGE, BookColumn.FORMAT,
                        BookColumn.LAST_OFFSET}, null, null, null);


        String path = "";
        String encode = null;
        int format = 0;
        int lastPage = 0;
        int lastOffset = 0;
        if (cursor.moveToNext()) {
            int pathIdx = cursor.getColumnIndexOrThrow(BookColumn.PATH);
            int encodeIdx = cursor.getColumnIndexOrThrow(BookColumn.ENDCODE);
            int lastpageIdx = cursor
                    .getColumnIndexOrThrow(BookColumn.LAST_PAGE);
            int formatIdx = cursor.getColumnIndexOrThrow(BookColumn.FORMAT);
            int offsetIdx = cursor
                    .getColumnIndexOrThrow(BookColumn.LAST_OFFSET);
            path = cursor.getString(pathIdx);
            encode = cursor.getString(encodeIdx);
            lastPage = cursor.getInt(lastpageIdx);
            format = cursor.getInt(formatIdx);
            lastOffset = cursor.getInt(offsetIdx);
        }
        cursor.close();

        mBook = new PDBBookInfo(id);
        try {
            mBook.setFile(new File(path), encode);
            mBook.setFormat(format);
            mBook.setPage(lastPage);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }



        topPanel = findViewById(R.id.top_panel);


        
        mBody = (TextView) findViewById(R.id.text);
        mBody.setFocusable(false);
      
        
        
        
        TextView pageTitle = (TextView) findViewById(R.id.page_title);
        TextView pageTitle1 = (TextView) findViewById(R.id.page_title1);
        pageTitle.setText(mBook.mName);
        pageTitle1.setText(mBook.mName);



        scrollview = ((ScrollView) findViewById(R.id.scrollview));
        scrollview.setFillViewport(true);
        scrollview.setFocusable(false);


        View prevButton = findViewById(R.id.prev_button);
        prevButton.setOnClickListener(this);
        View nextButton = findViewById(R.id.next_button);
        nextButton.setOnClickListener(this);

        View prevButton1 = findViewById(R.id.prev_button1);
        prevButton1.setOnClickListener(this);
        View nextButton1 = findViewById(R.id.next_button1);
        nextButton1.setOnClickListener(this);
        

        zoomControl = (ZoomControls) findViewById(R.id.zoom_control);
        zoomControl.hide();

        zoomControl.setOnZoomInClickListener(new OnClickListener() {
            public void onClick(View view) {
                float size = mBody.getTextSize() + 1;
                if (MAX_TEXT_SIZE >= size) {
                    mBody.setTextSize(size);
                    zoomControl.setIsZoomOutEnabled(true);
                    zoomControl.setIsZoomInEnabled(size != MAX_TEXT_SIZE);
                }
            }
        });

        zoomControl.setOnZoomOutClickListener(new OnClickListener() {
            public void onClick(View view) {

                float size = mBody.getTextSize() - 1;
                if (MIN_TEXT_SIZE <= size) {
                    mBody.setTextSize(size);
                    zoomControl.setIsZoomInEnabled(true);
                    zoomControl.setIsZoomOutEnabled(size != MIN_TEXT_SIZE);
                }

            }
        });

        mBody.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionevent) {
                switch (motionevent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (zoomControl.isShown()) {
                        SharedPreferences pref = getSharedPreferences(
                                Constatnts.PREF_TAG, Context.MODE_PRIVATE);
                        Editor editor = pref.edit();
                        editor.putFloat(Constatnts.TEXT_SIZE, mBody
                                .getTextSize());
                        editor.commit();

                        zoomControl.hide();
                    }



                    break;
                }
                return false;
            }
        });
        doShow(lastOffset);
        SharedPreferences pref = getSharedPreferences(Constatnts.PREF_TAG,
                Context.MODE_PRIVATE);
        mBody.setTextSize(pref.getFloat(Constatnts.TEXT_SIZE, mBody
                .getTextSize()));

        colorUtil = new ColorUtil(this);
        changeColor(-1); // use pref
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            scrollview.scrollBy(0, (scrollview.getHeight() - mBody
                    .getLineHeight()));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            scrollview.scrollBy(0, -(scrollview.getHeight() - mBody
                    .getLineHeight()));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            scrollview.fullScroll(View.FOCUS_UP);
            return true;
        }

        
        return super.onKeyDown(keyCode, event);
    }


    /* ---------------------menu---------------------------- */
    private static final int MENU_ZOOM = 0;
    private static final int MENU_COLOR = 1;
    private static final int MENU_CHARSET = 2;
    private static final int MENU_ROTATAION = 3;
    private static final int MENU_FORMAT = 4;
    private static final int MENU_BRIGHTNESS = 5;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ZOOM, MENU_ZOOM, getResources().getString(
                R.string.menu_text_size));

        menu.add(0, MENU_COLOR, MENU_COLOR, R.string.menu_color);
        menu.add(0, MENU_CHARSET, MENU_CHARSET, R.string.menu_charset);
        menu.add(0, MENU_ROTATAION, MENU_ROTATAION, "");
        menu.add(0, MENU_FORMAT, MENU_FORMAT, R.string.menu_format);
        menu.add(0, MENU_BRIGHTNESS, MENU_BRIGHTNESS, R.string.menu_brightness);
        return true;

    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem rotationIgtem = menu.getItem(MENU_ROTATAION);
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_NOSENSOR) {
            rotationIgtem.setTitle(R.string.menu_lock);
        } else {
            rotationIgtem.setTitle(R.string.menu_unlock);
        }

        return super.onPrepareOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == MENU_ZOOM) {
            zoomControl.show();
        } else if (item.getItemId() == MENU_COLOR) {
            Intent intent = new Intent(this, ColorListActivity.class);

            startActivityForResult(intent, REQUEST_COLOR);
            // scrollview.fullScroll(View.FOCUS_UP);
            // mBody.moveCursorToVisibleOffset();
        } else if (item.getItemId() == MENU_CHARSET) {
            showDialog(ENCODE_DIALOG);
        } else if (item.getItemId() == MENU_ROTATAION) {
            if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_NOSENSOR) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);// .SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
            }
        } else if (item.getItemId() == MENU_FORMAT) {
            showDialog(FORMAT_DIALOG);
        } else if (item.getItemId() == MENU_BRIGHTNESS) {
            showDialog(BRIGHTNESS_DIALOG);
        }



        return true;
    }


    private void setPageTitle(){
        TextView pageview = (TextView) findViewById(R.id.page_index);
        TextView pageview1 = (TextView) findViewById(R.id.page_index1);
        
        pageview.setText((mBook.mPage + 1) + " of " + mBook.mCount);
        pageview1.setText(pageview.getText());
    }
    

    private void doShow(final int offset) {
        try {
            mBody.setText(mBook.getText());
            if (offset > 0) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        int line = mBody.getLayout().getLineForOffset(offset);
                        int scollY = topPanel.getHeight() + line
                                * mBody.getLineHeight();
                        scrollview.scrollTo(0, scollY);
                    }
                }, 50);

            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }

        setPageTitle();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_COLOR && resultCode == Activity.RESULT_OK) {
            int index = data.getIntExtra("DATA", -1);



            changeColor(index);
        }
    }



    protected void onDestroy() {
        int y = Math.max(0, scrollview.getScrollY() - topPanel.getHeight());
        int line = y / mBody.getLineHeight();
        int offset = mBody.getLayout().getLineStart(line);

        Uri pdbUri = Uri.parse(BookColumn.CONTENT_URI + "/" + mBook.mID);
        ContentValues values = new ContentValues();
        // values.put(BookColumn.NAME, mBook.mName);
        values.put(BookColumn.LAST_PAGE, mBook.mPage);
        values.put(BookColumn.ENDCODE, mBook.mEncode);
        values.put(BookColumn.FORMAT, mBook.mFormat);
        values.put(BookColumn.LAST_OFFSET, offset);

        Long now = Long.valueOf(System.currentTimeMillis());
        values.put(BookColumn.CREATE_DATE, now);


        int result = getContentResolver().update(pdbUri, values, null, null);
        // if(result>0){
        // Toast.makeText(this, R.string.msg_store, 2000).show();
        // }

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();

    }

    private static final int ENCODE_DIALOG = 0;
    private static final int FORMAT_DIALOG = 1;
    private static final int BRIGHTNESS_DIALOG = 2;

    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {
        case ENCODE_DIALOG:
            String[] charsetArray = getResources().getStringArray(
                    R.array.charset);
            int i = 0;
            for (String charset : charsetArray) {
                if (charset.equals(mBook.mEncode)) {
                    break;
                }
                i++;
            }
            return new AlertDialog.Builder(this).setTitle(
                    R.string.default_charset).setSingleChoiceItems(
                    R.array.charset, i, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String encode = PalmBookReaderActivity.this
                                    .getResources().getStringArray(
                                            R.array.charset)[which];
                            try {
                                mBody.setText("");
                                int page = mBook.mPage;
                                mBook.setFile(mBook.mFile, encode);

                                mBook.setPage(page);
                                mBody.setText(mBook.getText());
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage(), e);
                            }

                            dialog.dismiss();
                        }
                    }).create();

        case FORMAT_DIALOG:
            return new AlertDialog.Builder(this).setTitle(
                    R.string.default_charset).setSingleChoiceItems(
                    R.array.format, mBook.mFormat,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
//                            String encode = PalmBookReaderActivity.this
//                                    .getResources().getStringArray(
//                                            R.array.charset)[which];
                            try {
                                mBody.setText("");
                                int page = mBook.mPage;
                                mBook.setFile(mBook.mFile, mBook.mEncode);
                                mBook.setFormat(which);
                                mBook.setPage(page);
                                mBody.setText(mBook.getText());
                            } catch (Exception e) {
                                Log.e(TAG, e.getMessage(), e);
                            }

                            dialog.dismiss();
                        }
                    }).create();

        case BRIGHTNESS_DIALOG:
            return new BrightnessDialog(this);



        }
        return null;
    }


    private void changeColor(int index) {
        int[] color = colorUtil.getColor(index); // use pref
        mBody.setTextColor(color[0]);
        mBody.setBackgroundColor(color[1]);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.prev_button
                || view.getId() == R.id.prev_button1) {
            if (mBook.hasPrevPage()) {
                mBody.setText("");
                mBook.prevPage();
                doShow(0);
                scrollview.scrollTo(0,0);
            }
        } else if (view.getId() == R.id.next_button
                || view.getId() == R.id.next_button1) {
            if (mBook.hasNextPage()) {
                mBody.setText("");
                mBook.nextPage();
                doShow(0);
                scrollview.scrollTo(0,0);
            }
        }
    }
}
