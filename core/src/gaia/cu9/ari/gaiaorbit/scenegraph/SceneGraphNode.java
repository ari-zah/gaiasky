/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Bits;
import com.badlogic.gdx.utils.ObjectMap;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.render.SceneGraphRenderer;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.octreewrapper.AbstractOctreeWrapper;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector2d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.tree.IPosition;

import java.util.Iterator;
import java.util.List;

/**
 * A scene graph entity.
 * 
 * @author Toni Sagrista
 *
 */
public class SceneGraphNode implements IStarContainer, IPosition {
    public static final String ROOT_NAME = "Universe";

    protected static class TLV3D extends ThreadLocal<Vector3d> {
        @Override
        protected Vector3d initialValue() {
            return new Vector3d();
        }
    }

    protected static class TLV3 extends ThreadLocal<Vector3> {
        @Override
        protected Vector3 initialValue() {
            return new Vector3();
        }
    }

    protected static class TLV2D extends ThreadLocal<Vector2d> {
        @Override
        protected Vector2d initialValue() {
            return new Vector2d();
        }
    }

    protected static TLV3D aux3d1 = new TLV3D(), aux3d2 = new TLV3D(), aux3d3 = new TLV3D(), aux3d4 = new TLV3D();
    protected static TLV3 aux3f1 = new TLV3(), aux3f2 = new TLV3(), aux3f3 = new TLV3(), aux3f4 = new TLV3();

    /**
     * Describes to which render group this node belongs at a particular time
     * step.
     */
    public static enum RenderGroup {
        /** Using normal shader for per-pixel lighting **/
        MODEL_NORMAL(0),
        /** Using default shader, no normal map **/
        MODEL_DEFAULT(1),
        /** IntShader - stars **/
        BILLBOARD_STAR(2),
        /** IntShader - galaxies **/
        BILLBOARD_GAL(3),
        /** IntShader - front (planets, satellites...) **/
        BILLBOARD_SSO(4),
        /** Billboard with custom texture **/
        BILLBOARD_TEX(5),
        /** Single pixel **/
        POINT_STAR(6),
        /** Line **/
        LINE(7),
        /** Annotations **/
        FONT_ANNOTATION(8),
        /** Atmospheres of planets **/
        MODEL_ATM(9),
        /** Label **/
        FONT_LABEL(10),
        /** Model star **/
        MODEL_STAR(11),
        /** Galaxy as a whole **/
        GALAXY(12),
        /** Model close up **/
        MODEL_CLOSEUP(13),
        /** Beams **/
        MODEL_BEAM(14),
        /** Particle grup **/
        PARTICLE_GROUP(15),
        /** Star grup **/
        STAR_GROUP(16),
        /** Shapes **/
        SHAPE(17),
        /** Regular billboard sprite **/
        BILLBOARD_SPRITE(18),
        /** Line GPU **/
        LINE_GPU(19),
        /** Particle positions from orbital elements **/
        PARTICLE_ORBIT_ELEMENTS(20),
        /** Transparent meshes **/
        MODEL_MESH(21),
        /** Grids shader **/
        MODEL_GRIDS(22),
        /** Clouds **/
        MODEL_CLOUD(23),
        /** Using normal shader for per-pixel lighting, with additive blending **/
        MODEL_NORMAL_ADDITIVE(24),
        /** Point **/
        POINT(25),
        /** Point GPU **/
        POINT_GPU(26),

        /** Line VR **/
        LINE_VR(50),


        /** None **/
        NONE(-1);

        private int index;

        RenderGroup(int index) {
            this.index = index;
        }

        public boolean is(Bits rgmask) {
            return (index < 0 && rgmask.isEmpty()) || rgmask.get(index);
        }

        /**
         * Adds the given render groups to the given Bits mask
         * 
         * @param rgmask
         *            The bit mask
         * @param rgs
         *            The render groups
         * @return The bits instance
         */
        public static Bits add(Bits rgmask, RenderGroup... rgs) {
            for (RenderGroup rg : rgs) {
                rgmask.set(rg.index);
            }
            return rgmask;
        }

        /**
         * Sets the given Bits mask to the given render groups
         * 
         * @param rgmask
         *            The bit mask
         * @param rgs
         *            The render groups
         * @return The bits instance
         */
        public static Bits set(Bits rgmask, RenderGroup... rgs) {
            rgmask.clear();
            return add(rgmask, rgs);
        }

    }

    /** Reference to scene graph **/
    public static ISceneGraph sg;

    /** The internal identifier **/
    public long id = -1;

    /**
     * The parent entity.
     */
    public SceneGraphNode parent;

    /**
     * List of children entities.
     */
    public Array<SceneGraphNode> children;

