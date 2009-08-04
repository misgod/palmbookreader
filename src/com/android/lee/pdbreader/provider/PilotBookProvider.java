package com.android.lee.pdbreader.provider;



import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.HashMap;

/**
 * Provides access to a database of notes. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
public class PilotBookProvider extends ContentProvider {

    private static final String TAG = "PilotBookProvider";
    private static final String DATABASE_NAME = "pdbbooks.db";
    private static final int DATABASE_VERSION =11;
    private static final String TABLE_NAME = "books";
    private static final String AUTGIRUTIES = "PilotBookProvider";

    private static final int BOOKS =0;
    private static final int BOOK_ID =2;
    private static HashMap<String, String> sNotesProjectionMap;

    private static final UriMatcher sUriMatcher;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);

        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            StringBuilder createSql = new StringBuilder();
            createSql.append("CREATE TABLE ");
            createSql.append(TABLE_NAME ).append("(");
            createSql.append(BookColumn._ID).append(" INTEGER PRIMARY KEY," );
            createSql.append(BookColumn.NAME).append(" TEXT NOT NULL, " );
            createSql.append(BookColumn.AUTHOR).append("  TEXT, " );
            createSql.append(BookColumn.PATH).append("  TEXT, " );
            createSql.append(BookColumn.LAST_OFFSET).append(" INTEGER, ");
            createSql.append(BookColumn.LAST_PAGE).append(" INTEGER, ");
            createSql.append(BookColumn.ENDCODE).append("  TEXT, " );
            createSql.append(BookColumn.RATING).append(" INTEGER, ");
            createSql.append(BookColumn.REPLACE).append(" INTEGER, ");
            createSql.append(BookColumn.FORMAT).append(" INTEGER, ");
            createSql.append(BookColumn.CREATE_DATE).append(" LONG NOT NULL ");
            createSql.append(");");
            
            db.execSQL(createSql.toString());


            
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
        case BOOKS:
            qb.setTables(TABLE_NAME);
            qb.setProjectionMap(sNotesProjectionMap);
            break;

        case BOOK_ID:
            qb.setTables(TABLE_NAME);
            qb.setProjectionMap(sNotesProjectionMap);
            qb.appendWhere(BookColumn._ID + "=" + uri.getPathSegments().get(1));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy =BookColumn.RATING +" desc, ";
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy += BookColumn.NAME +" asc";
        } else {
            orderBy += sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return "";
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != BOOKS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = Long.valueOf(System.currentTimeMillis());

        // Make sure that the fields are all set
        if (values.containsKey(BookColumn.RATING) == false) {
            values.put(BookColumn.RATING, 0);
        }
        
        if (values.containsKey(BookColumn.ENDCODE) == false) {
            values.put(BookColumn.ENDCODE, "UTF-8");
        }
        
        if (values.containsKey(BookColumn.REPLACE) == false) {
            values.put(BookColumn.REPLACE, 0);
        }
        
        if (values.containsKey(BookColumn.FORMAT) == false) {
            values.put(BookColumn.FORMAT, "0");
        }
        
        
        if (values.containsKey(BookColumn.LAST_OFFSET) == false) {
            values.put(BookColumn.LAST_OFFSET, 0);
        }
        if (values.containsKey(BookColumn.LAST_PAGE) == false) {
            values.put(BookColumn.LAST_PAGE, 0);
        }

        if (values.containsKey(BookColumn.CREATE_DATE) == false) {
            values.put(BookColumn.CREATE_DATE, now);
        }


        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(TABLE_NAME, "", values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(BookColumn.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case BOOKS:
            count = db.delete(TABLE_NAME, where, whereArgs);
            break;

        case BOOK_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.delete(TABLE_NAME, BookColumn._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case BOOKS:
            count = db.update(TABLE_NAME, values, where, whereArgs);
            break;

        case BOOK_ID:
            String noteId = uri.getPathSegments().get(1);
            count = db.update(TABLE_NAME, values, BookColumn._ID + "=" + noteId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTGIRUTIES, "books", BOOKS);
        sUriMatcher.addURI(AUTGIRUTIES, "books/#", BOOK_ID);

        sNotesProjectionMap = new HashMap<String, String>();
        sNotesProjectionMap.put(BookColumn._ID, BookColumn._ID);
        sNotesProjectionMap.put(BookColumn.NAME, BookColumn.NAME);
        sNotesProjectionMap.put(BookColumn.AUTHOR, BookColumn.AUTHOR);
        sNotesProjectionMap.put(BookColumn.PATH, BookColumn.PATH);
        sNotesProjectionMap.put(BookColumn.LAST_OFFSET, BookColumn.LAST_OFFSET);
        sNotesProjectionMap.put(BookColumn.LAST_PAGE, BookColumn.LAST_PAGE);
        sNotesProjectionMap.put(BookColumn.ENDCODE, BookColumn.ENDCODE);
        sNotesProjectionMap.put(BookColumn.RATING, BookColumn.RATING);
        sNotesProjectionMap.put(BookColumn.REPLACE, BookColumn.REPLACE);
        sNotesProjectionMap.put(BookColumn.LAST_PAGE, BookColumn.LAST_PAGE);
        sNotesProjectionMap.put(BookColumn.CREATE_DATE, BookColumn.CREATE_DATE);
        sNotesProjectionMap.put(BookColumn.FORMAT, BookColumn.FORMAT);
        
        
    }
}