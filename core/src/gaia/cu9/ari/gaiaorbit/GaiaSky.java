package gaia.cu9.ari.gaiaorbit;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

import gaia.cu9.ari.gaiaorbit.assets.AtmosphereShaderProviderLoader;
import gaia.cu9.ari.gaiaorbit.assets.GaiaAttitudeLoader;
import gaia.cu9.ari.gaiaorbit.assets.GaiaAttitudeLoader.GaiaAttitudeLoaderParameter;
import gaia.cu9.ari.gaiaorbit.assets.GroundShaderProviderLoader;
import gaia.cu9.ari.gaiaorbit.assets.OrbitDataLoader;
import gaia.cu9.ari.gaiaorbit.assets.RelativisticShaderProviderLoader;
import gaia.cu9.ari.gaiaorbit.assets.SGLoader;
import gaia.cu9.ari.gaiaorbit.assets.SGLoader.SGLoaderParameter;
import gaia.cu9.ari.gaiaorbit.data.AssetBean;
import gaia.cu9.ari.gaiaorbit.data.StreamingOctreeLoader;
import gaia.cu9.ari.gaiaorbit.data.orbit.OrbitData;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.ConsoleLogger;
import gaia.cu9.ari.gaiaorbit.interfce.DebugGui;
import gaia.cu9.ari.gaiaorbit.interfce.FullGui;
import gaia.cu9.ari.gaiaorbit.interfce.GuiRegistry;
import gaia.cu9.ari.gaiaorbit.interfce.IGui;
import gaia.cu9.ari.gaiaorbit.interfce.InitialGui;
import gaia.cu9.ari.gaiaorbit.interfce.KeyInputController;
import gaia.cu9.ari.gaiaorbit.interfce.LoadingGui;
import gaia.cu9.ari.gaiaorbit.interfce.MobileGui;
import gaia.cu9.ari.gaiaorbit.interfce.SpacecraftGui;
import gaia.cu9.ari.gaiaorbit.interfce.StereoGui;
import gaia.cu9.ari.gaiaorbit.render.AbstractRenderer;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.IMainRenderer;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.PostProcessBean;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.RenderType;
import gaia.cu9.ari.gaiaorbit.render.PostProcessorFactory;
import gaia.cu9.ari.gaiaorbit.render.SceneGraphRenderer;
import gaia.cu9.ari.gaiaorbit.scenegraph.CameraManager;
import gaia.cu9.ari.gaiaorbit.scenegraph.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.scenegraph.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.script.HiddenHelperUser;
import gaia.cu9.ari.gaiaorbit.util.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.util.ConfInit;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.MemInfo;
import gaia.cu9.ari.gaiaorbit.util.ModelCache;
import gaia.cu9.ari.gaiaorbit.util.MusicManager;
import gaia.cu9.ari.gaiaorbit.util.g3d.loader.ObjLoader;
import gaia.cu9.ari.gaiaorbit.util.gaia.GaiaAttitudeServer;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.override.AtmosphereShaderProvider;
import gaia.cu9.ari.gaiaorbit.util.override.GroundShaderProvider;
import gaia.cu9.ari.gaiaorbit.util.override.RelativisticShaderProvider;
import gaia.cu9.ari.gaiaorbit.util.override.ShaderProgramProvider;
import gaia.cu9.ari.gaiaorbit.util.samp.SAMPClient;
import gaia.cu9.ari.gaiaorbit.util.time.GlobalClock;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.time.RealTimeClock;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

/**
 * The main class. Holds all the entities manages the update/draw cycle as well
 * as the image rendering.
 * 
 * @author Toni Sagrista
 *
 */
public class GaiaSky implements ApplicationListener, IObserver, IMainRenderer {

    /**
     * Private state boolean indicating whether we are still loading resources.
     */
    private static boolean LOADING = false;

    /** Attitude folder **/
    private static String ATTITUDE_FOLDER = "data/attitudexml/";

