/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.render.ILineRenderable;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.render.SceneGraphRenderer;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import org.lwjgl.opengl.GL11;

import java.util.Comparator;

public class LineRenderSystem extends ImmediateRenderSystem {
    protected ICamera camera;
    protected Vector3 aux2;

    private ShaderProgram shaderProgram;

    public LineRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] shaders) {
        super(rg, alphas, shaders, -1);
        aux2 = new Vector3();
    }

    @Override
    protected void initShaderProgram() {
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
        initVertices(meshIdx++);
    }

    private void initVertices(int index) {
        if (index >= meshes.size) {
            meshes.setSize(index + 1);
        }
        if (meshes.get(index) == null) {
            if (index > 0)
                logger.info("Capacity too small, creating new meshdata: " + curr.capacity);
            curr = new MeshData();
            meshes.set(index, curr);

            curr.capacity = 10000;

            VertexAttribute[] attribs = buildVertexAttributes();
            curr.mesh = new IntMesh(false, curr.capacity, 0, attribs);

            curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
            curr.vertices = new float[curr.capacity * curr.vertexSize];
            curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        } else {
            curr = meshes.get(index);
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

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        // Enable GL_LINE_SMOOTH
        Gdx.gl20.glEnable(GL11.GL_LINE_SMOOTH);
        Gdx.gl.glHint(GL20.GL_NICEST, GL11.GL_LINE_SMOOTH_HINT);
        // Enable GL_LINE_WIDTH
        Gdx.gl20.glEnable(GL20.GL_LINE_WIDTH);
        Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl20.glEnable(GL20.GL_BLEND);
        Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shaderProgram = getShaderProgram();

        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);

        // Relativistic effects
        addEffectsUniforms(shaderProgram, camera);

        this.camera = camera;
        int size = renderables.size;
        for (int i = 0; i < size; i++) {
            ILineRenderable renderable = (ILineRenderable) renderables.get(i);
            boolean rend = true;
            // TODO ugly hack
            if (renderable instanceof Particle && !SceneGraphRenderer.instance.isOn(ComponentTypes.ComponentType.VelocityVectors))
                rend = false;
            if (rend) {
                renderable.render(this, camera, getAlpha(renderable));
            }

            Gdx.gl.glLineWidth(renderable.getLineWidth() * 2.0f);

            for (int md = 0; md < meshIdx; md++) {
                MeshData meshd = meshes.get(md);
                meshd.mesh.setVertices(meshd.vertices, 0, meshd.vertexIdx);
                meshd.mesh.render(shaderProgram, renderable.getGlType());

                meshd.clear();
            }
        }
        shaderProgram.end();

        // Reset indices
        meshIdx = 1;
        curr = meshes.get(0);
    }

    /**
     * Breaks current line of points
     */
    public void breakLine() {

    }

    public void addPoint(ILineRenderable lr, double x, double y, double z, float r, float g, float b, float a) {
        // Check if 3 more indices fit
        if (curr.numVertices + 1 >= curr.capacity) {
            // Create new mesh data
            initVertices(meshIdx++);
        }

        color(r, g, b, a);
        vertex((float) x, (float) y, (float) z);
    }

    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, Color col) {
        addLinePostproc(x0, y0, z0, x1, y1, z1, col.r, col.g, col.b, col.a);
    }

    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a) {
        addLinePostproc(x0, y0, z0, x1, y1, z1, r, g, b, a);
    }

    public void addLinePostproc(double x0, double y0, double z0, double x1, double y1, double z1, double r, double g, double b, double a) {
        // Check if 3 more indices fit
        if (curr.numVertices + 2 >= curr.capacity) {
            // Create new mesh data
            initVertices(meshIdx++);
        }

        color(r, g, b, a);
        vertex((float) x0, (float) y0, (float) z0);
        color(r, g, b, a);
        vertex((float) x1, (float) y1, (float) z1);
    }

    protected class LineArraySorter implements Comparator<double[]> {
        private int idx;

        public LineArraySorter(int idx) {
            this.idx = idx;
        }

        @Override
        public int compare(double[] o1, double[] o2) {
            double f = o1[idx] - o2[idx];
            if (f == 0)
                return 0;
            else if (f < 0)
                return 1;
            else
                return -1;
        }

    }

}
