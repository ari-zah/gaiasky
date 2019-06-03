/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.render.*;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.system.FontRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.NaturalCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.RotationComponent;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.ModelCache;
import gaia.cu9.ari.gaiaorbit.util.Nature;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntMeshPartBuilder;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBuilder;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModel;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModelInstance;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.Intersectord;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Quaterniond;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

public class StarCluster extends AbstractPositionEntity implements IFocus, IProperMotion, IModelRenderable, I3DTextRenderable, IQuadRenderable {

    private static final double TH_ANGLE = Math.toRadians(0.6);
    private static final double TH_ANGLE_OVERLAP = Math.toRadians(0.7);
    private static IntModel model;
    private static Matrix4 modelTransform;
    private static Texture clusterTex;

    // Label and model colors
    private static float[] col = new float[] { 0.9f, 0.9f, 0.2f, 0.6f };

    private ModelComponent mc;

    /** Proper motion in units/year **/
    protected Vector3d pm;
    /** Proper motion in mas/year **/
    protected Vector3 pmSph;

    protected float[] labelcolor;

    // Distance of this cluster to Sol, in internal units
    protected double dist;

    // Radius of this cluster in degrees
    protected double raddeg;

    // Number of stars of this cluster
    protected int nstars;

    // Years since epoch
    protected double ySinceEpoch;

    /**
     * Fade alpha between quad and model. Attribute contains model opacity. Quad
     * opacity is <code>1-fadeAlpha</code>
     **/
    protected float fadeAlpha;

    public StarCluster() {
        super();
        this.localTransform = new Matrix4();
        this.pm = new Vector3d();
        this.pmSph = new Vector3();
    }

    public StarCluster(String name, String parentName, Vector3d pos, Vector3d pm, Vector3d posSph, Vector3 pmSph, double raddeg, int nstars) {
        this();
        this.parentName = parentName;
        this.name = name.replace("_", " ");
        this.pos = pos;
        this.posSph.set((float) posSph.x, (float) posSph.y);
        this.pm = pm;
        this.pmSph = pmSph;
        this.dist = posSph.z;
        this.raddeg = raddeg;
        this.nstars = nstars;
        this.labelcolor = new float[] { col[0], col[1], col[2], 8.0f };
    }

    public void initModel() {
        if (clusterTex == null) {
            clusterTex = new Texture(GlobalConf.data.dataFileHandle("data/tex/base/cluster-tex.png"), true);
            clusterTex.setFilter(TextureFilter.MipMapLinearNearest, TextureFilter.Linear);
        }
        if (model == null) {
            Material mat = new Material(new BlendingAttribute(GL20.GL_ONE, GL20.GL_ONE), new ColorAttribute(ColorAttribute.Diffuse, col[0], col[1], col[2], col[3]));
            IntModelBuilder modelBuilder = ModelCache.cache.mb;
            modelBuilder.begin();
            // create part
            IntMeshPartBuilder bPartBuilder = modelBuilder.part("sph", GL20.GL_LINES, Usage.Position, mat);
            bPartBuilder.icosphere(1, 3, false, true);

            model = (modelBuilder.end());
            modelTransform = new Matrix4();
        }

        mc = new ModelComponent(false);
        mc.initialize();
        mc.dlight = new DirectionalLight();
        mc.dlight.set(1, 1, 1, 1, 1, 1);
        mc.env = new Environment();
        mc.env.add(mc.dlight);
        mc.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        mc.env.set(new FloatAttribute(FloatAttribute.Shininess, 0.2f));
        mc.instance = new IntModelInstance(model, modelTransform);

        // Relativistic effects
        if (GlobalConf.runtime.RELATIVISTIC_ABERRATION)
            mc.rec.setUpRelativisticEffectsMaterial(mc.instance.materials);
        // Gravitational waves
        if (GlobalConf.runtime.GRAVITATIONAL_WAVES)
            mc.rec.setUpGravitationalWavesMaterial(mc.instance.materials);

    }

    public void initialize() {
        this.ct = new ComponentTypes(ComponentType.Clusters.ordinal());
        // Compute size from distance and radius, convert to units
        this.size = (float) (Math.tan(Math.toRadians(this.raddeg)) * this.dist * 2);

    }

    @Override
    public void doneLoading(AssetManager manager) {
        initModel();
    }

    /**
     * Updates the local transform matrix.
     * 
     * @param time
     */
    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        // Update pos, local transform
        this.translation.add(pos);
        ySinceEpoch = AstroUtils.getMsSince(time.getTime(), AstroUtils.JD_J2015_5) * Nature.MS_TO_Y;
        Vector3d pmv = aux3d1.get().set(pm).scl(ySinceEpoch);
        this.translation.add(pmv);

        this.localTransform.idt().translate(this.translation.put(aux3f1.get())).scl(this.size);

        Vector3d aux = aux3d1.get();
        this.distToCamera = (float) aux.set(translation).len();
        this.viewAngle = (float) FastMath.atan(size / distToCamera);
        this.viewAngleApparent = this.viewAngle / camera.getFovFactor();
        if (!copy) {
            addToRenderLists(camera);
        }

