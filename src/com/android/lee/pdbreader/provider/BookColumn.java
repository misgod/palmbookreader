package com.android.lee.pdbreader.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public final class BookColumn implements BaseColumns {
    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI = Uri
            .parse("content://PilotBookProvider/books");


    public static final String NAME = "name";
    public static final String AUTHOR = "author";
    public static final String PATH = "path";
    public static final String LAST_PAGE = "lastpage";
    public static final String LAST_OFFSET = "lastoffset";
    public static final String RATING = "rating"; //use for top 1:top 0:normal
    public static final String ENDCODE = "encode";
    public static final String REPLACE = "replace"; 
    public static final String CREATE_DATE = "createdate"; //use for recently read
    public static final String FORMAT = "format";
}
