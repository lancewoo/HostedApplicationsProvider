/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jamdeo.tv.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * This class tests the content provider for the HostedApplicationsProvider application.
 *
 * To learn how to run an entire test package or one of its classes, please see
 * "Testing in Eclipse, with ADT" or "Testing in Other IDEs" in the Developer Guide.
 *
 * Run the test like this:
 * <code>
 * adb shell am instrument -e class com.jamdeo.tv.provider.HostedApplicationsProviderTest \
 *         -w com.jamdeo.tv.provider.tests/android.test.InstrumentationTestRunner
 * </code>
 */
public class HostedApplicationsProviderTest extends ProviderTestCase2<HostedApplicationsProvider> {
    private static final String TAG = "HostedApplicationsProviderTest";
    // A URI that the provider does not offer, for testing error handling.
    private static final Uri INVALID_URI =
        Uri.withAppendedPath(HostedApplicationsProvider.CONTENT_URI, "invalid");

    // Contains a reference to the mocked content resolver for the provider under test.
    private MockContentResolver mMockResolver;

    // Contains an SQLite database, used as test data
    private SQLiteDatabase mDb;

    // Contains the test data, as an array of AppInfo instances.
    private final AppInfo[] TEST_APPS = {
        new AppInfo("App0", "com.hisense.app.0", "This is app 0", "hisense"),
        new AppInfo("App1", "com.hisense.app.1", "This is app 1", "hisense"),
        new AppInfo("App2", "com.hisense.app.2", "This is app 2", "hisense"),
        new AppInfo("App3", "com.hisense.app.3", "This is app 3", "hisense"),
        new AppInfo("App4", "com.hisense.app.4", "This is app 4", "hisense"),
        new AppInfo("App5", "com.hisense.app.5", "This is app 5", "hisense"),
        new AppInfo("App6", "com.hisense.app.6", "This is app 6", "hisense"),
        new AppInfo("App7", "com.hisense.app.7", "This is app 7", "hisense"),
        new AppInfo("App8", "com.hisense.app.8", "This is app 8", "hisense"),
        new AppInfo("App9", "com.hisense.app.9", "This is app 9", "hisense") };

    private final static String TEST_PKG_NAME = "com.hisense.app";

    // A utility for converting data to a ContentValues map.
    private static class AppInfo {
        String name;
        String pkg;
        String desc;
        String vendor;

        public AppInfo(String name, String pkg, String description, String vendor) {
            this.name = name;
            this.pkg = pkg;
            this.desc = description;
            this.vendor = vendor;
        }

        public ContentValues getContentValues() {
            // Gets a new ContentValues object
            ContentValues v = new ContentValues();

            // Adds map entries for the user-controlled fields in the map
            v.put(AppsTable.COLUMN_NAME, name);
            v.put(AppsTable.COLUMN_PACKAGE, pkg);
            v.put(AppsTable.COLUMN_VENDOR, vendor);
            v.put(AppsTable.COLUMN_DESCRIPTION, desc);
            return v;
        }
    }

    /*
     * Constructor for the test case class.
     * Calls the super constructor with the class name of the provider under test and the
     * authority name of the provider.
     */
    public HostedApplicationsProviderTest() {
        super(HostedApplicationsProvider.class, "com.jamdeo.tv.provider.hostedapps");
    }

    /*
     * Sets up the test environment before each test method. Creates a mock content resolver,
     * gets the provider under test, and creates a new database for the provider.
     */
    @Override
    protected void setUp() throws Exception {
        // Calls the base class implementation of this method.
        super.setUp();

        // Gets the resolver for this test.
        mMockResolver = getMockContentResolver();

        /*
         * Gets a handle to the database underlying the provider. Gets the provider instance
         * created in super.setUp(), gets the DatabaseOpenHelper for the provider, and gets
         * a database object from the helper.
         */
        mDb = getProvider().getOpenHelperForTest().getWritableDatabase();
    }

