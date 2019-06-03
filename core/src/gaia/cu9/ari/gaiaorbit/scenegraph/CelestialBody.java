/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.*;
import gaia.cu9.ari.gaiaorbit.render.system.FontRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.FovCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.NaturalCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.RotationComponent;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.color.ColourUtils;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.Intersectord;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Quaterniond;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

/**
 * Represents any celestial body.
 * 
 * @author Toni Sagrista
 *
 */
public abstract class CelestialBody extends AbstractPositionEntity implements I3DTextRenderable, IQuadRenderable, IModelRenderable, IFocus {
    private static float[] labelColour = new float[] { 1, 1, 1, 1 };

    /**
     * radius/distance limit for rendering at all. If angle is smaller than this
     * quantity, no rendering happens.
     */
    public abstract double THRESHOLD_NONE();

    /**
     * radius/distance limit for rendering as shader. If angle is any bigger, we
     * render as a model.
     */
    public abstract double THRESHOLD_QUAD();

    /**
     * radius/distance limit for rendering as point. If angle is any bigger, we
     * render with shader.
     */
    public abstract double THRESHOLD_POINT();

    public float TH_OVER_FACTOR;

    /** Absolute magnitude, m = -2.5 log10(flux), with the flux at 10 pc **/
    public float absmag;
    /** Apparent magnitude, m = -2.5 log10(flux) **/
    public float appmag;
    /** Red, green and blue colors and their revamped cousins **/
    public float[] ccPale;
    /** Colour for stars that have been observed by Gaia **/
    public float[] ccTransit;
    /**
     * The B-V color index, calculated as the magnitude in B minus the magnitude
     * in V
     **/
    public float colorbv;
    /** Holds information about the rotation of the body **/
    public RotationComponent rc;

    /** Number of times this body has been observed by Gaia **/
    public int transits = 0;
    /** Last observations increase in ms **/
    public long lastTransitIncrease = 0;

    /** Component alpha mirror **/
    public float compalpha;

    /** Alternative name **/
    private String altname;

    /**
     * Whether we are out of the time baseline range in the algorithm that works
     * out the coordinates of this body
     **/
    protected boolean coordinatesTimeOverflow = false;

    /**
     * Simple constructor
     */
    public CelestialBody() {
        super();
        TH_OVER_FACTOR = (float) (THRESHOLD_POINT() / GlobalConf.scene.LABEL_NUMBER_FACTOR);
    }

