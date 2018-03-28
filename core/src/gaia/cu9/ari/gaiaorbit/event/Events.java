package gaia.cu9.ari.gaiaorbit.event;

/**
 * Contains all the events
 * 
 * @author Toni Sagrista
 *
 */
public enum Events {
    /**
     * Event names
     */
    /** Notifies of a change in the time, contains the Date object **/
    TIME_CHANGE_INFO,
    /**
     * Issues a change time command, contains the Instant object with the new time
     **/
    TIME_CHANGE_CMD,

    GAIA_POSITION,

    /** Dataset has been choosen, loading can start **/
    LOAD_DATA_CMD,

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

    /**
     * Contains a boolean with the cinematic mode state (on/off) and a boolean
     * indicating if this comes from the interface
     **/
    CAMERA_CINEMATIC_CMD,

    /**
     * Contains the new camera rotation speed and a boolean indicating if this
     * comes from the interface
     **/
    ROTATION_SPEED_CMD,
    /** Contains the new turning speed **/
    TURNING_SPEED_CMD,
    /**
     * Contains the speed limit index as in: 
     * 0 - 100 km/h 
     * 1 - 0.5 * c
     * 2 - 0.8 * c
     * 3 - 0.9 * c
     * 4 - 0.99 * c
     * 5 - 0.99999 * c
     * 6 - c (3e8 m/s) 
     * 7 - 2*c
     * 8 - 10*c
     * 9 - 1000*c
     * 10 - 1 AU/s
     * 11 - 10 AU/s
     * 12 - 1000 AU/s
     * 13 - 10000 AU/s
     * 14 - 1 pc/s
     * 15 - 2 pc/s
     * 16 - 10 pc/s
     * 17 - 1000 pc/s
     * 18 - No limit
     * It also contains a boolean indicating whether this comes from
     * the interface.
     **/
    SPEED_LIMIT_CMD,
    /** Contains the value between 0 and 1 **/
    CAMERA_FWD,
    /** Contains the deltaX and deltaY between 0 and 1 **/
    CAMERA_ROTATE,
    /** Stops the camera motion **/
    CAMERA_STOP,

    /**
     * Informs that the camera has started or stopped playing. Contains a
     * boolean (true - start, false - stop)
     **/
    CAMERA_PLAY_INFO,

    CAMERA_PAN,
    /** Contains the roll value between 0 and 1 **/
    CAMERA_ROLL,
    /** Contains the deltaX and deltaY between 0 and 1 **/
    CAMERA_TURN,
    /** Removes the turn of the camera in focus mode **/
    CAMERA_CENTER,

    /**
     * Focus change command.
     * <ul>
     * <li>[0] - The new focus object OR its name.</li>
     * </ul>
     **/
    FOCUS_CHANGE_CMD,
    /**
     * Informs that the focus has somehow changed and the GUI must be updated.
     * <ul>
     * <li>[0] - The new focus object OR its name.</li>
     * </ul>
     **/
    FOCUS_CHANGED,
    /**
     * Contains the distance to the camera [0], the viewing angle [1], right
     * ascension in deg [2], declination in deg [3] and the distance to Sol [4]
     **/
    FOCUS_INFO_UPDATED,
    /** 
     * Posts the coordinates of the free mode focus.
     * <ul>
     * <li>[0] - ra  [deg]</li>
     * <li>[1] - dec [deg]</li>
     * </ul>
     */
    FREE_MODE_COORD_CMD,
    /**
     * Will show a popup menu for a focus candidate. Contains the candidate and
     * the screenX and screenY coordinates of the click
     **/
    POPUP_MENU_FOCUS,

    /** Contains two Double values, the longitude and latitude in degrees **/
    LON_LAT_UPDATED,

    /**
     * Updates the position of the pointer and the view in equatorial coordinates.
     * <ul>
     * <li>[0] - pointer ra  [deg]</li>
     * <li>[1] - pointer dec [deg]</li>
     * <li>[2] - view ra     [deg]</li>
     * <li>[3] - view dec    [deg]</li>
     * <li>[4] - pointer x   [pixels]</li>
     * <li>[5] - pointer y   [pixels]</li>
     * </ul>
     **/
    RA_DEC_UPDATED,