    /*
     *  This method is called after each test method, to clean up the current fixture. Since
     *  this sample test case runs in an isolated context, no cleanup is necessary.
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Sets up test data.
     * The test data is in an SQL database. It is created in setUp() without any data,
     * and populated in insertData if necessary.
     */
    private void insertData() {
        // Creates an instance of the ContentValues map type expected by database insertions
        ContentValues values = new ContentValues();

        // Sets up test data
        for (int index = 0; index < TEST_APPS.length; index++) {
            // Adds a record to the database.
            mDb.insertOrThrow(
                AppsTable.TABLE_NAME,             // the table name for the insert
                null,      // column set to null if empty values map
                TEST_APPS[index].getContentValues()  // the values map to insert
            );
        }
    }

    /*
     * Tests the provider's publicly available URIs. If the URI is not one that the provider
     * understands, the provider should throw an exception. It also tests the provider's getType()
     * method for each URI, which should return the MIME type associated with the URI.
     */
    public void testUriAndGetType() {
        // Tests the MIME type for the table URI.
        String mimeType = mMockResolver.getType(HostedApplicationsProvider.CONTENT_URI);
        assertEquals(HostedApplicationsProvider.CONTENT_TYPE, mimeType);

        // Creates a URI with a pattern for app ids. The id doesn't have to exist.
        Uri appIdUri = ContentUris.withAppendedId(HostedApplicationsProvider.CONTENT_URI, 1);

        // Gets the ID URI MIME type.
        mimeType = mMockResolver.getType(appIdUri);
        assertEquals(HostedApplicationsProvider.CONTENT_ITEM_TYPE, mimeType);

        // Tests an invalid URI. This should throw an IllegalArgumentException.
        mimeType = mMockResolver.getType(INVALID_URI);
    }


    /*
     * Tests the provider's public API for querying data in the table, using the URI for
     * a dataset of records.
     */
    public void testQueriesOnAppsUri() {
        // Defines a projection of column names to return for a query
        final String[] TEST_PROJECTION = {
            AppsTable.COLUMN_NAME,
            AppsTable.COLUMN_PACKAGE,
            AppsTable.COLUMN_VENDOR
        };

        // Defines a selection column for the query. When the selection columns are passed
        // to the query, the selection arguments replace the placeholders.
        final String NAME_SELECTION = AppsTable.COLUMN_NAME + " = " + "?";

        // Defines the selection columns for a query.
        final String SELECTION_COLUMNS =
            NAME_SELECTION + " OR " + NAME_SELECTION + " OR " + NAME_SELECTION;

         // Defines the arguments for the selection columns.
        final String[] SELECTION_ARGS = { "App0", "App2", "App5" };

         // Defines a query sort order
        final String SORT_ORDER = AppsTable.COLUMN_NAME+ " ASC";

        // Query subtest 1.
        // If there are no records in the table, the returned cursor from a query should be empty.
        Cursor cursor = mMockResolver.query(
            HostedApplicationsProvider.CONTENT_URI,  // the URI for the main data table
            null,                       // no projection, get all columns
            null,                       // no selection criteria, get all records
            null,                       // no selection arguments
            null                        // use default sort order
        );

         // Asserts that the returned cursor contains no records
        assertEquals(0, cursor.getCount());

         // Query subtest 2.
         // If the table contains records, the returned cursor from a query should contain records.

        // Inserts the test data into the provider's underlying data source
        insertData();

        // Gets all the columns for all the rows in the table
        cursor = mMockResolver.query(
            HostedApplicationsProvider.CONTENT_URI,  // the URI for the main data table
            null,                       // no projection, get all columns
            null,                       // no selection criteria, get all records
            null,                       // no selection arguments
            null                        // use default sort order
        );

        // Asserts that the returned cursor contains the same number of rows as the size of the
        // test data array.
        assertEquals(TEST_APPS.length, cursor.getCount());

        // Query subtest 3.
        // A query that uses a projection should return a cursor with the same number of columns
        // as the projection, with the same names, in the same order.
        Cursor projectionCursor = mMockResolver.query(
              HostedApplicationsProvider.CONTENT_URI,  // the URI for the main data table
              TEST_PROJECTION,            // get the name, package, and vendor columns
              null,                       // no selection columns, get all the records
              null,                       // no selection criteria
              null                        // use default the sort order
        );

        // Asserts that the number of columns in the cursor is the same as in the projection
        assertEquals(TEST_PROJECTION.length, projectionCursor.getColumnCount());

        // Asserts that the names of the columns in the cursor and in the projection are the same.
        // This also verifies that the names are in the same order.
        assertEquals(TEST_PROJECTION[0], projectionCursor.getColumnName(0));
        assertEquals(TEST_PROJECTION[1], projectionCursor.getColumnName(1));
        assertEquals(TEST_PROJECTION[2], projectionCursor.getColumnName(2));

        // Query subtest 4
        // A query that uses selection criteria should return only those rows that match the
        // criteria. Use a projection so that it's easy to get the data in a particular column.
        projectionCursor = mMockResolver.query(
            HostedApplicationsProvider.CONTENT_URI, // the URI for the main data table
            TEST_PROJECTION,           // get the name, package, vendor columns
            SELECTION_COLUMNS,         // select on the name column
            SELECTION_ARGS,            // select names "App0", "App2", or "App5"
            SORT_ORDER                 // sort ascending on the name column
        );

        // Asserts that the cursor has the same number of rows as the number of selection arguments
        assertEquals(SELECTION_ARGS.length, projectionCursor.getCount());

        int index = 0;

        while (projectionCursor.moveToNext()) {

            // Asserts that the selection argument at the current index matches the value of
            // the name column (column 0) in the current record of the cursor
            assertEquals(SELECTION_ARGS[index], projectionCursor.getString(0));

            index++;
        }

        // Asserts that the index pointer is now the same as the number of selection arguments, so
        // that the number of arguments tested is exactly the same as the number of rows returned.
        assertEquals(SELECTION_ARGS.length, index);

    }

