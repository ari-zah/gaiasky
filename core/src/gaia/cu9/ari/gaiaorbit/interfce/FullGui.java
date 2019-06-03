/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaia.cu9.ari.gaiaorbit.desktop.util.MemInfoWindow;
import gaia.cu9.ari.gaiaorbit.desktop.util.RunCameraWindow;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.update.VersionCheckEvent;
import gaia.cu9.ari.gaiaorbit.util.update.VersionChecker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Full OpenGL GUI with all the controls and whistles.
 *
 * @author Toni Sagrista
 */
public class FullGui extends AbstractGui {
    private static final Log logger = Logger.getLogger(FullGui.class);

    protected ControlsWindow controlsWindow;

    protected Container<FocusInfoInterface> fi;
    protected FocusInfoInterface focusInterface;
    protected NotificationsInterface notificationsInterface;
    protected MessagesInterface messagesInterface;
    protected CustomInterface customInterface;
    protected RunStateInterface runStateInterface;

    protected SearchDialog searchDialog;
    protected RunCameraWindow runcameraWindow;
    protected MemInfoWindow memInfoWindow;
    protected LogWindow logWindow;

    protected INumberFormat nf;
    protected Label pointerXCoord, pointerYCoord;

    protected ISceneGraph sg;
    private ComponentType[] visibilityEntities;
    private boolean[] visible;

    private List<Actor> invisibleInStereoMode;

    public FullGui() {
        super();
    }

    @Override
    public void initialize(AssetManager assetManager) {
        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        logger.info(I18n.txt("notif.gui.init"));

        skin = GlobalResources.skin;
        interfaces = new Array<>();

        buildGui();

        // We must subscribe to the desired events
        EventManager.instance.subscribe(this, Events.FOV_CHANGED_CMD, Events.SHOW_TUTORIAL_ACTION, Events.SHOW_SEARCH_ACTION, Events.SHOW_PLAYCAMERA_ACTION, Events.DISPLAY_MEM_INFO_WINDOW, Events.REMOVE_KEYBOARD_FOCUS, Events.REMOVE_GUI_COMPONENT, Events.ADD_GUI_COMPONENT, Events.SHOW_LOG_ACTION, Events.RA_DEC_UPDATED, Events.LON_LAT_UPDATED, Events.POPUP_MENU_FOCUS, Events.SHOW_LAND_AT_LOCATION_ACTION, Events.DISPLAY_POINTER_COORDS_CMD, Events.TOGGLE_MINIMAP);
    }

