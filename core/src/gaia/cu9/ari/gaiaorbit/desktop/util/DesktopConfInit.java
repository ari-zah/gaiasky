package gaia.cu9.ari.gaiaorbit.desktop.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Properties;

import com.badlogic.gdx.Gdx;

import gaia.cu9.ari.gaiaorbit.desktop.GaiaSkyDesktop;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.util.ConfInit;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ControlsConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.DataConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.FrameConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ImageFormat;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.PerformanceConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.PostprocessConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.PostprocessConf.Antialias;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf.StereoProfile;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.RuntimeConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.SceneConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ScreenConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ScreenshotConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ScreenshotMode;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.SpacecraftConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.VersionConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.SysUtilsFactory;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;

/**
 * Desktop GlobalConf initialiser, where the configuration comes from a
 * global.properties file.
 * 
 * @author tsagrista
 *
 */
public class DesktopConfInit extends ConfInit {
    CommentedProperties p;
    Properties vp;

    IDateFormat df = DateFormatFactory.getFormatter("dd/MM/yyyy HH:mm:ss");

    public DesktopConfInit(String assetsLocation) {
        super();
        try {
            String propsFileProperty = System.getProperty("properties.file");
            if (propsFileProperty == null || propsFileProperty.isEmpty()) {
                propsFileProperty = initConfigFile(false);
            }

            File confFile = new File(propsFileProperty);
            InputStream fis = new FileInputStream(confFile);

            // This should work for the normal execution
            InputStream vis = GaiaSkyDesktop.class.getResourceAsStream("/version");
            if (vis == null) {
                // In case of running in 'developer' mode
                vis = new FileInputStream(assetsLocation + "data/dummyversion");
            }
            vp = new Properties();
            vp.load(vis);

            p = new CommentedProperties();
            p.load(fis);

        } catch (Exception e) {
            Logger.error(e);
        }
    }

