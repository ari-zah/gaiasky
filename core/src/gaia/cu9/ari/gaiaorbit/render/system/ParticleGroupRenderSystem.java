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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.comp.DistToCameraComparator;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;

import java.util.Random;

public class ParticleGroupRenderSystem extends ImmediateRenderSystem implements IObserver {
    Vector3 aux1;
    int additionalOffset, pmOffset;
    Random rand;

    public ParticleGroupRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] shaders) {
        super(rg, alphas, shaders);
        comp = new DistToCameraComparator<>();
        rand = new Random(123);
        aux1 = new Vector3();
        EventManager.instance.subscribe(this, Events.DISPOSE_PARTICLE_GROUP_GPU_MESH);
    }

    @Override
    protected void initShaderProgram() {
    }

    @Override
    protected void initVertices() {
        /** STARS **/
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
        pmOffset = curr.mesh.getVertexAttribute(Usage.Tangent) != null ? curr.mesh.getVertexAttribute(Usage.Tangent).offset / 4 : 0;
        additionalOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
        return mdi;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            for (IRenderable renderable : renderables) {
                ParticleGroup particleGroup = (ParticleGroup) renderable;
                /**
                 * GROUP RENDER
                 */
                if (!particleGroup.inGpu) {
                    particleGroup.offset = addMeshData(particleGroup.size());
                    curr = meshes.get(particleGroup.offset);

                    ensureTempVertsSize(particleGroup.size() * curr.vertexSize);
                    for (ParticleBean pb : particleGroup.data()) {
                        double[] p = pb.data;
                        // COLOR
                        float[] c = particleGroup.getColor();
                        tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(c[0], c[1], c[2], c[3]);

                        // SIZE
                        tempVerts[curr.vertexIdx + additionalOffset] = (particleGroup.size + (float) (rand.nextGaussian() * particleGroup.size / 4d)) * particleGroup.highlightedSizeFactor();

                        // cb.transform.getTranslationf(aux);
                        // POSITION
                        final int idx = curr.vertexIdx;
                        tempVerts[idx] = (float) p[0];
                        tempVerts[idx + 1] = (float) p[1];
                        tempVerts[idx + 2] = (float) p[2];

                        curr.vertexIdx += curr.vertexSize;
                    }
                    particleGroup.count = particleGroup.size() * curr.vertexSize;
                    curr.mesh.setVertices(tempVerts, 0, particleGroup.count);

                    particleGroup.inGpu = true;

                }

                curr = meshes.get(particleGroup.offset);
                if (curr != null) {
                    // Enable gl_PointCoord
                    Gdx.gl20.glEnable(34913);
                    // Enable point sizes
                    Gdx.gl20.glEnable(0x8642);

                    // Additive blending
                    Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);

                    ShaderProgram shaderProgram = getShaderProgram();

                    boolean stereohw = GlobalConf.program.isStereoHalfWidth();

                    shaderProgram.begin();
                    shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
                    shaderProgram.setUniformf("u_alpha", alphas[particleGroup.ct.getFirstOrdinal()] * particleGroup.getOpacity());
                    shaderProgram.setUniformf("u_ar", stereohw ? 0.5f : 1f);
                    shaderProgram.setUniformf("u_profileDecay", particleGroup.profileDecay);
                    shaderProgram.setUniformf("u_sizeFactor", (((stereohw ? 2f : 1f) * rc.scaleFactor * GlobalConf.scene.STAR_POINT_SIZE / 5f)) * particleGroup.highlightedSizeFactor());
                    shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux1));
                    shaderProgram.setUniformf("u_camDir", camera.getCurrent().getCamera().direction);
                    shaderProgram.setUniformi("u_cubemap", GlobalConf.program.CUBEMAP360_MODE ? 1 : 0);

                    // Relativistic effects
                    addEffectsUniforms(shaderProgram, camera);

                    curr.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                    shaderProgram.end();

                    // Restore
                    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                }
            }
        }
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<VertexAttribute>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 1, "a_size"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case DISPOSE_PARTICLE_GROUP_GPU_MESH:
            Integer meshIdx = (Integer) data[0];
            clearMeshData(meshIdx);
            break;
        default:
            break;
        }
    }

}
