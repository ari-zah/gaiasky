/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.script;

import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;

import java.util.List;

/**
 * Scripting interface. Provides an interface to the Gaia Sandbox core and
 * exposes all the methods that are callable from a script in order to interact
 * with the program (create demonstrations, tutorials, load data, etc.). You
 * should never use any integration other than this interface for scripting.
 *
 * @author Toni Sagrista
 */
@SuppressWarnings("unused")
public interface IScriptingInterface {

    /**
     * Pre-loads the given images as textures for later use. They will be cached
     * for the subsequent uses.
     *
     * @param paths The texture paths.
     */
    void preloadTextures(String[] paths);

    /**
     * Pre-loads the given image as a texture for later use. The texture will
     * be cached for later use.
     *
     * @param path The path of the image file to preload
     */
    void preloadTexture(String path);

    /**
     * Sets the current time frame to <b>real time</b>. All the commands
     * executed after this command becomes active will be in the <b>real
     * time</b> frame (clock ticks).
     */
    void activateRealTimeFrame();

    /**
     * Sets the current time frame to <b>simulation time</b>. All the commands
     * executed after this command becomes active will be in the <b>simulation
     * time</b> frame (simulation clock in the app).
     */
    void activateSimulationTimeFrame();

    /**
     * Sets a headline message that will appear in a big font in the screen.
     *
     * @param headline The headline text.
     */
    void setHeadlineMessage(String headline);

    /**
     * Sets a subhead message that will appear in a small font below the
     * headline.
     *
     * @param subhead The subhead text.
     */
    void setSubheadMessage(String subhead);

    /**
     * Clears the headline messge.
     */
    void clearHeadlineMessage();

    /**
     * Clears the subhead message
     */
    void clearSubheadMessage();

    /**
     * Clears both the subhead and the headline messages.
     */
    void clearAllMessages();

    /**
     * Adds a new one-line message in the screen with the given id and the given
     * coordinates. If an object already exists with the given id, it is
     * removed. However, if a message object already exists with the same id,
     * its properties are updated. <strong>The messages placed with this method
     * will not appear in the screenshots/frames in advanced mode. This is
     * intended for running interactively only.</strong>
     *
     * @param id       A unique identifier, used to identify this message when you
     *                 want to remove it.
     * @param message  The string message, to be displayed in one line. But explicit
     *                 newline breaks the line.
     * @param x        The x coordinate of the bottom-left corner, in [0..1] from
     *                 left to right. This is not resolution-dependant.
     * @param y        The y coordinate of the bottom-left corner, in [0..1] from
     *                 bottom to top. This is not resolution-dependant.
     * @param r        The red component of the color in [0..1].
     * @param g        The green component of the color in [0..1].
     * @param b        The blue component of the color in [0..1].
     * @param a        The alpha component of the color in [0..1].
     * @param fontSize The size of the font. The system will use the existing font
     *                 closest to the chosen size and scale it up or down to match
     *                 the desired size. Scaling can cause artifacts, so to ensure
     *                 the best font quality, stick to the existing sizes.
     */
    void displayMessageObject(int id, String message, float x, float y, float r, float g, float b, float a, float fontSize);

    /**
     * Adds a new multi-line text in the screen with the given id, coordinates
     * and size. If an object already exists with the given id, it is removed.
     * However, if a text object already exists with the same id, its properties
     * are updated. <strong>The texts placed with this method will not appear in
     * the screenshots/frames in advanced mode. This is intended for running
     * interactively only.</strong>
     *
     * @param id        A unique identifier, used to identify this message when you
     *                  want to remove it.
     * @param text      The string message, to be displayed line-wrapped in the box
     *                  defined by maxWidth and maxHeight. Explicit newline still
     *                  breaks the line.
     * @param x         The x coordinate of the bottom-left corner, in [0..1] from
     *                  left to right. This is not resolution-dependant.
     * @param y         The y coordinate of the bottom-left corner, in [0..1] from
     *                  bottom to top. This is not resolution-dependant.
     * @param maxWidth  The maximum width in screen percentage [0..1]. Set to 0 to let
     *                  the system decide.
     * @param maxHeight The maximum height in screen percentage [0..1]. Set to 0 to
     *                  let the system decide.
     * @param r         The red component of the color in [0..1].
     * @param g         The green component of the color in [0..1].
     * @param b         The blue component of the color in [0..1].
     * @param a         The alpha component of the color in [0..1].
     * @param fontSize  The size of the font. The system will use the existing font
     *                  closest to the chosen size.
     */
    void displayTextObject(int id, String text, float x, float y, float maxWidth, float maxHeight, float r, float g, float b, float a, float fontSize);

    /**
     * Adds a new image object at the given coordinates. If an object already
     * exists with the given id, it is removed. However, if an image object
     * already exists with the same id, its properties are updated.<br>
     * <strong>The messages placed with this method will not appear in the
     * screenshots/frames in advanced mode. This is intended for running
     * interactively only.</strong>
     *
     * @param id   A unique identifier, used to identify this message when you
     *             want to remove it.
     * @param path The path to the image. It can either be an absolute path (not
     *             recommended) or a path relative to the Gaia Sandbox folder.
     * @param x    The x coordinate of the bottom-left corner, in [0..1] from
     *             left to right. This is not resolution-dependant.
     * @param y    The y coordinate of the bottom-left corner, in [0..1] from
     *             bottom to top. This is not resolution-dependant.
     */
    void displayImageObject(int id, String path, float x, float y);

    /**
     * Adds a new image object at the given coordinates. If an object already
     * exists with the given id, it is removed. However, if an image object
     * already exists with the same id, its properties are updated.<br>
     * <strong>Warning: This method will only work in the asynchronous mode. Run
     * the script with the "asynchronous" check box activated!</strong>
     *
     * @param id   A unique identifier, used to identify this message when you
     *             want to remove it.
     * @param path The path to the image. It can either be an absolute path (not
     *             recommended) or a path relative to the Gaia Sky folder.
     * @param x    The x coordinate of the bottom-left corner, in [0..1] from
     *             left to right. This is not resolution-dependant.
     * @param y    The y coordinate of the bottom-left corner, in [0..1] from
     *             bottom to top. This is not resolution-dependant.
     * @param r    The red component of the color in [0..1].
     * @param g    The green component of the color in [0..1].
     * @param b    The blue component of the color in [0..1].
     * @param a    The alpha component of the color in [0..1].
     */
    void displayImageObject(int id, final String path, float x, float y, float r, float g, float b, float a);

