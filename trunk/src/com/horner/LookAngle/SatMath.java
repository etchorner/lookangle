package com.horner.LookAngle;

import android.hardware.GeomagneticField;
import android.location.Location;
import android.util.Log;

/**
 * Container class for public static methods. Provides some spherical geometry
 * calculations and rounding methods.
 * 
 * @author etchorner
 * 
 */
public class SatMath {

	// CONSTANTS
	private static final String TAG = "SatMath"; // for DBG logging

	// END CONSTANTS

	/**
	 * Calculates the antenna pointing <B>TRUE</B> azimuth to the target
	 * satellite. The math comes from Soler, et al. (1995)
	 * <em>Determination of Look Angles to Geostationary Communication Satellites</em>
	 * 
	 * @return {@link Double} degree value of the <B>TRUE</B>azimuth look angle
	 *         from the antenna to the target satellite.
	 * @param site
	 *            {@link Location} object describing the coordinates of the
	 *            antenna site.
	 * @param sat
	 *            {@link Location} object describing the longitude of the
	 *            geostationary satellite target.
	 * 
	 * @see link http://www.ngs.noaa.gov/CORS/Articles/SolerEisemannJSE.pdf
	 */
	public static float getAzimuth(Location site, Location sat) {
		double azimuth = 0.0;
		double beta = 0.0;
		double siteLat = Math.toRadians(site.getLatitude());
		double satLon = Math.toRadians(sat.getLongitude());
		double siteLon = Math.toRadians(site.getLongitude());

		// get the right spherical triangle beta angle
		// from the antenna to the sub-satellite spot
		beta = Math.tan(satLon - siteLon) / Math.sin(siteLat);

		// azimuth is the supplement of the beta angle
		if (Math.abs(beta) < Math.PI)
			azimuth = Math.PI - Math.atan(beta);
		else
			azimuth = Math.PI + Math.atan(beta);

		// back to degrees
		azimuth = Math.toDegrees(azimuth);

		// manage N/S hemispheres
		if (site.getLatitude() < 0.0)
			azimuth = azimuth - 180;
		if (azimuth < 0.0)
			azimuth = azimuth + 360;

		Log.d(TAG, "calculating azimuth: " + azimuth);
		return (float) azimuth;
	}

	/**
	 * Calculates the antenna pointing elevation above the horizon to the target
	 * satellite. The math comes from Soler, et al. (1995)
	 * <em>Determination of Look Angles to Geostationary Communication Satellites</em>
	 * 
	 * @return {@link Double} value of the elevation look angle from the antenna
	 *         to the target satellite.
	 * @param site
	 *            {@link Location} object describing the coordinates of the
	 *            antenna site.
	 * @param sat
	 *            {@link Location} object describing the longitude of the
	 *            geostationary satellite target.
	 * 
	 * @see link http://www.ngs.noaa.gov/CORS/Articles/SolerEisemannJSE.pdf
	 * 
	 *      TODO: implement the rigorous matrix version of Soler's paper.
	 */
	public static float getElevation(Location site, Location sat) {
		double elev = 0.0f;
		double siteLat = Math.toRadians(site.getLatitude());
		double satLon = Math.toRadians(sat.getLongitude());
		double siteLon = Math.toRadians(site.getLongitude());
		double deltaLon = satLon - siteLon;

		// TODO: Dispose of the magic number 0.1512 by converting
		// to a constant (after I understand it's provenance)
		elev = Math.atan((Math.cos(deltaLon) * Math.cos(siteLat) - 0.1512f)
				/ Math.sqrt(1 - (Math.pow(Math.cos(deltaLon), 2) * Math.pow(
						Math.cos(siteLat), 2))));

		Log.d(TAG, "calculated elevation: " + Math.toDegrees(elev));
		return (float) Math.toDegrees(elev);
	}

	/**
	 * Calculates the LNB/dish skew angle
	 * 
	 * @return {@link Double} value of the skew angle for the LNB/Dish. Positive
	 *         values are CW, Negatives are CCW.
	 * @param site
	 *            {@link Location} object describing the coordinates of the
	 *            antenna site.
	 * @param sat
	 *            {@link Location} object describing the longitude of the
	 *            geostationary satellite target.
	 */
	public static double getSkew(Location site, Location sat) {
		double longdiff = Math.toRadians(site.getLongitude()
				- sat.getLongitude());
		double latr = Math.toRadians(site.getLatitude());
		double skew = (Math.atan(Math.sin(longdiff) / Math.tan(latr)));
		Log.d(TAG, "calculated skew: " + Math.toDegrees(skew));
		return Math.toDegrees(skew);
	}

	/**
	 * Returns a float value containing the magnetic declination of the provided
	 * {@link Location} object at this exact moment.
	 * 
	 * @param site
	 *            the {@link Location} at which we need a magnetic declination
	 * @return a float value containing the magnetic declination. Positive value
	 *         is east declination, negative is west declination.
	 */
	public static float getMagneticDeclination(Location site) {
		GeomagneticField mGeoMagFld = new GeomagneticField(
				(float) site.getLatitude(), (float) site.getLongitude(),
				(float) site.getAltitude(), System.currentTimeMillis());

		return mGeoMagFld.getDeclination();
	}

	public String convertLLtoMGRS(Location in) {
		// LOCALS
		String positMGRS = "";

		// TODO: Implement the conversion...ref
		// http://www.uwgb.edu/dutchs/UsefulData/UTMFormulas.htm
		// or similar (this ref is for UTM only, but keep looking
		// or
		// http://www.linz.govt.nz/geodetic/conversion-coordinates/projection-conversions/transverse-mercator-preliminary-computations/index.aspx
		return positMGRS;
	}
}