package gaia.cu9.ari.gaiaorbit.util.coord;

import java.sql.Timestamp;
import java.util.Date;

import gaia.cu9.ari.gaiaorbit.util.LruCache;
import gaia.cu9.ari.gaiaorbit.util.coord.vsop87.VSOP87;
import gaia.cu9.ari.gaiaorbit.util.coord.vsop87.iVSOP87;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector2d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Some astronomical algorithms to get the position of the Sun, Moon, work out Julian dates, etc.
 * @author Toni Sagrista
 *
 */
public class AstroUtils {

    /** Julian date of J2000 epoch **/
    static final public double JD_J2000 = 2451545.; // Julian Date of J2000
    /** Julian date of the Gaia-specific reference epoch J2010 = J2010.0 = JD2455197.5 = 2010-01-01T00:00:00 **/
    static final public double JD_J2010 = 2455197.5;
    /** Julian date of TGAS epoch 2010-01-01T00:00:00 **/
    static final public double JD_J2015 = 2457023.500000;

    /** Julian date of B1900 epoch */
    static final public double JD_B1900 = 2415020.31352;// Julian Date of B1900
    /** Milliseconds of J2000 in the scale of java.util.Date **/
    public static final long J2000_MS;

    static {
        Date d = new Date(2000, 0, 1, 0, 0, 0);
        J2000_MS = d.getTime();
    }

    /** Julian date cache, since most dates are used more than once **/
    private static LruCache<Long, Double> jdcache = new LruCache<Long, Double>(50);

    // Time units
    public static final double DAY_TO_NS = 86400e9;
    public static final double DAY_TO_MS = 86400e3;
    public static final double NS_TO_DAY = 1 / DAY_TO_NS;

    // Angle conversion
    public static final double TO_RAD = Math.PI / 180;
    public static final double TO_DEG = 180 / Math.PI;

    public static final double DEG_TO_ARCSEC = 3600;
    public static final double ARCSEC_TO_DEG = 1 / DEG_TO_ARCSEC;
    public static final double DEG_TO_MILLARCSEC = DEG_TO_ARCSEC * 1000;
    public static final double MILLARCSEC_TO_DEG = 1 / DEG_TO_ARCSEC;

    // Distance units
    public static final float PC_TO_KM = 3.08567758e13f;
    public static final float KM_TO_PC = 1 / PC_TO_KM;
    public static final float AU_TO_KM = 149597871f;
    public static final float KM_TO_AU = 1 / AU_TO_KM;

    // Earth equatorial radius in Km
    public static final float EARTH_RADIUS = 6378.1370f;

    /** Initialize nsl Sun **/
    private static final NslSun nslSun = new NslSun();

    private static final Vector3d aux3 = new Vector3d();
    private static final Vector2d aux2 = new Vector2d();

    /**
     * Algorithm in "Astronomical Algorithms" book by Jean Meeus. Finds out the distance from the Sun to the Earth in km.
     * @param date
     * @return
     */
    public static double getSunDistance(Date date) {
        return getSunDistance(getJulianDateCache(date));
    }

    public static double getSunDistance(double jd) {
        double T = T(jd);
        double T2 = T * T;
        double T3 = T2 * T;
        double M = 357.5291 + 35999.0503 * T - 0.0001599 * T2 - 0.00000048 * T3;
        double e = 0.016708617 - 0.000042037 * T - 0.0000001236 * T2;
        double C = (1.9146 - 0.004817 * T - 0.000014 * T2) * Math.sin(Math.toRadians(M)) + (0.019993 - 0.000101 * T) * Math.sin(Math.toRadians(2 * M)) + 0.00029 * Math.sin(Math.toRadians(3 * M));
        double v = M + C;

        double R = (1.000001018 * (1 - e * e)) / (1 + e * Math.cos(Math.toRadians(v)));
        return R * AU_TO_KM;
    }

    private static Date cacheSunLongitudeDate = new Date();
    private static double cacheSunLongitude;

