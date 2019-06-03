/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.ModelComponent;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.TextureComponent;
import gaia.cu9.ari.gaiaorbit.util.coord.StaticCoordinates;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

import java.util.HashMap;
import java.util.Map;

public class BillboardGalaxy extends Billboard {

    protected static final double TH_ANGLE_POINT_M = Math.toRadians(0.9);


    public BillboardGalaxy(){
        super();
    }

    public BillboardGalaxy(String name, String altname, double alpha, double delta, double dist, double sizePc, String tex) {
        super();
        setName(name);
        setAltname(altname);
        setColor(new double[] { 1, 1, 1, 1 });
        setSizepc(sizePc);
        setCt("Galaxies");
        setParent("NBG");
        StaticCoordinates sc = new StaticCoordinates();
        sc.setEquatorial(new double[] { alpha, delta, dist });
        setCoordinates(sc);
        ModelComponent mc = new ModelComponent(true);
        mc.setType("twofacedbillboard");
        mc.setStaticlight(1.0);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("diameter", 1.0);
        mc.setParams(params);
        TextureComponent tc = new TextureComponent();
        tc.setBase(tex);
        mc.setTexture(tc);
        setModel(mc);
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        super.updateLocalValues(time, camera);
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (GaiaSky.instance.isOn(ct)) {
            double thPoint = (TH_ANGLE_POINT_M * camera.getFovFactor()) / sizeScaleFactor;
            //if (viewAngleApparent >= thPoint) {
            //    addToRender(this, RenderGroup.MODEL_NORMAL);
            //} else if (opacity > 0) {
                addToRender(this, RenderGroup.BILLBOARD_GAL);
            //}

            if (renderText()) {
                addToRender(this, RenderGroup.FONT_LABEL);
            }

        }
    }

    @Override
    public void render(ShaderProgram shader, float alpha, IntMesh mesh, ICamera camera) {
        compalpha = alpha;

        float size = getFuzzyRenderSize(camera) * 1e-4f;

        Vector3 aux = aux3f1.get();
        shader.setUniformf("u_pos", translation.put(aux));
        shader.setUniformf("u_size", size);

        shader.setUniformf("u_color", ccPale[0], ccPale[1], ccPale[2], alpha);
        shader.setUniformf("u_alpha", alpha * opacity);
        shader.setUniformf("u_distance", (float) distToCamera);
        shader.setUniformf("u_apparent_angle", (float) viewAngleApparent);
        shader.setUniformf("u_time", (float) GaiaSky.instance.getT() / 5f);

        shader.setUniformf("u_radius", size);

        // Sprite.render
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    @Override
    public boolean renderText() {
        return name != null && GaiaSky.instance.isOn(ComponentType.Labels) && (opacity > 0 || fadeOpacity > 0);
    }

}
