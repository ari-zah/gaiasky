package gaia.cu9.ari.gaiaorbit.desktop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.brsanthu.googleanalytics.GoogleAnalyticsResponse;

import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.analytics.AnalyticsPermission;
import gaia.cu9.ari.gaiaorbit.analytics.AnalyticsReporting;
import gaia.cu9.ari.gaiaorbit.data.DesktopSceneGraphImplementationProvider;
import gaia.cu9.ari.gaiaorbit.data.SceneGraphImplementationProvider;
import gaia.cu9.ari.gaiaorbit.desktop.concurrent.MultiThreadIndexer;
import gaia.cu9.ari.gaiaorbit.desktop.concurrent.MultiThreadLocalFactory;
import gaia.cu9.ari.gaiaorbit.desktop.concurrent.ThreadPoolManager;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopNumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.render.DesktopPostProcessorFactory;
import gaia.cu9.ari.gaiaorbit.desktop.render.ScreenModeCmd;
import gaia.cu9.ari.gaiaorbit.desktop.util.CamRecorder;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopConfInit;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopMusicActors;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopNetworkChecker;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopSysUtilsFactory;
import gaia.cu9.ari.gaiaorbit.desktop.util.MemInfoWindow;
import gaia.cu9.ari.gaiaorbit.desktop.util.RunCameraWindow;
import gaia.cu9.ari.gaiaorbit.desktop.util.RunScriptWindow;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.KeyBindings;
import gaia.cu9.ari.gaiaorbit.interfce.MusicActorsManager;
import gaia.cu9.ari.gaiaorbit.interfce.NetworkCheckerManager;
import gaia.cu9.ari.gaiaorbit.render.PostProcessorFactory;
import gaia.cu9.ari.gaiaorbit.screenshot.ScreenshotsManager;
import gaia.cu9.ari.gaiaorbit.script.JythonFactory;
import gaia.cu9.ari.gaiaorbit.script.ScriptingFactory;
import gaia.cu9.ari.gaiaorbit.util.ConfInit;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.MusicManager;
import gaia.cu9.ari.gaiaorbit.util.SysUtilsFactory;
import gaia.cu9.ari.gaiaorbit.util.concurrent.SingleThreadIndexer;
import gaia.cu9.ari.gaiaorbit.util.concurrent.SingleThreadLocalFactory;
import gaia.cu9.ari.gaiaorbit.util.concurrent.ThreadIndexer;
import gaia.cu9.ari.gaiaorbit.util.concurrent.ThreadLocalFactory;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.math.MathManager;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;

/**
 * Main class for the desktop launcher
 * 
 * @author Toni Sagrista
 *
 */
public class GaiaSkyDesktop implements IObserver {
    private static GaiaSkyDesktop gsd;
    public static String ASSETS_LOC;

    private MemInfoWindow memInfoWindow;