    /**
     * Contains a boolean with the display status
     */
    DISPLAY_POINTER_COORDS_CMD,

    /**
     * Issues the command to toggle the time. Contains the boolean indicating
     * the state (may be null) and a boolean indicating whether this comes from
     * the interface.
     **/
    TOGGLE_TIME_CMD,
    /**
     * Contains the name of the type, a boolean indicating if this comes from
     * the interface and an optional boolean with the state
     **/
    TOGGLE_VISIBILITY_CMD,
    /**
     * Contains the name, the boolean value, and an optional boolean indicating
     * if this comes from the interface
     **/
    FOCUS_LOCK_CMD,
    /**
     * Contains the name, the lock orientation boolean value and an optional
     * boolean indicating if this comes from the interface.
     */
    ORIENTATION_LOCK_CMD,
    /**
     * Contains the name, the boolean value and a boolean indicating if this
     * comes from the interface
     **/
    PROPER_MOTIONS_CMD,
    /** Contains a float with the intensity of the light between 0 and 1 **/
    AMBIENT_LIGHT_CMD,
    /** Contains the name of the check box and a boolean **/
    TOGGLE_AMBIENT_LIGHT,
    /**
     * Contains the name, the boolean value, and a boolean indicating if this
     * comes from the interface
     **/
    COMPUTE_GAIA_SCAN_CMD,
    /**
     * Contains the name, the boolean value, and a boolean indicating if this
     * comes from the interface
     **/
    TRANSIT_COLOUR_CMD,
    /**
     * Contains the name, the boolean value, and a boolean indicating if this
     * comes from the interface
     **/
    ONLY_OBSERVED_STARS_CMD,
    /**
     * Activate/deactivate lens flare. Contains a boolean with the new state and
     * a boolean for whether it comes from the interface
     **/
    LENS_FLARE_CMD,
    /**
     * Activate/deactivate the light scattering. Contains boolean with state and
     * a boolean for whether it comes from the interface
     **/
    LIGHT_SCATTERING_CMD,
    /** Fisheye effect toggle. Contains boolean **/
    FISHEYE_CMD,
    /**
     * Contains the intensity value between 0 and 1 and a boolean for whether it
     * comes from the interface
     **/
    BLOOM_CMD,
    /**
     * Contains the opacity of motion blur between 0 and 1 and a boolean for
     * whether it comes from the interface
     **/
    MOTION_BLUR_CMD,

    /**
     * Contains the brightness level (float) in [-1..1] and an optional boolean
     * indicating whether this comes from the interface
     **/
    BRIGHTNESS_CMD,

    /**
     * Contains the contrast level (float) in [0..2] and an optional boolean
     * indicating whether this comes from the interface
     **/
    CONTRAST_CMD,

    /** Contains a float with the pace **/
    PACE_CHANGE_CMD,
    /** Double the pace **/
    TIME_WARP_INCREASE_CMD,
    /** Divide the pace by 2 **/
    TIME_WARP_DECREASE_CMD,
    /** Contains the new pace **/
    PACE_CHANGED_INFO,
    /**
     * Sets and unsets the target time. Contains a time (set), or nothing
     * (unset)
     **/
    TARGET_TIME_CMD,
    /**
     * Issues the command to enable camera recording. Contains the boolean
     * indicating the state (may be null) and a boolean indicating whether this
     * comes from the interface.
     **/
    RECORD_CAMERA_CMD,

    /** Issues the play command. Contains the path to the file to play **/
    PLAY_CAMERA_CMD,

    /** Stops the current camera playing operation, if any **/
    STOP_CAMERA_PLAY,

    /**
     * Updates the camera recorder. Contains dt (float), position (vector3d),
     * direction (vector3d) and up (vector3d)
     **/
    UPDATE_CAM_RECORDER,

    /**
     * Issues the command to change the high accuracy setting. Contains a
     * boolean with the setting
     */
    HIGH_ACCURACY_CMD,

    /**
     * Issues the frame output command. Contains a boolean with the state.
     **/
    FRAME_OUTPUT_CMD,