    public DesktopConfInit(InputStream fis, InputStream vis) {
        super();
        try {
            vp = new Properties();
            vp.load(vis);

            p = new CommentedProperties();
            p.load(fis);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    public void initialiseProperties(File confFile) {
        try {
            InputStream fis = new FileInputStream(confFile);
            p = new CommentedProperties();
            p.load(fis);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    @Override
    public void initGlobalConf() throws Exception {

        /** VERSION CONF **/
        VersionConf vc = new VersionConf();
        String versionStr = vp.getProperty("version");
        DateTimeFormatter df = DateTimeFormatter.ofPattern("EEE MMM dd kk:mm:ss z yyyy", Locale.ENGLISH);
        Instant buildtime = null;
        try {
            buildtime = LocalDateTime.parse(vp.getProperty("buildtime"), df).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            df = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.ENGLISH);
            try {
                buildtime = LocalDateTime.parse(vp.getProperty("buildtime"), df).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e1) {
                Logger.error(e1);
            }
        }
        vc.initialize(versionStr, buildtime, vp.getProperty("builder"), vp.getProperty("system"), vp.getProperty("build"));
        Logger.info("Gaia Sky version " + vc.version + " - build " + vc.build);

        /** PERFORMANCE CONF **/
        PerformanceConf pc = new PerformanceConf();
        boolean MULTITHREADING = Boolean.parseBoolean(p.getProperty("global.conf.multithreading"));
        String propNumthreads = p.getProperty("global.conf.numthreads");
        int NUMBER_THREADS = Integer.parseInt((propNumthreads == null || propNumthreads.isEmpty()) ? "0" : propNumthreads);
        pc.initialize(MULTITHREADING, NUMBER_THREADS);

        /** POSTPROCESS CONF **/
        PostprocessConf ppc = new PostprocessConf();
        Antialias POSTPROCESS_ANTIALIAS = ppc.getAntialias(Integer.parseInt(p.getProperty("postprocess.antialiasing")));
        float POSTPROCESS_BLOOM_INTENSITY = Float.parseFloat(p.getProperty("postprocess.bloom.intensity"));
        float POSTPROCESS_MOTION_BLUR = Float.parseFloat(p.getProperty("postprocess.motionblur"));
        boolean POSTPROCESS_LENS_FLARE = Boolean.parseBoolean(p.getProperty("postprocess.lensflare"));
        boolean POSTPROCESS_LIGHT_SCATTERING = Boolean.parseBoolean(p.getProperty("postprocess.lightscattering", "false"));
        boolean POSTPROCESS_FISHEYE = Boolean.parseBoolean(p.getProperty("postprocess.fisheye", "false"));
        float POSTPROCESS_BRIGHTNESS = Float.parseFloat(p.getProperty("postprocess.brightness", "0"));
        float POSTPROCESS_CONTRAST = Float.parseFloat(p.getProperty("postprocess.contrast", "1"));
        ppc.initialize(POSTPROCESS_ANTIALIAS, POSTPROCESS_BLOOM_INTENSITY, POSTPROCESS_MOTION_BLUR, POSTPROCESS_LENS_FLARE, POSTPROCESS_LIGHT_SCATTERING, POSTPROCESS_FISHEYE, POSTPROCESS_BRIGHTNESS, POSTPROCESS_CONTRAST);

        /** RUNTIME CONF **/
        RuntimeConf rc = new RuntimeConf();
        rc.initialize(true, false, false, false, true, false, 20, false, true);

        /** DATA CONF **/
        DataConf dc = new DataConf();

        String CATALOG_JSON_FILE = p.getProperty("data.json.catalog");

        String OBJECTS_JSON_FILE = p.getProperty("data.json.objects");

        boolean REAL_GAIA_ATTITUDE = Boolean.parseBoolean(p.getProperty("data.attitude.real"));
        boolean HIGH_ACCURACY_POSITIONS = Boolean.parseBoolean(p.getProperty("data.highaccuracy.positions", "false"));

        float LIMIT_MAG_LOAD;
        if (p.getProperty("data.limit.mag") != null && !p.getProperty("data.limit.mag").isEmpty()) {
            LIMIT_MAG_LOAD = Float.parseFloat(p.getProperty("data.limit.mag"));
        } else {
            LIMIT_MAG_LOAD = Float.MAX_VALUE;
        }
        dc.initialize(CATALOG_JSON_FILE, OBJECTS_JSON_FILE, LIMIT_MAG_LOAD, REAL_GAIA_ATTITUDE, HIGH_ACCURACY_POSITIONS);

        /** PROGRAM CONF **/
        ProgramConf prc = new ProgramConf();
        String LOCALE = p.getProperty("program.locale");

        boolean DISPLAY_TUTORIAL = Boolean.parseBoolean(p.getProperty("program.tutorial"));
        String TUTORIAL_POINTER_SCRIPT_LOCATION = p.getProperty("program.tutorial.pointer.script", "scripts/tutorial/tutorial-pointer.py");
        String TUTORIAL_SCRIPT_LOCATION = p.getProperty("program.tutorial.script", "scripts/tutorial/tutorial.py");
        boolean SHOW_DEBUG_INFO = Boolean.parseBoolean(p.getProperty("program.debuginfo"));
        Instant LAST_CHECKED;
        try {
            LAST_CHECKED = LocalDateTime.parse(p.getProperty("program.lastchecked"), df).toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            LAST_CHECKED = null;
        }
        String LAST_VERSION = p.getProperty("program.lastversion", "0.0.0");
        String VERSION_CHECK_URL = p.getProperty("program.versioncheckurl");
        String UI_THEME = p.getProperty("program.ui.theme");
        // Update scale factor according to theme - for HiDPI screens
        GlobalConf.updateScaleFactor(UI_THEME.endsWith("x2") ? 2f : 1f);
        String SCRIPT_LOCATION = p.getProperty("program.scriptlocation").isEmpty() ? System.getProperty("user.dir") + File.separatorChar + "scripts" : p.getProperty("program.scriptlocation");
        int REST_PORT = Integer.parseInt(p.getProperty("program.restport", "-1"));

        boolean STEREOSCOPIC_MODE = Boolean.parseBoolean(p.getProperty("program.stereoscopic"));
        StereoProfile STEREO_PROFILE = StereoProfile.values()[Integer.parseInt(p.getProperty("program.stereoscopic.profile"))];
        boolean CUBEMAPE360_MODE = Boolean.parseBoolean(p.getProperty("program.cubemap360", "false"));
        boolean ANALYTICS_ENABLED = Boolean.parseBoolean(p.getProperty("program.analytics", "true"));
        boolean DISPLAY_HUD = Boolean.parseBoolean(p.getProperty("program.displayhud", "false"));
        boolean DISPLAY_POINTER_COORDS = Boolean.parseBoolean(p.getProperty("program.displaypointercoords", "true"));
        boolean DISPLAY_DATASET_DIALOG = Boolean.parseBoolean(p.getProperty("program.dataset.dialog", "false"));

        prc.initialize(DISPLAY_TUTORIAL, TUTORIAL_POINTER_SCRIPT_LOCATION, TUTORIAL_SCRIPT_LOCATION, SHOW_DEBUG_INFO, LAST_CHECKED, LAST_VERSION, VERSION_CHECK_URL, UI_THEME, SCRIPT_LOCATION, REST_PORT, LOCALE, STEREOSCOPIC_MODE, STEREO_PROFILE, CUBEMAPE360_MODE, ANALYTICS_ENABLED, DISPLAY_HUD, DISPLAY_POINTER_COORDS, DISPLAY_DATASET_DIALOG);

        /** SCENE CONF **/
        int GRAPHICS_QUALITY = Integer.parseInt(p.getProperty("scene.graphics.quality"));
        long OBJECT_FADE_MS = Long.parseLong(p.getProperty("scene.object.fadems"));
        float STAR_BRIGHTNESS = Float.parseFloat(p.getProperty("scene.star.brightness"));
        float AMBIENT_LIGHT = Float.parseFloat(p.getProperty("scene.ambient"));
        int CAMERA_FOV = Integer.parseInt(p.getProperty("scene.camera.fov"));
        int CAMERA_SPEED_LIMIT_IDX = Integer.parseInt(p.getProperty("scene.camera.speedlimit"));
        float CAMERA_SPEED = Float.parseFloat(p.getProperty("scene.camera.focus.vel"));
        boolean FOCUS_LOCK = Boolean.parseBoolean(p.getProperty("scene.focuslock"));
        boolean FOCUS_LOCK_ORIENTATION = Boolean.parseBoolean(p.getProperty("scene.focuslock.orientation", "false"));
        float TURNING_SPEED = Float.parseFloat(p.getProperty("scene.camera.turn.vel"));
        float ROTATION_SPEED = Float.parseFloat(p.getProperty("scene.camera.rotate.vel"));
        float LABEL_NUMBER_FACTOR = Float.parseFloat(p.getProperty("scene.labelfactor"));
        double STAR_TH_ANGLE_QUAD = Double.parseDouble(p.getProperty("scene.star.threshold.quad"));
        double STAR_TH_ANGLE_POINT = Double.parseDouble(p.getProperty("scene.star.threshold.point"));
        double STAR_TH_ANGLE_NONE = Double.parseDouble(p.getProperty("scene.star.threshold.none"));
        float POINT_ALPHA_MIN = Float.parseFloat(p.getProperty("scene.point.alpha.min"));
        float POINT_ALPHA_MAX = Float.parseFloat(p.getProperty("scene.point.alpha.max"));
        int ORBIT_RENDERER = Integer.parseInt(p.getProperty("scene.renderer.orbit", "0"));
        int LINE_RENDERER = Integer.parseInt(p.getProperty("scene.renderer.line"));
        boolean OCTREE_PARTICLE_FADE = Boolean.parseBoolean(p.getProperty("scene.octree.particle.fade"));
        float OCTANT_THRESHOLD_0 = Float.parseFloat(p.getProperty("scene.octant.threshold.0"));
        float OCTANT_THRESHOLD_1 = Float.parseFloat(p.getProperty("scene.octant.threshold.1"));
        boolean PROPER_MOTION_VECTORS = Boolean.parseBoolean(p.getProperty("scene.propermotion.vectors", "true"));
        float PM_NUM_FACTOR = Float.parseFloat(p.getProperty("scene.propermotion.numfactor", "20f"));
        float PM_LEN_FACTOR = Float.parseFloat(p.getProperty("scene.propermotion.lenfactor", "1E1f"));
        boolean GALAXY_3D = Boolean.parseBoolean(p.getProperty("scene.galaxy.3d", "true"));
        boolean CROSSHAIR = Boolean.parseBoolean(p.getProperty("scene.crosshair", "true"));
        boolean CINEMATIC_CAMERA = Boolean.parseBoolean(p.getProperty("scene.camera.cinematic", "false"));
        boolean FREE_CAMERA_TARGET_MODE_ON = Boolean.parseBoolean(p.getProperty("scene.camera.free.targetmode", "false"));
        int CUBEMAP_FACE_RESOLUTION = Integer.parseInt(p.getProperty("scene.cubemapface.resolution", "1000"));
        boolean SHADOW_MAPPING = Boolean.parseBoolean(p.getProperty("scene.shadowmapping", "true"));
        int SHADOW_MAPPING_N_SHADOWS = MathUtilsd.clamp(Integer.parseInt(p.getProperty("scene.shadowmapping.nshadows", "2")), 0, 4);
        int SHADOW_MAPPING_RESOLUTION = Integer.parseInt(p.getProperty("scene.shadowmapping.resolution", "512"));
        // Visibility of components
        ComponentType[] cts = ComponentType.values();
        boolean[] VISIBILITY = new boolean[cts.length];
        for (ComponentType ct : cts) {
            String key = "scene.visibility." + ct.name();
            if (p.containsKey(key)) {
                VISIBILITY[ct.ordinal()] = Boolean.parseBoolean(p.getProperty(key));
            }
        }
        float STAR_POINT_SIZE = Float.parseFloat(p.getProperty("scene.star.point.size", "-1"));
        boolean LAZY_TEXTURE_INIT = true;
        SceneConf sc = new SceneConf();
        sc.initialize(GRAPHICS_QUALITY, OBJECT_FADE_MS, STAR_BRIGHTNESS, AMBIENT_LIGHT, CAMERA_FOV, CAMERA_SPEED, TURNING_SPEED, ROTATION_SPEED, CAMERA_SPEED_LIMIT_IDX, FOCUS_LOCK, FOCUS_LOCK_ORIENTATION, LABEL_NUMBER_FACTOR, VISIBILITY, ORBIT_RENDERER, LINE_RENDERER, STAR_TH_ANGLE_NONE, STAR_TH_ANGLE_POINT, STAR_TH_ANGLE_QUAD, POINT_ALPHA_MIN, POINT_ALPHA_MAX, OCTREE_PARTICLE_FADE, OCTANT_THRESHOLD_0, OCTANT_THRESHOLD_1, PROPER_MOTION_VECTORS, PM_NUM_FACTOR, PM_LEN_FACTOR, STAR_POINT_SIZE, GALAXY_3D, CUBEMAP_FACE_RESOLUTION, CROSSHAIR, CINEMATIC_CAMERA, LAZY_TEXTURE_INIT, FREE_CAMERA_TARGET_MODE_ON, SHADOW_MAPPING, SHADOW_MAPPING_N_SHADOWS, SHADOW_MAPPING_RESOLUTION);

        /** FRAME CONF **/
        String renderFolder = null;
        if (p.getProperty("graphics.render.folder") == null || p.getProperty("graphics.render.folder").isEmpty()) {
            File framesDir = SysUtilsFactory.getSysUtils().getDefaultFramesDir();
            framesDir.mkdirs();
            renderFolder = framesDir.getAbsolutePath();
        } else {
            renderFolder = p.getProperty("graphics.render.folder");
        }
        String RENDER_FOLDER = renderFolder;
        String RENDER_FILE_NAME = p.getProperty("graphics.render.filename");
        int RENDER_WIDTH = Integer.parseInt(p.getProperty("graphics.render.width"));
        int RENDER_HEIGHT = Integer.parseInt(p.getProperty("graphics.render.height"));
        int RENDER_TARGET_FPS = Integer.parseInt(p.getProperty("graphics.render.targetfps", "60"));
        int CAMERA_REC_TARGET_FPS = Integer.parseInt(p.getProperty("graphics.camera.recording.targetfps", "60"));
        boolean AUTO_FRAME_OUTPUT_CAMERA_PLAY = Boolean.parseBoolean(p.getProperty("graphics.camera.recording.frameoutputauto", "false"));
        boolean RENDER_SCREENSHOT_TIME = Boolean.parseBoolean(p.getProperty("graphics.render.time"));

        ScreenshotMode FRAME_MODE = ScreenshotMode.valueOf(p.getProperty("graphics.render.mode"));
        ImageFormat FRAME_FORMAT = ImageFormat.valueOf(p.getProperty("graphcis.render.format", "jpg").toUpperCase());
        float FRAME_QUALITY = Float.parseFloat(p.getProperty("graphics.render.quality", "0.93"));
        FrameConf fc = new FrameConf();
        fc.initialize(RENDER_WIDTH, RENDER_HEIGHT, RENDER_TARGET_FPS, CAMERA_REC_TARGET_FPS, AUTO_FRAME_OUTPUT_CAMERA_PLAY, RENDER_FOLDER, RENDER_FILE_NAME, RENDER_SCREENSHOT_TIME, RENDER_SCREENSHOT_TIME, FRAME_MODE, FRAME_FORMAT, FRAME_QUALITY);

        /** SCREEN CONF **/
        int SCREEN_WIDTH = Integer.parseInt(p.getProperty("graphics.screen.width"));
        int SCREEN_HEIGHT = Integer.parseInt(p.getProperty("graphics.screen.height"));
        int FULLSCREEN_WIDTH = Integer.parseInt(p.getProperty("graphics.screen.fullscreen.width"));
        int FULLSCREEN_HEIGHT = Integer.parseInt(p.getProperty("graphics.screen.fullscreen.height"));
        boolean FULLSCREEN = Boolean.parseBoolean(p.getProperty("graphics.screen.fullscreen"));
        boolean RESIZABLE = Boolean.parseBoolean(p.getProperty("graphics.screen.resizable"));
        boolean VSYNC = Boolean.parseBoolean(p.getProperty("graphics.screen.vsync"));
        boolean SCREEN_OUTPUT = Boolean.parseBoolean(p.getProperty("graphics.screen.screenoutput"));
        ScreenConf scrc = new ScreenConf();
        scrc.initialize(SCREEN_WIDTH, SCREEN_HEIGHT, FULLSCREEN_WIDTH, FULLSCREEN_HEIGHT, FULLSCREEN, RESIZABLE, VSYNC, SCREEN_OUTPUT);

        /** SCREENSHOT CONF **/
        String screenshotFolder = null;
        if (p.getProperty("screenshot.folder") == null || p.getProperty("screenshot.folder").isEmpty()) {
            File screenshotDir = SysUtilsFactory.getSysUtils().getDefaultScreenshotsDir();
            screenshotDir.mkdirs();
            screenshotFolder = screenshotDir.getAbsolutePath();
        } else {
            screenshotFolder = p.getProperty("screenshot.folder");
        }
        String SCREENSHOT_FOLDER = screenshotFolder;
        int SCREENSHOT_WIDTH = Integer.parseInt(p.getProperty("screenshot.width"));
        int SCREENSHOT_HEIGHT = Integer.parseInt(p.getProperty("screenshot.height"));
        ScreenshotMode SCREENSHOT_MODE = ScreenshotMode.valueOf(p.getProperty("screenshot.mode"));
        ImageFormat SCREENSHOT_FORMAT = ImageFormat.valueOf(p.getProperty("screenshot.format", "jpg").toUpperCase());
        float SCREENSHOT_QUALITY = Float.parseFloat(p.getProperty("screenshot.quality", "0.93"));
        ScreenshotConf shc = new ScreenshotConf();
        shc.initialize(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT, SCREENSHOT_FOLDER, SCREENSHOT_MODE, SCREENSHOT_FORMAT, SCREENSHOT_QUALITY);

        /** CONTROLS CONF **/
        ControlsConf cc = new ControlsConf();
        String CONTROLLER_MAPPINGS_FILE = p.getProperty("controls.mappings.file", "mappings/xbox360.controller");
        boolean INVERT_LOOK_Y_AXIS = Boolean.parseBoolean(p.getProperty("controls.invert.y", "true"));
        boolean DEBUG_MODE = Boolean.parseBoolean(p.getProperty("controls.debugmode", "false"));

        cc.initialize(CONTROLLER_MAPPINGS_FILE, INVERT_LOOK_Y_AXIS, DEBUG_MODE);

        /** SPACECRAFT CONF **/
        SpacecraftConf scc = new SpacecraftConf();
        float sC_RESPONSIVENESS = Float.parseFloat(p.getProperty("spacecraft.responsiveness", "1.65e7"));
        boolean sC_VEL_TO_DIRECTION = Boolean.parseBoolean(p.getProperty("spacecraft.velocity.direction", "false"));
        float sC_HANDLING_FRICTION = Float.parseFloat(p.getProperty("spacecraft.handling.friction", "0.37"));
        boolean sC_SHOW_AXES = Boolean.parseBoolean(p.getProperty("spacecraft.show.axes", "false"));

        scc.initialize(sC_RESPONSIVENESS, sC_VEL_TO_DIRECTION, sC_HANDLING_FRICTION, sC_SHOW_AXES);

        /** INIT GLOBAL CONF **/
        GlobalConf.initialize(vc, prc, sc, dc, rc, ppc, pc, fc, scrc, shc, cc, scc);

    }

    @Override
    public void persistGlobalConf(File propsFile) {

        /** SCREENSHOT **/
        p.setProperty("screenshot.folder", GlobalConf.screenshot.SCREENSHOT_FOLDER);
        p.setProperty("screenshot.width", Integer.toString(GlobalConf.screenshot.SCREENSHOT_WIDTH));
        p.setProperty("screenshot.height", Integer.toString(GlobalConf.screenshot.SCREENSHOT_HEIGHT));
        p.setProperty("screenshot.mode", GlobalConf.screenshot.SCREENSHOT_MODE.toString());
        p.setProperty("screenshot.format", GlobalConf.screenshot.SCREENSHOT_FORMAT.toString().toLowerCase());
        p.setProperty("screenshot.quality", Float.toString(GlobalConf.screenshot.SCREENSHOT_QUALITY));

        /** PERFORMANCE **/
        p.setProperty("global.conf.multithreading", Boolean.toString(GlobalConf.performance.MULTITHREADING));
        p.setProperty("global.conf.numthreads", Integer.toString(GlobalConf.performance.NUMBER_THREADS));

        /** POSTPROCESS **/
        p.setProperty("postprocess.antialiasing", Integer.toString(GlobalConf.postprocess.POSTPROCESS_ANTIALIAS.getAACode()));
        p.setProperty("postprocess.bloom.intensity", Float.toString(GlobalConf.postprocess.POSTPROCESS_BLOOM_INTENSITY));
        p.setProperty("postprocess.motionblur", Float.toString(GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR));
        p.setProperty("postprocess.lensflare", Boolean.toString(GlobalConf.postprocess.POSTPROCESS_LENS_FLARE));
        p.setProperty("postprocess.lightscattering", Boolean.toString(GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING));
        p.setProperty("postprocess.brightness", Float.toString(GlobalConf.postprocess.POSTPROCESS_BRIGHTNESS));
        p.setProperty("postprocess.contrast", Float.toString(GlobalConf.postprocess.POSTPROCESS_CONTRAST));

        /** FRAME CONF **/
        p.setProperty("graphics.render.folder", GlobalConf.frame.RENDER_FOLDER);
        p.setProperty("graphics.render.filename", GlobalConf.frame.RENDER_FILE_NAME);
        p.setProperty("graphics.render.width", Integer.toString(GlobalConf.frame.RENDER_WIDTH));
        p.setProperty("graphics.render.height", Integer.toString(GlobalConf.frame.RENDER_HEIGHT));
        p.setProperty("graphics.render.targetfps", Integer.toString(GlobalConf.frame.RENDER_TARGET_FPS));
        p.setProperty("graphics.camera.recording.targetfps", Integer.toString(GlobalConf.frame.CAMERA_REC_TARGET_FPS));
        p.setProperty("graphics.camera.recording.frameoutputauto", Boolean.toString(GlobalConf.frame.AUTO_FRAME_OUTPUT_CAMERA_PLAY));
        p.setProperty("graphics.render.time", Boolean.toString(GlobalConf.frame.RENDER_SCREENSHOT_TIME));
        p.setProperty("graphics.render.mode", GlobalConf.frame.FRAME_MODE.toString());
        p.setProperty("graphics.render.format", GlobalConf.frame.FRAME_FORMAT.toString().toLowerCase());
        p.setProperty("graphics.render.quality", Float.toString(GlobalConf.frame.FRAME_QUALITY));

        /** DATA **/
        p.setProperty("data.json.catalog", GlobalConf.data.CATALOG_JSON_FILES);
        p.setProperty("data.json.objects", GlobalConf.data.OBJECTS_JSON_FILES);
        p.setProperty("data.limit.mag", Float.toString(GlobalConf.data.LIMIT_MAG_LOAD));
        p.setProperty("data.attitude.real", Boolean.toString(GlobalConf.data.REAL_GAIA_ATTITUDE));
        p.setProperty("data.highaccuracy.positions", Boolean.toString(GlobalConf.data.HIGH_ACCURACY_POSITIONS));

        /** SCREEN **/
        p.setProperty("graphics.screen.width", Integer.toString(Gdx.graphics.isFullscreen() ? GlobalConf.screen.SCREEN_WIDTH : Gdx.graphics.getWidth()));
        p.setProperty("graphics.screen.height", Integer.toString(Gdx.graphics.isFullscreen() ? GlobalConf.screen.SCREEN_HEIGHT : Gdx.graphics.getHeight()));
        p.setProperty("graphics.screen.fullscreen.width", Integer.toString(!Gdx.graphics.isFullscreen() ? GlobalConf.screen.FULLSCREEN_WIDTH : Gdx.graphics.getWidth()));
        p.setProperty("graphics.screen.fullscreen.height", Integer.toString(!Gdx.graphics.isFullscreen() ? GlobalConf.screen.FULLSCREEN_HEIGHT : Gdx.graphics.getHeight()));
        p.setProperty("graphics.screen.fullscreen", Boolean.toString(Gdx.graphics.isFullscreen()));
        p.setProperty("graphics.screen.resizable", Boolean.toString(GlobalConf.screen.RESIZABLE));
        p.setProperty("graphics.screen.vsync", Boolean.toString(GlobalConf.screen.VSYNC));
        p.setProperty("graphics.screen.screenoutput", Boolean.toString(GlobalConf.screen.SCREEN_OUTPUT));

        /** PROGRAM **/
        p.setProperty("program.tutorial", Boolean.toString(GlobalConf.program.DISPLAY_TUTORIAL));
        p.setProperty("program.tutorial.pointer.script", GlobalConf.program.TUTORIAL_POINTER_SCRIPT_LOCATION);
        p.setProperty("program.tutorial.script", GlobalConf.program.TUTORIAL_SCRIPT_LOCATION);
        p.setProperty("program.analytics", Boolean.toString(GlobalConf.program.ANALYTICS_ENABLED));
        p.setProperty("program.displayhud", Boolean.toString(GlobalConf.program.DISPLAY_HUD));
        p.setProperty("program.displaypointercoords", Boolean.toString(GlobalConf.program.DISPLAY_POINTER_COORDS));
        p.setProperty("program.debuginfo", Boolean.toString(GlobalConf.program.SHOW_DEBUG_INFO));
        p.setProperty("program.lastchecked", GlobalConf.program.LAST_CHECKED != null ? df.format(GlobalConf.program.LAST_CHECKED) : "");
        p.setProperty("program.versioncheckurl", GlobalConf.program.VERSION_CHECK_URL);
        p.setProperty("program.ui.theme", GlobalConf.program.UI_THEME);
        p.setProperty("program.scriptlocation", GlobalConf.program.SCRIPT_LOCATION);
        p.setProperty("program.restport", Integer.toString(GlobalConf.program.REST_PORT));
        p.setProperty("program.locale", GlobalConf.program.LOCALE);
        p.setProperty("program.stereoscopic", Boolean.toString(GlobalConf.program.STEREOSCOPIC_MODE));
        p.setProperty("program.stereoscopic.profile", Integer.toString(GlobalConf.program.STEREO_PROFILE.ordinal()));
        p.setProperty("program.cubemap360", Boolean.toString(GlobalConf.program.CUBEMAP360_MODE));
        p.setProperty("program.dataset.dialog", Boolean.toString(GlobalConf.program.DISPLAY_DATASET_DIALOG));

        /** SCENE **/
        p.setProperty("scene.graphics.quality", Integer.toString(GlobalConf.scene.GRAPHICS_QUALITY));
        p.setProperty("scene.object.fadems", Long.toString(GlobalConf.scene.OBJECT_FADE_MS));
        p.setProperty("scene.star.brightness", Double.toString(GlobalConf.scene.STAR_BRIGHTNESS));
        p.setProperty("scene.ambient", Double.toString(GlobalConf.scene.AMBIENT_LIGHT));
        p.setProperty("scene.camera.fov", Integer.toString(GlobalConf.scene.CAMERA_FOV));
        p.setProperty("scene.camera.speedlimit", Integer.toString(GlobalConf.scene.CAMERA_SPEED_LIMIT_IDX));
        p.setProperty("scene.camera.focus.vel", Double.toString(GlobalConf.scene.CAMERA_SPEED));
        p.setProperty("scene.camera.turn.vel", Double.toString(GlobalConf.scene.TURNING_SPEED));
        p.setProperty("scene.camera.rotate.vel", Double.toString(GlobalConf.scene.ROTATION_SPEED));
        p.setProperty("scene.focuslock", Boolean.toString(GlobalConf.scene.FOCUS_LOCK));
        p.setProperty("scene.focuslock.orientation", Boolean.toString(GlobalConf.scene.FOCUS_LOCK_ORIENTATION));
        p.setProperty("scene.labelfactor", Float.toString(GlobalConf.scene.LABEL_NUMBER_FACTOR));
        p.setProperty("scene.star.threshold.quad", Double.toString(GlobalConf.scene.STAR_THRESHOLD_QUAD));
        p.setProperty("scene.star.threshold.point", Double.toString(GlobalConf.scene.STAR_THRESHOLD_POINT));
        p.setProperty("scene.star.threshold.none", Double.toString(GlobalConf.scene.STAR_THRESHOLD_NONE));
        p.setProperty("scene.star.point.size", Float.toString(GlobalConf.scene.STAR_POINT_SIZE));
        p.setProperty("scene.point.alpha.min", Float.toString(GlobalConf.scene.POINT_ALPHA_MIN));
        p.setProperty("scene.point.alpha.max", Float.toString(GlobalConf.scene.POINT_ALPHA_MAX));
        p.setProperty("scene.renderer.orbit", Integer.toString(GlobalConf.scene.ORBIT_RENDERER));
        p.setProperty("scene.renderer.line", Integer.toString(GlobalConf.scene.LINE_RENDERER));
        p.setProperty("scene.octree.particle.fade", Boolean.toString(GlobalConf.scene.OCTREE_PARTICLE_FADE));
        p.setProperty("scene.octant.threshold.0", Float.toString(GlobalConf.scene.OCTANT_THRESHOLD_0));
        p.setProperty("scene.octant.threshold.1", Float.toString(GlobalConf.scene.OCTANT_THRESHOLD_1));
        p.setProperty("scene.propermotion.vectors", Boolean.toString(GlobalConf.scene.PROPER_MOTION_VECTORS));
        p.setProperty("scene.propermotion.numfactor", Float.toString(GlobalConf.scene.PM_NUM_FACTOR));
        p.setProperty("scene.propermotion.lenfactor", Float.toString(GlobalConf.scene.PM_LEN_FACTOR));
        p.setProperty("scene.galaxy.3d", Boolean.toString(GlobalConf.scene.GALAXY_3D));
        p.setProperty("scene.cubemapface.resolution", Integer.toString(GlobalConf.scene.CUBEMAP_FACE_RESOLUTION));
        p.setProperty("scene.crosshair", Boolean.toString(GlobalConf.scene.CROSSHAIR));
        p.setProperty("scene.camera.cinematic", Boolean.toString(GlobalConf.scene.CINEMATIC_CAMERA));
        p.setProperty("scene.camera.free.targetmode", Boolean.toString(GlobalConf.scene.FREE_CAMERA_TARGET_MODE_ON));
        p.setProperty("scene.shadowmapping", Boolean.toString(GlobalConf.scene.SHADOW_MAPPING));
        p.setProperty("scene.shadowmapping.nshadows", Integer.toString(GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS));
        p.setProperty("scene.shadowmapping.resolution", Integer.toString(GlobalConf.scene.SHADOW_MAPPING_RESOLUTION));

        // Visibility of components
        int idx = 0;
        ComponentType[] cts = ComponentType.values();
        for (boolean b : GlobalConf.scene.VISIBILITY) {
            ComponentType ct = cts[idx];
            p.setProperty("scene.visibility." + ct.name(), Boolean.toString(b));
            idx++;
        }

        /** CONTROLS **/
        p.setProperty("controls.mappings.file", GlobalConf.controls.CONTROLLER_MAPPINGS_FILE);
        p.setProperty("controls.invert.y", Boolean.toString(GlobalConf.controls.INVERT_LOOK_Y_AXIS));
        p.setProperty("controls.debugmode", Boolean.toString(GlobalConf.controls.DEBUG_MODE));

        /** SPACECRAFT **/
        p.setProperty("spacecraft.responsiveness", Float.toString(GlobalConf.spacecraft.SC_RESPONSIVENESS));
        p.setProperty("spacecraft.velocity.direction", Boolean.toString(GlobalConf.spacecraft.SC_VEL_TO_DIRECTION));
        p.setProperty("spacecraft.handling.friction", Float.toString(GlobalConf.spacecraft.SC_HANDLING_FRICTION));
        p.setProperty("spacecraft.show.axes", Boolean.toString(GlobalConf.spacecraft.SC_SHOW_AXES));

        try {
            FileOutputStream fos = new FileOutputStream(propsFile);
            p.store(fos, null);
            fos.close();
            Logger.info("Configuration saved to " + propsFile.getAbsolutePath());
        } catch (Exception e) {
            Logger.error(e);
        }

    }

    private String initConfigFile(boolean ow) throws IOException {
        // Use user folder
        File userFolder = SysUtilsFactory.getSysUtils().getGSHomeDir();
        userFolder.mkdirs();
        File userFolderConfFile = new File(userFolder, "global.properties");

        if (ow || !userFolderConfFile.exists()) {
            // Copy file
            copyFile(new File("conf" + File.separator + "global.properties"), userFolderConfFile, ow);
        }
        String props = userFolderConfFile.getAbsolutePath();
        System.setProperty("properties.file", props);
        return props;
    }

    private void copyFile(File sourceFile, File destFile, boolean ow) throws IOException {
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

        FileInputStream sourceFis = null;
        FileOutputStream destinationFis = null;
        try {
            // Open channels
            sourceFis = new FileInputStream(sourceFile);
            destinationFis = new FileOutputStream(destFile);

            FileChannel source = sourceFis.getChannel();

            // Transfer
            destinationFis.getChannel().transferFrom(source, 0, source.size());
        } finally {
            if (sourceFis != null)
                sourceFis.close();
            if (destinationFis != null)
                destinationFis.close();
        }
    }

}