    protected void buildGui() {
        // Component types name init
        for (ComponentType ct : ComponentType.values()) {
            ct.getName();
        }

        // CONTROLS WINDOW
        addControlsWindow();

        nf = NumberFormatFactory.getFormatter("##0.##");

        // FOCUS INFORMATION - BOTTOM RIGHT
        focusInterface = new FocusInfoInterface(skin);
        // focusInterface.setFillParent(true);
        focusInterface.left().top();
        fi = new Container<>(focusInterface);
        fi.setFillParent(true);
        fi.bottom().right();
        fi.pad(0, 0, 10, 10);

        // NOTIFICATIONS INTERFACE - BOTTOM LEFT
        notificationsInterface = new NotificationsInterface(skin, lock, true, true, true, true);
        notificationsInterface.setFillParent(true);
        notificationsInterface.left().bottom();
        notificationsInterface.pad(0, 5, 5, 0);
        interfaces.add(notificationsInterface);

        // MESSAGES INTERFACE - LOW CENTER
        messagesInterface = new MessagesInterface(skin, lock);
        messagesInterface.setFillParent(true);
        messagesInterface.left().bottom();
        messagesInterface.pad(0, 300, 150, 0);
        interfaces.add(messagesInterface);

        // INPUT STATE
        runStateInterface = new RunStateInterface(skin, true);
        runStateInterface.setFillParent(true);
        runStateInterface.center().bottom();
        //runStateInterface.pad(GlobalConf.SCALE_FACTOR == 1 ? 135 : 200, 0, 0, 5);
        runStateInterface.pad(0, 0, 5, 0);
        interfaces.add(runStateInterface);

        // CUSTOM OBJECTS INTERFACE
        customInterface = new CustomInterface(ui, skin, lock);
        interfaces.add(customInterface);

        // MOUSE X/Y COORDINATES
        pointerXCoord = new OwnLabel("", skin, "default");
        pointerXCoord.setAlignment(Align.bottom);
        pointerXCoord.setVisible(GlobalConf.program.DISPLAY_POINTER_COORDS);
        pointerYCoord = new OwnLabel("", skin, "default");
        pointerYCoord.setAlignment(Align.right | Align.center);
        pointerYCoord.setVisible(GlobalConf.program.DISPLAY_POINTER_COORDS);

        /** ADD TO UI **/
        rebuildGui();

        // INVISIBLE IN STEREOSCOPIC MODE
        invisibleInStereoMode = new ArrayList<>();
        invisibleInStereoMode.add(controlsWindow);
        invisibleInStereoMode.add(fi);
        invisibleInStereoMode.add(messagesInterface);
        invisibleInStereoMode.add(runStateInterface);
        // invisibleInStereoMode.add(customInterface);
        invisibleInStereoMode.add(pointerXCoord);
        invisibleInStereoMode.add(pointerYCoord);

        /** VERSION CHECK **/
        if (GlobalConf.program.VERSION_LAST_TIME == null || Instant.now().toEpochMilli() - GlobalConf.program.VERSION_LAST_TIME.toEpochMilli() > GlobalConf.ProgramConf.VERSION_CHECK_INTERVAL_MS) {
            // Start version check
            VersionChecker vc = new VersionChecker(GlobalConf.program.VERSION_CHECK_URL);
            vc.setListener(event -> {
                if (event instanceof VersionCheckEvent) {
                    VersionCheckEvent vce = (VersionCheckEvent) event;
                    if (!vce.isFailed()) {
                        // Check version
                        String tagVersion = vce.getTag();
                        Integer versionNumber = vce.getVersionNumber();
                        Instant tagDate = vce.getTagTime();

                        GlobalConf.program.VERSION_LAST_TIME = Instant.now();

                        if (versionNumber > GlobalConf.version.versionNumber) {
                            logger.info(I18n.txt("gui.newversion.available", GlobalConf.version.version, tagVersion));
                            // There's a new version!
                            UpdatePopup newVersion = new UpdatePopup(tagVersion, ui, skin);
                            newVersion.pack();
                            float ww = newVersion.getWidth();
                            float margin = 5 * GlobalConf.SCALE_FACTOR;
                            newVersion.setPosition(Gdx.graphics.getWidth() - ww - margin, margin);
                            ui.addActor(newVersion);
                        } else {
                            // No new version
                            logger.info(I18n.txt("gui.newversion.nonew", GlobalConf.program.getLastCheckedString()));
                        }

                    } else {
                        // Handle failed case
                        // Do nothing
                        logger.info(I18n.txt("gui.newversion.fail"));
                    }
                }
                return false;
            });

            // Start in 10 seconds
            Thread vct = new Thread(vc);
            Timer.Task t = new Timer.Task() {
                @Override
                public void run() {
                    logger.info(I18n.txt("gui.newversion.checking"));
                    vct.start();
                }
            };
            Timer.schedule(t, 10);
        }

    }

    public void recalculateOptionsSize() {
        controlsWindow.recalculateSize();
    }

