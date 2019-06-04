package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.IGui;
import gaia.cu9.ari.gaiaorbit.interfce.VRControllerInfoGui;
import gaia.cu9.ari.gaiaorbit.interfce.VRGui;
import gaia.cu9.ari.gaiaorbit.interfce.VRInfoGui;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.PostProcessBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.StubModel;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.NaturalCamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.vr.VRContext;
import gaia.cu9.ari.gaiaorbit.vr.VRContext.Space;
import gaia.cu9.ari.gaiaorbit.vr.VRContext.VRDevice;
import gaia.cu9.ari.gaiaorbit.vr.VRContext.VRDeviceType;
import org.lwjgl.openvr.*;

import java.util.Map;

/**
 * Renders to OpenVR. Renders basically two scenes, one for each eye, using the
 * OpenVR context.
 *
 * @author tsagrista
 */
public class SGROpenVR extends SGRAbstract implements ISGR, IObserver {

    private VRContext vrContext;

    /**
     * Frame buffers for each eye
     **/
    FrameBuffer fbLeft, fbRight;
    /**
     * Textures
     **/
    Texture texLeft, texRight;

    HmdMatrix44 projectionMat = HmdMatrix44.create();
    HmdMatrix34 eyeMat = HmdMatrix34.create();

    public final Matrix4 eyeSpace = new Matrix4();
    public final Matrix4 invEyeSpace = new Matrix4();

    private IntModelBatch modelBatch;
    public Array<StubModel> controllerObjects;
    private Map<VRDevice, StubModel> vrDeviceToModel;
    private Environment controllersEnv;

    private SpriteBatch sb;

    // Focus info
    private VRGui<VRInfoGui> infoGui;
    private VRGui<VRControllerInfoGui> controllerHintGui;

    private Vector3 auxf1;
    private Vector3d auxd1;

    public SGROpenVR(VRContext vrContext, IntModelBatch modelBatch) {
        super();
        // VR Context
        this.vrContext = vrContext;
        // Model batch
        this.modelBatch = modelBatch;
        // Sprite batch for screen rendering
        this.sb = new SpriteBatch();

        if (vrContext != null) {
            // Left eye, fb and texture
            fbLeft = new FrameBuffer(Format.RGBA8888, vrContext.getWidth(), vrContext.getHeight(), true);
            texLeft = org.lwjgl.openvr.Texture.create();
            texLeft.set(fbLeft.getColorBufferTexture().getTextureObjectHandle(), VR.ETextureType_TextureType_OpenGL, VR.EColorSpace_ColorSpace_Gamma);

            // Right eye, fb and texture
            fbRight = new FrameBuffer(Format.RGBA8888, vrContext.getWidth(), vrContext.getHeight(), true);
            texRight = org.lwjgl.openvr.Texture.create();
            texRight.set(fbRight.getColorBufferTexture().getTextureObjectHandle(), VR.ETextureType_TextureType_OpenGL, VR.EColorSpace_ColorSpace_Gamma);

            // Aux vectors
            auxf1 = new Vector3();
            auxd1 = new Vector3d();

            // Controllers
            Array<VRDevice> controllers = vrContext.getDevicesByType(VRDeviceType.Controller);

            // Env
            controllersEnv = new Environment();
            controllersEnv.set(new ColorAttribute(ColorAttribute.AmbientLight, .2f, .2f, .2f, 1f));
            DirectionalLight dlight = new DirectionalLight();
            dlight.color.set(1f, 1f, 1f, 1f);
            dlight.direction.set(0, -1, 0);
            controllersEnv.add(dlight);

            // Controller objects
            vrDeviceToModel = GaiaSky.instance.getVRDeviceToModel();
            controllerObjects = new Array<>(controllers.size);
            for (VRDevice controller : controllers) {
                addVRController(controller);
            }

            infoGui = new VRGui(VRInfoGui.class, (int) (GlobalConf.screen.SCREEN_WIDTH / 10f));
            infoGui.initialize(null);

            controllerHintGui = new VRGui(VRControllerInfoGui.class, (int) (-GlobalConf.screen.SCREEN_WIDTH / 10f));
            controllerHintGui.initialize(null);

            EventManager.instance.subscribe(this, Events.FRAME_SIZE_UDPATE, Events.SCREENSHOT_SIZE_UDPATE, Events.VR_DEVICE_CONNECTED, Events.VR_DEVICE_DISCONNECTED);
        }
    }

    public void renderStubModels(IntModelBatch modelBatch, ICamera camera, PerspectiveCamera pc, Array<StubModel> controllerObjects, int eye) {
        updateCamera(camera != null ? (NaturalCamera) camera.getCurrent() : null, pc, eye, false, rc, 0.1f);
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        modelBatch.begin(pc);
        for (StubModel controller : controllerObjects) {
            if (controller.getDelayRender())
                modelBatch.render(controller.instance, controllersEnv);
        }
        modelBatch.end();
    }

