/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class LightBeam extends ModelBody {

    protected static final double TH_ANGLE_NONE = ModelBody.TH_ANGLE_POINT / 1e18;
    protected static final double TH_ANGLE_POINT = ModelBody.TH_ANGLE_POINT / 1e9;
    protected static final double TH_ANGLE_QUAD = ModelBody.TH_ANGLE_POINT / 8;

    Matrix4 orientationf;

    Vector3 rotation3axis;
    float angle;
    Vector3 translation3;

    public LightBeam() {
        orientationf = new Matrix4();
    }

    @Override
    public double THRESHOLD_NONE() {
        return TH_ANGLE_NONE;
    }

    @Override
    public double THRESHOLD_POINT() {
        return TH_ANGLE_POINT;
    }

    @Override
    public double THRESHOLD_QUAD() {
        return TH_ANGLE_QUAD;
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    @Override
    protected void updateLocalTransform() {
        setToLocalTransform(1, localTransform, true);
    }

    /**
     * Sets the local transform of this satellite
     */
    public void setToLocalTransform(float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        if (sizeFactor != 1 || forceUpdate) {

            translation.getMatrix(localTransform).scl(size * sizeFactor);

            parent.orientation.putIn(orientationf);
            localTransform.mul(orientationf);

            localTransform.rotate(1, 0, 0, 90);

            // First beam
            localTransform.rotate(rotation3axis, angle).translate(translation3).rotate(0, 0, 1, 180);
        } else {
            localTransform.set(this.localTransform);
        }

    }

    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc) {
        mc.touch();
        mc.setTransparency(alpha * opacity);
        modelBatch.render(mc.instance, mc.env);
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        addToRender(this, RenderGroup.MODEL_BEAM);
    }

    @Override
    protected float labelFactor() {
        return 0f;
    }

    @Override
    protected float labelMax() {
        return 0f;
    }

    protected float getViewAnglePow() {
        return 1f;
    }

    protected float getThOverFactorScl() {
        return 0f;
    }

    public float getFuzzyRenderSize(ICamera camera) {
        return 0;
    }

    public void setRotationaxis(double[] rotationaxis) {
        this.rotation3axis = new Vector3((float) rotationaxis[0], (float) rotationaxis[1], (float) rotationaxis[2]);
    }

    public void setTranslation(double[] translation) {
        this.translation3 = new Vector3((float) translation[0], (float) translation[1], (float) translation[2]);
    }

    public void setAngle(Double angle) {
        this.angle = angle.floatValue();
    }

}