    /** Singleton instance **/
    public static GaiaSky instance;

    // Asset manager
    public AssetManager manager;

    // Camera
    public CameraManager cam;

    // Data load string
    private String dataLoadString;

    /**
     * Whether the dataset has been chosen. If this is set to false, a window
     * will prompt at startup asking for the dataset to use.
     */
    private boolean DSCHOSEN;

    public ISceneGraph sg;
    // TODO make this private again
    public SceneGraphRenderer sgr;
    private IPostProcessor pp;

    // Start time
    private long startTime;

    // The current actual dt in seconds
    private double dt;
    // Time since the start in seconds
    private double t;

    // The frame number
    public long frames;

    // Frame buffer map
    private Map<String, FrameBuffer> fbmap;

    // The input multiplexer
    private InputMultiplexer inputMultiplexer;

    /**
     * Provisional console logger
     */
    private ConsoleLogger clogger;

    /**
     * The user interfaces
     */
    public IGui initialGui, loadingGui, mainGui, spacecraftGui, stereoGui, debugGui, currentGui, previousGui;

    /**
     * List of GUIs
     */
    private List<IGui> guis;

    /**
     * Time
     */
    public ITimeFrameProvider time;
    private ITimeFrameProvider clock, real;

    /**
     * Music
     */
    public Music music;

    /**
     * Camera recording or not?
     */
    private boolean camRecording = false;

    private boolean initialized = false;

    /**
     * Save state on exit
     */
    public boolean savestate = true;

    /**
     * Creates a GaiaSky instance.
     */
    public GaiaSky() {
        super();
        instance = this;
    }

    public void setSceneGraph(ISceneGraph sg) {
        this.sg = sg;
    }

    @Override
    public void create() {
        startTime = TimeUtils.millis();
        Gdx.app.setLogLevel(Application.LOG_INFO);
        clogger = new ConsoleLogger(true);

        fbmap = new HashMap<String, FrameBuffer>();

        // Disable all kinds of input
        EventManager.instance.post(Events.INPUT_ENABLED_CMD, false);

        DSCHOSEN = !GlobalConf.program.DISPLAY_DATASET_DIALOG;

        if (!GlobalConf.initialized()) {
            Logger.error(new RuntimeException("FATAL: Global configuration not initlaized"));
            return;
        }

        // Initialise times
        clock = new GlobalClock(1, Instant.now());
        real = new RealTimeClock();
        time = GlobalConf.runtime.REAL_TIME ? real : clock;
        t = 0;

        // Initialise i18n
        I18n.initialize();

        // Tooltips
        TooltipManager.getInstance().initialTime = 1f;
        TooltipManager.getInstance().hideAll();

        // Initialise asset manager
        FileHandleResolver resolver = new InternalFileHandleResolver();
        manager = new AssetManager(resolver);
        //manager.setLoader(Model.class, ".obj", new AdvancedObjLoader(resolver));
        manager.setLoader(ISceneGraph.class, new SGLoader(resolver));
        manager.setLoader(OrbitData.class, new OrbitDataLoader(resolver));
        manager.setLoader(GaiaAttitudeServer.class, new GaiaAttitudeLoader(resolver));
        manager.setLoader(ShaderProgram.class, new ShaderProgramProvider(resolver, ".vertex.glsl", ".fragment.glsl"));
        //manager.setLoader(DefaultShaderProvider.class, new DefaultShaderProviderLoader<>(resolver));
        manager.setLoader(AtmosphereShaderProvider.class, new AtmosphereShaderProviderLoader<>(resolver));
        manager.setLoader(GroundShaderProvider.class, new GroundShaderProviderLoader<>(resolver));
        manager.setLoader(RelativisticShaderProvider.class, new RelativisticShaderProviderLoader<>(resolver));
        manager.setLoader(Model.class, ".obj", new ObjLoader(resolver));

        // Init global resources
        GlobalResources.initialize(manager);

        // Initialise Cameras
        cam = new CameraManager(manager, CameraMode.Focus);

        // Set asset manager to asset bean
        AssetBean.setAssetManager(manager);

        // Tooltip to 1s
        TooltipManager.getInstance().initialTime = 1f;

        // Initialise Gaia attitudes
        manager.load(ATTITUDE_FOLDER, GaiaAttitudeServer.class, new GaiaAttitudeLoaderParameter(GlobalConf.runtime.STRIPPED_FOV_MODE ? new String[] { "OPS_RSLS_0022916_rsls_nsl_gareq1_afterFirstSpinPhaseOptimization.2.xml" } : new String[] {}));

        // Initialise hidden helper user
        HiddenHelperUser.initialize();

        // Initialise gravitational waves helper
        RelativisticEffectsManager.initialize(time);

        // GUI
        guis = new ArrayList<IGui>(3);
        reinitialiseGUI1();

        // Post-processor
        pp = PostProcessorFactory.instance.getPostProcessor();
        pp.initialize(manager);

        // Scene graph renderer
        sgr = new SceneGraphRenderer();
        sgr.initialize(manager);

        // Tell the asset manager to load all the assets
        Set<AssetBean> assets = AssetBean.getAssets();
        for (AssetBean ab : assets) {
            ab.load(manager);
        }

        EventManager.instance.subscribe(this, Events.LOAD_DATA_CMD);

        initialGui = new InitialGui();
        initialGui.initialize(manager);
        Gdx.input.setInputProcessor(initialGui.getGuiStage());

        if (DSCHOSEN) {
            EventManager.instance.post(Events.LOAD_DATA_CMD);
        }

        Logger.info(this.getClass().getSimpleName(), "v" + GlobalConf.version.version + " - " + I18n.bundle.format("gui.build", GlobalConf.version.build));
        Logger.info(this.getClass().getSimpleName(), "Display mode set to " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight() + ", fullscreen: " + Gdx.graphics.isFullscreen());
        Logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.glslversion", Gdx.gl.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION)));
    }

