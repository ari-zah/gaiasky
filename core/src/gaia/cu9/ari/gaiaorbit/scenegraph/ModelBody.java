/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.SceneGraphRenderer;
import gaia.cu9.ari.gaiaorbit.render.ShadowMapImpl;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.NaturalCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ITransform;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.math.Intersectord;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

/**
 * Abstract class with the basic functionality of bodies represented by a 3D
 * model.
 * 
 * @author Toni Sagrista
 *
 */
public abstract class ModelBody extends CelestialBody {
    protected static final double TH_ANGLE_POINT = Math.toRadians(0.30);

    /**
     * Angle limit for rendering as point. If angle is any bigger, we render
     * with shader.
     */
    public double THRESHOLD_POINT() {
        return TH_ANGLE_POINT;
    }

    /** MODEL **/
    public ModelComponent mc;

    /** NAME FOR WIKIPEDIA **/
    public String wikiname;

    /** TRANSFORMATIONS - are applied each cycle **/
    public ITransform[] transformations;

    /** Multiplier for Loc view angle **/
    public float locVaMultiplier = 1f;
    /** ThOverFactor for Locs **/
    public float locThOverFactor = 1f;

    /** Size factor, which can be set to scale model objects up or down **/
    public float sizeScaleFactor = 1f;

    /** Fade opacity, special to model bodies **/
    protected float fadeOpacity;

    /** Shadow map properties **/
    private ShadowMapImpl shadowMap;

    /** State flag; whether to render the shadow (number of times left) **/
    public int shadow;

    /** Name of the reference plane for this object. Defaults to equator **/
    public String refPlane;
    /** Name of the transformation to the reference plane **/
    public String refPlaneTransform;
    public String inverseRefPlaneTransform;

    /**
     * Array with shadow camera distance, cam near and cam far as a function of
     * the radius of the object
     */
    public double[] shadowMapValues;

    public ModelBody() {
        super();
        localTransform = new Matrix4();
        orientation = new Matrix4d();
    }

