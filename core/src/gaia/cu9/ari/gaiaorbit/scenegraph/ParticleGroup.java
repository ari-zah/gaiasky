/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.data.group.IParticleGroupDataProvider;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.I3DTextRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.system.FontRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.NaturalCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.RotationComponent;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Pair;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Intersectord;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Quaterniond;
import gaia.cu9.ari.gaiaorbit.util.math.Vector2d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

import java.io.Serializable;

/**
 * This class represents a group of non-focusable particles, all with the same
 * luminosity. The contents of this group will be sent once to GPU memory and
 * stay there, so all particles get rendered directly in the GPU from the GPU
 * with no CPU intervention. This allows for much faster rendering. Use this for
 * large groups of particles.
 *
 * @author tsagrista
 */
public class ParticleGroup extends FadeNode implements I3DTextRenderable, IFocus, IObserver {
    public static class ParticleBean implements Serializable {
        private static final long serialVersionUID = 1L;

        /* INDICES */

        /* doubles */
        public static final int I_X = 0;
        public static final int I_Y = 1;
        public static final int I_Z = 2;

        public double[] data;

        public ParticleBean(double[] data) {
            this.data = data;
        }

        public Vector3d pos(Vector3d aux) {
            return aux.set(x(), y(), z());
        }

        public double x() {
            return data[I_X];
        }

        public double y() {
            return data[I_Y];
        }

        public double z() {
            return data[I_Z];
        }
    }

    /**
     * List that contains the point data. It contains only [x y z]
     */
    protected Array<? extends ParticleBean> pointData;

    /**
     * Fully qualified name of data provider class
     */
    protected String provider;

    /**
     * Path of data file
     */
    protected String datafile;

    /**
     * Profile decay of the particles in the shader
     */
    public float profileDecay = 4.0f;

    /**
     * Are the data of this group in the GPU memory?
     */
    public boolean inGpu;

    // Offset and count for this group
    public int offset, count;

    /**
     * This flag indicates whether the mean position is already given by the
     * JSON injector
     */
    protected boolean fixedMeanPosition = false;

    /**
     * Factor to apply to the data points, usually to normalise distances
     */
    protected Double factor = null;

    /**
     * Index of the particle acting as focus. Negative if we have no focus here.
     */
    int focusIndex;

    /**
     * Candidate to focus. Will be used in {@link #makeFocus()}
     */
    int candidateFocusIndex;

    /**
     * Position of the current focus
     */
    Vector3d focusPosition;

    /**
     * Position in equatorial coordinates of the current focus
     */
    Vector2d focusPositionSph;

    /**
     * Focus attributes
     */
    double focusDistToCamera, focusViewAngle, focusViewAngleApparent, focusSize;

    /**
     * Stores the time when the last sort operation finished, in ms
     */
    protected long lastSortTime;

    /**
     * Geometric centre at epoch, for render sorting
     */
    private static Vector3d geomCentre;

    /**
     * Reference to the current focus
     */
    ParticleBean focus;

    // Has been disposed
    public boolean disposed = false;

    public ParticleGroup() {
        super();
        inGpu = false;
        focusIndex = -1;
        focusPosition = new Vector3d();
        focusPositionSph = new Vector2d();
        EventManager.instance.subscribe(this, Events.FOCUS_CHANGED);
    }

    public void initialize() {
        /** Load data **/
        try {
            Class<?> clazz = Class.forName(provider);
            IParticleGroupDataProvider provider = (IParticleGroupDataProvider) clazz.newInstance();

            if (factor == null)
                factor = 1d;

            lastSortTime = -1;

            pointData = provider.loadData(datafile, factor);

            if (!fixedMeanPosition) {
                // Mean position
                for (ParticleBean point : pointData) {
                    pos.add(point.data[0], point.data[1], point.data[2]);
                }
                pos.scl(1d / pointData.size);
            }

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
            pointData = null;
        }
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
    }

    /**
     * Returns the data list
     *
     * @return The data list
     */
    public Array<? extends ParticleBean> data() {
        return pointData;
    }

    /**
     * Computes the geometric centre of this data cloud
     */
    public Vector3d computeGeomCentre(){
        return computeGeomCentre(false);
    }

    /**
     * Computes the geometric centre of this data cloud
     * @param forceRecompute Recomputes the geometric centre even if it has been already computed
     */
    public Vector3d computeGeomCentre(boolean forceRecompute){
        if(pointData != null && (forceRecompute || geomCentre == null)){
            geomCentre = new Vector3d(0,0,0);
            int n = pointData.size;
            for(int i =0; i < n; i++){
                ParticleBean pb = pointData.get(i);
                geomCentre.add(pb.x(), pb.y(), pb.z());
            }
            geomCentre.scl(1d / (double) n);
        }
        return geomCentre;
    }

