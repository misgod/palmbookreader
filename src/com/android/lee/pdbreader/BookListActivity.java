package com.android.lee.pdbreader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.android.lee.pdbreader.provider.BookColumn;
import com.android.lee.pdbreader.util.Constatnts;
import com.android.lee.pdbreader.util.DBUtil;
import com.android.lee.pdbreader.util.SyncAgent;

import java.io.Externalizable;
import java.io.File;

public class BookListActivity extends ListActivity {

    private static final String TAG = "BookListActivity";
    private static final int ENCODE_DIALOG = 0;
    private static final int PROGRESS_DIALOG = 1;
    private static final int EXPIRED_DIALOG = 2;
    private static final int MENU_DIALOG = 3;
    private static final int SORT_DIALOG = 4;
    private static final int EDIT_DIALOG = 5;
    private static final int ABOUT_DIALOG = 6;
    private static final int SYNC_DIALOG = 7;
    private int selectedIndex;

    private static final int SORT_NAME = 0;
    private static final int SORT_AUTHOR = 1;
    private static final int SORT_RECENT = 2;
    private static final int SORT_PATH = 3;


    private int sortMode;
    private BooksListAdapter mAdapter;
    private EditText mFilterText;
    private static final String[] BookField = new String[] {
            BookColumn._ID, BookColumn.NAME, BookColumn.AUTHOR,
            BookColumn.ENDCODE, BookColumn.PATH, BookColumn.RATING};


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.books_list);

        ListView list = (ListView) findViewById(android.R.id.list);


        // Query for people
        Cursor cursor = managedQuery(BookColumn.CONTENT_URI, BookField, null,
                null, null);

        mAdapter = new BooksListAdapter(this, R.layout.books_list_item, cursor);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(mAdapter);
        list.setOnItemLongClickListener(mAdapter);

        // if (!CheckUtil.checkAvailiable()) {
        // showDialog(EXPIRED_DIALOG);
        // }

        SharedPreferences pref = getSharedPreferences(Constatnts.PREF_TAG,
                Context.MODE_PRIVATE);
        sortMode = pref.getInt("SORT_MODE", SORT_NAME);

        mFilterText = (EditText) findViewById(R.id.search);
        mFilterText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable editable) {
                mAdapter.getFilter().filter(editable.toString());
            }

            public void beforeTextChanged(CharSequence charsequence, int i,
                    int j, int k) {
            }

            public void onTextChanged(CharSequence charsequence, int i, int j,
                    int k) {
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences pref = getSharedPreferences(Constatnts.PREF_TAG,
                Context.MODE_PRIVATE);
        Editor edit = pref.edit();
        edit.putInt("SORT_MODE", SORT_NAME);
        edit.commit();
    }

    public class BooksListAdapter extends ResourceCursorAdapter implements
            OnItemClickListener, OnItemLongClickListener {

        public BooksListAdapter(Context context, int resource, Cursor cursor) {
            super(context, resource, cursor);
        }



        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            boolean favorite = cursor.getInt(cursor
                    .getColumnIndexOrThrow(BookColumn.RATING)) > 0;

            TextView bookText = (TextView) view.findViewById(R.id.book_name);
            int nameIndex = cursor.getColumnIndexOrThrow(BookColumn.NAME);
            String name = cursor.getString(nameIndex);

            if (favorite) {
                bookText.setTextColor(Color.YELLOW);
            } else {
                bookText.setTextColor(Color.WHITE);
            }


            TextView authorText = (TextView) view
                    .findViewById(R.id.book_author);
            int authorIndex = cursor.getColumnIndexOrThrow(BookColumn.AUTHOR);
            String author = cursor.getString(authorIndex);


            TextView pathText = (TextView) view.findViewById(R.id.file_path);
            int pathIndex = cursor.getColumnIndexOrThrow(BookColumn.PATH);
            pathText
                    .setText(cursor.getString(pathIndex).replace("/sdcard", ""));



            CharSequence input = mFilterText.getText();

            String inputLowerCase = input.toString().toLowerCase();
            // Name
            int length = input.length();
            if (name != null && length > 0) {
                String nameLowerCase = name.toLowerCase();
                SpannableString displayString = new SpannableString(name);
                int c = 0;
                if ((c = nameLowerCase.indexOf(inputLowerCase)) > -1) {
                    displayString.setSpan(new ForegroundColorSpan(0xff444444),
                            c, c + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    displayString.setSpan(new BackgroundColorSpan(0xff55fdff),
                            c, c + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }


                bookText.setText(displayString);
            } else {
                bookText.setText(name);
            }

            // author
            if (author != null && length > 0) {
                String authorLowerCase = author.toLowerCase();
                SpannableString displayString = new SpannableString(author);
                int c = 0;
                if ((c = authorLowerCase.indexOf(inputLowerCase)) > -1) {
                    displayString.setSpan(new ForegroundColorSpan(0xff444444),
                            c, c + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    displayString.setSpan(new BackgroundColorSpan(0xff55fdff),
                            c, c + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }


                authorText.setText(displayString);
            } else {
                authorText.setText(author);
            }


        }

        public long getItemId(int position) {
            Cursor cursor = getCursor();
            if (!cursor.isClosed()) {
                cursor.moveToPosition(position);
                return cursor.getLong(cursor
                        .getColumnIndexOrThrow(BookColumn._ID));
            }
            return -1;

        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if (getFilterQueryProvider() != null) {
                return getFilterQueryProvider().runQuery(constraint);
            }



            StringBuilder where = new StringBuilder();
            String keyword = mFilterText.getText().toString();
            where.append(BookColumn.NAME).append(" like '%").append(keyword)
                    .append("%' or ");
            where.append(BookColumn.AUTHOR).append(" like '%").append(keyword)
                    .append("%'");

            String orderBy = "";
            switch (sortMode) {
            case SORT_NAME:
                orderBy = BookColumn.NAME;
                break;
            case SORT_AUTHOR:
                orderBy = BookColumn.AUTHOR + " desc";
                break;
            case SORT_RECENT:
                orderBy = BookColumn.CREATE_DATE + " desc";
                break;
            case SORT_PATH:
                orderBy = BookColumn.PATH + " asc";
                break;
            }

            Cursor cursor = managedQuery(BookColumn.CONTENT_URI, BookField,
                    where.toString(), null, orderBy);
            stopManagingCursor(getCursor());


            return cursor;
        }


        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            getCursor().moveToPosition(position);
            Intent intent = new Intent();
            intent.putExtra("ID", getItemId(position));
            intent.setClassName(BookListActivity.this,
                    PalmBookReaderActivity.class.getName());
            startActivity(intent);

        }



        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                int position, long arg3) {
            selectedIndex = position;
            showDialog(MENU_DIALOG);
            return true;
        }
    }



    /* ---------------------menu---------------------------- */
    private final int MENU_SYCN = 0;
    private final int MENU_CHARSET = 1;
    private final int MENU_SORT = 2;
    private final int MENU_ABOUT = 3;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SYCN, MENU_SYCN, R.string.menu_sync);
        menu.add(0, MENU_CHARSET, MENU_CHARSET, R.string.menu_charset);
        menu.add(0, MENU_SORT, MENU_SORT, R.string.menu_sort);
        menu.add(0, MENU_ABOUT, MENU_ABOUT, R.string.menu_about);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(MENU_SYCN);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == MENU_SYCN) {
            showDialog(SYNC_DIALOG);
        } else if (item.getItemId() == MENU_CHARSET) {
            showDialog(ENCODE_DIALOG);
        } else if (item.getItemId() == MENU_SORT) {
            showDialog(SORT_DIALOG);
        } else if (item.getItemId() == MENU_ABOUT) {
            showDialog(ABOUT_DIALOG);
        }

        return true;
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case ENCODE_DIALOG:
            SharedPreferences pref = getSharedPreferences(Constatnts.PREF_TAG,
                    Context.MODE_PRIVATE);
            int charset = pref.getInt(Constatnts.DEFAULT_ENCODE, 0);

            return new AlertDialog.Builder(this).setTitle(
                    R.string.default_charset).setSingleChoiceItems(
                    R.array.charset, 0, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences pref = getSharedPreferences(
                                    Constatnts.PREF_TAG, Context.MODE_PRIVATE);
                            Editor editor = pref.edit();
                            editor.putInt(Constatnts.DEFAULT_ENCODE, which);
                            editor.commit();
                            dialog.dismiss();
                            SyncAgent agent = new SyncAgent();
                            agent.syncSD(BookListActivity.this,new File("/sdcard"));
                        }
                    }).create();

        case PROGRESS_DIALOG:
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getString(R.string.msg_sync_sd));
            dialog.setIndeterminate(true);
            dialog.setCancelable(true);
            return dialog;

        case EXPIRED_DIALOG:
            return new AlertDialog.Builder(this)

            .setMessage(R.string.msg_expired).setNegativeButton(
                    android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            dialog.dismiss();
                            finish();
                        }
                    }).create();

        case MENU_DIALOG:
            return new AlertDialog.Builder(this)

            .setItems(R.array.menu_dialog_items,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                showDialog(EDIT_DIALOG);
                                dialog.dismiss();
                            } else {
                                long id = mAdapter.getItemId(selectedIndex);
                                Uri pdbUri = Uri.parse(BookColumn.CONTENT_URI
                                        + "/" + id);
                                getContentResolver().delete(pdbUri, null, null);
                                dialog.dismiss();
                            }

                        }
                    }).create();

        case SORT_DIALOG:
            return new AlertDialog.Builder(this).setTitle(R.string.sortby)
                    .setSingleChoiceItems(R.array.sort_dialog_item, 0,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {

                                    mAdapter.getFilter().filter(null);
                                    sortMode = whichButton;
                                    dialog.dismiss();
                                }
                            }).create();

        case EDIT_DIALOG:

            final View editView = getLayoutInflater().inflate(
                    R.layout.edit_dialog, null);

            return new AlertDialog.Builder(this).setTitle(R.string.dialog_edit)
                    .setView(editView).setCancelable(false).setPositiveButton(
                            android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    final EditText editName = (EditText) editView
                                            .findViewById(R.id.edit_name);
                                    final EditText editAuthor = (EditText) editView
                                            .findViewById(R.id.edit_author);
                                    final CheckBox checkView = (CheckBox) editView
                                            .findViewById(R.id.check_favorite);
                                    final Cursor cursor = (Cursor) mAdapter
                                            .getItem(selectedIndex);
                                    final int bookId = cursor.getInt(cursor
                                            .getColumnIndex(BookColumn._ID));

                                    Uri pdbUri = Uri
                                            .parse(BookColumn.CONTENT_URI + "/"
                                                    + bookId);
                                    ContentValues values = new ContentValues();
                                    values.put(BookColumn.NAME, editName
                                            .getText().toString().trim());
                                    values.put(BookColumn.AUTHOR, editAuthor
                                            .getText().toString().trim());
                                    values.put(BookColumn.RATING, checkView
                                            .isChecked() ? 1 : 0);


                                    getContentResolver().update(pdbUri, values,
                                            null, null);

                                }
                            }).create();

        case ABOUT_DIALOG:
            View aboutView = getLayoutInflater().inflate(R.layout.about, null);
            final TextView messageView = (TextView)aboutView.findViewById(R.id.message);
            messageView.setTextSize(14);
            StringBuilder msg = new StringBuilder();
            msg.append("<center>");
            msg.append("<h1>").append(getString(R.string.app_name)).append(" </h2><br/>");
            msg.append("<h2><a href=\"mailto:chihhsiang.li@gmail.com\">Lee Szu-Hsien</a></h3> <br/>");
            msg.append("<h3><a href=\"http://androidlife.blogspot.com\">androidlife.blogspot.com</a></h3>");
            msg.append("</center>");



            
            messageView.setText(Html.fromHtml(msg.toString()));

            messageView.setMovementMethod(LinkMovementMethod.getInstance());
            return new AlertDialog.Builder(this)
                    .setView(aboutView)
                    .setNeutralButton(android.R.string.ok, null).create();

            
        case SYNC_DIALOG:
            return new AlertDialog.Builder(this).setTitle(R.string.sortby)
                    .setItems(R.array.menu_sync_items,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    File f;
                                    if(whichButton<2){
                                        if(whichButton==0){
                                            f = new File("/sdcard");
                                        }else{
                                            f = new File("/sdcard/ebooks");
                                        }
                                        if(f.exists()){
                                            showDialog(PROGRESS_DIALOG);
                                            new Thread() {
                                                public void run() {
                                                    SyncAgent syncAgent = new SyncAgent();
                                                    syncAgent.syncSD(BookListActivity.this,new File("/sdcard"));
                                                    dismissDialog(PROGRESS_DIALOG);
                                                }
                                            }.start();
                                        }else{
                                            Toast.makeText(BookListActivity.this, R.string.msg_need_folder, 5000).show();
                                        }
                                    }else{
                                        DBUtil.clearAllBooks(BookListActivity.this); 
                                    }
                                   
                                   
                                    dialog.dismiss();
                                }
                            }).create();
            
            
            
        }
        return null;
    }

    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        case EDIT_DIALOG:
            final EditText editName = (EditText) dialog
                    .findViewById(R.id.edit_name);
            final EditText editAuthor = (EditText) dialog
                    .findViewById(R.id.edit_author);
            final CheckBox checkView = (CheckBox) dialog
                    .findViewById(R.id.check_favorite);
            final Cursor cursor = (Cursor) mAdapter.getItem(selectedIndex);
            final int bookId = cursor.getInt(cursor
                    .getColumnIndex(BookColumn._ID));
            editName.setText(cursor.getString(cursor
                    .getColumnIndex(BookColumn.NAME)));
            editAuthor.setText(cursor.getString(cursor
                    .getColumnIndex(BookColumn.AUTHOR)));
            checkView.setChecked(cursor.getInt(cursor
                    .getColumnIndex(BookColumn.RATING)) > 0);


            break;
        }

    }



}
