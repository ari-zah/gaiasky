package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.render.ILineRenderable;
import gaia.cu9.ari.gaiaorbit.render.IModelRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModelInstance;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.vr.VRContext.VRDevice;

public class StubModel extends AbstractPositionEntity implements IModelRenderable, ILineRenderable {

    public IntModelInstance instance;
    private Environment env;
    private VRDevice device;
    private boolean delayRender = false;
    private Vector3 beamP0, beamP1;

    public StubModel(VRDevice device, Environment env) {
        super();
        this.env = env;
        this.instance = device.getModelInstance();
        this.device = device;
        beamP0 = new Vector3();
        beamP1 = new Vector3();
        this.cc = new float[] { 1f, 0f, 0f };
        setCt("Others");
    }

    @Override
    public ComponentTypes getComponentType() {
        return ct;
    }

    @Override
    public double getDistToCamera() {
        return 0;
    }

    @Override
    public float getOpacity() {
        return 0;
    }

    public void addToRenderLists(RenderGroup rg) {
        if (rg != null) {
            addToRender(this, rg);
        }
        addToRender(this, RenderGroup.LINE_VR);
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
    }

    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc) {
        setTransparency(alpha);
        modelBatch.render(instance, env);
    }

    /**
     * Occlusion rendering
     */
    public void renderOpaque(IntModelBatch modelBatch, float alpha, double t) {
        setTransparency(alpha);
        modelBatch.render(instance, env);
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        Matrix4 transform = instance.transform;
        beamP0.set(0, -0.1f, 0).mul(transform);
        beamP1.set(0, -.8e15f, -1e15f).mul(transform);
        renderer.addLine(this, beamP0.x, beamP0.y, beamP0.z, beamP1.x, beamP1.y - 0.1f, beamP1.z, 1f, 0.0f, 0.0f, 0.5f);
    }

    @Override
    public int getGlType() {
        return GL20.GL_LINES;
    }

    public void setTransparency(float alpha) {
        if (instance != null) {
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                Material mat = instance.materials.get(i);
                BlendingAttribute ba = null;
                if (mat.has(BlendingAttribute.Type)) {
                    ba = (BlendingAttribute) mat.get(BlendingAttribute.Type);
                } else {
                    ba = new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                    mat.set(ba);
                }
                ba.opacity = alpha;
            }
        }
    }

    @Override
    public boolean hasAtmosphere() {
        return false;
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {

    }

    public VRDevice getDevice() {
        return device;
    }

    public boolean getDelayRender() {
        return delayRender;
    }

    public void setDelayRender(boolean dr) {
        this.delayRender = dr;
    }

    /**
     * Gets the initial point of the controller beam in camera space
     * 
     * @return Initial point of controller beam
     */
    public Vector3 getBeamP0() {
        return beamP0;
    }

    /**
     * Gets the end point of the controller beam in camera space
     * 
     * @return End point of controller beam
     */
    public Vector3 getBeamP1() {
        return beamP1;
    }

    @Override
    public float getLineWidth() {
        return 2.5f;
    }

}
