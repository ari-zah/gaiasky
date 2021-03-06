/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import gaiasky.ErrorDialog;
import gaiasky.GaiaSky;
import gaiasky.data.DesktopSceneGraphImplementationProvider;
import gaiasky.data.SceneGraphImplementationProvider;
import gaiasky.desktop.format.DesktopDateFormatFactory;
import gaiasky.desktop.format.DesktopNumberFormatFactory;
import gaiasky.desktop.render.DesktopPostProcessorFactory;
import gaiasky.desktop.render.ScreenModeCmd;
import gaiasky.desktop.util.CrashReporter;
import gaiasky.desktop.util.DesktopConfInit;
import gaiasky.desktop.util.DesktopMusicActors;
import gaiasky.desktop.util.SysUtils;
import gaiasky.desktop.util.camera.CamRecorder;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interafce.ConsoleLogger;
import gaiasky.interafce.KeyBindings;
import gaiasky.interafce.MusicActorsManager;
import gaiasky.render.PostProcessorFactory;
import gaiasky.rest.RESTServer;
import gaiasky.screenshot.ScreenshotsManager;
import gaiasky.util.*;
import gaiasky.util.GlobalConf.SceneConf.ElevationType;
import gaiasky.util.Logger.Log;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.math.MathManager;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Main class for the Gaia Sky desktop and VR launcher.
 */
public class GaiaSkyDesktop implements IObserver {
    private static final Log logger = Logger.getLogger(GaiaSkyDesktop.class);

    /*
     * Source version to compare to config file and datasets.
     * This is usually tag where each number is allocated 2 digits.
     * Version = major.minor.rev -> 1.2.5 major=1; minor=2; rev=5
     * Version = major * 10000 + minor * 100 + rev
     * So 1.2.5 -> 10205
     *    2.1.7 -> 20107
     *
     * Leading zeroes are omitted to avoid octal literal interpretation.
     */
    public static int SOURCE_VERSION = 30105;
    private static GaiaSkyDesktop gsd;
    private static boolean REST_ENABLED = false;
    private static boolean JAVA_VERSION_FLAG = false;

    private static final String REQUIRED_JAVA_VERSION = "11";

    private static GaiaSkyArgs gsArgs;

    private static final int DEFAULT_OPENGL_MAJOR = 4;
    private static final int DEFAULT_OPENGL_MINOR = 1;
    private static final String DEFAULT_OPENGL = DEFAULT_OPENGL_MAJOR + "." + DEFAULT_OPENGL_MINOR;

    private static final int MIN_OPENGL_MAJOR = 3;
    private static final int MIN_OPENGL_MINOR = 2;
    private static final String MIN_OPENGL = MIN_OPENGL_MAJOR + "." + MIN_OPENGL_MINOR;
    private static final int MIN_GLSL_MAJOR = 3;
    private static final int MIN_GLSL_MINOR = 3;
    private static final String MIN_GLSL = MIN_GLSL_MAJOR + "." + MIN_GLSL_MINOR;

    /**
     * Program arguments
     */
    private static class GaiaSkyArgs {
        @Parameter(names = { "-h", "--help" }, description = "Show program options and usage information.", help = true, order = 0)
        private boolean help = false;

        @Parameter(names = { "-v", "--version" }, description = "List Gaia Sky version and relevant information.", order = 1)
        private boolean version = false;

        @Parameter(names = { "-i", "--asciiart" }, description = "Add nice ascii art to --version information.", order = 1)
        private boolean asciiart = false;

        @Parameter(names = { "-s", "--skip-welcome" }, description = "Skip the welcome screen if possible (base-data package must be present).", order = 2)
        private boolean skipWelcome = false;

        @Parameter(names = { "-p", "--properties" }, description = "Specify the location of the properties file.", order = 4)
        private String propertiesFile = null;

        @Parameter(names = { "-a", "--assets" }, description = "Specify the location of the assets folder. If not present, the default assets location (in the installation folder) is used.", order = 5)
        private String assetsLocation = null;

        @Parameter(names = { "-vr", "--openvr" }, description = "Launch in Virtual Reality mode. Gaia Sky will attempt to create a VR context through OpenVR.", order = 6)
        private boolean vr = false;

        @Parameter(names = { "-e", "--externalview" }, description = "Create a window with a view of the scene and no UI.", order = 7)
        private boolean externalView = false;