    @Override
    public void render(SceneGraphRenderer sgr, ICamera camera, double t, int rw, int rh, FrameBuffer fb, PostProcessBean ppb) {
        if (vrContext != null) {
            rc.ppb = null;
            vrContext.pollEvents();

            // Add controllers
            float camnearbak = camera.getCamera().near;
            double closestDist = camera.getClosest().getDistToCamera();
            boolean r = false;
            for (StubModel controller : controllerObjects) {
                Vector3 devicepos = controller.getDevice().getPosition(Space.Tracker);
                // Length from headset to controller
                auxd1.set(devicepos).sub(vrContext.getDeviceByType(VRDeviceType.HeadMountedDisplay).getPosition(Space.Tracker));
                double controllerDist = auxd1.len();
                if (camnearbak < controllerDist || closestDist / controllerDist < 0.5) {
                    controller.addToRenderLists(RenderGroup.MODEL_NORMAL);
                    controller.setDelayRender(false);
                } else {
                    controller.addToRenderLists(null);
                    controller.setDelayRender(true);
                    r = true;
                }

            }

            /** LEFT EYE **/

            // Camera to left
            updateCamera((NaturalCamera) camera.getCurrent(), camera.getCamera(), 0, false, rc, camnearbak);

            sgr.renderGlowPass(camera);

            boolean postproc = postprocessCapture(ppb, fbLeft, rw, rh);

            // Render scene
            sgr.renderScene(camera, t, rc);
            // Camera
            camera.render(1496, 1780);

            // GUI
            if (controllerHintGui.left().isVisible()) {
                renderGui(controllerHintGui.left());
            } else if (GlobalConf.runtime.DISPLAY_VR_GUI) {
                renderGui(infoGui.left());
            }

            if (r) {
                renderStubModels(modelBatch, camera, camera.getCamera(), controllerObjects, 0);
            }

            postprocessRender(ppb, fbLeft, postproc, camera, rw, rh);

            /** RIGHT EYE **/

            // Camera to right
            updateCamera((NaturalCamera) camera.getCurrent(), camera.getCamera(), 1, false, rc, camnearbak);

            sgr.renderGlowPass(camera);

            postproc = postprocessCapture(ppb, fbRight, rw, rh);

            // Render scene
            sgr.renderScene(camera, t, rc);
            // Camera
            camera.render(1496, 1780);

            // GUI
            if (controllerHintGui.right().isVisible()) {
                renderGui(controllerHintGui.right());
            } else if (GlobalConf.runtime.DISPLAY_VR_GUI) {
                renderGui(infoGui.right());
            }

            if (r) {
                renderStubModels(modelBatch, camera, camera.getCamera(), controllerObjects, 1);
            }

            postprocessRender(ppb, fbRight, postproc, camera, rw, rh);

            /** SUBMIT TO VR COMPOSITOR **/
            VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Left, texLeft, null, VR.EVRSubmitFlags_Submit_Default);
            VRCompositor.VRCompositor_Submit(VR.EVREye_Eye_Right, texRight, null, VR.EVRSubmitFlags_Submit_Default);

            /** Render to screen **/
            sb.begin();
            sb.draw(fbLeft.getColorBufferTexture(), 0, 0, rw, rh, 0, 0, fbRight.getWidth(), fbRight.getHeight(), false, true);
            sb.end();
        }

    }

    private void updateCamera(NaturalCamera cam, PerspectiveCamera camera, int eye, boolean updateFrustum, RenderingContext rc, float near) {
        // get the projection matrix from the HDM 
        VRSystem.VRSystem_GetProjectionMatrix(eye, near, camera.far, projectionMat);
        VRContext.hmdMat4toMatrix4(projectionMat, camera.projection);

        // get the eye space matrix from the HDM
        VRSystem.VRSystem_GetEyeToHeadTransform(eye, eyeMat);
        VRContext.hmdMat34ToMatrix4(eyeMat, eyeSpace);
        invEyeSpace.set(eyeSpace).inv();

        // get the pose matrix from the HDM
        VRDevice hmd = vrContext.getDeviceByType(VRDeviceType.HeadMountedDisplay);
        Vector3 up = hmd.getUp(Space.Tracker);
        Vector3 dir = hmd.getDirection(Space.Tracker);
        Vector3 pos = hmd.getPosition(Space.Tracker);

        // Update main camera
        if (cam != null) {
            cam.vroffset.set(pos).scl(VRContext.VROFFSET_FACTOR);
            cam.direction.set(dir);
            cam.up.set(up);
            rc.vroffset = cam.vroffset;
        }

        // Update Eye camera
        //pos.set(0, 0, 0);
        camera.view.idt();
        camera.view.setToLookAt(pos, auxf1.set(pos).add(dir), up);

        camera.combined.set(camera.projection);
        Matrix4.mul(camera.combined.val, invEyeSpace.val);
        Matrix4.mul(camera.combined.val, camera.view.val);

        if (updateFrustum) {
            camera.invProjectionView.set(camera.combined);
            Matrix4.inv(camera.invProjectionView.val);
            camera.frustum.update(camera.invProjectionView);
        }
    }

    private void renderGui(IGui gui) {
        gui.update(Gdx.graphics.getDeltaTime());
        gui.render(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(final int w, final int h) {

    }

    public void dispose() {
        if (fbLeft != null)
            fbLeft.dispose();
        if (fbRight != null)
            fbRight.dispose();
        if (vrContext != null) {
            vrContext.dispose();
        }
    }

    private void addVRController(VRDevice device) {
        if (!vrDeviceToModel.containsKey(device)) {
            StubModel sm = new StubModel(device, controllersEnv);
            controllerObjects.add(sm);
            vrDeviceToModel.put(device, sm);
        }
    }

    private void removeVRController(VRDevice device) {
        if (vrDeviceToModel.containsKey(device)) {
            StubModel sm = vrDeviceToModel.get(device);
            controllerObjects.removeValue(sm, true);
            vrDeviceToModel.remove(device);
        }
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
            case VR_DEVICE_CONNECTED:
                VRDevice device = (VRDevice) data[0];
                if (device.getType() == VRDeviceType.Controller) {
                    Gdx.app.postRunnable(() -> {
                        addVRController(device);
                    });
                }
                break;
            case VR_DEVICE_DISCONNECTED:
                device = (VRDevice) data[0];
                if (device.getType() == VRDeviceType.Controller) {
                    Gdx.app.postRunnable(() -> {
                        removeVRController(device);
                    });
                }
                break;
            default:
                break;
        }

    }

}
