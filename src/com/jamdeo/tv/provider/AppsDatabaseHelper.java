package com.jamdeo.tv.provider;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;

public class AppsDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "appstable.db";
    private static final int DATABASE_VERSION = 1;

    public AppsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        AppsTable.onCreate(database);
    }

    // Method is called during an upgrade of the database,
    // e.g. if you increase the database version
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion,
            int newVersion) {
        AppsTable.onUpgrade(database, oldVersion, newVersion);
    }
}