        @Parameter(names = { "-n", "--noscript" }, description = "Do not start the scripting server. Useful to run more than one Gaia Sky instance at once in the same machine.", order = 8)
        private boolean noScriptingServer = false;

        @Parameter(names = { "-d", "--debug" }, description = "Launch in debug mode. Prints out debug information from Gaia Sky to the logs.", order = 9)
        private boolean debug = false;

        @Parameter(names = { "-g", "--gpudebug" }, description = "Activate OpenGL debug mode. Prints out debug information from OpenGL to the standard output.", order = 9)
        private boolean debugGpu = false;

        @Parameter(names = { "--safemode" }, description = "Activate safe graphics mode. This forces the creation of an OpenGL 3.2 context, and disables float buffers and tessellation.", order = 10)
        private boolean safeMode = false;
    }

    /**
     * Formats the regular usage so that it removes the left padding characters.
     * This is necessary so that help2man recognizes the OPTIONS block.
     *
     * @param jc The JCommander object
     */
    private static void printUsage(JCommander jc) {
        jc.usage();
    }

    /**
     * UTF-8 output stream printer
     **/
    private static PrintStream out;

    /**
     * Main method
     *
     * @param args Arguments
     */
    public static void main(String[] args) {
        Thread.currentThread().setName("gaiasky-main-thread");
        out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        gsArgs = new GaiaSkyArgs();
        JCommander jc = JCommander.newBuilder().addObject(gsArgs).build();
        jc.setProgramName("gaiasky");
        try {
            jc.parse(args);

            if (gsArgs.help) {
                printUsage(jc);
                return;
            }
        } catch (Exception e) {
            out.print("gaiasky: bad program arguments\n\n");
            printUsage(jc);
            return;
        }

        try {
            // Check java version
            javaVersionCheck();

            // Experimental features
            experimentalCheck();

            // Set properties file from arguments to VM params if needed
            if (gsArgs.propertiesFile != null && !gsArgs.propertiesFile.isEmpty()) {
                System.setProperty("properties.file", gsArgs.propertiesFile);
            }

            // Set assets location to VM params if needed
            if (gsArgs.assetsLocation != null && !gsArgs.assetsLocation.isEmpty()) {
                System.setProperty("assets.location", gsArgs.assetsLocation);
            }

            if (gsArgs.vr) {
                GlobalConf.APPLICATION_NAME += " VR";
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
                initConfigFile(false, gsArgs.vr);
            }

            // Initialize i18n (only for global config logging)
            I18n.initialize(Gdx.files.internal("i18n/gsbundle"));

            // Init global configuration
            ConfInit.initialize(new DesktopConfInit(gsArgs.vr));

            // Safe mode
            if (gsArgs.safeMode && !GlobalConf.program.SAFE_GRAPHICS_MODE) {
                GlobalConf.program.SAFE_GRAPHICS_MODE = true;
                GlobalConf.program.SAFE_GRAPHICS_MODE_FLAG = true;
            }

            // Reinitialize with user-defined locale
            I18n.initialize(Gdx.files.absolute(GlobalConf.ASSETS_LOC + File.separator + "i18n/gsbundle"));

            if (gsArgs.version) {
                out.println(GlobalConf.getShortApplicationName());
                if (gsArgs.asciiart) {
                    BufferedReader ascii = new BufferedReader(new InputStreamReader(Gdx.files.internal("icon/gsascii.txt").read()));
                    out.println();
                    String line;
                    while ((line = ascii.readLine()) != null) {
                        out.println(line);
                    }
                }
                out.println();
                out.println("License MPL 2.0: Mozilla Public License 2.0 <https://www.mozilla.org/en-US/MPL/2.0/>");
                out.println("Written by " + GlobalConf.AUTHOR_NAME + " <" + GlobalConf.AUTHOR_EMAIL + ">");
                out.println();
                out.println(I18n.txt("gui.help.javaversion").toLowerCase() + ": " + System.getProperty("java.vm.version"));
                out.println(I18n.txt("gui.help.javavmname").toLowerCase() + ": " + System.getProperty("java.vm.name"));
                out.println();
                out.println("gaiasky homepage  <" + GlobalConf.WEBPAGE + ">");
                out.println("docs              <" + GlobalConf.DOCUMENTATION + ">");
                out.println();
                out.println("ZAH/DLR/BWT/DPAC");
                return;
            }

            ConsoleLogger consoleLogger = new ConsoleLogger();

            // REST API server
            REST_ENABLED = GlobalConf.program.REST_PORT >= 0 && checkRestDepsInClasspath();
            if (REST_ENABLED) {
                RESTServer.initialize(GlobalConf.program.REST_PORT);
            }

            // Slave manager
            SlaveManager.initialize();

            // Fullscreen command
            ScreenModeCmd.initialize();

            // Init cam recorder
            CamRecorder.initialize();

            // Music actors
            MusicActorsManager.initialize(new DesktopMusicActors());

            // Init music manager
            MusicManager.initialize(Paths.get(GlobalConf.ASSETS_LOC, "music"), SysUtils.getDefaultMusicDir());

            // Initialize post processor factory
            PostProcessorFactory.initialize(new DesktopPostProcessorFactory());

            // Key mappings
            KeyBindings.initialize();

            // Scene graph implementation provider
            SceneGraphImplementationProvider.initialize(new DesktopSceneGraphImplementationProvider());

            // Math
            MathManager.initialize();

            consoleLogger.dispose();

            gsd.init();
        } catch (Exception e) {
            CrashReporter.reportCrash(e, logger);
        }
    }