    /**
     * Will be displayed in the notifications area (bottom left). Contains an
     * array of strings with the messages and an optional boolean indicating
     * whether the message is permanent so should stay until the next message is
     * received.
     **/
    POST_NOTIFICATION,
    /**
     * Contains a string with the headline message, will be displayed in a big
     * font in the center of the screen
     **/
    POST_HEADLINE_MESSAGE,
    /** Clears the headline message **/
    CLEAR_HEADLINE_MESSAGE,
    /**
     * Contains a string with the subhead message, will be displayed in a small
     * font below the headline message
     **/
    POST_SUBHEAD_MESSAGE,
    /** Clears the subhead message **/
    CLEAR_SUBHEAD_MESSAGE,
    /** Clears all messages in the message interface **/
    CLEAR_MESSAGES,
    /** Contains the new time frame object **/
    EVENT_TIME_FRAME_CMD,
    /**
     * Notifies a fov update in the camera. Contains the new fov value (float)
     * and the new fovFactor (float)
     **/
    FOV_CHANGE_NOTIFICATION,
    /**
     * Informs of a new camera state. Contains:
     * <ul>
     * <li>Vector3d with the current position of the camera</li>
     * <li>Double with the speed of the camera in km/s</li>
     * <li>Vector3d with the velocity vector of the camera</li>
     * <li>The PerspectiveCamera</li>
     * </ul>
     **/
    CAMERA_MOTION_UPDATED,

    /**
     * Activates/deactivates the crosshair in focus mode. Contains a boolean
     **/
    CROSSHAIR_CMD,
    /**
     * Contains an int with the number of lights and a float[] with [x, y] of
     * the 10 closest stars in screen coordinates in [0..1]
     **/
    LIGHT_POS_2D_UPDATED,
    /** Executes the command to position the camera near the object in focus **/
    GO_TO_OBJECT_CMD,
    /** Navigates smoothly to the given object **/
    NAVIGATE_TO_OBJECT,
    /** Lands on a planet object **/
    LAND_ON_OBJECT,
    /** Lands at a certain location on a planet object **/
    LAND_AT_LOCATION_OF_OBJECT,
    /**
     * Contains an optional boolean indicating whether debug info should be
     * shown or not. Otherwise, it toggles its state
     **/
    SHOW_DEBUG_CMD,
    SHOW_ABOUT_ACTION,
    SHOW_TUTORIAL_ACTION,
    SHOW_PREFERENCES_ACTION,
    SHOW_RUNSCRIPT_ACTION,
    SHOW_LAND_AT_LOCATION_ACTION,
    /** Shows the camera path file selector, contains the stage and the skin **/
    SHOW_PLAYCAMERA_ACTION,
    /** Informs about the number of running scripts **/
    NUM_RUNNING_SCRIPTS,
    /** Cancels the next script **/
    CANCEL_SCRIPT_CMD,
    SHOW_SEARCH_ACTION,
    /**
     * This event is issued when the screen has been resized. It contains the
     * new width and height
     **/
    SCREEN_RESIZE,
    /**
     * Issued when the viewport size changed. Contains the new width and height
     **/
    VIEWPORT_RESIZE,

    /**
     * Set a new value for the star point size and a boolean saying if it comes
     * from the interface
     **/
    STAR_POINT_SIZE_CMD,
    /**
     * Increase star point size by
     * {@link gaia.cu9.ari.gaiaorbit.util.Constants#STEP_STAR_POINT_SIZE}
     **/
    STAR_POINT_SIZE_INCREASE_CMD,
    /**
     * Decrease star point size by
     * {@link gaia.cu9.ari.gaiaorbit.util.Constants#STEP_STAR_POINT_SIZE}
     **/
    STAR_POINT_SIZE_DECREASE_CMD,
    /** Reset star point size to original value **/
    STAR_POINT_SIZE_RESET_CMD,

    /**
     * Minimum star opacity. Contains the opacity in [0..100] and a boolean
     * whether it comes from the interface
     **/
    STAR_MIN_OPACITY_CMD,