    /*
     * Tests queries against the provider, using the app id URI. This URI encodes a single
     * record ID. The provider should only return 0 or 1 record.
     */
    public void testQueriesOnAppIdUri() {
      // Defines the selection column for a query. The "?" is replaced by entries in the
      // selection argument array
      final String SELECTION_COLUMNS = AppsTable.COLUMN_NAME + " = " + "?";

      // Defines the argument for the selection column.
      final String[] SELECTION_ARGS = { "App1" };

      // A sort order for the query.
      final String SORT_ORDER = AppsTable.COLUMN_NAME + " ASC";

      // Creates a projection includes the app id column, so that app id can be retrieved.
      final String[] APP_ID_PROJECTION = {
           AppsTable.COLUMN_ID,
           AppsTable.COLUMN_NAME};

      // Query subtest 1.
      // Tests that a query against an empty table returns null.

      // Constructs a URI that matches the provider's id URI pattern, using an arbitrary
      // value of 1 as the ID.
      Uri appIdUri = ContentUris.withAppendedId(HostedApplicationsProvider.CONTENT_ID_URI_BASE, 1);

      // Queries the table with the ID URI. This should return an empty cursor.
      Cursor cursor = mMockResolver.query(
          appIdUri, // URI pointing to a single record
          null,      // no projection, get all the columns for each record
          null,      // no selection criteria, get all the records in the table
          null,      // no need for selection arguments
          null       // default sort, by ascending 
      );

      // Asserts that the cursor is null.
      assertEquals(0,cursor.getCount());

      // Query subtest 2.
      // Tests that a query against a table containing records returns a single record whose ID
      // is the one requested in the URI provided.

      // Inserts the test data into the provider's underlying data source.
      insertData();

      // Queries the table using the URI for the full table.
      cursor = mMockResolver.query(
          HostedApplicationsProvider.CONTENT_URI, // the base URI for the table
          APP_ID_PROJECTION,        // returns the ID and name columns of rows
          SELECTION_COLUMNS,         // select based on the name column
          SELECTION_ARGS,            // select name of "App1"
          SORT_ORDER                 // sort order returned is by name, ascending
      );

      // Asserts that the cursor contains only one row.
      assertEquals(1, cursor.getCount());

      // Moves to the cursor's first row, and asserts that this did not fail.
      assertTrue(cursor.moveToFirst());

      // Saves the record's ID.
      int inputAppId = cursor.getInt(0);

      // Builds a URI based on the provider's content ID URI base and the saved ID.
      appIdUri = ContentUris.withAppendedId(HostedApplicationsProvider.CONTENT_ID_URI_BASE, inputAppId);

      // Queries the table using the content ID URI, which returns a single record with the
      // specified app ID, matching the selection criteria provided.
      cursor = mMockResolver.query(appIdUri, // the URI for a single record
          APP_ID_PROJECTION,                 // same projection, get ID and name columns
          SELECTION_COLUMNS,                  // same selection, based on name column
          SELECTION_ARGS,                     // same selection arguments, name = "App1"
          SORT_ORDER                          // same sort order returned, by name, ascending
      );

      // Asserts that the cursor contains only one row.
      assertEquals(1, cursor.getCount());

      // Moves to the cursor's first row, and asserts that this did not fail.
      assertTrue(cursor.moveToFirst());

      // Asserts that the app ID passed to the provider is the same as the ID returned.
      assertEquals(inputAppId, cursor.getInt(0));
    }

