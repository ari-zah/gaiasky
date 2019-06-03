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
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf.StereoProfile;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.util.Random;

public class ParticleEffectsRenderSystem extends ImmediateRenderSystem {
    private static int N_PARTICLES = GlobalConf.scene.isHighQuality() ? 300 : (GlobalConf.scene.isNormalQuality() ? 200 : 100);

    private Random rand;
    private Vector3 aux1f;
    private Vector3d aux1, aux2, aux5;
    private int sizeOffset, tOffset;
    private ComponentTypes ct;
    private Vector3[] positions, additional;
    private Vector3d[] campositions;
    private long baset;

    public ParticleEffectsRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] programs) {
        super(rg, alphas, programs);
        aux1f = new Vector3();
        aux1 = new Vector3d();
        aux2 = new Vector3d();
        aux5 = new Vector3d();
        rand = new Random(123);
        baset = System.currentTimeMillis();
        ct = new ComponentTypes(ComponentType.valueOf("Effects"));
        positions = new Vector3[N_PARTICLES * 2];
        additional = new Vector3[N_PARTICLES * 2];
        campositions = new Vector3d[N_PARTICLES];
        float colfade = Color.toFloatBits(0.f, 0.f, 0.f, 0.f);
        float ctm = System.currentTimeMillis() / 1000;
        for (int i = 0; i < N_PARTICLES * 2; i++) {
            if (i % 2 == 0) {
                // First in the pair
                positions[i] = new Vector3((float) (rand.nextFloat() * Constants.AU_TO_U), 0f, (float) (rand.nextFloat() * Constants.AU_TO_U));
                additional[i] = new Vector3(Color.toFloatBits(1f - rand.nextFloat() * 0.3f, 1f - rand.nextFloat() * 0.3f, 1f - rand.nextFloat() * 0.3f, 1f), 3 + rand.nextInt() % 8, ctm);
                campositions[i / 2] = new Vector3d();
            } else {
                // Companion, start with same positions
                positions[i] = new Vector3(positions[i - 1]);
                additional[i] = new Vector3(colfade, additional[i - 1].y, ctm);
            }
        }
    }

    private float getT() {
        return (float) ((System.currentTimeMillis() - baset) / 1000d);
    }

    @Override
    protected void initShaderProgram() {
    }

    private double getFactor(double cspeed) {
        if (cspeed <= 0.4) {
            return 1;
        } else if (cspeed <= 1.0) {
            // lint(1..0.5)
            return MathUtilsd.lint(cspeed, 0.4, 1.0, 1.0, 0.5);
        } else if (cspeed <= 2.0) {
            // lint(0.5..0.3)
            return MathUtilsd.lint(cspeed, 1.0, 2.0, 0.5, 0.3);
        } else if (cspeed <= 3.0) {
            return 0.3;
        } else {
            // lint(0.3..0.1)
            return MathUtilsd.lint(cspeed, 3, 5, 0.3, 0.1);
        }
    }

    private void updatePositions(ICamera cam) {
        double tu = cam.getCurrent().getTranslateUnits();
        double dist = 1200000 * tu * Constants.KM_TO_U * getFactor(GlobalConf.scene.CAMERA_SPEED);

        // If focus is very close, stop (jittering errors kick in)
        if (cam.getMode().isFocus()) {
            /**
             * Empirical fit to determine where the particle effects start to break down wrt distance to sol (since they are positioned globally)
             * <a href="https://mycurvefit.com/share/7b20c3bf-267d-4a8a-9498-844832d6509b">See curve and fit</a>
             */
            double distToSol = cam.getFocus().getAbsolutePosition(aux1).len() * Constants.U_TO_KM;
            double focusDistKm = cam.getFocus().getDistToCamera() * Constants.U_TO_KM;
            double cutDistKm = 11714150000000000d + (1900.228d - 11714150000000000d) / (1d + Math.pow(distToSol / 93302269999999990000d, 1.541734d));
            if (focusDistKm < cutDistKm) {
                return;
            }
        }
        double dists = dist * 0.9;
        Vector3d campos = aux1.set(cam.getPos());
        for (int i = 0; i < N_PARTICLES * 2; i++) {
            Vector3d pos = aux5.set(positions[i]);
            if (i % 2 == 0) {
                // Base particle
                if (pos.dst(campos) > dist) {
                    pos.set(rand.nextDouble() - 0.5, rand.nextDouble() - 0.5, rand.nextDouble() - 0.5).scl(dists).add(campos);
                    pos.put(positions[i]);
                    additional[i].z = getT();
                    campositions[i / 2].set(campos);
                }
            } else {
                // Companion, use previous camera position
                Vector3d prev_campos = campositions[(i - 1) / 2];
                Vector3d camdiff = aux2.set(campos).sub(prev_campos);
                pos.set(positions[i - 1]).add(camdiff);
                pos.put(positions[i]);
            }
        }
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
        addMeshData(N_PARTICLES * 2);
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

        curr.vertices = new float[nVertices * (curr.mesh.getVertexAttributes().vertexSize / 4)];
        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        sizeOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
        tOffset = curr.mesh.getVertexAttribute(Usage.Tangent) != null ? curr.mesh.getVertexAttribute(Usage.Tangent).offset / 4 : 0;
        return mdi;
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<VertexAttribute>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 1, "a_size"));
        attribs.add(new VertexAttribute(Usage.Tangent, 1, "a_t"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void render(Array<IRenderable> renderables, ICamera camera, double t, RenderingContext rc) {
        this.rc = rc;
        run(preRunnable, renderables, camera);
        renderStud(renderables, camera, t);
        run(postRunnable, renderables, camera);
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        float alpha = getAlpha(ct);
        if (alpha > 0) {
            updatePositions(camera);

            // Enable GL_LINE_SMOOTH
            Gdx.gl20.glEnable(0xB20);
            // Enable GL_LINE_WIDTH
            Gdx.gl20.glEnable(0xB21);
            // Additive blending
            Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
            // Regular
            Gdx.gl.glLineWidth(1f * GlobalConf.SCALE_FACTOR);

            if (curr != null) {
                curr.vertexIdx = 0;
                curr.numVertices = N_PARTICLES * 2;
                for (int i = 0; i < N_PARTICLES * 2; i++) {
                    // COLOR
                    curr.vertices[curr.vertexIdx + curr.colorOffset] = additional[i].x;
                    // SIZE
                    curr.vertices[curr.vertexIdx + sizeOffset] = additional[i].y;
                    // T
                    curr.vertices[curr.vertexIdx + tOffset] = additional[i].z;
                    // POSITION
                    curr.vertices[curr.vertexIdx] = positions[i].x;
                    curr.vertices[curr.vertexIdx + 1] = positions[i].y;
                    curr.vertices[curr.vertexIdx + 2] = positions[i].z;

                    curr.vertexIdx += curr.vertexSize;
                }

                /**
                 * RENDER
                 */
                ShaderProgram shaderProgram = getShaderProgram();

                shaderProgram.begin();
                shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
                shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux1f));
                shaderProgram.setUniformf("u_alpha", alpha * 0.6f);
                shaderProgram.setUniformf("u_ar", GlobalConf.program.STEREOSCOPIC_MODE && (GlobalConf.program.STEREO_PROFILE != StereoProfile.HD_3DTV_HORIZONTAL && GlobalConf.program.STEREO_PROFILE != StereoProfile.ANAGLYPHIC) ? 0.5f : 1f);
                shaderProgram.setUniformf("u_sizeFactor", rc.scaleFactor);
                shaderProgram.setUniformf("u_t", getT());

                // Relativistic effects
                addEffectsUniforms(shaderProgram, camera);

                curr.mesh.setVertices(curr.vertices, 0, N_PARTICLES * 2 * curr.vertexSize);
                curr.mesh.render(shaderProgram, GL20.GL_LINES);
                shaderProgram.end();

                // Restore
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }

    }

}