    public GaiaSkyDesktop() {
        super();
        EventManager.instance.subscribe(this, Events.SCENE_GRAPH_LOADED, Events.DISPOSE);
    }

    private void init() {
        launchMainApp();
    }

    private GaiaSky gs;

    public void launchMainApp() {
        ConsoleLogger consoleLogger = new ConsoleLogger();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle(GlobalConf.APPLICATION_NAME);
        if (!gsArgs.vr) {
            if (GlobalConf.screen.FULLSCREEN) {
                // Fullscreen mode
                DisplayMode[] modes = Lwjgl3ApplicationConfiguration.getDisplayModes();
                DisplayMode mymode = null;
                for (DisplayMode mode : modes) {
                    if (mode.height == GlobalConf.screen.FULLSCREEN_HEIGHT && mode.width == GlobalConf.screen.FULLSCREEN_WIDTH) {
                        mymode = mode;
                        break;
                    }
                }
                if (mymode == null) {
                    // Fall back to windowed
                    logger.warn(I18n.txt("error.fullscreen.notfound", GlobalConf.screen.FULLSCREEN_WIDTH, GlobalConf.screen.FULLSCREEN_HEIGHT));
                    cfg.setWindowedMode(GlobalConf.screen.getScreenWidth(), GlobalConf.screen.getScreenHeight());
                    cfg.setResizable(GlobalConf.screen.RESIZABLE);
                } else {
                    cfg.setFullscreenMode(mymode);
                }
            } else {
                // Windowed mode
                configureWindowSize(cfg);
                cfg.setResizable(GlobalConf.screen.RESIZABLE);
            }
            cfg.useVsync(GlobalConf.screen.VSYNC);
        } else {
            // Note that we disable VSync! The VRContext manages vsync with respect to the HMD
            cfg.useVsync(false);
            // Always windowed, actual render sent to headset
            configureWindowSize(cfg);
            cfg.setResizable(true);
        }
        if (gsArgs.vr) {
            cfg.setWindowIcon(FileType.Internal, "icon/gsvr_icon.png");
        } else {
            cfg.setWindowIcon(FileType.Internal, "icon/gs_icon.png");
        }
        cfg.useOpenGL3(true, DEFAULT_OPENGL_MAJOR, DEFAULT_OPENGL_MINOR);
        // Disable logical DPI modes (macOS, Windows)
        cfg.setHdpiMode(HdpiMode.Pixels);
        // OpenGL debug
        if (gsArgs.debugGpu) {
            cfg.enableGLDebugOutput(true, System.out);
        }
        // Color, Depth, stencil buffers, MSAA
        cfg.setBackBufferConfig(8, 8, 8, 8, 24, 8, 0);

        // Launch app
        try {
            if (GlobalConf.program.SAFE_GRAPHICS_MODE) {
                setSafeMode(cfg);
            }
            consoleLogger.unsubscribe();

            runGaiaSky(cfg);
        } catch (GdxRuntimeException e) {
            checkLogger(consoleLogger);
            logger.error(e);
            if(gs != null) {
                gs.setCrashed(true);
                try {
                    gs.dispose();
                } catch (Exception e1) {
                    logger.error(I18n.txt("error.dispose"), e1);
                }
            }
            if (!JAVA_VERSION_FLAG) {
                if (gs != null && !gs.windowCreated) {
                    // Probably, OpenGL 4.x is not supported and window creation failed
                    logger.error(I18n.txt("error.windowcreation", DEFAULT_OPENGL, MIN_OPENGL));
                    setSafeMode(cfg);
                    consoleLogger.unsubscribe();

                    try {
                        runGaiaSky(cfg);
                    } catch (GdxRuntimeException e1) {
                        logger.error(I18n.txt("error.opengl", MIN_OPENGL, MIN_GLSL));
                        showDialogOGL(e, I18n.txt("dialog.opengl.title"), I18n.txt("dialog.opengl.message", MIN_OPENGL, MIN_GLSL));
                    }
                } else {
                    logger.error(I18n.txt("error.crash", GlobalConf.REPO_ISSUES, SysUtils.getCrashReportsDir()));
                    showDialogOGL(e, I18n.txt("error.crash.title"), I18n.txt("error.crash", GlobalConf.REPO_ISSUES, SysUtils.getCrashReportsDir()));
                }
            } else {
                logger.error(I18n.txt("error.java", REQUIRED_JAVA_VERSION));
                showDialogOGL(e, I18n.txt("dialog.java.title"), I18n.txt("dialog.java.message", REQUIRED_JAVA_VERSION));
            }
        } catch (Exception e) {
            logger.error(e);
            showDialogOGL(e, I18n.txt("error.crash.title"), I18n.txt("error.crash.exception", e, GlobalConf.REPO_ISSUES, SysUtils.getCrashReportsDir()));
        }
    }