    /**
     * Removes all objects.
     */
    void removeAllObjects();

    /**
     * Removes the item with the given id.
     *
     * @param id Integer with the integer id of the object to remove.
     */
    void removeObject(int id);

    /**
     * Removes the items with the given ids. They can either messages, images or
     * whatever else.
     *
     * @param ids Vector with the integer ids of the objects to remove
     */
    void removeObjects(int[] ids);

    /**
     * Disables all input events from mouse, keyboard, touchscreen, etc.
     */
    void disableInput();

    /**
     * Enables all input events.
     */
    void enableInput();

    /**
     * Enables or disables the cinematic camera mode.
     *
     * @param cinematic Whether to enable or disable the cinematic mode.
     */
    void setCinematicCamera(boolean cinematic);

    /**
     * Sets the camera in focus mode with the focus object that bears the given
     * <code>focusName</code>. It returns immediately, so it does not wait for
     * the camera direction to point to the focus.
     *
     * @param focusName The name of the new focus object.
     */
    void setCameraFocus(String focusName);

    /**
     * Sets the camera in focus mode with the focus object that bears the given
     * <code>focusName</code>. The amount of time to block and wait for the
     * camera to face the focus can also be specified in
     * <code>waitTimeSeconds</code>.
     *
     * @param focusName       The name of the new focus object.
     * @param waitTimeSeconds Maximum time in seconds to wait for the camera to face the
     *                        focus. If negative, we wait indefinitely.
     */
    void setCameraFocus(String focusName, float waitTimeSeconds);

    /**
     * Sets the camera in focus mode with the given focus object. It also
     * instantly sets the camera direction vector to point towards the focus.
     *
     * @param focusName The name of the new focus object.
     */
    void setCameraFocusInstant(final String focusName);

    /**
     * Sets the camera in focus mode with the given focus object and instantly moves
     * the camera next to the focus object.
     *
     * @param focusName The name of the new focus object.
     */
    void setCameraFocusInstantAndGo(final String focusName);

    /**
     * Activates or deactivates the camera lock to the focus reference system
     * when in focus mode.
     *
     * @param lock Activate or deactivate the lock.
     */
    void setCameraLock(boolean lock);

    /**
     * Sets the camera in free mode.
     */
    void setCameraFree();

    /**
     * Sets the camera in FoV1 mode. The camera is positioned in Gaia's focal
     * plane and observes what Gaia observes through its field of view 1.
     */
    void setCameraFov1();

    /**
     * Sets the camera in FoV2 mode. The camera is positioned in Gaia's focal
     * plane and observes what Gaia observes through its field of view 2.
     */
    void setCameraFov2();

    /**
     * Sets the camera in Fov1 and 2 mode. The camera is positioned in Gaia's
     * focal plane and observes what Gaia observes through its two fields of
     * view.
     */
    void setCameraFov1and2();

    /**
     * Sets the camera position to the given coordinates, in Km, equatorial
     * system.
     *
     * @param vec Vector of three components in internal coordinates and Km.
     * @deprecated Use {@link #setCameraPosition(double[])} instead.
     */
    void setCameraPostion(double[] vec);

    /**
     * Sets the camera position to the given coordinates, in Km, equatorial
     * system.
     *
     * @param vec Vector of three components in internal coordinates and Km.
     */
    void setCameraPosition(double[] vec);

    /**
     * Gets the current camera position, in km.
     *
     * @return The camera position coordinates in the internal reference system,
     * in km.
     */
    double[] getCameraPosition();

    /**
     * Sets the camera direction vector to the given vector, equatorial system.
     *
     * @param dir The direction vector in equatorial coordinates.
     */
    void setCameraDirection(double[] dir);

    /**
     * Gets the current camera direction vector.
     *
     * @return The camera direction vector in the internal reference system.
     */
    double[] getCameraDirection();

    /**
     * Sets the camera up vector to the given vector, equatorial system.
     *
     * @param up The up vector in equatorial coordinates.
     */
    void setCameraUp(double[] up);

    /**
     * Gets the current camera up vector.
     *
     * @return The camera up vector in the internal reference system.
     */
    double[] getCameraUp();

    /**
     * Sets the focus and instantly moves the camera to a point in the line
     * defined by <code>focus</code>-<code>other</code> and rotated
     * <code>rotation</code> degrees around <code>focus<code> using the camera
     * up vector as a rotation axis.
     *
     * @param focus     The name of the focus object
     * @param other     The name of the other object, to the fine a line from this to
     *                  foucs. Usually a light source
     * @param rotation  The rotation angle, in degrees
     * @param viewAngle The view angle which determines the distance, in degrees.
     */
    void setCameraPositionAndFocus(String focus, String other, double rotation, double viewAngle);

    /**
     * Sets the camera in free mode and points it to the given coordinates in equatorial system
     *
     * @param ra  Right ascension in degrees
     * @param dec Declination in degrees
     */
    void pointAtSkyCoordinate(double ra, double dec);

    /**
     * Changes the speed multiplier of the camera and its acceleration
     *
     * @param speed The new speed, from 1 to 100
     */
    void setCameraSpeed(float speed);

    /**
     * Gets the current physical speed of the camera in km/h
     *
     * @return The current speed of the camera in km/h
     */
    double getCameraSpeed();

    /**
     * Changes the speed of the camera when it rotates around a focus.
     *
     * @param speed The new rotation speed, from 1 to 100.
     */
    void setRotationCameraSpeed(float speed);

    /**
     * Changes the turning speed of the camera.
     *
     * @param speed The new turning speed, from 1 to 100.
     */
    void setTurningCameraSpeed(float speed);

    /**
     * Sets the speed limit of the camera given an index. The index corresponds
     * to the following:
     * <ul>
     * <li>0 - 100 Km/h</li>
     * <li>1 - 1 c</li>
     * <li>2 - 2 c</li>
     * <li>3 - 10 c</li>
     * <li>4 - 1e3 c</li>
     * <li>5 - 1 AU/s</li>
     * <li>6 - 10 AU/s</li>
     * <li>7 - 1000 AU/s</li>
     * <li>8 - 10000 AU/s</li>
     * <li>9 - 1 pc/s</li>
     * <li>10 - 1 pc/s</li>
     * <li>11 - 2 pc/s</li>
     * <li>12 - 10 pc/s</li>
     * <li>13 - 1000 pc/s</li>
     * <li>14 - unlimited</li>
     * </ul>
     *
     * @param index The index of the top speed.
     */
    void setCameraSpeedLimit(int index);

