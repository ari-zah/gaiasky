/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import gaiasky.GaiaSky;
import gaiasky.desktop.format.DesktopNumberFormat;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.I3DTextRenderable;
import gaiasky.render.ILineRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.GlobalResources;
import gaiasky.util.Pair;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

/**
 * Cosmic ruler between two objects
 * @author tsagrista
 *
 */
public class CosmicRuler extends AbstractPositionEntity implements I3DTextRenderable, ILineRenderable, IObserver {
    private String name0, name1;
    private double[] pos0, pos1;
    private Vector3d p0, p1, m;
    private boolean rulerOk = false;
    private String dist;
    private ISceneGraph sg;
    private INumberFormat nf;

    public CosmicRuler() {
        super();
        this.parentName = "Universe";
        this.pos0 = new double[3];
        this.pos1 = new double[3];
        this.p0 = new Vector3d();
        this.p1 = new Vector3d();
        this.m = new Vector3d();
        this.sg = GaiaSky.instance.sg;
        this.name = "Cosmicruler";
        this.cc = new float[] { 1f, 1f, 0f };
        this.nf = new DesktopNumberFormat("0.#########E0");
        setCt("Ruler");
        EventManager.instance.subscribe(this, Events.RULER_ATTACH_0, Events.RULER_ATTACH_1, Events.RULER_CLEAR);
    }

    public CosmicRuler(String name0, String name1) {
        this();
        this.name0 = name0;
        this.name1 = name1;
    }

    @Override
    public float getLineWidth() {
        return 1f;
    }

    // Render Line
    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        double va = 0.01 * camera.getFovFactor();
        
        // Main line
        renderer.addLine(this, p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, cc[0], cc[1], cc[2], alpha);
        // Cap 1
        addCap(p0, p1, va, renderer, alpha);
        // Cap 1
        addCap(p1, p0, va, renderer, alpha);
    }
    
    private void addCap(Vector3d p0, Vector3d p1, double va, LineRenderSystem renderer, float alpha) {
        // cpos-p0
        Vector3d cp = aux3d2.get().set(p0);
        // cross(cpos-p0, p0-p1)
        Vector3d crs = aux3d1.get().set(p1).sub(p0).crs(cp);
        
        double d = p0.len();
        double caplen = FastMath.tan(va) * d;
        crs.setLength(caplen);
        Vector3d aux0 = aux3d2.get().set(p0).add(crs);
        Vector3d aux1 = aux3d3.get().set(p0).sub(crs);
        renderer.addLine(this, aux0.x, aux0.y, aux0.z, aux1.x, aux1.y, aux1.z, cc[0], cc[1], cc[2], alpha);
    }

    // Render Label
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        // 3D distance font
        Vector3d pos = aux3d1.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", 90f);
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thOverFactor", 1f);
        shader.setUniformf("u_thOverFactorScl", 1f);

        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor());
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (rulerOk) {
            addToRender(this, RenderGroup.LINE);
            addToRender(this, RenderGroup.FONT_LABEL);
        }
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        // Update positions
        rulerOk = (sg.getObjectPosition(name0, pos0) != null);
        rulerOk = rulerOk && (sg.getObjectPosition(name1, pos1) != null);

        if (rulerOk) {
            p0.set(pos0).add(translation);
            p1.set(pos1).add(translation);
            // Mid-point
            m.set(p1).sub(p0).scl(0.5).add(p0);
            pos.set(m).sub(translation);
            // Distance in internal units
            double dst = p0.dst(p1);
            Pair<Double, String> d = GlobalResources.doubleToDistanceString(dst);
            dist = nf.format(d.getFirst()) + " " + d.getSecond();
            
            Gdx.app.postRunnable(()->{
               EventManager.instance.post(Events.RULER_DIST, dst, dist); 
            });
        } else {
            dist = null;
        }

    }

    public String getName0() {
        return name0;
    }

    public void setName0(String name0) {
        this.name0 = name0;
    }

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public boolean rulerOk() {
        return rulerOk;
    }
    
    /**
     * Returns true if the ruler is attached to at least one object.
     * @return Ture if the ruler is attached.
     */
    public boolean hasAttached() {
        return name0 != null || name1 != null;
    }

    public boolean hasObject0() {
        return name0 != null && !name0.isEmpty();
    }

    public boolean hasObject1() {
        return name1 != null && !name1.isEmpty();
    }

    @Override
    public boolean renderText() {
        return rulerOk;
    }

    @Override
    public float[] textColour() {
        return new float[] { 1f, 1f, 0f, 1f };
    }

    @Override
    public float textSize() {
        return (float)(0.0005 * distToCamera);
    }

    @Override
    public float textScale() {
        return 0.2f;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        out.set(m);
        
        double len = out.len();
        out.clamp(0, len - getRadius()).scl(0.9f);

        Vector3d aux = aux3d2.get();
        aux.set(cam.getUp());

        aux.crs(out).nor();

        float dist = -0.025f * (float) out.len();

        aux.add(cam.getUp()).nor().scl(dist);

        out.add(aux);
    }

    @Override
    public String text() {
        return dist;
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
    }

    @Override
    public boolean isLabel() {
        return true;
    }

    @Override
    public float getTextOpacity(){
        return getOpacity();
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case RULER_ATTACH_0:
            String name = (String) data[0];
            setName0(name);
            break;
        case RULER_ATTACH_1:
            name = (String) data[0];
            setName1(name);
            break;
        case RULER_CLEAR:
            setName0(null);
            setName1(null);
            break;
        default:
            break;
        }
    }

    @Override
    public int getGlType() {
        return GL20.GL_LINES;
    }
}