    /**
     * Returns the Sun's ecliptic longitude in degrees for the given time.
     * Caches the last Sun's longitude for future use.
     * @param date The time for which the longitude must be calculated
     * @return The Sun's longitude in [deg]
     */
    public static double getSunLongitude(Date date) {
        if (!date.equals(cacheSunLongitudeDate)) {
            double julianDate = getJulianDateCache(date);

            nslSun.setTime(julianDate);
            double aux = Math.toDegrees(nslSun.getSolarLongitude()) % 360;

            cacheSunLongitudeDate.setTime(date.getTime());
            cacheSunLongitude = aux % 360;
        }
        return cacheSunLongitude;
    }

    /**
     * Gets the ecliptic longitude of the Sun in degrees as published in Wikipedia.
     * @see <a href="http://en.wikipedia.org/wiki/Position_of_the_Sun">http://en.wikipedia.org/wiki/Position_of_the_Sun</a>
     * @param jd The Julian date for which to calculate the latitude.
     * @return The ecliptic longitude of the Sun at the given Julian date, in degrees.
     */
    public static double getSunLongitudeWikipedia(double jd) {
        double n = jd - JD_J2000;
        double L = 280.460d + 0.9856474d * n;
        double g = 357.528d + 0.9856003d * n;
        double l = L + 1.915 * Math.sin(Math.toRadians(g)) + 0.02 * Math.sin(Math.toRadians(2 * g));
        return l;
    }

    /**
     * @deprecated This is no longer of use
     * Assumes a uniform speed.
     * @param date
     * @return
     */
    @SuppressWarnings("unused")
    public static double getFakeSunLongitude(Date date) {
        int year = date.getYear();
        int month = date.getMonth();
        int day = date.getDate();

        int hour = date.getHours();
        int min = date.getMinutes();
        int sec = date.getSeconds();
        int nanos = ((Timestamp) date).getNanos();

        double frac = (1d / 12d) * month + (1d / 365.242d) * day + (1d / 8765.81d) * hour + (1d / 525949d) * min + (1d / 31556940d) * sec + (1d / 31556940e9d) * nanos;
        double l = year + frac * 360d;
        return l;
    }

    /**
     * Algorithm in "Astronomical Algorithms" book by Jean Meeus. Returns a vector with the equatorial longitude (&alpha;) in radians, the
     * equatorial latitude (&delta;) in radians and the distance in kilometers.
     * @param date
     */
    public static void moonEquatorialCoordinates(Vector3d placeholder, Date date) {
        moonEquatorialCoordinates(placeholder, getJulianDateCache(date));
    }

    /**
     * Algorithm in "Astronomical Algorithms" book by Jean Meeus. Returns a vector with the equatorial longitude (&alpha;) in radians, the
     * equatorial latitude (&delta;) in radians and the distance in kilometers.
     * @param julianDate
     */
    public static void moonEquatorialCoordinates(Vector3d placeholder, double julianDate) {
        moonEclipticCoordinates(julianDate, aux3);
        Vector2d equatorial = Coordinates.eclipticToEquatorial(aux3.x, aux3.y, aux2);
        placeholder.set(equatorial.x, equatorial.y, aux3.z);
    }

    /**
     * Algorithm in "Astronomical Algorithms" book by Jean Meeus. Returns a vector with the ecliptic longitude (&lambda;) in radians, the
     * ecliptic latitude (&beta;) in radians and the distance in kilometers.
     * @param date
     * @param out The output vector.
     * @return The output vector, for chaining.
     */
    public static Vector3d moonEclipticCoordinates(Date date, Vector3d out) {
        return moonEclipticCoordinates(getJulianDateCache(date), out);
    }

