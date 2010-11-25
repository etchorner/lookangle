package com.horner.LookAngle;

import android.hardware.GeomagneticField;
import android.location.Location;

/**
 * Container class for public static methods. Provides some spherical geometry
 * calculations and rounding methods.
 * 
 * @author etchorner
 * 
 */
public class SatMath {

	// CONSTANTS
	private static final float RADIANS = 57.29578f; // for Math.* methods

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
		double siteLat = site.getLatitude() / RADIANS;
		double satLon = sat.getLongitude() / RADIANS;
		double siteLon = site.getLongitude() / RADIANS;

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
		double siteLat = site.getLatitude() / RADIANS;
		double satLon = sat.getLongitude() / RADIANS;
		double siteLon = site.getLongitude() / RADIANS;
		double deltaLon = satLon - siteLon;

		elev = Math.atan((Math.cos(deltaLon) * Math.cos(siteLat) - 0.1512f)
				/ Math.sqrt(1 - (Math.pow(Math.cos(deltaLon), 2) * Math.pow(
						Math.cos(siteLat), 2))));

		return (float) (elev * RADIANS);
	}

	/**
	 * Converts a decimal degree representation of a coordinate into an array of
	 * {@link String} objects, each element containing one of Degrees, Minutes,
	 * and Seconds.
	 * 
	 * @param coordinate
	 *            the real decimal degree value to be converted.
	 * @return an array of {@link String} objects. String[0] contains degrees,
	 *         String[1] contains minutes, and String[2] contains the seconds.
	 */
	public static final String dDtoDMS(double coordinate) {
		return Location.convert(coordinate, Location.FORMAT_SECONDS);
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
}
