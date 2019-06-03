/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.data.group.IStarGroupDataProvider;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.*;
import gaia.cu9.ari.gaiaorbit.render.system.FontRenderSystem;
import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.FovCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.color.ColourUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModel;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModelInstance;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;
import net.jafama.FastMath;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;

/**
 * A particle group which additionally to the xyz position, supports color and
 * magnitude. id x y z pmx pmy pmz appmag absmag col size additional
 *
 * @author tsagrista
 */
public class StarGroup extends ParticleGroup implements ILineRenderable, IStarFocus, IQuadRenderable, IModelRenderable, IObserver {

    /**
     * Contains info on one star
     */
    public static class StarBean extends ParticleBean {
        private static final long serialVersionUID = 1L;

        public static final int SIZE = 14;
        /* INDICES */

        /* Stored doubles */
        public static final int I_PMX = 3;
        public static final int I_PMY = 4;
        public static final int I_PMZ = 5;
        public static final int I_MUALPHA = 6;
        public static final int I_MUDELTA = 7;
        public static final int I_RADVEL = 8;

        /* Stored as float */
        public static final int I_APPMAG = 9;
        public static final int I_ABSMAG = 10;
        public static final int I_COL = 11;
        public static final int I_SIZE = 12;

        /* Stored as int */
        public static final int I_HIP = 13;

        public Long id;
        public transient OctreeNode octant;
        public String name;

        public StarBean(double[] data, Long id, String name) {
            super(data);
            this.id = id;
            this.name = name;
            this.octant = null;
        }

        public double pmx() {
            return data[I_PMX];
        }

        public double pmy() {
            return data[I_PMY];
        }

        public double pmz() {
            return data[I_PMZ];
        }

        public double appmag() {
            return data[I_APPMAG];
        }

        public double absmag() {
            return data[I_ABSMAG];
        }

        public double col() {
            return data[I_COL];
        }

        public double size() {
            return data[I_SIZE];
        }

        public double radius() {
            return size() * Constants.STAR_SIZE_FACTOR;
        }

        public int hip() {
            return (int) data[I_HIP];
        }

        public double mualpha() {
            return data[I_MUALPHA];
        }

        public double mudelta() {
            return data[I_MUDELTA];
        }

        public double radvel() {
            return data[I_RADVEL];
        }

    }

    // Camera dx threshold
    private static final double CAM_DX_TH = 100 * Constants.AU_TO_U;
    // Min update time
    private static final double MIN_UPDATE_TIME_MS = 100;
    // Sequence id
    private static long idSeq = 0;
    /**
     * Star model
     **/
    private static ModelComponent mc;
    // Model transform
    private static Matrix4 modelTransform;

    /**
     * Epoch in julian days
     **/
    private double epoch_jd = AstroUtils.JD_J2015_5;
    /**
     * Current computed epoch time
     **/
    private double currDeltaYears = 0;