    /**
     * Number of objects of this group
     *
     * @return The number of objects
     */
    public int size() {
        return pointData.size;
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
        if (pointData != null && this.isVisible()) {
            this.opacity = 1;
            super.update(time, parentTransform, camera, opacity);

            if (focusIndex >= 0) {
                updateFocus(time, camera);
            }
        }
    }

    @Override
    public void update(ITimeFrameProvider time, Vector3d parentTransform, ICamera camera) {
        update(time, parentTransform, camera, 1f);
    }

    /**
     * Updates the parameters of the focus, if the focus is active in this group
     *
     * @param time   The time frame provider
     * @param camera The current camera
     */
    public void updateFocus(ITimeFrameProvider time, ICamera camera) {

        Vector3d aux = aux3d1.get().set(this.focusPosition);
        this.focusDistToCamera = aux.sub(camera.getPos()).len();
        this.focusSize = getFocusSize();
        this.focusViewAngle = (float) ((getRadius() / this.focusDistToCamera) / camera.getFovFactor());
        this.focusViewAngleApparent = this.focusViewAngle * GlobalConf.scene.STAR_BRIGHTNESS;
    }

    public void updateSorter(ITimeFrameProvider time, ICamera camera) {
        // Simple particle group does not sort
        lastSortTime = TimeUtils.millis();
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        addToRender(this, RenderGroup.PARTICLE_GROUP);

        if (renderText()) {
            addToRender(this, RenderGroup.FONT_LABEL);
        }
    }