    /**
     * Algorithm in "Astronomical Algorithms" book by Jean Meeus. Returns a vector with the ecliptic longitude (&lambda;) in radians, the
     * ecliptic latitude (&beta;) in radians and the distance in kilometers.
     * @param julianDate
     * @param out The output vector.
     * @return The output vector, for chaining.
     */
    public static Vector3d moonEclipticCoordinates(double julianDate, Vector3d out) {
        // Time T measured in Julian centuries from the Epoch J2000.0
        double T = T(julianDate);
        double T2 = T * T;
        double T3 = T2 * T;
        double T4 = T3 * T;
        // Moon's mean longitude, referred to the mean equinox of the date
        double Lp = 218.3164591 + 481267.88134236 * T - 0.0013268 * T2 + T3 / 538841 - T4 / 65194000;
        Lp = prettyAngle(Lp);
        // Mean elongation of the Moon
        double D = 297.8502042 + 445267.1115168 * T - 0.00163 * T2 + T3 / 545868 - T4 / 113065000;
        D = prettyAngle(D);
        // Sun's mean anomaly
        double M = 357.5291092 + 35999.0502909 * T - 0.0001536 * T2 + T3 / 24490000;
        M = prettyAngle(M);
        // Moon's mean anomaly
        double Mp = 134.9634114 + 477198.8676313 * T + 0.008997 * T2 + T3 / 69699 - T4 / 14712000;
        Mp = prettyAngle(Mp);
        // Moon's argument of latitude (mean distance of the Moon from its ascending node)
        double F = 93.2720993 + 483202.0175273 * T - 0.0034029 * T2 - T3 / 3526000 + T4 / 863310000;
        F = prettyAngle(F);
        // Three further arguments (again, in degrees) are needed
        double A1 = 119.75 + 131.849 * T;
        A1 = prettyAngle(A1);
        double A2 = 53.09 + 479264.290 * T;
        A2 = prettyAngle(A2);
        double A3 = 313.45 + 481266.484 * T;
        A3 = prettyAngle(A3);

        // Multiply by E the arguments that contain M or -M, multiply by E2 the arguments that contain 2M or -2M
        double E = 1 - 0.002516 * T - 0.0000074 * T2;

        double[] aux = calculateSumlSumr(D, M, Mp, F, E, A1, A2, Lp);
        double suml = aux[0];
        double sumr = aux[1];
        double sumb = calculateSumb(D, sumr, Mp, F, E, A1, A3, Lp);

        double lambda = prettyAngle(Lp + suml / 1000000);
        double beta = (sumb / 1000000) % 360;
        double dist = 385000.56 + sumr / 1000;

        return out.set(Math.toRadians(lambda), Math.toRadians(beta), dist);
    }

    /** 
     * Calculates the longitude Sum(l) and distance Sum(r) of the Moon using the table.
     * @param D
     * @param M
     * @param Mp
     * @param F
     * @return
     */
    private static double[] calculateSumlSumr(double D, double M, double Mp, double F, double E, double A1, double A2, double Lp) {
        double suml = 0, sumr = 0;
        for (int i = 0; i < table45a.length; i++) {
            int[] curr = table45a[i];
            // Take into effect terms that contain M and thus depend on the eccentricity of the Earth's orbit around the 
            // Sun, which presently is decreasing with time.
            double mul = 1;
            if (curr[2] == 1 || curr[2] == -1) {
                mul = E;
            } else if (curr[2] == 2 || curr[2] == -2) {
                mul = E * E;
            }
            suml += curr[4] * MathUtilsd.sin(Math.toRadians(curr[0] * D + curr[1] * M + curr[2] * Mp + curr[3] * F)) * mul;
            sumr += curr[5] * MathUtilsd.cos(Math.toRadians(curr[0] * D + curr[1] * M + curr[2] * Mp + curr[3] * F)) * mul;
        }
        // Addition to Suml. The terms involving A1 are due to the action of Venus. The term involving A2 is due to Jupiter
        // while those involing L' are due to the flattening of the Earth.
        double sumladd = 3958 * MathUtilsd.sin(Math.toRadians(A1)) + 1962 * MathUtilsd.sin(Math.toRadians(Lp - F)) + 318 * MathUtilsd.sin(Math.toRadians(A2));
        suml += sumladd;

        return new double[] { suml, sumr };

    }

