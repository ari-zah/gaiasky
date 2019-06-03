/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.BitmapFontLoader.BitmapFontParameter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.RenderableSorter;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.bitfire.postprocessing.filters.Glow;
import com.bitfire.utils.ShaderLoader;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.assets.AtmosphereShaderProviderLoader.AtmosphereShaderProviderParameter;
import gaia.cu9.ari.gaiaorbit.assets.GroundShaderProviderLoader.GroundShaderProviderParameter;
import gaia.cu9.ari.gaiaorbit.assets.RelativisticShaderProviderLoader.RelativisticShaderProviderParameter;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.PostProcessBean;
import gaia.cu9.ari.gaiaorbit.render.system.*;
import gaia.cu9.ari.gaiaorbit.render.system.AbstractRenderSystem.RenderSystemRunnable;
import gaia.cu9.ari.gaiaorbit.render.system.ModelBatchRenderSystem.ModelRenderType;
import gaia.cu9.ari.gaiaorbit.scenegraph.*;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.StubModel;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.override.AtmosphereShaderProvider;
import gaia.cu9.ari.gaiaorbit.util.override.GroundShaderProvider;
import gaia.cu9.ari.gaiaorbit.util.override.RelativisticShaderProvider;
import gaia.cu9.ari.gaiaorbit.util.override.ShaderProgramProvider.ShaderProgramParameter;
import gaia.cu9.ari.gaiaorbit.vr.VRContext;

import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders a scenegraph.
 *
 * @author Toni Sagrista
 */
public class SceneGraphRenderer extends AbstractRenderer implements IProcessRenderer, IObserver {
    private static final Log logger = Logger.getLogger(SceneGraphRenderer.class);

    /** Contains the flags representing each type's visibility **/
    public static ComponentTypes visible;
    /** Contains the last update time of each of the flags **/
    public static long[] times;
    /** Alpha values for each type **/
    public static float[] alphas;

    private ShaderProgram[] starGroupShaders, particleGroupShaders, particleEffectShaders, orbitElemShaders, pointShaders, lineShaders, lineQuadShaders, lineGpuShaders, mwPointShaders, mwOitShaders, mwNebulaShaders, starPointShaders, galShaders, spriteShaders, starBillboardShaders;
    private AssetDescriptor<ShaderProgram>[] starGroupDesc, particleGroupDesc, particleEffectDesc, orbitElemDesc, pointDesc, lineDesc, lineQuadDesc, lineGpuDesc, mwPointDesc, mwOitDesc, mwNebulaDesc, starPointDesc, galDesc, spriteDesc, starBillboardDesc;

    /** Render lists for all render groups **/
    public static Array<Array<IRenderable>> render_lists;

    // Two model batches, for front (models), back and atmospheres
    private SpriteBatch spriteBatch, fontBatch;

    private Array<IRenderSystem> renderProcesses;

    private RenderSystemRunnable blendNoDepthRunnable, blendDepthRunnable, additiveBlendDepthRunnable, restoreRegularBlend;

    /** The particular current scene graph renderer **/
    private ISGR sgr;
    /**
     * Renderers vector, with 0 = normal, 1 = stereoscopic, 2 = FOV, 3 = cubemap
     **/
    private ISGR[] sgrs;
    // Indexes
    private final int SGR_DEFAULT_IDX = 0, SGR_STEREO_IDX = 1, SGR_FOV_IDX = 2, SGR_CUBEMAP_IDX = 3, SGR_OPENVR_IDX = 4;

    // Camera at light position, with same direction. For shadow mapping
    private Camera cameraLight;
    private Array<ModelBody> candidates;
    private FrameBuffer[] shadowMapFb;
    private Matrix4[] shadowMapCombined;
    public Map<ModelBody, Texture> smTexMap;
    public Map<ModelBody, Matrix4> smCombinedMap;
    private ModelBatch modelBatchDepth;

    // Light glow pre-render
    private FrameBuffer glowFb;
    private Texture glowTex;
    private ModelBatch modelBatchOpaque;

    private Vector3 aux1;
    private Vector3d aux1d;

    // VRContext, may be null
    private VRContext vrContext;
    
    private Array<IRenderable> stars;

    private AbstractRenderSystem billboardStarsProc;
    private MWModelRenderSystem mwrs;

    public SceneGraphRenderer(VRContext vrContext) {
        super();
        this.vrContext = vrContext;
    }

    private AssetDescriptor<ShaderProgram>[] loadShader(AssetManager manager, String vfile, String ffile, String[] names, String[] prependVertex) {
        @SuppressWarnings("unchecked") AssetDescriptor<ShaderProgram>[] result = new AssetDescriptor[prependVertex.length];

        int i = 0;
        for (String prep : prependVertex) {
            ShaderProgramParameter spp = new ShaderProgramParameter();
            spp.prependVertexCode = prep;
            spp.vertexFile = vfile;
            spp.fragmentFile = ffile;
            manager.load(names[i], ShaderProgram.class, spp);
            AssetDescriptor<ShaderProgram> desc = new AssetDescriptor<ShaderProgram>(names[i], ShaderProgram.class, spp);
            result[i] = desc;

            i++;
        }

        return result;
    }