    public static void main(String[] args) {

        try {
            gsd = new GaiaSkyDesktop();
            // Assets location
            ASSETS_LOC = (System.getProperty("assets.location") != null ? System.getProperty("assets.location") : "");

            Gdx.files = new Lwjgl3Files();

            // Sys utils
            SysUtilsFactory.initialize(new DesktopSysUtilsFactory());

            // Initialize number format
            NumberFormatFactory.initialize(new DesktopNumberFormatFactory());

            // Initialize date format
            DateFormatFactory.initialize(new DesktopDateFormatFactory());

            // Init .gaiasky folder in user's home folder
            initUserDirectory();

            // Init properties file
            String props = System.getProperty("properties.file");
            if (props == null || props.isEmpty()) {
                props = initConfigFile(false);
            }

            // Init global configuration
            ConfInit.initialize(new DesktopConfInit(ASSETS_LOC));

            // Initialize i18n
            I18n.initialize(Gdx.files.internal("i18n/gsbundle"));

            // Dev mode
            I18n.initialize(Gdx.files.absolute(ASSETS_LOC + "i18n/gsbundle"));

            // Jython
            ScriptingFactory.initialize(JythonFactory.getInstance());

            // Fullscreen command
            ScreenModeCmd.initialize();

            // Init cam recorder
            CamRecorder.initialize();

            // Music actors
            MusicActorsManager.initialize(new DesktopMusicActors());

            // Init music manager
            MusicManager.initialize(Gdx.files.absolute(ASSETS_LOC + "music"), Gdx.files.absolute(SysUtilsFactory.getSysUtils().getDefaultMusicDir().getAbsolutePath()));

            // Initialize post processor factory
            PostProcessorFactory.initialize(new DesktopPostProcessorFactory());

            // Key mappings
            Constants.desktop = true;
            KeyBindings.initialize();

            // Scene graph implementation provider
            SceneGraphImplementationProvider.initialize(new DesktopSceneGraphImplementationProvider());

            // Initialize screenshots manager
            ScreenshotsManager.initialize();

            // Network checker
            NetworkCheckerManager.initialize(new DesktopNetworkChecker());

            // Analytics
            AnalyticsReporting.initialize(new AnalyticsPermission());
            AnalyticsReporting.getInstance().sendStartAppReport();

            // Math
            MathManager.initialize();

            gsd.init();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

    }

    public static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value != null && value instanceof javax.swing.plaf.FontUIResource && ((FontUIResource) value).getSize() > f.getSize()) {
                UIManager.put(key, f);
            }
        }
    }

    public GaiaSkyDesktop() {
        super();
        EventManager.instance.subscribe(this, Events.SHOW_ABOUT_ACTION, Events.SHOW_RUNSCRIPT_ACTION, Events.JAVA_EXCEPTION, Events.SHOW_PLAYCAMERA_ACTION, Events.DISPLAY_MEM_INFO_WINDOW);
    }

    private void init() {
        launchMainApp();
    }

    public void terminate() {
        System.exit(0);
    }

    public void launchMainApp() {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle(GlobalConf.getFullApplicationName());

        if (GlobalConf.screen.FULLSCREEN) {
            Monitor m = Lwjgl3ApplicationConfiguration.getPrimaryMonitor();

            // Get mode
            DisplayMode[] modes = Lwjgl3ApplicationConfiguration.getDisplayModes(m);
            DisplayMode mymode = null;
            for (DisplayMode mode : modes) {
                if (mode.height == GlobalConf.screen.FULLSCREEN_HEIGHT && mode.width == GlobalConf.screen.FULLSCREEN_WIDTH) {
                    mymode = mode;
                    break;
                }
            }
            if (mymode == null)
                mymode = Lwjgl3ApplicationConfiguration.getDisplayMode(Gdx.graphics.getPrimaryMonitor());
            cfg.setFullscreenMode(mymode);
        } else {
            // First monitor
            Monitor[] monitors = Lwjgl3ApplicationConfiguration.getMonitors();
            Monitor m = monitors[0];
            // Find out position
            DisplayMode dm = Lwjgl3ApplicationConfiguration.getDisplayMode(m);
            int posx = m.virtualX + dm.width / 2 - GlobalConf.screen.getScreenWidth() / 2;
            int posy = m.virtualY + dm.height / 2 - GlobalConf.screen.getScreenHeight() / 2;
            cfg.setWindowedMode(GlobalConf.screen.getScreenWidth(), GlobalConf.screen.getScreenHeight());
            cfg.setResizable(GlobalConf.screen.RESIZABLE);
            cfg.setWindowPosition(posx, posy);
        }
        cfg.setBackBufferConfig(8, 8, 8, 8, 24, 0, MathUtilsd.clamp(GlobalConf.postprocess.POSTPROCESS_ANTIALIAS, 0, 16));
        cfg.setIdleFPS(0);
        cfg.useVsync(GlobalConf.screen.VSYNC);
        cfg.setWindowIcon(Files.FileType.Internal, "icon/ic_launcher.png");
        cfg.setWindowListener(new GaiaSkyWindowListener());

        Logger.info("Display mode set to " + Lwjgl3ApplicationConfiguration.getDisplayMode().width + "x" + Lwjgl3ApplicationConfiguration.getDisplayMode().height + ", fullscreen: " + GlobalConf.screen.FULLSCREEN);

        // Thread pool manager
        if (GlobalConf.performance.MULTITHREADING) {
            ThreadIndexer.initialize(new MultiThreadIndexer());
            ThreadPoolManager.initialize(GlobalConf.performance.NUMBER_THREADS());
            ThreadLocalFactory.initialize(new MultiThreadLocalFactory());
        } else {
            ThreadIndexer.initialize(new SingleThreadIndexer());
            ThreadLocalFactory.initialize(new SingleThreadLocalFactory());
        }

        // Launch app
        new Lwjgl3Application(new GaiaSky(), cfg);

        EventManager.instance.unsubscribe(this, Events.POST_NOTIFICATION, Events.JAVA_EXCEPTION);
    }

    RunScriptWindow scriptWindow = null;
    RunCameraWindow cameraWindow = null;

    @Override
    public void notify(Events event, final Object... data) {
        switch (event) {
        case SHOW_PLAYCAMERA_ACTION:
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    if (cameraWindow == null)
                        cameraWindow = new RunCameraWindow((Stage) data[0], (Skin) data[1]);
                    cameraWindow.display();
                }
            });
            break;
        case SHOW_RUNSCRIPT_ACTION:
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    if (scriptWindow == null)
                        scriptWindow = new RunScriptWindow((Stage) data[0], (Skin) data[1]);
                    scriptWindow.display();
                }
            });

            break;
        case DISPLAY_MEM_INFO_WINDOW:
            if (memInfoWindow == null) {
                memInfoWindow = new MemInfoWindow((Stage) data[0], (Skin) data[1]);
            }
            memInfoWindow.show((Stage) data[0]);
            break;
        case SHOW_ABOUT_ACTION:
            // Exit fullscreen
            // EventManager.instance.post(Events.FULLSCREEN_CMD, false);
            // Gdx.app.postRunnable(new Runnable() {
            //
            // @Override
            // public void run() {
            // JFrame frame = new HelpDialog();
            // frame.toFront();
            // }
            //
            // });
            break;
        case JAVA_EXCEPTION:
            ((Throwable) data[0]).printStackTrace(System.err);
            break;
        default:
            break;
        }

    }

    private static void initUserDirectory() {
        SysUtilsFactory.getSysUtils().getGSHomeDir().mkdirs();
        SysUtilsFactory.getSysUtils().getDefaultFramesDir().mkdirs();
        SysUtilsFactory.getSysUtils().getDefaultScreenshotsDir().mkdirs();
        SysUtilsFactory.getSysUtils().getDefaultMusicDir().mkdirs();
        SysUtilsFactory.getSysUtils().getDefaultScriptDir().mkdirs();
        SysUtilsFactory.getSysUtils().getDefaultCameraDir().mkdirs();
    }

    private static String initConfigFile(boolean ow) throws IOException {
        // Use user folder
        File userFolder = SysUtilsFactory.getSysUtils().getGSHomeDir();
        userFolder.mkdirs();
        File userFolderConfFile = new File(userFolder, "global.properties");

        if (ow || !userFolderConfFile.exists()) {
            // Copy file
            File confFolder = new File("conf" + File.separator);
            if (confFolder.exists() && confFolder.isDirectory()) {
                // Running released package
                copyFile(new File("conf" + File.separator + "global.properties"), userFolderConfFile, ow);
            } else {
                // Running from code?
                if (!new File("../android/assets/conf" + File.separator).exists()) {
                    throw new IOException("File ../android/assets/conf does not exist!");
                }
                copyFile(new File("../android/assets/conf" + File.separator + "global.properties"), userFolderConfFile, ow);
            }
        }
        String props = userFolderConfFile.getAbsolutePath();
        System.setProperty("properties.file", props);
        return props;
    }

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

    private class GaiaSkyWindowListener implements Lwjgl3WindowListener {

        @Override
        public void iconified(boolean isIconified) {
        }

        @Override
        public void maximized(boolean isMaximized) {
        }

        @Override
        public void focusLost() {
        }

        @Override
        public void focusGained() {
        }

        @Override
        public boolean closeRequested() {
            // Terminate here

            // Analytics stop event
            Future<GoogleAnalyticsResponse> f1 = AnalyticsReporting.getInstance().sendTimingAppReport();

            try {
                f1.get(2000, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                Logger.error(e);
            }

            return true;
        }

        @Override
        public void filesDropped(String[] files) {
        }

        @Override
        public void refreshRequested() {
        }
    }
}
