package com.jamdeo.tv.provider;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class AppsTable {

    // Database table
    public static final String TABLE_NAME = "apps";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PACKAGE = "package";
    public static final String COLUMN_VENDOR = "vendor";
    public static final String COLUMN_DESCRIPTION = "description";

    // Database creation SQL statement
    private static final String DATABASE_CREATE = "create table " 
            + TABLE_NAME
            + "(" 
            + COLUMN_ID + " integer primary key autoincrement, " 
            + COLUMN_NAME + " text not null, " 
            + COLUMN_PACKAGE + " text not null, " 
            + COLUMN_VENDOR + " text not null, " 
            + COLUMN_DESCRIPTION + " text not null" 
            + ");";

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
            int newVersion) {
        Log.w(AppsTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(database);
    }
}