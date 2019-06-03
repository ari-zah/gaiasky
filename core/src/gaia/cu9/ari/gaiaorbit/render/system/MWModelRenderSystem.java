/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FloatFrameBufferBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.bitfire.postprocessing.utils.FullscreenQuad;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.MilkyWay;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf.StereoProfile;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.util.Random;

public class MWModelRenderSystem extends ImmediateRenderSystem implements IObserver {
    public static final boolean oit = false;
    private boolean UPDATE_POINTS = true;

    private Vector3 aux3f1;
    private Vector3d aux3d1;
    private int additionalOffset;

    private ShaderProgram postShader;
    private FullscreenQuad postMesh;
    private Random rand = new Random(24601);

    public FrameBuffer oitFb;
    private int accumLoc, revealLoc;

    private class Chunk {
        public MeshData data;
        public Vector3d centre;
        public double distcam;
        public boolean dust;

        public Chunk(MeshData data, Vector3d centre) {
            this.data = data;
            this.centre = centre;
        }

        public Chunk(MeshData data, Vector3d centre, boolean dust) {
            this(data, centre);
            this.dust = dust;
        }

        public void updateDistcam(Vector3d campos) {
            distcam = aux3d1.set(centre).sub(campos).len();
        }

    }

    private Array<Chunk> chunks;

