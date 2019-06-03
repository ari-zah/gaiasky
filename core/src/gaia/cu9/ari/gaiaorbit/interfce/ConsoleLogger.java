/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.data.util.PointCloudData;
import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormat;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf.StereoProfile;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

/**
 * Widget that captures and displays messages in a GUI.
 *
 * @author Toni Sagrista
 */
public class ConsoleLogger implements IObserver {
    private static final long DEFAULT_TIMEOUT = 5000;
    private static final String TAG_SEPARATOR = " - ";
    IDateFormat df;
    long msTimeout;
    boolean writeDates;
    boolean useHistorical;

    public ConsoleLogger() {
        this(true, true);
    }

    /**
     * Initializes the notifications interface.
     *
     * @param writeDates    Log the date and time.
     * @param useHistorical Keep logs.
     */
    public ConsoleLogger(boolean writeDates, boolean useHistorical) {
        this.msTimeout = DEFAULT_TIMEOUT;
        this.useHistorical = useHistorical;
        this.writeDates = writeDates;

        try {
            this.df = DateFormatFactory.getFormatter("uuuu-MM-dd HH:mm:ss");
        } catch (Exception e) {
            this.df = new DesktopDateFormat(I18n.locale, true, true);
        }
        EventManager.instance.subscribe(this, Events.POST_NOTIFICATION, Events.FOCUS_CHANGED, Events.TOGGLE_TIME_CMD, Events.TOGGLE_VISIBILITY_CMD, Events.CAMERA_MODE_CMD, Events.PACE_CHANGED_INFO, Events.FOCUS_LOCK_CMD, Events.TOGGLE_AMBIENT_LIGHT, Events.FOV_CHANGE_NOTIFICATION, Events.JAVA_EXCEPTION, Events.ORBIT_DATA_LOADED, Events.SCREENSHOT_INFO, Events.COMPUTE_GAIA_SCAN_CMD, Events.ONLY_OBSERVED_STARS_CMD, Events.TRANSIT_COLOUR_CMD, Events.LIMIT_MAG_CMD, Events.STEREOSCOPIC_CMD, Events.DISPLAY_GUI_CMD, Events.FRAME_OUTPUT_CMD, Events.STEREO_PROFILE_CMD, Events.OCTREE_PARTICLE_FADE_CMD);
    }

    public void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    private void addMessage(String msg) {
        Instant date = Instant.now();
        log(df.format(date), msg, false);
        if (useHistorical) {
            NotificationsInterface.historical.add(new MessageBean(date, msg));
        }
    }

    private void addMessage(String msg, boolean debug) {
        Instant date = Instant.now();
        log(df.format(date), msg, debug);
        if (useHistorical) {
            NotificationsInterface.historical.add(new MessageBean(date, msg));
        }
    }

