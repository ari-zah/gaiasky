package gaia.cu9.ari.gaiaorbit.event;

/**
 * Contains all the events
 * @author Toni Sagrista
 *
 */
public enum Events {
    /**
     * Event names
     */
    /** Notifies of a change in the time, contains the Date object **/
    TIME_CHANGE_INFO,
    /** Issues a change time command, contains the Date object with the new time **/
    TIME_CHANGE_CMD, GAIA_POSITION,

    // CAMERA
    /** Contains the new CameraMode object **/
    CAMERA_MODE_CMD,
    /** Contains a double[] with the new position **/
    CAMERA_POS_CMD,
    /** Contains a double[] with the new direction **/
    CAMERA_DIR_CMD,
    /** Contains a double[] with the new up vector **/
    CAMERA_UP_CMD,
    /** Contains the a float with the new fov value **/
    FOV_CHANGED_CMD,
    /** Contains the new camera speed **/
    CAMERA_SPEED_CMD,
    /** Contains the new camera rotation speed and a boolean indicating if this comes from the interface **/
    ROTATION_SPEED_CMD,
    /** Contains the new turning speed **/
    TURNING_SPEED_CMD,
    /**
     * Contains the speed limit index as in:
     *  0 - 100 km/h
     *  1 - c (3e8 m/s)
     *  2 - 2*c
     *  3 - 10*c
     *  4 - 1000*c
     *  5 - 1 pc/s
     *  6 - 2 pc/s
     *  7 - 10 pc/s
     *  8 - 1000 pc/s
     *  9 - No limit
     *  **/
    SPEED_LIMIT_CMD,
    /** Contains the value between 0 and 1 **/
    CAMERA_FWD,
    /** Contains the deltaX and deltaY between 0 and 1 **/
    CAMERA_ROTATE,
    /** Stops the camera motion **/
    CAMERA_STOP,

    CAMERA_PAN,
    /** Contains the roll value between 0 and 1 **/
    CAMERA_ROLL,
    /** Contains the deltaX and deltaY between 0 and 1 **/
    CAMERA_TURN,
    /** Removes the turn of the camera in focus mode **/
    CAMERA_CENTER,

    /** Focus change command.
     * <ul><li>
     * [0] - The new focus object OR its name.
     * </li></ul>
     * **/
    FOCUS_CHANGE_CMD,
    /** Informs that the focus has somehow changed and the GUI must be updated.
     * <ul><li>
     * [0] - The new focus object OR its name.
     * </li></ul>
     * **/
    FOCUS_CHANGED,
    /** Contains the distance [0] and the viewing angle [1] **/
    FOCUS_INFO_UPDATED,

    /** Issues the command to toggle the time. Contains the boolean indicating the state (may be null) and a boolean indicating whether this comes from the interface. **/
    TOGGLE_TIME_CMD,
    /** Contains the name of the type, a boolean indicating if this comes from the interface and an optional boolean with the state **/
    TOGGLE_VISIBILITY_CMD,
    /** Contains the name, the boolean value, and a boolean indicating if this comes from the interface **/
    FOCUS_LOCK_CMD,
    /** Contains the name, the boolean value and a boolean indicating if this comes from the interface**/
    PROPER_MOTIONS_CMD,
    /** Contains a float with the intensity of the light between 0 and 1 **/
    AMBIENT_LIGHT_CMD,
    /** Contains the name of the check box and a boolean **/
    TOGGLE_AMBIENT_LIGHT,
    /** Contains the name, the boolean value, and a boolean indicating if this comes from the interface **/
    COMPUTE_GAIA_SCAN_CMD,
    /** Contains the name, the boolean value, and a boolean indicating if this comes from the interface **/
    TRANSIT_COLOUR_CMD,
    /** Contains the name, the boolean value, and a boolean indicating if this comes from the interface **/
    ONLY_OBSERVED_STARS_CMD,
    /** Activate/deactivate lens flare. Contains a boolean with the new state **/
    LENS_FLARE_CMD,
    /** Contains the intensity value between 0 and 1 **/
    BLOOM_CMD,
    /** Contains the opacity of motion blur between 0 and 1 **/
    MOTION_BLUR_CMD,
    /** Contains a float with the pace **/
    PACE_CHANGE_CMD,
    /** Double the pace **/
    PACE_DOUBLE_CMD,
    /** Divide the pace by 2 **/
    PACE_DIVIDE_CMD,
    /** Contains the new pace **/
    PACE_CHANGED_INFO,
    /** Issues the command to enable camera recording. Contains the boolean indicating the state (may be null) and a boolean indicating whether this comes from the interface. **/
    RECORD_CAMERA_CMD,

    /** Issues the play command. Contains the path to the file to play **/
    PLAY_CAMERA_CMD,

