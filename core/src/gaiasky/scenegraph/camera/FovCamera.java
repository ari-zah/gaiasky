/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.Gaia;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.util.GlobalResources;
import gaiasky.util.gaia.GaiaAttitudeServer;
import gaiasky.util.gaia.Satellite;
import gaiasky.util.math.Frustumd;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Quaterniond;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The field of view cameras
 */
public class FovCamera extends AbstractCamera implements IObserver {
    private static final float FOV_CORR = 0.2f;
    private static final float FOV = (float) Satellite.FOV_AC + FOV_CORR;
    private static final float BAM_2 = (float) Satellite.BASICANGLE_DEGREE / 2f;
    private static final double GAIA_ASPECT_RATIO = (Satellite.FOV_AL + FOV_CORR) / FOV;

    /**
     * time that has to pass with the current scan rate so that we scan to the
     * edge of the current field of view.
     **/
    public long MAX_OVERLAP_TIME = 0l;
    public float MAX_OVERLAP_ANGLE = 0;

    private PerspectiveCamera camera2;
    private Frustumd frustum2;

    public Gaia gaia;

    Vector3d dirMiddle, up;
    public Vector3d[] directions;
    public List<Vector3d[]> interpolatedDirections;
    private Matrix4d trf;

    public long currentTime, lastTime;

    // Direction index for the render stage
    public int dirindex;

    private final Vector3d dir1;
    private final Vector3d dir2;
    private final Matrix4d matrix;

    private Stage[] fpstages;
    private Drawable fp, fp_fov1, fp_fov2;

    public FovCamera(final AssetManager assetManager, final CameraManager parent, final SpriteBatch spriteBatch) {
        super(parent);
        initialize(spriteBatch);
        directions = new Vector3d[] { new Vector3d(), new Vector3d() };
        interpolatedDirections = new ArrayList<>();
        dirMiddle = new Vector3d();
        up = new Vector3d();

        currentTime = 0L;
        lastTime = 0L;
        dir1 = new Vector3d();
        dir2 = new Vector3d();
        matrix = new Matrix4d();
    }

