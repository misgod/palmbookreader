package com.misgod.pdbreader.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.misgod.pdbreader.R;
import com.misgod.pdbreader.pdb.AbstractBookInfo;
import com.misgod.pdbreader.provider.BookColumn;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;

public class SyncAgent {
	private static final String TAG = "SyncAgent";
	private ArrayList<File> pdbFileList = new ArrayList<File>();

	public void syncSD(Context context, File path, boolean otherType) {
		scanFile(path, otherType);
		SharedPreferences pref = context.getSharedPreferences(
				Constatnts.PREF_TAG, Context.MODE_PRIVATE);
		int charset = pref.getInt(Constatnts.DEFAULT_ENCODE, 0);

		String encode = context.getResources().getStringArray(R.array.charset)[charset];
		for (File f : pdbFileList) {
			try {
				if (DBUtil.isExits(context, f)) {
					continue;
				}
				AbstractBookInfo book = AbstractBookInfo.newBookInfo(f, -1);
				book.setEncode(encode);

				try {
					try {
						if (f.getName().toLowerCase().endsWith(".txt")) {
							String guessCharset = ConvertUtil.guessCharset(f
									.getAbsolutePath());
							if (guessCharset != null) {
								book.setEncode(guessCharset);
							}
						}
					} catch (IOException e) {
						Log.d(TAG, e.getMessage(), e);
						// skip
					}

					book.setFile(f, true);
					ContentValues values = new ContentValues();
					values.put(BookColumn.NAME, book.mName);
					values.put(BookColumn.PATH, f.getAbsolutePath());
					values.put(BookColumn.ENDCODE, book.mEncode);
					values.put(BookColumn.FORMAT, book.mFormat);
					context.getContentResolver().insert(BookColumn.CONTENT_URI,
							values);
				} catch (IOException e) {
					Log.d(TAG, e.getMessage(), e);
					// skip
				}
			} catch (RuntimeException e) {
				Log.e(TAG, e.getMessage(), e);
				// ignore...
			}
		}
		DBUtil.clearFileNoFound(context);
	}

	private void scanFile(File dir, final boolean otherType) {

		File[] pdbList = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				try {
					if (otherType) {
						return file.isDirectory()
								|| file.getName().toLowerCase()
										.endsWith(".pdb")
								|| file.getName().toLowerCase()
										.endsWith(".txt")
								|| file.getName().toLowerCase()
										.endsWith(".updb");
						// || file.getName().toLowerCase().endsWith(".htm")
						// || file.getName().toLowerCase().endsWith(".html");
					} else {
						return file.isDirectory()
								|| file.getName().toLowerCase()
										.endsWith(".pdb")
								|| file.getName().toLowerCase()
										.endsWith(".txt")		
								|| file.getName().toLowerCase()
										.endsWith(".updb");
					}

				} catch (RuntimeException e) {
					Log.e(TAG, e.getMessage(), e);
					return false;
				}
			}
		});

		if (pdbList != null) {
			for (File p : pdbList) {
				if (p.isDirectory()) {
					scanFile(p, otherType);
				} else {
					pdbFileList.add(p);
				}
			}

		}

	}
}