    /**
     * Stereoscopic vision, side by side rendering. Contains the state boolean
     * and another boolean indicating whether it comes from the interface.
     **/
    STEREOSCOPIC_CMD,
    /** Switches stereoscopic profile, contains the index of the new profile **/
    STEREO_PROFILE_CMD,
    /**
     * Sets the 360 mode. Contains a boolean with the new state and an optional
     * boolean indicating whether this comes from the interface
     **/
    CUBEMAP360_CMD,
    /**
     * Sets the resolution of the cubemap, contains an integer in [20..15000]
     * with the resolution.
     **/
    CUBEMAP_RESOLUTION_CMD,
    /**
     * Enables and disables the planetarium mode. Contains a boolean with the
     * state and another boolean indicating whether it comes from the interface.
     */
    PLANETARIUM_CMD,
    /** Anti aliasing changed, contains the new value for aa **/
    ANTIALIASING_CMD,
    /**
     * Toggles whole GUI display. Contains the localised name and an optional
     * boolean with the state (display/no display)
     **/
    DISPLAY_GUI_CMD,
    /**
     * Informs the UI theme has been reloaded. Contains the new skin.
     */
    UI_THEME_RELOAD_INFO,
    /**
     * Toggles the pause of the update process. Contains the localised name.
     **/
    TOGGLE_UPDATEPAUSE,
    /** Contains the new value **/
    UPDATEPAUSE_CHANGED,
    /** Graphics quality updated **/
    GRAPHICS_QUALITY_UPDATED,
    /** Toggles minimap visibility **/
    TOGGLE_MINIMAP,
    /**
     * Sets the vertical scroll position. Contains the scroll position in pixels
     **/
    GUI_SCROLL_POSITION_CMD,
    /**
     * Maximizes or minimizes the GUI window. Contains a boolean with the fold
     * state (true - minimize, false - maximize)
     **/
    GUI_FOLD_CMD,
    /**
     * Moves the GUI window.
     * <ol>
     * <li><strong>x</strong> - X coordinate of the top-left corner, float in
     * [0..1] from left to right.</li>
     * <li><strong>y</strong> - Y coordinate of top-left corner, float in [0..1]
     * from bottom to top.</li>
     * </ol>
     */
    GUI_MOVE_CMD,
    /**
     * Adds or modifies a custom message. Contains:
     * <ol>
     * <li><strong>id</strong> - integer</li>
     * <li><strong>message</strong> - string</li>
     * <li><strong>x</strong> - X position of bottom-left corner, float in
     * [0..1]</li>
     * <li><strong>y</strong> - Y position of bottom-left corner, float in
     * [0..1]</li>
     * <li><strong>r</strong> - float in [0..1]</li>
     * <li><strong>g</strong> - float in [0..1]</li>
     * <li><strong>b</strong> - float in [0..1]</li>
     * <li><strong>a</strong> - float in [0..1]</li>
     * <li><strong>size</strong> - float</li>
     * </ol>
     */
    ADD_CUSTOM_MESSAGE,
    /**
     * Adds or modifies a custom message. Contains:
     * <ol>
     * <li><strong>id</strong> - integer</li>
     * <li><strong>message</strong> - string</li>
     * <li><strong>x</strong> - X position of bottom-left corner, float in
     * [0..1]</li>
     * <li><strong>y</strong> - Y position of bottom-left corner, float in
     * [0..1]</li>
     * <li><strong>x</strong> - maxWidth maximum width in screen percentage,
     * float in [0..1]</li>
     * <li><strong>y</strong> - maxHeight maximum height in screen percentage,
     * float in [0..1]</li>
     * <li><strong>r</strong> - float in [0..1]</li>
     * <li><strong>g</strong> - float in [0..1]</li>
     * <li><strong>b</strong> - float in [0..1]</li>
     * <li><strong>a</strong> - float in [0..1]</li>
     * <li><strong>size</strong> - float</li>
     * </ol>
     */
    ADD_CUSTOM_TEXT,
    /**
     * Adds or modifies a custom image. Contains:
     * <ol>
     * <li><strong>id</strong> - integer</li>
     * <li><strong>tex</strong> - Texture</li>
     * <li><strong>x</strong> - X position of bottom-left corner, float in
     * [0..1]</li>
     * <li><strong>y</strong> - Y position of bottom-left corner, float in
     * [0..1]</li>
     * <li><strong>r</strong> - optional, float in [0..1]</li>
     * <li><strong>g</strong> - optional, float in [0..1]</li>
     * <li><strong>b</strong> - optional, float in [0..1]</li>
     * <li><strong>a</strong> - optional, float in [0..1]</li>
     * </ol>
     */
    ADD_CUSTOM_IMAGE,
    /** Removes a previously added message or image. Contains the id. **/
    REMOVE_OBJECTS,
    /** Removes all the custom objects **/
    REMOVE_ALL_OBJECTS,
    /**
     * Contains the star brightness multiplier and an optional boolean saying if
     * it comes from the interface
     **/
    STAR_BRIGHTNESS_CMD,
    /** Frames per second info **/
    FPS_INFO,
    /** Contains the number factor for pm vectors **/
    PM_NUM_FACTOR_CMD,
    /** Contains the length factor for pm vectors **/
    PM_LEN_FACTOR_CMD,
    /**
     * Updates the screen mode according to whats in the
     * {@link gaia.cu9.ari.gaiaorbit.util.GlobalConf#screen} bean.
     **/
    SCREEN_MODE_CMD,
    /** Informs the scene graph has been loaded. Program can start **/
    SCENE_GRAPH_LOADED,
    /**
     * Contains the width, height (integers) and the folder name and filename
     * (strings)
     **/
    SCREENSHOT_CMD,
    /** Contains the path where the screenshot has been saved */
    SCREENSHOT_INFO,
    /** Informs of the new size of the screenshot system **/
    SCREENSHOT_SIZE_UDPATE,
    /** Informs of the new size of the frame output system **/
    FRAME_SIZE_UDPATE,