    private void configureWindowSize(Lwjgl3ApplicationConfiguration cfg) {
        int w = GlobalConf.screen.getScreenWidth();
        int h = GlobalConf.screen.getScreenHeight();
        if (!SysUtils.isMac()) {
            // Graphics device method
            if (w <= 0 || h <= 0) {
                try {
                    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                    GraphicsConfiguration gc = gd.getDefaultConfiguration();
                    w = (int) (gc.getBounds().getWidth() * 0.85f);
                    h = (int) (gc.getBounds().getHeight() * 0.85f);
                    GlobalConf.screen.SCREEN_WIDTH = w;
                    GlobalConf.screen.SCREEN_HEIGHT = h;
                } catch (HeadlessException he) {
                    logger.error(I18n.txt("error.screensize.gd"));
                    logger.debug(he);
                }
            }
            // Toolkit method
            if (w <= 0 || h <= 0) {
                try {
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    w = (int) (screenSize.width * 0.85f);
                    h = (int) (screenSize.height * 0.85f);
                    GlobalConf.screen.SCREEN_WIDTH = w;
                    GlobalConf.screen.SCREEN_HEIGHT = h;
                } catch (Exception e) {
                    // Default
                    w = 1600;
                    h = 900;
                    GlobalConf.screen.SCREEN_WIDTH = w;
                    GlobalConf.screen.SCREEN_HEIGHT = h;
                    logger.error(I18n.txt("error.screensize.toolkit", w, h));
                    logger.debug(e);
                }
            }
        } else {
            // macOS is retarded and only likes headless mode, using default
            w = 1600;
            h = 900;
            GlobalConf.screen.SCREEN_WIDTH = w;
            GlobalConf.screen.SCREEN_HEIGHT = h;
        }
        cfg.setWindowedMode(w, h);
    }

    private void runGaiaSky(Lwjgl3ApplicationConfiguration cfg) {
        gs = new GaiaSky(gsArgs.skipWelcome, gsArgs.vr, gsArgs.externalView, gsArgs.noScriptingServer, gsArgs.debug);
        new Lwjgl3Application(gs, cfg);
    }

    private void setSafeMode(Lwjgl3ApplicationConfiguration cfg) {
        logger.info(I18n.txt("startup.safe.enable", MIN_OPENGL, MIN_GLSL));
        GlobalConf.scene.ELEVATION_TYPE = ElevationType.NONE;
        GlobalConf.program.SAFE_GRAPHICS_MODE = true;
        cfg.useOpenGL3(true, MIN_OPENGL_MAJOR, MIN_OPENGL_MINOR);
    }

    private void showDialogOGL(Exception ex, String title, String message) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setHdpiMode(HdpiMode.Pixels);
        cfg.useVsync(true);
        cfg.setWindowedMode(1300, 450);
        cfg.setResizable(false);
        cfg.setTitle(title);