    /**
     * Translation object.
     */
    public Vector3d translation;

    /**
     * Local transform matrix. Contains the transform matrix and the
     * transformations that will be applied to this object and not to its
     * children.
     */
    public Matrix4 localTransform;

    /**
     * This transform stores only the orientation of the object. For example in
     * planets, it stores their orientation with respect to their equatorial
     * plane, but not other transformations applied to the object such as the
     * size or the rotation angle at the time.
     */
    public Matrix4d orientation;

    /**
     * The name of the node, if any.
     */
    public String name;

    /**
     * The key to the name in the i18n system.
     */
    protected String namekey = null;

    /**
     * The parent name.
     */
    public String parentName = null;

    /**
     * The key of the parent
     */
    protected String parentkey = null;

    /**
     * The total number of descendants under this node.
     */
    public int numChildren;

    /**
     * Flag indicating whether the object has been computed in this step.
     */
    public boolean computed = true;

    /**
     * The ownOpacity value (alpha)
     */
    public float opacity = 1f;

    /**
     * Component types, for managing visibility
     */
    public ComponentTypes ct;

    public SceneGraphNode() {
        // Identity
        this.translation = new Vector3d();
    }

    public SceneGraphNode(int id) {
        this();
        this.id = id;
    }

    public SceneGraphNode(ComponentType ct) {
        super();
        this.ct = new ComponentTypes(ct);
    }

    public SceneGraphNode(String name, SceneGraphNode parent) {
        this();
        this.name = name;
        this.parent = parent;
    }

    public SceneGraphNode(String name) {
        this(name, null);
    }

    public SceneGraphNode(SceneGraphNode parent) {
        this(null, parent);
    }

    /**
     * Adds the given SceneGraphNode list as children to this node.
     * 
     * @param children
     */
    public final void add(SceneGraphNode... children) {
        if (this.children == null) {
            initChildren(this.parent == null || this instanceof AbstractOctreeWrapper ? 300000 : children.length * 5, this.parent == null ? 1000 : children.length);
        }
        for (int i = 0; i < children.length; i++) {
            SceneGraphNode child = children[i];
            this.children.add(child);
            child.parent = this;
        }
        numChildren += children.length;
    }

    /**
     * Adds a child to the given node and updates the number of children in this
     * node and in all ancestors.
     * 
     * @param child
     *            The child node to add.
     * @param updateAncestorCount
     *            Whether to update the ancestors number of children.
     */
    public final void addChild(SceneGraphNode child, boolean updateAncestorCount) {
        if (this.children == null) {
            initChildren(this.parent == null ? 200 : 5, this.parent == null ? 100 : 1);
        }
        this.children.add(child);
        child.parent = this;
        numChildren++;

        if (updateAncestorCount) {
            // Update num children in ancestors
            SceneGraphNode ancestor = this.parent;
            while (ancestor != null) {
                ancestor.numChildren++;
                ancestor = ancestor.parent;
            }
        }
    }

    /**
     * Removes the given child from this node, if it exists.
     * 
     * @param child
     * @param updateAncestorCount
     */
    public final void removeChild(SceneGraphNode child, boolean updateAncestorCount) {
        if (this.children.contains(child, true)) {
            this.children.removeValue(child, true);
            child.parent = null;
            numChildren--;
            if (updateAncestorCount) {
                // Update num children in ancestors
                SceneGraphNode ancestor = this.parent;
                while (ancestor != null) {
                    ancestor.numChildren--;
                    ancestor = ancestor.parent;
                }
            }
        }
    }

    /**
     * Adds a child to the given node and updates the number of children in this
     * node and in all ancestors.
     * 
     * @param child
     *            The child node to add.
     * @param updateAncestorCount
     *            Whether to update the ancestors number of children.
     * @param numChildren
     *            The number of children this will hold.
     */
    public final void addChild(SceneGraphNode child, boolean updateAncestorCount, int numChildren) {
        if (this.children == null) {
            initChildren(numChildren, 1);
        }
        this.children.add(child);
        child.parent = this;
        numChildren++;

        if (updateAncestorCount) {
            // Update num children in ancestors
            SceneGraphNode ancestor = this.parent;
            while (ancestor != null) {
                ancestor.numChildren++;
                ancestor = ancestor.parent;
            }
        }
    }

    /**
     * Adds the given list of children as child nodes.
     * 
     * @param children
     */
    public void add(List<? extends SceneGraphNode> children) {
        add(children.toArray(new SceneGraphNode[children.size()]));
    }