    /** Issues the command to render a screenshot **/
    RENDER_SCREENSHOT,
    /** Issues the command to render a frame **/
    RENDER_FRAME,
    /**
     * Issues the command to render the current frame buffer with a given
     * folder, file (without filename), width and height
     **/
    RENDER_FRAME_BUFFER,

    /** Issues the command to flush the frame system **/
    FLUSH_FRAMES,
    /**
     * Reloads the controller mappings. Contains the path to the new mappings
     * file.
     **/
    RELOAD_CONTROLLER_MAPPINGS,

    /**
     * Contains an array of booleans with the visibility of each ComponentType,
     * in the same order returned by ComponentType.values()
     **/
    VISIBILITY_OF_COMPONENTS,
    /** Sets the limit magnitude. Contains a double with the new magnitude **/
    LIMIT_MAG_CMD,
    /** Debug info **/
    DEBUG1,
    DEBUG2,
    DEBUG3,
    DEBUG4,
    /**
     * Notifies from a java exception, it sends the Throwable and an optional
     * tag.
     **/
    JAVA_EXCEPTION,

    /**
     * Enables/disables input from mouse/keyboard/etc. Contains a boolean with
     * the new state
     **/
    INPUT_ENABLED_CMD,

    /**
     * Issued when an input event is received. It contains the key or button
     * integer code (see {@link com.badlogic.gdx.Input})
     **/
    INPUT_EVENT,

    /**
     * Sent when the properties in GlobalConf have been modified, usually after
     * a configuration dialog. Contains no data
     **/
    PROPERTIES_WRITTEN,

    /**
     * Contains the string with the script code and an optional boolean
     * indicating whether it must be run asynchronous
     **/
    RUN_SCRIPT_PATH,
    /**
     * Contains the script PyCode object, the path and an optional boolean
     * indicating whether it must be run asynchronous
     **/
    RUN_SCRIPT_PYCODE,

    /** Passes the OrbitData and the file name **/
    ORBIT_DATA_LOADED,

    /**
     * Configures the render system. Contains width, height, FPS, folder and
     * file
     **/
    CONFIG_FRAME_OUTPUT,
    /** Forces recalculation of main GUI window size **/
    RECALCULATE_OPTIONS_SIZE,

    /**
     * Issues command to chagne the galaxy appearance. Contains boolean, if true
     * gaiaxy will be 3D, if false galaxy will be 2D
     **/
    GALAXY_3D_CMD,

    /**
     * Contains the angle to use in the directionToTarget() function. Defaults
     * to 0
     **/
    PLANETARIUM_FOCUS_ANGLE_CMD,

    /** Contains the x and the y in pixels of the position of the mass **/
    GRAVITATIONAL_LENSING_PARAMS,