        new Lwjgl3Application(new ErrorDialog(ex, message), cfg);
    }

    private static void checkLogger(ConsoleLogger consoleLogger) {
        EventManager.instance.clearAllSubscriptions();
        consoleLogger.subscribe();
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
                try {
                    RESTServer.activate();
                } catch (SecurityException | IllegalArgumentException e) {
                    logger.error(e);
                }
            }
            break;
        case DISPOSE:
            if (REST_ENABLED) {
                /* Shutdown REST server thread on termination */
                try {
                    RESTServer.dispose();
                } catch (SecurityException | IllegalArgumentException e) {
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
     * <code>$GS_CONFIG_DIR/global.properties</code>. Checks the
     * <code>properties.version</code> key and compares it with the version in
     * the default configuration file of this release
     * to determine whether the config file must be overwritten
     *
     * @param ow Whether to force overwrite
     * @return The path of the file used
     * @throws IOException
     */
    private static String initConfigFile(boolean ow, boolean vr) throws IOException {
        // Use user folder
        Path userFolderConfFile = SysUtils.getConfigDir().resolve(DesktopConfInit.getConfigFileName(vr));

        // Internal config
        Path confFolder = GlobalConf.assetsPath("conf");
        Path internalFolderConfFile = confFolder.resolve(DesktopConfInit.getConfigFileName(vr));

        boolean overwrite = ow;
        boolean userConfExists = Files.exists(userFolderConfFile);
        if (userConfExists) {
            Properties userProps = new Properties();
            userProps.load(Files.newInputStream(userFolderConfFile));
            int internalVersion = 0;
            if (Files.exists(internalFolderConfFile)) {
                Properties internalProps = new Properties();
                internalProps.load(Files.newInputStream(internalFolderConfFile));
                internalVersion = Integer.parseInt(internalProps.getProperty("properties.version"));
            }

            // Check latest version
            if (!userProps.containsKey("properties.version")) {
                out.println("Properties file version not found, overwriting with new version (" + internalVersion + ")");
                overwrite = true;
            } else if (Integer.parseInt(userProps.getProperty("properties.version")) < internalVersion) {
                out.println("Properties file version mismatch, overwriting with new version: found " + Integer.parseInt(userProps.getProperty("properties.version")) + ", required " + internalVersion);
                overwrite = true;
            }
        }

        if (overwrite || !userConfExists) {
            // Copy file
            if (Files.exists(confFolder) && Files.isDirectory(confFolder)) {
                // Running released package
                GlobalResources.copyFile(internalFolderConfFile, userFolderConfFile, overwrite);
            } else {
                logger.warn("Configuration folder does not exist: " + confFolder.toString());
            }
        }
        String props = userFolderConfFile.toAbsolutePath().toString();
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
            Class.forName("spark.Spark");
            Class.forName("gaiasky.rest.RESTServer");
            return true;
        } catch (ClassNotFoundException e) {
            // my class isn't there!
            return false;
        }
    }

    /**
     * Checks for incompatibilities between the java version and the OS. Prints the necessary warnings for known issues.
     */
    private static void javaVersionCheck() {
        double jv = SysUtils.getJavaVersion();
        boolean linux = SysUtils.isLinux();
        boolean gnome = SysUtils.checkGnome();
        if (jv >= 10 && linux && gnome) {
            out.println("======================================= WARNING ========================================");
            out.println("It looks like you are running Gaia Sky with java " + jv + " in Linux with Gnome.\n" + "This version may crash. If it does, comment out the property\n" + "'assistive_technologies' in the '/etc/java-[version]/accessibility.properties' file.");
            out.println("========================================================================================");
            out.println();
        }

        if (jv < 9) {
            out.println("========================== ERROR ==============================");
            out.println("You are using Java " + jv + ", which is unsupported by Gaia Sky");
            out.println("             Please, use at least Java " + REQUIRED_JAVA_VERSION);
            out.println("===============================================================");
            JAVA_VERSION_FLAG = true;
        }
    }

    /**
     * Checks for experimental features and issues warnings
     */
    private static void experimentalCheck() {
        if (gsArgs.externalView) {
            out.println("============================ WARNING ================================");
            out.println("The -e/--externalview feature is experimental and may cause problems!");
            out.println("=====================================================================");
            out.println();
        }
    }
}
