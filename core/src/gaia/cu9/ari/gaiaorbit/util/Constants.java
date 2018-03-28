package gaia.cu9.ari.gaiaorbit.util;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;

import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;

public class Constants {

    /**
     * Scale factor that applies to all distances.
     */
    public static final double SCALE_FACTOR = 5e-6;

    /**
     * Metre to local unit conversion. Multiply this by all values in m.
     */
    public static final double M_TO_U = 1e-9;
    /**
     * Local unit to m conversion.
     */
    public static final double U_TO_M = 1 / M_TO_U;

    /**
     * Kilometre to local unit conversion. Multiply this by all values in Km.
     */
    public static final double KM_TO_U = M_TO_U * 1000;
    /**
     * Local unit to km conversion.
     */
    public static final double U_TO_KM = 1 / KM_TO_U;

    /**
     * AU to local units conversion.
     */
    public static final double AU_TO_U = AstroUtils.AU_TO_KM * KM_TO_U;

    /**
     * Local unit to AU conversion.
     */
    public static final double U_TO_AU = 1 / AU_TO_U;

    /**
     * Parsec to local unit conversion. Multiply this by all values in pc.
     */
    public static final double PC_TO_U = AstroUtils.PC_TO_KM * KM_TO_U;

    /**
     * Kiloparsec to local unit conversion. Multiply this by all values in Kpc.
     */
    public static final double KPC_TO_U = PC_TO_U * 1000;

    /**
     * Megaparsec to local unit conversion. Multiply this by all values in Mpc.
     */
    public static final double MPC_TO_U = PC_TO_U * 1000000;

    /**
     * Local unit to pc conversion.
     */
    public static final double U_TO_PC = 1 / PC_TO_U;

    /** Hours to seconds **/
    public static final double H_TO_S = 3600;

    /** Seconds to hours **/
    public static final double S_TO_H = 1 / H_TO_S;

    /** Hours to milliseconds **/
    public static final double H_TO_MS = H_TO_S * 1000;

    /** Milliseconds to hours **/
    public static final double MS_TO_H = 1 / H_TO_MS;

    /** Days to milliseconds **/
    public static final double D_TO_MS = 86400 * 1000;

    /** Milliseconds to days **/
    public static final double MS_TO_D = 1 / D_TO_MS;

    /** Years to seconds **/
    public static final double Y_TO_S = 31557600;

    /** Seconds to years **/
    public static final double S_TO_Y = 1 / Y_TO_S;

    /** Years to milliseconds **/
    public static final double Y_TO_MS = Y_TO_S * 1000;

    /** Milliseconds to year **/
    public static final double MS_TO_Y = 1 / Y_TO_MS;


    /**
     * Speed of light in m/s
     */
    public static final double C = 299792458;

    /**
     * Speed of light in km/h
     */
    public static final double C_KMH = 1.079253e9;

    /**
     * Speed of light in internal units per second
     */
    public static final double C_US = C * M_TO_U;

    /**
     * Internal units to c
     */
    public static final double U_TO_C = U_TO_M / C;

    /**
     * Solar radius in Km
     */
    public static final double Ro_TO_KM = .6957964e6;

    /**
     * Solar radius to local units
     */
    public static final double Ro_TO_U = Ro_TO_KM * KM_TO_U;

    /**
     * Units to solar radius
     */
    public static final double U_TO_Ro = 1 / Ro_TO_U;

    /** Multiplier for all KM values in the application **/
    public static final double KM_MULTIPLIER = AstroUtils.KM_TO_PC * 1e9 * SCALE_FACTOR;

    /** Distance from Sun that marks the end of the solar system **/
    public static final double SOLAR_SYSTEM_THRESHOLD = 5e9 * KM_MULTIPLIER;

    /**
     * Factor we need to use to get the real size of the star given its quad
     * *texture* size
     **/
    public static final double STAR_SIZE_FACTOR = 1.31526e-6;
    public static final double STAR_SIZE_FACTOR_INV = 1d / STAR_SIZE_FACTOR;

    /** Threshold radius/distance where star size remains constant. **/
    public static final double THRESHOLD_DOWN = 5e-7;
    public static final double THRESHOLD_UP = 1e-2;

    /**
     * 
     * MAXIMUM AND MINIMUM VALUES FOR SEVERAL PARAMETERS - THESE SHOULD BE
     * ENFORCED
     *
     */

    /** Minimum generic slider value **/
    public static final float MIN_SLIDER = 0;
    /** Minimum generic slider value (1) **/
    public static final float MIN_SLIDER_1 = 1;
    /** Maximum generic slider value **/
    public static final float MAX_SLIDER = 100;

    /** Max motion blur value **/
    public static final int MAX_MOTION_BLUR = 85;

    /** Maximum fov value, in degrees **/
    public static final int MAX_FOV = 95;
    /** Minimum fov value, in degrees **/
    public static final int MIN_FOV = 2;

    /** Maximum rotation speed **/
    public static final float MAX_ROT_SPEED = 0.5e4f;
    /** Minimum rotation speed **/
    public static final float MIN_ROT_SPEED = 2e2f;

    /** Maximum turning speed **/
    public static final float MAX_TURN_SPEED = 3e3f;
    /** Minimum turning speed **/
    public static final float MIN_TURN_SPEED = 2e2f;

