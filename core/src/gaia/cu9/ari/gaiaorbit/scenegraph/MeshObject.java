/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.I3DTextRenderable;
import gaia.cu9.ari.gaiaorbit.render.IModelRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.system.FontRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ITransform;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class MeshObject extends FadeNode implements IModelRenderable, I3DTextRenderable, IVisibilitySwitch {

    private String description;
    private String transformName;
    private Matrix4 coordinateSystem;
    private Vector3 scale, axis, translate;
    private float degrees;

    /**
     * MODEL
     **/
    public ModelComponent mc;

    /**
     * TRANSFORMATIONS - are applied each cycle
     **/
    public ITransform[] transformations;

    // Aux array
    private float[] auxArray;

    public MeshObject() {
        super();
        localTransform = new Matrix4();
        auxArray = new float[3];
    }

    public void initialize() {
        if (mc != null) {
            mc.initialize(true);
        }
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        if (mc != null) {
            try {
                mc.doneLoading(manager, localTransform, cc, true);
            } catch (Exception e) {
                mc = null;
            }
        }

        if (mc != null) {
            coordinateSystem = new Matrix4();
            if (transformName != null) {
                Class<Coordinates> c = Coordinates.class;
                try {
                    Method m = ClassReflection.getMethod(c, transformName);
                    Matrix4 trf = (Matrix4) m.invoke(null);
                    coordinateSystem.set(trf);
                } catch (ReflectionException e) {
                    Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transformName + "()");
                }
            } else {
                // Equatorial, nothing
            }

            if (axis != null)
                coordinateSystem.rotate(axis, degrees);
            if (translate != null) {
                pos.set(translate);
                coordinateSystem.translate(translate.x, translate.y, translate.z);
            }
            if (scale != null)
                coordinateSystem.scale(scale.x, scale.y, scale.z);
        }
    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);
        // Update light with global position
        if (mc != null) {
            mc.dlight.direction.set(1f, 0f, 0f);
            mc.dlight.color.set(1f, 1f, 1f, 1f);

            updateLocalTransform();
        }
    }

    /**
     * Update the local transform with the transform and the rotations/scales
     * necessary. Override if your model contains more than just the position
     * and size.
     */
    protected void updateLocalTransform() {
        setToLocalTransform(localTransform, true);
    }

    public void setToLocalTransform(Matrix4 localTransform, boolean forceUpdate) {
        if (forceUpdate) {
            float[] trn = translation.valuesf(auxArray);
            localTransform.idt().translate(trn[0], trn[1], trn[2]).scl(size).mul(coordinateSystem);
        } else {
            localTransform.set(this.localTransform);
        }

        // Apply transformations
        if (transformations != null)
            for (ITransform tc : transformations)
                tc.apply(localTransform);
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (GaiaSky.instance.isInitialised() && GaiaSky.instance.isOn(ct) & opacity > 0) {
            addToRender(this, RenderGroup.MODEL_MESH);
            addToRender(this, RenderGroup.FONT_LABEL);
        }

    }

    /**
     * Model rendering
     */
    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc) {
        if (mc != null) {
            mc.touch(localTransform);
            if (mc.instance != null) {
                mc.setTransparency(alpha * opacity, GL20.GL_ONE, GL20.GL_ONE);
                mc.updateRelativisticEffects(GaiaSky.instance.getICamera());
                modelBatch.render(mc.instance, mc.env);
            }
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setModel(ModelComponent mc) {
        this.mc = mc;
    }

    public void setTransformName(String transformName) {
        this.transformName = transformName;
    }

    public void setTranslate(double[] tr) {
        translate = new Vector3((float) tr[0], (float) tr[1], (float) tr[2]);
    }

    public void setRotate(double[] rt) {
        axis = new Vector3((float) rt[0], (float) rt[1], (float) rt[2]);
        degrees = (float) rt[3];
    }

    public void setScale(double[] sc) {
        scale = new Vector3((float) sc[0], (float) sc[1], (float) sc[2]);
    }

    @Override
    public boolean hasAtmosphere() {
        return false;
    }

    @Override
    public boolean renderText() {
        return name != null && GaiaSky.instance.isOn(ComponentType.Labels) && this.opacity > 0;
    }

    /**
     * Label rendering
     */
    @Override
    public void render(SpriteBatch batch, ShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        Vector3d pos = aux3d1.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thOverFactor", 1f);
        shader.setUniformf("u_thOverFactorScl", 1f);
        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor());
    }

    @Override
    public float[] textColour() {
        return labelColour;
    }

    @Override
    public float textSize() {
        return (float) distToCamera * .8e-3f;
    }

    @Override
    public float textScale() {
        return 0.2f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        if (labelPosition != null)
            out.set(labelPosition).add(cam.getInversePos());
        else
            out.set(pos).add(cam.getInversePos());
    }

    @Override
    public String text() {
        return name;
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
    }

    @Override
    public boolean isLabel() {
        return true;
    }

    @Override
    public void setSize(Double size) {
        this.size = (float) (size * Constants.VR_SCALE);
    }

    @Override
    public void setSize(Long size) {
        this.size = (float) (size.doubleValue() * Constants.VR_SCALE);
    }

    @Override
    public float getTextOpacity() {
        return getOpacity();
    }

}