    public MWModelRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] starShaders) {
        super(rg, alphas, starShaders);

        aux3f1 = new Vector3();
        aux3d1 = new Vector3d();
        chunks = new Array<>();

        if (oit) {
            FloatFrameBufferBuilder ffbb = new FloatFrameBufferBuilder(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            ffbb.addFloatAttachment(GL30.GL_RGBA32F, GL30.GL_RGBA, GL30.GL_FLOAT, false);
            ffbb.addFloatAttachment(GL30.GL_RGBA32F, GL30.GL_RGBA, GL30.GL_FLOAT, false);
            ffbb.addDepthRenderBuffer(GL30.GL_DEPTH_COMPONENT16);
            oitFb = ffbb.build();

            postShader = new ShaderProgram(Gdx.files.internal("shader/galaxy.post.vertex.glsl"), Gdx.files.internal("shader/galaxy.post.fragment.glsl"));
            if (!postShader.isCompiled()) {
                logger.error("Galaxy post shader compilation failed:");
                logger.error(postShader.getLog());
            }

            postShader.begin();
            accumLoc = postShader.getUniformLocation("tex_accum");
            revealLoc = postShader.getUniformLocation("tex_reveal");
            postShader.setUniformi("tex_accum", accumLoc);
            postShader.setUniformi("tex_reveal", revealLoc);
            postShader.end();
            
            /** Post mesh **/
            postMesh = new FullscreenQuad();
        }

    }

    @Override
    protected void initShaderProgram() {
        for (ShaderProgram shaderProgram : programs) {
            shaderProgram.begin();
            shaderProgram.setUniformf("u_pointAlphaMin", 0.1f);
            shaderProgram.setUniformf("u_pointAlphaMax", 1.0f);
            shaderProgram.end();
        }
    }

    @Override
    protected void initVertices() {
    }

    private void initMesh(MeshData md, int nvertices) {

        VertexAttribute[] attribs = buildVertexAttributes();
        md.mesh = new IntMesh(false, nvertices, 0, attribs);

        md.vertexSize = md.mesh.getVertexAttributes().vertexSize / 4;
        md.colorOffset = md.mesh.getVertexAttribute(Usage.ColorPacked) != null ? md.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        additionalOffset = md.mesh.getVertexAttribute(Usage.Generic) != null ? md.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
    }

    private Vector3d computeCentre(Array<? extends ParticleBean> arr) {
        Vector3d c = new Vector3d();
        for (int i = 0; i < arr.size; i++) {
            ParticleBean pb = arr.get(i);
            c.add(pb.data[0], pb.data[1], pb.data[2]);
        }
        c.scl(1d / (double) arr.size);
        return c;
    }

    private void streamToGpu(MilkyWay mw) {
        Vector3 center = mw.getPosition().toVector3();
        float density = GlobalConf.SCALE_FACTOR;

        /** BULGE **/
        MeshData bulge = new MeshData();
        initMesh(bulge, mw.starData.size);
        
        ensureTempVertsSize(mw.starData.size * bulge.vertexSize);
        for (ParticleBean star : mw.starData) {
            // VERTEX
            aux3f1.set((float) star.data[0], (float) star.data[1], (float) star.data[2]);
            double distanceCenter = aux3f1.sub(center).len() / (mw.getRadius() * 2f);

            float[] col = new float[] { (float) (rand.nextGaussian() * 0.02f) + 0.93f, (float) (rand.nextGaussian() * 0.02) + 0.8f, (float) (rand.nextGaussian() * 0.02) + 0.97f, 0.2f };

            if (distanceCenter < 1f) {
                float add = (float) MathUtilsd.clamp(1f - distanceCenter, 0f, 1f) * 0.5f;
                col[0] = col[0] + add;
                col[1] = col[1] + add;
                col[2] = col[2] + add;
            }

            col[0] = MathUtilsd.clamp(col[0], 0f, 1f);
            col[1] = MathUtilsd.clamp(col[1], 0f, 1f);
            col[2] = MathUtilsd.clamp(col[2], 0f, 1f);

            // COLOR
            tempVerts[bulge.vertexIdx + bulge.colorOffset] = Color.toFloatBits(col[0], col[1], col[2], col[3]);

            // SIZE
            double starSize = 0;
            if (star.data.length > 3) {
                starSize = (star.data[3] * 1.5 + 1);
            } else {
                starSize = (float) Math.abs(rand.nextGaussian()) * 8f + 1.0f;
            }
            tempVerts[bulge.vertexIdx + additionalOffset] = (float) (starSize * density * 2);
            tempVerts[bulge.vertexIdx + additionalOffset + 1] = 0.7f;

            // cb.transform.getTranslationf(aux);
            // POSITION
            aux3f1.set((float) star.data[0], (float) star.data[1], (float) star.data[2]);
            final int idx = bulge.vertexIdx;
            tempVerts[idx] = aux3f1.x;
            tempVerts[idx + 1] = aux3f1.y;
            tempVerts[idx + 2] = aux3f1.z;

            bulge.vertexIdx += bulge.vertexSize;
        }
        bulge.mesh.setVertices(tempVerts, 0, bulge.vertexIdx);
        Chunk bulgeChunk = new Chunk(bulge, computeCentre(mw.starData));
        chunks.add(bulgeChunk);

        int partitions = 32;
        Array<Array<ParticleBean>> partition = new Array<Array<ParticleBean>>(8);
        for (int i = 0; i < partitions; i++)
            partition.add(new Array<ParticleBean>());

        for (int i = 0; i < mw.dustData.size; i++) {
            ParticleBean dust = mw.dustData.get(i);
            int idx = mw.dustPartition[i];
            partition.get(idx).add(dust);
        }

        for (Array<ParticleBean> part : partition) {
            MeshData partmd = new MeshData();
            initMesh(partmd, part.size);
            
            ensureTempVertsSize(part.size * partmd.vertexSize);
            for (ParticleBean p : part) {
                // VERTEX
                aux3f1.set((float) p.data[0], (float) p.data[1], (float) p.data[2]);

                float[] col = new float[] { (float) (rand.nextGaussian() * 0.02f) + 0.23f, (float) (rand.nextGaussian() * 0.02) + 0.23f, (float) (rand.nextGaussian() * 0.02) + 0.3f, 0.55f };

                col[0] = MathUtilsd.clamp(col[0], 0f, 1f);
                col[1] = MathUtilsd.clamp(col[1], 0f, 1f);
                col[2] = MathUtilsd.clamp(col[2], 0f, 1f);

                // COLOR
                tempVerts[partmd.vertexIdx + partmd.colorOffset] = Color.toFloatBits(col[0], col[1], col[2], col[3]);

                // SIZE
                double starSize = 0;
                if (p.data.length > 3) {
                    starSize = (p.data[3] * 1 + 1);
                } else {
                    starSize = (float) Math.abs(rand.nextGaussian()) * 8f + 1.0f;
                }
                tempVerts[partmd.vertexIdx + additionalOffset] = (float) (starSize * density * 2);
                tempVerts[partmd.vertexIdx + additionalOffset + 1] = 0.7f;

                // POSITION
                aux3f1.set((float) p.data[0], (float) p.data[1], (float) p.data[2]);
                final int idx = partmd.vertexIdx;
                tempVerts[idx] = aux3f1.x;
                tempVerts[idx + 1] = aux3f1.y;
                tempVerts[idx + 2] = aux3f1.z;

                partmd.vertexIdx += partmd.vertexSize;

            }
            partmd.mesh.setVertices(tempVerts, 0, partmd.vertexIdx);
            Chunk chunk = new Chunk(partmd, computeCentre(part), true);
            chunks.add(chunk);
        }
    }

    public void renderPrePasses(MilkyWay mw, ICamera camera) {
        if (oit) {
            if (UPDATE_POINTS) {
                streamToGpu(mw);
                // Put flag down
                UPDATE_POINTS = false;
            }

            /**
             * PARTICLES RENDERER
             */
            float alpha = getAlpha(mw);
            if (alpha > 0) {
                // Enable gl_PointCoord
                Gdx.gl20.glEnable(34913);
                // Enable point sizes
                Gdx.gl20.glEnable(0x8642);

                Gdx.gl30.glDisable(GL30.GL_DEPTH_TEST);
                Gdx.gl30.glDepthMask(false);
                Gdx.gl30.glEnable(GL30.GL_BLEND);

                // Set camera (hack)
                //camera.getCamera().near = .3e11f;
                //camera.getCamera().far = 1e22f;
                //camera.getCamera().update(false);
                /**
                 * PASS 0
                 */

                ShaderProgram shaderProgram = getShaderProgram();

                oitFb.begin();
                Gdx.gl30.glClearColor(0, 0, 0, 1);
                Gdx.gl30.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
                Gdx.gl30.glBlendFuncSeparate(GL30.GL_ONE, GL30.GL_ONE, GL30.GL_COLOR, GL30.GL_ONE_MINUS_SRC_COLOR);

                shaderProgram.begin();
                shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
                shaderProgram.setUniformMatrix("u_view", camera.getCamera().view);
                shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux3f1));
                shaderProgram.setUniformf("u_alpha", mw.opacity * alpha);
                shaderProgram.setUniformf("u_ar", GlobalConf.program.STEREOSCOPIC_MODE && (GlobalConf.program.STEREO_PROFILE != StereoProfile.HD_3DTV_HORIZONTAL && GlobalConf.program.STEREO_PROFILE != StereoProfile.ANAGLYPHIC) ? 0.5f : 1f);

                // Relativistic effects
                addEffectsUniforms(shaderProgram, camera);

                for (Chunk chunk : chunks)
                    chunk.data.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                shaderProgram.end();
                oitFb.end();


                // Restore
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (oit) {
            renderStudOit(renderables, camera, t);
        } else {
            renderStudNoOit(renderables, camera, t);
        }
    }

    public void renderStudOit(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            MilkyWay mw = (MilkyWay) renderables.get(0);
            // Send data to GPU

            /**
             * PARTICLES RENDERER
             */
            float alpha = getAlpha(mw);
            if (alpha > 0) {

                ShaderProgram shaderProgram = postShader;

                shaderProgram.begin();
                oitFb.getTextureAttachments().get(0).bind(accumLoc);
                oitFb.getTextureAttachments().get(1).bind(revealLoc);
                Gdx.gl20.glBlendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
                postMesh.render(shaderProgram);
                shaderProgram.end();

            }
        }

    }

    public void renderStudNoOit(Array<IRenderable> renderables, ICamera camera, double t) {
        if (renderables.size > 0) {
            MilkyWay mw = (MilkyWay) renderables.get(0);

            /**
             * PARTICLES RENDERER
             */
            if (UPDATE_POINTS) {
                streamToGpu(mw);
                // Put flag down
                UPDATE_POINTS = false;
            }
            float alpha = getAlpha(mw);
            if (alpha > 0) {
                /**
                 * PARTICLE RENDERER
                 */
                // Enable gl_PointCoord
                Gdx.gl20.glEnable(34913);
                // Enable point sizes
                Gdx.gl20.glEnable(0x8642);

                // Multiplicative blending (RGBs * RBGd)
                //Gdx.gl20.glBlendFunc(GL20.GL_ZERO, GL20.GL_SRC_COLOR);

                // Additive blending (RGBs + RGBd)
                //Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);

                // Transparency blending - Order dependent ownOpacity
                //Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                // Sort chunks
                Vector3d campos = camera.getPos();
                for (Chunk chunk : chunks)
                    chunk.updateDistcam(campos);
                chunks.sort((a, b) -> {
                    return Double.compare(b.distcam, a.distcam);
                });

                ShaderProgram shaderProgram = getShaderProgram();

                shaderProgram.begin();
                shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
                shaderProgram.setUniformMatrix("u_view", camera.getCamera().view);
                shaderProgram.setUniformMatrix("u_projection", camera.getCamera().projection);

                shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux3f1));
                shaderProgram.setUniformf("u_alpha", mw.opacity * alpha);
                shaderProgram.setUniformf("u_ar", GlobalConf.program.STEREOSCOPIC_MODE && (GlobalConf.program.STEREO_PROFILE != StereoProfile.HD_3DTV_HORIZONTAL && GlobalConf.program.STEREO_PROFILE != StereoProfile.ANAGLYPHIC) ? 0.5f : 1f);
                // Relativistic effects
                addEffectsUniforms(shaderProgram, camera);

                for (Chunk chunk : chunks) {
                    if (chunk.dust) {
                        // Alpha blending
                        Gdx.gl20.glBlendFunc(GL20.GL_SRC_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA);
                        shaderProgram.setUniformf("u_blending", 0);
                    } else {
                        // Additive
                        Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
                        shaderProgram.setUniformf("u_blending", 1);
                    }
                    chunk.data.mesh.render(shaderProgram, ShapeType.Point.getGlType());
                }
                shaderProgram.end();

                // Restore
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
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

    protected VertexAttribute[] buildVertexAttributesPost() {
        Array<VertexAttribute> attribs = new Array<VertexAttribute>();
        attribs.add(new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void notify(Events event, Object... data) {
    }

}
