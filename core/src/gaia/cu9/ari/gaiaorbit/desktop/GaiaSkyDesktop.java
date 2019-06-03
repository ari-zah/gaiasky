/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.data.DesktopSceneGraphImplementationProvider;
import gaia.cu9.ari.gaiaorbit.data.SceneGraphImplementationProvider;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopNumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.render.DesktopPostProcessorFactory;
import gaia.cu9.ari.gaiaorbit.desktop.render.ScreenModeCmd;
import gaia.cu9.ari.gaiaorbit.desktop.util.*;
import gaia.cu9.ari.gaiaorbit.desktop.util.camera.CamRecorder;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.ConsoleLogger;
import gaia.cu9.ari.gaiaorbit.interfce.KeyBindings;
import gaia.cu9.ari.gaiaorbit.interfce.MusicActorsManager;
import gaia.cu9.ari.gaiaorbit.interfce.NetworkCheckerManager;
import gaia.cu9.ari.gaiaorbit.render.PostProcessorFactory;
import gaia.cu9.ari.gaiaorbit.screenshot.ScreenshotsManager;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.math.MathManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.Properties;

/**
 * Main class for the desktop launcher
 *
 * @author Toni Sagrista
 */
public class GaiaSkyDesktop implements IObserver {
    private static final Log logger = Logger.getLogger(GaiaSkyDesktop.class);

    /*
    * Configuration file version of the source code
    * This is usually tag where each chunk takes 2 spaces.
    * Version = major.minor.rev -> 1.2.5 major=1; minor=2; rev=5
    * Version = major * 10000 + minor * 100 + rev
    * So 1.2.5 -> 010205
    *    2.1.7 -> 020107
    */
    public static int SOURCE_CONF_VERSION = 020200;
    private static GaiaSkyDesktop gsd;
    private static boolean REST_ENABLED = false;
    private static Class<?> REST_SERVER_CLASS = null;

    private static GaiaSkyArgs gsargs;

    /**
     * Program arguments
     *
     * @author Toni Sagrista
     */
    private static class GaiaSkyArgs {
        @Parameter(names = { "-h", "--help" }, description = "Show program options and usage information", help = true, order = 0) private boolean help = false;

        @Parameter(names = { "-v", "--version" }, description = "List Gaia Sky version and relevant information.", order = 1) private boolean version = false;

        @Parameter(names = { "-d", "--ds-download" }, description = "Display the data download dialog at startup. If no data is found, the download dialog is shown automatically.", order = 2) private boolean download = false;

        @Parameter(names = { "-c", "--cat-chooser" }, description = "Display the catalog chooser dialog at startup. This enables the selection of different available catalogs when Gaia Sky starts.", order = 3) private boolean catalogChooser = false;

        @Parameter(names = { "-p", "--properties" }, description = "Specify the location of the properties file.", order = 4) private String propertiesFile = null;

        @Parameter(names = { "-a", "--assets" }, description = "Specify the location of the assets folder. If not present, the default assets location is used.", order = 5) private String assetsLocation = null;
    }