    /** Updates the camera recorder. Contains dt (float), position (vector3d), direction (vector3d) and up (vector3d)**/
    UPDATE_CAM_RECORDER,

    /** Issues the frame output command. Contains an optional boolean with the state. Otherwise, the state is toggled **/
    FRAME_OUTPUT_CMD,

    /**
     * Will be displayed in the notifications area (bottom left). Contains an array of strings with the messages and an optional boolean
     * indicating whether the message is permanent so should stay until the next message is received.
     **/
    POST_NOTIFICATION,
    /** Contains a string with the headline message, will be displayed in a big font in the center of the screen **/
    POST_HEADLINE_MESSAGE,
    /** Clears the headline message **/
    CLEAR_HEADLINE_MESSAGE,
    /** Contains a string with the subhead message, will be displayed in a small font below the headline message **/
    POST_SUBHEAD_MESSAGE,
    /** Clears the subhead message **/
    CLEAR_SUBHEAD_MESSAGE,
    /** Clears all messages in the message interface **/
    CLEAR_MESSAGES,
    /** Contains the new time frame object **/
    EVENT_TIME_FRAME_CMD,
    /** Notifies a fov update in the camera **/
    FOV_CHANGE_NOTIFICATION,
    /** Contains a Vector3d with the position and a double with the velocity [km/h] **/
    CAMERA_MOTION_UPDATED,
    /** Executes the command to position the camera near the object in focus **/
    GO_TO_OBJECT_CMD,
    /** Contains an optional boolean indicating whether debug info should be shown or not. Otherwise, it toggles its state **/
    SHOW_DEBUG_CMD, SHOW_ABOUT_ACTION, SHOW_TUTORIAL_ACTION, SHOW_PREFERENCES_ACTION, SHOW_RUNSCRIPT_ACTION, SHOW_PLAYCAMERA_ACTION,
    /** Informs about the number of running scripts **/
    NUM_RUNNING_SCRIPTS,
    /** Cancels the next script **/
    CANCEL_SCRIPT_CMD, SHOW_SEARCH_ACTION,
    /** This event is issued when the screen has been resized. It contains the new width and height **/
    SCREEN_RESIZE,
    /** Issued when the viewport size changed. Contains the new width and height **/
    VIEWPORT_RESIZE,

    /** Stereoscopic vision, side by side rendering. Contains the localized name. **/
    TOGGLE_STEREOSCOPIC,
    /** Switches stereoscopic mode images, left to right and right to left **/
    TOGGLE_STEREO_PROFILE,
    /** Toggles no GUI mode. Contains the localized name. **/
    TOGGLE_CLEANMODE,
    /** Toggles the pause of the update process. Contains the localized name. **/
    TOGGLE_UPDATEPAUSE,
    /** Contains the new value **/
    UPDATEPAUSE_CHANGED,
    /** Sets the vertical scroll position. Contains the scroll position in pixels **/
    GUI_SCROLL_POSITION_CMD,
    /** Maximizes or minimizes the GUI window. Contains a boolean with the fold state (true - minimize, false - maximize) **/
    GUI_FOLD_CMD,
    /** Moves the GUI window.
     * <ol><li>
     * <strong>x</strong> - X coordinate of the top-left corner, float in [0..1] from left to right.
     * </li><li>
     * <strong>y</strong> - Y coordinate of top-left corner, float in [0..1] from bottom to top.
     * </li></ol>
     */
    GUI_MOVE_CMD,
    /** Adds or modifies a custom message. Contains:
     * <ol><li>
     * <strong>id</strong> - integer
     * </li><li>
     * <strong>message</strong> - string
     * </li><li>
     * <strong>x</strong> - X position of bottom-left corner, float in [0..1]
     * </li><li>
     * <strong>y</strong> - Y position of bottom-left corner, float in [0..1]
     * </li><li>
     * <strong>r</strong> - float in [0..1]
     * </li><li>
     * <strong>g</strong> - float in [0..1]
     * </li><li>
     * <strong>b</strong> - float in [0..1]
     * </li><li>
     * <strong>a</strong> - float in [0..1]
     * </li><li>
     * <strong>size</strong> - float
     * </li></ol>
     */
    ADD_CUSTOM_MESSAGE,
    /** Adds or modifies a custom message. Contains:
     * <ol><li>
     * <strong>id</strong> - integer
     * </li><li>
     * <strong>message</strong> - string
     * </li><li>
     * <strong>x</strong> - X position of bottom-left corner, float in [0..1]
     * </li><li>
     * <strong>y</strong> - Y position of bottom-left corner, float in [0..1]
     * </li><li>
     * <strong>x</strong> - maxWidth maximum width in screen percentage, float in [0..1]
     * </li><li>
     * <strong>y</strong> - maxHeight maximum height in screen percentage, float in [0..1]
     * </li><li>
     * <strong>r</strong> - float in [0..1]
     * </li><li>
     * <strong>g</strong> - float in [0..1]
     * </li><li>
     * <strong>b</strong> - float in [0..1]
     * </li><li>
     * <strong>a</strong> - float in [0..1]
     * </li><li>
     * <strong>size</strong> - float
     * </li></ol>
     */
    ADD_CUSTOM_TEXT,
    /** Adds or modifies a custom image. Contains:
     * <ol><li>
     * <strong>id</strong> - integer
     * </li><li>
     * <strong>tex</strong> - Texture
     * </li><li>
     * <strong>x</strong> - X position of bottom-left corner, float in [0..1]
     * </li><li>
     * <strong>y</strong> - Y position of bottom-left corner, float in [0..1]
     * </li><li>
     * <strong>r</strong> - optional, float in [0..1]
     * </li><li>
     * <strong>g</strong> - optional, float in [0..1]
     * </li><li>
     * <strong>b</strong> - optional, float in [0..1]
     * </li><li>
     * <strong>a</strong> - optional, float in [0..1]
     * </li></ol>
     */
    ADD_CUSTOM_IMAGE,
    /** Removes a previously added message or image. Contains the id. **/
    REMOVE_OBJECTS,
    /** Removes all the custom objects **/
    REMOVE_ALL_OBJECTS,
    /** Contains the star brightness multiplier **/
    STAR_BRIGHTNESS_CMD, FPS_INFO,
    /** Contains an optional boolean indicating whether full screen must be activated (true) or deactivated (false). If no
     * boolean is attached, it functions as a toggle. **/
    FULLSCREEN_CMD, SCENE_GRAPH_LOADED,
    /** Contains the width, height (integers) and the folder name and filename (strings) **/
    SCREENSHOT_CMD,
    /** Contains the path where the screenshot has been saved */
    SCREENSHOT_INFO,