    /**
     * Inserts the list of nodes under the parents that match each node's name.
     * 
     * @param nodes
     */
    public final void insert(List<? extends SceneGraphNode> nodes) {
        Iterator<? extends SceneGraphNode> it = nodes.iterator();

        // Insert top level
        while (it.hasNext()) {
            SceneGraphNode node = it.next();
            if ((this.name == null && node.parentName == null) || (this.name != null && this.name.equals(node.parentName))) {
                // Match, add and remove from list
                addChild(node, false);
                node.setUp();
                it.remove();
            }
        }

        // Add to children
        if (children != null) {
            for (SceneGraphNode child : children) {
                child.insert(nodes);
            }
        }

    }

    private void initChildren(int size, int grow) {
        children = new Array<SceneGraphNode>(false, size);
    }

    public SceneGraphNode getChildByNameAndType(String name, Class<? extends SceneGraphNode> clazz) {
        int size = children.size;
        for (int i = 0; i < size; i++) {
            SceneGraphNode child = children.get(i);
            if (child.getName().equalsIgnoreCase(name) && clazz.isInstance(child)) {
                return child;
            }
        }
        return null;
    }

    public SceneGraphNode getChildByName(String name) {
        int size = children.size;
        for (int i = 0; i < size; i++) {
            SceneGraphNode child = children.get(i);
            if (child.getName().equalsIgnoreCase(name)) {
                return child;
            }
        }
        return null;
    }