    private static double calculateSumb(double D, double M, double Mp, double F, double E, double A1, double A3, double Lp) {
        double sumb = 0;
        for (int i = 0; i < table45b.length; i++) {
            int[] curr = table45b[i];
            // Take into effect terms that contain M and thus depend on the eccentricity of the Earth's orbit around the 
            // Sun, which presently is decreasing with time.
            double mul = 1;
            if (curr[2] == 1 || curr[2] == -1) {
                mul *= E;
            } else if (curr[2] == 2 || curr[2] == -2) {
                mul *= E * E;
            }
            sumb += curr[4] * MathUtilsd.sin(Math.toRadians(curr[0] * D + curr[1] * M + curr[2] * Mp + curr[3] * F)) * mul;
        }
        // Addition to Sumb. The terms involving A1 are due to the action of Venus. The term involving A2 is due to Jupiter
        // while those involing L' are due to the flattening of the Earth.
        double sumbadd = -2235 * MathUtilsd.sin(Math.toRadians(Lp)) + 382 * MathUtilsd.sin(Math.toRadians(A3)) + 175 * MathUtilsd.sin(Math.toRadians(A1 - F)) + 175 * MathUtilsd.sin(Math.toRadians(A1 + F)) + 127 * MathUtilsd.sin(Math.toRadians(Lp - Mp)) - 115 * MathUtilsd.sin(Math.toRadians(Lp + Mp));
        sumb += sumbadd;

        return sumb;
    }

    /**
     * Periodic terms for the longitude (Sum(l)) and distance (Sum(r)) of the Moon. The unit is 0.000001 degree
     * for Sum(l), and 0.001 km for Sum(r).
     * Multiple of
     * D M M' F  CoeffSine CoeffCosine
     */
    private static final int[][] table45a = { { 0, 0, 1, 0, 6288774, -20905355 }, { 2, 0, -1, 0, 1274027, -3699111 }, { 2, 0, 0, 0, 658314, -2955968 }, { 0, 0, 2, 0, 213618, -569925 }, { 0, 1, 0, 0, -185116, 48888 }, { 0, 0, 0, 2, -114332, -3149 }, { 2, 0, -2, 0, 58793, 246158 }, { 2, -1, -1, 0, 57066, -152138 }, { 2, 0, 1, 0, 53322, -170733 }, { 2, -1, 0, 0, 45758, -204586 }, { 0, 1, -1, 0, -40923, -129620 }, { 1, 0, 0, 0, -34720, 108743 }, { 0, 1, 1, 0, -30383, 104755 },
            { 2, 0, 0, -2, 15327, 10321 }, { 0, 0, 1, 2, -12528, 0 }, { 0, 0, 1, -2, 10980, 79661 }, { 4, 0, -1, 0, 10675, -34782 }, { 0, 0, 3, 0, 10034, -23210 }, { 4, 0, -2, 0, 8548, -21636 }, { 2, 1, -1, 0, -7888, 24208 }, { 2, 1, 0, 0, -6766, 30824 }, { 1, 0, -1, 0, -5163, -8379 }, { 1, 1, 0, 0, 4987, -16675 }, { 2, -1, 1, 0, 4036, -12831 }, { 2, 0, 2, 0, 3994, -10445 }, { 4, 0, 0, 0, 3861, -11650 }, { 2, 0, -3, 0, 3665, 14403 }, { 0, 1, -2, 0, -2689, -7003 }, { 2, 0, -1, 2, -2602, 0 },
            { 2, -1, -2, 0, 2390, 10056 }, { 1, 0, 1, 0, -2348, 6322 }, { 2, -2, 0, 0, 2236, -9884 }, { 0, 1, 2, 0, -2120, 5751 }, { 0, 2, 0, 0, -2069, 0 }, { 2, -2, -1, 0, 2048, -4950 }, { 2, 0, 1, -2, -1773, 4130 }, { 2, 0, 0, 2, -1595, 0 }, { 4, -1, -1, 0, 1215, -3958 }, { 0, 0, 2, 2, -1110, 0 }, { 3, 0, -1, 0, -892, 3258 }, { 2, 1, 1, 0, -810, 2616 }, { 4, -1, -2, 0, 759, -1897 }, { 0, 2, -1, 0, -713, -2117 }, { 2, 2, -1, 0, -700, 2354 }, { 2, 1, -2, 0, 691, 0 }, { 2, -1, 0, -2, 596, 0 },
            { 4, 0, 1, 0, 549, -1423 }, { 0, 0, 4, 0, 537, -1117 }, { 4, -1, 0, 0, 520, -1571 }, { 1, 0, -2, 0, -487, -1739 }, { 2, 1, 0, -2, -399, 0 }, { 0, 0, 2, -2, -381, -4421 }, { 1, 1, 1, 0, 351, 0 }, { 3, 0, -2, 0, -340, 0 }, { 4, 0, -3, 0, 330, 0 }, { 2, -1, 2, 0, 327, 0 }, { 0, 2, 1, 0, -323, 1165 }, { 1, 1, -1, 0, 299, 0 }, { 2, 0, 3, 0, 294, 0 }, { 2, 0, -1, -2, 0, 8752 } };

