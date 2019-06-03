/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;

public class StarPointRenderSystem extends ImmediateRenderSystem implements IObserver {
    private final double BRIGHTNESS_FACTOR;

    boolean starColorTransit = false;
    Vector3 aux;
    int sizeOffset, pmOffset;
    ComponentType ct;
    float[] pointAlpha, alphaSizeFovBr;

    boolean initializing;

    public StarPointRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] shaders, ComponentType ct) {
        super(rg, alphas, shaders);
        EventManager.instance.subscribe(this, Events.TRANSIT_COLOUR_CMD, Events.ONLY_OBSERVED_STARS_CMD, Events.STAR_MIN_OPACITY_CMD);
        BRIGHTNESS_FACTOR = 10;
        this.ct = ct;
        this.alphaSizeFovBr = new float[4];
        initializing = true;
    }

    @Override
    protected void initShaderProgram() {
        pointAlpha = new float[] { GlobalConf.scene.POINT_ALPHA_MIN, GlobalConf.scene.POINT_ALPHA_MIN + GlobalConf.scene.POINT_ALPHA_MAX };

        for (ShaderProgram p : programs) {
            if (p != null) {
                p.begin();
                p.setUniform2fv("u_pointAlpha", pointAlpha, 0, 2);
                p.end();
            }
        }

    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
        curr = new MeshData();
        meshes.add(curr);

        aux = new Vector3();

        /** Init renderer **/
        int nVertices = 30;

        VertexAttribute[] attribs = buildVertexAttributes();
        curr.mesh = new IntMesh(false, nVertices, 0, attribs);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        pmOffset = curr.mesh.getVertexAttribute(Usage.Tangent) != null ? curr.mesh.getVertexAttribute(Usage.Tangent).offset / 4 : 0;
        sizeOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Tangent, 3, "a_pm"));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 1, "a_size"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (POINT_UPDATE_FLAG) {
            // Reset variables
            curr.clear();
            
            int size = renderables.size;
            ensureTempVertsSize(size * curr.vertexSize);
            for (int i = 0; i < size; i++) {
                // 2 FPS gain
                CelestialBody cb = (CelestialBody) renderables.get(i);
                float[] col = starColorTransit ? cb.ccTransit : cb.cc;

                // COLOR
                tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(col[0], col[1], col[2], cb.opacity);

                // SIZE
                tempVerts[curr.vertexIdx + sizeOffset] = (float) cb.getRadius();

                // POSITION
                aux.set((float) cb.pos.x, (float) cb.pos.y, (float) cb.pos.z);
                final int idx = curr.vertexIdx;
                tempVerts[idx] = aux.x;
                tempVerts[idx + 1] = aux.y;
                tempVerts[idx + 2] = aux.z;

                // PROPER MOTION
                tempVerts[curr.vertexIdx + pmOffset] = (float) cb.getPmX() * 0f;
                tempVerts[curr.vertexIdx + pmOffset + 1] = (float) cb.getPmY() * 0f;
                tempVerts[curr.vertexIdx + pmOffset + 2] = (float) cb.getPmZ() * 0f;

                curr.vertexIdx += curr.vertexSize;
            }
            // Put flag down
            POINT_UPDATE_FLAG = false;
        }
        if (!camera.getMode().isGaiaFov()) {
            if (Gdx.app.getType() == ApplicationType.Desktop) {
                // Enable gl_PointCoord
                Gdx.gl20.glEnable(34913);
                // Enable point sizes
                Gdx.gl20.glEnable(0x8642);
            }

            // Additive blending
            Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
            int fovmode = camera.getMode().getGaiaFovMode();

            ShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();
            shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
            shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux));

            alphaSizeFovBr[0] = alphas[ct.ordinal()];
            alphaSizeFovBr[1] = fovmode == 0 ? GlobalConf.scene.STAR_POINT_SIZE * rc.scaleFactor * (GlobalConf.program.isStereoFullWidth() ? 1 : 2) : GlobalConf.scene.STAR_POINT_SIZE * rc.scaleFactor * 10;
            alphaSizeFovBr[2] = camera.getFovFactor();
            alphaSizeFovBr[3] = (float) (GlobalConf.scene.STAR_BRIGHTNESS * BRIGHTNESS_FACTOR);
            shaderProgram.setUniform4fv("u_alphaSizeFovBr", alphaSizeFovBr, 0, 4);

            shaderProgram.setUniformf("u_t", (float) AstroUtils.getMsSinceJ2000(GaiaSky.instance.time.getTime()));
            shaderProgram.setUniformf("u_ar", GlobalConf.program.isStereoHalfWidth() ? 0.5f : 1f);
            shaderProgram.setUniformf("u_thAnglePoint", (float) GlobalConf.scene.STAR_THRESHOLD_POINT);

            // Relativistic effects
            addEffectsUniforms(shaderProgram, camera);

            curr.mesh.setVertices(tempVerts, 0, curr.vertexIdx);
            curr.mesh.render(shaderProgram, ShapeType.Point.getGlType());
            shaderProgram.end();

            // Restore
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

    }


    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case TRANSIT_COLOUR_CMD:
            starColorTransit = (boolean) data[1];
            POINT_UPDATE_FLAG = true;
            break;
        case ONLY_OBSERVED_STARS_CMD:
            POINT_UPDATE_FLAG = true;
            break;
        case STAR_MIN_OPACITY_CMD:
            for (ShaderProgram p : programs) {
                if (p != null && p.isCompiled()) {
                    pointAlpha[0] = (float) data[0];
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            p.begin();
                            p.setUniform2fv("u_pointAlpha", pointAlpha, 0, 2);
                            p.end();
                        }

                    });
                }
            }
            break;
        default:
            break;
        }
    }

    @Override
    public void resize(int w, int h) {

    }
}