    public Array<SceneGraphNode> getChildrenByType(Class<? extends SceneGraphNode> clazz, Array<SceneGraphNode> list) {
        if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                if (clazz.isInstance(child))
                    list.add(child);

                child.getChildrenByType(clazz, list);
            }
        }
        return list;
    }

    public SceneGraphNode getNode(String name) {
        if (this.name != null && this.name.equals(name)) {
            return this;
        } else if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                SceneGraphNode n = child.getNode(name);
                if (n != null) {
                    return n;
                }
            }
        }
        return null;
    }

    public SceneGraphNode getNode(int id) {
        if (this.id >= 0 && this.id == id) {
            return this;
        } else if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                SceneGraphNode n = child.getNode(id);
                if (n != null) {
                    return n;
                }
            }
        }
        return null;
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera) {
        update(time, parentTransform, camera, 1f);
    }

    public void update(ITimeFrameProvider time, final Vector3d parentTransform, ICamera camera, float opacity) {
        this.opacity = opacity;
        translation.set(parentTransform);

        // Update with translation/rotation/etc
        updateLocal(time, camera);

        if (children != null) {
            for (int i = 0; i < children.size; i++) {
                children.get(i).update(time, translation, camera, opacity);
            }
        }
    }

    /**
     * Updates the transform matrix with the transformations that will apply to
     * the children and the local transform matrix with the transformations that
     * will apply only to this object.
     * 
     * @param time
     */
    protected void updateLocal(ITimeFrameProvider time, ICamera camera) {
    }

    public void initialize() {
        if (ct == null)
            ct = new ComponentTypes(ComponentType.Others.ordinal());
    }

    public void doneLoading(AssetManager manager) {
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setNamekey(String namekey) {
        this.namekey = namekey;
        updateNames();
    }

    /**
     * Updates the name using the key. This must be called when the language
     * changes.
     */
    public void updateNames() {
        if (namekey != null)
            this.name = I18n.bundle.get(namekey);
        if (parentkey != null)
            this.parentName = I18n.bundle.get(parentkey);
    }

    /**
     * Recursively updates the name using the key. This must be called when the
     * language changes.
     */
    public void updateNamesRec() {
        this.updateNames();
        if (children != null && children.size > 0) {
            for (SceneGraphNode node : children)
                node.updateNamesRec();
        }
    }

    public void setId(Long id){
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setParent(String parentName) {
        this.parentName = parentName;
    }

    public void setParentkey(String parentkey) {
        this.parentkey = parentkey;
        this.updateNames();
    }

    public void dispose() {
        if (children != null)
            for (SceneGraphNode child : children) {
                child.dispose();
            }
    }

    /**
     * Adds all the children that are focusable objects to the list.
     * 
     * @param list
     */
    public void addFocusableObjects(Array<IFocus> list) {
        if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                child.addFocusableObjects(list);
            }
        }
    }

    public void addNodes(Array<SceneGraphNode> nodes) {
        nodes.add(this);
        if (children != null) {
            int size = children.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode child = children.get(i);
                child.addNodes(nodes);
            }
        }
    }

    public void setUp() {

    }

    public void setCt(String ct) {
        this.ct = new ComponentTypes(ComponentType.valueOf(ct));
    }

    public void setCt(String[] cts) {
        this.ct = new ComponentTypes();
        for (int i = 0; i < cts.length; i++) {
            this.ct.set(ComponentType.valueOf(cts[i]).ordinal());
        }
    }

    public ComponentTypes getCt() {
        return ct;
    }

    public ComponentTypes getComponentType() {
        return ct;
    }

    /**
     * Gets the number of nodes contained in this node, including itself
     * 
     * @return The number of children of this node and its descendents
     */
    public int getAggregatedChildren() {
        return numChildren + 1;
    }

    public <T extends SceneGraphNode> T getLineCopy() {
        if (this.parent != null) {
            T parentCopy = parent.getLineCopy();
            T me = getSimpleCopy();
            parentCopy.addChild(me, false, 1);
            return me;
        } else {
            return getSimpleCopy();
        }

    }

    /**
     * Gets a copy of this object but does not copy its parent or children
     * 
     * @return The copied object
     */
    public <T extends SceneGraphNode> T getSimpleCopy() {
        T copy = null;
        try {
            copy = (T) this.getClass().newInstance();
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        copy.name = this.name;
        copy.parentName = this.parentName;
        return copy;
    }

    public SceneGraphNode getRoot() {
        if (this.parent == null) {
            return this;
        } else {
            return this.parent.getRoot();
        }
    }

    @Override
    public String toString() {
        if (name != null)
            return name;
        return super.toString();
    }

    public void returnToPool() {
        // if (this.children != null) {
        // for (SceneGraphNode child : children)
        // child.returnToPool();
        // this.children.clear();
        // }
        // Class clazz = this.getClass();
        // MyPools.get(clazz).free(this);
    }

    /**
     * Sets the computed flag of the list of nodes and their children to the
     * given value.
     * 
     * @param nodes
     *            List of nodes to set the flag to. May be null.
     * @param computed
     *            The computed value.
     */
    public void setComputedFlag(Array<SceneGraphNode> nodes, boolean computed) {
        if (nodes != null) {
            int size = nodes.size;
            for (int i = 0; i < size; i++) {
                SceneGraphNode node = nodes.get(i);
                node.computed = computed;
                setComputedFlag(node.children, computed);
            }
        }
    }

    /**
     * Adds the given renderable to the given render group list
     * 
     * @param renderable
     *            The renderable to add
     * @param rg
     *            The render group that identifies the renderable list
     * @return True if added, false otherwise
     */
    protected boolean addToRender(IRenderable renderable, RenderGroup rg) {
        boolean on = ct.intersects(SceneGraphRenderer.visible);
        if (on || SceneGraphRenderer.alphas[ct.getFirstOrdinal()] > 0) {
            SceneGraphRenderer.render_lists.get(rg.ordinal()).add(renderable);
            return true;
        }
        return false;
    }

    /**
     * Removes the given renderable from the given render group list.
     * 
     * @param renderable
     *            The renderable to remove
     * @param rg
     *            The render group to remove from
     * @return True if removed, false otherwise
     */
    protected boolean removeFromRender(IRenderable renderable, RenderGroup rg) {
        return SceneGraphRenderer.render_lists.get(rg.ordinal()).removeValue(renderable, true);
    }

    protected boolean isInRender(IRenderable renderable, RenderGroup rg) {
        return SceneGraphRenderer.render_lists.get(rg.ordinal()).contains(renderable, true);
    }

    /**
     * Gets the first ancestor of this node that is of type {@link Star}
     * 
     * @return The first ancestor of type {@link Star}
     */
    public SceneGraphNode getFirstStarAncestor() {
        if (this instanceof Star) {
            return this;
        } else if (parent != null) {
            return parent.getFirstStarAncestor();
        } else {
            return null;
        }
    }

    @Override
    public int getStarCount() {
        return 0;
    }

    @Override
    public Vector3d getPosition() {
        return null;
    }

    @Override
    public Vector3d getVelocity() {
        return null;
    }

    public Matrix4d getOrientation() {
        return orientation;
    }

    public boolean isVisibilityOn() {
        return GaiaSky.instance.isOn(ct);
    }

    public float getOpacity() {
        return opacity;
    }

    public int getSceneGraphDepth() {
        if (this.parent == null) {
            return 0;
        } else {
            return this.parent.getSceneGraphDepth() + 1;
        }
    }
   
    /**
     * Special actions to be taken for this object when adding to the index.
     * @param map The index
     */
    protected void addToIndex(ObjectMap<String, SceneGraphNode> map) {
    }

    /**
     * Special actions to be taken for this object when removing from the index. Must implement if addToIndex is implemented
     * @param map The index
     */
    protected void removeFromIndex(ObjectMap<String, SceneGraphNode> map) {
    }

    /**
     * Whether to add this node to the index
     * @return True if the node needs to be added to the index.
     */
    public boolean mustAddToIndex() {
        return true;
    }
}