    /** Issues the command to render a screenshot **/
    RENDER_SCREENSHOT,
    /** Issues the command to render a frame **/
    RENDER_FRAME,
    /** Issues the command to flush the frame system **/
    FLUSH_FRAMES,
    /** Contains an array of booleans with the visibility of each ComponentType, in the same order returned by ComponentType.values() **/
    VISIBILITY_OF_COMPONENTS,
    /** Sets the limit magnitude. Contains a double with the new magnitude **/
    LIMIT_MAG_CMD, DEBUG1, DEBUG2, DEBUG3,
    /** Notifies from a java exception, it sends the Throwable and an optional tag. **/
    JAVA_EXCEPTION,

    /** Enables/disables input from mouse/keyboard/etc. Contains a boolean with the new state **/
    INPUT_ENABLED_CMD,

    /** Issued when an input event is received. It contains the key or button integer code (see {@link com.badlogic.gdx.Input}) **/
    INPUT_EVENT,

    /** Sent when the properties in GlobalConf have been modified, usually after a configuration dialog. Contains no data **/
    PROPERTIES_WRITTEN,

    /** Contains the string with the script code and an optional boolean indicating whether it must be run asynchronous **/
    RUN_SCRIPT_PATH,
    /** Contains the script PyCode object, the path and an optional boolean indicating whether it must be run asynchronous  **/
    RUN_SCRIPT_PYCODE,

    /** Passes the OrbitData and the file name **/
    ORBIT_DATA_LOADED,

    /** Contains the index of the new renderer **/
    PIXEL_RENDERER_CMD,
    /** Configures the render system. Contains width, height, FPS, folder and file **/
    CONFIG_PIXEL_RENDERER,
    /** Forces recalculation of main GUI window size **/
    RECALCULATE_OPTIONS_SIZE,

    /** Contains the x and the y in pixels of the position of the mass **/
    GRAVITATIONAL_LENSING_PARAMS,

    /** Contains the Gaia object [0] **/
    GAIA_LOADED,
    /** Issues the command to update the pixel render system. Contains no parameters. **/
    PIXEL_RENDERER_UPDATE,
    /** Removes the keyboard focus in the GUI **/
    REMOVE_KEYBOARD_FOCUS,
    /** Removes the gui component identified by the given name **/
    REMOVE_GUI_COMPONENT,
    /** Adds the gui component identified by the given name **/
    ADD_GUI_COMPONENT,

    /** Toggles the fading of particles in the octree. Contains a boolean with the state of the flag. **/
    OCTREE_PARTICLE_FADE_CMD,

    /** Update external GUIs signal. Contains the dt in seconds. **/
    UPDATE_GUI,

    /** Dispose all resources, app is shutting down **/
    DISPOSE;

}
