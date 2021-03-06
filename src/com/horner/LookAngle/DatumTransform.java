/**
 *
 */
package com.horner.LookAngle;

import java.text.DecimalFormat;

/**
 * Class that holds all manner of coordinate system transformation routines. All
 * are currently based on WGS84 geoid and ACE algorithms. To reflect alternate
 * geoids, change the constants 'a' and 'b' in the member fields.
 * 
 * @author etchorner
 * 
 */
public class DatumTransform {
	// CONSTANTS
	/** formatting object for longitude zone */
	private static DecimalFormat longZoneFormat = new DecimalFormat("#");
	/** formatting object for UTM northing value */
	private static DecimalFormat northingFormat = new DecimalFormat("#");
	/** formatting object for UTM easting value */
	private static DecimalFormat eastingFormat = new DecimalFormat("#");
	/** semi-major axis of WGS84 ellipsoid */
	private static final double a = 6378137d;
	/** semi-minor axis of WGS84 ellipsoid */
	private static final double b = 6356752.3142d;
	/** first eccentricity of WGS84 ellipsoid */
	private static final double epsilon = Math.sqrt((a * a - b * b) / (a * a));
	private static final double epsilonP2 = Math.pow(epsilon, 2)
			/ (1 - Math.pow(epsilon, 2));
	/** point scale factor of UTM */
	private static final double ptScaleFactorUTM = 0.9996;
	/** UTM easting origin */
	private static final int UTMeastingOrigin = 500000;
	/** First Meridional arc component */
	public static final double A0 = 1 - Math.pow(epsilon, 2) / 4 - 3
			* Math.pow(epsilon, 4) / 64 - 5 * Math.pow(epsilon, 6) / 256 - 175
			* Math.pow(epsilon, 8) / 16384;
	/** Second Meridional arc component */
	public static final double A2 = 3 * (Math.pow(epsilon, 2)
			+ Math.pow(epsilon, 4) / 4 + 15 * Math.pow(epsilon, 6) / 128 - 455 * Math
			.pow(epsilon, 8) / 4096) / 8;
	/** Third Meridional arc component */
	public static final double A4 = 15 * (Math.pow(epsilon, 4) + 3
			* Math.pow(epsilon, 6) / 4 - 77 * Math.pow(epsilon, 8) / 128) / 256;
	/** Fourth Meridional arc component */
	public static final double A6 = 35 * (Math.pow(epsilon, 6) - 41 * Math.pow(
			epsilon, 8) / 32) / 3072;
	/** Fifth Meridional arc component */
	public static final double A8 = -315 * Math.pow(epsilon, 8) / 131072;
	/** Latitude Zone stuff */
	private static final char[] latZoneLetters = { 'A', 'C', 'D', 'E', 'F',
			'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U',
			'V', 'W', 'X', 'Z' };
	private static final int[] latZoneDegrees = { -90, -84, -72, -64, -56, -48,
			-40, -32, -24, -16, -8, 0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 84 };
	private static final char[] latZoneNegLetters = { 'A', 'C', 'D', 'E', 'F',
			'G', 'H', 'J', 'K', 'L', 'M' };
	private static final int[] latZoneNegDegrees = { -90, -84, -72, -64, -56,
			-48, -40, -32, -24, -16, -8 };
	private static final char[] latZonePosLetters = { 'N', 'P', 'Q', 'R', 'S',
			'T', 'U', 'V', 'W', 'X', 'Z' };
	private static final int[] latZonePosDegrees = { 0, 8, 16, 24, 32, 40, 48,
			56, 64, 72, 84 };