    /**
     * Execute this when the models have finished loading. This sets the models
     * to their classes and removes the Loading message
     */
    private void doneLoading() {
        // Dispose of initial and loading GUIs
        initialGui.dispose();
        initialGui = null;
        loadingGui.dispose();
        loadingGui = null;

        // Get attitude
        if (manager.isLoaded(ATTITUDE_FOLDER)) {
            GaiaAttitudeServer.instance = manager.get(ATTITUDE_FOLDER);
        }

        /**
         * SAMP
         */
        SAMPClient.getInstance().initialize();

        /**
         * POST-PROCESSOR
         */
        pp.doneLoading(manager);

        /**
         * GET SCENE GRAPH
         */
        if (manager.isLoaded(dataLoadString)) {
            sg = manager.get(dataLoadString);
        }

        /**
         * SCENE GRAPH RENDERER
         */
        AbstractRenderer.initialize(sg);
        sgr.doneLoading(manager);
        sgr.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // First time, set assets
        Array<SceneGraphNode> nodes = sg.getNodes();
        for (SceneGraphNode sgn : nodes) {
            sgn.doneLoading(manager);
        }

        // Initialise input handlers
        inputMultiplexer = new InputMultiplexer();

        // Destroy console logger
        clogger.dispose();
        clogger = null;
        // Init GUIs, step 2
        reinitialiseGUI2();

        // Publish visibility
        EventManager.instance.post(Events.VISIBILITY_OF_COMPONENTS, new Object[] { SceneGraphRenderer.visible });

        // Key bindings controller
        inputMultiplexer.addProcessor(new KeyInputController());

        Gdx.input.setInputProcessor(inputMultiplexer);

        EventManager.instance.post(Events.SCENE_GRAPH_LOADED, sg);

        // Update whole tree to initialize positions
        OctreeNode.LOAD_ACTIVE = false;
        time.update(0.000000001f);
        // Update whole scene graph
        sg.update(time, cam);
        sgr.clearLists();
        time.update(0);
        OctreeNode.LOAD_ACTIVE = true;

        // Initialise time in GUI
        EventManager.instance.post(Events.TIME_CHANGE_INFO, time.getTime());

        // Subscribe to events
        EventManager.instance.subscribe(this, Events.TOGGLE_AMBIENT_LIGHT, Events.AMBIENT_LIGHT_CMD, Events.RECORD_CAMERA_CMD, Events.CAMERA_MODE_CMD, Events.STEREOSCOPIC_CMD, Events.FRAME_SIZE_UDPATE, Events.SCREENSHOT_SIZE_UDPATE);

        // Re-enable input
        if (!GlobalConf.runtime.STRIPPED_FOV_MODE)
            EventManager.instance.post(Events.INPUT_ENABLED_CMD, true);

        // Set current date
        EventManager.instance.post(Events.TIME_CHANGE_CMD, Instant.now());

        if (Constants.focalplane) {
            // Activate time
            EventManager.instance.post(Events.TOGGLE_TIME_CMD, true, false);
        }

        // Resize GUIs to current size
        for (IGui gui : guis)
            gui.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Initialise frames
        frames = 0;

        // Set focus to Earth
        EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.Focus);
        EventManager.instance.post(Events.FOCUS_CHANGE_CMD, sg.getNode("Solar sail"), true);
        EventManager.instance.post(Events.GO_TO_OBJECT_CMD);

