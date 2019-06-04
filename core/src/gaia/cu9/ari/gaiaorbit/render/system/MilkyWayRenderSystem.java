/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.MilkyWayReal;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf.StereoProfile;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;

import java.util.Random;

public class MilkyWayRenderSystem extends ImmediateRenderSystem implements IObserver {
    private boolean UPDATE_POINTS = true;

    Vector3 aux1;
    int additionalOffset, pmOffset;

    private ShaderProgram[] nebulaShaders;
    private MeshData quad;
    private Texture[] nebulatextures;

    private IntModelBatch modelBatch;

    public MilkyWayRenderSystem(RenderGroup rg, float[] alphas, IntModelBatch modelBatch, ShaderProgram[] pointShaders, ShaderProgram[] nebulaShaders) {
        super(rg, alphas, pointShaders);
        this.nebulaShaders = nebulaShaders;
        this.modelBatch = modelBatch;
    }

    @Override
    protected void initShaderProgram() {
        for (ShaderProgram shaderProgram : programs) {
            shaderProgram.begin();
            shaderProgram.setUniformf("u_pointAlphaMin", 0.1f);
            shaderProgram.setUniformf("u_pointAlphaMax", 1.0f);
            shaderProgram.end();
        }

        nebulatextures = new Texture[4];
        for (int i = 0; i < 4; i++) {
            Texture tex = new Texture(GlobalConf.data.dataFile("tex/base/nebula00" + (i + 1) + ".png"));
            tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
            nebulatextures[i] = tex;
        }
    }

    @Override
    protected void initVertices() {
        /** STARS **/
        meshes = new Array<>();
        curr = new MeshData();
        meshes.add(curr);

        aux1 = new Vector3();

        int nVertices = 3000000;

        VertexAttribute[] attribs = buildVertexAttributes();
        curr.mesh = new IntMesh(false, nVertices, 0, attribs);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        pmOffset = curr.mesh.getVertexAttribute(Usage.Tangent) != null ? curr.mesh.getVertexAttribute(Usage.Tangent).offset / 4 : 0;
        additionalOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;

        /** NEBULA **/

        // Max of 5000 nebula clouds
        int maxQuads = 5000;
        int maxQuadVertices = maxQuads * 4;
        int maxQuadIndices = maxQuads * 6;
        quad = new MeshData();

        quad.mesh = new IntMesh(false, maxQuadVertices, maxQuadIndices, VertexAttribute.Position(), VertexAttribute.Normal(), VertexAttribute.TexCoords(0), new VertexAttribute(Usage.Generic, 2, "a_additional"));
        quad.vertices = new float[maxQuadVertices * (quad.mesh.getVertexAttributes().vertexSize / 4)];
        quad.vertexSize = quad.mesh.getVertexAttributes().vertexSize / 4;
        quad.indices = new int[maxQuadIndices];
    }