    /*
     *  Tests inserts into the data model.
     */
    public void testInserts() {
        AppInfo ai = new AppInfo("AppName", "com.hisense.app", "Desc", "hisense");

        // Insert subtest 1.
        // Inserts a row using the new AppInfo instance.
        // No assertion will be done. The insert() method either works or throws an Exception
        Uri rowUri = mMockResolver.insert(
            HostedApplicationsProvider.CONTENT_URI,  // the main table URI
            ai.getContentValues()     // the map of values to insert as a new record
        );

        // Parses the returned URI to get the app ID of the new app. The ID is used in subtest 2.
        long appId = ContentUris.parseId(rowUri);

        // Does a full query on the table. Since insertData() hasn't yet been called, the
        // table should only contain the record just inserted.
        Cursor cursor = mMockResolver.query(
            HostedApplicationsProvider.CONTENT_URI, // the main table URI
            null,                      // no projection, return all the columns
            null,                      // no selection criteria, return all the rows in the model
            null,                      // no selection arguments
            null                       // default sort order
        );

        // Asserts that there should be only 1 record.
        assertEquals(1, cursor.getCount());

        // Moves to the first (and only) record in the cursor and asserts that this worked.
        assertTrue(cursor.moveToFirst());

        // Since no projection was used, get the column indexes of the returned columns
        int nameIndex = cursor.getColumnIndex(AppsTable.COLUMN_NAME);
        int packageIndex = cursor.getColumnIndex(AppsTable.COLUMN_PACKAGE);
        int descIndex = cursor.getColumnIndex(AppsTable.COLUMN_DESCRIPTION);
        int vendorIndex = cursor.getColumnIndex(AppsTable.COLUMN_VENDOR);

        // Tests each column in the returned cursor against the data that was inserted, comparing
        // the field in the AppInfo object to the data at the column index in the cursor.
        assertEquals(ai.name, cursor.getString(nameIndex));
        assertEquals(ai.pkg, cursor.getString(packageIndex));
        assertEquals(ai.desc, cursor.getString(descIndex));
        assertEquals(ai.vendor, cursor.getString(vendorIndex));

        // Insert subtest 2.
        // Tests that we can't insert a record whose id value already exists.

        // Defines a ContentValues object so that the test can add an ID to it.
        ContentValues values = ai.getContentValues();

        // Adds the app ID retrieved in subtest 1 to the ContentValues object.
        values.put(AppsTable.COLUMN_ID, (int) appId);

        // Tries to insert this record into the table. This should fail and drop into the
        // catch block. If it succeeds, issue a failure message.
        try {
            rowUri = mMockResolver.insert(HostedApplicationsProvider.CONTENT_URI, values);
            fail("Expected insert failure for existing record but insert succeeded.");
        } catch (Exception e) {
            // succeeded, so do nothing.
            Log.d(TAG, "Exception expected: " + e.toString());
        }
    }