    private static void initModel() {
        if (mc == null) {
            Texture tex = new Texture(GlobalConf.data.dataFile("tex/base/star.jpg"));
            Texture lut = new Texture(GlobalConf.data.dataFile("tex/base/lut.jpg"));
            tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

            Map<String, Object> params = new TreeMap<>();
            params.put("quality", 120l);
            params.put("diameter", 1d);
            params.put("flip", false);

            Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
            IntModel model = pair.getFirst();
            Material mat = pair.getSecond().get("base");
            mat.clear();
            mat.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
            mat.set(new TextureAttribute(TextureAttribute.Normal, lut));
            // Only to activate view vector (camera position)
            mat.set(new TextureAttribute(TextureAttribute.Specular, lut));
            mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            modelTransform = new Matrix4();
            mc = new ModelComponent(false);
            mc.initialize();
            mc.env = new Environment();
            mc.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
            mc.env.set(new FloatAttribute(FloatAttribute.Shininess, 0f));
            mc.instance = new IntModelInstance(model, modelTransform);
            // Relativistic effects
            if (GlobalConf.runtime.RELATIVISTIC_ABERRATION)
                mc.rec.setUpRelativisticEffectsMaterial(mc.instance.materials);
        }
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        private int sequence = 0;

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "updater-daemon-" + sequence);
            sequence++;
            t.setDaemon(true);
            return t;
        }

    }

    /**
     * Thread pool executor
     */
    private static ThreadPoolExecutor pool;
    private static BlockingQueue<Runnable> workQueue;

    static {
        workQueue = new LinkedBlockingQueue<>();
        int nThreads = !GlobalConf.performance.MULTITHREADING ? 1 : Math.max(1, GlobalConf.performance.NUMBER_THREADS() - 1);
        pool = new ThreadPoolExecutor(nThreads, nThreads, 5, TimeUnit.SECONDS, workQueue);
        pool.setThreadFactory(new DaemonThreadFactory());
    }

    /**
     * The name index
     */
    ObjectIntMap<String> index;

    /**
     * Additional values
     */
    double[] additional;

    // Indices list buffer 1
    Integer[] indices1;
    // Indices list buffer 2
    Integer[] indices2;
    // Active indices list
    Integer[] active;
    // Background indices list (the one we sort)
    Integer[] background;

    // Close up stars treated
    private int N_CLOSEUP_STARS;

    // Updater task
    private UpdaterTask updaterTask;

    /**
     * CLOSEST
     **/
    private Vector3d closestPos, closestPm;
    private String closestName;
    private double closestDist;
    private double closestSize;
    private float[] closestCol;

    private double modelDist;

    private Vector3d lastSortCameraPos;

    /**
     * Comparator
     **/
    private Comparator<Integer> comp;

    private Vector3d aux;

    // Is it updating?
    private volatile boolean updating = false;

    public StarGroup() {
        super();
        id = idSeq++;
        comp = new StarGroupComparator();
        closestPos = new Vector3d();
        closestPm = new Vector3d();
        lastSortCameraPos = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        closestCol = new float[4];
        lastSortTime = -1;
        aux = new Vector3d();
        EventManager.instance.subscribe(this, Events.CAMERA_MOTION_UPDATED, Events.GRAPHICS_QUALITY_UPDATED);
    }

    @SuppressWarnings("unchecked")
    public void initialize() {
        /** Load data **/
        try {
            Class<?> clazz = Class.forName(provider);
            IStarGroupDataProvider provider = (IStarGroupDataProvider) clazz.getConstructor().newInstance();

            if (factor == null)
                factor = 1d;

            // Set data, generate index
            Array<StarBean> l = (Array<StarBean>) provider.loadData(datafile, factor);
            this.setData(l);

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
            pointData = null;
        }

        computeFixedMeanPosition();
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        initModel();

        // Additional
        additional = new double[pointData.size];

        // Initialise indices list with natural order
        indices1 = new Integer[pointData.size];
        indices2 = new Integer[pointData.size];
        for (int i = 0; i < pointData.size; i++) {
            indices1[i] = i;
            indices2[i] = i;
        }
        active = indices1;
        background = indices2;

        /**
         * INIT UPDATER TASK
         */
        updaterTask = new UpdaterTask(this);
    }

    /**
     * Returns the data list
     *
     * @return The data list
     */
    @SuppressWarnings("unchecked")
    public Array<StarBean> data() {
        return (Array<StarBean>) pointData;
    }

    public void setData(Array<StarBean> pointData) {
        setData(pointData, true);
    }

    public void setData(Array<StarBean> pointData, boolean regenerateIndex) {
        this.pointData = pointData;
        this.N_CLOSEUP_STARS = getNCloseupStars();

        // Regenerate index
        if (regenerateIndex)
            regenerateIndex();
    }

    private int getNCloseupStars() {
        return Math.min(GlobalConf.scene.isHighQuality() ? 80 : (GlobalConf.scene.isNormalQuality() ? 60 : 40), pointData.size);
    }

    public void regenerateIndex() {
        index = generateIndex(data());
    }

    /**
     * Generates the index (maps star name and id to array index)
     * and computes the geometric center of this star group
     *
     * @param pointData The star data
     * @return An map{string,int} mapping names/ids to indexes
     */
    public ObjectIntMap<String> generateIndex(Array<StarBean> pointData) {
        ObjectIntMap<String> index = new ObjectIntMap<>();
        int n = pointData.size;
        for (int i = 0; i < n; i++) {
            StarBean sb = pointData.get(i);
            String lcname = sb.name.toLowerCase();
            index.put(lcname, i);
            String lcid = sb.id.toString().toLowerCase();
            if (sb.id > 0 && !lcid.equals(lcname)) {
                index.put(lcid, i);
            }
            if (sb.hip() > 0) {
                String lchip = "hip " + sb.hip();
                if (!lchip.equals(lcname))
                    index.put(lchip, i);
            }
        }
        return index;
    }

    public void computeFixedMeanPosition() {
        if (!fixedMeanPosition) {
            // Mean position
            for (StarBean point : data()) {
                pos.add(point.x(), point.y(), point.z());
            }
            pos.scl(1d / pointData.size);
        }
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
        // Fade node visibility
        if (this.isVisible() && active.length > 0) {
            // Delta years
            currDeltaYears = AstroUtils.getMsSince(time.getTime(), epoch_jd) * Nature.MS_TO_Y;

            super.update(time, parentTransform, camera, opacity);

            // Update closest star
            StarBean closestStar = (StarBean) pointData.get(active[0]);

            closestPm.set(closestStar.pmx(), closestStar.pmy(), closestStar.pmz()).scl(currDeltaYears);
            closestPos.set(closestStar.x(), closestStar.y(), closestStar.z()).sub(camera.getPos()).add(closestPm);
            closestDist = closestPos.len() - getRadius(active[0]);
            Color c = new Color();
            Color.abgr8888ToColor(c, (float) closestStar.col());
            closestCol[0] = c.r;
            closestCol[1] = c.g;
            closestCol[2] = c.b;
            closestCol[3] = c.a;
            closestSize = getSize(active[0]);
            closestName = closestStar.name;
            camera.setClosestStar(this);

            // Model dist
            modelDist = 172.4643429 * getRadius(active[0]);
        }

    }

    /**
     * Updates the parameters of the focus, if the focus is active in this group
     *
     * @param time   The time frame provider
     * @param camera The current camera
     */
    public void updateFocus(ITimeFrameProvider time, ICamera camera) {
        StarBean focus = (StarBean) pointData.get(focusIndex);
        Vector3d aux = this.fetchPosition(focus, camera.getPos(), aux3d1.get(), currDeltaYears);

        this.focusPosition.set(aux).add(camera.getPos());
        this.focusDistToCamera = aux.len();
        this.focusSize = getFocusSize();
        this.focusViewAngle = (float) ((getRadius() / this.focusDistToCamera) / camera.getFovFactor());
        this.focusViewAngleApparent = this.focusViewAngle * GlobalConf.scene.STAR_BRIGHTNESS;
    }

    /**
     * Overrides {@link ParticleGroup}'s implementation by actually integrating
     * the position using the proper motion and the given time.
     */
    public Vector3d getPredictedPosition(Vector3d aux, ITimeFrameProvider time, ICamera camera, boolean force) {
        if (time.getDt() == 0 && !force) {
            return getAbsolutePosition(aux);
        } else {
            double deltaYears = AstroUtils.getMsSince(time.getTime(), epoch_jd) * Nature.MS_TO_Y;
            return this.fetchPosition(pointData.get(focusIndex), null, aux, deltaYears);
        }
    }

    /**
     * Updates the additional information array, to use for sorting.
     *
     * @param camera
     */
    public void updateAdditional(ITimeFrameProvider time, ICamera camera) {
        Vector3d cpos = camera.getPos();
        double deltaYears = AstroUtils.getMsSince(time.getTime(), epoch_jd) * Nature.MS_TO_Y;
        int n = pointData.size;
        for (int i = 0; i < n; i++) {
            StarBean d = (StarBean) pointData.get(i);

            // Pm
            Vector3d dx = aux3d2.get().set(d.pmx(), d.pmy(), d.pmz()).scl(deltaYears);
            // Pos
            Vector3d x = aux3d1.get().set(d.x(), d.y(), d.z()).add(dx);

            additional[i] = -(((d.size() * Constants.STAR_SIZE_FACTOR) / cpos.dst(x.x, x.y, x.z)) / camera.getFovFactor()) * GlobalConf.scene.STAR_BRIGHTNESS;
        }
    }

    /**
     * Sorts the list of particles using the apparent magnitude (view angle)
     * criterion. This should be called only when the camera frustum is
     * significantly changed. By no means should this be called every cycle.
     * Sorting happens always in the working buffer, which is not the active
     * buffer. After sorting, buffers are swapped.
     */
    public void updateSorter(ITimeFrameProvider time, ICamera camera) {
        // Prepare metadata to sort
        updateAdditional(time, camera);

        // Sort background list of indices
        Arrays.sort(background, comp);
        // Swap indices lists
        swapBuffers();
        // Update last sort call
        super.updateSorter(time, camera);

    }

    private void swapBuffers() {
        Gdx.app.postRunnable(() -> {
            if (active == indices1) { //-V6013
                active = indices2;
                background = indices1;
            } else {
                active = indices1;
                background = indices2;
            }

        });
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        addToRender(this, RenderGroup.STAR_GROUP);
        addToRender(this, RenderGroup.BILLBOARD_STAR);
        addToRender(this, RenderGroup.MODEL_STAR);
        if (SceneGraphRenderer.instance.isOn(ComponentTypes.ComponentType.VelocityVectors)) {
            addToRender(this, RenderGroup.LINE);
            addToRender(this, RenderGroup.LINE);
        }
        if (renderText()) {
            addToRender(this, RenderGroup.FONT_LABEL);
        }
    }

    /**
     * Quad rendering
     */
    @Override
    public void render(ShaderProgram shader, float alpha, IntMesh mesh, ICamera camera) {
        double thpointTimesFovfactor = GlobalConf.scene.STAR_THRESHOLD_POINT * camera.getFovFactor() * .5e-1f;
        double thupOverFovfactor = Constants.THRESHOLD_UP / camera.getFovFactor();
        double thdownOverFovfactor = Constants.THRESHOLD_DOWN / camera.getFovFactor();
        double innerRad = 0.006 + GlobalConf.scene.STAR_POINT_SIZE * 0.008;
        float alph = alpha * this.opacity;

        /** GENERAL UNIFORMS **/
        shader.setUniformf("u_thpoint", (float) GlobalConf.scene.STAR_THRESHOLD_POINT * camera.getFovFactor());
        // Light glow always disabled with star groups
        shader.setUniformi("u_lightScattering", 0);
        shader.setUniformf("u_inner_rad", (float) innerRad);

        /** RENDER ACTUAL STARS **/
        boolean focusRendered = false;
        for (int i = 0; i < N_CLOSEUP_STARS; i++) {
            renderCloseupStar(active[i], camera, shader, mesh, thpointTimesFovfactor, thupOverFovfactor, thdownOverFovfactor, alph);
            focusRendered = focusRendered || active[i] == focusIndex;
        }
        if (focus != null && !focusRendered) {
            renderCloseupStar(focusIndex, camera, shader, mesh, thpointTimesFovfactor, thupOverFovfactor, thdownOverFovfactor, alph);
        }

    }

    Color c = new Color();

    private void renderCloseupStar(int idx, ICamera camera, ShaderProgram shader, IntMesh mesh, double thpointTimesFovfactor, double thupOverFovfactor, double thdownOverFovfactor, float alpha) {
        StarBean star = (StarBean) pointData.get(idx);
        double size = getSize(idx);
        double radius = size * Constants.STAR_SIZE_FACTOR;
        Vector3d lpos = fetchPosition(star, camera.getPos(), aux3d1.get(), currDeltaYears);
        double distToCamera = lpos.len();
        double viewAngle = (radius / distToCamera) / camera.getFovFactor();

        Color.abgr8888ToColor(c, getColor(idx));
        if (viewAngle >= thpointTimesFovfactor) {
            double fuzzySize = getFuzzyRenderSize(size, radius, distToCamera, viewAngle, thdownOverFovfactor, thupOverFovfactor);

            Vector3 pos = lpos.put(aux3f3.get());
            shader.setUniformf("u_pos", pos);
            shader.setUniformf("u_size", (float) fuzzySize);

            shader.setUniformf("u_color", c.r, c.g, c.b, alpha);
            shader.setUniformf("u_distance", (float) distToCamera);
            shader.setUniformf("u_apparent_angle", (float) (viewAngle * GlobalConf.scene.STAR_BRIGHTNESS));
            shader.setUniformf("u_radius", (float) radius);

            // Sprite.render
            mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);

        }
    }

    public double getFuzzyRenderSize(double size, double radius, double distToCamera, double viewAngle, double thDown, double thUp) {
        double computedSize = size;
        if (viewAngle > thDown) {
            double dist = distToCamera;
            if (viewAngle > thUp) {
                dist = radius / Constants.THRESHOLD_UP;
            }
            computedSize = (size * (dist / radius) * Constants.THRESHOLD_DOWN);
        }
        // Change the factor at the end here to control the stray light of stars
        computedSize *= GlobalConf.scene.STAR_BRIGHTNESS * 0.1;

        return computedSize;
    }

    /**
     * Model rendering
     */
    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc) {
        mc.touch();
        float opct = (float) MathUtilsd.lint(closestDist, modelDist / 50f, modelDist, 1f, 0f);
        if (alpha * opct > 0) {
            mc.setTransparency(alpha * opct);
            float[] col = closestCol;
            ((ColorAttribute) mc.env.get(ColorAttribute.AmbientLight)).color.set(col[0], col[1], col[2], 1f);
            ((FloatAttribute) mc.env.get(FloatAttribute.Shininess)).value = (float) t;
            // Local transform
            mc.instance.transform.idt().translate((float) closestPos.x, (float) closestPos.y, (float) closestPos.z).scl((float) (getRadius(active[0]) * 2d));
            mc.updateRelativisticEffects(GaiaSky.instance.getICamera());
            modelBatch.render(mc.instance, mc.env);
        }
    }

    private long getMaxProperMotionLines() {
        return GlobalConf.scene.N_PM_STARS > 0 ? GlobalConf.scene.N_PM_STARS : (N_CLOSEUP_STARS * 20);
    }

    private boolean rvLines = false;
    private float[] rgba = new float[4];

    /**
     * Proper motion rendering
     */
    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        alpha *= SceneGraphRenderer.instance.alphas[ComponentTypes.ComponentType.VelocityVectors.ordinal()];
        float thPointTimesFovFactor = (float) GlobalConf.scene.STAR_THRESHOLD_POINT * camera.getFovFactor();
        int n = (int) Math.min(getMaxProperMotionLines(), pointData.size);
        for (int i = n - 1; i >= 0; i--) {
            StarBean star = (StarBean) pointData.get(active[i]);
            if ((star.radvel() == 0 && !rvLines) || (star.radvel() != 0 && rvLines)) {
                float radius = (float) (getSize(active[i]) * Constants.STAR_SIZE_FACTOR);
                // Position
                Vector3d lpos = fetchPosition(star, camera.getPos(), aux3d1.get(), currDeltaYears);
                // Proper motion
                Vector3d pm = aux3d2.get().set(star.pmx(), star.pmy(), star.pmz()).scl(currDeltaYears);
                // Rest of attributes
                float distToCamera = (float) lpos.len();
                float viewAngle = (float) (((radius / distToCamera) / camera.getFovFactor()) * GlobalConf.scene.STAR_BRIGHTNESS);
                if (viewAngle >= thPointTimesFovFactor / GlobalConf.scene.PM_NUM_FACTOR && (star.pmx() != 0 || star.pmy() != 0 || star.pmz() != 0)) {

                    Vector3d p1 = aux3d1.get().set(star.x() + pm.x, star.y() + pm.y, star.z() + pm.z).sub(camera.getPos());
                    Vector3d ppm = aux3d2.get().set(star.pmx(), star.pmy(), star.pmz()).scl(GlobalConf.scene.PM_LEN_FACTOR);
                    double p1p2len = ppm.len();
                    Vector3d p2 = aux3d3.get().set(ppm).add(p1);

                    // Max speed in km/s, to normalize
                    double maxSpeedKms = 100;
                    float r, g, b;
                    switch (GlobalConf.scene.PM_COLOR_MODE) {
                    case 0:
                    default:
                        // DIRECTION
                        // Normalize, each component is in [-1:1], map to [0:1] and to a color channel
                        ppm.nor();
                        r = (float) (ppm.x + 1d) / 2f;
                        g = (float) (ppm.y + 1d) / 2f;
                        b = (float) (ppm.z + 1d) / 2f;
                        break;
                    case 1:
                        // LENGTH
                        ppm.set(star.pmx(), star.pmy(), star.pmz());
                        // Units/year to Km/s
                        ppm.scl(Constants.U_TO_KM / Nature.Y_TO_S);
                        double len = MathUtilsd.clamp(ppm.len(), 0d, maxSpeedKms) / maxSpeedKms;
                        ColourUtils.long_rainbow((float) (1 - len), rgba);
                        r = rgba[0];
                        g = rgba[1];
                        b = rgba[2];
                        break;
                    case 2:
                        // HAS RADIAL VELOCITY - blue: stars with RV, red: stars without RV
                        if (star.radvel() != 0) {
                            r = GlobalResources.gBlue[0] + 0.2f;
                            g = GlobalResources.gBlue[1] + 0.4f;
                            b = GlobalResources.gBlue[2] + 0.4f;
                        } else {
                            r = GlobalResources.gRed[0] + 0.4f;
                            g = GlobalResources.gRed[1] + 0.2f;
                            b = GlobalResources.gRed[2] + 0.2f;
                        }
                        break;
                    case 3:
                        // REDSHIFT from Sun - blue: -100 Km/s, red: 100 Km/s
                        double rav = star.radvel();
                        if (rav != 0) {
                            double max = maxSpeedKms;
                            // rv in [0:1]
                            double rv = ((MathUtilsd.clamp(rav, -max, max) / max) + 1) / 2;
                            ColourUtils.blue_white_red((float) rv, rgba);
                            r = rgba[0];
                            g = rgba[1];
                            b = rgba[2];
                        } else {
                            r = g = b = 1;
                        }
                        break;
                    case 4:
                        // REDSHIFT from Camera - blue: -100 Km/s, red: 100 Km/s
                        if (ppm.len2() != 0) {
                            double max = maxSpeedKms;
                            ppm.set(star.pmx(), star.pmy(), star.pmz());
                            // Units/year to Km/s
                            ppm.scl(Constants.U_TO_KM / Nature.Y_TO_S);
                            Vector3d camstar = aux3d4.get().set(p1);
                            double pr = ppm.dot(camstar.nor());
                            double projection = ((MathUtilsd.clamp(pr, -max, max) / max) + 1) / 2;
                            ColourUtils.blue_white_red((float) projection, rgba);
                            r = rgba[0];
                            g = rgba[1];
                            b = rgba[2];
                        } else {
                            r = g = b = 1;
                        }
                        break;
                    case 5:
                        // SINGLE COLOR
                        r = GlobalResources.gBlue[0] + 0.2f;
                        g = GlobalResources.gBlue[1] + 0.4f;
                        b = GlobalResources.gBlue[2] + 0.4f;
                        break;
                    }

                    // Clamp
                    r = MathUtilsd.clamp(r, 0, 1);
                    g = MathUtilsd.clamp(g, 0, 1);
                    b = MathUtilsd.clamp(b, 0, 1);

                    renderer.addLine(this, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, r, g, b, alpha * this.opacity);
                    if(GlobalConf.scene.PM_ARROWHEADS) {
                        // Add Arrow cap
                        Vector3d p3 = aux3d2.get().set(ppm).nor().scl(p1p2len * .86).add(p1);
                        p3.rotate(p2, 30);
                        renderer.addLine(this, p3.x, p3.y, p3.z, p2.x, p2.y, p2.z, r, g, b, alpha * this.opacity);
                        p3.rotate(p2, -60);
                        renderer.addLine(this, p3.x, p3.y, p3.z, p2.x, p2.y, p2.z, r, g, b, alpha * this.opacity);
                    }

                }
            }
        }
        rvLines = !rvLines;

    }

    @Override
    public float getLineWidth() {
        return 1.0f;
    }

    @Override
    public int getGlType() {
        return GL20.GL_LINES;
    }

    /**
     * Label rendering
     */
    @Override
    public void render(SpriteBatch batch, ShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        float thOverFactor = (float) (GlobalConf.scene.STAR_THRESHOLD_POINT / GlobalConf.scene.LABEL_NUMBER_FACTOR / camera.getFovFactor());

        if (camera.getCurrent() instanceof FovCamera) {
            int n = Math.min(pointData.size, N_CLOSEUP_STARS * 5);
            for (int i = 0; i < n; i++) {
                StarBean star = (StarBean) pointData.get(active[i]);
                Vector3d lpos = fetchPosition(star, camera.getPos(), aux3d1.get(), currDeltaYears);
                float distToCamera = (float) lpos.len();
                float radius = (float) getRadius(active[i]);
                float viewAngle = (float) (((radius / distToCamera) / camera.getFovFactor()) * GlobalConf.scene.STAR_BRIGHTNESS);

                if (camera.isVisible(GaiaSky.instance.time, viewAngle, lpos, distToCamera)) {
                    render2DLabel(batch, shader, rc, sys.font2d, camera, star.name, lpos);
                }
            }
        } else {
            for (int i = 0; i < N_CLOSEUP_STARS; i++) {
                StarBean star = (StarBean) pointData.get(active[i]);
                Vector3d lpos = fetchPosition(star, camera.getPos(), aux3d1.get(), currDeltaYears);
                float distToCamera = (float) lpos.len();
                float radius = (float) getRadius(active[i]);
                float viewAngle = (float) (((radius / distToCamera) / camera.getFovFactor()) * GlobalConf.scene.STAR_BRIGHTNESS);

                if (viewAngle >= thOverFactor && camera.isVisible(GaiaSky.instance.time, viewAngle, lpos, distToCamera)) {
                    textPosition(camera, lpos, distToCamera, radius);

                    shader.setUniformf("u_viewAngle", viewAngle);
                    shader.setUniformf("u_viewAnglePow", 1f);
                    shader.setUniformf("u_thOverFactor", thOverFactor);
                    shader.setUniformf("u_thOverFactorScl", camera.getFovFactor());
                    float textSize = (float) FastMath.tanh(viewAngle) * distToCamera * 1e5f;
                    float alpha = Math.min((float) FastMath.atan(textSize / distToCamera), 1.e-3f);
                    textSize = (float) FastMath.tan(alpha) * distToCamera * 0.5f;
                    render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, star.name, lpos, textScale() * camera.getFovFactor(), textSize * camera.getFovFactor());

                }
            }
        }
    }

    public void textPosition(ICamera cam, Vector3d out, float len, float rad) {
        out.clamp(0, len - rad);

        Vector3d aux = aux3d2.get();
        aux.set(cam.getUp());

        aux.crs(out).nor();

        float dist = -0.02f * cam.getFovFactor() * (float) out.len();

        aux.add(cam.getUp()).nor().scl(dist);

        out.add(aux);

        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public float textScale() {
        return 0.1f;
    }

    public double getFocusSize() {
        return focus.data[StarBean.I_SIZE];
    }

    // Radius in stars is different!
    public double getRadius() {
        return getSize() * Constants.STAR_SIZE_FACTOR;
    }

    // Radius in stars is different!
    public double getRadius(int i) {
        return getSize(i) * Constants.STAR_SIZE_FACTOR;
    }

    public float getAppmag() {
        return (float) focus.data[StarBean.I_APPMAG];
    }

    public float getAbsmag() {
        return (float) focus.data[StarBean.I_ABSMAG];
    }

    public String getName() {
        if (focus != null)
            return ((StarBean) focus).name;
        else
            return null;
    }

    public long getId() {
        if (focus != null)
            return ((StarBean) focus).id;
        else
            return -1;
    }

    @Override
    public double getMuAlpha() {
        if (focus != null)
            return focus.data[StarBean.I_MUALPHA];
        else
            return 0;
    }

    @Override
    public double getMuDelta() {
        if (focus != null)
            return focus.data[StarBean.I_MUDELTA];
        else
            return 0;
    }

    @Override
    public double getRadialVelocity() {
        if (focus != null)
            return focus.data[StarBean.I_RADVEL];
        else
            return 0;
    }

    /**
     * Returns the size of the particle at index i
     *
     * @param i The index
     * @return The size
     */
    public double getSize(int i) {
        return ((StarBean) pointData.get(i)).size();
    }

    /**
     * Uses whatever precomputed value is in index 8 to compare the values
     *
     * @author tsagrista
     */
    private class StarGroupComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer i1, Integer i2) {
            return Double.compare(additional[i1], additional[i2]);
        }
    }

    public class UpdaterTask implements Runnable {

        private StarGroup sg;

        public UpdaterTask(StarGroup sg) {
            this.sg = sg;
        }

        @Override
        public void run() {
            sg.updateSorter(GaiaSky.instance.time, GaiaSky.instance.getICamera());
            updating = false;
        }

    }

    @Override
    public void notify(Events event, Object... data) {
        // Super handles FOCUS_CHANGED event
        super.notify(event, data);
        switch (event) {
        case CAMERA_MOTION_UPDATED:
            if (updaterTask != null) {
                final Vector3d currentCameraPos = (Vector3d) data[0];
                long t = TimeUtils.millis() - lastSortTime;
                if (!updating && !pool.isShutdown() && !workQueue.contains(updaterTask) && this.opacity > 0 && (t > MIN_UPDATE_TIME_MS * 2 || (lastSortCameraPos.dst(currentCameraPos) > CAM_DX_TH && t > MIN_UPDATE_TIME_MS))) {
                    updating = true;
                    pool.execute(updaterTask);
                }
            }
            break;
        case GRAPHICS_QUALITY_UPDATED:
            this.N_CLOSEUP_STARS = getNCloseupStars();
            break;
        default:
            break;
        }

    }

    @Override
    public int getCatalogSource() {
        return 1;
    }

    @Override
    public int getHip() {
        if (focus != null && focus.data[StarBean.I_HIP] > 0)
            return (int) focus.data[StarBean.I_HIP];
        return -1;
    }

    @Override
    public long getCandidateId() {
        return ((StarBean) pointData.get(candidateFocusIndex)).id;
    }

    @Override
    public String getCandidateName() {
        return ((StarBean) pointData.get(candidateFocusIndex)).name;
    }

    @Override
    public double getCandidateViewAngleApparent() {
        if (candidateFocusIndex >= 0) {
            StarBean candidate = (StarBean) pointData.get(candidateFocusIndex);
            Vector3d aux = candidate.pos(aux3d1.get());
            ICamera camera = GaiaSky.instance.getICamera();
            double va = (float) ((candidate.radius() / aux.sub(camera.getPos()).len()) / camera.getFovFactor());
            return va * GlobalConf.scene.STAR_BRIGHTNESS;
        } else {
            return -1;
        }
    }

    @Override
    public double getClosestDist() {
        return this.closestDist;
    }

    @Override
    public String getClosestName() {
        return this.closestName;
    }

    @Override
    public double getClosestSize() {
        return this.closestSize;
    }

    @Override
    public Vector3d getClosestPos(Vector3d out) {
        return out.set(this.closestPos);
    }

    @Override
    public float[] getClosestCol() {
        return this.closestCol;
    }

    @Override
    public boolean hasAtmosphere() {
        return false;
    }

    @Override
    public IFocus getFocus(String name) {
        candidateFocusIndex = index.get(name, -1);
        return this;
    }

    public Vector3d getAbsolutePosition(String name, Vector3d aux) {
        if (index.containsKey(name)) {
            int idx = index.get(name, 0);
            StarBean sb = (StarBean) pointData.get(idx);
            fetchPosition(sb, null, aux, currDeltaYears);
            return aux;
        } else {
            return null;
        }
    }

    @Override
    protected Vector3d fetchPosition(ParticleBean pb, Vector3d campos, Vector3d dest, double deltaYears) {
        StarBean sb = (StarBean) pb;
        Vector3d pm = aux.set(sb.pmx(), sb.pmy(), sb.pmz()).scl(deltaYears);
        if (campos != null && !campos.hasNaN())
            return dest.set(sb.x(), sb.y(), sb.z()).sub(campos).add(pm);
        else
            return dest.set(sb.x(), sb.y(), sb.z()).add(pm);
    }

    @Override
    protected double getDeltaYears() {
        return currDeltaYears;
    }

    /**
     * Sets the epoch to use for the stars in this group
     *
     * @param epochJd The epoch in julian days (days since January 1, 4713 BCE)
     */
    public void setEpoch(Double epochJd) {
        this.epoch_jd = epochJd;
    }

    /**
     * Returns the epoch in Julian Days used for the stars in this group
     *
     * @return The epoch in julian days
     */
    public Double getEpoch() {
        return this.epoch_jd;
    }

    @Override

    protected void addToIndex(ObjectMap<String, SceneGraphNode> map) {
        if (index != null) {
            ObjectIntMap.Keys<String> keys = index.keys();
            for (String key : keys) {
                map.put(key, this);
            }
        }
    }

    @Override
    protected void removeFromIndex(ObjectMap<String, SceneGraphNode> map) {
        if (index != null) {
            ObjectIntMap.Keys<String> keys = index.keys();
            for (String key : keys) {
                map.remove(key);
            }
        }
    }

    @Override
    public void dispose() {
        this.disposed = true;
        sg.remove(this, true);
        // Unsubscribe from all events
        EventManager.instance.removeAllSubscriptions(this);
        // Shut down pool
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
            try {
                // Wait for task to end before proceeding
                pool.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                Logger.getLogger(this.getClass().getSimpleName()).error(e);
            }
        }
        // Dispose of GPU datOLO
        EventManager.instance.post(Events.DISPOSE_STAR_GROUP_GPU_MESH, this.offset);
        // Data to be gc'd
        this.pointData = null;
        // Remove focus if needed
        CameraManager cam = GaiaSky.instance.getCameraManager();
        if (cam != null && cam.getFocus() != null && cam.getFocus() == this) {
            this.setFocusIndex(-1);
            EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.Free_Camera);
        }
    }

    public float getColor(int index) {
        return highlighted ? hlColorFloat[hlci] : (float) ((Array<StarBean>) pointData).get(index).col();
    }

    /**
     * Creates a default star group with some sane parameters, given the name and the data
     *
     * @param name The name of the star group
     * @param data The data of the star group
     * @return A new star group with sane parameters
     */
    public static StarGroup getDefaultStarGroup(String name, Array<StarBean> data) {
        StarGroup sg = new StarGroup();
        sg.setName(name);
        sg.setParent("Universe");
        sg.setFadeout(new double[] { 21e2, 1e5 });
        sg.setLabelcolor(new double[] { 1.0, 1.0, 1.0, 1.0 });
        sg.setColor(new double[] { 1.0, 1.0, 1.0, 0.25 });
        sg.setSize(6.0);
        sg.setLabelposition(new double[] { 0.0, -5.0e7, -4e8 });
        sg.setCt("Stars");
        sg.setData(data);
        sg.setVisible(true);
        sg.doneLoading(null);
        return sg;
    }
}