    protected void rebuildGui() {

        if (ui != null) {
            ui.clear();
            boolean collapsed;
            if (controlsWindow != null) {
                collapsed = controlsWindow.isCollapsed();
                recalculateOptionsSize();
                if (collapsed)
                    controlsWindow.collapseInstant();
                controlsWindow.setPosition(0, Gdx.graphics.getHeight() - controlsWindow.getHeight());
                ui.addActor(controlsWindow);
            }
            if (notificationsInterface != null)
                ui.addActor(notificationsInterface);
            if (messagesInterface != null)
                ui.addActor(messagesInterface);
            if (focusInterface != null && !GlobalConf.runtime.STRIPPED_FOV_MODE)
                ui.addActor(fi);
            if (runStateInterface != null) {
                ui.addActor(runStateInterface);
            }
            if (pointerXCoord != null && pointerYCoord != null) {
                ui.addActor(pointerXCoord);
                ui.addActor(pointerYCoord);
            }

            if (customInterface != null) {
                customInterface.reAddObjects();
            }

            /** CAPTURE SCROLL FOCUS **/
            ui.addListener(new EventListener() {

                @Override
                public boolean handle(Event event) {
                    if (event instanceof InputEvent) {
                        InputEvent ie = (InputEvent) event;

                        if (ie.getType() == Type.mouseMoved) {
                            Actor scrollPanelAncestor = getScrollPanelAncestor(ie.getTarget());
                            ui.setScrollFocus(scrollPanelAncestor);
                        } else if (ie.getType() == Type.touchDown) {
                            if (ie.getTarget() instanceof TextField)
                                ui.setKeyboardFocus(ie.getTarget());
                        }
                    }
                    return false;
                }

                private Actor getScrollPanelAncestor(Actor actor) {
                    if (actor == null) {
                        return null;
                    } else if (actor instanceof ScrollPane) {
                        return actor;
                    } else {
                        return getScrollPanelAncestor(actor.getParent());
                    }
                }

            });

            /** KEYBOARD FOCUS **/
            ui.addListener((event) -> {
                if (event instanceof InputEvent) {
                    InputEvent ie = (InputEvent) event;
                    if(ie.getType() == Type.touchDown && !ie.isHandled()){
                        ui.setKeyboardFocus(null);
                    }
                }
                return false;
            });
        }
    }

    /**
     * Removes the focus from this Gui and returns true if the focus was in the
     * GUI, false otherwise.
     *
     * @return true if the focus was in the GUI, false otherwise.
     */
    public boolean cancelTouchFocus() {
        if (ui.getScrollFocus() != null) {
            ui.setScrollFocus(null);
            ui.setKeyboardFocus(null);
            return true;
        }
        return false;
    }

