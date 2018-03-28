package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import gaia.cu9.ari.gaiaorbit.render.I3DTextRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext.CubemapSide;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.DecalUtils;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.coord.IBodyCoordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;
import net.jafama.FastMath;

/**
 * A base abstract graphical entity with the basics.
 * 
 * @author Toni Sagrista
 *
 */
public abstract class AbstractPositionEntity extends SceneGraphNode {
    /**
     * Overlap factor applied to angle to get the upper boundary when rendering
     * with shader and model.
     */
    public static final float SHADER_MODEL_OVERLAP_FACTOR = 5;
    /**
     * Position of this entity in the local reference system. The units are
     * {@link gaia.cu9.ari.gaiaorbit.util.Constants#U_TO_KM} by default.
     */
    public Vector3d pos;

    /**
     * Coordinates provider. Helps updating the position at each time step.
     **/
    protected IBodyCoordinates coordinates;

    /**
     * Position in the equatorial system; ra, dec.
     */
    public Vector2 posSph;

    /**
     * Size factor in internal units.
     */
    public float size;

    /**
     * The distance to the camera from the focus center.
     */
    public double distToCamera;

    /**
     * The view angle, in radians.
     */
    public double viewAngle;

    /**
     * The view angle corrected with the field of view angle, in radians.
     */
    public double viewAngleApparent;

    /**
     * Base color
     */
    public float[] cc;

    /**
     * Is this just a copy?
     */
    protected boolean copy = false;

    /** The id of the octant it belongs to, if any **/
    public Long octantId;

    /** Its page **/
    public OctreeNode octant;

    protected AbstractPositionEntity() {
        super();
        // Positions
        pos = new Vector3d();
        posSph = new Vector2();
    }

    public AbstractPositionEntity(SceneGraphNode parent) {
        super(parent);
        // Positions
        pos = new Vector3d();
        posSph = new Vector2();
    }

    public AbstractPositionEntity(String name) {
        super(name);
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);