    /**
     * Locks or unlocks the orientation of the camera to the focus object's
     * rotation.
     *
     * @param lock Whether to lock or unlock the camera orientation to the focus
     */
    void setCameraOrientationLock(boolean lock);

    /**
     * Adds a forward movement to the camera with the given value. If value is
     * negative the movement is backwards.
     *
     * @param value The magnitude of the movement, between -1 and 1.
     */
    void cameraForward(double value);

    /**
     * Adds a rotation movement to the camera, or a pitch/yaw if in free mode.
     *
     * @param deltaX The x component, between 0 and 1. Positive is right and
     *               negative is left.
     * @param deltaY The y component, between 0 and 1. Positive is up and negative
     *               is down.
     */
    void cameraRotate(double deltaX, double deltaY);

    /**
     * Adds a roll force to the camera.
     *
     * @param roll The intensity of the roll.
     */
    void cameraRoll(double roll);

    /**
     * Adds a turn force to the camera. If the camera is in focus mode, it
     * permanently deviates the line of sight from the focus until centered
     * again.
     *
     * @param deltaX The x component, between 0 and 1. Positive is right and
     *               negative is left.
     * @param deltaY The y component, between 0 and 1. Positive is up and negative
     *               is down.
     */
    void cameraTurn(double deltaX, double deltaY);

    /**
     * Stops all camera motion.
     */
    void cameraStop();

    /**
     * Centers the camera to the focus, removing any deviation of the line of
     * sight. Useful to center the focus object again after turning.
     */
    void cameraCenter();

    /**
     * Returns the closest object to the camera in this instant as a
     * {@link gaia.cu9.ari.gaiaorbit.scenegraph.IFocus}.
     *
     * @return The closest object to the camera
     */
    IFocus getClosestObjectToCamera();

    /**
     * Changes the field of view of the camera.
     *
     * @param newFov The new field of view value in degrees, between 20 and 160.
     */
    void setFov(float newFov);

    /**
     * Sets the camera state (position, direction and up vector).
     *
     * @param pos The position of the camera in internal units, not Km
     * @param dir The direction of the camera
     * @param up  The up vector of the camera
     */
    void setCameraState(double[] pos, double[] dir, double[] up);

    /**
     * Sets the camera state (position, direction and up vector) plus the current time.
     *
     * @param pos  The position of the camera in internal units, not Km
     * @param dir  The direction of the camera
     * @param up   The up vector of the camera
     * @param time The new time of the camera as the
     *             number of milliseconds since the epoch (Jan 1, 1970)
     */
    void setCameraStateAndTime(double[] pos, double[] dir, double[] up, long time);

    /**
     * Sets the component described by the given name visible or invisible.
     *
     * @param key     The key of the component, see
     *                {@link gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType}. Usually
     *                'element.stars', 'element.moons', 'element.atmospheres', etc.
     * @param visible The visible value.
     */
    void setVisibility(String key, boolean visible);

    /**
     * Sets the number factor of proper motion vectors that are visible. In [1..100].
     *
     * @param factor Factor in [1..100]
     */
    void setProperMotionsNumberFactor(float factor);

    /**
     * Sets the length of the proper motion vectors, in [500..30000].
     *
     * @param factor Factor in [500.30000]
     */
    void setProperMotionsLengthFactor(float factor);

    /**
     * Sets the color mode of proper motion vectors.
     * @param mode The color mode:
     *             <ul>
     *             <li>0 - direction: the normalised cartesian velocity components XYZ are mapped to the color channels RGB</li>
     *             <li>1 - magnitude (speed): the magnitude of the velocity vector is mapped using a rainbow scheme (blue-green-yellow-red) with the color map limit at 100 Km/s</li>
     *             <li>2 - has radial velocity: blue for stars with radial velocity, red for stars without</li>
     *             <li>3 - redshift from Sun: blue stars have negative radial velocity (from the Sun), red stars have positive radial velocity (from the Sun). Blue is mapped to -100 Km/s, red is mapped to 100 Km/s</li>
     *             <li>4 - redshift from camera: blue stars have negative radial velocity (from the camera), red stars have positive radial velocity (from the camera). Blue is mapped to -100 Km/s, red is mapped to 100 Km/s</li>
     *             <li>5 - single color: same color for all velocity vectors</li>
     *             </ul>
     */
    void setProperMotionsColorMode(int mode);

    /**
     * Sets whether to show arrowheads or not for the velocity vectors.
     * @param arrowheadsEnabled Whether to show the velocity vectors with arrowheads.
     */
    void setProperMotionsArrowheads(boolean arrowheadsEnabled);

    /**
     * Overrides the maximum number of proper motion vectors that the program
     * is allowed to show.
     * @param maxNumber The maximum number of proper motion vectors. Negative to use default
     */
    void setProperMotionsMaxNumber(long maxNumber);

    /**
     * Returns the current maximum number of proper motion vectors allowed.
     * @return Max number of pm vectors
     */
    long getProperMotionsMaxNumber();

    /**
     * Sets the visibility of the crosshair in focus and free modes.
     *
     * @param visible The visibility state.
     */
    void setCrosshairVisibility(boolean visible);

    /**
     * Sets the ambient light to a certain value.
     *
     * @param value The value of the ambient light, between 0 and 100.
     */
    void setAmbientLight(float value);

    /**
     * Sets the time of the application, in UTC.
     *
     * @param year     The year to represent
     * @param month    The month-of-year to represent, from 1 (January) to 12
     *                 (December)
     * @param day      The day-of-month to represent, from 1 to 31
     * @param hour     The hour-of-day to represent, from 0 to 23
     * @param min      The minute-of-hour to represent, from 0 to 59
     * @param sec      The second-of-minute to represent, from 0 to 59
     * @param millisec The millisecond-of-second, from 0 to 999
     */
    void setSimulationTime(int year, int month, int day, int hour, int min, int sec, int millisec);

    /**
     * Sets the time of the application. The long value represents specified
     * number of milliseconds since the standard base time known as "the epoch",
     * namely January 1, 1970, 00:00:00 GMT.
     *
     * @param time Number of milliseconds since the epoch (Jan 1, 1970)
     */
    void setSimulationTime(long time);