        initialized = true;
    }

    /**
     * Reinitialises all the GUI (step 1)
     */
    public void reinitialiseGUI1() {
        if (guis != null && !guis.isEmpty()) {
            for (IGui gui : guis)
                gui.dispose();
            guis.clear();
        }

        if (Constants.desktop || Constants.webgl) {
            // Full GUI for desktop
            mainGui = new FullGui();
        } else if (Constants.mobile) {
            // Reduced GUI for android/iOS/...
            mainGui = new MobileGui();
        }
        mainGui.initialize(manager);

        debugGui = new DebugGui();
        debugGui.initialize(manager);

        spacecraftGui = new SpacecraftGui();
        spacecraftGui.initialize(manager);

        stereoGui = new StereoGui();
        stereoGui.initialize(manager);

        guis.add(mainGui);
        guis.add(debugGui);
        guis.add(spacecraftGui);
        guis.add(stereoGui);
    }

    /**
     * Second step in GUI initialisation.
     */
    public void reinitialiseGUI2() {
        // Unregister all current GUIs
        GuiRegistry.unregisterAll();

        // Only for the Full GUI
        mainGui.setSceneGraph(sg);
        mainGui.setVisibilityToggles(ComponentType.values(), SceneGraphRenderer.visible);

        for (IGui gui : guis)
            gui.doneLoading(manager);

        if (GlobalConf.program.STEREOSCOPIC_MODE) {
            GuiRegistry.registerGui(stereoGui);
            inputMultiplexer.addProcessor(stereoGui.getGuiStage());
            // Initialise current and previous
            currentGui = stereoGui;
            previousGui = mainGui;
        } else {
            GuiRegistry.registerGui(mainGui);
            inputMultiplexer.addProcessor(mainGui.getGuiStage());
            // Initialise current and previous
            currentGui = mainGui;
            previousGui = null;
        }
        GuiRegistry.registerGui(debugGui);
    }

    @Override
    public void pause() {
        EventManager.instance.post(Events.FLUSH_FRAMES);
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {

        if (Constants.desktop && savestate)
            ConfInit.instance.persistGlobalConf(new File(System.getProperty("properties.file")));

        // Flush frames
        EventManager.instance.post(Events.FLUSH_FRAMES);

        // Dispose all
        for (IGui gui : guis)
            gui.dispose();

        EventManager.instance.post(Events.DISPOSE);
        if (sg != null) {
            sg.dispose();
        }
        ModelCache.cache.dispose();

        // Renderer
        if (sgr != null)
            sgr.dispose();

        // Post processor
        if (pp != null)
            pp.dispose();

        // Dispose music manager
        MusicManager.dispose();

    }

    long lastDebugTime = -1;
    // Debug info scheduler
    Runnable debugTask = new Runnable() {

        @Override
        public void run() {
            // FPS
            EventManager.instance.post(Events.FPS_INFO, 1f / Gdx.graphics.getDeltaTime());
            // Current session time
            EventManager.instance.post(Events.DEBUG1, TimeUtils.timeSinceMillis(startTime) / 1000d);
            // Memory
            EventManager.instance.post(Events.DEBUG2, MemInfo.getUsedMemory(), MemInfo.getFreeMemory(), MemInfo.getTotalMemory(), MemInfo.getMaxMemory());
            // Observed octants
            EventManager.instance.post(Events.DEBUG4, GLFrameBuffer.getManagedStatus() + ", Observed octants: " + OctreeNode.nOctantsObserved + ", Load queue: " + StreamingOctreeLoader.getLoadQueueSize());
        }
    };

    @Override
    public void render() {
        try {
            if (!DSCHOSEN) {
                renderGui(initialGui);
            } else if (LOADING) {
                if (manager.update()) {
                    doneLoading();

                    LOADING = false;
                } else {
                    // Display loading screen
                    renderGui(loadingGui);
                }
            } else {
                //GlobalConf.scene.OCTANT_THRESHOLD_0 = (float) (1.d * MathUtilsd.degRad);
                //GlobalConf.scene.OCTANT_THRESHOLD_1 = GlobalConf.scene.OCTANT_THRESHOLD_0 + 1.f * GlobalConf.scene.OCTANT_THRESHOLD_0;
                // Asynchronous load of textures and resources
                manager.update();

                if (!GlobalConf.runtime.UPDATE_PAUSE) {
                    /**
                     * UPDATE
                     */
                    update(Gdx.graphics.getDeltaTime());

                    /**
                     * FRAME OUTPUT
                     */
                    EventManager.instance.post(Events.RENDER_FRAME, this);

                    /**
                     * SCREENSHOT OUTPUT - simple|redraw mode
                     */
                    EventManager.instance.post(Events.RENDER_SCREENSHOT, this);

                    /**
                     * SCREEN OUTPUT
                     */
                    if (GlobalConf.screen.SCREEN_OUTPUT) {
                        /** RENDER THE SCENE **/
                        preRenderScene();
                        renderSgr(cam, t, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), null, pp.getPostProcessBean(RenderType.screen));

                        if (GlobalConf.runtime.DISPLAY_GUI) {
                            // Render the GUI, setting the viewport
                            GuiRegistry.render(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        }

                    }
                    // Clean lists
                    sgr.clearLists();
                    // Number of frames
                    frames++;

                    /** DEBUG - each 1 secs **/
                    if (TimeUtils.millis() - lastDebugTime > 1000) {
                        Gdx.app.postRunnable(debugTask);
                        lastDebugTime = TimeUtils.millis();
                    }
                }

            }
        } catch (Throwable t) {
            EventManager.instance.post(Events.JAVA_EXCEPTION, t);
            // TODO implement error reporting?
        }
    }

    /**
     * Update method.
     * 
     * @param deltat
     *            Delta time in seconds.
     */
    public void update(double deltat) {
        if (GlobalConf.frame.RENDER_OUTPUT) {
            // If RENDER_OUTPUT is active, we need to set our dt according to
            // the fps
            this.dt = 1f / GlobalConf.frame.RENDER_TARGET_FPS;
        } else if (camRecording) {
            // If Camera is recording, we need to set our dt according to
            // the fps
            this.dt = 1f / GlobalConf.frame.CAMERA_REC_TARGET_FPS;
        } else {
            // Max time step is 0.1 seconds. Not in RENDER_OUTPUT MODE.
            this.dt = Math.min(deltat, 0.1f);
        }

        this.t += this.dt;

        // Update GUI 
        GuiRegistry.update(this.dt);
        EventManager.instance.post(Events.UPDATE_GUI, this.dt);

        double dtScene = this.dt;
        if (!GlobalConf.runtime.TIME_ON) {
            dtScene = 0;
        }
        // Update clock
        time.update(dtScene);

        // Update events
        EventManager.instance.dispatchDelayedMessages();

        // Update cameras
        cam.update(this.dt, time);

        // Precompute isOn for all stars and galaxies
        Particle.renderOn = isOn(ComponentType.Stars);

        // Update GravWaves params
        RelativisticEffectsManager.getInstance().update(time, cam.current);

        // Update scene graph
        sg.update(time, cam);

    }

    public void preRenderScene() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void renderSgr(ICamera camera, double t, int width, int height, FrameBuffer frameBuffer, PostProcessBean ppb) {
        sgr.render(camera, t, width, height, frameBuffer, ppb);
    }

    @Override
    public void resize(final int width, final int height) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                resizeImmediate(width, height, true, true, true);
            }
        });

    }

    public void resizeImmediate(final int width, final int height, boolean resizePostProcessors, boolean resizeRenderSys, boolean resizeGuis) {
        if (!initialized) {
            if (initialGui != null)
                initialGui.resize(width, height);
            if (loadingGui != null)
                loadingGui.resizeImmediate(width, height);
        } else {
            if (resizePostProcessors)
                pp.resizeImmediate(width, height);

            if (resizeGuis)
                for (IGui gui : guis)
                    gui.resizeImmediate(width, height);

            sgr.resize(width, height, resizeRenderSys);
        }

        cam.updateAngleEdge(width, height);
        cam.resize(width, height);

        EventManager.instance.post(Events.SCREEN_RESIZE, width, height);
    }

    /**
     * Renders a particular GUI
     * 
     * @param gui
     *            The GUI to render
     */
    private void renderGui(IGui gui) {
        gui.update(Gdx.graphics.getDeltaTime());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        gui.render(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public Array<IFocus> getFocusableEntities() {
        return sg.getFocusableObjects();
    }

    public FrameBuffer getFrameBuffer(int w, int h) {
        String key = getKey(w, h);
        if (!fbmap.containsKey(key)) {
            FrameBuffer fb = new FrameBuffer(Format.RGB888, w, h, true);
            fbmap.put(key, fb);
        }
        return fbmap.get(key);
    }

    private String getKey(int w, int h) {
        return w + "x" + h;
    }

    public void clearFrameBufferMap() {
        Set<String> keySet = fbmap.keySet();
        for (String key : keySet) {
            FrameBuffer fb = fbmap.get(key);
            fb.dispose();
        }
        fbmap.clear();
    }

    public ICamera getICamera() {
        return cam.current;
    }

    public double getT() {
        return t;
    }

    public CameraManager getCameraManager() {
        return cam;
    }

    public IPostProcessor getPostProcessor() {
        return pp;
    }

    public boolean isOn(int ordinal) {
        return sgr.isOn(ordinal);
    }

    public boolean isOn(ComponentType comp) {
        return sgr.isOn(comp);
    }

    public boolean isOn(ComponentTypes cts) {
        return sgr.isOn(cts);
    }

    private String concatenate(String split, String... strs) {
        String out = "";
        for (String str : strs) {
            if (str != null && !str.isEmpty()) {
                if (!out.isEmpty())
                    out += split;
                out += str;
            }
        }
        return out;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case LOAD_DATA_CMD:
            // Initialise loading screen
            loadingGui = new LoadingGui();
            loadingGui.initialize(manager);
            Gdx.input.setInputProcessor(loadingGui.getGuiStage());
            DSCHOSEN = true;
            LOADING = true;

            /** LOAD SCENE GRAPH **/
            if (sg == null) {
                dataLoadString = concatenate(",", GlobalConf.data.CATALOG_JSON_FILES, GlobalConf.data.OBJECTS_JSON_FILES);
                manager.load(dataLoadString, ISceneGraph.class, new SGLoaderParameter(time, GlobalConf.performance.MULTITHREADING, GlobalConf.performance.NUMBER_THREADS()));
            }
            break;
        case TOGGLE_AMBIENT_LIGHT:
            // TODO No better place to put this??
            ModelComponent.toggleAmbientLight((Boolean) data[1]);
            break;
        case AMBIENT_LIGHT_CMD:
            ModelComponent.setAmbientLight((float) data[0]);
            break;
        case RECORD_CAMERA_CMD:
            if (data != null) {
                camRecording = (Boolean) data[0];
            } else {
                camRecording = !camRecording;
            }
            break;
        case CAMERA_MODE_CMD:
            InputMultiplexer im = (InputMultiplexer) Gdx.input.getInputProcessor();
            // Register/unregister GUI
            CameraMode mode = (CameraMode) data[0];
            if (GlobalConf.program.isStereoHalfViewport()) {
                if (currentGui != stereoGui) {
                    // Remove current GUI
                    GuiRegistry.unregisterGui(currentGui);
                    im.removeProcessor(currentGui.getGuiStage());

                    // Add spacecraft GUI
                    GuiRegistry.registerGui(stereoGui);
                    im.addProcessor(0, stereoGui.getGuiStage());

                    // Update state
                    currentGui = stereoGui;
                }
            } else if (mode == CameraMode.Spacecraft) {
                // Remove current GUI
                GuiRegistry.unregisterGui(currentGui);
                im.removeProcessor(currentGui.getGuiStage());

                // Add spacecraft GUI
                GuiRegistry.registerGui(spacecraftGui);
                im.addProcessor(0, spacecraftGui.getGuiStage());

                // Update state
                currentGui = spacecraftGui;

            } else {
                // Remove current GUI
                GuiRegistry.unregisterGui(currentGui);
                im.removeProcessor(currentGui.getGuiStage());

                // Add main GUI
                GuiRegistry.registerGui(mainGui);
                im.addProcessor(0, mainGui.getGuiStage());

                // Update state
                currentGui = mainGui;
            }
            break;
        case STEREOSCOPIC_CMD:
            boolean stereomode = (Boolean) data[0];
            im = (InputMultiplexer) Gdx.input.getInputProcessor();
            if (stereomode && currentGui != stereoGui) {
                // Remove current GUI
                GuiRegistry.unregisterGui(currentGui);
                im.removeProcessor(currentGui.getGuiStage());

                // Add stereo GUI
                GuiRegistry.registerGui(stereoGui);
                im.addProcessor(0, stereoGui.getGuiStage());

                // Update state
                previousGui = currentGui;
                currentGui = stereoGui;
            } else if (!stereomode && previousGui != stereoGui) {
                // Remove current GUI
                GuiRegistry.unregisterGui(currentGui);
                im.removeProcessor(currentGui.getGuiStage());

                // Add backed up GUI
                if (previousGui == null)
                    previousGui = mainGui;
                GuiRegistry.registerGui(previousGui);
                im.addProcessor(0, previousGui.getGuiStage());

                // Update state
                currentGui = previousGui;
            }

            break;
        case SCREENSHOT_SIZE_UDPATE:
        case FRAME_SIZE_UDPATE:
            Gdx.app.postRunnable(() -> {
                //clearFrameBufferMap();
            });
            break;
        default:
            break;
        }

    }

}
