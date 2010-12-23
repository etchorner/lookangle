package com.horner.LookAngle;

import java.text.NumberFormat;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The main {@link Activity} for the LookAngle application. The bulk of this
 * class is designed around the UI and event management. The {@link DbAdapter}
 * class handles data storage and abstraction, and the {@link SatMath} performs
 * spherical geometry and other calculations relevant to this application.
 * 
 * <ul>
 * <li>TODO: add ability to select satellite by longitude entry (save in db)
 * <BR>
 * <li>TODO: also make a "pick by map" input for the earth station location <BR>
 * <LI>TODO: pre-filter the db output to the listview so as to only display
 * visible satellites for the current position.
 * </ul>
 * 
 * @author etchorner
 * 
 *         WORKLIST:
 * 
 */
public class LookAngle extends Activity implements LocationListener,
		OnItemSelectedListener {
	// CONSTANTS
	private static final char SYM_DEGREE = '\u00b0'; // Unicode for degrees
	private static final String TAG = "LookAngle"; // for DBG logging
	private static final int OPT_DD_ID = 1; // for option menu
	private static final int OPT_DMS_ID = 2; // ...ditto/.
	// END CONSTANTS

	// ATTRIBUTES
	/** handles the listener for the GPS (or other location) service */
	private LocationManager mLocMgr;
	/** {@link Location} object for target satellite. longitude used only now. */
	private Location mSatellite;
	/** {@link Location} object for antenna location. */
	private Location mAntennaSite;
	/**
	 * {@link DbAdapter} object contains the ephemeris database handler and
	 * methods
	 */
	private DbAdapter mDbHelper;
	/** {@link TextView} handle to the longitude UI field. */
	private TextView mTxtPositionLong;
	/** {@link TextView} handle to the latitude UI field */
	private TextView mTxtPositionLat;
	/** {@link TextView} handle to the CEP (accuracy) UI field. */
	private TextView mTxtCEP;
	/** {@link TextView} handle to the azimuth UI field. */
	private TextView mTxtAzimuth;
	/** {@link TextView} handle to the elevation UI field. */
	private TextView mTxtElevation;
	/** {@link ImageView} handle to the GPS status UI icon. */
	private ImageView mImgStatusGPS;
	/** {@link ImageView} handle to the accelerometer status icon (unused). */
	@SuppressWarnings("unused")
	private ImageView mImgStatusAccel;
	/** {@link Spinner} handle to the satellite target selector. */
	private Spinner mSpnSatPicker;
	/** {@link Boolean} flag to indicate valid position data. */
	private Boolean flagGoodLocation = false;
	/** {@link Boolean} flag to indicate display mode of lat/long */
	private Boolean flagDMS = false;
	/** {@link NumberFormat} object to describe the desired numerical fmting */
	private NumberFormat mNumFmt;

	// END ATTRIBUTES

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// standard onCreate() super call and view creation.
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// instantiate the satellite location - used in onItemSelected()
		// method
		mSatellite = new Location("gps");
		mAntennaSite = new Location("gps");

		// GET HANDLES TO UI FIELDS
		mTxtPositionLat = (TextView) findViewById(R.id.txtPositionLat);
		mTxtPositionLong = (TextView) findViewById(R.id.txtPositionLong);
		mTxtCEP = (TextView) findViewById(R.id.txtCEPValue);
		mTxtAzimuth = (TextView) findViewById(R.id.txtAzimuth);
		mTxtElevation = (TextView) findViewById(R.id.txtElevation);
		mImgStatusAccel = (ImageView) findViewById(R.id.statusAccel);
		mImgStatusGPS = (ImageView) findViewById(R.id.statusGPS);
		mSpnSatPicker = (Spinner) findViewById(R.id.spnSatPicker);
		mTxtAzimuth = (TextView) findViewById(R.id.txtAzimuth);

		// get handle to db
		mDbHelper = new DbAdapter(this);

		// set number Format handler
		mNumFmt = NumberFormat.getInstance(Locale.ENGLISH);
		mNumFmt.setMinimumFractionDigits(2);
		mNumFmt.setMaximumFractionDigits(2);

		// get a location manager...only done at start to solve pause problems.
		// TODO: see Issue #2 on the project site
		// (http://code.google.com/p/lookangle/issues/detail?id=2)
		mLocMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	} // END of onCreate() method

	@Override
	public void onRestart() {
		super.onRestart();
	}

	/** Called when starting activity, also after pause and restart */
	@Override
	public void onStart() {
		// re-establish location listener after start or pause
		mLocMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

		// open/create/fill satty db and populate w/ ephemeris
		mDbHelper.open();

		// populate the satellite target selection spinner
		// with names (and _id) from the database, then set
		// a listener on it.
		fillData();
		mSpnSatPicker.setOnItemSelectedListener(this);

		super.onStart();
	}

	/** called after a pause is interrupted */
	@Override
	public void onResume() {
		// the usual override call out
		super.onResume();
	}

	/** called just before the activity is paused */
	@Override
	public void onPause() {
		// unregister the location listener while in background
		// (will be re-registered in onResume() method)
		mLocMgr.removeUpdates(this);
		mDbHelper.close();
		super.onPause();
	}

	/**
	 * Called when the app quits or is killed by the system; it's our final
	 * opportunity to clean up handles and other memory leak sources.
	 */
	@Override
	public void onDestroy() {
		mDbHelper.close();
		super.onDestroy();
	}

	/** save state in case interruption never resumes and proc is killed... */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putDouble("sat_long", mSatellite.getLongitude());
		outState.putBoolean("flag", flagGoodLocation);
		if (flagGoodLocation) {
			outState.putDouble("ant_lat", mAntennaSite.getLatitude());
			outState.putDouble("ant_long", mAntennaSite.getLongitude());
		}
		super.onSaveInstanceState(outState);
		// TODO: add state saving for pause/resume action

	}

	@Override
	public void onRestoreInstanceState(Bundle inState) {
		flagGoodLocation = inState.getBoolean("flag");
		mSatellite.setLongitude(inState.getDouble("sat_long"));
		if (flagGoodLocation) {
			mAntennaSite.setLatitude(inState.getDouble("ant_lat"));
			mAntennaSite.setLongitude(inState.getDouble("ant_long"));
			mImgStatusGPS.setImageResource(R.drawable.status_good);
		} else {
			mImgStatusGPS.setImageResource(R.drawable.status_bad);
		}
		super.onRestoreInstanceState(inState);
	}

	/**
	 * Triggered when the {@link Location} provider we are listening to becomes
	 * enabled.
	 * 
	 * @param provider
	 *            {@link Location} provider indicating status change.
	 */
	@Override
	public void onProviderEnabled(String provider) {
		mImgStatusGPS.setImageResource(R.drawable.status_good);
		flagGoodLocation = true;
	}

	/**
	 * Triggered when the {@link Location} provider we are listening to goes
	 * offline.
	 * 
	 * @param provider
	 *            {@link Location} provider indicating status change.
	 */
	@Override
	public void onProviderDisabled(String provider) {
		mImgStatusGPS.setImageResource(R.drawable.status_bad);
		flagGoodLocation = false;
	}

	/**
	 * Triggered when the {@link Location} provider indicates that the location
	 * of the phone has changed more than set during the registration for a
	 * listener in
	 * {@link android.location.LocationManager#requestLocationUpdates(String, long, float, LocationListener)
	 * requestLocationUpdates()} call.
	 */
	@Override
	public void onLocationChanged(Location location) {
		// locals
		Cursor cur; // cursor to a 1 row result based on _id
		float newLat;
		float newLong;
		String latOrdinal = "N";
		String longOrdinal = "W";

		// Store the updated antenna position data...
		mAntennaSite = location;

		// process the location
		newLat = (float) location.getLatitude();
		newLong = (float) location.getLongitude();

		// create ordinals and discard negative signs
		if (newLat >= 0)
			latOrdinal = "N";
		else
			latOrdinal = "S";
		newLat = Math.abs(newLat);

		if (newLong >= 0)
			longOrdinal = "E";
		else
			longOrdinal = "W";
		newLong = Math.abs(newLong);

		// get the target satellite from the spinner and retrieve the database
		// row for that bird (_id, name, norad_nbr, longitude)
		cur = mDbHelper.fetchSatellite(mSpnSatPicker.getSelectedItemId());
		startManagingCursor(cur);

		// extract longitude only
		mSatellite.setLongitude(cur.getDouble(cur
				.getColumnIndex(DbAdapter.KEY_LONGITUDE)));

		// update position displays and status flag
		if (flagDMS) {
			String strLat = Location.convert(newLat, Location.FORMAT_SECONDS);
			String strLon = Location.convert(newLong, Location.FORMAT_SECONDS);
			mTxtPositionLat.setText(strLat + latOrdinal);
			mTxtPositionLong.setText(strLon + longOrdinal);
		} else {
			mTxtPositionLat.setText(mNumFmt.format(newLat) + SYM_DEGREE
					+ latOrdinal);
			mTxtPositionLong.setText(mNumFmt.format(newLong) + SYM_DEGREE
					+ longOrdinal);
		}

		mImgStatusGPS.setImageResource(R.drawable.status_good);
		mTxtCEP.setText(Double.toString(location.getAccuracy()) + 'm');
		flagGoodLocation = true;

		// send the target longitude to the method call that calculate look
		// angle and updates the az/el display view.
		updateLookAngleDisplay();
	}

	/**
	 * Triggered when the provider indicates some change in status. Currently
	 * unused and empty.
	 * 
	 * @param provider
	 *            {@link String} describing the {@link Location} provider which
	 *            is indicating the status change.
	 * @param status
	 *            integer containing the new provider status.<BR>
	 *            AVAILABLE == 2<BR>
	 *            OUT_OF_SERVICE == 0<BR>
	 *            TEMPORARILY_UNAVAILABLE == 1<BR>
	 * @param extras
	 *            {@link Bundle} of extra status information from the provider.
	 * */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	/**
	 * Fills the {@link Spinner} object handled by {@link #mSpnSatPicker} with
	 * all of the satellite targets contained in the application database.
	 */
	private void fillData() {
		Cursor curTarget = mDbHelper.fetchAllSatellites();

		// avoid unclosed cursor at onDestroy()...let the Activity manage this
		// problem
		startManagingCursor(curTarget);
		Log.d(TAG, "DB count of satellites: " + curTarget.getCount());

		// create a simplecursoradapter to mangle the db into the spinner field
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, // current
				android.R.layout.simple_spinner_item, // template view
				curTarget, // this is the cursor into the list adapter from the
				// db
				new String[] { DbAdapter.KEY_NAME }, // from db columns
				new int[] { android.R.id.text1 }); // ...to views in the UI

		// set the adapter style/data onto the spinner object
		mSpnSatPicker.setAdapter(adapter);
	}

	/**
	 * When the user selects a target in the {@link Spinner} handled by
	 * {@link LookAngle#mSpnSatPicker mSpnSatPicker}, this method will trigger.
	 * It will retrieve the ephemeris for the satellite in the db indicated by
	 * the parameter 'id', which then calls the
	 * {@link #updateLookAngleDisplay(double) updateLookAngleDisplay()} to
	 * calculate and display the look angle to the target.
	 * 
	 * @param parent
	 *            The {@link AdapterView} that owns the child {@link Spinner}
	 *            which triggered this method call.
	 * @param v
	 *            The {@link View} that is the spinner
	 * @param position
	 *            The ordered position of the item selected in the zero-based
	 *            list in the {@link Spinner}.
	 * @param id
	 *            The {@link DbAdapter#KEY_ID _id} field in the satellite target
	 *            database representing the selected item in the {@link Spinner}
	 *            .
	 */
	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position,
			long id) {
		// locals
		Cursor cur; // cursor to a 1 row result based on _id

		// get the satellite data (_id, name, norad_nbr, longitude)
		cur = mDbHelper.fetchSatellite(id);
		startManagingCursor(cur);

		// extract longitude only and set it on the mSatellite Location object
		mSatellite.setLongitude(cur.getDouble(cur
				.getColumnIndex(DbAdapter.KEY_LONGITUDE)));

		// pass the target longitude to the updateLookAngleDisplay() method for
		// calculation and presentation
		updateLookAngleDisplay();
	}

	/**
	 * Empty handler for "nothing selected in the {@link Spinner}. Currently
	 * empty and unused.
	 */
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

	/**
	 * Method to execute an update of the look angle display. Performs
	 * {@link SatMath#getAzimuth(Location, Location) azimuth} and
	 * {@link SatMath#getElevation(Location, Location) elevation} calculations
	 * on the parameter <code>longitude</code>. The {@link TextView} fields are
	 * updated with the results.
	 * 
	 * @param longitude
	 *            the longitude of the target satellite.
	 */
	private void updateLookAngleDisplay() {
		// locals
		double azimuth, elevation, magDecl;

		// only update display if current position is known goodish
		if (flagGoodLocation) {
			magDecl = SatMath.getMagneticDeclination(mAntennaSite);

			// calculate look angle, adjust az for magnetic decl
			azimuth = SatMath.getAzimuth(mAntennaSite, mSatellite) - magDecl;
			elevation = SatMath.getElevation(mAntennaSite, mSatellite);

			// write into the look angle text view
			mTxtAzimuth.setText(mNumFmt.format(azimuth) + SYM_DEGREE);
			mTxtElevation.setText(mNumFmt.format(elevation) + SYM_DEGREE);

			// warn if target is below horizon
			if (elevation <= 0.0) {
				Toast.makeText(this, getString(R.string.out_of_view),
						Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(
					this,
					"Antenna location unknown\nNo look angle available\nGPS down?",
					Toast.LENGTH_SHORT).show();
		}
		// dbg line
		SatMath.getSkew(mAntennaSite, mSatellite);
	}

	/**
	 * Create the options menu when the user pushes the menu key.
	 * 
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, OPT_DD_ID, 1, R.string.menu_DD);
		menu.add(0, OPT_DMS_ID, 2, R.string.menu_DMS);
		return result;
	}

	/**
	 * Perform actions based on the users selections in the options menu.
	 * 
	 * @param item
	 *            The {@link Menu} object containing user selection state(s).
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case OPT_DD_ID:
			flagDMS = false;
			// necessary to adjust the static display
			onLocationChanged(mAntennaSite);
			return true;
		case OPT_DMS_ID:
			flagDMS = true;
			// necessary to adjust the static display
			onLocationChanged(mAntennaSite);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}