    /**
     * Returns the current simulation time as the number of milliseconds since
     * Jan 1, 1970 GMT.
     *
     * @return Number of milliseconds since the epoch (Jan 1, 1970)
     */
    long getSimulationTime();

    /**
     * Returns the current UTC simulation time in an array.
     *
     * @return The current simulation time in an array with the given indices.
     * <ul>
     * <li>0 - The year</li>
     * <li>1 - The month, from 1 (January) to 12 (December)</li>
     * <li>2 - The day-of-month, from 1 to 31</li>
     * <li>3 - The hour-of-day, from 0 to 23</li>
     * <li>4 - The minute-of-hour, from 0 to 59</li>
     * <li>5 - The second-of-minute, from 0 to 59</li>
     * <li>6 - The millisecond-of-second, from 0 to 999</li>
     * </ul>
     */
    int[] getSimulationTimeArr();

    /**
     * Starts the simulation.
     */
    void startSimulationTime();

    /**
     * Stops the simulation time.
     */
    void stopSimulationTime();

    /**
     * Queries whether the time is on or not.
     *
     * @return True if the time is on, false otherwise.
     */
    boolean isSimulationTimeOn();

    /**
     * Changes the pace of time.
     *
     * @param pace The pace as a factor of real physical time pace. 2.0 sets the
     *             pace to be twice as fast as real time.
     */
    void setSimulationPace(double pace);

    /**
     * Sets a time bookmark in the global clock that, when reached, the clock
     * automatically stops.
     *
     * @param ms The time as the number of milliseconds since the epoch (Jan 1,
     *           1970)
     */
    void setTargetTime(long ms);

    /**
     * Sets a time bookmark in the global clock that, when reached, the clock
     * automatically stops.
     *
     * @param year     The year to represent
     * @param month    The month-of-year to represent, from 1 (January) to 12
     *                 (December)
     * @param day      The day-of-month to represent, from 1 to 31
     * @param hour     The hour-of-day to represent, from 0 to 23
     * @param min      The minute-of-hour to represent, from 0 to 59
     * @param sec      The second-of-minute to represent, from 0 to 59
     * @param millisec The millisecond-of-second, from 0 to 999
     */
    void setTargetTime(int year, int month, int day, int hour, int min, int sec, int millisec);

    /**
     * Unsets the target time bookmark from the global clock, if any.
     */
    void unsetTargetTime();

    /**
     * Gets the star brightness value.
     *
     * @return The brightness value, between 0 and 100.
     */
    float getStarBrightness();

    /**
     * Sets the star brightness value.
     *
     * @param brightness The brightness value, between 0 and 100.
     */
    void setStarBrightness(float brightness);

    /**
     * Gets the current star size.
     *
     * @return The size value, between 0 and 100.
     */
    float getStarSize();

    /**
     * Sets the star size value.
     *
     * @param size The size value, between 0 and 100.
     */
    void setStarSize(float size);

    /**
     * Gets the minimum star opacity.
     *
     * @return The minimum opacity value, between 0 and 100.
     */
    float getMinStarOpacity();

    /**
     * Sets the minimum star opacity.
     *
     * @param opacity The minimum opacity value, between 0 and 100.
     */
    void setMinStarOpacity(float opacity);

    /**
     * Configures the frame output system, setting the resolution of the images,
     * the target frames per second, the output folder and the image name
     * prefix.
     *
     * @param width      Width of images.
     * @param height     Height of images.
     * @param fps        Target frames per second (number of images per second).
     * @param folder     The output folder path.
     * @param namePrefix The file name prefix.
     * @deprecated
     */
    void configureRenderOutput(int width, int height, int fps, String folder, String namePrefix);

    /**
     * Configures the frame output system, setting the resolution of the images,
     * the target frames per second, the output folder and the image name
     * prefix. This function sets the frame output mode to 'redraw'.
     *
     * @param width      Width of images.
     * @param height     Height of images.
     * @param fps        Target frames per second (number of images per second).
     * @param folder     The output folder path.
     * @param namePrefix The file name prefix.
     */
    void configureFrameOutput(int width, int height, int fps, String folder, String namePrefix);

    /**
     * Sets the frame output mode. Possible values are 'redraw' or 'simple'.
     * Simple mode is faster and just outputs the last frame rendered to the Gaia Sky window, with the same
     * resolution and containing the UI elements.
     * Redraw mode redraws the last frame using the resolution configured using {@link #configureFrameOutput(int, int, int, String, String)} and
     * it does not draw the UI elements.
     * @param screenshotMode The screenshot mode. 'simple' or 'redraw'.
     */
    void setFrameOutputMode(String screenshotMode);

    /**
     * Is the frame output system on?
     *
     * @return True if the frame output is active.
     * @deprecated
     */
    boolean isRenderOutputActive();

    /**
     * Is the frame output system on?
     *
     * @return True if the render output is active.
     */
    boolean isFrameOutputActive();

    /**
     * Gets the current FPS setting in the frame output system.
     *
     * @return The FPS setting.
     * @deprecated
     */
    int getRenderOutputFps();

    /**
     * Gets the current FPS setting in the frame output system.
     *
     * @return The FPS setting.
     */
    int getFrameOutputFps();

    /**
     * Activates or deactivates the image output system. If called with true,
     * the system starts outputting images right away.
     *
     * @param active Whether to activate or deactivate the frame output system.
     */
    void setFrameOutput(boolean active);

    /**
     * Gets an object from the scene graph by <code>name</code> or id (HIP, TYC, Gaia SourceId).
     *
     * @param name The name or id (HIP, TYC, Gaia SourceId) of the object.
     * @return The object as a
     * {@link gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode}, or null
     * if it does not exist.
     */
    SceneGraphNode getObject(String name);

    /**
     * Gets an object by <code>name</code> or id (HIP, TYC, Gaia SourceID), optionally waiting
     * until the object is available, with a timeout.
     *
     * @param name The name or id (HIP, TYC, Gaia SourceId) of the object
     * @param timeOutSeconds The timeout in seconds to wait until returning.
     *                       If negative, it waits indefinitely.
     * @return The object if it exists, or null if it does not and block is false, or if block is true and
     * the timeout has passed.
     */
    SceneGraphNode getObject(String name, double timeOutSeconds);
    /**
     * Sets the given size scaling factor to the object identified by
     * <code>name</code>. This method will only work with model objects such as
     * planets, asteroids, satellites etc. It will not work with orbits, stars
     * or any other types.
     * <p>
     * Also, <strong>use this with caution</strong>, as scaling the size of
     * objects can have unintended side effects, and remember to set the scaling
     * back to 1.0 at the end of your script.
     *
     * @param name          The name or id (HIP, TYC, sourceId) of the object.
     * @param scalingFactor The scaling factor to scale the size of that object.
     */
    void setObjectSizeScaling(String name, double scalingFactor);

