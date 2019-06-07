/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectMap;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.FovCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.ModelCache;
import gaia.cu9.ari.gaiaorbit.util.Pair;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModel;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModelInstance;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a star. The Gaia sourceid is put in the id attribute. Otherwise,
 * the id is fabricated.
 * 
 * @author tsagrista
 *
 */
public class Star extends Particle {

    /** Has the model used to represent the star **/
    private static ModelComponent mc;
    private static Matrix4 modelTransform;

    /** HIP number, negative if non existent **/
    public int hip = -1;
    /** TYCHO2 identifier string **/
    public String tycho = null;

    public static void initModel() {
        if (mc == null) {
            Texture tex = new Texture(GlobalConf.data.dataFile("tex/base/star.jpg"));
            Texture lut = new Texture(GlobalConf.data.dataFile("tex/base/lut.jpg"));
            tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

            Map<String, Object> params = new TreeMap<>();
            params.put("quality", 120l);
            params.put("diameter", 1d);
            params.put("flip", false);

            Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
            IntModel model = pair.getFirst();
            Material mat = pair.getSecond().get("base");
            mat.clear();
            mat.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
            mat.set(new TextureAttribute(TextureAttribute.Normal, lut));
            // Only to activate view vector (camera position)
            mat.set(new TextureAttribute(TextureAttribute.Specular, lut));
            mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            modelTransform = new Matrix4();
            mc = new ModelComponent(false);
            mc.initialize();
            mc.env = new Environment();
            mc.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
            mc.env.set(new FloatAttribute(FloatAttribute.Shininess, 0f));
            mc.instance = new IntModelInstance(model, modelTransform);
            // Relativistic effects
            if (GlobalConf.runtime.RELATIVISTIC_ABERRATION)
                mc.rec.setUpRelativisticEffectsMaterial(mc.instance.materials);
        }
    }

    double modelDistance;

    public Star() {
        this.parentName = ROOT_NAME;
    }

    public Star(Vector3d pos, float appmag, float absmag, float colorbv, String name, long starid) {
        super(pos, appmag, absmag, colorbv, name, starid);
    }

    /**
     * Creates a new Star object
     * 
     * @param pos
     *            The position of the star in equatorial cartesian coordinates
     * @param appmag
     *            The apparent magnitude
     * @param absmag
     *            The absolute magnitude
     * @param colorbv
     *            The B-V color index
     * @param name
     *            The proper name of the star, if any
     * @param ra
     *            in degrees
     * @param dec
     *            in degrees
     * @param starid
     *            The star id
     */
    public Star(Vector3d pos, float appmag, float absmag, float colorbv, String name, float ra, float dec, long starid) {
        super(pos, appmag, absmag, colorbv, name, ra, dec, starid);
    }

    /**
     * Creates a new Star object
     * 
     * @param pos
     *            The position of the star in equatorial cartesian coordinates
     * @param appmag
     *            The apparent magnitude
     * @param absmag
     *            The absolute magnitude
     * @param colorbv
     *            The B-V color index
     * @param name
     *            The proper name of the star, if any
     * @param ra
     *            in degrees
     * @param dec
     *            in degrees
     * @param starid
     *            The star id
     * @param hip
     *            The HIP identifier
     * @param source
     *            Catalog source. 1: Gaia, 2: HIP, 3: TYC, -1: Unknown
     */
    public Star(Vector3d pos, float appmag, float absmag, float colorbv, String name, float ra, float dec, long starid, int hip, byte source) {
        super(pos, appmag, absmag, colorbv, name, ra, dec, starid);
        this.hip = hip;
        this.catalogSource = source;
    }

    /**
     * Creates a new Star object
     * 
     * @param pos
     *            The position of the star in equatorial cartesian coordinates
     * @param pm
     *            The proper motion of the star in equatorial cartesian
     *            coordinates.
     * @param pmSph
     *            The proper motion with mualpha, mudelta, radvel.
     * @param appmag
     *            The apparent magnitude
     * @param absmag
     *            The absolute magnitude
     * @param colorbv
     *            The B-V color index
     * @param name
     *            The proper name of the star, if any
     * @param ra
     *            in degrees
     * @param dec
     *            in degrees
     * @param starid
     *            The star id
     * @param hip
     *            The HIP identifier
     * @param source
     *            Catalog source. See {#Particle}
     */
    public Star(Vector3d pos, Vector3 pm, Vector3 pmSph, float appmag, float absmag, float colorbv, String name, float ra, float dec, long starid, int hip, String tycho, byte source) {
        super(pos, pm, pmSph, appmag, absmag, colorbv, name, ra, dec, starid);
        this.hip = hip;
        this.catalogSource = source;
        this.tycho = tycho;
    }