	/**
	 * Uses Army Corps of Engineers algorithm to transform geodetic coordinate
	 * values of latitude and longitude to a UTM String.
	 * 
	 * Source for algorithm is "Handbook for Transformation of Datums,
	 * Projections, Grids, and Common Coordinate Systems", May 2004.
	 * 
	 * @param inLat
	 *            geodetic latitude value (positive is North, negative is South)
	 * @param inLon
	 *            geodetic longitude value (positive is East, negative is West)
	 * @return a String object containing the full UTM coordinate in the form
	 *         Longitude Zone (Z), Latitude Zone (z), Northing (N), Easting (E)
	 *         "ZZ z NNNNNN EEEEEE"
	 */
	public static String convertLLtoUTM(double inLat, double inLon) {
		// LOCALS
		int longZone = 0;
		String latZone = "";
		double lat = 0;
		double lon = 0;
		/** Geoid separation */
		double nu;
		double t, eta, lambda, xTM, yTM, xUTM, yUTM;

		// handle hemispheres of input, convert to positive radians
		if (inLon < 0) {
			lon = Math.toRadians(360 - Math.abs(inLon));
		} else {
			lon = Math.toRadians(inLon);
		}
		lat = Math.toRadians(inLat);

		// get the UTM zones and lambda (diff, between central meridian and
		// longitude of fix
		latZone = getLatZone(lat);
		longZone = getLongZone(lon);
		lambda = lon - getCentralMeridian(longZone);

		// set up the terms for the Transverse Mercator coordinate computation
		// from eq. 7.9
		nu = a / Math.sqrt(1 - Math.pow(epsilon * Math.sin(lat), 2));
		t = Math.tan(lat);
		eta = Math.sqrt(epsilonP2) * Math.cos(lat);

		// get Transverse Mercator x and y coordinates
		// from eq. 7.8
		xTM = (nu * lambda * Math.cos(lat))
				+ ((nu * Math.pow(lambda, 3) * Math.pow(Math.cos(lat), 3)) / 6)
				* (1 - Math.pow(t, 2) + Math.pow(eta, 2))
				+ ((nu * Math.pow(lambda, 5) * Math.pow(Math.cos(lat), 5)) / 120)
				* (5 - 18 * Math.pow(t, 2) + Math.pow(t, 4) + 14
						* Math.pow(eta, 2) - 58 * Math.pow(t, 2)
						* Math.pow(eta, 2));

		yTM = getMeridionalArc(lat)
				+ (nu * Math.pow(lambda, 2) / 2)
				* (Math.sin(lat) * Math.cos(lat))
				+ (nu * Math.pow(lambda, 4) / 24)
				* (Math.sin(lat) * Math.pow(Math.cos(lat), 3))
				* (5 - Math.pow(t, 2) + 9 * Math.pow(eta, 2) + 4 * Math.pow(
						eta, 4))
				+ (nu * Math.pow(lambda, 6) / 720)
				* (Math.sin(lat) * Math.pow(Math.cos(lat), 5))
				* (61 - 58 * Math.pow(t, 2) + Math.pow(t, 4) + 270
						* Math.pow(eta, 2) - 330 * Math.pow(t, 2)
						* Math.pow(eta, 2));

		// transform TM to UTM
		xUTM = ptScaleFactorUTM * xTM + UTMeastingOrigin;
		if (lat >= 0) {
			yUTM = ptScaleFactorUTM * yTM;
		} else {
			yUTM = ptScaleFactorUTM * yTM + 10000000;
		}

		// Build the whole UTM string and return it
		longZoneFormat.setMinimumIntegerDigits(2);
		northingFormat.setMinimumIntegerDigits(6);
		eastingFormat.setMinimumIntegerDigits(7);
		return longZoneFormat.format(longZone) + " " + latZone + " "
				+ northingFormat.format(xUTM) + " "
				+ eastingFormat.format(yUTM);
	}

	/**
	 * Uses Army Corps of Engineers algorithm to transform UTM coordinate values
	 * to geodetic latitude and longitude.
	 * 
	 * Source for algorithm is "Handbook for Transformation of Datums,
	 * Projections, Grids, and Common Coordinate Systems", May 2004.
	 * 
	 * @param inUTM
	 *            a String object containing the UTM coordinate
	 * @return an array of doubles, the 0th is the latitude, and the 1st is
	 *         longitude
	 */
	public static double[] convertUTMtoLL(String inUTM) {
		// TODO: implement IAW ACE section 7.4.3.2
		// LOCALS
		double[] rtnLL = null;

		return rtnLL;
	}

	/**
	 * Uses Army Corps of Engineers algorithm to transform geodetic coordinate
	 * values of latitude and longitude to a MGRS String.
	 * 
	 * Source for algorithm is "Handbook for Transformation of Datums,
	 * Projections, Grids, and Common Coordinate Systems", May 2004.
	 * 
	 * @param inLat
	 *            double value of latitude to be converted
	 * @param inLon
	 *            double value of longitude to be converted
	 * @return a String object containing the ten digit MGRS grid reference.
	 */
	public static String convertLLtoMGRS(double inLat, double inLon) {
		// TODO: implement
		return "";
	}