    /**
     * Gets the size of the object identified by <code>name</code>, in Km, by
     * name or id (HIP, TYC, sourceId).
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     * @return The radius of the object in Km. If the object identifed by name
     * or id (HIP, TYC, sourceId). does not exist, it returns a negative
     * value.
     */
    double getObjectRadius(String name);

    /**
     * Runs a seamless trip to the object with the name <code>focusName</code>
     * until the object view angle is <code>20 degrees</code>.
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     */
    void goToObject(String name);

    /**
     * Runs a seamless trip to the object with the name <code>focusName</code>
     * until the object view angle <code>viewAngle</code> is met. If angle is
     * negative, the default angle is <code>20 degrees</code>.
     *
     * @param name      The name or id (HIP, TYC, sourceId) of the object.
     * @param viewAngle The target view angle of the object, in degrees. The angle
     *                  gets larger and larger as we approach the object.
     */
    void goToObject(String name, double viewAngle);

    /**
     * Runs a seamless trip to the object with the name <code>focusName</code>
     * until the object view angle <code>viewAngle</code> is met. If angle is
     * negative, the default angle is <code>20 degrees</code>. If
     * <code>waitTimeSeconds</code> is positive, it indicates the number of
     * seconds to wait (block the function) for the camera to face the focus
     * before starting the forward movement. This very much depends on the
     * <code>turn velocity</code> of the camera. See
     * {@link #setTurningCameraSpeed(float)}.
     *
     * @param name            The name or id (HIP, TYC, sourceId) of the object.
     * @param viewAngle       The target view angle of the object, in degrees. The angle
     *                        gets larger and larger as we approach the object.
     * @param waitTimeSeconds The seconds to wait for the camera direction vector and the
     *                        vector from the camera position to the target object to be
     *                        aligned.
     */
    void goToObject(String name, double viewAngle, float waitTimeSeconds);

    /**
     * Sets the camera in focus mode with the given focus object and instantly moves
     * the camera next to the focus object.
     *
     * @param name The name of the new focus object.
     */
    void goToObjectInstant(String name);

    /**
     * Lands on the object with the given name, if it is an instance of
     * {@link gaia.cu9.ari.gaiaorbit.scenegraph.Planet}. The land location is
     * determined by the line of sight from the current position of the camera
     * to the object.
     *
     * @param name The proper name of the object.
     */
    void landOnObject(String name);

    /**
     * Lands on the object with the given <code>name</code>, if it is an
     * instance of {@link gaia.cu9.ari.gaiaorbit.scenegraph.Planet}, at the
     * location with the given name, if it exists.
     *
     * @param name         The proper name of the object.
     * @param locationName The name of the location to land on
     */
    void landOnObjectLocation(String name, String locationName);

    /**
     * Lands on the object with the given <code>name</code>, if it is an
     * instance of {@link gaia.cu9.ari.gaiaorbit.scenegraph.Planet}, at the
     * location specified in by [latitude, longitude], in degrees.
     *
     * @param name      The proper name of the object.
     * @param longitude The location longitude, in degrees.
     * @param latitude  The location latitude, in degrees.
     */
    void landOnObjectLocation(String name, double longitude, double latitude);

    /**
     * Returns the distance to the surface of the object identified with the
     * given <code>name</code>. If the object is an abstract node or does not
     * exist, it returns a negative distance.
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     * @return The distance to the object in km if it exists, a negative value
     * otherwise.
     */
    double getDistanceTo(String name);

    /**
     * Gets the current position of the object identified by <code>name</code> in
     * the internal coordinate system and internal units. If the object does not exist,
     * it returns null
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     * @return A 3-vector with the object's position in the internal reference system.
     */
    double[] getObjectPosition(String name);

    /**
     * Adds a new polyline with the given name, points and color. The polyline will
     * be created with the 'Others' component type, so you need to enable the
     * visibility of 'Others' in order to see it.
     *
     * @param name   The name to identify the polyline, to possibly remove it later.
     * @param points The points of the polyline. It is an array containing all the
     *               points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn].
     * @param color  A 4D array with the RGBA color, where each element is in [0..1].
     */
    void addPolyline(String name, double[] points, double[] color);

    /**
     * Adds a new polyline with the given name, points, color and line width. The polyline will
     * be created with the 'Others' component type, so you need to enable the
     * visibility of 'Others' in order to see it.
     *
     * @param name      The name to identify the polyline, to possibly remove it later.
     * @param points    The points of the polyline. It is an array containing all the
     *                  points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn].
     * @param color     A 4D array with the RGBA color, where each element is in [0..1].
     * @param lineWidth The line width. Usually a value between 1 (default) and 10.
     */
    void addPolyline(String name, double[] points, double[] color, float lineWidth);

    /**
     * <p>
     * Removes the model object identified by the given name from the internal
     * scene graph model of Gaia Sky, if it exists.
     * If the object has children, they are removed recursively.
     * Be careful with this function, as it can have unexpected side effects
     * depending on what objects are removed.
     * For example,
     * </p>
     *
     * <code>
     * gs.removeModelObject("Earth")
     * </code>
     *
     * <p>
     * removes the Earth, the Moon, Gaia and any dependent object from Gaia Sky.
     * </p>
     *
     * @param name The name of the object to remove.
     */
    void removeModelObject(String name);

    /**
     * Sets the vertical scroll position in the GUI.
     *
     * @param pixelY The pixel to set the scroll position to.
     */
    void setGuiScrollPosition(float pixelY);

    /**
     * Enables the GUI rendering. This makes the user interface
     * to be rendered and updated again if it was previously disabled. Otherwise, it has
     * no effect.
     */
    void enableGui();

    /**
     * Disables the GUI rendering. This causes the user interface
     * to no longer be rendered or updated.
     */
    void disableGui();

    /**
     * Maximizes the interface window.
     */
    void maximizeInterfaceWindow();

    /**
     * Minimizes the interface window.
     */
    void minimizeInterfaceWindow();