        if (coordinates != null)
            coordinates.doneLoading(sg, this);
    }

    /**
     * Returns the position of this entity in the internal reference system.
     * 
     * @param aux
     *            The vector where the result will be put
     * @return The aux vector with the position
     */
    public Vector3d getPosition(Vector3d aux) {
        return aux.set(pos);
    }

    /**
     * Gets the predicted position of this entity in the next time step in the
     * internal reference system using the given time provider and the given
     * camera.
     * 
     * @param aux
     *            The out vector where the result will be stored.
     * @param time
     *            The time frame provider.
     * @param camera
     *            The camera.
     * @param force
     *            Whether to force the computation if time is off.
     * @return The aux vector for chaining.
     */
    public Vector3d getPredictedPosition(Vector3d aux, ITimeFrameProvider time, ICamera camera, boolean force) {
        if (!mustUpdatePosition(time) && !force) {
            return getAbsolutePosition(aux);
        } else {
            // Get copy of focus and update it to know where it will be in the
            // next step
            AbstractPositionEntity fc = (AbstractPositionEntity) this;
            AbstractPositionEntity fccopy = fc.getLineCopy();
            SceneGraphNode root = fccopy.getRoot();
            root.transform.position.set(camera.getInversePos());
            root.update(time, root.transform, camera);

            fccopy.getAbsolutePosition(aux);

            // Return to poolvec
            SceneGraphNode ape = fccopy;
            do {
                ape.returnToPool();
                ape = ape.parent;
            } while (ape != null);

            return aux;
        }
    }

    /**
     * Whether position must be recomputed for this entity. By default, only
     * when time is on
     * 
     * @param time
     *            The current time
     * @return True if position should be recomputed for this entity
     */
    protected boolean mustUpdatePosition(ITimeFrameProvider time) {
        return time.getDt() != 0;
    }

    /**
     * Returns the absolute position of this entity in the native coordinates
     * (equatorial system)
     * 
     * @param aux
     *            Auxiliary vector to put the result in
     * @return The vector with the position
     */
    public Vector3d getAbsolutePosition(Vector3d aux) {
        aux.set(pos);
        AbstractPositionEntity entity = this;
        while (entity.parent != null && entity.parent instanceof AbstractPositionEntity) {
            entity = (AbstractPositionEntity) entity.parent;
            aux.add(entity.pos);
        }
        return aux;
    }

    public Matrix4d getAbsoluteOrientation(Matrix4d aux) {
        aux.set(orientation);
        AbstractPositionEntity entity = this;
        while (entity.parent != null && entity.parent instanceof AbstractPositionEntity) {
            entity = (AbstractPositionEntity) entity.parent;
            if (entity.orientation != null)
                aux.mul(entity.orientation);
        }
        return aux;
    }

    /**
     * Updates the local transform matrix.
     * 
     * @param time
     */
    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        updateLocalValues(time, camera);
        if (this.name.equals("Solar sail")) {
            pos.set(0, 0, 0.0012125);
            size = (float) (10 * Constants.M_TO_U);
        }
        this.transform.translate(pos);

        Vector3d aux = aux3d1.get();
        this.distToCamera = (float) transform.getTranslation(aux).len();
        this.viewAngle = (float) FastMath.atan(size / distToCamera);
        this.viewAngleApparent = this.viewAngle;
        if (!copy) {
            addToRenderLists(camera);
        }
    }

    /**
     * Adds this entity to the necessary render lists after the distance to the
     * camera and the view angle have been determined.
     */
    protected abstract void addToRenderLists(ICamera camera);

    /**
     * This function updates all the local values before the localTransform is
     * updated. Position, rotations and scale must be updated in here.
     * 
     * @param time
     * @param camera
     */
    public abstract void updateLocalValues(ITimeFrameProvider time, ICamera camera);

    /**
     * Returns the radius in internal units
     * 
     * @return The radius of the object, in internal units
     */
    public double getRadius() {
        return size / 2d;
    }

    /**
     * Returns the size (diameter) of this entity in internal units.
     * 
     * @return The size in internal units.
     */
    public double getSize() {
        return size;
    }

    /**
     * Sets the absolute size (diameter) of this entity
     * 
     * @param size
     *            The diameter in internal units
     */
    public void setSize(Double size) {
        this.size = size.floatValue();
    }

    /**
     * Sets the absolute size (diameter) of this entity
     * 
     * @param size
     *            The diameter in internal units
     */
    public void setSize(Long size) {
        this.size = (float) size;
    }

    public Vector2 getPosSph() {
        return posSph;
    }

    public double getAlpha() {
        return posSph.x;
    }

    public double getDelta() {
        return posSph.y;
    }

    public void setColor(double[] color) {
        this.cc = GlobalResources.toFloatArray(color);
    }

    public Vector3d computeFuturePosition() {
        return null;
    }

    /**
     * Gets a copy of this object but does not copy its parent or children
     * 
     * @return The copied object
     */
    @Override
    public <T extends SceneGraphNode> T getSimpleCopy() {
        Class<? extends AbstractPositionEntity> clazz = this.getClass();
        try {
            AbstractPositionEntity instance = clazz.newInstance();
            instance.copy = true;
            instance.name = this.name;
            instance.pos.set(this.pos);
            instance.size = this.size;
            instance.distToCamera = this.distToCamera;
            instance.viewAngle = this.viewAngle;
            instance.transform.set(this.transform);
            instance.ct = this.ct;
            instance.coordinates = this.coordinates;
            if (this.localTransform != null)
                instance.localTransform.set(this.localTransform);

            return (T) instance;
        } catch (Exception e) {
            Logger.error(e);
        }
        return null;
    }

    public AbstractPositionEntity getComputedAncestor() {
        if (!this.computed) {
            return this.parent != null && this.parent instanceof AbstractPositionEntity ? ((AbstractPositionEntity) this.parent).getComputedAncestor() : null;
        } else {
            return this;
        }
    }

    /**
     * Returns the current distance to the camera in internal units.
     * 
     * @return The current distance to the camera, in internal units.
     */
    public double getDistToCamera() {
        return distToCamera;
    }

    /**
     * Returns the current view angle of this entity, in radians.
     * 
     * @return The view angle in radians.
     */
    public double getViewAngle() {
        return viewAngle;
    }

    /**
     * Returns the current apparent view angle (view angle corrected with the
     * field of view) of this entity, in radians.
     * 
     * @return The apparent view angle in radians.
     */
    public double getViewAngleApparent() {
        return viewAngleApparent;
    }

    protected void render2DLabel(SpriteBatch batch, ShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, Vector3d pos3d) {
        Vector3 p = aux3f1.get();
        pos3d.setVector3(p);

        camera.getCamera().project(p);
        p.x += 15;
        p.y -= 15;

        shader.setUniformf("scale", 1f);
        DecalUtils.drawFont2D(font, batch, label, p);
    }

    protected void render2DLabel(SpriteBatch batch, ShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, float x, float y) {
        render2DLabel(batch, shader, rc, font, camera, label, x, y, 1f);
    }

    protected void render2DLabel(SpriteBatch batch, ShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, float x, float y, float scale) {
        render2DLabel(batch, shader, rc, font, camera, label, x, y, scale, -1);
    }

    protected void render2DLabel(SpriteBatch batch, ShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, float x, float y, float scale, int align) {
        shader.setUniformf("u_scale", scale);
        DecalUtils.drawFont2D(font, batch, rc, label, x, y, scale, align);
    }

    protected void render3DLabel(SpriteBatch batch, ShaderProgram shader, BitmapFont font, ICamera camera, RenderingContext rc, String label, Vector3d pos, float scale, float size) {
        // The smoothing scale must be set according to the distance
        shader.setUniformf("u_scale", scale / camera.getFovFactor());

        double len = pos.len();
        pos.clamp(0, len - size);

        // Enable or disable blending
        ((I3DTextRenderable) this).textDepthBuffer();

        float rot = 0;
        if (rc.cubemapSide == CubemapSide.SIDE_UP || rc.cubemapSide == CubemapSide.SIDE_DOWN) {
            Vector3 v1 = aux3f1.get();
            Vector3 v2 = aux3f2.get();
            camera.getCamera().project(v1.set((float) pos.x, (float) pos.y, (float) pos.z));
            v1.z = 0;
            v2.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, 0);
            rot = GlobalResources.angle2d(v1, v2) + (rc.cubemapSide == CubemapSide.SIDE_UP ? 90 : -90);
        }

        DecalUtils.drawFont3D(font, batch, label, (float) pos.x, (float) pos.y, (float) pos.z, size, rot, camera.getCamera(), true);
    }

    public void setCoordinates(IBodyCoordinates coord) {
        coordinates = coord;
    }

    @Override
    public Vector3d getPosition() {
        return pos;
    }

}
