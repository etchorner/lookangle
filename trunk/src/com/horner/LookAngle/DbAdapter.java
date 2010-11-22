package com.horner.LookAngle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter {

	// CONSTANTS
	@SuppressWarnings("unused")
	private static final String TAG = "DbAdapter";
	public static final String KEY_ID = "_id";
	public static final String KEY_NORAD_NBR = "norad_nbr";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_NAME = "name";
	private static final String DATABASE_NAME = "satellites";
	private static final String DATABASE_TABLE = "tblsatellites";
	private static final int DATABASE_VERSION = 7;
	private static final String DATABASE_CREATE = "CREATE TABLE "
			+ DATABASE_TABLE
			+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, norad_nbr INTEGER, inclination REAL, longitude REAL, name TEXT);";

	// ATTRIBUTES

	/** {@link DatabaseHelper} object to abstract the SQLite transactions */
	private DatabaseHelper mDbHelper;
	/**
	 * Handle to the database in use, provided by the {@link DatabaseHelper}
	 * abstraction.
	 */
	private SQLiteDatabase mDb;
	/** {@link Context} object for method calls. */
	private final Context mCtx;

	/**
	 * Private helper class to manage data abstraction through the
	 * {@link SQLiteOpenHelper}. Provides the ability to create and upgrade the
	 * databases.
	 * 
	 * @author etchorner
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private final Context mCtx;

		/**
		 * Create a helper object to manage database abstraction
		 * 
		 * @param context
		 *            the {@link Context} of the owner calling this c'tor.
		 */
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			this.mCtx = context;
		}

		/**
		 * Triggered when the database is created for the very first time. This
		 * implementation not only creates the db, but also populates it using
		 * CSV data in the res/raw directory.
		 * 
		 * @param db
		 *            the {@link SQLiteDatabase} being created.
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			// LOCALS
			String line;
			String insertStatement;

			// Make the db...
			// TODO: add other fields into the database
			db.execSQL(DATABASE_CREATE);

			// fill it up...
			InputStream inStream = mCtx.getResources().openRawResource(
					R.raw.ephemeris);
			BufferedReader buf = new BufferedReader(new InputStreamReader(
					inStream));

			try {
				// read the R.raw.ephemeris data line by line and insert
				while ((line = buf.readLine()) != null) {
					String[] s = line.split(",");
					// TODO: add other fields into the database
					insertStatement = "INSERT INTO " + DATABASE_TABLE + " ("
							+ KEY_NAME + "," + KEY_NORAD_NBR + ","
							+ KEY_LONGITUDE + ") VALUES ('" + s[0] + "', "
							+ s[1] + ", " + s[2] + ");";
					db.execSQL(insertStatement);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		/**
		 * Triggered when the APK contains a newer database as indicated in the
		 * {@link DbAdapter#DATABASE_VERSION DATABASE_VERSION} constant
		 * attribute. The existing db is destroyed and recreated (via the
		 * {@link DatabaseHelper#onCreate(SQLiteDatabase) onCreate()} call.
		 * 
		 * @param db
		 *            the database object being manipulated.
		 * @param oldVersion
		 *            the db version currently on the device to be replaced.
		 * @param newVersion
		 *            the db version to replace the current version db.
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}
	}

	/**
	 * Default constructor for a {@link DbAdapter}, nothing else is needed.
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public DbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the satellite database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public DbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	/**
	 * Closes the database.
	 */
	public void close() {
		mDbHelper.close();
	}

	/**
	 * Return a Cursor over the list of all satellites in the database
	 * 
	 * @return Cursor over all notes
	 */
	public Cursor fetchAllSatellites() {

		return mDb.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_NAME },
				null, null, null, null, KEY_NAME);
	}

	/**
	 * Return a Cursor positioned at the satellite that matches the given rowId
	 * 
	 * @param rowId
	 *            id of note to retrieve
	 * @return Cursor positioned to matching note, if found
	 * @throws SQLException
	 *             if note could not be found/retrieved
	 */
	public Cursor fetchSatellite(long rowId) throws SQLException {

		Cursor mCursor =

		mDb.query(true, DATABASE_TABLE, new String[] { KEY_ID, KEY_LONGITUDE,
				KEY_NAME, KEY_NORAD_NBR }, "_id = " + rowId, null, null, null,
				KEY_NAME, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
}