    /**
     * Periodic terms for the latitude of the Moon (Sum(b)). The unit is 0.000001 degree.
     * Multiple of
     * D M M' F 	Coefficient of the sine of the argument
     */
    private static final int[][] table45b = { { 0, 0, 0, 1, 5128122, 0 }, { 0, 0, 1, 1, 280602, 0 }, { 0, 0, 1, -1, 277693, 0 }, { 2, 0, 0, -1, 173237, 0 }, { 2, 0, -1, 1, 55413, 0 }, { 2, 0, -1, -1, 46271, 0 }, { 2, 0, 0, 1, 32573, 0 }, { 0, 0, 2, 1, 17198, 0 }, { 2, 0, 1, -1, 9266, 0 }, { 0, 0, 2, -1, 8822, 0 }, { 2, -1, 0, -1, 8216, 0 }, { 2, 0, -2, -1, 4324, 0 }, { 2, 0, 1, 1, 4200, 0 }, { 2, 1, 0, -1, -3359, 0 }, { 2, -1, -1, 1, 2463, 0 }, { 2, -1, 0, 1, 2211, 0 },
            { 2, -1, -1, -1, 2065, 0 }, { 0, 1, -1, -1, -1870, 0 }, { 4, 0, -1, -1, 1828, 0 }, { 0, 1, 0, 1, -1794, 0 }, { 0, 0, 0, 3, -1749, 0 }, { 0, 1, -1, 1, -1565, 0 }, { 1, 0, 0, 1, -1491, 0 }, { 0, 1, 1, 1, -1475, 0 }, { 0, 1, 1, -1, -1410, 0 }, { 0, 1, 0, -1, -1344, 0 }, { 1, 0, 0, -1, -1335, 0 }, { 0, 0, 3, 1, 1107, 0 }, { 4, 0, 0, -1, 1021, 0 }, { 4, 0, -1, 1, 833, 0 }, { 0, 0, 1, -3, 777, 0 }, { 4, 0, -2, 1, 671, 0 }, { 2, 0, 0, -3, 607, 0 }, { 2, 0, 2, -1, 596, 0 },
            { 2, -1, 1, -1, 491, 0 }, { 2, 0, -2, 1, -451, 0 }, { 0, 0, 3, -1, 439, 0 }, { 2, 0, 2, 1, 422, 0 }, { 2, 0, -3, -1, 421, 0 }, { 2, 1, -1, 1, -366, 0 }, { 2, 1, 0, 1, -351, 0 }, { 4, 0, 0, 1, 331, 0 }, { 2, -1, 1, 1, 315, 0 }, { 2, -2, 0, -1, 302, 0 }, { 0, 0, 1, 3, -283, 0 }, { 2, 1, 1, -1, -229, 0 }, { 1, 1, 0, -1, 223, 0 }, { 1, 1, 0, 1, 223, 0 }, { 0, 1, -2, -1, -220, 0 }, { 2, 1, -1, -1, -220, 0 }, { 1, 0, 1, 1, -185, 0 }, { 2, -1, -2, -1, 181, 0 }, { 0, 1, 2, 1, -177, 0 },
            { 4, 0, -2, -1, 176, 0 }, { 4, -1, -1, -1, 166, 0 }, { 1, 0, 1, -1, -164, 0 }, { 4, 0, 1, -1, 132, 0 }, { 1, 0, -1, -1, -119, 0 }, { 4, -1, 0, -1, 115, 0 }, { 2, -2, 0, 1, 107, 0 } };

