package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.StubModel;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.NaturalCamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.comp.ViewAngleComparator;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.vr.VRContext.VRControllerAxes;
import gaia.cu9.ari.gaiaorbit.vr.VRContext.VRControllerButtons;
import gaia.cu9.ari.gaiaorbit.vr.VRContext.VRDevice;
import gaia.cu9.ari.gaiaorbit.vr.VRDeviceListener;

import java.util.*;

public class OpenVRListener implements VRDeviceListener {
    private static final Log logger = Logger.getLogger(OpenVRListener.class);
    
    /** The natural camera **/
    private NaturalCamera cam;
    /** Focus comparator **/
    private Comparator<IFocus> comp;
    /** Map from VR device to model object **/
    private HashMap<VRDevice, StubModel> vrDeviceToModel;
    /** Aux vectors **/
    private Vector3d p0, p1;

    private boolean vrControllerHint = false;
    private boolean vrInfoGui = false;
    private long lastDoublePress = 0l;

    private Set<Integer> pressedButtons;

    public OpenVRListener(NaturalCamera cam) {
        this.cam = cam;
        this.comp = new ViewAngleComparator<IFocus>();
        this.p0 = new Vector3d();
        this.p1 = new Vector3d();
        pressedButtons = new HashSet<Integer>();
    }

    private void lazyInit() {
        if (vrDeviceToModel == null)
            vrDeviceToModel = GaiaSky.instance.getVRDeviceToModel();
    }

    public void connected(VRDevice device) {
        logger.info(device + " connected");
        EventManager.instance.post(Events.VR_DEVICE_CONNECTED, device);
    }

    public void disconnected(VRDevice device) {
        logger.info(device + " disconnected");
        EventManager.instance.post(Events.VR_DEVICE_DISCONNECTED, device);
    }

    /**
     * True if only the given button is pressed
     * @param button
     * @return
     */
    private boolean isPressed(int button) {
        return pressedButtons.contains(button);
    }

    /**
     * Returns true if all given buttons are pressed
     * @param buttons
     * @return
     */
    private boolean arePressed(int... buttons) {
        boolean result = true;
        for (int i = 0; i < buttons.length; i++)
            result = result && pressedButtons.contains(buttons[i]);
        return result;
    }

    public void buttonPressed(VRDevice device, int button) {
        if (GlobalConf.controls.DEBUG_MODE) {
            logger.info("vr button down [device/code]: " + device.toString() + " / " + button);
        }
        lazyInit();
        // Add to pressed
        pressedButtons.add(button);

        // VR controller hint
        if (arePressed(VRControllerButtons.A, VRControllerButtons.B)) {
            EventManager.instance.post(Events.DISPLAY_VR_CONTROLLER_HINT_CMD, true);
            vrControllerHint = true;
        }
    }

    public void buttonReleased(VRDevice device, int button) {
        if (GlobalConf.controls.DEBUG_MODE) {
            logger.info("vr button released [device/code]: " + device.toString() + " / " + button);
        }

        // Removed from pressed
        pressedButtons.remove(button);

        if (TimeUtils.millis() - lastDoublePress > 250) {
            // Give some time to recover from double press
            lazyInit();
            if (button == VRControllerButtons.SteamVR_Trigger) {
                // Selection
                StubModel sm = vrDeviceToModel.get(device);
                if (sm != null) {
                    p0.set(sm.getBeamP0());
                    p1.set(sm.getBeamP1());
                    IFocus hit = getBestHit(p0, p1);
                    if (hit != null) {
                        EventManager.instance.post(Events.FOCUS_CHANGE_CMD, hit);
                        EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.Focus);
                    }
                } else {
                    logger.info("Model corresponding to device not found");
                }
            }

            if (vrControllerHint && !arePressed(VRControllerButtons.A, VRControllerButtons.B)) {
                EventManager.instance.post(Events.DISPLAY_VR_CONTROLLER_HINT_CMD, false);
                vrControllerHint = false;
                lastDoublePress = TimeUtils.millis();
            } else if (button == VRControllerButtons.B) {
                vrInfoGui = !vrInfoGui;
                EventManager.instance.post(Events.DISPLAY_VR_GUI_CMD, vrInfoGui);
            } else if (button == VRControllerButtons.A) {
                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, "element.labels", false);
            } else if (button == VRControllerButtons.SteamVR_Touchpad) {
                // Change mode from free to focus and viceversa
                CameraMode cm = cam.getMode().equals(CameraMode.Focus) ? CameraMode.Free_Camera : CameraMode.Focus;
                // Stop
                cam.clearVelocityVR();
                EventManager.instance.post(Events.CAMERA_MODE_CMD, cm);
            }
        }
    }

    private Array<IFocus> getHits(Vector3d p0, Vector3d p1) {
        Array<IFocus> l = GaiaSky.instance.getFocusableEntities();

        Array<IFocus> hits = new Array<IFocus>();

        Iterator<IFocus> it = l.iterator();
        // Add all hits
        while (it.hasNext()) {
            IFocus s = it.next();
            s.addHit(p0, p1, cam, hits);
        }

        return hits;
    }

    private IFocus getBestHit(Vector3d p0, Vector3d p1) {
        Array<IFocus> hits = getHits(p0, p1);
        if (hits.size != 0) {
            // Sort using distance
            hits.sort(comp);
            // Get closest
            return hits.get(hits.size - 1);
        }
        return null;
    }

    @Override
    public void event(int code) {
        if (GlobalConf.controls.DEBUG_MODE) {
            logger.info("Unhandled event: " + code);
        }
    }

    @Override
    public void buttonTouched(VRDevice device, int button) {
        if (GlobalConf.controls.DEBUG_MODE) {
            logger.info("vr button touched [device/code]: " + device.toString() + " / " + button);
        }
    }

    @Override
    public void buttonUntouched(VRDevice device, int button) {
        if (GlobalConf.controls.DEBUG_MODE) {
            logger.info("vr button untouched [device/code]: " + device.toString() + " / " + button);
        }
    }

    @Override
    public void axisMoved(VRDevice device, int axis, float valueX, float valueY) {
        if (GlobalConf.controls.DEBUG_MODE) {
            logger.info("axis moved: [device/axis/x/y]: " + device.toString() + " / " + axis + " / " + valueX + " / " + valueY);
        }
        lazyInit();

        StubModel sm;
        switch (axis) {
        case VRControllerAxes.Axis1:
            // Forward
            //StubModel sm = vrDeviceToModel.get(device);
            //if (sm != null) {
            //    // Direct direction
            //   cam.setVelocityVR(sm.getBeamP0(), sm.getBeamP1(), valueX);
            //}
            break;
        case VRControllerAxes.Axis2:
            // Backward
            //sm = vrDeviceToModel.get(device);
            //if (sm != null) {
            //    // Invert direction
            //    cam.setVelocityVR(sm.getBeamP0(), sm.getBeamP1(), -valueX);
            //}
            break;
        case VRControllerAxes.Axis0:
            // Joystick for forward/backward movement
            sm = vrDeviceToModel.get(device);
            if (sm != null) {
                cam.setVelocityVR(sm.getBeamP0(), sm.getBeamP1(), valueY);
            }

            break;
        }

    }
}
