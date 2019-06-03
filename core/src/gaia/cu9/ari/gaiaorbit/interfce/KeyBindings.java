/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Input.Keys;
import com.bitfire.postprocessing.effects.CubemapProjections.CubemapProjection;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf.StereoProfile;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;

import java.time.Instant;
import java.util.*;

/**
 * Contains the key mappings and the actions. This should be persisted somehow
 * in the future.
 *
 * @author Toni Sagrista
 */
public class KeyBindings {
    private Map<TreeSet<Integer>, ProgramAction> mappings;

    public static KeyBindings instance;

    public static void initialize() {
        if (instance == null) {
            instance = new KeyBindings();
        }
    }

    /** CONTROL **/
    public static int SPECIAL1;
    /** SHIFT **/
    public static int SPECIAL2;
    /** ALT **/
    public static int SPECIAL3;

    /**
     * Creates a key mappings instance.
     */
    private KeyBindings() {
        mappings = new HashMap<>();
        // Init special keys
        SPECIAL1 = Keys.CONTROL_LEFT;
        SPECIAL2 = Keys.SHIFT_LEFT;
        SPECIAL3 = Keys.ALT_LEFT;
        // For now this will do
        initDefault();
    }

    public Map<TreeSet<Integer>, ProgramAction> getMappings() {
        return mappings;
    }

    Map<TreeSet<Integer>, ProgramAction> getSortedMappings() {
        return GlobalResources.sortByValue(mappings);
    }

    private void addMapping(ProgramAction action, int... keyCodes) {
        TreeSet<Integer> keys = new TreeSet<>();
        for (int key : keyCodes) {
            keys.add(key);
        }
        mappings.put(keys, action);
    }

    /**
     * Finds an action given its name
     * @param name The name
     * @return The action if it exists
     */
    public ProgramAction findAction(String name){
        Collection<ProgramAction> actions = mappings.values();
        for(ProgramAction action : actions){
            if(action.actionName.equals(name))
                return action;
        }
        return null;
    }