    /**
     * Returns a vector with the heliocentric ecliptic latitude and longitude in radians and the distance in km.
     * @param body The body.
     * @param date The date to get the position.
     * @param out The output vector
     * @return The output vector with L, B and R, for chaining.
     * @deprecated Should use the classes that extend IBodyCoordinates instead.
     */
    public static Vector3d getEclipticCoordinates(String body, Date date, Vector3d out) {

        switch (body) {
        case "Moon":
            return new MoonAACoordinates().getEclipticSphericalCoordinates(date, out);
        default:
            double tau = tau(getJulianDateCache(date));

            iVSOP87 coor = VSOP87.instance.getVOSP87(body);
            double L = (coor.L0(tau) + coor.L1(tau) + coor.L2(tau) + coor.L3(tau) + coor.L4(tau) + coor.L5(tau));
            double B = (coor.B0(tau) + coor.B1(tau) + coor.B2(tau) + coor.B3(tau) + coor.B4(tau) + coor.B5(tau));
            double R = (coor.R0(tau) + coor.R1(tau) + coor.R2(tau) + coor.R3(tau) + coor.R4(tau) + coor.R5(tau));
            R = R * AU_TO_KM;

            out.set(L, B, R);
            return out;
        }
    }

    /**
     * Gets the orbital elements of the given celestial body, from Astronomical Algorithms (Jean Meeus).
     * L - mean longitude of the planet
     * a - semimajor axis of the orbit
     * e - eccentricity of the orbit
     * i - inclination on the plane of the ecliptic
     * omega - longitude of the ascending node
     * pi - longitude of the perihelion
     * @param body The body
     * @param date The date
     * @return A vector with the orbital elements of the given body in the above order.
     */
    private static double[] getOrbitalElements(String body, Date date) {
        return getOrbitalElements(body, getJulianDateCache(date));
    }

    /**
     * Gets the orbital elements of the given celestial body, from Astronomical Algorithms (Jean Meeus).
     * L - mean longitude of the planet
     * a - semimajor axis of the orbit
     * e - eccentricity of the orbit
     * i - inclination on the plane of the ecliptic
     * omega - longitude of the ascending node
     * pi - longitude of the perihelion
     * @param body The body
     * @param julianDate The julian date
     * @return A vector with the orbital elements of the given body in the above order.
     */
    private static double[] getOrbitalElements(String body, double julianDate) {
        double[] el = new double[6];
        // Time T measured in Julian centuries from the Epoch J2000.0
        double T = T(julianDate);
        double T2 = T * T;
        double T3 = T2 * T;
        switch (body) {
        case "Mercury":
            el[0] = 252.250960 + 149474.0722491 * T + 0.00030397 * T2 + 0.000000018 * T3;
            el[1] = 0.387098310;
            el[2] = 0.20563175 + 0.000020406 * T - 0.0000000284 * T2 - 0.00000000017 * T3;
            el[3] = 7.004986 + 0.0018215 * T - 0.00001809 * T2 + 0.000000053 * T3;
            el[4] = 48.330893 + 1.1861890 * T + 0.00017587 * T2 + 0.000000211 * T3;
            el[5] = 77.456119 + 1.5564775 * T + 0.00029589 * T2 + 0.000000056 * T3;
            break;
        case "Venus":

            break;
        case "Mars":

            break;
        case "Jupiter":
            el[0] = 34.351484 + 3036.3027889 * T + 0.00022374 * T2 - 0.000000025 * T3;
            el[1] = 5.202603191 + 0.0000001913 * T;
            el[2] = 0.04849485 + 0.000163244 * T - 0.0000004719 * T2 - 0.00000000197 * T3;
            el[3] = 1.30327 - 0.0054966 * T + 0.00000465 * T2 - 0.000000004 * T3;
            el[4] = 100.464441 + 1.020955 * T + 0.00040117 * T2 + 0.000000569 * T3;
            el[5] = 14.331309 + 1.6126668 * T + 0.00103127 * T2 - 0.000004569 * T3;
            break;
        case "Saturn":

            break;
        case "Uranus":

            break;
        case "Neptune":

            break;
        default:
            break;
        }
        return el;
    }

