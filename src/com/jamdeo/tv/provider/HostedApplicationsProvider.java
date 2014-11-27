package com.jamdeo.tv.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

public class HostedApplicationsProvider extends ContentProvider {
    // database
    private AppsDatabaseHelper mDb;

    // used for the UriMacher
    private static final int APPS = 1;
    private static final int APP_ID = 2;
    private static final int APP_NAME = 3;
    private static final int APP_PACKAGE = 4;
    private static final int APP_DESCRIPTION = 5;
    private static final int APP_VENDOR = 6;

    private static final String AUTHORITY = "com.jamdeo.tv.provider.hostedapps";
    private static final String BASE_PATH = "hosted_apps";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
        + "/" + BASE_PATH);
    public static final Uri CONTENT_ID_URI_BASE = Uri.parse("content://" + AUTHORITY
        + "/" + BASE_PATH + "/");

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
        + "/hosted_apps";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
        + "/hosted_app";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, APPS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", APP_ID);
    }

    @Override
    public boolean onCreate() {
            mDb = new AppsDatabaseHelper(getContext());
            return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                    String sortOrder) {

        // Uisng SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // check if the caller has requested a column which does not exists
        checkColumns(projection);

        // Set the table
        queryBuilder.setTables(AppsTable.TABLE_NAME);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
        case APPS:
            break;
        case APP_ID:
            // adding the ID to the original query
            queryBuilder.appendWhere(AppsTable.COLUMN_ID + "="
                + uri.getLastPathSegment());
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = mDb.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
            selectionArgs, null, null, sortOrder);
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match) {
            case APPS:
                return CONTENT_TYPE;
            case APP_ID:
                return CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mDb.getWritableDatabase();
        int rowsDeleted = 0;
        long id = 0;
        switch (uriType) {
        case APPS:
            id = sqlDB.insertOrThrow(AppsTable.TABLE_NAME, null, values);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mDb.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType) {
        case APPS:
            rowsDeleted = sqlDB.delete(AppsTable.TABLE_NAME, selection,
                selectionArgs);
            break;
        case APP_ID:
            String id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete(AppsTable.TABLE_NAME
                    , AppsTable.COLUMN_ID + "=" + id
                    , null);
            } else {
                rowsDeleted = sqlDB.delete(AppsTable.TABLE_NAME
                    , AppsTable.COLUMN_ID + "=" + id + " and " + selection
                    , selectionArgs);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mDb.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {
        case APPS:
            rowsUpdated = sqlDB.update(AppsTable.TABLE_NAME
                , values
                , selection
                , selectionArgs);
            break;
        case APP_ID:
            String id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.update(AppsTable.TABLE_NAME
                    , values
                    , AppsTable.COLUMN_ID + "=" + id
                    , null);
            } else {
                rowsUpdated = sqlDB.update(AppsTable.TABLE_NAME
                    , values
                    , AppsTable.COLUMN_ID + "=" + id 
                    + " and " 
                    + selection
                    , selectionArgs);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(String[] projection) {
        String[] available = {
            AppsTable.COLUMN_ID
            , AppsTable.COLUMN_NAME
            , AppsTable.COLUMN_PACKAGE
            , AppsTable.COLUMN_VENDOR
            , AppsTable.COLUMN_DESCRIPTION
            };
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }

    /**
     * A test package can call this to get a handle to the database underlying the provider,
     * so it can insert test data into the database. The test case class is responsible for
     * instantiating the provider in a test context; {@link android.test.ProviderTestCase2} does
     * this during the call to setUp()
     *
     * @return a handle to the database helper object for the provider's data.
     */
    public AppsDatabaseHelper getOpenHelperForTest() {
        return mDb;
    }

}