    public void initialize() {
        if (mc != null) {
            mc.initialize();
        }
        setColor2Data();
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);
        if (mc != null) {
            mc.doneLoading(manager, localTransform, cc);
        }
    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);
        // Update light with global position
        if (mc != null) {
            translation.put(mc.dlight.direction);
            IStarFocus sf = camera.getClosestStar();
            if (sf != null) {
                float[] col = sf.getClosestCol();
                mc.dlight.direction.sub(sf.getClosestPos(aux3d1.get()).put(aux3f1.get()));
                mc.dlight.color.set(col[0], col[1], col[2], 1.0f);
            } else {
                Vector3d campos = camera.getPos();
                mc.dlight.direction.add((float) campos.x, (float) campos.y, (float) campos.z);
                mc.dlight.color.set(1f, 1f, 1f, 1f);
            }
        }
        updateLocalTransform();
    }

    /**
     * Update the local transform with the transform and the rotations/scales
     * necessary. Override if your model contains more than just the position
     * and size.
     */
    protected void updateLocalTransform() {
        setToLocalTransform(sizeScaleFactor, localTransform, true);
    }

    public void setToLocalTransform(float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        setToLocalTransform(size, sizeFactor, localTransform, forceUpdate);
    }

    public void setToLocalTransform(float size, float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        if (sizeFactor != 1 || forceUpdate) {
            // NEW
            translation.getMatrix(localTransform).scl(size * sizeFactor).mul(Coordinates.getTransformF(refPlaneTransform)).rotate(0, 1, 0, (float) rc.ascendingNode).rotate(0, 0, 1, (float) (rc.inclination + rc.axialTilt)).rotate(0, 1, 0, (float) rc.angle);
            orientation.idt().mul(Coordinates.getTransformD(refPlaneTransform)).rotate(0, 0, 1, (float) (rc.inclination + rc.axialTilt)).rotate(0, 1, 0, (float) rc.ascendingNode);
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
        if (!coordinatesTimeOverflow) {
            camera.checkClosest(this);
            if (GaiaSky.instance.isOn(ct)) {
                double thPoint = (THRESHOLD_POINT() * camera.getFovFactor()) / sizeScaleFactor;
                if (viewAngleApparent >= thPoint) {
                    double thQuad2 = THRESHOLD_QUAD() * camera.getFovFactor() * 2 / sizeScaleFactor;
                    double thQuad1 = thQuad2 / 8.0 / sizeScaleFactor;
                    if (viewAngleApparent < thPoint * 4) {
                        fadeOpacity = (float) MathUtilsd.lint(viewAngleApparent, thPoint, thPoint * 4, 1, 0);
                    } else {
                        fadeOpacity = (float) MathUtilsd.lint(viewAngleApparent, thQuad1, thQuad2, 0, 1);
                    }

                    if (viewAngleApparent < thQuad1) {
                        addToRender(this, RenderGroup.BILLBOARD_SSO);
                    } else if (viewAngleApparent > thQuad2) {
                        addToRender(this, RenderGroup.MODEL_NORMAL);
                    } else {
                        // Both
                        addToRender(this, RenderGroup.BILLBOARD_SSO);
                        addToRender(this, RenderGroup.MODEL_NORMAL);
                    }

                    if (renderText()) {
                        addToRender(this, RenderGroup.FONT_LABEL);
                    }
                }
            }
        }
    }

    @Override
    public float getInnerRad() {
        return .5f;
        // return .02f;
    }

    public void dispose() {
        super.dispose();
        mc.dispose();
    }

    /**
     * Billboard quad rendering
     */
    @Override
    public void render(ShaderProgram shader, float alpha, IntMesh mesh, ICamera camera) {
        compalpha = alpha;

        float size = getFuzzyRenderSize(camera);

        Vector3 aux = aux3f1.get();
        shader.setUniformf("u_pos", translation.put(aux));
        shader.setUniformf("u_size", size);

        shader.setUniformf("u_color", cc[0], cc[1], cc[2], alpha * (1 - fadeOpacity));
        shader.setUniformf("u_inner_rad", getInnerRad());
        shader.setUniformf("u_distance", (float) distToCamera);
        shader.setUniformf("u_apparent_angle", (float) viewAngleApparent);
        shader.setUniformf("u_thpoint", (float) THRESHOLD_POINT() * camera.getFovFactor());

        // Whether light scattering is enabled or not
        shader.setUniformi("u_lightScattering", 0);

        shader.setUniformf("u_radius", (float) getRadius());

        // Sprite.render
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    /** Model rendering **/
    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc) {
        prepareShadowEnvironment();
        mc.touch();
        mc.setTransparency(alpha * fadeOpacity);
        mc.updateRelativisticEffects(GaiaSky.instance.getICamera());
        modelBatch.render(mc.instance, mc.env);
    }

    /** Model opaque rendering. Disable shadow mapping **/
    public void renderOpaque(IntModelBatch modelBatch, float alpha, double t) {
        mc.touch();
        mc.setTransparency(alpha * fadeOpacity);
        mc.updateRelativisticEffects(GaiaSky.instance.getICamera());
        modelBatch.render(mc.instance, mc.env);
    }

    public boolean withinMagLimit() {
        return this.absmag <= GlobalConf.runtime.LIMIT_MAG_RUNTIME;
    }

    @Override
    protected float labelMax() {
        return 1e-9f;
    }

    public void setModel(ModelComponent mc) {
        this.mc = mc;
    }

    public float getFuzzyRenderSize(ICamera camera) {
        float thAngleQuad = (float) THRESHOLD_QUAD() * camera.getFovFactor();
        double size = 0f;
        if (viewAngle >= THRESHOLD_POINT() * camera.getFovFactor()) {
            size = Math.tan(thAngleQuad) * distToCamera * 2f;
        }
        return (float) size;
    }

    protected float getViewAnglePow() {
        return 1.0f;
    }

    protected float getThOverFactorScl() {
        return ct.get(ComponentType.Moons.ordinal()) ? 2500f : 25f;
    }

    protected float getThOverFactor(ICamera camera) {
        return TH_OVER_FACTOR;
    }

    @Override
    public float textScale() {
        return Math.max(1f, labelSizeConcrete()) * 2e-1f;
    }

    protected float labelSizeConcrete() {
        return (float) Math.pow(this.size * .6e1f, .001f);
    }

    public String getWikiname() {
        return wikiname;
    }

    public void setWikiname(String wikiname) {
        this.wikiname = wikiname;
    }

    public void setLocvamultiplier(Double val) {
        this.locVaMultiplier = val.floatValue();
    }

    public void setLocthoverfactor(Double val) {
        this.locThOverFactor = val.floatValue();
    }

    public void setTransformations(Object[] transformations) {
        this.transformations = new ITransform[transformations.length];
        for (int i = 0; i < transformations.length; i++)
            this.transformations[i] = (ITransform) transformations[i];
    }

    /**
     * Returns the cartesian position in the internal reference system above the
     * surface at the given longitude and latitude and distance.
     * 
     * @param longitude
     *            The longitude in deg
     * @param latitude
     *            The latitude in deg
     * @param distance
     *            The distance in km
     * @param out
     *            The vector to store the result
     * @return The cartesian position above the surface of this body
     */
    public Vector3d getPositionAboveSurface(double longitude, double latitude, double distance, Vector3d out) {
        Vector3d aux1 = aux3d1.get();
        Vector3d aux2 = aux3d2.get();

        // Lon/Lat/Radius
        longitude *= MathUtilsd.degRad;
        latitude *= MathUtilsd.degRad;
        double rad = 1;
        Coordinates.sphericalToCartesian(longitude, latitude, rad, aux1);

        aux2.set(aux1.z, aux1.y, aux1.x).scl(1, -1, -1).scl(-(getRadius() + distance * Constants.KM_TO_U));
        //aux2.rotate(rc.angle, 0, 1, 0);
        Matrix4d ori = new Matrix4d(orientation);
        ori.rotate(0, 1, 0, (float) rc.angle);
        aux2.mul(ori);

        getAbsolutePosition(out).add(aux2);
        return out;
    }

    /**
     * Sets the shadow environment
     */
    protected void prepareShadowEnvironment() {
        if (GlobalConf.scene.SHADOW_MAPPING) {
            Environment env = mc.env;
            SceneGraphRenderer sgr = SceneGraphRenderer.instance;
            if (shadow > 0 && sgr.smTexMap.containsKey(this)) {
                Matrix4 combined = sgr.smCombinedMap.get(this);
                Texture tex = sgr.smTexMap.get(this);
                if (env.shadowMap == null) {
                    if (shadowMap == null)
                        shadowMap = new ShadowMapImpl(combined, tex);
                    env.shadowMap = shadowMap;
                }
                shadowMap.setProjViewTrans(combined);
                shadowMap.setDepthMap(tex);

                shadow--;
            } else {
                env.shadowMap = null;
            }
        } else {
            mc.env.shadowMap = null;
        }
    }

    /**
     * If we render the model, we set up a sphere at the object's position with
     * its radius and check for intersections with the ray
     */
    public void addHit(int screenX, int screenY, int w, int h, int minPixDist, NaturalCamera camera, Array<IFocus> hits) {
        if (withinMagLimit() && checkHitCondition()) {
            if (viewAngleApparent < THRESHOLD_QUAD() * camera.getFovFactor()) {
                super.addHit(screenX, screenY, w, h, minPixDist, camera, hits);
            } else {
                Vector3 auxf = aux3f1.get();
                Vector3d aux1d = aux3d1.get();
                Vector3d aux2d = aux3d2.get();
                Vector3d aux3d = aux3d3.get();

                // aux1d contains the position of the body in the camera ref sys
                aux1d.set(translation);
                auxf.set(aux1d.valuesf());

                if (camera.direction.dot(aux1d) > 0) {
                    // The object is in front of us
                    auxf.set(screenX, screenY, 2f);
                    camera.camera.unproject(auxf).nor();

                    // aux2d contains the position of the click in the camera ref sys
                    aux2d.set(auxf.x, auxf.y, auxf.z);

                    // aux3d contains the camera position, [0,0,0]
                    aux3d.set(0, 0, 0);

                    boolean intersect = Intersectord.checkIntersectRaySpehre(aux3d, aux2d, aux1d, getRadius());
                    if (intersect) {
                        //Hit
                        hits.add(this);
                    }
                }
            }
        }

    }

    public void addHit(Vector3d p0, Vector3d p1, NaturalCamera camera, Array<IFocus> hits) {
        if (withinMagLimit() && checkHitCondition()) {
            if (viewAngleApparent < THRESHOLD_QUAD() * camera.getFovFactor()) {
                super.addHit(p0, p1, camera, hits);
            } else {
                Vector3d aux1d = aux3d1.get();

                // aux1d contains the position of the body in the camera ref sys
                aux1d.set(translation);

                boolean intersect = Intersectord.checkIntersectRaySpehre(p0, p1, aux1d, getRadius());
                if (intersect) {
                    //Hit
                    hits.add(this);
                }
            }
        }
    }

    @Override
    public double getSize() {
        return super.getSize() * sizeScaleFactor;
    }

    public double getRadius() {
        return super.getRadius() * sizeScaleFactor;
    }

    /**
     * Whether shadows should be rendered for this object
     * 
     * @return Whether shadows should be rendered for this object
     */
    public boolean isShadow() {
        return shadowMapValues != null;
    }

    /**
     * Sets the shadow mapping values for this object
     * 
     * @param shadowMapValues
     *            The values
     */
    public void setShadowvalues(double[] shadowMapValues) {
        this.shadowMapValues = shadowMapValues;
    }

    public void setSizescalefactor(Double sizescalefactor) {
        this.sizeScaleFactor = sizescalefactor.floatValue();
    }

    public void setRefplane(String refplane) {
        this.refPlane = refplane;
        this.refPlaneTransform = refplane + "toequatorial";
        this.inverseRefPlaneTransform = "equatorialto" + refplane;
    }

}