    private static double prettyAngle(double angle) {
        return angle % 360 + (angle < 0 ? 360 : 0);
    }

    /**
     * Gets the Julian date number given the Gregorian calendar quantities.
     * @param year
     * @param month
     * @param day
     * @param hour
     * @param min
     * @param sec
     * @param nanos
     * @param gregorian Whether to use the Gregorian or the Julian calendar
     * @return
     */
    public static double getJulianDate(int year, int month, int day, int hour, int min, int sec, int nanos, boolean gregorian) {
        if (gregorian) {
            return getJulianDayNumberWikipediaGregorianCalendar(year, month, day) + getDayFraction(hour, min, sec, nanos);
        } else {
            return getJulianDayNumberWikipediaJulianCalendar(year, month, day) + getDayFraction(hour, min, sec, nanos);
        }
    }

    /**
     * Gets the Julian Date for the given date. It uses a cache.
     * @param date The date.
     * @return The Julian Date.
     */
    public static double getJulianDateCache(Date date) {
        long time = date.getTime();
        if (jdcache.containsKey(time)) {
            return jdcache.get(time);
        } else {
            Double jd = getJulianDate(date);
            jdcache.put(time, jd);
            return jd;
        }
    }

    public static double getJulianDate(Date date) {
        int year = date.getYear() + 1900;
        int month = date.getMonth() + 1;
        int day = date.getDate();

        int hour = date.getHours();
        int min = date.getMinutes();
        int sec = date.getSeconds();
        int nanos = (int) (date.getTime() % 1000) * 1000000;
        return getJulianDate(year, month, day, hour, min, sec, nanos, true);
    }

    public static double getMsSinceJ2010(Date date) {
        return (getJulianDateCache(date) - JD_J2010) * DAY_TO_MS;
    }

    public static double getMsSinceJ2000(Date date) {
        return (getJulianDateCache(date) - JD_J2000) * DAY_TO_MS;
    }

    public static double getMsSinceJ2015(Date date) {
        return (getJulianDateCache(date) - JD_J2015) * DAY_TO_MS;
    }

    /**
     * Gets the Gregorian calendar quantities given the Julian date.
     * @param julianDate The Julian date
     * @return Vector with {year, month, day, hour, min, sec, nanos}
     */
    public static int[] getCalendarDay(double julianDate) {
        /**
        	y	4716	v	3
        	j	1401	u	5
        	m	2	s	153
        	n	12	w	2
        	r	4	B	274277
        	p	1461	C	−38
        	
        	1. f = J + j + (((4 * J + B)/146097) * 3)/4 + C
        	2. e = r * f + v
        	3. g = mod(e, p)/r
        	4. h = u * g + w
        	5. D = (mod(h, s))/u + 1
        	6. M = mod(h/s + m, n) + 1
        	7. Y = e/p - y + (n + m - M)/n
        	 */

        // J is the julian date number
        int J = (int) julianDate;
        int y = 4716, j = 1401, m = 2, n = 12, r = 4, p = 1461, v = 3, u = 5, s = 153, w = 2, B = 274277, C = -38;
        int f = J + j + (((4 * J + B) / 146097) * 3) / 4 + C;
        int e = r * f + v;
        int g = (e % p) / r;
        int h = u * g + w;
        int D = (h % s) / u + 1;
        int M = ((h / s + m) % n) + 1;
        int Y = e / p - y + (n + m - M) / n;

        double dayFraction = julianDate - J;
        int[] df = getDayQuantities(dayFraction);

        return new int[] { Y, M, D, df[0], df[1], df[2], df[3] };

    }