        this.opacity *= 0.1f;
        this.fadeAlpha = (float) MathUtilsd.lint(this.viewAngleApparent, TH_ANGLE, TH_ANGLE_OVERLAP, 0f, 1f);
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.opacity > 0) {
            if (this.viewAngleApparent > TH_ANGLE) {
                addToRender(this, RenderGroup.MODEL_MESH);
                addToRender(this, RenderGroup.FONT_LABEL);
            }

            if (this.viewAngleApparent < TH_ANGLE_OVERLAP) {
                addToRender(this, RenderGroup.BILLBOARD_SPRITE);
            }
        }
    }

    @Override
    public Vector3d getAbsolutePosition(Vector3d aux) {
        aux.set(pos);
        Vector3d pmv = aux3d2.get().set(pm).scl(ySinceEpoch);
        aux.add(pmv);
        AbstractPositionEntity entity = this;
        while (entity.parent != null && entity.parent instanceof AbstractPositionEntity) {
            entity = (AbstractPositionEntity) entity.parent;
            aux.add(entity.pos);
        }
        return aux;
    }

    /**
     * Model rendering
     */
    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc) {
        mc.touch();
        mc.setTransparency(alpha * opacity * fadeAlpha, GL20.GL_ONE, GL20.GL_ONE);
        mc.instance.transform.set(this.localTransform);
        mc.updateRelativisticEffects(GaiaSky.instance.getICamera());
        modelBatch.render(mc.instance, mc.env);

    }

    /**
     * Billboard quad rendering
     */
    @Override
    public void render(ShaderProgram shader, float alpha, IntMesh mesh, ICamera camera) {
        // Bind texture
        if (clusterTex != null) {
            clusterTex.bind(0);
            shader.setUniformi("u_texture0", 0);
        }

        float fa = 1 - fadeAlpha;

        Vector3 aux = aux3f1.get();
        shader.setUniformf("u_pos", translation.put(aux));
        shader.setUniformf("u_size", size);
        shader.setUniformf("u_color", col[0] * fa, col[1] * fa, col[2] * fa, col[3] * alpha * opacity * 8f);
        // Sprite.render
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    /**
     * Label rendering
     */
    @Override
    public void render(SpriteBatch batch, ShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        Vector3d pos = aux3d1.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", (float) this.viewAngle * 500f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thOverFactor", 1f);
        shader.setUniformf("u_thOverFactorScl", 1f);

        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor());
    }

    @Override
    public boolean hasAtmosphere() {
        return false;
    }

    @Override
    public boolean renderText() {
        return name != null && GaiaSky.instance.isOn(ComponentType.Labels) && this.opacity > 0;
    }

    @Override
    public float[] textColour() {
        labelcolor[3] = 8.0f * fadeAlpha;
        return labelcolor;
    }

    @Override
    public float textSize() {
        return (float) distToCamera * .5e-3f;
    }

    @Override
    public float textScale() {
        return 0.2f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        out.set(translation);
        double len = out.len();
        out.clamp(0, len - getRadius()).scl(0.9f);

        Vector3d aux = aux3d2.get();
        aux.set(cam.getUp());

        aux.crs(out).nor();

        float dist = -0.015f * (float) out.len();

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
    public boolean isLabel() {
        return true;
    }

    @Override
    public long getCandidateId() {
        return id;
    }

    @Override
    public String getCandidateName() {
        return name;
    }

    @Override
    public boolean isActive() {
        return GaiaSky.instance.isOn(ct) && this.opacity > 0;
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

    @Override
    public boolean withinMagLimit() {
        return true;
    }

    @Override
    public double getCandidateViewAngleApparent() {
        return this.viewAngleApparent;
    }

    @Override
    public float getAppmag() {
        return 0f;
    }

    @Override
    public float getAbsmag() {
        return 0f;
    }

    @Override
    public RotationComponent getRotationComponent() {
        return null;
    }

    @Override
    public Quaterniond getOrientationQuaternion() {
        return null;
    }

    @Override
    public void addHit(int screenX, int screenY, int w, int h, int pxdist, NaturalCamera camera, Array<IFocus> hits) {
        if (withinMagLimit() && isActive()) {
            Vector3 pos = aux3f1.get();
            Vector3d aux = aux3d1.get();
            Vector3d posd = getAbsolutePosition(aux).add(camera.posinv);
            pos.set(posd.valuesf());

            if (camera.direction.dot(posd) > 0) {
                // The star is in front of us
                // Diminish the size of the star
                // when we are close by
                double angle = viewAngle;

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
                double pixelSize = ((angle * pcamera.viewportHeight) / pcamera.fieldOfView) / 2;
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
        if (withinMagLimit() && isActive()) {
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

    @Override
    public void makeFocus() {
    }

    @Override
    public IFocus getFocus(String name) {
        return this;
    }

    @Override
    public boolean isCoordinatesTimeOverflow() {
        return false;
    }

    @Override
    public double getMuAlpha() {
        return pmSph.x;
    }

    @Override
    public double getMuDelta() {
        return pmSph.y;
    }

    @Override
    public double getRadialVelocity() {
        return pmSph.z;
    }

    public int getNStars() {
        return nstars;
    }

    @Override
    public float getTextOpacity(){
        return getOpacity();
    }

    @Override
    public <T extends SceneGraphNode> T getSimpleCopy() {
        StarCluster copy = (StarCluster) super.getSimpleCopy();
        copy.localTransform.set(this.localTransform);
        copy.pm.set(this.pm);
        copy.pmSph.set(this.pmSph);
        copy.labelcolor = this.labelcolor;
        copy.dist = this.dist;
        copy.raddeg = this.raddeg;
        copy.nstars = this.nstars;

        return (T) copy;
    }

}