    /**
     * Moves the interface window to a new position.
     *
     * @param x The new x coordinate of the new top-left corner of the window,
     *          in [0..1] from left to right.
     * @param y The new y coordinate of the new top-left corner of the window,
     *          in [0..1] from bottom to top.
     */
    void setGuiPosition(float x, float y);

    /**
     * Blocks the execution until any kind of input (keyboard, mouse, etc.) is
     * received.
     */
    void waitForInput();

    /**
     * Blocks the execution until the Enter key is pressed.
     */
    void waitForEnter();

    /**
     * Blocks the execution until the given key or button is pressed.
     *
     * @param code The key or button code. Please see
     *             {@link com.badlogic.gdx.Input}.
     */
    void waitForInput(int code);

    /**
     * Returns the screen width in pixels.
     *
     * @return The screen width in pixels.
     */
    int getScreenWidth();

    /**
     * Returns the screen height in pixels.
     *
     * @return The screen height in pixels.
     */
    int getScreenHeight();

    /**
     * Returns the size and position of the GUI element that goes by the given
     * name or null if such element does not exist. <strong>Warning> This will
     * only work in asynchronous mode.</strong>
     *
     * @param name The name of the gui element.
     * @return A vector of floats with the position (0, 1) of the bottom left
     * corner in pixels from the bottom-left of the screen and the size
     * (2, 3) in pixels of the element.
     */
    float[] getPositionAndSizeGui(String name);

    /**
     * Returns the version number string.
     *
     * @return The version number string.
     */
    String getVersionNumber();

    /**
     * Blocks the script until the focus is the object indicated by the name.
     * There is an optional time out.
     *
     * @param name      The name of the focus to wait for
     * @param timeoutMs Timeout in ms to wait. Set negative to disable timeout.
     * @return True if the timeout ran out. False otherwise.
     */
    boolean waitFocus(String name, long timeoutMs);

    /**
     * Starts recording the camera path to a temporary file. This command has no
     * effect if the camera is already being recorded.
     */
    void startRecordingCameraPath();

    /**
     * Stops the current camera recording. This command has no effect if the
     * camera was not being recorded.
     */
    void stopRecordingCameraPath();

    /**
     * Runs the camera recording file with the given path. Does not
     * wait for the camera file to finish playing.
     *
     * @param file The path of the camera file. Path is relative to the application's root directory or absolute.
     */
    void runCameraRecording(String file);

    /**
     * Runs a .gsc camera path file and returns immediately. This
     * function does not wait for the camera file to finish playing.
     *
     * @param file The path to the camera file. Path is relative to the application's root directory or absolute.
     */
    void runCameraPath(String file);

    /**
     * Runs a .gsc camera path file and returns immediately. This
     * function accepts a boolean indicating whether to wait for the
     * camera path file to finish or not.
     *
     * @param file The path to the camera file. Path is relative to the application's root directory or absolute.
     * @param sync If true, the call is synchronous and waits for the camera
     *             file to finish. Otherwise, it returns immediately.
     */
    void runCameraPath(String file, boolean sync);

    /**
     * Creates a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds. This function waits for the transition to finish and then returns control
     * to the script.
     * This function will put the camera in free mode, so make sure to change it afterwards if you need to. Also,
     * this only works with the natural camera.
     *
     * @param camPos  The target camera position in the internal reference system.
     * @param camDir  The target camera direction in the internal reference system.
     * @param camUp   The target camera up in the internal reference system.
     * @param seconds The duration of the transition in seconds.
     */
    void cameraTransition(double[] camPos, double[] camDir, double[] camUp, double seconds);

    /**
     * Creates a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds. Optionally, the transition may be run synchronously or asynchronously to the
     * current script.
     * This function will put the camera in free mode, so make sure to change it afterwards if you need to. Also,
     * this only works with the natural camera.
     *
     * @param camPos  The target camera position in the internal reference system.
     * @param camDir  The target camera direction in the internal reference system.
     * @param camUp   The target camera up in the internal reference system.
     * @param seconds The duration of the transition in seconds.
     * @param sync    If true, the call waits for the transition to finish before returning, otherwise it returns immediately
     */
    void cameraTransition(double[] camPos, double[] camDir, double[] camUp, double seconds, boolean sync);

    /**
     * Sleeps for the given number of seconds in the application time (FPS), so
     * if we are capturing frames and the frame rate is set to 30 FPS, the
     * command sleep(1) will put the script to sleep for 30 frames.
     *
     * @param seconds The number of seconds to wait.
     */
    void sleep(float seconds);

    /**
     * Sleeps for a number of frames. This is very useful for scripts which need
     * to run alongside the frame output system.
     *
     * @param frames The number of frames to wait.
     */
    void sleepFrames(long frames);

    /**
     * Expands the component with the given name.
     *
     * @param name The name, as in `CameraComponent` or `ObjectsComponent`
     */
    void expandGuiComponent(String name);

    /**
     * Collapses the component with the given name.
     *
     * @param name The name, as in `CameraComponent` or `ObjectsComponent`
     */
    void collapseGuiComponent(String name);

    /**
     * Converts galactic coordinates to the internal cartesian coordinate
     * system.
     *
     * @param l The galactic longitude in degrees.
     * @param b The galactic latitude in degrees.
     * @param r The distance in Km.
     * @return An array of doubles containing <code>[x, y, z]</code> in the
     * internal reference system, in internal units.
     */
    double[] galacticToInternalCartesian(double l, double b, double r);

    /**
     * Converts ecliptic coordinates to the internal cartesian coordinate
     * system.
     *
     * @param l The ecliptic longitude in degrees.
     * @param b The ecliptic latitude in degrees.
     * @param r The distance in Km.
     * @return An array of doubles containing <code>[x, y, z]</code> in the
     * internal reference system, in internal units.
     */
    double[] eclipticToInternalCartesian(double l, double b, double r);

    /**
     * Converts equatorial coordinates to the internal cartesian coordinate
     * system.
     *
     * @param ra  The right ascension in degrees.
     * @param dec The declination in degrees.
     * @param r   The distance in Km.
     * @return An array of doubles containing <code>[x, y, z]</code> in the
     * internal reference system, in internal units.
     */
    double[] equatorialToInternalCartesian(double ra, double dec, double r);

