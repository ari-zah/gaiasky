/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.data.util.PointCloudData;
import gaia.cu9.ari.gaiaorbit.render.IGPUVertsRenderable;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.Gaia;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import org.lwjgl.opengl.GL11;

/**
 * Renders vertices using a VBO.
 *
 * @author tsagrista
 */
public class VertGPURenderSystem<T extends IGPUVertsRenderable> extends ImmediateRenderSystem {
    protected ICamera camera;
    protected int glType;

    public VertGPURenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] shaders, int glType) {
        super(rg, alphas, shaders);
        this.glType = glType;
    }

    public boolean isLine() {
        return glType == GL20.GL_LINE_STRIP || glType == GL20.GL_LINES;
    }

    public boolean isPoint() {
        return glType == GL20.GL_POINTS;
    }

    @Override
    protected void initShaderProgram() {
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
    }

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index
     *
     * @param nVertices The max number of vertices this mesh data can hold
     * @return The index of the new mesh data
     */
    private int addMeshData(int nVertices) {
        int mdi = createMeshData();
        curr = meshes.get(mdi);

        VertexAttribute[] attribs = buildVertexAttributes();
        curr.mesh = new IntMesh(false, nVertices, 0, attribs);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        return mdi;
    }


    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (isLine()) {
            // Enable GL_LINE_SMOOTH
            Gdx.gl20.glEnable(GL11.GL_LINE_SMOOTH);
            Gdx.gl.glHint(GL20.GL_NICEST, GL11.GL_LINE_SMOOTH_HINT);
            // Enable GL_LINE_WIDTH
            Gdx.gl20.glEnable(GL20.GL_LINE_WIDTH);
        }

        this.camera = camera;
        int size = renderables.size;

        for (int i = 0; i < size; i++) {
            T renderable = (T) renderables.get(i);

            /*
             * ADD LINES
             */
            if (!renderable.inGpu()) {
                // Remove previous line data if present
                if (renderable.getOffset() >= 0) {
                    clearMeshData(renderable.getOffset());
                    renderable.setOffset(-1);
                }

                // Actually add data
                PointCloudData od = renderable.getPointCloud();
                int nPoints = od.getNumPoints();

                // Initialize or fetch mesh data
                if (renderable.getOffset() < 0) {
                    renderable.setOffset(addMeshData(nPoints));
                } else {
                    curr = meshes.get(renderable.getOffset());
                    // Check we still have capacity, otherwise, reinitialize.
                    if (curr.numVertices != od.getNumPoints()) {
                        curr.clear();
                        curr.mesh.dispose();
                        meshes.set(renderable.getOffset(), null);
                        renderable.setOffset(addMeshData(nPoints));
                    }
                }

                // Ensure vertices capacity
                ensureTempVertsSize((nPoints + 2) * curr.vertexSize);
                curr.vertices = tempVerts;
                float[] cc = renderable.getColor();
                for (int point_i = 0; point_i < nPoints; point_i++) {
                    color(cc[0], cc[1], cc[2], 1.0);
                    vertex((float) od.getX(point_i), (float) od.getY(point_i), (float) od.getZ(point_i));
                }

                // Close loop
                if (renderable.isClosedLoop()) {
                    color(cc[0], cc[1], cc[2], 1.0);
                    vertex((float) od.getX(0), (float) od.getY(0), (float) od.getZ(0));
                }

                renderable.setCount(nPoints * curr.vertexSize);
                curr.mesh.setVertices(curr.vertices, 0, renderable.getCount());
                curr.vertices = null;
                renderable.setInGpu(true);
            }
            curr = meshes.get(renderable.getOffset());

            /*
             * RENDER
             */
            ShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();

            renderable.blend();
            renderable.depth();

            // Regular
            if (isLine())
                Gdx.gl.glLineWidth(renderable.getPrimitiveSize() * GlobalConf.SCALE_FACTOR);
            if (isPoint())
                shaderProgram.setUniformf("u_pointSize", renderable.getPrimitiveSize() * GlobalConf.SCALE_FACTOR);

            shaderProgram.setUniformMatrix("u_worldTransform", renderable.getLocalTransform());
            shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
            shaderProgram.setUniformf("u_alpha", (float) (renderable.getAlpha()) * getAlpha(renderable));
            if (renderable.getParent() != null && renderable.getParent().name.equals("Gaia")) {
                Vector3d ppos = ((Gaia) renderable.getParent()).unrotatedPos;
                shaderProgram.setUniformf("u_parentPos", (float) ppos.x, (float) ppos.y, (float) ppos.z);
            } else {
                shaderProgram.setUniformf("u_parentPos", 0, 0, 0);
            }

            // Relativistic effects
            addEffectsUniforms(shaderProgram, camera);

            curr.mesh.render(shaderProgram, glType);

            shaderProgram.end();
        }
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

}
