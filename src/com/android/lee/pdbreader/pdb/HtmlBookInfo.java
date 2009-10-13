package com.android.lee.pdbreader.pdb;

import android.text.Html;
import android.text.Spannable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.DataFormatException;

public class HtmlBookInfo extends TxtBookInfo {
    public HtmlBookInfo(long id) {
        super(id);
    }



    public CharSequence getText() throws IOException {
        return Html.fromHtml(super.getText().toString());
     

    }

    @Override
    public boolean supportFormat() {
        return false;
    }

}