    /**
     * Converts internal cartesian coordinates to equatorial
     * <code>[ra, dec, distance]</code> coordinates.
     *
     * @param x The x component, in any distance units.
     * @param y The y component, in any distance units.
     * @param z The z component, in any distance units.
     * @return An array of doubles containing <code>[ra, dec, distance]</code>
     * with <code>ra</code> and <code>dec</code> in degrees and
     * <code>distance</code> in the same distance units as the input
     * position.
     */
    double[] internalCartesianToEquatorial(double x, double y, double z);

    /**
     * Converts equatorial cartesian coordinates (in the internal reference system)
     * to galactic cartesian coordinates.
     *
     * @param eq Vector with [x, y, z] equatorial cartesian coordinates
     * @return Vector with [x, y, z] galactic cartesian coordinates
     */
    double[] equatorialToGalactic(double[] eq);

    /**
     * Converts equatorial cartesian coordinates (in the internal reference system)
     * to ecliptic cartesian coordinates.
     *
     * @param eq Vector with [x, y, z] equatorial cartesian coordinates
     * @return Vector with [x, y, z] ecliptic cartesian coordinates
     */
    double[] equatorialToEcliptic(double[] eq);

    /**
     * Converts galactic cartesian coordinates (in the internal reference system)
     * to equatorial cartesian coordinates.
     *
     * @param gal Vector with [x, y, z] galactic cartesian coordinates
     * @return Vector with [x, y, z] equatorial cartesian coordinates
     */
    double[] galacticToEquatorial(double[] gal);

    /**
     * Converts ecliptic cartesian coordinates (in the internal reference system)
     * to equatorial cartesian coordinates.
     *
     * @param ecl Vector with [x, y, z] ecliptic cartesian coordinates
     * @return Vector with [x, y, z] equatorial cartesian coordinates
     */
    double[] eclipticToEquatorial(double[] ecl);

    /**
     * Sets the brightness level of the render system.
     *
     * @param level The brightness level as a double precision floating point
     *              number in [-1..1]. The neutral value is 0.0.
     */
    void setBrightnessLevel(double level);

    /**
     * Sets the contrast level of the render system.
     *
     * @param level The contrast level as a double precision floating point number
     *              in [0..2]. The neutral value is 1.0.
     */
    void setContrastLevel(double level);

    /**
     * Sets the hue level of the render system.
     *
     * @param level The hue level as a double precision floating point number
     *              in [0..2]. The neutral value is 1.0.
     */
    void setHueLevel(double level);

    /**
     * Sets the saturation level of the render system.
     *
     * @param level The saturation level as a double precision floating point number
     *              in [0..2]. The neutral value is 1.0.
     */
    void setSaturationLevel(double level);

    /**
     * Enables and disables the planetarium mode.
     *
     * @param state The boolean sate. True to activate, false to deactivate.
     */
    void setPlanetariumMode(boolean state);

    /**
     * Enables and disables the 360 mode.
     *
     * @param state The boolean sate. True to activate, false to deactivate.
     */
    void set360Mode(boolean state);

    /**
     * Sets the resolution (width and height are the same) of each side of the
     * frame buffers used to capture each of the 6 directions that go into the
     * cubemap to construct the equirectangular image for the 360 mode. This
     * should roughly be 1/3 of the output resolution at which the 360 mode are
     * to be captured (or screen resolution).
     *
     * @param resolution The resolution of each of the sides of the cubemap for the 360
     *                   mode.
     */
    void setCubemapResolution(int resolution);

    /**
     * Sets the cubemap projection to use.
     * Accepted values are "EQUIRECTANGULAR", "CYLINDRICAL" and "HAMMER".
     * See {@link com.bitfire.postprocessing.effects.CubemapProjections.CubemapProjection} for possible
     * values.
     * @param projection
     */
    void setCubemapProjection(String projection);

    /**
     * Enables and disables the stereoscopic mode.
     *
     * @param state The boolean sate. True to activate, false to deactivate.
     */
    void setStereoscopicMode(boolean state);

    /**
     * Changes the stereoscopic profile.
     *
     * @param index The index of the new profile:
     *              <ul>
     *              <li>0 - VR_HEADSET</li>
     *              <li>1 - HD_3DTV</li>
     *              <li>2 - CROSSEYE</li>
     *              <li>3 - PARALLEL_VIEW</li>
     *              <li>4 - ANAGLYPHIC (red-cyan)</li>
     *              </ul>
     */
    void setStereoscopicProfile(int index);

    /**
     * Gets the current frame number. Useful for timing actions in scripts.
     *
     * @return The current frame number
     */
    long getCurrentFrameNumber();

    /**
     * Enables or deisables the lens flare effect.
     *
     * @param state Activate (true) or deactivate (false)
     */
    void setLensFlare(boolean state);

    /**
     * Enables or disables the motion blur effect.
     *
     * @param state Activate (true) or deactivate (false)
     */
    void setMotionBlur(boolean state);

    /**
     * Enables or disables the star glow effect.
     *
     * @param state Activate (true) or deactivate (false)
     */
    void setStarGlow(boolean state);

    /**
     * Sets the strength value for the bloom effect.
     *
     * @param value Bloom strength between 0 and 100. Set to 0 to deactivate the
     *              bloom.
     */
    void setBloom(float value);

    /**
     * Sets the value of smooth lod transitions, allowing or disallowing octant fade-ins of
     * as they come into view.
     *
     * @param value Activate (true) or deactivate (false)
     */
    void setSmoothLodTransitions(boolean value);

    /**
     * Gets the absolute path of the default directory where the still frames are saved
     *
     * @return Absolute path of directory where still frames are saved
     */
    String getDefaultFramesDir();

    /**
     * Gets the absolute path of the default directory where the screenshots are saved
     *
     * @return Absolute path of directory where screenshots are saved
     */
    String getDefaultScreenshotsDir();

    /**
     * Gets the absolute path of the default directory where the camera files are saved
     *
     * @return Absolute path of directory where camera files are saved
     */
    String getDefaultCameraDir();

    /**
     * Gets the absolute path to the location of the music files
     *
     * @return Absolute path to the location of the music files
     */
    String getDefaultMusicDir();

    /**
     * Gets the absolute path to the location of the controller mappings
     *
     * @return Absolute path to the location of the controller mappings
     */
    String getDefaultMappingsDir();

    /**
     * Gets the absolute path of the local data directory, configured in your global.properties file
     *
     * @return Absolute path to the location of the data files
     */
    String getDataDir();

    /**
     * Gets the absolute path to the location of the configuration directory
     *
     * @return Absolute path of config directory
     */
    String getConfigDir();