    public void initialize(final SpriteBatch spriteBatch) {
        camera = new PerspectiveCamera(FOV, (float) (Gdx.graphics.getHeight() * GAIA_ASPECT_RATIO), Gdx.graphics.getHeight());
        camera.near = (float) CAM_NEAR;
        camera.far = (float) CAM_FAR;

        camera2 = new PerspectiveCamera(FOV, (float) (Gdx.graphics.getHeight() * GAIA_ASPECT_RATIO), Gdx.graphics.getHeight());
        camera2.near = (float) CAM_NEAR;
        camera2.far = (float) CAM_FAR;

        frustum2 = new Frustumd();

        fovFactor = FOV / 5f;

        /* Prepare stage with FP image */
        fp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("img/gaia-focalplane.png"))));
        fp_fov1 = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("img/gaia-focalplane-fov1.png"))));
        fp_fov2 = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("img/gaia-focalplane-fov2.png"))));

        fpstages = new Stage[3];

        Stage fov12 = new Stage(new ScalingViewport(Scaling.stretch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new OrthographicCamera()), spriteBatch);
        Image i = new Image(fp);
        i.setFillParent(true);
        i.setAlign(Align.center);
        i.setColor(0.3f, 0.8f, 0.3f, .9f);
        fov12.addActor(i);

        Stage fov1 = new Stage(new ScalingViewport(Scaling.stretch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new OrthographicCamera()), spriteBatch);
        i = new Image(fp_fov1);
        i.setFillParent(true);
        i.setAlign(Align.center);
        i.setColor(0.3f, 0.8f, 0.3f, .9f);
        fov1.addActor(i);

        Stage fov2 = new Stage(new ScalingViewport(Scaling.stretch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new OrthographicCamera()), spriteBatch);
        i = new Image(fp_fov2);
        i.setFillParent(true);
        i.setAlign(Align.center);
        i.setColor(0.3f, 0.8f, 0.3f, .9f);
        fov2.addActor(i);

        fpstages[0] = fov1;
        fpstages[1] = fov2;
        fpstages[2] = fov12;

        EventManager.instance.subscribe(this, Events.GAIA_LOADED);
    }

    public void update(double dt, ITimeFrameProvider time) {
        distance = pos.lend();

        up.set(0, 1, 0);

        /** POSITION **/
        SceneGraphNode fccopy = gaia.getLineCopy();
        fccopy.getRoot().translation.setZero();
        fccopy.getRoot().update(time, null, this);

        fccopy.translation.put(this.pos);
        this.posinv.set(this.pos).scl(-1);

        /** ORIENTATION - directions and up **/
        updateDirections(time);
        trf = matrix;
        up.mul(trf).nor();

        // Update cameras
        updateCamera(directions[0], up, camera);

        updateCamera(directions[1], up, camera2);

        // Dir middle
        dirMiddle.set(0, 0, 1).mul(trf);

        // Return to pool
        SceneGraphNode ape = fccopy;
        do {
            ape.returnToPool();
            ape = ape.parent;
        } while (ape != null);

    }

    /**
     * Updates both FOVs' directions applying the right transformation.
     *
     * @param time
     */
    public void updateDirections(ITimeFrameProvider time) {
        lastTime = currentTime;
        currentTime = time.getTime().toEpochMilli();
        trf = matrix;
        trf.idt();
        Quaterniond quat = GaiaAttitudeServer.instance.getAttitude(new Date(time.getTime().toEpochMilli())).getQuaternion();
        trf.rotate(quat).rotate(0, 0, 1, 180);
        directions[0].set(0, 0, 1).rotate(BAM_2, 0, 1, 0).mul(trf).nor();
        directions[1].set(0, 0, 1).rotate(-BAM_2, 0, 1, 0).mul(trf).nor();

        /** WORK OUT INTERPOLATED DIRECTIONS IN THE CASE OF FAST SCANNING **/
        interpolatedDirections.clear();
    }

    public Vector3d[] getDirections(Date d) {
        trf = matrix;
        trf.idt();
        Quaterniond quat = GaiaAttitudeServer.instance.getAttitude(d).getQuaternion();
        trf.rotate(quat).rotate(0, 0, 1, 180);
        dir1.set(0, 0, 1).rotate(BAM_2, 0, 1, 0).mul(trf).nor();
        dir2.set(0, 0, 1).rotate(-BAM_2, 0, 1, 0).mul(trf).nor();
        return new Vector3d[] { dir1.cpy(), dir2.cpy() };
    }

    /**
     * Updates the given camera using the given direction and up vectors. Sets
     * the position to zero.
     *
     * @param dir
     * @param up
     * @param cam
     */
    private void updateCamera(Vector3d dir, Vector3d up, PerspectiveCamera cam) {
        cam.position.set(0f, 0f, 0f);
        cam.direction.set(dir.valuesf());
        cam.up.set(up.valuesf());
        cam.update();
    }

    @Override
    public PerspectiveCamera[] getFrontCameras() {
        switch (parent.mode) {
        case GAIA_FOV1_MODE:
        default:
            return new PerspectiveCamera[] { camera };
        case GAIA_FOV2_MODE:

            return new PerspectiveCamera[] { camera2 };
        case GAIA_FOVS_MODE:
            return new PerspectiveCamera[] { camera, camera2 };
        }
    }

    @Override
    public PerspectiveCamera getCamera() {
        switch (parent.mode) {
        case GAIA_FOV2_MODE:
            return camera2;
        default:
            return camera;
        }
    }

    @Override
    public float getFovFactor() {
        return this.fovFactor;
    }

    @Override
    public Vector3d getDirection() {
        int idx = parent.mode.ordinal() - CameraMode.GAIA_FOV1_MODE.ordinal();
        idx = Math.max(Math.min(idx, 1), 0);
        return directions[idx];
    }

    @Override
    public void setDirection(Vector3d dir) {
        int idx = parent.mode.ordinal() - CameraMode.GAIA_FOV1_MODE.ordinal();
        idx = Math.max(Math.min(idx, 1), 0);
        directions[idx].set(dir);
    }

    @Override
    public Vector3d getUp() {
        return up;
    }

    @Override
    public final Vector3d[] getDirections() {
        return directions;
    }

    @Override
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case GAIA_LOADED:
            this.gaia = (Gaia) data[0];
            break;
        default:
            break;
        }

    }

    @Override
    public void updateMode(ICamera previousCam, CameraMode previousMode, CameraMode newMode, boolean centerFocus, boolean postEvent) {
    }

    @Override
    public int getNCameras() {
        switch (parent.mode) {
        case GAIA_FOV1_MODE:
        case GAIA_FOV2_MODE:
            return 1;
        case GAIA_FOVS_MODE:
            return 2;
        default:
            return 0;
        }
    }

    @Override
    public CameraMode getMode() {
        return parent.mode;
    }

    /**
     * We have fixed field of view angles and thus fixed aspect ratio.
     */
    public void updateAngleEdge(int width, int height) {
        angleEdgeRad = (float) (Satellite.FOV_AL * Math.PI / 180);
        // Update max overlap time
        MAX_OVERLAP_TIME = (long) (angleEdgeRad / (Satellite.SCANRATE * (Math.PI / (3600 * 180)))) * 1000;
        MAX_OVERLAP_ANGLE = angleEdgeRad;
    }

    @Override
    public void render(int rw, int rh) {
        // Renders the focal plane CCDs
        fpstages[parent.mode.ordinal() - CameraMode.GAIA_FOV1_MODE.ordinal()].draw();
    }

    @Override
    public double getSpeed() {
        return parent.getSpeed();
    }

    @Override
    public boolean isFocus(IFocus cb) {
        return false;
    }

    @Override
    public IFocus getFocus() {
        return null;
    }

    @Override
    public boolean isVisible(CelestialBody cb) {
        switch (parent.mode) {
        case GAIA_FOV1_MODE:
        case GAIA_FOV2_MODE:
            return super.isVisible(cb);
        case GAIA_FOVS_MODE:
            return computeVisibleFovs(cb, this);
        default:
            return false;
        }
    }

    @Override
    public void setCamera(PerspectiveCamera cam) {
        // Nothing to do
    }

    @Override
    public void resize(int width, int height) {
        for (Stage stage : fpstages)
            stage.getViewport().update(width, height, true);

    }

    public Frustumd getFrustum2() {
        return frustum2;
    }

    @Override
    public double speedScaling() {
        return 0;
    }

}