    /** Minimum star brightness **/
    public static final float MIN_STAR_BRIGHT = 0.2f;
    /** Maximum star brightness **/
    public static final float MAX_STAR_BRIGHT = 25f;

    /** Minimum number factor for proper motion vectors **/
    public static final float MIN_PM_NUM_FACTOR = 1f;
    /** Maximum number factor for proper motion vectors **/
    public static final float MAX_PM_NUM_FACTOR = 6f;

    /** Minimum length factor for proper motion vectors **/
    public static final float MIN_PM_LEN_FACTOR = 500f;
    /** Maximum length factor for proper motion vectors **/
    public static final float MAX_PM_LEN_FACTOR = 30000f;

    /** Minimum angle where the LOD transitions start **/
    public static final float MIN_LOD_TRANS_ANGLE_DEG = 0f;
    /** Maximum angle where the LOD transitions end **/
    public static final float MAX_LOD_TRANS_ANGLE_DEG = 93f;

    /** Minimum star pixel size **/
    public static final float MIN_STAR_POINT_SIZE = 0.5f;
    /** Maximum star pixel size **/
    public static final float MAX_STAR_POINT_SIZE = 35;
    /** Step to increase/decrease **/
    public static final float STEP_STAR_POINT_SIZE = 0.5f;

    /** Minimum spacecraft responsiveness **/
    public static final float MIN_SC_RESPONSIVENESS = .5e6f;
    /** Maximum spacecraft responsiveness **/
    public static final float MAX_SC_RESPONSIVENESS = .5e7f;

    /** Minimum star minimum opacity **/
    public static final float MIN_STAR_MIN_OPACITY = 0.0f;
    /** Maximum star minimum opacity **/
    public static final float MAX_STAR_MIN_OPACITY = 0.9f;

    public static final float MIN_BRIGHTNESS = -1.0f;
    public static final float MAX_BRIGHTNESS = 1.0f;

    public static final float MIN_CONTRAST = 0.0f;
    public static final float MAX_CONTRAST = 2.0f;

    // Max time, 5 Myr
    public static final long MAX_TIME_MS = 5000000l * (long) Y_TO_MS;
    // Min time, -5 Myr
    public static final long MIN_TIME_MS = -MAX_TIME_MS;

    // Max time for VSOP87 algorithms
    public static final long MAX_VSOP_TIME_MS = 50000l * (long) Y_TO_MS;

    // Min time for VSOP87 algorithms
    public static final long MIN_VSOP_TIME_MS = -MAX_VSOP_TIME_MS;

    // Maximum time warp factor
    public static final double MAX_WARP = 35184372088832d;
    // Minimum time warp factor
    public static final double MIN_WARP = -MAX_WARP;

    /** Camera speed factor **/
    public static final float CAMERA_SPEED_FACTOR = 10f;

    /** Motion blur value **/
    public static final float MOTION_BLUR_VALUE = 0.7f;

    /**
     * Checks whether the given time is within the acceptable bounds of VSOP87
     * algorithms.
     * 
     * @param time
     *            The time as the number of milliseconds since January 1, 1970,
     *            00:00:00 GMT.
     * @return Whether the given time is within the bounds of VSOP
     */
    public static boolean withinVSOPTime(long time) {
        return time <= Constants.MAX_VSOP_TIME_MS && time >= Constants.MIN_VSOP_TIME_MS;
    }

    /**
     * 
     * SYSTEM-DEPENDENT STUFF
     * 
     */
    public static boolean mobile = false;
    public static boolean webgl = false;
    public static boolean desktop = false;
    public static boolean focalplane = false;

    static {
        if (Gdx.app != null) {
            mobile = (Gdx.app.getType() == ApplicationType.Android || Gdx.app.getType() == ApplicationType.iOS) && !ConfInit.instance.webgl;
            desktop = Gdx.app.getType() == ApplicationType.Desktop && !ConfInit.instance.webgl;
            webgl = Gdx.app.getType() == ApplicationType.WebGL || ConfInit.instance.webgl;
        }
    }

    /**
     * Nature
     */
    public static class Nature {
        /** Number of seconds per day */
        public static final double DAY_SECOND = 86400.0D;
        /** One degree in units of radians */
        public static final double DEGREE_RADIAN = 0.0174532925199433D;
        /** One radian in units of degrees */
        public static final double RADIAN_DEGREE = 57.2957795130823D;
        /** One arcsecond in units of radians */
        public static final double ARCSECOND_RADIAN = 4.84813681109536E-6D;
        /** Number of days per Julian year */
        public static final double JULIANYEAR_DAY = 365.25D;
        /**
         * Mean (geometric) longitude rate of the nominal Sun for use in
         * simulations of the NSL (mean ecliptic orbital elements, at the
         * standard epoch J2000.0). Note that a value of 1295977422.83429 /
         * (1.0E3 * 365.25 * 3600.0) = 0.98560911 degrees day^-1 is given in
         * Section 5.8.3 of J.L. Simon, P. Bretagnon, J. Chapront, M.
         * Chapront-Touze, G. Francou, J. Laskar, 1994, \'Numerical expressions
         * for precession formulae and mean elements for the Moon and the
         * planets\', A\&A, 282, 663 (1994A\&A...282..663S)
         */
        public static final double NOMINALSUN_MEANLONGITUDERATE_J2000 = 0.98560903D;
    }

}