    /**
     * Returns the default data directory. That is ~/.gaiasky/ in Windows and macOS, and ~/.local/share/gaiasky
     * in Linux.
     *
     * @return Absolute path of data directory
     */
    String getLocalDataDir();

    /**
     * Posts a {@link Runnable} to the main loop thread. The runnable runs only once.
     * This will execute the runnable right after the current update-render cycle has finished.
     *
     * @param runnable The runnable to run
     */
    void postRunnable(Runnable runnable);

    /**
     * Parks a {@link Runnable} to the main loop thread, and keeps it running every frame
     * until it finishes or it is unparked by {@link #unparkRunnable(String)}.
     * Be careful with this function, as it probably needs a cleanup before the script is finished. Otherwise,
     * all parked runnables will keep running until Gaia Sky is restarted, so make sure to
     * remove them with {@link #unparkRunnable(String)} if needed.
     *
     * @param id       The string id to identify the runnable
     * @param runnable The runnable to park
     */
    void parkRunnable(String id, Runnable runnable);

    /**
     * Removes the runnable with the given id, if any
     *
     * @param id The id of the runnable to remove
     */
    void unparkRunnable(String id);

    /**
     * Loads a VOTable file (<code>.vot</code>) with a given name.
     * In this version, the loading happens synchronously, so the catalog is available to Gaia Sky immediately after
     * this call returns.
     * The actual loading process is carried out
     * making educated guesses about semantics using UCDs and column names.
     * Please check <a href="http://gaia.ari.uni-heidelberg.de/gaiasky/docs/html/latest/SAMP.html#stil-data-provider">the
     * official documentation</a> for a complete reference on what can and what can't be loaded.
     *
     * @param dsName       The name of the dataset, used to identify the subsequent operations on the
     *                     dataset
     * @param absolutePath Absolute path to the <code>.vot</code> file to load
     *
     * @return False if the dataset could not be loaded, true otherwise
     */
    boolean loadDataset(String dsName, String absolutePath);

    /**
     * Loads a VOTable file (<code>.vot</code>) with a given name.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call acts exactly like
     * {@link IScriptingInterface#loadDataset(String, String)}.<br/>
     * If <code>sync</code> is false, the loading happens
     * in a new thread and the call returns immediately. In this case, you can use {@link IScriptingInterface#hasDataset(String)}
     * to check whether the dataset is already loaded and available.
     * The actual loading process is carried out making educated guesses about semantics using UCDs and column names.
     * Please check <a href="http://gaia.ari.uni-heidelberg.de/gaiasky/docs/html/latest/SAMP.html#stil-data-provider">the
     * official documentation</a> for a complete reference on what can and what can't be loaded.
     *
     * @param dsName       The name of the dataset, used to identify the subsequent operations on the
     *                     dataset
     * @param absolutePath Absolute path to the <code>.vot</code> file to load
     * @param sync        Whether the load must happen synchronously or asynchronously
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or <code>sync</code> is false
     */
    boolean loadDataset(String dsName, String absolutePath, boolean sync);

    /**
     * Removes the dataset identified by the given name, if it exists
     *
     * @param dsName The name of the dataset to remove
     *
     * @return False if the dataset could not be found
     */
    boolean removeDataset(String dsName);

    /**
     * Hides the dataset identified by the given name, if it exists and is not hidden
     *
     * @param dsName The name of the dataset to hide
     *
     * @return False if the dataset could not be found
     */
    boolean hideDataset(String dsName);

    /**
     * Returns the names of all datasets currently loaded
     * @return A list with all the names of the loaded datasets
     */
    List<String> listDatasets();

    /**
     * Checks whether the dataset identified by the given name is loaded
     * @param dsName The name of the dataset to query
     * @return True if the dataset is loaded, false otherwise
     */
    boolean hasDataset(String dsName);

    /**
     * Shows (un-hides) the dataset identified by the given name, if it exists and is hidden
     *
     * @param dsName The name of the dataset to show
     *
     * @return False if the dataset could not be found
     */
    boolean showDataset(String dsName);

    /**
     * Enables or disables the dataset highlight, using a cyclic color which changes every call
     * @param dsName The dataset name
     * @param highlight State
     * @return False if the dataset could not be found
     */
    boolean highlightDataset(String dsName, boolean highlight);

    /**
     * Enables or disables the dataset highlight, using a given color index:
     * <ul>
     *     <li>0 - red</li>
     *     <li>1 - green</li>
     *     <li>2 - blue</li>
     *     <li>3 - cyan</li>
     *     <li>4 - magenta</li>
     *     <li>5 - yellow</li>
     * </ul>
     * @param dsName The dataset name
     * @param colorIndex Color index in [0..5]
     * @param highlight State
     * @return False if the dataset could not be found
     */
    boolean highlightDataset(String dsName, int colorIndex, boolean highlight);

    /**
     * Gets the current frame number. The number begins at 0 for the first frame produced
     * when Gaia Sky is started and increases continuously.
     * @return The current frame number
     */
    long getFrameNumber();

    /**
     * Rotates a 3D vector around the given axis by the specified angle in degrees.
     * Vectors are arrays with 3 components. If more components are there, they are ignored.
     *
     * @param vector Vector to rotate, with at least 3 components
     * @param axis   The axis, with at least 3 components
     * @param angle  Angle in degrees
     * @return The new vector, rotated
     */
    double[] rotate3(double[] vector, double[] axis, double angle);

    /**
     * Rotates a 2D vector by the specified angle in degrees, counter-clockwise assuming that
     * the y axis points up.
     *
     * @param vector Vector to rotate, with at least 2 components
     * @return The new vector, rotated
     */
    double[] rotate2(double[] vector, double angle);

    /**
     * Computes the cross product between the two 3D vectors.
     *
     * @param vec1 First 3D vector
     * @param vec2 Second 3D vector
     * @return Cross product 3D vector
     */
    double[] cross3(double[] vec1, double[] vec2);

    /**
     * Computes the dot product between the two 3D vectors.
     *
     * @param vec1 First 3D vector
     * @param vec2 Second 3D vector
     * @return The dot product scalar
     */
    double dot3(double[] vec1, double[] vec2);

    /**
     * Print text using the internal logging system
     *
     * @param message The message
     */
    void print(String message);

    /**
     * Print text using the internal logging system
     *
     * @param message The message
     */
    void log(String message);

    /**
     * Log an error using the internal logging system
     *
     * @param message The error message
     */
    void error(String message);

}
