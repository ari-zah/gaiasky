/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.IPostProcessor.PostProcessBean;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.GlobalConf;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections.CubemapProjection;
import gaiasky.util.gdx.contrib.postprocess.filters.Copy;

import java.util.Set;

/**
 * Renders the cube map projection mode. Basically, it renders the six sides of
 * the cube map (front, back, up, down, right, left) with a 90 degree fov each
 * and applies a cube map projection (spherical, cylindrical, hammer, fisheye)
 */
public class SGRCubemapProjections extends SGRCubemap implements ISGR, IObserver {

    private final CubemapProjections cubemapEffect;
    private final Copy copy;

    public SGRCubemapProjections() {
        super();

        cubemapEffect = new CubemapProjections(0, 0);
        setPlanetariumAngle(GlobalConf.program.PLANETARIUM_ANGLE);
        setPlanetariumAperture(GlobalConf.program.PLANETARIUM_APERTURE);
        setProjection(GlobalConf.program.CUBEMAP_PROJECTION);

        copy = new Copy();

        EventManager.instance.subscribe(this, Events.CUBEMAP_RESOLUTION_CMD, Events.CUBEMAP_PROJECTION_CMD, Events.CUBEMAP_CMD, Events.PLANETARIUM_APERTURE_CMD, Events.PLANETARIUM_ANGLE_CMD);
    }

    private void setProjection(CubemapProjection projection) {
        if (cubemapEffect != null) {
            cubemapEffect.setProjection(projection);
        }
        switch (projection) {
            case FISHEYE:
                // In planetarium mode we only render back iff aperture > 180
                xposFlag = true;
                xnegFlag = true;
                yposFlag = true;
                ynegFlag = true;
                zposFlag = true;
                znegFlag = cubemapEffect.getPlanetariumAperture() > 180f;
                setPlanetariumAngle(GlobalConf.program.PLANETARIUM_ANGLE);
                break;
            default:
                // In 360 mode we always need all sides
                xposFlag = true;
                xnegFlag = true;
                yposFlag = true;
                ynegFlag = true;
                zposFlag = true;
                znegFlag = true;
                setPlanetariumAngle(0);
                break;
        }
    }

    private void setPlanetariumAngle(float planetariumAngle) {
        // We do not use the planetarium angle in the effect because
        // we optimize the rendering of the cubemap sides when
        // using planetarium mode and the aperture is <= 180 by
        // skipping the -Z direction (back). We manipulate
        // the cameras before rendering instead.
        cubemapEffect.setPlanetariumAngle(0);
        angleFromZenith = planetariumAngle;
    }

    private void setPlanetariumAperture(float planetariumAperture) {
        cubemapEffect.setPlanetariumAperture(planetariumAperture);
    }

    @Override
    public void render(SceneGraphRenderer sgr, ICamera camera, double t, int rw, int rh, int tw, int th, FrameBuffer fb, PostProcessBean ppb) {
        // This renders the cubemap to [x|y|z][pos|neg]fb
        super.renderCubemapSides(sgr, camera, t, rw, rh, ppb);

        // Render to frame buffer
        resultBuffer = fb == null ? getFrameBuffer(rw, rh) : fb;
        cubemapEffect.setViewportSize(tw, th);
        cubemapEffect.setSides(xposfb, xnegfb, yposfb, ynegfb, zposfb, znegfb);
        cubemapEffect.render(null, resultBuffer, null);

        // To screen
        if (fb == null)
            copy.setInput(resultBuffer).setOutput(null).render();

        // Post render actions
        super.postRender(fb);
    }

    @Override
    public void resize(int w, int h) {

    }

    @Override
    public void dispose() {
        Set<Integer> keySet = fbcm.keySet();
        for (Integer key : keySet) {
            fbcm.get(key).dispose();
        }
    }

    @Override
    public void notify(final Events event, final Object... data) {
        if (!GlobalConf.runtime.OPENVR) {
            switch (event) {
                case CUBEMAP_CMD:
                    CubemapProjection p = (CubemapProjection) data[1];
                    GaiaSky.postRunnable(() -> {
                        setProjection(p);
                    });
                    break;
                case CUBEMAP_PROJECTION_CMD:
                    p = (CubemapProjection) data[0];
                    GaiaSky.postRunnable(() -> {
                        setProjection(p);
                    });
                    break;
                case CUBEMAP_RESOLUTION_CMD:
                    int res = (Integer) data[0];
                    GaiaSky.postRunnable(() -> {
                        // Create new ones
                        if (!fbcm.containsKey(getKey(res, res, 0))) {
                            // Clear
                            dispose();
                            fbcm.clear();
                        } else {
                            // All good
                        }
                    });
                    break;
                case PLANETARIUM_APERTURE_CMD:
                    setPlanetariumAperture((float) data[0]);
                    break;
                case PLANETARIUM_ANGLE_CMD:
                    setPlanetariumAngle((float) data[0]);
                    break;
                default:
                    break;
            }
        }
    }

}