    /**
     * Overrides the update adding the magnitude limit thingy.
     */
    @Override
    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera) {
        if (appmag <= GlobalConf.runtime.LIMIT_MAG_RUNTIME) {
            super.update(time, parentTransform, camera);
        }
    }

    float smoothstep(float edge0, float edge1, float x) {
        // Scale, bias and saturate x to 0..1 range
        x = MathUtilsd.clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        // Evaluate polynomial
        return x * x * (3f - 2f * x);
    }

    float step(float edge, float x) {
        if (x < edge)
            return 0f;
        else
            return 1f;
    }

    float core(float distance_center, float inner_rad) {
        float core = 1.0f - step(inner_rad / 5.0f, distance_center);
        float core_glow = smoothstep(inner_rad / 2.0f, inner_rad / 5.0f, distance_center);
        return core_glow + core;
    }

    /**
     * Billboard quad render, for planets and stars.
     */
    @Override
    public void render(ShaderProgram shader, float alpha, IntMesh mesh, ICamera camera) {
        compalpha = alpha;

        float size = getFuzzyRenderSize(camera);

        Vector3 aux = aux3f1.get();
        shader.setUniformf("u_pos", translation.put(aux));
        shader.setUniformf("u_size", size);

        shader.setUniformf("u_color", ccPale[0], ccPale[1], ccPale[2], alpha * opacity);
        shader.setUniformf("u_inner_rad", getInnerRad());
        shader.setUniformf("u_distance", (float) distToCamera);
        shader.setUniformf("u_apparent_angle", (float) viewAngleApparent);
        shader.setUniformf("u_thpoint", (float) THRESHOLD_POINT() * camera.getFovFactor());

        // Whether light scattering is enabled or not
        shader.setUniformi("u_lightScattering", (this instanceof Star && PostProcessorFactory.instance.getPostProcessor().isLightScatterEnabled()) ? 1 : 0);

        shader.setUniformf("u_radius", (float) getRadius());

        // Sprite.render
        mesh.render(shader, GL20.GL_TRIANGLES);
    }

    public float getFuzzyRenderSize(ICamera camera) {
        float thAngleQuad = (float) THRESHOLD_QUAD() * camera.getFovFactor();
        double size = 0f;
        if (viewAngle >= THRESHOLD_POINT() * camera.getFovFactor()) {
            if (viewAngle < thAngleQuad) {
                size = FastMath.tan(thAngleQuad) * distToCamera;
            } else {
                size = this.size;
            }
        }
        return (float) size / camera.getFovFactor();
    }

    /**
     * Label rendering.
     */
    @Override
    public void render(SpriteBatch batch, ShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        if (camera.getCurrent() instanceof FovCamera) {
            render2DLabel(batch, shader, rc, sys.font2d, camera, text(), pos);
        } else {
            // render2DLabel(batch, shader, font, camera, text(),
            // transform.position);
            // 3D distance font
            Vector3d pos = aux3d1.get();
            textPosition(camera, pos);
            shader.setUniformf("u_viewAngle", (float) viewAngleApparent);
            shader.setUniformf("u_viewAnglePow", getViewAnglePow());
            shader.setUniformf("u_thOverFactor", getThOverFactor(camera));
            shader.setUniformf("u_thOverFactorScl", getThOverFactorScl());

            render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor());
        }

    }

    protected float getViewAnglePow() {
        return 1f;
    }

    protected float getThOverFactorScl() {
        return 1f;
    }

    protected float getThOverFactor(ICamera camera) {
        return TH_OVER_FACTOR / camera.getFovFactor();
    }

    protected void setColor2Data() {
        final float plus = .1f;
        ccPale = new float[] { Math.min(1, cc[0] + plus), Math.min(1, cc[1] + plus), Math.min(1, cc[2] + plus) };
        ccTransit = new float[] { ccPale[0], ccPale[1], ccPale[2], cc[3] };
    }

    public abstract float getInnerRad();

    public void setMag(Double mag) {
        this.absmag = mag.floatValue();
        this.appmag = mag.floatValue();
    }

    public void setAbsmag(Double absmag) {
        this.absmag = absmag.floatValue();
    }

    public void setAppmag(Double appmag) {
        this.appmag = appmag.floatValue();
    }

    public float getAppmag() {
        return appmag;
    }

    public float getAbsmag() {
        return absmag;
    }

    public boolean isActive() {
        return true;
    }

    /**
     * Adds all the children that are focusable objects to the list.
     * 
     * @param list
     */
    public void addFocusableObjects(Array<IFocus> list) {
        list.add(this);
        super.addFocusableObjects(list);
    }

    public double getViewAngle() {
        return viewAngle;
    }

    /**
     * Sets the size of this entity in kilometers
     * 
     * @param size
     *            The size in km
     */
    public void setSize(Double size) {
        // Size gives us the radius, and we want the diameter
        this.size = (float) (size * 2 * Constants.KM_TO_U);
    }

    public boolean isStar() {
        return false;
    }

    /**
     * Sets the rotation period in hours
     */
    public void setRotation(RotationComponent rc) {
        this.rc = rc;
    }

    public boolean withinMagLimit() {
        return this.appmag <= GlobalConf.runtime.LIMIT_MAG_RUNTIME;
    }

    @Override
    public <T extends SceneGraphNode> T getSimpleCopy() {
        CelestialBody copy = super.getSimpleCopy();
        copy.absmag = this.absmag;
        copy.appmag = this.appmag;
        copy.colorbv = this.colorbv;
        copy.rc = this.rc;
        return (T) copy;
    }

    /**
     * Updates the transit number of this body if visible is true and it is a
     * new transit. It also updates the colour if needed.
     * 
     * @param visible
     * @param time
     */
    public void updateTransitNumber(boolean visible, ITimeFrameProvider time, FovCamera fcamera) {
        if (GlobalConf.scene.COMPUTE_GAIA_SCAN && visible && timeCondition(time)) {
            // Update observations. Add if forward time, subtract if backward
            // time
            transits = Math.max(0, transits + (int) Math.signum(time.getDt()));
            lastTransitIncrease = time.getTime().toEpochMilli();
            // Update transit colour
            ColourUtils.long_rainbow(ColourUtils.normalize(transits, 0, 30), this.ccTransit);
        }
    }

    protected boolean timeCondition(ITimeFrameProvider time) {
        // 95 seconds minimum since last increase, this ensures we are not
        // increasing more than once in the same transit
        if (time.getDt() < 0 && lastTransitIncrease - time.getTime().toEpochMilli() < 0) {
            lastTransitIncrease = time.getTime().toEpochMilli();
            return true;
        } else if (time.getDt() > 0 && time.getTime().toEpochMilli() - lastTransitIncrease < 0) {
            lastTransitIncrease = time.getTime().toEpochMilli();
            return true;
        } else {
            return (time.getDt() > 0 && time.getTime().toEpochMilli() - lastTransitIncrease > 90000) || (time.getDt() < 0 && lastTransitIncrease - time.getTime().toEpochMilli() > 90000);
        }
    }

    @Override
    public boolean renderText() {
        return name != null && GaiaSky.instance.isOn(ComponentType.Labels) && Math.pow(viewAngleApparent, getViewAnglePow()) >= (TH_OVER_FACTOR * getThOverFactorScl());
    }

    @Override
    public float[] textColour() {
        return labelColour;
    }

    @Override
    public float textScale() {
        return (float) FastMath.atan(labelMax()) * labelFactor() * 4e2f;
    }

    @Override
    public float textSize() {
        return (float) (Math.min(labelSizeConcrete() / FastMath.powQuick(distToCamera, 1.05f), labelMax()) * distToCamera * labelFactor());
    }

    protected float labelSizeConcrete() {
        return this.size;
    }

    protected abstract float labelFactor();

    protected abstract float labelMax();

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        translation.put(out);
        double len = out.len();
        out.clamp(0, len - getRadius()).scl(0.9f);

        Vector3d aux = aux3d2.get();
        aux.set(cam.getUp());

        aux.crs(out).nor();

        float dist = -0.02f * cam.getFovFactor() * (float) out.len();

        aux.add(cam.getUp()).nor().scl(dist);

        out.add(aux);

        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
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
    public boolean hasAtmosphere() {
        return false;
    }

    @Override
    public boolean isLabel() {
        return true;
    }

    public double getPmX() {
        return 0;
    }

    public double getPmY() {
        return 0;
    }

    public double getPmZ() {
        return 0;
    }

    public RotationComponent getRotationComponent() {
        return rc;
    }

    public Quaterniond getOrientationQuaternion() {
        return null;
    }

    public void addHit(int screenX, int screenY, int w, int h, int minPixDist, NaturalCamera camera, Array<IFocus> hits) {
        if (withinMagLimit() && checkHitCondition()) {
            Vector3 pos = aux3f1.get();
            Vector3d aux = aux3d1.get();
            Vector3d posd = getAbsolutePosition(aux).add(camera.getInversePos());
            pos.set(posd.valuesf());

            if (camera.direction.dot(posd) > 0) {
                // The object is in front of us
                double angle = computeViewAngle(camera.getFovFactor());

                PerspectiveCamera pcamera;
                if (GlobalConf.program.STEREOSCOPIC_MODE) {
                    if (screenX < Gdx.graphics.getWidth() / 2f) {
                        pcamera = camera.getCameraStereoLeft();
                        pcamera.update();
                    } else {
                        pcamera = camera.getCameraStereoRight();
                        pcamera.update();
                    }
                } else {
                    pcamera = camera.camera;
                }

                angle = (float) Math.toDegrees(angle * camera.getFovFactor()) * (40f / pcamera.fieldOfView);
                double pixelSize = Math.max(minPixDist, ((angle * pcamera.viewportHeight) / pcamera.fieldOfView) / 2);
                pcamera.project(pos);
                pos.y = pcamera.viewportHeight - pos.y;
                if (GlobalConf.program.STEREOSCOPIC_MODE) {
                    pos.x /= 2;
                }
                // Check click distance
                if (checkClickDistance(screenX, screenY, pos, camera, pcamera, pixelSize)) {
                    //Hit
                    hits.add(this);
                }
            }
        }

    }

    public void addHit(Vector3d p0, Vector3d p1, NaturalCamera camera, Array<IFocus> hits) {
        if (withinMagLimit() && checkHitCondition()) {
            Vector3d aux = aux3d1.get();
            Vector3d posd = getAbsolutePosition(aux).add(camera.getInversePos());

            if (camera.direction.dot(posd) > 0) {
                // The star is in front of us
                // Diminish the size of the star
                // when we are close by
                double dist = posd.len();
                double distToLine = Intersectord.distanceLinePoint(p0, p1, posd);
                double value = distToLine / dist;

                if (value < 0.01) {
                    hits.add(this);
                }
            }
        }
    }

    protected boolean checkClickDistance(int screenX, int screenY, Vector3 pos, NaturalCamera camera, PerspectiveCamera pcamera, double pixelSize) {
        return pos.dst(screenX % pcamera.viewportWidth, screenY, pos.z) <= pixelSize;
    }

    protected double computeViewAngle(float fovFactor) {
        return viewAngle;
    }

    protected boolean checkHitCondition() {
        return !coordinatesTimeOverflow && GaiaSky.instance.isOn(ct);
    }

    public void makeFocus() {
    }

    @Override
    public long getCandidateId() {
        return getId();
    }

    @Override
    public String getCandidateName() {
        return getName();
    }

    @Override
    public double getCandidateViewAngleApparent() {
        return getViewAngleApparent();
    }

    @Override
    public boolean isCoordinatesTimeOverflow() {
        return coordinatesTimeOverflow;
    }

    @Override
    public float getTextOpacity(){
        return getOpacity();
    }

    public void setAltname(String altname) {
        this.altname = altname;
    }

    public String getAltname() {
        return this.altname;
    }

    @Override
    protected void addToIndex(ObjectMap<String, SceneGraphNode> map) {
        if (altname != null && !altname.isEmpty()) {
            map.put(altname.toLowerCase(), this);
        }
    }

    @Override
    protected void removeFromIndex(ObjectMap<String, SceneGraphNode> map) {
        if (altname != null && !altname.isEmpty()) {
            map.remove(altname.toLowerCase());
        }
    }
    @Override
    public IFocus getFocus(String name) {
        if (this.name.toLowerCase().equals(name) || (altname != null && altname.toLowerCase().equals(name)))
            return this;

        return null;
    }

}