    private void log(String date, String msg, boolean debug) {
        if (Gdx.app != null) {
            if (debug) {
                if (writeDates)
                    Gdx.app.debug(date, msg);
                else
                    Gdx.app.debug(this.getClass().getSimpleName(), msg);
            } else {
                if (writeDates)
                    Gdx.app.log(date, msg);
                else
                    Gdx.app.log(this.getClass().getSimpleName(), msg);
            }
        } else {
            if (writeDates)
                System.out.println("[" + date + "] " + msg);
            else
                System.out.println(msg);
        }
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case POST_NOTIFICATION:
            String message = "";
            for (int i = 0; i < data.length; i++) {
                if (i == data.length - 1 && data[i] instanceof Boolean) {
                } else {
                    message += data[i].toString();
                    if (i < data.length - 1 && !(i == data.length - 2 && data[data.length - 1] instanceof Boolean)) {
                        message += TAG_SEPARATOR;
                    }
                }
            }
            addMessage(message);
            break;
        case FOCUS_CHANGED:
            if (data[0] != null) {
                IFocus sgn = null;
                if (data[0] instanceof String) {
                    sgn = GaiaSky.instance.sg.findFocus((String) data[0]);
                } else {
                    sgn = (IFocus) data[0];
                }
                addMessage(I18n.bundle.format("notif.camerafocus", sgn.getName()));
            }
            break;
        case TOGGLE_TIME_CMD:
            Boolean bool = (Boolean) data[0];
            if (bool == null) {
                addMessage(I18n.bundle.format("notif.toggle", I18n.bundle.format("gui.time")));
            } else {
                addMessage(I18n.bundle.format("notif.simulation." + (bool ? "resume" : "pause")));
            }
            break;
        case TOGGLE_VISIBILITY_CMD:
            if (data.length == 3)
                addMessage(I18n.bundle.format("notif.visibility." + (((Boolean) data[2]) ? "on" : "off"), I18n.bundle.get((String) data[0])));
            else
                addMessage(I18n.bundle.format("notif.visibility.toggle", I18n.bundle.get((String) data[0])));
            break;
        case FOCUS_LOCK_CMD:
        case ORIENTATION_LOCK_CMD:
        case TOGGLE_AMBIENT_LIGHT:
        case COMPUTE_GAIA_SCAN_CMD:
        case ONLY_OBSERVED_STARS_CMD:
        case TRANSIT_COLOUR_CMD:
        case OCTREE_PARTICLE_FADE_CMD:
            addMessage(data[0] + (((Boolean) data[1]) ? " on" : " off"));
            break;
        case CAMERA_MODE_CMD:
            CameraMode cm = (CameraMode) data[0];
            if (cm != CameraMode.Focus)
                addMessage(I18n.bundle.format("notif.cameramode.change", (CameraMode) data[0]));
            break;
        case PACE_CHANGED_INFO:
            addMessage(I18n.bundle.format("notif.timepace.change", data[0]));
            break;
        case LIMIT_MAG_CMD:
            addMessage(I18n.bundle.format("notif.limitmag", data[0]));
            break;
        case FOV_CHANGE_NOTIFICATION:
            // addMessage("Field of view changed to " + (float) data[0]);
            break;
        case JAVA_EXCEPTION:
            Throwable t = (Throwable) data[0];
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            String stackTrace = sw.toString();
            if (data.length == 1) {
                if(I18n.bundle != null)
                    addMessage(I18n.bundle.format("notif.error", stackTrace));
                else
                    addMessage("Error: " + stackTrace);
            } else {
                if(I18n.bundle != null)
                addMessage(I18n.bundle.format("notif.error", data[1] + TAG_SEPARATOR + stackTrace));
                else
                    addMessage("Error: " + data[1] + TAG_SEPARATOR + stackTrace);
            }
            break;
        case ORBIT_DATA_LOADED:
            addMessage(I18n.bundle.format("notif.orbitdata.loaded", data[1], ((PointCloudData) data[0]).getNumPoints()), true);
            break;
        case SCREENSHOT_INFO:
            addMessage(I18n.bundle.format("notif.screenshot", data[0]));
            break;
        case STEREOSCOPIC_CMD:
            addMessage(I18n.bundle.format("notif.toggle", I18n.bundle.get("notif.stereoscopic")));
            break;
        case DISPLAY_GUI_CMD:
            addMessage(I18n.bundle.format("notif.toggle", data[0]));
            break;
        case STEREO_PROFILE_CMD:
            addMessage(I18n.bundle.format("notif.stereoscopic.profile", StereoProfile.values()[(Integer) data[0]].toString()));
            break;
        case FRAME_OUTPUT_CMD:
            boolean activated = (Boolean) data[0];
            if (activated) {
                addMessage(I18n.bundle.format("notif.activated", I18n.bundle.get("element.frameoutput")));
            } else {
                addMessage(I18n.bundle.format("notif.deactivated", I18n.bundle.get("element.frameoutput")));
            }
            break;
        default:
            break;
        }
    }

    public void dispose() {
        unsubscribe();
    }

}