    /**
     * Gets the keys that trigger the action identified by the given name
     * @param actionName The action name
     * @return The keys
     */
    public TreeSet<Integer> getKeys(String actionName){
        ProgramAction action = findAction(actionName);
        if(action != null){
            Set<Map.Entry<TreeSet<Integer>, ProgramAction>> entries = mappings.entrySet();
            for(Map.Entry<TreeSet<Integer>, ProgramAction> entry : entries){
                if(entry.getValue().equals(action)){
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public String getStringKeys(String actionName){
        TreeSet<Integer> keys = getKeys(actionName);
        StringBuilder sb = new StringBuilder();
        Iterator<Integer> it = keys.descendingIterator();
        while(it.hasNext()){
            sb.append(Keys.toString(it.next()));
            if(it.hasNext())
                sb.append("+");
        }
        return sb.toString();
    }

    /**
     * Initializes the default keyboard mappings. In the future these should be
     * read from a configuration file.
     */
    private void initDefault() {

        // Condition which checks the current GUI is the FullGui
        BooleanRunnable fullGuiCondition = () -> GuiRegistry.current instanceof FullGui;

        // Show about
        final Runnable runnableAbout = () -> EventManager.instance.post(Events.SHOW_ABOUT_ACTION);

        // F1 -> Help dialog
        addMapping(new ProgramAction(I18n.txt("action.help"), runnableAbout), Keys.F1);

        // h -> Help dialog
        addMapping(new ProgramAction(I18n.txt("action.help"), runnableAbout), Keys.H);

        // Show quit
        final Runnable runnableQuit = () -> EventManager.instance.post(Events.SHOW_QUIT_ACTION);

        // ESCAPE -> Exit
        addMapping(new ProgramAction(I18n.txt("action.exit"), runnableQuit), Keys.ESCAPE);

        // q -> Exit
        addMapping(new ProgramAction(I18n.txt("action.exit"), runnableQuit), Keys.Q);

        // p -> Show preferences dialog
        addMapping(new ProgramAction(I18n.txt("action.preferences"), () ->
                EventManager.instance.post(Events.SHOW_PREFERENCES_ACTION)), Keys.P);

        // c -> Show play camera dialog
        addMapping(new ProgramAction(I18n.txt("action.playcamera"), () ->
                EventManager.instance.post(Events.SHOW_PLAYCAMERA_ACTION), fullGuiCondition), Keys.C);

        // SHIFT+O -> Toggle orbits
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.orbits")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.orbits", false)), SPECIAL2, Keys.O);

        // SHIFT+P -> Toggle planets
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.planets")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.planets", false)), SPECIAL2, Keys.P);

        // SHIFT+M -> Toggle moons
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.moons")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.moons", false)), SPECIAL2, Keys.M);

        // SHIFT+S -> Toggle stars
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.stars")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.stars", false)), SPECIAL2, Keys.S);

        // SHIFT+T -> Toggle satellites
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.satellites")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.satellites", false)), SPECIAL2, Keys.T);

        // SHIFT+A -> Toggle asteroids
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.asteroids")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.asteroids", false)), SPECIAL2, Keys.A);

        // SHIFT+L -> Toggle labels
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.labels")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.labels", false)), SPECIAL2, Keys.L);

        // SHIFT+C -> Toggle constellations
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.constellations")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.constellations", false)), SPECIAL2, Keys.C);

        // SHIFT+B -> Toggle boundaries
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.boundaries")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.boundaries", false)), SPECIAL2, Keys.B);

        // SHIFT+Q -> Toggle equatorial
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.equatorial")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.equatorial", false)), SPECIAL2, Keys.Q);

        // SHIFT+E -> Toggle ecliptic
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.ecliptic")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.ecliptic", false)), SPECIAL2, Keys.E);

        // SHIFT+G -> Toggle galactic
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.galactic")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.galactic", false)), SPECIAL2, Keys.G);

        //SHIFT+H -> Toggle meshes
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.meshes")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.meshes", false)), SPECIAL2, Keys.H);

        //SHIFT+V -> Toggle clusters
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.clusters")), () ->
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.clusters", false)), SPECIAL2, Keys.V);

        // Left bracket -> divide speed
        addMapping(new ProgramAction(I18n.txt("action.dividetime"), () ->
                EventManager.instance.post(Events.TIME_WARP_DECREASE_CMD)), Keys.COMMA);

        // Right bracket -> double speed
        addMapping(new ProgramAction(I18n.txt("action.doubletime"), () ->
                EventManager.instance.post(Events.TIME_WARP_INCREASE_CMD)), Keys.PERIOD);

        // SPACE -> toggle time
        addMapping(new ProgramAction(I18n.txt("action.pauseresume"), () ->
                EventManager.instance.post(Events.TOGGLE_TIME_CMD, null, false)), Keys.SPACE);

        // Plus -> increase limit magnitude
        addMapping(new ProgramAction(I18n.txt("action.incmag"), () ->
                EventManager.instance.post(Events.LIMIT_MAG_CMD, GlobalConf.runtime.LIMIT_MAG_RUNTIME + 0.1f)), Keys.PLUS);

        // Minus -> decrease limit magnitude
        addMapping(new ProgramAction(I18n.txt("action.decmag"), () ->
                EventManager.instance.post(Events.LIMIT_MAG_CMD, GlobalConf.runtime.LIMIT_MAG_RUNTIME - 0.1f)), Keys.MINUS);

        // Star -> reset limit mag
        addMapping(new ProgramAction(I18n.txt("action.resetmag"), () ->
                EventManager.instance.post(Events.LIMIT_MAG_CMD, GlobalConf.data.LIMIT_MAG_LOAD)), Keys.STAR);

        // F11 -> fullscreen
        addMapping(new ProgramAction(I18n.txt("action.togglefs"), () -> {
            GlobalConf.screen.FULLSCREEN = !GlobalConf.screen.FULLSCREEN;
            EventManager.instance.post(Events.SCREEN_MODE_CMD);
        }), Keys.F11);

        // F4 -> toggle fisheye effect
        addMapping(new ProgramAction(I18n.txt("action.fisheye"), () ->
                EventManager.instance.post(Events.FISHEYE_CMD, !GlobalConf.postprocess.POSTPROCESS_FISHEYE)), Keys.F4);

        // F5 -> take screenshot
        addMapping(new ProgramAction(I18n.txt("action.screenshot"), () ->
                EventManager.instance.post(Events.SCREENSHOT_CMD, GlobalConf.screenshot.SCREENSHOT_WIDTH, GlobalConf.screenshot.SCREENSHOT_HEIGHT, GlobalConf.screenshot.SCREENSHOT_FOLDER)), Keys.F5);

        // F6 -> toggle frame output
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.frameoutput")), () ->
                EventManager.instance.post(Events.FRAME_OUTPUT_CMD, !GlobalConf.frame.RENDER_OUTPUT)), Keys.F6);

        // U -> toggle UI collapse/expand
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.controls")), () ->
                EventManager.instance.post(Events.GUI_FOLD_CMD), fullGuiCondition), Keys.U);

        // CTRL+K -> toggle cubemap mode
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.360")), () ->
                EventManager.instance.post(Events.CUBEMAP360_CMD, !GlobalConf.program.CUBEMAP360_MODE, false)), SPECIAL1, Keys.K);

        // CTRL+SHIFT+K -> toggle cubemap projection
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.projection")), () -> {
            int newprojidx = (GlobalConf.program.CUBEMAP_PROJECTION.ordinal() + 1) % CubemapProjection.values().length;
            EventManager.instance.post(Events.CUBEMAP_PROJECTION_CMD, CubemapProjection.values()[newprojidx]);
        }), SPECIAL1, SPECIAL2, Keys.K);

        // CTRL + SHIFT + UP -> increase star point size by 0.5
        addMapping(new ProgramAction(I18n.txt("action.starpointsize.inc"), () ->
                EventManager.instance.post(Events.STAR_POINT_SIZE_INCREASE_CMD)), SPECIAL1, SPECIAL2, Keys.UP);

        // CTRL + SHIFT + DOWN -> decrease star point size by 0.5
        addMapping(new ProgramAction(I18n.txt("action.starpointsize.dec"), () ->
                EventManager.instance.post(Events.STAR_POINT_SIZE_DECREASE_CMD)), SPECIAL1, SPECIAL2, Keys.DOWN);

        // CTRL + SHIFT + R -> reset star point size
        addMapping(new ProgramAction(I18n.txt("action.starpointsize.reset"), () ->
                EventManager.instance.post(Events.STAR_POINT_SIZE_RESET_CMD)), SPECIAL1, SPECIAL2, Keys.R);

        // CTRL + W -> new keyframe
        addMapping(new ProgramAction(I18n.txt("action.keyframe"), () ->
                EventManager.instance.post(Events.KEYFRAME_ADD)), SPECIAL1, Keys.W);

        // Camera modes (NUMBERS)
        for (int i = 7; i <= 16; i++) {
            // Camera mode
            int m = i - 7;
            final CameraMode mode = CameraMode.getMode(m);
            if (mode != null) {
                addMapping(new ProgramAction(mode.name(), () ->
                        EventManager.instance.post(Events.CAMERA_MODE_CMD, mode)), i);
            }
        }

        // Camera modes (NUM_KEYPAD)
        for (int i = 144; i <= 153; i++) {
            // Camera mode
            int m = i - 144;
            final CameraMode mode = CameraMode.getMode(m);
            if (mode != null) {
                addMapping(new ProgramAction(mode.name(), () ->
                        EventManager.instance.post(Events.CAMERA_MODE_CMD, mode)), i);
            }
        }

        // CTRL + D -> Toggle debug information
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.debugmode")), () -> {
            EventManager.instance.post(Events.SHOW_DEBUG_CMD);
        }), SPECIAL1, Keys.D);

        // CTRL + F -> Search dialog
        final Runnable runnableSearch = () -> {
            EventManager.instance.post(Events.SHOW_SEARCH_ACTION);
        };

        addMapping(new ProgramAction(I18n.txt("action.search"), runnableSearch, fullGuiCondition), SPECIAL1, Keys.F);

        // f -> Search dialog
        addMapping(new ProgramAction(I18n.txt("action.search"), runnableSearch, fullGuiCondition), Keys.F);

        // / -> Search dialog
        addMapping(new ProgramAction(I18n.txt("action.search"), runnableSearch, fullGuiCondition), Keys.SLASH);

        // CTRL + SHIFT + O -> Toggle particle fade
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.octreeparticlefade")), () ->
                EventManager.instance.post(Events.OCTREE_PARTICLE_FADE_CMD, I18n.txt("element.octreeparticlefade"), !GlobalConf.scene.OCTREE_PARTICLE_FADE)), SPECIAL1, SPECIAL2, Keys.O);

        // CTRL + S -> Toggle stereoscopic mode
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.stereomode")), () ->
                EventManager.instance.post(Events.STEREOSCOPIC_CMD, !GlobalConf.program.STEREOSCOPIC_MODE, false)), SPECIAL1, Keys.S);

        // CTRL + SHIFT + S -> Switch stereoscopic profile
        addMapping(new ProgramAction(I18n.txt("action.switchstereoprofile"), () -> {
            int newidx = GlobalConf.program.STEREO_PROFILE.ordinal();
            newidx = (newidx + 1) % StereoProfile.values().length;
            EventManager.instance.post(Events.STEREO_PROFILE_CMD, newidx);
        }), SPECIAL1, SPECIAL2, Keys.S);

        // CTRL + P -> Toggle planetarium mode
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.planetarium")), () ->
                EventManager.instance.post(Events.PLANETARIUM_CMD, !GlobalConf.postprocess.POSTPROCESS_FISHEYE, false)), SPECIAL1, Keys.P);

        // CTRL + U -> Toggle clean (no GUI) mode
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.cleanmode")), () ->
                EventManager.instance.post(Events.DISPLAY_GUI_CMD, I18n.txt("notif.cleanmode"))), SPECIAL1, Keys.U);

        // CTRL + G -> Travel to focus object
        addMapping(new ProgramAction(I18n.txt("action.gotoobject"), () ->
                EventManager.instance.post(Events.GO_TO_OBJECT_CMD)), SPECIAL1, Keys.G);

        // CTRL + R -> Reset time to current system time
        addMapping(new ProgramAction(I18n.txt("action.resettime"), () ->
                EventManager.instance.post(Events.TIME_CHANGE_CMD, Instant.now())), SPECIAL1, Keys.R);

        // CTRL + SHIFT + G -> Galaxy 2D - 3D
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("element.galaxy3d")), () ->
                EventManager.instance.post(Events.GALAXY_3D_CMD, !GlobalConf.scene.GALAXY_3D)), SPECIAL1, SPECIAL2, Keys.G);

        // HOME -> Back home
        addMapping(new ProgramAction(I18n.txt("action.home"), () -> {
            EventManager.instance.post(Events.HOME_CMD);
        }), Keys.HOME);

        // TAB -> Minimap toggle
        addMapping(new ProgramAction(I18n.txt("action.toggle", I18n.txt("gui.minimap.title")), () ->
                EventManager.instance.post(Events.TOGGLE_MINIMAP)), Keys.TAB);


        // ALT_L + T -> Expand/collapse time pane
        addMapping(new ProgramAction(I18n.txt("action.expandcollapse.pane", I18n.txt("gui.time")), () ->
                EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "TimeComponent")), SPECIAL3, Keys.T);

        // ALT_L + C -> Expand/collapse camera pane
        addMapping(new ProgramAction(I18n.txt("action.expandcollapse.pane", I18n.txt("gui.camera")), () ->
            EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "CameraComponent")), SPECIAL3, Keys.C);

        // ALT_L + V -> Expand/collapse visibility pane
        addMapping(new ProgramAction(I18n.txt("action.expandcollapse.pane", I18n.txt("gui.visibility")), () ->
                EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "VisibilityComponent")), SPECIAL3, Keys.V);

        // ALT_L + L -> Expand/collapse visual effects pane
        addMapping(new ProgramAction(I18n.txt("action.expandcollapse.pane", I18n.txt("gui.lighting")), () ->
                EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "VisualEffectsComponent")), SPECIAL3, Keys.L);

        // ALT_L + D -> Expand/collapse datasets pane
        addMapping(new ProgramAction(I18n.txt("action.expandcollapse.pane", I18n.txt("gui.dataset.title")), () ->
                EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "DatasetsComponent")), SPECIAL3, Keys.D);

        // ALT_L + O -> Expand/collapse objects pane
        addMapping(new ProgramAction(I18n.txt("action.expandcollapse.pane", I18n.txt("gui.objects")), () ->
                EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "ObjectsComponent")), SPECIAL3, Keys.O);

        // ALT_L + M -> Expand/collapse music pane
        addMapping(new ProgramAction(I18n.txt("action.expandcollapse.pane", I18n.txt("gui.music")), () ->
                EventManager.instance.post(Events.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, "MusicComponent")), SPECIAL3, Keys.M);
    }

    /**
     * A simple program action. It can optionally contain a condition which must
     * evaluate to true for the action to be run.
     *
     * @author Toni Sagrista
     */
    public class ProgramAction implements Runnable, Comparable<ProgramAction> {
        final String actionName;
        /**
         * Action to run
         **/
        private final Runnable action;

        /**
         * Condition that must be met
         **/
        private final BooleanRunnable condition;

        ProgramAction(String actionName, Runnable action, BooleanRunnable condition) {
            this.actionName = actionName;
            this.action = action;
            this.condition = condition;
        }

        ProgramAction(String actionName, Runnable action) {
            this(actionName, action, null);
        }

        @Override
        public void run() {
            // Run if condition not set or condition is met
            if (condition != null) {
                if (condition.run())
                    action.run();
            } else {
                action.run();
            }
        }

        @Override
        public int compareTo(ProgramAction other) {
            return actionName.toLowerCase().compareTo(other.actionName.toLowerCase());
        }

    }

    public interface BooleanRunnable {
        boolean run();
    }

}
