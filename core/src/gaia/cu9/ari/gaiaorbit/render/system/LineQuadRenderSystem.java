/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.render.ILineRenderable;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.render.SceneGraphRenderer;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Renders lines as Polyline Quadstrips (Polyboards).
 * Slower but higher quality.
 *
 * @author tsagrista
 */
public class LineQuadRenderSystem extends LineRenderSystem {
    protected static final int INI_DPOOL_SIZE = 1000;
    protected static final int MAX_DPOOL_SIZE = 10000;
    private MeshDataExt currext;
    private Array<double[]> provisionalLines;
    private Array<Line> provLines;
    private LineArraySorter sorter;
    private Pool<double[]> dpool;

    private class MeshDataExt extends MeshData {
        int uvOffset;
        int indexIdx;
        int maxIndices;
        int[] indices;

        public void clear() {
            super.clear();
            indexIdx = 0;
        }
    }

    private class Line {
        public float r, g, b, a;
        public double widthAngleTan;
        public double[][] points;
        public double[] dists;
    }

    Vector3d line, camdir0, camdir1, camdir15, point, vec;
    final static double baseWidthAngle = Math.toRadians(.13);
    final static double baseWidthAngleTan = Math.tan(baseWidthAngle);

    public LineQuadRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        dpool = new DPool(INI_DPOOL_SIZE, MAX_DPOOL_SIZE, 14);
        provisionalLines = new Array<>();
        provLines = new Array<>();
        sorter = new LineArraySorter(12);
        line = new Vector3d();
        camdir0 = new Vector3d();
        camdir1 = new Vector3d();
        camdir15 = new Vector3d();
        point = new Vector3d();
        vec = new Vector3d();
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
        initVertices(meshIdx++);
    }

    private void initVertices(int index) {
        if (index >= meshes.size){
            meshes.setSize(index + 1);
        }
        if(meshes.get(index) == null) {
            if (index > 0)
                logger.info("Capacity too small, creating new meshdata: " + curr.capacity);
            currext = new MeshDataExt();
            meshes.set(index, currext);
            curr = currext;

            curr.capacity = 10000;
            currext.maxIndices = curr.capacity + curr.capacity / 2;

            VertexAttribute[] attribs = buildVertexAttributes();
            currext.mesh = new IntMesh(false, curr.capacity, currext.maxIndices, attribs);

            currext.indices = new int[currext.maxIndices];
            currext.vertexSize = currext.mesh.getVertexAttributes().vertexSize / 4;
            currext.vertices = new float[curr.capacity * currext.vertexSize];

            currext.colorOffset = currext.mesh.getVertexAttribute(Usage.ColorPacked) != null ? currext.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
            currext.uvOffset = currext.mesh.getVertexAttribute(Usage.TextureCoordinates) != null ? currext.mesh.getVertexAttribute(Usage.TextureCoordinates).offset / 4 : 0;
        } else {
            currext = (MeshDataExt) meshes.get(index);
            curr = currext;
        }
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.TextureCoordinates, 2, "a_uv"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    public void uv(float u, float v) {
        currext.vertices[currext.vertexIdx + currext.uvOffset] = u;
        currext.vertices[currext.vertexIdx + currext.uvOffset + 1] = v;
    }

    private boolean two = false;
    private Vector3d aux = new Vector3d();

    public void breakLine(){
        two = false;
    }

    public void addPoint(ILineRenderable lr, double x, double y, double z, float r, float g, float b, float a) {
        if (!two) {
            aux.set(x, y, z);
            two = true;
        } else {
            addLine(lr, aux.x, aux.y, aux.z, x, y, z, r, g, b, a);
            aux.set(x, y, z);
        }
    }

    @Override
    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, Color c) {
        addLine(lr, x0, y0, z0, x1, y1, z1, c.r, c.g, c.b, c.a);
    }

    @Override
    public void addLine(ILineRenderable lr, double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a) {
        addLineInternal(x0, y0, z0, x1, y1, z1, r, g, b, a, lr.getLineWidth() * baseWidthAngleTan);
    }

    private void addLineInternal(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a, double widthAngleTan) {
        addLineInternal(x0, y0, z0, x1, y1, z1, r, g, b, a, widthAngleTan, true);
    }

    private void addLineInternal(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a, double widthAngleTan, boolean rec) {
        double distToSegment = MathUtilsd.distancePointSegment(x0, y0, z0, x1, y1, z1, 0, 0, 0);

        double dist0 = Math.sqrt(x0 * x0 + y0 * y0 + z0 * z0);
        double dist1 = Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);

        Vector3d p15;

        if (rec && distToSegment < dist0 && distToSegment < dist1) {
            // Projection falls in line, split line
            p15 = MathUtilsd.getClosestPoint2(x0, y0, z0, x1, y1, z1, 0, 0, 0);

            addLineInternal(x0, y0, z0, p15.x, p15.y, p15.z, r, g, b, a, widthAngleTan, true);
            addLineInternal(p15.x, p15.y, p15.z, x1, y1, z1, r, g, b, a, widthAngleTan, true);
        } else {
            // Add line to list
            // x0 y0 z0 x1 y1 z1 r g b a dist0 dist1 distMean
            double[] l = dpool.obtain();
            l[0] = x0;
            l[1] = y0;
            l[2] = z0;
            l[3] = x1;
            l[4] = y1;
            l[5] = z1;
            l[6] = r;
            l[7] = g;
            l[8] = b;
            l[9] = a;
            l[10] = dist0;
            l[11] = dist1;
            l[12] = (dist0 + dist1) / 2d;
            l[13] = widthAngleTan;
            provisionalLines.add(l);
        }
    }

    public void addLinePostproc(Line l) {
        int npoints = l.points.length;
        // Check if npoints more indices fit
        if (currext.numVertices + npoints * 2 - 2 > curr.capacity) {
            initVertices(meshIdx++);
        }

        for (int i = 1; i < npoints; i++) {
            if (i == 1) {
                // Line from 0 to 1
                line.set(l.points[1][0] - l.points[0][0], l.points[1][1] - l.points[0][1], l.points[1][2] - l.points[0][2]);
            } else if (i == npoints - 1) {
                // Line from npoints-1 to npoints
                line.set(l.points[npoints - 1][0] - l.points[npoints - 2][0], l.points[npoints - 1][1] - l.points[npoints - 2][1], l.points[npoints - 1][2] - l.points[npoints - 2][2]);
            } else {
                // Line from i-1 to i+1
                line.set(l.points[i + 1][0] - l.points[i - 1][0], l.points[i + 1][1] - l.points[i - 1][1], l.points[i + 1][2] - l.points[i - 1][2]);
            }
            camdir0.set(l.points[i]);
            camdir0.crs(line);
            camdir0.setLength(l.widthAngleTan * l.dists[i] * camera.getFovFactor());

            // P1
            point.set(l.points[i]).add(camdir0);
            color(l.r, l.g, l.b, l.a);
            uv(i / (npoints - 1), 0);
            vertex((float) point.x, (float) point.y, (float) point.z);

            // P2
            point.set(l.points[i]).sub(camdir0);
            color(l.r, l.g, l.b, l.a);
            uv(i / (npoints - 1), 1);
            vertex((float) point.x, (float) point.y, (float) point.z);

            // Indices
            if (i > 1) {
                index((currext.numVertices - 4));
                index((currext.numVertices - 2));
                index((currext.numVertices - 3));

                index((currext.numVertices - 2));
                index((currext.numVertices - 1));
                index((currext.numVertices - 3));
            }
        }
    }

    public void addLinePostproc(double x0, double y0, double z0, double x1, double y1, double z1, double r, double g, double b, double a, double dist0, double dist1, double widthTan) {

        // Check if 4 more indices fit
        if (currext.numVertices + 4 >= curr.capacity) {
            // We need to open a new MeshDataExt!
            initVertices(meshIdx++);
        }

        // Projection falls outside line
        double width0 = widthTan * dist0 * camera.getFovFactor();
        double width1 = widthTan * dist1 * camera.getFovFactor();

        line.set(x1 - x0, y1 - y0, z1 - z0);

        camdir0.set(x0, y0, z0);
        camdir1.set(x1, y1, z1);

        // Camdir0 and 1 will contain the perpendicular to camdir and line
        camdir0.crs(line);
        camdir1.crs(line);

        camdir0.setLength(width0);
        // P1
        point.set(x0, y0, z0).add(camdir0);
        color(r, g, b, a);
        uv(0, 0);
        vertex((float) point.x, (float) point.y, (float) point.z);

        // P2
        point.set(x0, y0, z0).sub(camdir0);
        color(r, g, b, a);
        uv(0, 1);
        vertex((float) point.x, (float) point.y, (float) point.z);

        camdir1.setLength(width1);
        // P3
        point.set(x1, y1, z1).add(camdir1);
        color(r, g, b, a);
        uv(1, 0);
        vertex((float) point.x, (float) point.y, (float) point.z);

        // P4
        point.set(x1, y1, z1).sub(camdir1);
        color(r, g, b, a);
        uv(1, 1);
        vertex((float) point.x, (float) point.y, (float) point.z);

        // Add indexes
        index(currext.numVertices - 4);
        index(currext.numVertices - 2);
        index(currext.numVertices - 3);

        index(currext.numVertices - 2);
        index(currext.numVertices - 1);
        index(currext.numVertices - 3);

    }

    private void index(int idx) {
        currext.indices[currext.indexIdx] = idx;
        currext.indexIdx++;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        this.camera = camera;

        // Reset
        meshIdx = 1;
        currext = (MeshDataExt) meshes.get(0);
        curr = currext;

        int size = renderables.size;
        for (int i = 0; i < size; i++) {
            ILineRenderable renderable = (ILineRenderable) renderables.get(i);
            boolean rend = true;
            // TODO ugly hack
            if (renderable instanceof Particle && !SceneGraphRenderer.instance.isOn(ComponentTypes.ComponentType.VelocityVectors))
                rend = false;
            if (rend)
                renderable.render(this, camera, getAlpha(renderable));
        }

        // Sort phase
        provisionalLines.sort(sorter);
        for (double[] l : provisionalLines)
            addLinePostproc(l[0], l[1], l[2], l[3], l[4], l[5], l[6], l[7], l[8], l[9], l[10], l[11], l[13]);

        for (Line l : provLines)
            addLinePostproc(l);

        ShaderProgram shaderProgram = getShaderProgram();

        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);

        // Relativistic effects
        addEffectsUniforms(shaderProgram, camera);

        for (int i = 0; i < meshIdx; i++) {
            MeshDataExt md = (MeshDataExt) meshes.get(i);
            md.mesh.setVertices(md.vertices, 0, md.vertexIdx);
            md.mesh.setIndices(md.indices, 0, md.indexIdx);
            md.mesh.render(shaderProgram, GL20.GL_TRIANGLES);

            md.clear();
        }

        shaderProgram.end();

        // Reset mesh index and current
        int n = provisionalLines.size;
        for (int i = 0; i < n; i++)
            dpool.free(provisionalLines.get(i));

        provisionalLines.clear();
        provLines.clear();
    }

    protected class DPool extends Pool<double[]> {

        private int dsize;

        public DPool(int initialCapacity, int max, int dsize) {
            super(initialCapacity, max);
            this.dsize = dsize;
        }

        @Override
        protected double[] newObject() {
            return new double[dsize];
        }

    }

    public void dispose(){
        super.dispose();
        currext = null;
        provisionalLines.clear();
        provLines.clear();
        provisionalLines = null;
        provLines = null;

    }

}
