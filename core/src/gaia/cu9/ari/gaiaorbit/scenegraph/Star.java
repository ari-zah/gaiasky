package gaia.cu9.ari.gaiaorbit.scenegraph;

import java.util.Map;
import java.util.TreeMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.ModelCache;
import gaia.cu9.ari.gaiaorbit.util.Pair;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.TimeUtils;

/**
 * Represents a star. The Gaia sourceid is put in the id attribute. Otherwise, the id is fabricated.
 * @author tsagrista
 *
 */
public class Star extends Particle {

    /** Has the model used to represent the star **/
    private static ModelComponent mc;
    private static Matrix4 modelTransform;

    /** HIP number, negative if non existent **/
    public int hip = -1;
    /** TYCHO star number, negative if non existent **/
    public int tycho = -1;

    public static void initModel() {
        if (mc == null) {
            Texture tex = new Texture(Gdx.files.internal(GlobalConf.TEXTURES_FOLDER + "star.jpg"));
            Texture lut = new Texture(Gdx.files.internal(GlobalConf.TEXTURES_FOLDER + "lut.jpg"));
            tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

            Map<String, Object> params = new TreeMap<String, Object>();
            params.put("quality", 120l);
            params.put("diameter", 1d);
            params.put("flip", false);

            Pair<Model, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
            Model model = pair.getFirst();
            Material mat = pair.getSecond().get("base");
            mat.clear();
            mat.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
            mat.set(new TextureAttribute(TextureAttribute.Normal, lut));
            // Only to activate view vector (camera position)
            mat.set(new TextureAttribute(TextureAttribute.Specular, lut));
            mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            modelTransform = new Matrix4();
            mc = new ModelComponent(false);
            mc.env = new Environment();
            mc.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
            mc.env.set(new FloatAttribute(FloatAttribute.Shininess, 0f));
            mc.instance = new ModelInstance(model, modelTransform);
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
     * @param pos The position of the star in equatorial cartesian coordinates
     * @param appmag The apparent magnitude
     * @param absmag The absolute magnitude
     * @param colorbv The B-V color index
     * @param name The proper name of the star, if any
     * @param ra in degrees
     * @param dec in degrees 
     * @param starid The star id
     */
    public Star(Vector3d pos, float appmag, float absmag, float colorbv, String name, float ra, float dec, long starid) {
        super(pos, appmag, absmag, colorbv, name, ra, dec, starid);
    }

    /**
     * Creates a new Star object
     * @param pos The position of the star in equatorial cartesian coordinates
     * @param appmag The apparent magnitude
     * @param absmag The absolute magnitude
     * @param colorbv The B-V color index
     * @param name The proper name of the star, if any
     * @param ra in degrees
     * @param dec in degrees 
     * @param starid The star id
     * @param hip The HIP identifier
     * @param source Catalog source. 1: Gaia, 2: HIP, 3: TYC, -1: Unknown
     */
    public Star(Vector3d pos, float appmag, float absmag, float colorbv, String name, float ra, float dec, long starid, int hip, byte source) {
        super(pos, appmag, absmag, colorbv, name, ra, dec, starid);
        this.hip = hip;
        this.catalogSource = source;
    }

    /**
     * Creates a new Star object
     * @param pos The position of the star in equatorial cartesian coordinates
     * @param vel The proper motion of the star in equatorial cartesian coordinates.
     * @param appmag The apparent magnitude
     * @param absmag The absolute magnitude
     * @param colorbv The B-V color index
     * @param name The proper name of the star, if any
     * @param ra in degrees
     * @param dec in degrees 
     * @param starid The star id
     * @param hip The HIP identifier
     * @param tycho The TYC identifier, remove the dashes '-' and join the strings, then parse to integer
     * @param source Catalog source. See {#Particle}
     */
    public Star(Vector3d pos, Vector3 vel, float appmag, float absmag, float colorbv, String name, float ra, float dec, long starid, int hip, byte source) {
        super(pos, vel, appmag, absmag, colorbv, name, ra, dec, starid);
        this.hip = hip;
        this.catalogSource = source;
    }

    /**
     * Creates a new Star object
     * @param pos The position of the star in equatorial cartesian coordinates
     * @param appmag The apparent magnitude
     * @param absmag The absolute magnitude
     * @param colorbv The B-V color index
     * @param name The proper name of the star, if any
     * @param ra in degrees
     * @param dec in degrees 
     * @param starid The star id
     * @param hip The HIP identifier
     * @param tycho The TYC identifier, remove the dashes '-' and join the strings, then parse to integer
     * @param source Catalog source. See {#Particle}
     */
    public Star(Vector3d pos, float appmag, float absmag, float colorbv, String name, float ra, float dec, long starid, int hip, int tycho, byte source) {
        this(pos, appmag, absmag, colorbv, name, ra, dec, starid, hip, source);
        this.tycho = tycho;
    }

    /**
     * Creates a new Star object
     * @param pos The position of the star in equatorial cartesian coordinates
     * @param vel The proper motion of the star in equatorial cartesian coordinates.
     * @param appmag The apparent magnitude
     * @param absmag The absolute magnitude
     * @param colorbv The B-V color index
     * @param name The proper name of the star, if any
     * @param ra in degrees
     * @param dec in degrees 
     * @param starid The star id
     * @param hip The HIP identifier
     * @param tycho The TYC identifier, remove the dashes '-' and join the strings, then parse to integer
     * @param source Catalog source. See {#Particle}
     */
    public Star(Vector3d pos, Vector3 vel, float appmag, float absmag, float colorbv, String name, float ra, float dec, long starid, int hip, int tycho, byte source) {
        this(pos, vel, appmag, absmag, colorbv, name, ra, dec, starid, hip, source);
        this.tycho = tycho;
    }

    /**
     * Creates a new Star object
     * @param pos The position of the star in equatorial cartesian coordinates
     * @param pm The proper motion of the star in equatorial cartesian coordinates
     * @param appmag The apparent magnitude
     * @param absmag The absolute magnitude
     * @param colorbv The B-V color index
     * @param name The proper name of the star, if any
     * @param ra in degrees
     * @param dec in degrees 
     * @param starid The star id
     */
    public Star(Vector3d pos, Vector3 pm, float appmag, float absmag, float colorbv, String name, float ra, float dec, long starid) {
        super(pos, pm, appmag, absmag, colorbv, name, ra, dec, starid);
    }

    @Override
    public void initialize() {
        super.initialize();
        modelDistance = 172.4643429 * radius;
        ct = ComponentType.Stars;
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (camera.getCurrent() instanceof FovCamera) {
            // Only shader for FovCamera
            addToRender(this, RenderGroup.SHADER);
            addToRender(this, RenderGroup.LABEL);
        } else {
            if (viewAngleApparent >= (THRESHOLD_POINT() / 1.5f) * camera.getFovFactor()) {
                addToRender(this, RenderGroup.SHADER);
                if (distToCamera < modelDistance) {
                    camera.checkClosest(this);
                    addToRender(this, RenderGroup.MODEL_S);
                }
                if (pm.len2() != 0)
                    addToRender(this, RenderGroup.LINE);
            }
            if (renderText()) {
                addToRender(this, RenderGroup.LABEL);
            }
        }

    }

    @Override
    public void render(ModelBatch modelBatch, float alpha) {
        mc.setTransparency(alpha);
        ((ColorAttribute) mc.env.get(ColorAttribute.AmbientLight)).color.set(cc[0], cc[1], cc[2], 1f);
        ((FloatAttribute) mc.env.get(FloatAttribute.Shininess)).value = TimeUtils.getRunningTimeSecs();
        // Local transform
        mc.instance.transform.set(transform.getMatrix().valuesf()).scl(getRadius() * 2);
        modelBatch.render(mc.instance, mc.env);
    }

    @Override
    public void doneLoading(AssetManager manager) {
        initModel();
    }

    public String toString() {
        return "Star{" + " name=" + name + " id=" + id + " sph=" + posSph + " pos=" + pos + " absmag=" + absmag + '}';
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

}