    /**
     * Label rendering.
     */
    @Override
    public void render(SpriteBatch batch, ShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        Vector3d pos = aux3d1.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thOverFactor", 1f);
        shader.setUniformf("u_thOverFactorScl", 1f);
        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor());
    }

    /**
     * LABEL
     */

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setDatafile(String datafile) {
        this.datafile = datafile;
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    @Override
    public boolean renderText() {
        return GaiaSky.instance.isOn(ComponentType.Labels);
    }

    @Override
    public float[] textColour() {
        return labelColour;
    }

    @Override
    public float textSize() {
        return (float) distToCamera * 1e-3f;
    }

    @Override
    public float textScale() {
        return 0.1f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        out.set(labelPosition).add(cam.getInversePos());
    }

    @Override
    public String text() {
        return name;
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
    }

    @Override
    public boolean isLabel() {
        return true;
    }

    public void setFactor(Double factor) {
        this.factor = factor;
    }

    public void setProfiledecay(Double profiledecay) {
        this.profileDecay = profiledecay.floatValue();
    }

    /**
     * FOCUS
     */

    /**
     * Default size if not in data, 1e5 km
     *
     * @return The size
     */
    public double getFocusSize() {
        return 1e5 * Constants.KM_TO_U;
    }

    /**
     * Returns the id
     */
    public long getId() {
        return 123l;
    }

    /**
     * Returns name of focus
     */
    public String getName() {
        return name;
    }

    @Override
    public int getStarCount() {
        return pointData.size;
    }

    public boolean isActive() {
        return focusIndex >= 0;
    }

    /**
     * Returns position of focus
     */
    public void setPosition(double[] pos) {
        super.setPosition(pos);
        this.fixedMeanPosition = true;
    }

    /**
     * Adds all the children that are focusable objects to the list.
     *
     * @param list
     */
    public void addFocusableObjects(Array<IFocus> list) {
        list.add(this);
        super.addFocusableObjects(list);
    }

    // Myself!
    public AbstractPositionEntity getComputedAncestor() {
        return this;
    }

    // Myself?
    public SceneGraphNode getFirstStarAncestor() {
        return this;
    }

    // The focus position
    public Vector3d getAbsolutePosition(Vector3d aux) {
        return aux.set(focusPosition);
    }

    public Vector3d getAbsolutePosition(String name, Vector3d aux) {
        return getAbsolutePosition(aux);
    }

    // Same position
    public Vector3d getPredictedPosition(Vector3d aux, ITimeFrameProvider time, ICamera camera, boolean force) {
        return getAbsolutePosition(aux);
    }

    // Spherical position for focus info, will be computed
    public Vector2d getPosSph() {
        return focusPositionSph;
    }

    // Focus dist to camera
    public double getDistToCamera() {
        return focusDistToCamera;
    }

    // Focus view angle
    public double getViewAngle() {
        return focusViewAngle;
    }

    // Focus apparent view angle
    public double getViewAngleApparent() {
        return focusViewAngleApparent;
    }

    // Focus size
    public double getSize() {
        return focusSize;
    }

    public float getAppmag() {
        return 0;
    }

    public float getAbsmag() {
        return 0;
    }

    /**
     * Returns the size of the particle at index i
     *
     * @param i The index
     * @return The size
     */
    public double getSize(int i) {
        return getFocusSize();
    }

    public double getRadius(int i) {
        // All particles have the same radius
        return getRadius();
    }

    // Half the size
    public double getRadius() {
        return getSize() / 2d;
    }

    @Override
    public boolean withinMagLimit() {
        return true;
    }

    @Override
    public RotationComponent getRotationComponent() {
        return null;
    }

    @Override
    public Quaterniond getOrientationQuaternion() {
        return null;
    }

    public float[] getColor() {
        return highlighted ? hlColor[hlci] : cc;

    }

    public float highlightedSizeFactor() {
        return highlighted ? 2f : 1f;
    }

    public void addHit(int screenX, int screenY, int w, int h, int pxdist, NaturalCamera camera, Array<IFocus> hits) {
        int n = pointData.size;
        if (GaiaSky.instance.isOn(ct) && this.opacity > 0) {
            Array<Pair<Integer, Double>> temporalHits = new Array<>();
            for (int i = 0; i < n; i++) {
                ParticleBean pb = pointData.get(i);
                Vector3 pos = aux3f1.get();
                Vector3d posd = fetchPosition(pb, camera.getPos(), aux3d1.get(), getDeltaYears());
                pos.set(posd.valuesf());

                if (camera.direction.dot(posd) > 0) {
                    // The star is in front of us
                    // Diminish the size of the star
                    // when we are close by
                    double dist = posd.len();
                    double angle = getRadius(i) / dist / camera.getFovFactor();

                    PerspectiveCamera pcamera;
                    if (GlobalConf.program.STEREOSCOPIC_MODE) {
                        if (screenX < Gdx.graphics.getWidth() / 2f) {
                            pcamera = camera.getCameraStereoLeft();
                            pcamera.update();
                        } else {
                            pcamera = camera.getCameraStereoRight();
                            pcamera.update();
                        }
                    } else {
                        pcamera = camera.camera;
                    }

                    angle = (float) Math.toDegrees(angle * camera.fovFactor) * (40f / pcamera.fieldOfView);
                    double pixelSize = Math.max(pxdist, ((angle * pcamera.viewportHeight) / pcamera.fieldOfView) / 2);
                    pcamera.project(pos);
                    pos.y = pcamera.viewportHeight - pos.y;
                    if (GlobalConf.program.STEREOSCOPIC_MODE) {
                        pos.x /= 2;
                    }

                    // Check click distance
                    if (pos.dst(screenX % pcamera.viewportWidth, screenY, pos.z) <= pixelSize) {
                        //Hit
                        temporalHits.add(new Pair<>(i, angle));
                    }
                }
            }

            Pair<Integer, Double> best = null;
            for (Pair<Integer, Double> hit : temporalHits) {
                if (best == null)
                    best = hit;
                else if (hit.getSecond() > best.getSecond()) {
                    best = hit;
                }
            }
            if (best != null) {
                // We found the best hit
                candidateFocusIndex = best.getFirst();
                updateFocusDataPos();
                hits.add(this);
                return;
            }

        }
        candidateFocusIndex = -1;
        updateFocusDataPos();
    }

    public void addHit(Vector3d p0, Vector3d p1, NaturalCamera camera, Array<IFocus> hits) {
        int n = pointData.size;
        if (GaiaSky.instance.isOn(ct) && this.opacity > 0) {
            Vector3d beamDir = new Vector3d();
            Array<Pair<Integer, Double>> temporalHits = new Array<Pair<Integer, Double>>();
            for (int i = 0; i < n; i++) {
                ParticleBean pb = pointData.get(i);
                Vector3d posd = fetchPosition(pb, camera.getPos(), aux3d1.get(), getDeltaYears());
                beamDir.set(p1).sub(p0);
                if (camera.direction.dot(posd) > 0) {
                    // The star is in front of us
                    // Diminish the size of the star
                    // when we are close by
                    double dist = posd.len();
                    double angle = getRadius(i) / dist / camera.getFovFactor();
                    double distToLine = Intersectord.distanceLinePoint(p0, p1, posd);
                    double value = distToLine / dist;

                    if (value < 0.01) {
                        temporalHits.add(new Pair<Integer, Double>(i, angle));
                    }
                }
            }

            Pair<Integer, Double> best = null;
            for (Pair<Integer, Double> hit : temporalHits) {
                if (best == null)
                    best = hit;
                else if (hit.getSecond() > best.getSecond()) {
                    best = hit;
                }
            }
            if (best != null) {
                // We found the best hit
                candidateFocusIndex = best.getFirst();
                updateFocusDataPos();
                hits.add(this);
                return;
            }

        }
        candidateFocusIndex = -1;
        updateFocusDataPos();
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case FOCUS_CHANGED:
            if (data[0] instanceof String) {
                focusIndex = data[0].equals(this.getName()) ? focusIndex : -1;
            } else {
                focusIndex = data[0] == this ? focusIndex : -1;
            }
            updateFocusDataPos();
            break;
        default:
            break;
        }

    }

    private void updateFocusDataPos() {
        if (focusIndex < 0) {
            focus = null;
        } else {
            focus = pointData.get(focusIndex);
            focusPosition.set(focus.data[0], focus.data[1], focus.data[2]);
            Vector3d possph = Coordinates.cartesianToSpherical(focusPosition, aux3d1.get());
            focusPositionSph.set((float) (MathUtilsd.radDeg * possph.x), (float) (MathUtilsd.radDeg * possph.y));
        }
    }

    public void setFocusIndex(int index) {
        if (index >= 0 && index < pointData.size) {
            candidateFocusIndex = index;
            makeFocus();
        }
    }

    @Override
    public void makeFocus() {
        focusIndex = candidateFocusIndex;
        updateFocusDataPos();

    }

    public int getCandidateIndex() {
        return candidateFocusIndex;
    }

    @Override
    public long getCandidateId() {
        return getId();
    }

    @Override
    public String getCandidateName() {
        return getName();
    }

    @Override
    public double getCandidateViewAngleApparent() {
        return getViewAngleApparent();
    }

    @Override
    public IFocus getFocus(String name) {
        return this;
    }

    @Override
    public double getAlpha() {
        return focusPositionSph.x;
    }

    @Override
    public double getDelta() {
        return focusPositionSph.y;
    }

    @Override
    protected float getBaseOpacity() {
        return this.opacity;
    }

    /**
     * Fetches the real position of the particle. It will apply the necessary
     * integrations (i.e. proper motion).
     *
     * @param pb         The particle bean
     * @param campos     The position of the camera. If null, the camera position is
     *                   not subtracted so that the coordinates are given in the global
     *                   reference system instead of the camera reference system.
     * @param dest       The destination fector
     * @param deltaYears The delta years
     * @return The vector for chaining
     */
    protected Vector3d fetchPosition(ParticleBean pb, Vector3d campos, Vector3d dest, double deltaYears) {
        if (campos != null)
            return dest.set(pb.data[0], pb.data[1], pb.data[2]).sub(campos);
        else
            return dest.set(pb.data[0], pb.data[1], pb.data[2]);
    }

    /**
     * Returns the delta years to integrate the proper motion.
     *
     * @return
     */
    protected double getDeltaYears() {
        return 0;
    }

    @Override
    public boolean isCoordinatesTimeOverflow() {
        return false;
    }

    public boolean mustAddToIndex() {
        return false;
    }

    @Override
    public void dispose() {
        this.disposed = true;
        sg.remove(this, true);
        // Unsubscribe from all events
        EventManager.instance.removeAllSubscriptions(this);
        // Dispose of GPU data
        EventManager.instance.post(Events.DISPOSE_PARTICLE_GROUP_GPU_MESH, this.offset);
        // Data to be gc'd
        this.pointData = null;
        // Remove focus if needed
        CameraManager cam = GaiaSky.instance.getCameraManager();
        if (cam != null && cam.getFocus() != null && cam.getFocus() == this) {
            this.setFocusIndex(-1);
            EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.Free_Camera);
        }
    }

    @Override
    public float getTextOpacity() {
        return getOpacity();
    }

    @Override
    public void highlight(boolean hl) {
        this.inGpu = this.highlighted == hl;
        super.highlight(hl);
    }

    @Override
    public void highlight(boolean hl, int index){
        this.inGpu = this.highlighted == hl;
        super.highlight(hl, index);
    }

}