    /*
     * Tests deletions from the data model.
     */
    public void testDeletes() {
        // Subtest 1.
        // Tries to delete a record from a data model that is empty.

        // Sets the selection column to "package"
        final String SELECTION_COLUMNS = AppsTable.COLUMN_PACKAGE + " = " + "?";

        // Sets the selection argument 
        final String[] SELECTION_ARGS = { "com.hisense.app.0" };

        // Tries to delete rows matching the selection criteria from the data model.
        int rowsDeleted = mMockResolver.delete(
            HostedApplicationsProvider.CONTENT_URI, // the base URI of the table
            SELECTION_COLUMNS,
            SELECTION_ARGS
        );

        // Assert that the deletion did not work. The number of deleted rows should be zero.
        assertEquals(0, rowsDeleted);

        // Subtest 2.
        // Tries to delete an existing record. Repeats the previous subtest, but inserts data first.

        // Inserts data into the model.
        insertData();

        // Uses the same parameters to try to delete the row
        rowsDeleted = mMockResolver.delete(
            HostedApplicationsProvider.CONTENT_URI, // the base URI of the table
            SELECTION_COLUMNS,
            SELECTION_ARGS
        );

        // The number of deleted rows should be 1.
        assertEquals(1, rowsDeleted);

        // Tests that the record no longer exists. Tries to get it from the table, and
        // asserts that nothing was returned.

        // Queries the table with the same selection column and argument used to delete the row.
        Cursor cursor = mMockResolver.query(
            HostedApplicationsProvider.CONTENT_URI, // the base URI of the table
            null,                      // no projection, return all columns
            SELECTION_COLUMNS,
            SELECTION_ARGS,
            null                       // use the default sort order
        );

        // Asserts that the cursor is empty since the record had already been deleted.
        assertEquals(0, cursor.getCount());
    }

    /*
     * Tests updates to the data model.
     */
    public void testUpdates() {
        // Selection column for identifying a record in the data model.
        final String SELECTION_COLUMNS = AppsTable.COLUMN_PACKAGE + " = " + "?";

        // Selection argument for the selection column.
        final String[] SELECTION_ARGS = { "com.hisense.app.0" };
        final String UPDATED_VALUE = "Testing an update with this string";

        // Defines a map of column names and values
        ContentValues values = new ContentValues();

        // Subtest 1.
        // Tries to update a record in an empty table.

        // Sets up the update by putting the column and a value into the values map.
        values.put(AppsTable.COLUMN_NAME, UPDATED_VALUE);

        // Tries to update the table
        int rowsUpdated = mMockResolver.update(
            HostedApplicationsProvider.CONTENT_URI,  // the URI of the data table
            values,                     // a map of the updates to do
            SELECTION_COLUMNS,           // select based on the package column
            SELECTION_ARGS               // select "package = com.hisense.app.0"
        );

        // Asserts that no rows were updated.
        assertEquals(0, rowsUpdated);

        // Subtest 2.
        // Builds the table, and then tries the update again using the same arguments.

        // Inserts data into the model.
        insertData();

        //  Does the update again, using the same arguments as in subtest 1.
        rowsUpdated = mMockResolver.update(
            HostedApplicationsProvider.CONTENT_URI,   // The URI of the data table
            values,                      // the same map of updates
            SELECTION_COLUMNS,            // same selection, based on the package column
            SELECTION_ARGS                // same selection argument, to select "package = com.hisense.app.0"
        );

        // Asserts that only one row was updated. The selection criteria evaluated to
        // "package = com.hisense.app.0", and the test data should only contain one row that matches that.
        assertEquals(1, rowsUpdated);

        // query out the updatd column to ensure the updatd row was correct
        Cursor cursor = mMockResolver.query(
            HostedApplicationsProvider.CONTENT_URI, // the base URI of the table
            null,                      // no projection, return all columns
            SELECTION_COLUMNS,
            SELECTION_ARGS,
            null                       // use the default sort order
        );

        Log.d(TAG, "dumpCursorToString=" + DatabaseUtils.dumpCursorToString(cursor));
        // Asserts that the cursor contains only one row.
        assertEquals(1, cursor.getCount());
        int nameIndex = cursor.getColumnIndex(AppsTable.COLUMN_NAME);
        cursor.moveToFirst();
        assertEquals(UPDATED_VALUE, cursor.getString(nameIndex));
    }

}
