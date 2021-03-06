/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.camera;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

public interface ICamera {

    /**
     * Returns the perspective camera.
     * 
     * @return The perspective camera.
     */
    PerspectiveCamera getCamera();

    /**
     * Sets the active camera
     * 
     * @param cam
     */
    void setCamera(PerspectiveCamera cam);

    PerspectiveCamera getCameraStereoLeft();

    PerspectiveCamera getCameraStereoRight();

    void setCameraStereoLeft(PerspectiveCamera cam);

    void setCameraStereoRight(PerspectiveCamera cam);

    PerspectiveCamera[] getFrontCameras();

    ICamera getCurrent();

    float getFovFactor();

    Vector3b getPos();

    void setPos(Vector3d pos);
    void setPos(Vector3b pos);

    Vector3b getPreviousPos();

    void setPreviousPos(Vector3d pos);
    void setPreviousPos(Vector3b pos);

    void setDirection(Vector3d dir);

    Vector3b getInversePos();

    Vector3d getDirection();

    Vector3d getVelocity();

    Vector3d getUp();

    Vector3d[] getDirections();

    int getNCameras();

    double speedScaling();

    void setShift(Vector3d shift);

    Vector3d getShift();

    Matrix4 getProjView();

    Matrix4 getPreviousProjView();

    void setPreviousProjView(Matrix4 mat);

    /**
     * Updates the camera.
     * 
     * @param dt
     *            The time since the las frame in seconds.
     * @param time
     *            The frame time provider (simulation time).
     */
    void update(double dt, ITimeFrameProvider time);

    void updateMode(ICamera previousCam, CameraMode previousMode, CameraMode newMode, boolean centerFocus, boolean postEvent);

    CameraMode getMode();

    void updateAngleEdge(int width, int height);

    /**
     * Gets the angle of the edge of the screen, diagonally. It assumes the
     * vertical angle is the field of view and corrects the horizontal using the
     * aspect ratio. It depends on the viewport size and the field of view
     * itself.
     * 
     * @return The angle in radians.
     */
    float getAngleEdge();

    CameraManager getManager();

    void render(int rw, int rh);

    /**
     * Gets the current velocity of the camera in km/h.
     * 
     * @return The velocity in km/h.
     */
    double getSpeed();

    /**
     * Gets the distance from the camera to the centre of our reference frame
     * (Sun)
     * 
     * @return The distance
     */
    double getDistance();

    /**
     * Returns the foucs if any
     * 
     * @return The foucs object if it is in focus mode. Null otherwise
     */
    IFocus getFocus();

    /**
     * Checks if this body is the current focus
     * 
     * @param cb
     *            The body
     * @return Whether the body is focus
     */
    boolean isFocus(IFocus cb);

    /**
     * Called after updating the body's distance to the cam, it updates the
     * closest body in the camera to figure out the camera near
     * 
     * @param  focus
     *            The body to check
     */
    void checkClosestBody(IFocus focus);

    IFocus getClosestBody();

    IFocus getSecondClosestBody();

    boolean isVisible(CelestialBody cb);

    boolean isVisible(double viewAngle, Vector3d pos, double distToCamera);

    void resize(int width, int height);

    /**
     * Gets the current closest particle to this camera
     * 
     * @return The closest particle
     */
    IFocus getClosestParticle();

    /**
     * Gets the current i-close light source to this camera
     *
     * @return The i close light source (star?)
     */
    IFocus getCloseLightSource(int i);

    /**
     * Sets the current closest particle to this camera. This will be only set if
     * the given particle is closer than the current.
     * 
     * @param particle
     *            The candidate particle
     */
    void checkClosestParticle(IFocus particle);

    /**
     * Returns the current closest object
     */
    IFocus getClosest();

    /**
     * Sets the closest of all
     * @param focus The new closest object
     */
    void setClosest(IFocus focus);

    void updateFrustumPlanes();

    double getNear();
    double getFar();

    void swapBuffers();

}