    /** Contains the Gaia object [0] **/
    GAIA_LOADED,
    /**
     * Issues the command to update the pixel render system. Contains no
     * parameters.
     **/
    PIXEL_RENDERER_UPDATE,
    /**
     * Issues the command to update the line render system. Contains no
     * parameters.
     **/
    LINE_RENDERER_UPDATE,
    /** Removes the keyboard focus in the GUI **/
    REMOVE_KEYBOARD_FOCUS,
    /** Removes the gui component identified by the given name **/
    REMOVE_GUI_COMPONENT,
    /** Adds the gui component identified by the given name **/
    ADD_GUI_COMPONENT,

    DISPLAY_MEM_INFO_WINDOW,

    /** Volume of music, contains the volume (float in [0..1]) **/
    MUSIC_VOLUME_CMD,
    /** Toggles the play **/
    MUSIC_PLAYPAUSE_CMD,
    /** Plays previous music **/
    MUSIC_PREVIOUS_CMD,
    /** Plays next music **/
    MUSIC_NEXT_CMD,
    /** Reload music files **/
    MUSIC_RELOAD_CMD,

    /** Contains the spacecraft object after it has been loaded **/
    SPACECRAFT_LOADED,
    /** Level spacecraft command, contains boolean with state **/
    SPACECRAFT_STABILISE_CMD,
    /** Stop spacecraft, contains boolean with state **/
    SPACECRAFT_STOP_CMD,

    /** Increases thrust **/
    SPACECRAFT_THRUST_INCREASE_CMD,
    /** Decreases thrust **/
    SPACECRAFT_THRUST_DECREASE_CMD,
    /** Contains the integer index of the new thrust **/
    SPACECRAFT_THRUST_SET_CMD,
    /** Broadcasts the new thrust index **/
    SPACECRAFT_THRUST_INFO,

    /** Positions spacecraft at camera **/
    SPACECRAFT_POS_CAM,
    /** Positons spacecraft behind camera, same dir and some vel **/
    SPACECRAFT_BACK_CAM,
    /** Positions spacecraft in fron to camera, opposite dir and some vel **/
    SPACECRAFT_FRONT_CAM,

    /**
     * Contains following info:
     * <ul>
     * <li>current speed [u/s]</li>
     * <li>current yaw angle [deg]</li>
     * <li>current pitch angle [deg]</li>
     * <li>current roll angle [deg]</li>
     * <li>thrust factor</li>
     * <li>engine power [-1..1]</li>
     * <li>yaw power [-1..1]</li>
     * <li>pitch power [-1..1]</li>
     * <li>roll power [-1..1]</li>
     * </ul>
     **/
    SPACECRAFT_INFO,

    /**
     * Contains following info about the nearest object:
     * <ul>
     * <li>nearest object name</li>
     * <li>distance to nearest object [u]</li>
     * </ul>
     */
    SPACECRAFT_NEAREST_INFO,

    /**
     * Event to update the shadow map metadata
     */
    REBUILD_SHADOW_MAP_DATA_CMD,

    /**
     * Toggles the fading of particles in the octree. Contains a boolean with
     * the state of the flag.
     **/
    OCTREE_PARTICLE_FADE_CMD,

    /**
     * Show uncertainties for Tycho star, if available. Contains the star
     */
    SHOW_UNCERTAINTIES,

    /**
     * Hides all uncertainties
     */
    HIDE_UNCERTAINTIES,

    /** Pauses background data loading thread, if any **/
    PAUSE_BACKGROUND_LOADING,

    /**
     * Resumes background data loading thread, if it exists and it is paused
     **/
    RESUME_BACKGROUND_LOADING,

    /** Empty event which informs that background loading is active **/
    BACKGROUND_LOADING_INFO,

    /** Update external GUIs signal. Contains the dt in seconds. **/
    UPDATE_GUI,

    /** Contains an the index of the mesh to be cleared **/
    DISPOSE_STAR_GROUP_GPU_MESH,

    /** Start gravitational wave. Contains on-screen position (x,y) of source as two integer parameters **/
    GRAV_WAVE_START,

    /** Stops gravitational wave **/
    GRAV_WAVE_STOP,

    /** Dispose all resources, app is shutting down **/
    DISPOSE;

}