	/**
	 * Uses Army Corps of Engineers algorithm to transform MGRS grid reference
	 * to geodetic longitude and latitude coordinates.
	 * 
	 * Source for algorithm is "Handbook for Transformation of Datums,
	 * Projections, Grids, and Common Coordinate Systems", May 2004.
	 * 
	 * @param inMGRS
	 *            a String object containing a formatted MGRS grid reference.
	 * @return an array of doubles, 0th element is latitude, first element is
	 *         longitude.
	 */
	public static double[] convertMGRStoLL(String inMGRS) {
		// TODO: implement
		// LOCALS
		double[] rtnLL = null;

		return rtnLL;
	}

	@SuppressWarnings("unused")
	private static void testConvertLLtoUTM() {

		System.out.print(convertLLtoUTM(0.0000, 0.0000) + "\n31 N 166021 0\n");
		System.out.print(convertLLtoUTM(0.1300, -0.2324)
				+ "\n30 N 808084 14385\n");
		System.out.print(convertLLtoUTM(-45.6456, 23.3545)
				+ "\n34 G 683473 4942631\n");
		System.out.print(convertLLtoUTM(-12.7650, -33.8765)
				+ "\n25 L 404859 8588690\n");
		System.out.print(convertLLtoUTM(-80.5434, -170.6540)
				+ "\n02 C 506346 1057742\n");
		System.out.print(convertLLtoUTM(90.0000, 177.0000)
				+ "\n60 Z 500000 9997964\n");
		System.out.print(convertLLtoUTM(-90.0000, -177.0000)
				+ "\n01 A 500000 2035\n");
		System.out.print(convertLLtoUTM(90.0000, 3.0000)
				+ "\n31 Z 500000 9997964\n");
		System.out.print(convertLLtoUTM(23.4578, -135.4545)
				+ "\n08 Q 453580 2594272\n");
		System.out.print(convertLLtoUTM(77.3450, 156.9876)
				+ "\n57 X 450793 8586116\n");
		System.out.print(convertLLtoUTM(-89.3454, -48.9306)
				+ "\n22 A 502639 75072\n");
	}

	private static int getLongZone(double inLon) {
		// 7.4.1.1 Find the UTM zone using eq. 7.22
		if (inLon >= 0 && inLon <= Math.PI) {
			return (int) Math.floor(31 + (180 * inLon) / (6 * Math.PI));
		} else {
			return (int) Math.floor((180 * inLon) / (6 * Math.PI) - 29);
		}
	}

	private static double getCentralMeridian(double longZone) {
		// 7.4.1.1 Find the central meridian from the UTM zone, using eq. 7.23
		if (longZone >= 31) {
			return ((6 * longZone - 183) * Math.PI / 180);
		} else {
			return ((6 * longZone + 177) * Math.PI / 180);
		}
	}

	private static double getMeridionalArc(double lat) {
		return a
				* (A0 * lat - A2 * Math.sin(2 * lat) + A4 * Math.sin(4 * lat)
						- A6 * Math.sin(6 * lat) + A8 * Math.sin(8 * lat));
	}

	@SuppressWarnings("unused")
	private static int getLatZoneDegree(String letter) {
		char ltr = letter.charAt(0);
		for (int i = 0; i < latZoneLetters.length; i++) {
			if (latZoneLetters[i] == ltr) {
				return latZoneDegrees[i];
			}
		}
		return -100;
	}

	private static String getLatZone(double latitude) {
		int latIndex = -2;
		int lat = (int) latitude;

		if (lat >= 0) {
			int len = latZonePosLetters.length;
			for (int i = 0; i < len; i++) {
				if (lat == latZonePosDegrees[i]) {
					latIndex = i;
					break;
				}

				if (lat > latZonePosDegrees[i]) {
					continue;
				} else {
					latIndex = i - 1;
					break;
				}
			}
		} else {
			int len = latZoneNegLetters.length;
			for (int i = 0; i < len; i++) {
				if (lat == latZoneNegDegrees[i]) {
					latIndex = i;
					break;
				}

				if (lat < latZoneNegDegrees[i]) {
					latIndex = i - 1;
					break;
				} else {
					continue;
				}
			}
		}

		if (latIndex == -1) {
			latIndex = 0;
		}
		if (lat >= 0) {
			if (latIndex == -2) {
				latIndex = latZonePosLetters.length - 1;
			}
			return String.valueOf(latZonePosLetters[latIndex]);
		} else {
			if (latIndex == -2) {
				latIndex = latZoneNegLetters.length - 1;
			}
			return String.valueOf(latZoneNegLetters[latIndex]);
		}
	}
}