    @Override
    public void update(double dt) {
        ui.act((float) dt);
        notificationsInterface.update();
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case SHOW_TUTORIAL_ACTION:
            EventManager.instance.post(Events.RUN_SCRIPT_PATH, GlobalConf.program.TUTORIAL_SCRIPT_LOCATION);
            break;
        case SHOW_SEARCH_ACTION:
            if (searchDialog == null) {
                searchDialog = new SearchDialog(skin, ui, sg);
            } else {
                searchDialog.clearText();
            }
            searchDialog.show(ui);
            break;
        case SHOW_LAND_AT_LOCATION_ACTION:
            CelestialBody target = (CelestialBody) data[0];
            LandAtWindow landAtLocation = new LandAtWindow(target, ui, skin);
            landAtLocation.show(ui);
            break;
        case SHOW_PLAYCAMERA_ACTION:
            if (runcameraWindow != null)
                runcameraWindow.remove();

            runcameraWindow = new RunCameraWindow(ui, skin);
            runcameraWindow.show(ui);
            break;
        case DISPLAY_MEM_INFO_WINDOW:
            if (memInfoWindow == null) {
                memInfoWindow = new MemInfoWindow(ui, skin);
            }
            memInfoWindow.show(ui);
            break;
        case SHOW_LOG_ACTION:
            if (logWindow == null) {
                logWindow = new LogWindow(ui, skin);
            }
            logWindow.update();
            logWindow.show(ui);
            break;
        case REMOVE_KEYBOARD_FOCUS:
            ui.setKeyboardFocus(null);
            break;
        case REMOVE_GUI_COMPONENT:
            String name = (String) data[0];
            String method = "remove" + TextUtils.capitalise(name);
            try {
                Method m = ClassReflection.getMethod(this.getClass(), method);
                m.invoke(this);
            } catch (ReflectionException e) {
                logger.error(e);
            }
            rebuildGui();
            break;
        case ADD_GUI_COMPONENT:
            name = (String) data[0];
            method = "add" + TextUtils.capitalise(name);
            try {
                Method m = ClassReflection.getMethod(this.getClass(), method);
                m.invoke(this);
            } catch (ReflectionException e) {
                logger.error(e);
            }
            rebuildGui();
            break;
        case RA_DEC_UPDATED:
            if (GlobalConf.program.DISPLAY_POINTER_COORDS) {
                Double ra = (Double) data[0];
                Double dec = (Double) data[1];
                Integer x = (Integer) data[4];
                Integer y = (Integer) data[5];

                pointerXCoord.setText("RA/".concat(nf.format(ra)).concat("°"));
                pointerXCoord.setPosition(x, GlobalConf.SCALE_FACTOR);
                pointerYCoord.setText("DEC/".concat(nf.format(dec)).concat("°"));
                pointerYCoord.setPosition(Gdx.graphics.getWidth() + GlobalConf.SCALE_FACTOR, Gdx.graphics.getHeight() - y);
            }
            break;
        case LON_LAT_UPDATED:
            if (GlobalConf.program.DISPLAY_POINTER_COORDS) {
                Double lon = (Double) data[0];
                Double lat = (Double) data[1];
                Integer x = (Integer) data[2];
                Integer y = (Integer) data[3];

                pointerXCoord.setText("Lon/".concat(nf.format(lon)).concat("°"));
                pointerXCoord.setPosition(x, GlobalConf.SCALE_FACTOR);
                pointerYCoord.setText("Lat/".concat(nf.format(lat)).concat("°"));
                pointerYCoord.setPosition(Gdx.graphics.getWidth() + GlobalConf.SCALE_FACTOR, Gdx.graphics.getHeight() - y);
            }
            break;
        case DISPLAY_POINTER_COORDS_CMD:
            Boolean display = (Boolean) data[0];
            pointerXCoord.setVisible(display);
            pointerYCoord.setVisible(display);
            break;
        case POPUP_MENU_FOCUS:
            final IFocus candidate = (IFocus) data[0];
            int screenX = Gdx.input.getX();
            int screenY = Gdx.input.getY();

            GaiaSkyContextMenu popup = new GaiaSkyContextMenu(skin, "default", screenX, screenY, candidate);

            int h = Gdx.graphics.getHeight();

            float px = screenX;
            float py = h - screenY - 20 * GlobalConf.SCALE_FACTOR;

            popup.showMenu(ui, px, py);

            break;
        default:
            break;
        }

    }

    @Override
    public void setSceneGraph(ISceneGraph sg) {
        this.sg = sg;
    }

    @Override
    public void setVisibilityToggles(ComponentType[] entities, ComponentTypes visible) {
        this.visibilityEntities = entities;
        ComponentType[] vals = ComponentType.values();
        this.visible = new boolean[vals.length];
        for (int i = 0; i < vals.length; i++)
            this.visible[i] = visible.get(vals[i].ordinal());
    }

    public void removeControlsWindow() {
        if (controlsWindow != null) {
            controlsWindow.remove();
            controlsWindow = null;
        }
    }

    public void addControlsWindow() {
        controlsWindow = new ControlsWindow(I18n.txt("gui.controlpanel"), skin, ui);
        controlsWindow.setSceneGraph(sg);
        controlsWindow.setVisibilityToggles(visibilityEntities, visible);
        controlsWindow.initialize();
        controlsWindow.left();
        controlsWindow.getTitleTable().align(Align.left);
        controlsWindow.setFillParent(false);
        controlsWindow.setMovable(true);
        controlsWindow.setResizable(false);
        controlsWindow.padRight(5);
        controlsWindow.padBottom(5);

        controlsWindow.collapseInstant();
    }

}