    /**
     * Formats the regular usage so that it removes the left padding characters.
     * This is necessary so that help2man recognizes the OPTIONS block.
     *
     * @param jc The JCommander object
     */
    private static void printUsage(JCommander jc) {
        StringBuilder sb = new StringBuilder();
        jc.usage(sb, "");
        String usage = sb.toString();

        sb = new StringBuilder();
        String[] lines = usage.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i == 0) {
                // Add extra line between usage and options
                sb.append(lines[i] + "\n\n");
            } else {
                sb.append(lines[i].substring(2) + '\n');
            }
        }
        System.out.println(sb.toString());
    }

    /**
     * Main method
     *
     * @param args Arguments
     */
    public static void main(String[] args) {
        gsargs = new GaiaSkyArgs();
        JCommander jc = JCommander.newBuilder().addObject(gsargs).build();
        jc.setProgramName("gaiasky");
        try {
            jc.parse(args);

            if (gsargs.help) {
                printUsage(jc);
                return;
            }
        } catch (Exception e) {
            System.out.print("gaiasky: bad program arguments\n\n");
            printUsage(jc);
            return;
        }
        try {
            // Check java version
            javaVersionCheck();

            // Set properties file from arguments to VM params if needed
            if (gsargs.propertiesFile != null && !gsargs.propertiesFile.isEmpty()) {
                System.setProperty("properties.file", gsargs.propertiesFile);
            }

            // Set assets location to VM params if needed
            if (gsargs.assetsLocation != null && !gsargs.assetsLocation.isEmpty()) {
                System.setProperty("assets.location", gsargs.assetsLocation);
            }

            gsd = new GaiaSkyDesktop();

            Gdx.files = new Lwjgl3Files();

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            // Init gaiasky directories
            SysUtils.mkdirs();

            // Init properties file
            String props = System.getProperty("properties.file");
            if (props == null || props.isEmpty()) {
                initConfigFile(false);
            }

            // Initialize i18n (only for global config logging)
            I18n.initialize(Gdx.files.internal("i18n/gsbundle"));

            // Init global configuration
            ConfInit.initialize(new DesktopConfInit());
            GlobalConf.screen.SCREEN_WIDTH = 1080;
            GlobalConf.screen.SCREEN_HEIGHT = 1200;

            // Reinitialize with user-defined locale
            I18n.initialize(Gdx.files.absolute(GlobalConf.ASSETS_LOC + File.separator + "i18n/gsbundle"));

            if (gsargs.version) {
                System.out.println(GlobalConf.getShortApplicationName());
                System.out.println("License MPL 2.0: Mozilla Public License 2.0 <https://www.mozilla.org/en-US/MPL/2.0/>");
                System.out.println();
                System.out.println("Written by Toni Sagrista Selles <tsagrista@ari.uni-heidelberg.de>");
                return;
            }

            // REST API server
            REST_ENABLED = GlobalConf.program.REST_PORT >= 0 && checkRestDepsInClasspath();
            if (REST_ENABLED) {
                REST_SERVER_CLASS = Class.forName("gaia.cu9.ari.gaiaorbit.rest.RESTServer");
                Method init = REST_SERVER_CLASS.getMethod("initialize", Integer.class);
                init.invoke(null, GlobalConf.program.REST_PORT);
            }

            // Fullscreen command
            ScreenModeCmd.initialize();

            // Init cam recorder
            CamRecorder.initialize();

            // Music actors
            MusicActorsManager.initialize(new DesktopMusicActors());

            // Init music manager
            MusicManager.initialize(Gdx.files.absolute(GlobalConf.ASSETS_LOC + "music"), Gdx.files.absolute(SysUtils.getDefaultMusicDir().getAbsolutePath()));

            // Initialize post processor factory
            PostProcessorFactory.initialize(new DesktopPostProcessorFactory());

            // Key mappings
            KeyBindings.initialize();

            // Scene graph implementation provider
            SceneGraphImplementationProvider.initialize(new DesktopSceneGraphImplementationProvider());

            // Initialize screenshots manager
            ScreenshotsManager.initialize();

            // Network checker
            NetworkCheckerManager.initialize(new DesktopNetworkChecker());

            // Math
            MathManager.initialize();

            gsd.init();
        } catch (Exception e) {
            CrashReporter.reportCrash(e, logger);
        }

    }

    private ConsoleLogger consoleLogger;

    public GaiaSkyDesktop() {
        super();
        consoleLogger = new ConsoleLogger();
        EventManager.instance.subscribe(this, Events.SCENE_GRAPH_LOADED, Events.DISPOSE);
    }

    private void init() {
        launchMainApp();
    }

    public void launchMainApp() {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle(GlobalConf.APPLICATION_NAME);
        // Note that we disable VSync! The VRContext manages vsync with respect to the HMD
        cfg.useVsync(false);
        cfg.setWindowedMode(GlobalConf.screen.SCREEN_WIDTH, GlobalConf.screen.SCREEN_HEIGHT);
        cfg.setResizable(true);
        cfg.setWindowIcon(Files.FileType.Internal, "icon/ic_launcher.png");
        cfg.useOpenGL3(true, 3, 2);
        // Disable logical DPI modes (macOS, Windows)
        cfg.setHdpiMode(Lwjgl3ApplicationConfiguration.HdpiMode.Pixels);

        if (consoleLogger != null) {
            consoleLogger.unsubscribe();
            consoleLogger = null;
        }

        // Launch app
        Lwjgl3Application app = new Lwjgl3Application(new GaiaSky(gsargs.download, gsargs.catalogChooser), cfg);
        app.addLifecycleListener(new GaiaSkyWindowListener());
    }

    @Override
    public void notify(Events event, final Object... data) {
        switch (event) {
        case SCENE_GRAPH_LOADED:
            if (REST_ENABLED) {
                /*
                 * Notify REST server that GUI is loaded and everything should be in a
                 * well-defined state
                 */
                Method activate;
                try {
                    activate = REST_SERVER_CLASS.getMethod("activate");
                    activate.invoke(null, new Object[0]);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    logger.error(e);
                }
            }
            break;
        case DISPOSE:
            if (REST_ENABLED) {
                /* Shutdown REST server thread on termination */
                try {
                    Method stop = REST_SERVER_CLASS.getMethod("stop");
                    stop.invoke(null, new Object[0]);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    logger.error(e);
                }
            }
            break;
        default:
            break;
        }

    }

    /**
     * Initialises the configuration file. Tries to load first the file in
     * <code>$HOME/.gaiasky/global.properties</code>. Checks the
     * <code>properties.version</code> key to determine whether the file is
     * compatible or not. If it is, it uses the existing file. If it is not, it
     * replaces it with the default file.
     *
     * @param ow Whether to overwrite
     * @return The path of the file used
     * @throws IOException
     */
    private static String initConfigFile(boolean ow) throws IOException {
        // Use user folder
        File userFolderConfFile = new File(SysUtils.getConfigDir(), "global.vr.properties");

        // Internal config
        File confFolder = new File("conf" + File.separator);
        File internalFolderConfFile = new File(confFolder, "global.vr.properties");

        boolean overwrite = ow;
        if (userFolderConfFile.exists()) {
            Properties userprops = new Properties();
            userprops.load(new FileInputStream(userFolderConfFile));
            int internalversion = SOURCE_CONF_VERSION;
            if (internalFolderConfFile.exists()) {
                Properties internalprops = new Properties();
                internalprops.load(new FileInputStream(internalFolderConfFile));
                internalversion = Integer.parseInt(internalprops.getProperty("properties.version"));
            }

            // Check latest version
            if (!userprops.containsKey("properties.version") || Integer.parseInt(userprops.getProperty("properties.version")) < internalversion) {
                System.out.println("Properties file version mismatch, overwriting with new version: found " + Integer.parseInt(userprops.getProperty("properties.version")) + ", required " + internalversion);
                overwrite = true;
            }
        }

        if (overwrite || !userFolderConfFile.exists()) {
            // Copy file
            if (confFolder.exists() && confFolder.isDirectory()) {
                // Running released package
                copyFile(internalFolderConfFile, userFolderConfFile, overwrite);
            } else {
                // Running from code?
                if (!new File("../assets/conf" + File.separator).exists()) {
                    throw new IOException("File ../assets/conf does not exist!");
                }
                copyFile(new File("../assets/conf" + File.separator + "global.vr.properties"), userFolderConfFile, overwrite);
            }
        }
        String props = userFolderConfFile.getAbsolutePath();
        System.setProperty("properties.file", props);
        return props;
    }

    /**
     * Checks whether the REST server dependencies are in the classpath.
     *
     * @return True if REST dependencies are loaded.
     */
    private static boolean checkRestDepsInClasspath() {
        try {
            Class.forName("com.google.gson.Gson");
            Class.forName("spark.Spark");
            Class.forName("gaia.cu9.ari.gaiaorbit.rest.RESTServer");
            return true;
        } catch (ClassNotFoundException e) {
            // my class isn't there!
            return false;
        }
    }

    @SuppressWarnings("resource")
    private static void copyFile(File sourceFile, File destFile, boolean ow) throws IOException {
        if (destFile.exists()) {
            if (ow) {
                // Overwrite, delete file
                destFile.delete();
            } else {
                return;
            }
        }
        // Create new
        destFile.createNewFile();

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
     * Checks for incompatibilities between the java version and the OS. Prints the necessary warnings for known issues.
     */
    private static void javaVersionCheck() {
        double jv = getVersion();
        boolean linux = SysUtils.isLinux();
        boolean gnome = SysUtils.checkGnome();
        if (jv >= 10 && linux && gnome) {
            System.out.println("======================================= WARNING ========================================");
            System.out.println("It looks like you are running Gaia Sky with java " + jv + " in Linux with Gnome.\n" + "This version may crash. If it does, comment out the property\n" + "'assistive_technologies' in the '/etc/java-[version]/accessibility.properties' file.");
            System.out.println("========================================================================================");
            System.out.println();
        }
    }

    private static double getVersion() {
        String version = System.getProperty("java.version");
        int pos = version.indexOf('.');
        pos = version.indexOf('.', pos + 1);
        return Double.parseDouble(version.substring(0, pos)); //-V6009
    }

    private class GaiaSkyWindowListener implements LifecycleListener {

        @Override
        public void pause() {

        }

        @Override
        public void resume() {

        }

        @Override
        public void dispose() {
            // Terminate here
        }
    }
}
