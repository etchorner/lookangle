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
	 *            the geocoded antenna {@link Location} object
	 * @param sat
	 *            the geocoded satellite {@link Location} object (really only
	 *            need the longitude of geostationary satellites.
	 * @return an array of two double values. The 0th element is the azimuth,
	 *         and the 1st element is the elevation.
	 */
	public static double[] getLookAngle(Location site, Location sat) {
		// LOCALS
		/** return value array with azimut and elevation */
		double[] rtnLookAngle = new double[2];
		/** altitude of antenna site */
		double siteAlt = site.getAltitude();
		/** latitude of antenna site */
		double siteLat = Math.toRadians(site.getLatitude());
		/** longitude of antenna site */
		double siteLon = Math.toRadians(site.getLongitude());
		/** longitude of satellite (sub-satellite point) */
		double satLon = Math.toRadians(sat.getLongitude());
		/** Semi-major axis of WGS84 ellipsoid */
		long a = 6378137;
		/** Semi-minor axis of WGS84 ellipsoid */
		double b = 6356752.3142d;
		/** Eccentricity of WGS84 ellipsoid */
		double epsilon = Math.sqrt((a * a - b * b) / (a * a));
		/** Principal radius of curvature in the prime vertical */
		double N = a / Math.sqrt(1 - Math.pow(epsilon * Math.sin(siteLat), 2));
		/** Average satellite altitude above ellipsoid origin (in meters) */
		long r = 42200000;
		/** Cartesian coordinates of antenna site */
		double x_p, y_p, z_p;
		/** Cartesion coordinates of satellite */
		double x_s, y_s, z_s;
		/** Satellite terrestrial components */
		double x, y, z;
		/**
		 * local (right handed) geodetic components, e-axis points to geodetic
		 * east, n points to geodetic north and u points to geodetic zenith
		 */
		double e, n, u;
		/** azimuth from antenna to satellite */
		double alpha;
		/** elevation (vertical angle) from antenna to satellite */
		double nu;

		// Step 1: Transform curvilinear to cartesian coordinates
		// a. Antenna site
		x_p = (N + siteAlt) * Math.cos(siteLon) * Math.cos(siteLat);
		y_p = (N + siteAlt) * Math.sin(siteLon) * Math.cos(siteLat);
		z_p = (N * (1 - Math.pow(epsilon, 2)) + siteAlt) * Math.sin(siteLat);

		// b. Satellite location
		x_s = r * Math.cos(satLon);
		y_s = r * Math.sin(satLon);
		z_s = 0;

		// Step 2: Satellite components (x,y,z)
		x = x_s - x_p;
		y = y_s - y_p;
		z = z_s - z_p;

		// Step 3: Transform satellite components to geodetic e,n,u
		e = -1 * Math.sin(siteLon) * x + Math.cos(siteLon) * y;
		n = -1 * Math.sin(siteLat) * Math.cos(siteLon) * x - Math.sin(siteLat)
				* Math.sin(siteLon) * y + Math.cos(siteLat) * z;
		u = Math.cos(siteLat) * Math.cos(siteLon) * x + Math.cos(siteLat)
				* Math.sin(siteLon) * y + Math.sin(siteLat) * z;

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
	 * satellite.
	 * 
	 * @return double decimal degree value of the <B>TRUE</B>azimuth look angle
	 *         from the antenna to the target satellite.
	 * @param site
	 *            {@link Location} object describing the coordinates of the
	 *            antenna site.
	 * @param sat
	 *            {@link Location} object describing the longitude of the
	 *            geostationary satellite target.
	 * 
	 */
	public static double getAzimuth(Location site, Location sat) {
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

		return azimuth;
	}

	/**
	 * Calculates the antenna pointing elevation above the horizon to the target
	 * satellite.
	 * 
	 * @return double decimal degree value of the elevation look angle from the
	 *         antenna to the target satellite.
	 * @param site
	 *            {@link Location} object describing the coordinates of the
	 *            antenna site.
	 * @param sat
	 *            {@link Location} object describing the longitude of the
	 *            geostationary satellite target.
	 * 
	 */
	public static double getElevation(Location site, Location sat) {
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

		return Math.toDegrees(elev);
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
	public static double getMagneticDeclination(Location site) {
		GeomagneticField mGeoMagFld = new GeomagneticField((float) site
				.getLatitude(), (float) site.getLongitude(), (float) site
				.getAltitude(), System.currentTimeMillis());

		return mGeoMagFld.getDeclination();
	}
}