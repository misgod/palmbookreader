package com.android.lee.pdbreader.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.android.lee.pdbreader.provider.BookColumn;

import java.io.File;

public class DBUtil {


    public static void clearFileNoFound(Context context) {
        Cursor cursor = context.getContentResolver().query(
                BookColumn.CONTENT_URI,
                new String[] {BookColumn._ID, BookColumn.PATH}, null, null,
                null);
        cursor.deactivate();
        while (cursor.moveToNext()) {
            String path = cursor.getString(cursor
                    .getColumnIndexOrThrow(BookColumn.PATH));
            File file = new File(path);
            if (!file.exists()) {
                String id = cursor.getString(cursor
                        .getColumnIndexOrThrow(BookColumn._ID));
                Uri delUri = Uri.parse(BookColumn.CONTENT_URI + "/" + id);
                context.getContentResolver().delete(delUri, null, null);
            }
        }

        cursor.close();

    }


    public static boolean isExits(Context context, File path) {
        String pathOption = BookColumn.PATH + " = '" + path.getAbsolutePath()
                + "'";
        Cursor cursor = context.getContentResolver().query(
                BookColumn.CONTENT_URI, new String[] {BookColumn._ID},
                pathOption, null, null);
        boolean result = cursor.getCount() > 0;
        cursor.close();
        return result;
    }
}