    /**
     * Returns the Julian day number. Uses the method shown in "Astronomical Algorithms" by Jean Meeus.
     * @param year The year
     * @param month The month in [1:12]
     * @param day The day in the month, starting at 1
     * @return The Julian date
     * @deprecated This does not work well!
     */
    @SuppressWarnings("unused")
    public static double getJulianDayNumberBook(int year, int month, int day) {
        int a = (int) (year / 100);
        int b = 2 - a + (int) (a / 4);

        // Julian day
        return (int) (365.242 * (year + 4716)) + (int) (30.6001 * (month)) + day + b - 1524.5d;
    }

    /**
     * Returns the Julian day number of a date in the Gregorian calendar. Uses Wikipedia's algorithm.
     * @see <a href="http://en.wikipedia.org/wiki/Julian_day">http://en.wikipedia.org/wiki/Julian_day</a>
     * @param year The year
     * @param month The month in [1:12]
     * @param day The day in the month, starting at 1
     * @return The Julian date
     */
    public static double getJulianDayNumberWikipediaGregorianCalendar(int year, int month, int day) {
        int a = (int) ((14 - month) / 12);
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;

        return day + (int) ((153 * m + 2) / 5) + 365 * y + (int) (y / 4) - (int) (y / 100) + (int) (y / 400) - 32045.5;
    }

    /**
     * Returns the Julian day number of a date in the Julian calendar. Uses Wikipedia's algorithm.
     * @see <a href="http://en.wikipedia.org/wiki/Julian_day">http://en.wikipedia.org/wiki/Julian_day</a>
     * @param year The year
     * @param month The month in [1:12]
     * @param day The day in the month, starting at 1
     * @return The Julian date
     */
    public static double getJulianDayNumberWikipediaJulianCalendar(int year, int month, int day) {
        int a = (int) ((14 - month) / 12);
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;

        return day + (int) ((153 * m + 2) / 5) + 365 * y + (int) (y / 4) - 32083.5;
    }

    /**
     * Gets the day fraction from the day quantities
     * @param hour
     * @param min
     * @param sec
     * @param nanos
     * @return
     */
    public static double getDayFraction(int hour, int min, int sec, int nanos) {
        return hour / 24d + min / 1440d + (sec + nanos / 1E9d) / 86400d;
    }

    /**
     * Gets the day quantities from the day fraction
     * @param dayFraction
     * @return [hours, minutes, seconds, nanos]
     */
    public static int[] getDayQuantities(double dayFraction) {
        double hourf = dayFraction * 24d;
        double minf = (hourf - (int) hourf) * 60d;
        double secf = (minf - (int) minf) * 60d;
        double nanosf = (secf - (int) secf) * 1E9d;
        return new int[] { (int) hourf, (int) minf, (int) secf, (int) nanosf };
    }

    /**
     * Returns the obliquity of the ecliptic (inclination of the Earth's axis of rotation) for
     * a given date, in degrees.
     * @return
     */
    public static double obliquity(double julianDate) {
        // JPL's fundamental ephemerides have been continually updated. The Astronomical Almanac for 2010 specifies:
        // E = 23° 26′ 21″.406 − 46″.836769 T − 0″.0001831 T2 + 0″.00200340 T3 − 0″.576×10−6 T4 − 4″.34×10−8 T5
        
        double T = T(julianDate);
/*        double T2 = T * T;
        double T3 = T2 * T;
        double T4 = T3 * T;
        double T5 = T4 * T;

        double todeg = 1.0 / 3600.0;

        return 23.0 + 26.0 / 60.0 + 21.406 * todeg - 46.836769 * todeg * T - 0.0001831 * todeg * T2 + 0.00200340 * todeg * T3 - 0.576e-6 * todeg * T4 - 4.34e-8 * todeg * T5;
  */      
        return 23.0 + 26.0 / 60.0 
                + ( 21.406 - ( 46.836769 - ( 0.0001831 + ( 0.00200340 - ( 0.576e-6 - 4.34e-8 * T ) * T ) * T ) * T ) * T ) / 3600.0;
    }

    /**
     * Time T measured in Julian centuries from the Epoch J2000.0
     * @param julianDate
     * @return
     */
    public static double T(double julianDate) {
        return (julianDate - 2451545) / 36525;
    }

    public static double tau(double julianDate) {
        return (julianDate - 2451545) / 365250;
    }
    
}
