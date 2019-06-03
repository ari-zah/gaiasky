/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;

public abstract class ImmediateRenderSystem extends AbstractRenderSystem {
    protected static final Log logger = Logger.getLogger(ImmediateRenderSystem.class);

    protected int meshIdx;
    protected Array<MeshData> meshes;
    protected MeshData curr;
    // Auxiliary array that holds vertices temporarily
    protected float[] tempVerts;

    protected class MeshData {

        protected IntMesh mesh;

        protected int colorOffset;

        protected int vertexIdx;
        // Size of each vertex in number of entries in array. Multiply by array type
        // size to get vertex size in bytes.
        protected int vertexSize;
        // Vertex array, this usually is just a reference to an external temp array
        protected float[] vertices;

        protected int indexIdx;
        protected int indexVert;
        protected int[] indices;
        protected int numVertices;
        protected int capacity;

        public void clear() {
            vertexIdx = 0;
            indexIdx = 0;
            indexVert = 0;
            numVertices = 0;
        }

        public void dispose() {
            mesh.dispose();
            vertices = null;
            indices = null;
        }
    }

    /**
     * Creates a new mesh data object and adds it to the first available index
     * in the meshes array
     *
     * @return The index of the new mesh data in meshes
     */
    protected int createMeshData() {
        // look for index
        int mdi;
        for (mdi = 0; mdi < meshes.size; mdi++) {
            if (meshes.get(mdi) == null) {
                break;
            }
        }

        if (mdi >= meshes.size) {
            meshes.setSize(mdi + 1);
        }

        meshes.set(mdi, new MeshData());
        return mdi;
    }

    /**
     * Clears the mesh data at the index i
     *
     * @param i The index of the mesh data to remove
     */
    public void clearMeshData(int i) {
        try {
            MeshData md = meshes.get(i);
            if (md != null && md.mesh != null) {
                md.dispose();
                meshes.set(i, null);
            }
        }catch(IndexOutOfBoundsException e){
            // Nothing
        }
    }

    protected ImmediateRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] programs) {
        this(rg, alphas, programs, -1);
    }

    protected ImmediateRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] programs, int tempVertsSize) {
        super(rg, alphas, programs);
        initShaderProgram();
        initVertices();
        meshIdx = 0;
        if (tempVertsSize > 0)
            tempVerts = new float[tempVertsSize];
    }

    protected abstract void initShaderProgram();

    protected abstract void initVertices();

    public void dispose(){
        if(meshes != null) {
            for (int i = 0; i < meshes.size; i++) {
                MeshData md = meshes.get(i);
                if (md != null) {
                    md.dispose();
                }
            }
            meshes.clear();
        }
        tempVerts = null;
        curr = null;
    }

    /**
     * This function makes sure that the tempVerts array has at least
     * the given size. After calling this function, the elements of tempVerts
     * may have been cleared.
     * @param size The size to ensure
     */
    protected void ensureTempVertsSize(int size){
        if(tempVerts == null || tempVerts.length < size) {
            tempVerts = new float[size];
        }
    }

    public void color(Color color) {
        curr.vertices[curr.vertexIdx + curr.colorOffset] = color.toFloatBits();
    }

    public void color(float r, float g, float b, float a) {
        curr.vertices[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(r, g, b, a);
    }

    public void color(double r, double g, double b, double a) {
        curr.vertices[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits((float) r, (float) g, (float) b, (float) a);
    }

    public void color(float colorBits) {
        curr.vertices[curr.vertexIdx + curr.colorOffset] = colorBits;
    }

    public void vertex(float x, float y, float z) {
        curr.vertices[curr.vertexIdx] = x;
        curr.vertices[curr.vertexIdx + 1] = y;
        curr.vertices[curr.vertexIdx + 2] = z;

        curr.vertexIdx += curr.vertexSize;
        curr.numVertices++;
    }

}
