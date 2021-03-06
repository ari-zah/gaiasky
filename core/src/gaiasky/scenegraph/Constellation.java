/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.I3DTextRenderable;
import gaiasky.render.ILineRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.IPosition;

/**
 * Represents a constellation object.
 */
public class Constellation extends FadeNode implements ILineRenderable, I3DTextRenderable {
    private static final Array<Constellation> allConstellations = new Array<>(false, 88);
    private double deltaYears;

    public static void updateConstellations(ISceneGraph sceneGraph) {
        for (Constellation c : allConstellations) {
            c.setUp(sceneGraph);
        }
    }

    private float alpha = .2f;
    private boolean allLoaded = false;
    private Vector3d posd;

    /** List of pairs of HIP identifiers **/
    public Array<int[]> ids;
    /**
     * The lines themselves as pairs of positions
     **/
    public IPosition[][] lines;

    public Constellation() {
        super();
        cc = new float[] { .5f, 1f, .5f, alpha };
        this.posd = new Vector3d();
    }

    public Constellation(String name, String parentName) {
        this();
        this.names = new String[] { name };
        this.parentName = parentName;
    }

    @Override
    public void initialize() {
        allConstellations.add(this);
    }

    public void update(ITimeFrameProvider time, final Vector3b parentTransform, ICamera camera) {
        update(time, parentTransform, camera, 1f);
    }

    public void update(ITimeFrameProvider time, final Vector3b parentTransform, ICamera camera, float opacity) {
        // Recompute mean position
        posd.setZero();
        Vector3d p = aux3d1.get();
        int nStars = 0;
        for (IPosition[] line : lines) {
            if (line != null) {
                p.set(line[0].getPosition()).add(camera.getInversePos());
                posd.add(p);
                nStars++;
            }
        }
        if (nStars > 0) {
            posd.scl(1d / nStars);
            posd.nor().scl(100d * Constants.PC_TO_U);
            pos.set(posd);

            super.update(time, parentTransform, camera, opacity);

            addToRenderLists(camera);

            deltaYears = AstroUtils.getMsSince(time.getTime(), AstroUtils.JD_J2015_5) * Nature.MS_TO_Y;
        }

    }

    @Override
    public void setUp(ISceneGraph sceneGraph) {
        if (!allLoaded) {
            int nPairs = ids.size;
            if (lines == null) {
                lines = new IPosition[nPairs][];
            }
            ObjectMap<Integer, IPosition> hipMap = sceneGraph.getStarMap();
            allLoaded = true;
            for (int i = 0; i < nPairs; i++) {
                int[] pair = ids.get(i);
                IPosition s1, s2;
                s1 = hipMap.get(pair[0]);
                s2 = hipMap.get(pair[1]);
                if (lines[i] == null && s1 != null && s2 != null) {
                    lines[i] = new IPosition[] { s1, s2 };
                } else {
                    allLoaded = false;
                }
            }
        }
    }

    /**
     * Line rendering.
     */
    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        alpha *= this.alpha * opacity;

        Vector3d p1 = aux3d1.get();
        Vector3d p2 = aux3d2.get();
        Vector3b campos = camera.getPos();

        for (IPosition[] pair : lines) {
            if (pair != null) {
                getPosition(pair[0], campos, p1);
                getPosition(pair[1], campos, p2);

                renderer.addLine(this, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, cc[0], cc[1], cc[2], alpha);
            }
        }

    }

    private void getPosition(IPosition posBean, Vector3b camPos, Vector3d out) {
        Vector3d vel = aux3d3.get().setZero();
        if (posBean.getVelocity() != null && !posBean.getVelocity().hasNaN()) {
            vel.set(posBean.getVelocity()).scl(deltaYears);
        }
        out.set(posBean.getPosition()).sub(camPos).add(vel);
    }

    /**
     * Label rendering.
     */
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        Vector3d pos = aux3d1.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1);
        shader.setUniformf("u_thOverFactor", 1);
        shader.setUniformf("u_thOverFactorScl", 1);
        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, distToCamera, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor());
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender()) {
            addToRender(this, RenderGroup.LINE);
            if (renderText()) {
                addToRender(this, RenderGroup.FONT_LABEL);
            }
        }
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

    @Override
    public float[] textColour() {
        return cc;
    }

    @Override
    public float textSize() {
        return .2e7f;
    }

    @Override
    public float textScale() {
        return .2f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        out.set(pos);
        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public String text() {
        return names[0];
    }

    @Override
    public boolean renderText() {
        return GaiaSky.instance.isOn(ComponentType.Labels);
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    @Override
    public boolean isLabel() {
        return true;
    }

    @Override
    public float getTextOpacity() {
        return getOpacity();
    }

    @Override
    public float getLineWidth() {
        return 1;
    }

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINES;
    }
}