    /**
     * Creates a new Star object
     * 
     * @param pos
     *            The position of the star in equatorial cartesian coordinates
     * @param appmag
     *            The apparent magnitude
     * @param absmag
     *            The absolute magnitude
     * @param colorbv
     *            The B-V color index
     * @param name
     *            The proper name of the star, if any
     * @param ra
     *            in degrees
     * @param dec
     *            in degrees
     * @param starid
     *            The star id
     * @param hip
     *            The HIP identifier
     * @param tycho
     *            The TYC identifier
     * @param source
     *            Catalog source. See {#Particle}
     */
    public Star(Vector3d pos, float appmag, float absmag, float colorbv, String name, float ra, float dec, long starid, int hip, String tycho, byte source) {
        this(pos, appmag, absmag, colorbv, name, ra, dec, starid, hip, source);
        this.tycho = tycho;
    }

    /**
     * Creates a new Star object
     * 
     * @param pos
     *            The position of the star in equatorial cartesian coordinates
     * @param pm
     *            The proper motion of the star in equatorial cartesian
     *            coordinates
     * @param pmSph
     *            The proper motion with mualpha, mudelta, radvel.
     * @param appmag
     *            The apparent magnitude
     * @param absmag
     *            The absolute magnitude
     * @param colorbv
     *            The B-V color index
     * @param name
     *            The proper name of the star, if any
     * @param ra
     *            in degrees
     * @param dec
     *            in degrees
     * @param starid
     *            The star id
     */
    public Star(Vector3d pos, Vector3 pm, Vector3 pmSph, float appmag, float absmag, float colorbv, String name, float ra, float dec, long starid) {
        super(pos, pm, pmSph, appmag, absmag, colorbv, name, ra, dec, starid);
    }

    @Override
    public void initialize() {
        super.initialize();
        modelDistance = 172.4643429 * radius;
        ct = new ComponentTypes(ComponentType.Stars);
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (camera.getCurrent() instanceof FovCamera) {
            // Render as point, do nothing
            addToRender(this, RenderGroup.BILLBOARD_STAR);
        } else {
            if (viewAngleApparent >= thpointTimesFovfactor) {
                addToRender(this, RenderGroup.BILLBOARD_STAR);
                if (distToCamera < modelDistance) {
                    camera.checkClosest(this);
                    addToRender(this, RenderGroup.MODEL_STAR);
                    if (GlobalConf.program.CUBEMAP360_MODE)
                        removeFromRender(this, RenderGroup.BILLBOARD_STAR);
                }
            }
            if (this.hasPm && viewAngleApparent >= thpointTimesFovfactor / GlobalConf.scene.PM_NUM_FACTOR) {
                addToRender(this, RenderGroup.LINE);
            }
        }

        if ((renderText() || camera.getCurrent() instanceof FovCamera)) {
            addToRender(this, RenderGroup.FONT_LABEL);
        }

    }

    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc) {
        mc.touch();
        float opac = 1;
        if (!GlobalConf.program.CUBEMAP360_MODE)
            opac = (float) MathUtilsd.lint(distToCamera, modelDistance / 50f, modelDistance, 1f, 0f);
        mc.setTransparency(alpha * opac);
        float[] col = GlobalConf.scene.STAR_COLOR_TRANSIT ? ccTransit : cc;
        ((ColorAttribute) mc.env.get(ColorAttribute.AmbientLight)).color.set(col[0], col[1], col[2], 1f);
        ((FloatAttribute) mc.env.get(FloatAttribute.Shininess)).value = (float) t;
        // Local transform
        translation.getMatrix(mc.instance.transform).scl((float) (getRadius() * 2d));
        mc.updateRelativisticEffects(GaiaSky.instance.getICamera());
        modelBatch.render(mc.instance, mc.env);
    }

    @Override
    public void doneLoading(AssetManager manager) {
        initModel();
    }

    public String toString() {
        return "Star{" + " name=" + name + " id=" + id + " sph=" + posSph + " pos=" + pos + " appmag=" + appmag + '}';
    }

    @Override
    public double getPmX() {
        return pm.x;
    }

    @Override
    public double getPmY() {
        return pm.y;
    }

    @Override
    public double getPmZ() {
        return pm.z;
    }

    @Override
    protected double computeViewAngle(float fovFactor) {
        if (viewAngle > Constants.THRESHOLD_DOWN / fovFactor && viewAngle < Constants.THRESHOLD_UP / fovFactor) {
            return 20f * Constants.THRESHOLD_DOWN / fovFactor;
        }
        return viewAngle;
    }

    @Override
    public int getHip() {
        return hip;
    }

    @Override
    protected void addToIndex(ObjectMap<String, SceneGraphNode> map) {
        // Hip
        if (hip > 0) {
            String hipid = "hip " + hip;
            map.put(hipid, this);
        }
        // Tycho
        if (tycho != null && !tycho.isEmpty()) {
            map.put(tycho, this);
        }
    }

    @Override
    protected void removeFromIndex(ObjectMap<String, SceneGraphNode> map) {
        // Hip
        if (hip > 0) {
            String hipid = "hip " + hip;
            map.remove(hipid);
        }
        // Tycho
        if (tycho != null && !tycho.isEmpty()) {
            map.remove(tycho);
        }
    }
    public float getFuzzyRenderSize(ICamera camera) {
        return super.getFuzzyRenderSize(camera);
    }

}