    private ShaderProgram getNebulaProgram() {
        return GlobalConf.runtime.RELATIVISTIC_ABERRATION ? nebulaShaders[1] : nebulaShaders[0];
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            MilkyWayReal mw = (MilkyWayReal) renderables.get(0);
            Random rand = new Random(24601);

            /**
             * STAR RENDER
             */
            if (GlobalConf.scene.GALAXY_3D && UPDATE_POINTS) {
                Vector3 center = mw.getPosition().toVector3();

                /** STARS **/
                curr.clear();
                float density = GlobalConf.SCALE_FACTOR;

                ensureTempVertsSize(mw.starData.size * curr.vertexSize);
                curr.vertices = tempVerts;
                for (ParticleBean star : mw.starData) {
                    // VERTEX
                    aux1.set((float) star.data[0], (float) star.data[1], (float) star.data[2]);
                    double distanceCenter = aux1.sub(center).len() / (mw.getRadius() * 2f);

                    float[] col = new float[] { (float) (rand.nextGaussian() * 0.02f) + 0.93f, (float) (rand.nextGaussian() * 0.02) + 0.8f, (float) (rand.nextGaussian() * 0.02) + 0.97f, rand.nextFloat() * 0.5f + 0.9f };

                    if (distanceCenter < 1f) {
                        float add = (float) MathUtilsd.clamp(1f - distanceCenter, 0f, 1f) * 0.2f;
                        col[0] = col[0] + add;
                        col[1] = col[1] + add;
                        col[2] = col[2] + add;
                    }

                    col[0] = MathUtilsd.clamp(col[0], 0f, 1f);
                    col[1] = MathUtilsd.clamp(col[1], 0f, 1f);
                    col[2] = MathUtilsd.clamp(col[2], 0f, 1f);

                    // COLOR
                    curr.vertices[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(col[0], col[1], col[2], col[3] * 0.9f);

                    // SIZE
                    double starSize;
                    if (star.data.length > 3) {
                        starSize = (star.data[3] * 10f + 2.0f);
                    } else {
                        starSize = (float) Math.abs(rand.nextGaussian()) * 10f + 2.0f;
                    }
                    curr.vertices[curr.vertexIdx + additionalOffset] = (float) (starSize * density * 2);
                    curr.vertices[curr.vertexIdx + additionalOffset + 1] = 0.7f;

                    // cb.transform.getTranslationf(aux);
                    // POSITION
                    aux1.set((float) star.data[0], (float) star.data[1], (float) star.data[2]);
                    final int idx = curr.vertexIdx;
                    curr.vertices[idx] = aux1.x;
                    curr.vertices[idx + 1] = aux1.y;
                    curr.vertices[idx + 2] = aux1.z;

                    curr.vertexIdx += curr.vertexSize;

                }
                curr.mesh.setVertices(curr.vertices, 0, curr.vertexIdx);
                curr.vertices = null;

                /** QUADS **/
                quad.clear();
                Vector3 rotaxis = new Vector3();
                Vector3 transl = new Vector3();
                Vector3 bl = new Vector3();
                Vector3 br = new Vector3();
                Vector3 tl = new Vector3();
                Vector3 tr = new Vector3();
                Vector3 normal = new Vector3();
                Vector3 quadpoint = new Vector3();

                for (ParticleBean qp : mw.nebulaData) {
                    // 5 quads per nebula
                    for (int i = 0; i < 5; i++) {
                        quadpoint.set((float) qp.data[0], (float) qp.data[1], (float) qp.data[2]);
                        float quadpointdist = quadpoint.len();
                        float texnum, alphamultiplier, quadsize;
                        if (quadpointdist < mw.size / 2f) {
                            texnum = (float) Math.floor(rand.nextFloat() + 0.3f);
                        } else {
                            texnum = rand.nextInt(4);
                        }
                        quadsize = (float) ((qp.data.length > 3 ? (qp.data[3] + 0.7f) : (rand.nextFloat() + 1.0f)) * .6e11* Constants.VR_SCALE);
                        alphamultiplier = MathUtilsd.lint(quadpointdist, 0, mw.size * 3, 6.0f, 1.0f);

                        rotaxis.set(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
                        float rotangle = rand.nextFloat() * 360f;
                        transl.set(rand.nextFloat() * 1e10f, rand.nextFloat() * 1e10f, rand.nextFloat() * 1e10f).add(quadpoint);

                        bl.set(-1, -1, 0).scl(quadsize).rotate(rotaxis, rotangle).add(transl);
                        br.set(1, -1, 0).scl(quadsize).rotate(rotaxis, rotangle).add(transl);
                        tr.set(1, 1, 0).scl(quadsize).rotate(rotaxis, rotangle).add(transl);
                        tl.set(-1, 1, 0).scl(quadsize).rotate(rotaxis, rotangle).add(transl);
                        normal.set(0, 0, 1).rotate(rotaxis, rotangle);

                        // Bottom left
                        quad.vertices[quad.vertexIdx] = bl.x;
                        quad.vertices[quad.vertexIdx + 1] = bl.y;
                        quad.vertices[quad.vertexIdx + 2] = bl.z;
                        quad.vertices[quad.vertexIdx + 3] = normal.x;
                        quad.vertices[quad.vertexIdx + 4] = normal.y;
                        quad.vertices[quad.vertexIdx + 5] = normal.z;
                        quad.vertices[quad.vertexIdx + 6] = 0;
                        quad.vertices[quad.vertexIdx + 7] = 0;
                        quad.vertices[quad.vertexIdx + 8] = texnum; // texture
                        // number
                        quad.vertices[quad.vertexIdx + 9] = alphamultiplier; // alpha
                        // multiplier
                        quad.vertexIdx += quad.vertexSize;

                        // Bottom right
                        quad.vertices[quad.vertexIdx] = br.x;
                        quad.vertices[quad.vertexIdx + 1] = br.y;
                        quad.vertices[quad.vertexIdx + 2] = br.z;
                        quad.vertices[quad.vertexIdx + 3] = normal.x;
                        quad.vertices[quad.vertexIdx + 4] = normal.y;
                        quad.vertices[quad.vertexIdx + 5] = normal.z;
                        quad.vertices[quad.vertexIdx + 6] = 1;
                        quad.vertices[quad.vertexIdx + 7] = 0;
                        quad.vertices[quad.vertexIdx + 8] = texnum; // texture
                        // number
                        quad.vertices[quad.vertexIdx + 9] = alphamultiplier; // alpha
                        // multiplier
                        quad.vertexIdx += quad.vertexSize;

                        // Top right
                        quad.vertices[quad.vertexIdx] = tr.x;
                        quad.vertices[quad.vertexIdx + 1] = tr.y;
                        quad.vertices[quad.vertexIdx + 2] = tr.z;
                        quad.vertices[quad.vertexIdx + 3] = normal.x;
                        quad.vertices[quad.vertexIdx + 4] = normal.y;
                        quad.vertices[quad.vertexIdx + 5] = normal.z;
                        quad.vertices[quad.vertexIdx + 6] = 1;
                        quad.vertices[quad.vertexIdx + 7] = 1;
                        quad.vertices[quad.vertexIdx + 8] = texnum; // texture
                        // number
                        quad.vertices[quad.vertexIdx + 9] = alphamultiplier; // alpha
                        // multiplier
                        quad.vertexIdx += quad.vertexSize;

                        // Top left
                        quad.vertices[quad.vertexIdx] = tl.x;
                        quad.vertices[quad.vertexIdx + 1] = tl.y;
                        quad.vertices[quad.vertexIdx + 2] = tl.z;
                        quad.vertices[quad.vertexIdx + 3] = normal.x;
                        quad.vertices[quad.vertexIdx + 4] = normal.y;
                        quad.vertices[quad.vertexIdx + 5] = normal.z;
                        quad.vertices[quad.vertexIdx + 6] = 0;
                        quad.vertices[quad.vertexIdx + 7] = 1;
                        quad.vertices[quad.vertexIdx + 8] = texnum; // texture
                        // number
                        quad.vertices[quad.vertexIdx + 9] = alphamultiplier; // alpha
                        // multiplier
                        quad.vertexIdx += quad.vertexSize;

                        // Indices
                        quad.indices[quad.indexIdx] = quad.indexVert;
                        quad.indices[quad.indexIdx + 1] = quad.indexVert + 1;
                        quad.indices[quad.indexIdx + 2] = quad.indexVert + 2;
                        quad.indices[quad.indexIdx + 3] = quad.indexVert + 2;
                        quad.indices[quad.indexIdx + 4] = quad.indexVert + 3;
                        quad.indices[quad.indexIdx + 5] = quad.indexVert + 0;
                        quad.indexIdx += 6;
                        quad.indexVert += 4;

                    }

                }
                quad.mesh.setVertices(quad.vertices, 0, quad.vertexIdx);
                quad.vertices = null;
                quad.mesh.setIndices(quad.indices, 0, quad.indexIdx);
                quad.indices = null;

                // Put flag down
                UPDATE_POINTS = false;
            }

            float alpha = getAlpha(mw);
            if (alpha > 0) {

                if (GlobalConf.scene.GALAXY_3D) {
                    /**
                     * NEBULA RENDERER
                     */

                    ShaderProgram nebulaProgram = getNebulaProgram();

                    nebulaProgram.begin();

                    // General uniforms
                    nebulaProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
                    nebulaProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux1));
                    nebulaProgram.setUniformf("u_alpha", 0.04f * mw.opacity * alpha);

                    for (int i = 0; i < 4; i++) {
                        nebulatextures[i].bind(i);
                        nebulaProgram.setUniformi("u_nebulaTexture" + i, i);
                    }

                    // Relativistic effects
                    addEffectsUniforms(nebulaProgram, camera);

                    quad.mesh.render(nebulaProgram, GL20.GL_TRIANGLES, 0, quad.indexIdx);

                    nebulaProgram.end();

                    /**
                     * STAR RENDERER
                     */
                    if (Gdx.app.getType() == ApplicationType.Desktop) {
                        // Enable gl_PointCoord
                        Gdx.gl20.glEnable(34913);
                        // Enable point sizes
                        Gdx.gl20.glEnable(0x8642);
                    }
                    // Additive blending
                    Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);

                    ShaderProgram shaderProgram = getShaderProgram();

                    shaderProgram.begin();
                    shaderProgram.setUniformi("u_blending", 1);
                    shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
                    shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux1));
                    shaderProgram.setUniformf("u_alpha", mw.opacity * alpha * 0.2f);
                    shaderProgram.setUniformf("u_ar", GlobalConf.program.STEREOSCOPIC_MODE && (GlobalConf.program.STEREO_PROFILE != StereoProfile.HD_3DTV_HORIZONTAL && GlobalConf.program.STEREO_PROFILE != StereoProfile.ANAGLYPHIC) ? 0.5f : 1f);
                    shaderProgram.setUniformf("u_vrScale", (float) Constants.VR_SCALE);

                    // Relativistic effects
                    addEffectsUniforms(shaderProgram, camera);

                    curr.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                    shaderProgram.end();

                    // Restore
                    Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                }

                /**
                 * IMAGE RENDERER
                 */
                //mw.mc.touch();
                //mw.mc.setTransparency(mw.opacity * alpha * (GlobalConf.scene.GALAXY_3D ? 0.6f : 0.8f));
                //mw.mc.setTransparency(mw.opacity * alpha * 1f);
                //mw.mc.updateRelativisticEffects(camera);

                // Needed to restore render pipeline
                modelBatch.begin(camera.getCamera());
                //modelBatch.render(mw.mc.instance, mw.mc.env);
                modelBatch.end();

            }
        }

    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<VertexAttribute>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 4, "a_additional"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void notify(Events event, Object... data) {
    }

}