    @Override
    public void initialize(AssetManager manager) {
        ShaderLoader.Pedantic = false;
        ShaderProgram.pedantic = false;

        /** LOAD SHADER PROGRAMS WITH ASSET MANAGER **/
        manager.load("shader/font.vertex.glsl", ShaderProgram.class);

        String[] defines = new String[] { "", "#define relativisticEffects\n", "#define gravitationalWaves\n", "#define relativisticEffects\n#define gravitationalWaves\n" };

        starBillboardDesc = loadShader(manager, "shader/star.billboard.vertex.glsl", "shader/star.billboard.fragment.glsl", genShaderNames("starBillboard"), defines);
        spriteDesc = loadShader(manager, "shader/sprite.vertex.glsl", "shader/sprite.fragment.glsl", genShaderNames("sprite"), defines);
        starPointDesc = loadShader(manager, "shader/star.point.vertex.glsl", "shader/star.point.fragment.glsl", genShaderNames("starPoint"), defines);
        mwPointDesc = loadShader(manager, "shader/point.galaxy.vertex.glsl", "shader/point.galaxy.fragment.glsl", genShaderNames("mwPoint"), defines);
        mwOitDesc = loadShader(manager, "shader/galaxy.oit.vertex.glsl", "shader/galaxy.oit.fragment.glsl", genShaderNames("galOit"), defines);

        mwNebulaDesc = loadShader(manager, "shader/nebula.vertex.glsl", "shader/nebula.fragment.glsl", genShaderNames("nebula"), defines);
        pointDesc = loadShader(manager, "shader/point.cpu.vertex.glsl", "shader/point.cpu.fragment.glsl", genShaderNames("pointCpu"), defines);
        lineDesc = loadShader(manager, "shader/line.cpu.vertex.glsl", "shader/line.cpu.fragment.glsl", genShaderNames("lineCpu"), defines);
        lineQuadDesc = loadShader(manager, "shader/line.quad.vertex.glsl", "shader/line.quad.fragment.glsl", genShaderNames("lineQuad"), defines);
        lineGpuDesc = loadShader(manager, "shader/line.gpu.vertex.glsl", "shader/line.gpu.fragment.glsl", genShaderNames("lineGpu"), defines);
        galDesc = loadShader(manager, "shader/gal.vertex.glsl", "shader/gal.fragment.glsl", genShaderNames("gal"), defines);
        particleEffectDesc = loadShader(manager, "shader/particle.effect.vertex.glsl", "shader/particle.effect.fragment.glsl", genShaderNames("particleEffect"), defines);
        particleGroupDesc = loadShader(manager, "shader/particle.group.vertex.glsl", "shader/particle.group.fragment.glsl", genShaderNames("particleGroup"), defines);
        starGroupDesc = loadShader(manager, "shader/star.group.vertex.glsl", "shader/star.group.fragment.glsl", genShaderNames("starGroup"), defines);
        orbitElemDesc = loadShader(manager, "shader/orbitelem.vertex.glsl", "shader/particle.group.fragment.glsl", genShaderNames("orbitElem"), defines);

        manager.load("atmgrounddefault", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/default.vertex.glsl", "shader/default.fragment.glsl"));
        manager.load("additive", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.additive.fragment.glsl"));
        manager.load("grids", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/default.grid.fragment.glsl"));
        manager.load("spsurface", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/starsurface.vertex.glsl", "shader/starsurface.fragment.glsl"));
        manager.load("spbeam", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/default.vertex.glsl", "shader/beam.fragment.glsl"));
        manager.load("spdepth", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/normal.vertex.glsl", "shader/depth.fragment.glsl"));
        manager.load("spopaque", RelativisticShaderProvider.class, new RelativisticShaderProviderParameter("shader/normal.vertex.glsl", "shader/opaque.fragment.glsl"));
        manager.load("atm", AtmosphereShaderProvider.class, new AtmosphereShaderProviderParameter("shader/atm.vertex.glsl", "shader/atm.fragment.glsl"));
        manager.load("atmground", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/normal.vertex.glsl", "shader/normal.fragment.glsl"));
        manager.load("cloud", GroundShaderProvider.class, new GroundShaderProviderParameter("shader/cloud.vertex.glsl", "shader/cloud.fragment.glsl"));

        BitmapFontParameter bfp = new BitmapFontParameter();
        bfp.magFilter = TextureFilter.Linear;
        bfp.minFilter = TextureFilter.Linear;
        manager.load("font/main-font.fnt", BitmapFont.class, bfp);
        manager.load("font/font2d.fnt", BitmapFont.class, bfp);
        manager.load("font/font-titles.fnt", BitmapFont.class, bfp);

        stars = new Array<>();

        renderProcesses = new Array<>();

        blendNoDepthRunnable = (renderSystem, renderables, camera) -> {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthMask(false);
        };
        blendDepthRunnable = (renderSystem, renderables, camera) -> {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthMask(true);
        };
        additiveBlendDepthRunnable = (renderSystem, renderables, camera) -> {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
            Gdx.gl.glDepthMask(true);
        };
        restoreRegularBlend = (renderSystem, renderables, camera) -> Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (GlobalConf.scene.SHADOW_MAPPING) {
            // Shadow map camera
            cameraLight = new PerspectiveCamera(0.5f, GlobalConf.scene.SHADOW_MAPPING_RESOLUTION, GlobalConf.scene.SHADOW_MAPPING_RESOLUTION);

            // Aux vectors
            aux1 = new Vector3();
            aux1d = new Vector3d();

            // Build frame buffers and arrays
            buildShadowMapData();
        }

        if (GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING) {
            buildGlowData();
        }
    }

    private ShaderProgram[] fetchShaderProgram(AssetManager manager, AssetDescriptor<ShaderProgram>[] descriptors, String... names) {
        int n = descriptors.length;
        ShaderProgram[] shaders = new ShaderProgram[n];

        for (int i = 0; i < n; i++) {
            shaders[i] = manager.get(descriptors[i]);
            if (!shaders[i].isCompiled()) {
                logger.error(new RuntimeException(), names[i] + " shader compilation failed:\n" + shaders[i].getLog());
            }
        }
        return shaders;
    }

    public void doneLoading(AssetManager manager) {
        IntBuffer intBuffer = BufferUtils.newIntBuffer(16);
        Gdx.gl20.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, intBuffer);
        int maxTexSize = intBuffer.get();
        logger.info("Max texture size: " + maxTexSize + "^2 pixels");

        /*
          STAR BILLBOARD SHADER
         */
        starBillboardShaders = fetchShaderProgram(manager, starBillboardDesc, genShaderFullNames("star-billboard"));

        /*
         * GALAXY SHADER
         */
        galShaders = fetchShaderProgram(manager, galDesc, genShaderFullNames("galaxy"));

        /*
         * FONT SHADER
         */
        ShaderProgram distanceFieldFontShader = manager.get("shader/font.vertex.glsl");
        if (!distanceFieldFontShader.isCompiled()) {
            logger.error(new RuntimeException(), "Distance field font shader compilation failed:\n" + distanceFieldFontShader.getLog());
        }

        /*
         * SPRITE SHADER
         */
        spriteShaders = fetchShaderProgram(manager, spriteDesc, genShaderFullNames("sprite"));

        /*
         * POINT CPU
         */
        pointShaders = fetchShaderProgram(manager, pointDesc, genShaderFullNames("point-cpu"));

        /*
         * LINE CPU
         */
        lineShaders = fetchShaderProgram(manager, lineDesc, genShaderFullNames("line-cpu"));

        /*
         * LINE QUAD
         */
        lineQuadShaders = fetchShaderProgram(manager, lineQuadDesc, genShaderFullNames("line-quad"));

        /*
         * LINE GPU
         */
        lineGpuShaders = fetchShaderProgram(manager, lineGpuDesc, genShaderFullNames("line-gpu"));

        /*
         * MW POINTS
         */
        mwPointShaders = fetchShaderProgram(manager, mwPointDesc, genShaderFullNames("mw-point"));

        /*
         * MW Order-Independent Transparency
         */
        mwOitShaders = fetchShaderProgram(manager, mwOitDesc, genShaderFullNames("gal-oit"));

        /*
         * MW NEBULAE
         */
        mwNebulaShaders = fetchShaderProgram(manager, mwNebulaDesc, genShaderFullNames("mw-nebula"));

        /*
         * PARTICLE EFFECT - default and relativistic
         */
        particleEffectShaders = fetchShaderProgram(manager, particleEffectDesc, genShaderFullNames("particle-effects"));

        /*
         * PARTICLE GROUP - default and relativistic
         */
        particleGroupShaders = fetchShaderProgram(manager, particleGroupDesc, genShaderFullNames("particle-group"));

        /*
         * STAR GROUP - default and relativistic
         */
        starGroupShaders = fetchShaderProgram(manager, starGroupDesc, genShaderFullNames("star-group"));

        /*
         * STAR POINT
         */
        starPointShaders = fetchShaderProgram(manager, starPointDesc, genShaderFullNames("star-point"));

        /*
         * ORBITAL ELEMENTS PARTICLES - default and relativistic
         */
        orbitElemShaders = fetchShaderProgram(manager, orbitElemDesc, genShaderFullNames("orbital-elements-particles"));

        RenderGroup[] renderGroups = RenderGroup.values();
        render_lists = new Array<>(renderGroups.length);
        for (int i = 0; i < renderGroups.length; i++) {
            render_lists.add(new Array<>(40000));
        }

        ShaderProvider sp = manager.get("atmgrounddefault");
        ShaderProvider spadditive = manager.get("additive");
        ShaderProvider spgrids = manager.get("grids");
        ShaderProvider spnormal = manager.get("atmground");
        ShaderProvider spatm = manager.get("atm");
        ShaderProvider spcloud = manager.get("cloud");
        ShaderProvider spsurface = manager.get("spsurface");
        ShaderProvider spbeam = manager.get("spbeam");
        ShaderProvider spdepth = manager.get("spdepth");
        ShaderProvider spopaque = manager.get("spopaque");

        RenderableSorter noSorter = (camera, renderables) -> {
            // Does nothing
        };

        ModelBatch modelBatchDefault = new ModelBatch(sp, noSorter);
        ModelBatch modelBatchMesh = new ModelBatch(spadditive, noSorter);
        modelBatchMesh.getRenderContext().setBlending(true, GL30.GL_ONE, GL30.GL_ONE);
        //modelBatchMesh.getRenderContext().setDepthTest(GL30.GL_LEQUAL, 1e11f, 1e13f);
        ModelBatch modelBatchGrids = new ModelBatch(spgrids, noSorter);
        ModelBatch modelBatchNormal = new ModelBatch(spnormal, noSorter);
        ModelBatch modelBatchAtmosphere = new ModelBatch(spatm, noSorter);
        ModelBatch modelBatchCloud = new ModelBatch(spcloud, noSorter);
        ModelBatch modelBatchStar = new ModelBatch(spsurface, noSorter);
        ModelBatch modelBatchBeam = new ModelBatch(spbeam, noSorter);
        modelBatchDepth = new ModelBatch(spdepth, noSorter);
        modelBatchOpaque = new ModelBatch(spopaque, noSorter);

        // Fonts - all of these are distance field fonts
        BitmapFont font3d = manager.get("font/main-font.fnt");
        BitmapFont font2d = manager.get("font/font2d.fnt");
        BitmapFont fontTitles = manager.get("font/font-titles.fnt");

        // Sprites
        spriteBatch = GlobalResources.spriteBatch;
        spriteBatch.enableBlending();

        // Font batch
        fontBatch = new SpriteBatch(2000, distanceFieldFontShader);
        fontBatch.enableBlending();

        ComponentType[] comps = ComponentType.values();

        // Set reference
        visible = new ComponentTypes();
        for (int i = 0; i < GlobalConf.scene.VISIBILITY.length; i++) {
            if (GlobalConf.scene.VISIBILITY[i]) {
                visible.set(ComponentType.values()[i].ordinal());
            }
        }
        // Invisible are always visible :_D
        visible.set(ComponentType.Invisible.ordinal());

        times = new long[comps.length];
        alphas = new float[comps.length];
        for (int i = 0; i < comps.length; i++) {
            times[i] = -20000l;
            alphas[i] = 0f;
        }

        /*
         * INITIALIZE SGRs
         */
        sgrs = new ISGR[5];
        sgrs[SGR_DEFAULT_IDX] = new SGR();
        sgrs[SGR_STEREO_IDX] = new SGRStereoscopic();
        sgrs[SGR_FOV_IDX] = new SGRFov();
        sgrs[SGR_CUBEMAP_IDX] = new SGRCubemap();
        sgrs[SGR_OPENVR_IDX] = new SGROpenVR(vrContext, modelBatchNormal);
        sgr = null;

        /*
         *
         * ======= INITIALIZE RENDER COMPONENTS =======
         *
         */

        // POINTS
        AbstractRenderSystem pixelStarProc = new StarPointRenderSystem(RenderGroup.POINT_STAR, alphas, starPointShaders, ComponentType.Stars);
        pixelStarProc.setPreRunnable(blendNoDepthRunnable);

        // MODEL FRONT-BACK - NO CULL FACE
        AbstractRenderSystem modelFrontBackProc = new ModelBatchRenderSystem(RenderGroup.MODEL_DEFAULT, alphas, modelBatchDefault, ModelRenderType.NORMAL);
        modelFrontBackProc.setPreRunnable(blendDepthRunnable);
        modelFrontBackProc.setPostRunnable((renderSystem, renderables, camera) -> {
            // This always goes at the back, clear depth buffer
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        });

        // MODEL GRID
        AbstractRenderSystem modelGridsProc = new ModelBatchRenderSystem(RenderGroup.MODEL_GRIDS, alphas, modelBatchGrids, ModelRenderType.NORMAL);
        modelGridsProc.setPreRunnable(blendDepthRunnable);
        modelGridsProc.setPostRunnable((renderSystem, renderables, camera) -> {
            // This always goes at the back, clear depth buffer
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        });

        // VOLUMETRIC CLOUDS
        //        AbstractRenderSystem cloudsProc = new VolumeCloudsRenderSystem(alphas);
        //        cloudsProc.setPreRunnable(blendNoDepthRunnable);

        // ANNOTATIONS
        AbstractRenderSystem annotationsProc = new FontRenderSystem(RenderGroup.FONT_ANNOTATION, alphas, spriteBatch, null);
        annotationsProc.setPreRunnable(blendNoDepthRunnable);
        annotationsProc.setPostRunnable((renderSystem, renderables, camera) -> {
            // This always goes at the back, clear depth buffer
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        });

        // BILLBOARD STARS
        billboardStarsProc = new BillboardStarRenderSystem(RenderGroup.BILLBOARD_STAR, alphas, starBillboardShaders, "data/tex/base/star_glow_s.png", ComponentType.Stars.ordinal());
        billboardStarsProc.setPreRunnable(blendNoDepthRunnable);
        billboardStarsProc.setPostRunnable(new RenderSystemRunnable() {

            private float[] positions = new float[Glow.N * 2];
            private float[] viewAngles = new float[Glow.N];
            private float[] colors = new float[Glow.N * 3];
            private Vector3 auxv = new Vector3();
            private Vector3d auxd = new Vector3d();

            @Override
            public void run(AbstractRenderSystem renderSystem, Array<IRenderable> renderables, ICamera camera) {
                int size = renderables.size;
                if (PostProcessorFactory.instance.getPostProcessor().isLightScatterEnabled() && Particle.renderOn) {
                    // Compute light positions for light scattering or light
                    // glow
                    int lightIndex = 0;
                    float angleEdgeDeg = camera.getAngleEdge() * MathUtils.radDeg;
                    for (int i = size - 1; i >= 0; i--) {
                        IRenderable s = renderables.get(i);
                        if (s instanceof Particle) {
                            Particle p = (Particle) s;
                            if (lightIndex < Glow.N && (GlobalConf.program.CUBEMAP360_MODE || GaiaSky.instance.cam.getDirection().angle(p.translation) < angleEdgeDeg)) {
                                Vector3d pos3d = p.translation.put(auxd);

                                // Aberration
                                GlobalResources.applyRelativisticAberration(pos3d, camera);
                                // GravWaves
                                RelativisticEffectsManager.getInstance().gravitationalWavePos(pos3d);
                                Vector3 pos3 = pos3d.put(auxv);

                                camera.getCamera().project(pos3);
                                // Here we **need** to use
                                // Gdx.graphics.getWidth/Height() because we use
                                // camera.project() which uses screen
                                // coordinates only
                                positions[lightIndex * 2] = auxv.x / Gdx.graphics.getWidth();
                                positions[lightIndex * 2 + 1] = auxv.y / Gdx.graphics.getHeight();
                                viewAngles[lightIndex] = (float) p.viewAngleApparent;
                                colors[lightIndex * 3] = p.cc[0];
                                colors[lightIndex * 3 + 1] = p.cc[1];
                                colors[lightIndex * 3 + 2] = p.cc[2];
                                lightIndex++;
                            }
                        }
                    }
                    EventManager.instance.post(Events.LIGHT_POS_2D_UPDATED, lightIndex, positions, viewAngles, colors, glowTex);
                } else {
                    EventManager.instance.post(Events.LIGHT_POS_2D_UPDATED, 0, positions, viewAngles, colors, glowTex);
                }
            }

        });

        // BILLBOARD GALAXIES
        AbstractRenderSystem billboardGalaxiesProc = new BillboardStarRenderSystem(RenderGroup.BILLBOARD_GAL, alphas, galShaders, "data/tex/base/static.jpg", ComponentType.Galaxies.ordinal());
        billboardGalaxiesProc.setPreRunnable(blendNoDepthRunnable);

        // BILLBOARD SPRITES
        AbstractRenderSystem billboardSpritesProc = new BillboardSpriteRenderSystem(RenderGroup.BILLBOARD_SPRITE, alphas, spriteShaders, ComponentType.Clusters.ordinal());
        billboardSpritesProc.setPreRunnable(blendNoDepthRunnable);

        // LINES CPU
        AbstractRenderSystem lineProc = getLineRenderSystem();

        // LINES GPU
        AbstractRenderSystem lineGpuProc = new VertGPURenderSystem(RenderGroup.LINE_GPU, alphas, lineGpuShaders, GL20.GL_LINE_STRIP);
        lineGpuProc.setPreRunnable(blendDepthRunnable);

        // POINTS CPU
        AbstractRenderSystem pointProc = new PointRenderSystem(RenderGroup.POINT, alphas, pointShaders);

        // POINTS GPU
        AbstractRenderSystem pointGpuProc = new VertGPURenderSystem(RenderGroup.POINT_GPU, alphas, lineGpuShaders, GL20.GL_POINTS);
        pointGpuProc.setPreRunnable(blendDepthRunnable);

        // MODEL MESH
        AbstractRenderSystem modelMeshProc = new ModelBatchRenderSystem(RenderGroup.MODEL_MESH, alphas, modelBatchMesh, ModelRenderType.NORMAL, false);
        modelMeshProc.setPreRunnable(blendDepthRunnable);

        // LINES VR LASER
        LineRenderSystem lineVRProc = new LineRenderSystem(RenderGroup.LINE_VR, alphas, lineShaders);
        lineVRProc.setPreRunnable(blendDepthRunnable);

        // MODEL FRONT
        AbstractRenderSystem modelFrontProc = new ModelBatchRenderSystem(RenderGroup.MODEL_NORMAL, alphas, modelBatchNormal, ModelRenderType.NORMAL);
        modelFrontProc.setPreRunnable(blendDepthRunnable);

        // MODEL BEAM
        AbstractRenderSystem modelBeamProc = new ModelBatchRenderSystem(RenderGroup.MODEL_BEAM, alphas, modelBatchBeam, ModelRenderType.NORMAL, false);
        modelBeamProc.setPreRunnable(blendDepthRunnable);

        // GALAXY
        //mwrs = new MWModelRenderSystem(RenderGroup.GALAXY, alphas, MWModelRenderSystem.oit ? mwOitShaders : mwPointShaders);
        //AbstractRenderSystem galaxyProc = mwrs;
        AbstractRenderSystem galaxyProc = new MilkyWayRenderSystem(RenderGroup.GALAXY, alphas, modelBatchDefault, mwPointShaders, mwNebulaShaders);
        galaxyProc.setPreRunnable(blendNoDepthRunnable);

        // PARTICLE EFFECTS
        AbstractRenderSystem particleEffectsProc = new ParticleEffectsRenderSystem(null, alphas, particleEffectShaders);
        particleEffectsProc.setPreRunnable(blendNoDepthRunnable);

        // PARTICLE GROUP
        AbstractRenderSystem particleGroupProc = new ParticleGroupRenderSystem(RenderGroup.PARTICLE_GROUP, alphas, particleGroupShaders);
        particleGroupProc.setPreRunnable(blendNoDepthRunnable);

        // STAR GROUP
        AbstractRenderSystem starGroupProc = new StarGroupRenderSystem(RenderGroup.STAR_GROUP, alphas, starGroupShaders);
        starGroupProc.setPreRunnable(additiveBlendDepthRunnable);
        starGroupProc.setPostRunnable(restoreRegularBlend);

        // ORBITAL ELEMENTS PARTICLES
        AbstractRenderSystem orbitElemProc = new OrbitalElementsParticlesRenderSystem(RenderGroup.PARTICLE_ORBIT_ELEMENTS, alphas, orbitElemShaders);
        orbitElemProc.setPreRunnable(blendNoDepthRunnable);

        // MODEL STARS
        AbstractRenderSystem modelStarsProc = new ModelBatchRenderSystem(RenderGroup.MODEL_STAR, alphas, modelBatchStar, ModelRenderType.NORMAL);
        modelStarsProc.setPreRunnable(blendDepthRunnable);

        // LABELS
        AbstractRenderSystem labelsProc = new FontRenderSystem(RenderGroup.FONT_LABEL, alphas, fontBatch, distanceFieldFontShader, font3d, font2d, fontTitles);
        labelsProc.setPreRunnable(blendNoDepthRunnable);

        // BILLBOARD SSO
        AbstractRenderSystem billboardSSOProc = new BillboardStarRenderSystem(RenderGroup.BILLBOARD_SSO, alphas, starBillboardShaders, "data/tex/base/sso.png", -1);
        billboardSSOProc.setPreRunnable(additiveBlendDepthRunnable);
        billboardSSOProc.setPostRunnable(restoreRegularBlend);

        // MODEL ATMOSPHERE
        AbstractRenderSystem modelAtmProc = new ModelBatchRenderSystem(RenderGroup.MODEL_ATM, alphas, modelBatchAtmosphere, ModelRenderType.ATMOSPHERE) {
            @Override
            public float getAlpha(IRenderable s) {
                return alphas[ComponentType.Atmospheres.ordinal()] * (float) Math.pow(alphas[s.getComponentType().getFirstOrdinal()], 2);
            }

            @Override
            protected boolean mustRender() {
                return alphas[ComponentType.Atmospheres.ordinal()] * alphas[ComponentType.Planets.ordinal()] > 0;
            }
        };
        modelAtmProc.setPreRunnable(blendDepthRunnable);
        modelAtmProc.setPostRunnable((renderSystem, renderables, camera) -> {
            // Clear depth buffer before rendering things up close
            //Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        });

        // MODEL CLOUDS
        AbstractRenderSystem modelCloudProc = new ModelBatchRenderSystem(RenderGroup.MODEL_CLOUD, alphas, modelBatchCloud, ModelRenderType.CLOUD);

        // SHAPES
        AbstractRenderSystem shapeProc = new ShapeRenderSystem(RenderGroup.SHAPE, alphas);
        shapeProc.setPreRunnable(blendNoDepthRunnable);

        // MODEL CLOSE UP
        //        AbstractRenderSystem modelCloseUpProc = new ModelBatchRenderSystem(RenderGroup.MODEL_CLOSEUP,  alphas, modelBatchCloseUp, false);
        //        modelCloseUpProc.setPreRunnable(blendDepthRunnable);

        // Add components to set
        renderProcesses.add(modelFrontBackProc);
        renderProcesses.add(modelGridsProc);
        renderProcesses.add(pixelStarProc);
        renderProcesses.add(starGroupProc);
        renderProcesses.add(orbitElemProc);
        renderProcesses.add(annotationsProc);
        renderProcesses.add(particleGroupProc);

        // Billboards for galaxies and stars
        renderProcesses.add(billboardGalaxiesProc);
        renderProcesses.add(billboardStarsProc);
        renderProcesses.add(galaxyProc);

        // Billboard for sprites
        renderProcesses.add(billboardSpritesProc);

        // Models
        renderProcesses.add(modelFrontProc);
        renderProcesses.add(modelBeamProc);
        renderProcesses.add(modelMeshProc);

        // Labels
        renderProcesses.add(labelsProc);

        // Primitives
        renderProcesses.add(lineProc);
        renderProcesses.add(lineGpuProc);
        renderProcesses.add(lineVRProc);
        renderProcesses.add(pointProc);
        renderProcesses.add(pointGpuProc);

        // Billboards SSO
        renderProcesses.add(billboardSSOProc);

        renderProcesses.add(modelStarsProc);
        renderProcesses.add(modelAtmProc);
        renderProcesses.add(modelCloudProc);
        renderProcesses.add(shapeProc);
        renderProcesses.add(particleEffectsProc);
        // renderProcesses.add(cloudsProc);
        // renderProcesses.add(modelCloseUpProc);

        // Use Direct3D [0..1] depth range instead of OpenGL default's [-1..1]
        //ARBClipControl.glClipControl(ARBClipControl.GL_LOWER_LEFT, ARBClipControl.GL_ZERO_TO_ONE);
        //Gdx.gl30.glDepthRangef(0, 1);

        EventManager.instance.subscribe(this, Events.TOGGLE_VISIBILITY_CMD, Events.PIXEL_RENDERER_UPDATE, Events.LINE_RENDERER_UPDATE, Events.STEREOSCOPIC_CMD, Events.CAMERA_MODE_CMD, Events.CUBEMAP360_CMD, Events.REBUILD_SHADOW_MAP_DATA_CMD, Events.LIGHT_SCATTERING_CMD);

    }

    private String[] genShaderNames(String baseName) {
        return new String[] { baseName, baseName + "Rel", baseName + "Grav", baseName + "RelGrav" };
    }

    private String[] genShaderFullNames(String baseName) {
        return new String[] { baseName, baseName + " (rel)", baseName + " (grav)", baseName + " (rel+grav)" };
    }

    private void initSGR(ICamera camera) {
        if (GlobalConf.runtime.OPENVR) {
            // Using Steam OpenVR renderer
            sgr = sgrs[SGR_OPENVR_IDX];
        } else if (camera.getNCameras() > 1) {
            // FOV mode
            sgr = sgrs[SGR_FOV_IDX];
        } else if (GlobalConf.program.STEREOSCOPIC_MODE) {
            // Stereoscopic mode
            sgr = sgrs[SGR_STEREO_IDX];
        } else if (GlobalConf.program.CUBEMAP360_MODE) {
            // 360 mode: cube map -> equirectangular map
            sgr = sgrs[SGR_CUBEMAP_IDX];
        } else {
            // Default mode
            sgr = sgrs[SGR_DEFAULT_IDX];
        }
    }

    Array<StubModel> controllers = new Array<StubModel>();
    PerspectiveCamera camaux = new PerspectiveCamera();

    private void copyCamera(PerspectiveCamera from, PerspectiveCamera to) {
        to.combined.set(from.combined);
        to.view.set(from.view);
        to.invProjectionView.set(from.invProjectionView);
        to.direction.set(from.direction);
        to.position.set(from.position);
        to.up.set(from.up);

        to.near = from.near;
        to.far = from.far;
        to.fieldOfView = from.fieldOfView;
    }

    public void renderGlowPass(ICamera camera) {
        if (GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING && glowFb != null) {
            // Get all billboard stars
            Array<IRenderable> bbstars = render_lists.get(RenderGroup.BILLBOARD_STAR.ordinal());
            stars.clear();
            for (IRenderable st : bbstars) {
                if (st instanceof Star) {
                    stars.add(st);
                    break;
                }
            }

            // Get all models
            Array<IRenderable> models = render_lists.get(RenderGroup.MODEL_NORMAL.ordinal());

            // Controllers
            SGROpenVR sgrov = (SGROpenVR) sgrs[SGR_OPENVR_IDX];
            if (vrContext != null) {
                for (StubModel m : sgrov.controllerObjects) {
                    if (!models.contains(m, true))
                        controllers.add(m);
                }
            }

            glowFb.begin();
            Gdx.gl.glClearColor(0, 0, 0, 0);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            // Render billboard stars
            billboardStarsProc.renderStud(stars, camera, 0);

            if (!GlobalConf.program.CUBEMAP360_MODE) {
                // Render billboard stars
                billboardStarsProc.renderStud(stars, camera, 0);

                // Render models
                modelBatchOpaque.begin(camera.getCamera());
                for (IRenderable model : models) {
                    if (model instanceof ModelBody) {
                        ModelBody mb = (ModelBody) model;
                        mb.renderOpaque(modelBatchOpaque, 1, 0);
                    }
                }
                modelBatchOpaque.end();
            }

            if (vrContext != null) {
                copyCamera(camera.getCamera(), camaux);
                sgrov.renderStubModels(modelBatchOpaque, null, camaux, controllers, 0);
                sgrov.renderStubModels(modelBatchOpaque, null, camaux, controllers, 1);
            }
            controllers.clear();

            // Save to texture for later use
            glowTex = glowFb.getColorBufferTexture();

            glowFb.end();

        }

    }

    private void renderShadowMap(ICamera camera) {
        if (GlobalConf.scene.SHADOW_MAPPING) {
            /*
             * Shadow mapping here?
             * <ul>
             * <li>Extract model bodies (front)</li>
             * <li>Work out light direction</li>
             * <li>Set orthographic camera at set distance from bodies,
             * direction of light, clip planes</li>
             * <li>Render depth map to frame buffer (fb)</li>
             * <li>Send frame buffer texture in to ModelBatchRenderSystem along
             * with light position, direction, clip planes and light camera
             * combined matrix</li>
             * <li>Compare real distance from light to texture sample, render
             * shadow if different</li>
             * </ul>
             */
            Array<IRenderable> models = render_lists.get(RenderGroup.MODEL_NORMAL.ordinal());
            models.sort(Comparator.comparingDouble(a -> ((AbstractPositionEntity) a).getDistToCamera()));

            int shadowNRender = (GlobalConf.program.STEREOSCOPIC_MODE || GlobalConf.runtime.OPENVR) ? 2 : GlobalConf.program.CUBEMAP360_MODE ? 6 : 1;

            if (candidates != null && shadowMapFb != null && smCombinedMap != null) {
                candidates.clear();
                int num = 0;
                for (int i = 0; i < models.size; i++) {
                    if (models.get(i) instanceof ModelBody) {
                        ModelBody mr = (ModelBody) models.get(i);
                        if (mr.isShadow()) {
                            candidates.insert(num, mr);
                            mr.shadow = 0;
                            num++;
                            if (num == GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS)
                                break;
                        }
                    }
                }

                // Clear maps
                smTexMap.clear();
                smCombinedMap.clear();
                int i = 0;
                for (ModelBody candidate : candidates) {
                    // Yes!
                    candidate.shadow = shadowNRender;

                    Vector3 camDir = aux1.set(candidate.mc.dlight.direction);
                    // Direction is that of the light
                    cameraLight.direction.set(camDir);

                    double radius = candidate.getRadius();
                    // Distance from camera to object, radius * sv[0]
                    double distance = radius * candidate.shadowMapValues[0];
                    // Position, factor of radius
                    candidate.getAbsolutePosition(aux1d);
                    aux1d.sub(camera.getPos()).sub(camDir.nor().scl((float) distance));
                    aux1d.put(cameraLight.position);
                    // Up is perpendicular to dir
                    if (cameraLight.direction.y != 0 || cameraLight.direction.z != 0)
                        aux1.set(1, 0, 0);
                    else
                        aux1.set(0, 1, 0);
                    cameraLight.up.set(cameraLight.direction).crs(aux1);

                    // Near is sv[1]*radius before the object
                    cameraLight.near = (float) (distance - radius * candidate.shadowMapValues[1]);
                    // Far is sv[2]*radius after the object
                    cameraLight.far = (float) (distance + radius * candidate.shadowMapValues[2]);

                    // Update cam
                    cameraLight.update(false);

                    // Render model depth map to frame buffer
                    shadowMapFb[i].begin();
                    Gdx.gl.glClearColor(0, 0, 0, 0);
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
                    modelBatchDepth.begin(cameraLight);
                    candidate.render(modelBatchDepth, 1, 0, null);
                    modelBatchDepth.end();

                    // Save frame buffer and combined matrix
                    candidate.shadow = shadowNRender;
                    shadowMapCombined[i].set(cameraLight.combined);
                    smCombinedMap.put(candidate, shadowMapCombined[i]);
                    smTexMap.put(candidate, shadowMapFb[i].getColorBufferTexture());

                    shadowMapFb[i].end();
                    i++;
                }
            }
        }
    }

    private void renderMWPrePass(ICamera camera) {
        if (mwrs != null) {
            Array<IRenderable> arr = render_lists.get(RenderGroup.GALAXY.ordinal());
            if (arr != null && arr.size > 0)
                mwrs.renderPrePasses((MilkyWay) arr.get(0), camera);
        }
    }

    @Override
    public void render(ICamera camera, double t, int rw, int rh, FrameBuffer fb, PostProcessBean ppb) {
        if (sgr == null)
            initSGR(camera);

        // Shadow maps are the same for all
        renderShadowMap(camera);

        // In stereo and cubemap modes, the glow pass is rendered in the SGR itself
        if (!GlobalConf.program.STEREOSCOPIC_MODE && !GlobalConf.program.CUBEMAP360_MODE && !GlobalConf.runtime.OPENVR)
            renderGlowPass(camera);
        
        renderMWPrePass(camera);

        sgr.render(this, camera, t, rw, rh, fb, ppb);

        if (mwrs != null && MWModelRenderSystem.oit) {
            spriteBatch.begin();
            spriteBatch.draw(mwrs.oitFb.getTextureAttachments().get(0), 0, 0, 756, 504);
            spriteBatch.end();
        }

    }

    /**
     * Renders the scene
     *
     * @param camera The camera to use
     * @param t      The time in seconds since the start
     * @param rc     The render context
     */
    public void renderScene(ICamera camera, double t, RenderingContext rc) {
        // Update time difference since last update
        for (ComponentType ct : ComponentType.values()) {
            alphas[ct.ordinal()] = calculateAlpha(ct, t);
        }

        int size = renderProcesses.size;
        for (int i = 0; i < size; i++) {
            IRenderSystem process = renderProcesses.get(i);
            // If we have no render group, this means all the info is already in
            // the render system. No lists needed
            if (process.getRenderGroup() != null) {
                Array<IRenderable> l = render_lists.get(process.getRenderGroup().ordinal());
                process.render(l, camera, t, rc);
            } else {
                process.render(null, camera, t, rc);
            }
        }

    }

    /**
     * Renders all the systems which are the same type of the given class
     *
     * @param camera The camera to use
     * @param t      The time in seconds since the start
     * @param rc     The render contex
     * @param clazz  The class
     */
    public void renderSystem(ICamera camera, double t, RenderingContext rc, Class<? extends IRenderSystem> clazz) {
        // Update time difference since last update
        for (ComponentType ct : ComponentType.values()) {
            alphas[ct.ordinal()] = calculateAlpha(ct, t);
        }

        int size = renderProcesses.size;
        for (int i = 0; i < size; i++) {
            IRenderSystem process = renderProcesses.get(i);
            if (clazz.isInstance(process)) {
                // If we have no render group, this means all the info is already in
                // the render system. No lists needed
                if (process.getRenderGroup() != null) {
                    Array<IRenderable> l = render_lists.get(process.getRenderGroup().ordinal());
                    process.render(l, camera, t, rc);
                } else {
                    process.render(null, camera, t, rc);
                }
            }
        }
    }

    /**
     * This must be called when all the rendering for the current frame has
     * finished.
     */
    public void clearLists() {
        for (RenderGroup rg : RenderGroup.values()) {
            render_lists.get(rg.ordinal()).clear();
        }
    }

    /**
     * Checks if a given component type is on
     *
     * @param comp The component
     * @return Whether the component is on
     */
    public boolean isOn(ComponentType comp) {
        return visible.get(comp.ordinal()) || alphas[comp.ordinal()] > 0;
    }

    /**
     * Checks if the component types are all on
     *
     * @param comp The components
     * @return Whether the components are all on
     */
    public boolean isOn(ComponentTypes comp) {
        boolean allon = comp.allSetLike(visible);

        if (!allon) {
            allon = true;
            for (int i = comp.nextSetBit(0); i >= 0; i = comp.nextSetBit(i + 1)) {
                // operate on index i here
                allon = allon && alphas[i] > 0;
                if (i == Integer.MAX_VALUE) {
                    break; // or (i+1) would overflow
                }
            }
        }
        return allon;
    }

    public boolean isOn(int ordinal) {
        return visible.get(ordinal) || alphas[ordinal] > 0;
    }

    @Override
    public void notify(Events event, final Object... data) {
        switch (event) {
        case TOGGLE_VISIBILITY_CMD:
            ComponentType ct = ComponentType.getFromKey((String) data[0]);
            int idx = ct.ordinal();
            if (data.length == 3) {
                // We have the boolean
                boolean currvis = visible.get(ct.ordinal());
                boolean newvis = (boolean) data[2];
                if (currvis != newvis) {
                    // Only update if visibility different
                    if (newvis)
                        visible.set(ct.ordinal());
                    else
                        visible.clear(ct.ordinal());
                    times[idx] = (long) (GaiaSky.instance.getT() * 1000f);
                }
            } else {
                // Only toggle
                visible.flip(ct.ordinal());
                times[idx] = (long) (GaiaSky.instance.getT() * 1000f);
            }
            break;

        case PIXEL_RENDERER_UPDATE:
            Gdx.app.postRunnable(() -> {
                AbstractRenderSystem.POINT_UPDATE_FLAG = true;
                // updatePixelRenderSystem();
            });
            break;
        case LINE_RENDERER_UPDATE:
            Gdx.app.postRunnable(() -> updateLineRenderSystem());
            break;
        case STEREOSCOPIC_CMD:
            boolean stereo = (Boolean) data[0];
            if (stereo)
                sgr = sgrs[SGR_STEREO_IDX];
            else {
                if (GlobalConf.runtime.OPENVR)
                    sgr = sgrs[SGR_OPENVR_IDX];
                else
                    sgr = sgrs[SGR_DEFAULT_IDX];
            }
            break;
        case CUBEMAP360_CMD:
            boolean cubemap = (Boolean) data[0];
            if (cubemap) {
                sgr = sgrs[SGR_CUBEMAP_IDX];
            } else {
                if (GlobalConf.runtime.OPENVR)
                    sgr = sgrs[SGR_OPENVR_IDX];
                else
                    sgr = sgrs[SGR_DEFAULT_IDX];
            }
            break;
        case CAMERA_MODE_CMD:
            CameraMode cm = (CameraMode) data[0];
            if (cm.isGaiaFov())
                sgr = sgrs[SGR_FOV_IDX];
            else {
                if (GlobalConf.runtime.OPENVR)
                    sgr = sgrs[SGR_OPENVR_IDX];
                else if (GlobalConf.program.STEREOSCOPIC_MODE)
                    sgr = sgrs[SGR_STEREO_IDX];
                else if (GlobalConf.program.CUBEMAP360_MODE)
                    sgr = sgrs[SGR_CUBEMAP_IDX];
                else
                    sgr = sgrs[SGR_DEFAULT_IDX];

            }
            break;
        case REBUILD_SHADOW_MAP_DATA_CMD:
            buildShadowMapData();
            break;
        case LIGHT_SCATTERING_CMD:
            boolean glow = (Boolean) data[0];
            if (glow) {
                buildGlowData();
            }
            break;
        default:
            break;
        }
    }

    /**
     * Computes the alpha for the given component type.
     *
     * @param type The component type.
     * @param t    The current time in seconds.
     * @return The alpha value.
     */
    private float calculateAlpha(ComponentType type, double t) {
        int ordinal = type.ordinal();
        long diff = (long) (t * 1000f) - times[ordinal];
        if (diff > GlobalConf.scene.OBJECT_FADE_MS) {
            if (visible.get(ordinal)) {
                alphas[ordinal] = 1;
            } else {
                alphas[ordinal] = 0;
            }
            return alphas[ordinal];
        } else {
            return visible.get(ordinal) ? MathUtilsd.lint(diff, 0, GlobalConf.scene.OBJECT_FADE_MS, 0, 1) : MathUtilsd.lint(diff, 0, GlobalConf.scene.OBJECT_FADE_MS, 1, 0);
        }
    }

    public void resize(final int w, final int h) {
        resize(w, h, false);
    }

    public void resize(final int w, final int h, boolean resizeRenderSys) {
        if (resizeRenderSys)
            resizeRenderSystems(w, h);

        for (ISGR sgr : sgrs) {
            sgr.resize(w, h);
        }
    }

    public void resizeRenderSystems(final int w, final int h) {
        for (IRenderSystem rendSys : renderProcesses) {
            rendSys.resize(w, h);
        }
    }

    public void dispose() {
        if (sgrs != null)
            for (ISGR sgr : sgrs) {
                if (sgr != null)
                    sgr.dispose();
            }
        if (modelBatchDepth != null)
            modelBatchDepth.dispose();
        if (shadowMapFb != null)
            for (FrameBuffer fb : shadowMapFb)
                fb.dispose();
        if (spriteBatch != null)
            spriteBatch.dispose();
        if (fontBatch != null)
            fontBatch.dispose();
    }

    /**
     * Builds the shadow map data; frame buffers, arrays, etc.
     */
    private void buildShadowMapData() {
        if (shadowMapFb != null) {
            for (FrameBuffer fb : shadowMapFb)
                fb.dispose();
            shadowMapFb = null;
        }
        shadowMapCombined = null;

        // Shadow map frame buffer
        shadowMapFb = new FrameBuffer[GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS];
        // Shadow map combined matrices
        shadowMapCombined = new Matrix4[GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS];
        // Init
        for (int i = 0; i < GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS; i++) {
            shadowMapFb[i] = new FrameBuffer(Format.RGBA8888, GlobalConf.scene.SHADOW_MAPPING_RESOLUTION, GlobalConf.scene.SHADOW_MAPPING_RESOLUTION, true);
            shadowMapCombined[i] = new Matrix4();
        }
        if (smTexMap == null)
            smTexMap = new HashMap<>();
        smTexMap.clear();

        if (smCombinedMap == null)
            smCombinedMap = new HashMap<>();
        smCombinedMap.clear();

        if (candidates == null)
            candidates = new Array<>(GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS);
        candidates.clear();
    }

    private void buildGlowData() {
        if (glowFb == null)
            glowFb = new FrameBuffer(Format.RGBA8888, 1080, 720, false);
    }

    public void updateLineRenderSystem() {
        LineRenderSystem current = null;
        for (IRenderSystem proc : renderProcesses) {
            if (proc instanceof LineRenderSystem) {
                current = (LineRenderSystem) proc;
            }
        }
        final int idx = renderProcesses.indexOf(current, true);
        if ((current instanceof LineQuadRenderSystem && GlobalConf.scene.isNormalLineRenderer()) || (!(current instanceof LineQuadRenderSystem) && !GlobalConf.scene.isNormalLineRenderer())) {
            renderProcesses.removeIndex(idx);
            AbstractRenderSystem lineSys = getLineRenderSystem();
            lineSys.setPreRunnable(blendDepthRunnable);
            renderProcesses.insert(idx, lineSys);
            current.dispose();
        }
    }

    private AbstractRenderSystem getLineRenderSystem() {
        AbstractRenderSystem sys;
        if (GlobalConf.scene.isNormalLineRenderer()) {
            // Normal
            sys = new LineRenderSystem(RenderGroup.LINE, alphas, lineShaders);
            sys.setPreRunnable(blendDepthRunnable);
        } else {
            // Quad
            sys = new LineQuadRenderSystem(RenderGroup.LINE, alphas, lineQuadShaders);
            sys.setPreRunnable(blendDepthRunnable);
        }
        return sys;
    }

}
