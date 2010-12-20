package com.horner.LookAngle;

import android.hardware.GeomagneticField;
import android.location.Location;

/**
 * Container class for public static methods. Provides some spherical and
 * ellipsoidal geometry calculations.
 * 
 * @author etchorner
 * 
 */
public class SatMath {

	// CONSTANTS
	@SuppressWarnings("unused")
	private static final String TAG = "SatMath"; // for DBG logging

	// END CONSTANTS

	/**
	 * Uses Soler's rigorous elliptical method to compute azimuth and vertical
	 * angle to a geostationary satellite.
	 * 
	 * Soler, et al. (1995)
	 * <em>Determination of Look Angles to Geostationary Communication Satellites</em>
	 * 
	 * @param site
	 *            the geocoded antenna Location object
	 * @param sat
	 *            the geocoded satellite Location object (really only need the
	 *            longitude of geostationary satellites.
	 * @return an array of two double values. The 0th element is the azimuth,
	 *         and the 1st element is the elevation.
	 */
	public static double[] getLookAngle(Location site, Location sat) {
		// LOCALS
		double[] rtnLookAngle = new double[2];
		double siteAlt = site.getAltitude();
		double siteLat = Math.toRadians(site.getLatitude());
		double siteLon = Math.toRadians(site.getLongitude());
		double satLon = Math.toRadians(sat.getLongitude());
		long a = 6378137;
		double b = 6356752.3142d;
		double epsilon = Math.sqrt((a * a - b * b) / (a * a));
		double N = a / Math.sqrt(1 - Math.pow(epsilon * Math.sin(siteLat), 2));
		long r = 42200000;
		double x_p;
		double y_p;
		double z_p;
		double x_s;
		double y_s;
		double z_s;
		double[] mtxSatComponentsXYZ = new double[3]; // components in {x,y,z}
		double e; // component of the satellite in local {e,n,u}
		double n; // component of the satellite in local {e,n,u}
		double u; // component of the satellite in local {e,n,u}
		double alpha; // azimuth to satellite
		double nu; // elevation to satellite

		// Step 1: Transform curvilinear to cartesian coordinates
		x_p = (N + siteAlt) * Math.cos(siteLon) * Math.cos(siteLat);
		y_p = (N + siteAlt) * Math.sin(siteLon) * Math.cos(siteLat);
		z_p = (N * (1 - Math.pow(epsilon, 2)) + siteAlt) * Math.sin(siteLat);

		x_s = r * Math.cos(satLon);
		y_s = r * Math.sin(satLon);
		z_s = 0;

		// Step 2: Satellite components (x,y,z)
		// * Matrix subtraction of antenna position from satellite position
		mtxSatComponentsXYZ[0] = x_s - x_p;
		mtxSatComponentsXYZ[1] = y_s - y_p;
		mtxSatComponentsXYZ[2] = z_s - z_p;

		// Step 3: Satellite components (e,n,u)
		// [e] [x]
		// [n] = R_1(pi/2-inLat)*R_3(inLong+pi/2)[y]
		// [u] [z]
		e = -1 * Math.sin(siteLon) * mtxSatComponentsXYZ[0] + Math.cos(siteLon)
				* mtxSatComponentsXYZ[1];
		n = -1 * Math.sin(siteLat) * Math.cos(siteLon) * mtxSatComponentsXYZ[0]
				- Math.sin(siteLat) * Math.sin(siteLon)
				* mtxSatComponentsXYZ[1] + Math.cos(siteLat)
				* mtxSatComponentsXYZ[2];
		u = Math.cos(siteLat) * Math.cos(siteLon) * mtxSatComponentsXYZ[0]
				+ Math.cos(siteLat) * Math.sin(siteLon)
				* mtxSatComponentsXYZ[1] + Math.sin(siteLat)
				* mtxSatComponentsXYZ[2];

		// Step 4: Calculate look angle
		alpha = Math.atan(e / n);
		nu = Math.atan(u / Math.sqrt(e * e + n * n));

		// flip negative azimuth in the southern hemisphere
		if (alpha < 0 && siteLat <= 0)
			alpha = alpha + 2 * Math.PI;

		// add a half circle
		if (siteLat > 0)
			alpha = alpha + Math.PI;

		rtnLookAngle[0] = Math.toDegrees(alpha);
		rtnLookAngle[1] = Math.toDegrees(nu);
		return rtnLookAngle;
	}

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

		// manage N/S hemispheres
		if (site.getLatitude() < 0.0)
			azimuth = azimuth - Math.PI;
		if (azimuth < 0.0)
			azimuth = azimuth + 2 * Math.PI;

		azimuth = Math.toDegrees(azimuth);

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

		return (float) Math.toDegrees(elev);
	}

	/**
	 * Calculates the LNB/dish skew angle
	 * 
	 * @return a double value of the skew angle for the LNB/Dish. Positive
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
}