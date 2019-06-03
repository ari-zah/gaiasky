/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.camera;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.IStarFocus;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.math.Frustumd;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public abstract class AbstractCamera implements ICamera {
    protected static final Log logger = Logger.getLogger(AbstractCamera.class);

    /** Camera far value **/
    public static final double CAM_FAR = 1e11 * Constants.PC_TO_U;
    /** Camera near values **/
    public static final double CAM_NEAR = 1e7 * Constants.KM_TO_U;

    private static Matrix4d invProjectionView = new Matrix4d();

    public Vector3d pos, posinv, shift, tmp;
    /**
     * Angle from the center to the corner of the screen in scene coordinates,
     * in radians
     **/
    protected float angleEdgeRad;
    /** Aspect ratio **/
    protected float ar;

    /** Distance of camera to center **/
    protected double distance;

    /** The parent **/
    protected CameraManager parent;

    /** Closest entity to camera **/
    protected IFocus closest;

    /** The main camera **/
    public PerspectiveCamera camera;

    /** Stereoscopic mode cameras **/
    protected PerspectiveCamera camLeft, camRight;

    /** Vector with all perspective cameras **/
    protected PerspectiveCamera[] cameras;

    protected Matrix4d projection, view, combined;

    public float fovFactor;

    /**
     * The closest star to the camera
     */
    protected IStarFocus closestStar;

    public AbstractCamera(CameraManager parent) {
        this.parent = parent;
        pos = new Vector3d();
        posinv = new Vector3d();
        shift = new Vector3d();
        tmp = new Vector3d();

        camLeft = new PerspectiveCamera(GlobalConf.scene.CAMERA_FOV, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight());
        camLeft.near = (float) CAM_NEAR;
        camLeft.far = (float) CAM_FAR;

        camRight = new PerspectiveCamera(GlobalConf.scene.CAMERA_FOV, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight());
        camRight.near = (float) CAM_NEAR;
        camRight.far = (float) CAM_FAR;

        projection = new Matrix4d();
        view = new Matrix4d();
        combined = new Matrix4d();
    }

    @Override
    public void updateAngleEdge(int width, int height) {
        ar = (float) width / (float) height;
        float h = camera.fieldOfView;
        float w = h * ar;
        angleEdgeRad = (float) (Math.toRadians(Math.sqrt(h * h + w * w))) / 2f;
    }

    public float getAngleEdge(int width, int height, float angle) {
        float ar = (float) width / (float) height;
        float h = angle;
        float w = h * ar;
        return (float) (Math.toRadians(Math.sqrt(h * h + w * w))) / 2f;
    }

    @Override
    public float getFovFactor() {
        return 1.5f;
    }

    @Override
    public Vector3d getPos() {
        return pos;
    }

    @Override
    public void setPos(Vector3d pos) {
        this.pos.set(pos);
    }

    @Override
    public Vector3d getInversePos() {
        return posinv;
    }

    @Override
    public float getAngleEdge() {
        return angleEdgeRad;
    }

    @Override
    public CameraManager getManager() {
        return parent;
    }

    @Override
    public void render(int rw, int rh) {

    }

    @Override
    public ICamera getCurrent() {
        return this;
    }

    private static final double VIEW_ANGLE = Math.toRadians(0.05);

    public void computeGaiaScan(ITimeFrameProvider time, CelestialBody cb) {
        if (GlobalConf.scene.COMPUTE_GAIA_SCAN && time.getDt() != 0) {
            boolean visibleByGaia = computeVisibleFovs(cb, parent.fovCamera);
            cb.updateTransitNumber(visibleByGaia, time, parent.fovCamera);
        }
    }

    @Override
    public boolean isVisible(ITimeFrameProvider time, CelestialBody cb) {
        return isVisible(time, cb.viewAngle, cb.translation, cb.distToCamera);
    }

    @Override
    public boolean isVisible(ITimeFrameProvider time, double viewAngle, Vector3d pos, double distToCamera) {
        return (!(this instanceof FovCamera) && viewAngle > VIEW_ANGLE) || GlobalResources.isInView(pos, distToCamera, angleEdgeRad, getDirection());
    }

    /**
     * Returns true if a body with the given position is observed in any of the
     * given directions using the given cone angle
     * 
     * @param cb
     *            The body.
     * @param fcamera
     *            The FovCamera.
     * @return True if the body is observed. False otherwise.
     */
    protected boolean computeVisibleFovs(CelestialBody cb, FovCamera fcamera) {
        boolean visible = false;
        Vector3d[] dirs = null;
        if (GlobalConf.scene.COMPUTE_GAIA_SCAN && !fcamera.interpolatedDirections.isEmpty()) {
            // We need to interpolate...
            for (Vector3d[] interpolatedDirection : fcamera.interpolatedDirections) {
                visible = visible || GlobalResources.isInView(cb.translation, cb.distToCamera, fcamera.angleEdgeRad, interpolatedDirection[0]) || GlobalResources.isInView(cb.translation, cb.distToCamera, fcamera.angleEdgeRad, interpolatedDirection[1]);
                if (visible)
                    return true;
            }
        }
        dirs = fcamera.directions;
        visible = visible || GlobalResources.isInView(cb.translation, cb.distToCamera, fcamera.angleEdgeRad, dirs[0]) || GlobalResources.isInView(cb.translation, cb.distToCamera, fcamera.angleEdgeRad, dirs[1]);
        return visible;
    }

    public double getDistance() {
        return distance;
    }

    @Override
    public void checkClosest(IFocus cb) {
        // A copy can never bee the closest
        if (!cb.isCopy())
            if (closest == null) {
                closest = cb;
            } else {
                if (closest.getDistToCamera() - closest.getRadius() > cb.getDistToCamera() - cb.getRadius()) {
                    closest = cb;
                }
            }
    }

    @Override
    public IFocus getClosest() {
        return closest;
    }

    @Override
    public IFocus getClosest2() {
        return closest;
    }

    public void copyParamsFrom(AbstractCamera other) {
        this.pos.set(other.pos);
        this.posinv.set(other.posinv);
        this.getDirection().set(other.getDirection());
        this.getUp().set(other.getUp());
        this.closest = other.closest;

    }

    private void copyCamera(PerspectiveCamera source, PerspectiveCamera target) {
        target.far = source.far;
        target.near = source.near;
        target.direction.set(source.direction);
        target.up.set(source.up);
        target.position.set(source.position);
        target.fieldOfView = source.fieldOfView;
        target.viewportHeight = source.viewportHeight;
        target.viewportWidth = source.viewportWidth;
    }

    @Override
    public PerspectiveCamera getCameraStereoLeft() {
        return camLeft;
    }

    @Override
    public PerspectiveCamera getCameraStereoRight() {
        return camRight;
    }

    @Override
    public void setCameraStereoLeft(PerspectiveCamera cam) {
        copyCamera(cam, camLeft);
    }

    @Override
    public void setCameraStereoRight(PerspectiveCamera cam) {
        copyCamera(cam, camRight);
    }

    @Override
    public void setShift(Vector3d shift) {
        this.shift.set(shift);
    }

    @Override
    public Vector3d getShift() {
        return this.shift;
    }

    public void updateFrustum(Frustumd frustum, PerspectiveCamera cam, Vector3d position, Vector3d direction, Vector3d up) {
        double aspect = cam.viewportWidth / cam.viewportHeight;
        projection.setToProjection(cam.near, cam.far, cam.fieldOfView, aspect);
        view.setToLookAt(position, tmp.set(position).add(direction), up);
        combined.set(projection);
        Matrix4d.mul(combined.val, view.val);

        invProjectionView.set(combined);
        Matrix4d.inv(invProjectionView.val);
        frustum.update(invProjectionView);
    }

    public IStarFocus getClosestStar() {
        return closestStar;
    }

    public void setClosestStar(IStarFocus star) {
        if (closestStar == null || closestStar.getClosestDist() > star.getClosestDist()) {
            closestStar = star;
        }

    }

    @Override
    public Vector3d getVelocity() {
        return null;
    }

}
