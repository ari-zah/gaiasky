/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.desktop.util;

import java.io.File;

/**
 * Wee utility class to check the operating system and the desktop environment.
 * It also offers retrieval of common system folders.
 *
 * @author Toni Sagrista
 */
public class SysUtils {

    /**
     * Initialise directories
     */
    public static void mkdirs() {
        // Top level
        getDataDir().mkdirs();
        getConfigDir().mkdirs();
        // Bottom level
        getDefaultCameraDir().mkdirs();
        getDefaultMusicDir().mkdirs();
        getDefaultFramesDir().mkdirs();
        getDefaultScreenshotsDir().mkdirs();
        getDefaultTmpDir().mkdirs();
        getDefaultMappingsDir().mkdirs();
    }

    private static String OS = System.getProperty("os.name").toLowerCase();

    public static String getXdgDesktop() {
        return System.getenv("XDG_CURRENT_DESKTOP");
    }

    public static boolean checkLinuxDesktop(String desktop) {
        try {
            String value = getXdgDesktop();
            return value != null && !value.isEmpty() && value.equalsIgnoreCase(desktop);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return false;
    }

    public static boolean checkUnity() {
        return isLinux() && checkLinuxDesktop("ubuntu");
    }

    public static boolean checkGnome() {
        return isLinux() && checkLinuxDesktop("gnome");
    }

    public static boolean checkKDE() {
        return isLinux() && checkLinuxDesktop("kde");
    }

    public static boolean checkXfce() {
        return isLinux() && checkLinuxDesktop("xfce");
    }

    public static boolean checkBudgie() {
        return isLinux() && checkLinuxDesktop("budgie:GNOME");
    }

    public static boolean checkI3() {
        return isLinux() && checkLinuxDesktop("i3");
    }

    public static String getOSName() {
        return OS;
    }

    public static String getOSFamily() {
        if (isLinux())
            return "linux";
        if (isWindows())
            return "win";
        if (isMac())
            return "macos";
        if (isUnix())
            return "unix";
        if (isSolaris())
            return "solaris";

        return "unknown";
    }

    public static boolean isLinux() {
        return (OS.indexOf("linux") >= 0);
    }

    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    public static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    public static boolean isUnix() {
        return (OS.indexOf("unix") >= 0);
    }

    public static boolean isSolaris() {
        return (OS.indexOf("sunos") >= 0);
    }

    public static String getOSArchitecture() {
        return System.getProperty("os.arch");
    }

    public static String getOSVersion() {
        return System.getProperty("os.version");
    }

    private static final String GAIASKY_DIR_NAME = "gaiasky";
    private static final String DOTGAIASKY_DIR_NAME = ".gaiasky";
    private static final String CAMERA_DIR_NAME = "camera";
    private static final String SCREENSHOTS_DIR_NAME = "screenshots";
    private static final String FRAMES_DIR_NAME = "frames";
    private static final String MUSIC_DIR_NAME = "music";
    private static final String MAPPINGS_DIR_NAME = "mappings";
    private static final String DATA_DIR_NAME = "data";
    private static final String TMP_DIR_NAME = "tmp";
    private static final String CRASHREPORTS_DIR_NAME = "crashreports";

    /**
     * Gets a file pointer to the camera directory.
     *
     * @return A pointer to the Gaia Sky camera directory
     */
    public static File getDefaultCameraDir() {
        return new File(getDataDir(), CAMERA_DIR_NAME);
    }

    /**
     * Gets a file pointer to the default screenshots directory.
     *
     * @return A pointer to the Gaia Sky screenshots directory
     */
    public static File getDefaultScreenshotsDir() {
        return new File(getDataDir(), SCREENSHOTS_DIR_NAME);
    }

    /**
     * Gets a file pointer to the frames directory.
     *
     * @return A pointer to the Gaia Sky frames directory
     */
    public static File getDefaultFramesDir() {
        return new File(getDataDir(), FRAMES_DIR_NAME);
    }

    /**
     * Gets a file pointer to the music directory.
     *
     * @return A pointer to the Gaia Sky music directory
     */
    public static File getDefaultMusicDir() {
        return new File(getDataDir(), MUSIC_DIR_NAME);
    }

    /**
     * Gets a file pointer to the mappings directory.
     *
     * @return A pointer to the Gaia Sky mappings directory
     */
    public static File getDefaultMappingsDir() {
        return new File(getDataDir(), MAPPINGS_DIR_NAME);
    }

    /**
     * Gets a file pointer to the local data directory where the data files are downloaded and stored.
     *
     * @return A pointer to the local data directory where the data files are
     */
    public static File getLocalDataDir() {
        return new File(getDataDir(), DATA_DIR_NAME);
    }

    /**
     * Gets a file pointer to the crash reports directory, where crash reports are stored.
     *
     * @return A pointer to the crash reports directory
     */
    public static File getCrashReportsDir() {
        return new File(getDataDir(), CRASHREPORTS_DIR_NAME);
    }

    /**
     * Gets a file pointer to the $HOME/.gaiasky/tmp directory.
     *
     * @return A pointer to the Gaia Sky temporary directory in the user's home.
     */
    public static File getDefaultTmpDir() {
        return new File(getDataDir(), TMP_DIR_NAME);
    }

    /**
     * Returns the default data directory. That is ~/.gaiasky/ in Windows and macOS, and ~/.local/share/gaiasky
     * in Linux.
     *
     * @return Default data directory
     */
    public static File getDataDir() {
        if (isLinux()) {
            return new File(getXdgDataHome(), GAIASKY_DIR_NAME + File.separator);
        } else {
            return new File(System.getProperty("user.home"), DOTGAIASKY_DIR_NAME + File.separator);
        }
    }

    public static File getHomeDir() {
        return new File(System.getProperty("user.home"));
    }

    public static String getHomeDirString() {
        return System.getProperty("user.home");
    }

    public static File getConfigDir() {
        if (isLinux()) {
            return new File(getXdgConfigHome(), GAIASKY_DIR_NAME + File.separator);
        } else {
            return new File(System.getProperty("user.home"), DOTGAIASKY_DIR_NAME + File.separator);
        }
    }

    private static String getXdgDataHome() {
        String dhome = System.getenv("XDG_DATA_HOME");
        if (dhome == null || dhome.isEmpty()) {
            return System.getProperty("user.home") + File.separator + ".local" + File.separator + "share";
        } else {
            return dhome;
        }
    }

    private static String getXdgConfigHome() {
        String chome = System.getenv("XDG_CONFIG_HOME");
        if (chome == null || chome.isEmpty()) {
            return System.getProperty("user.home") + File.separator + ".config";
        } else {
            return chome;
        }

    }

}
