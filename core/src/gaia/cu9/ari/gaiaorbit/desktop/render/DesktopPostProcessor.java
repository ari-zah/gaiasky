/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.desktop.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.postprocessing.effects.*;
import com.bitfire.postprocessing.filters.Glow;
import com.bitfire.utils.ShaderLoader;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.PostprocessConf.Antialias;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf.StereoProfile;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

public class DesktopPostProcessor implements IPostProcessor, IObserver {
    private AssetManager manager;
    private PostProcessBean[] pps;

    float bloomFboScale = 0.5f;

    // Intensity of flare
    float flareIntensity = 0.4f;
    // Number of flares
    int nghosts = 6;
    // Number of samples for the light glow
    int lglowNSamples = 1;

    Vector3d auxd, prevCampos;
    Vector3 auxf;
    Matrix4 prevViewProj, prevCombined;

    public DesktopPostProcessor() {
        ShaderLoader.BasePath = "shaders/";

        auxd = new Vector3d();
        auxf = new Vector3();
        prevCampos = new Vector3d();
        prevViewProj = new Matrix4();
        prevCombined = new Matrix4();

    }

    public void initialize(AssetManager manager) {
        this.manager = manager;
        manager.load(GlobalConf.data.dataFile("tex/base/lenscolor.png"), Texture.class);
        if (GlobalConf.scene.isHighQuality()) {
            manager.load(GlobalConf.data.dataFile("tex/base/lensdirt.jpg"), Texture.class);
            manager.load(GlobalConf.data.dataFile("tex/base/star_glow.png"), Texture.class);
        } else {
            manager.load(GlobalConf.data.dataFile("tex/base/lensdirt_s.jpg"), Texture.class);
            manager.load(GlobalConf.data.dataFile("tex/base/star_glow_s.png"), Texture.class);
        }
        manager.load(GlobalConf.data.dataFile("tex/base/lensstarburst.jpg"), Texture.class);
    }

    public void doneLoading(AssetManager manager) {
        pps = new PostProcessBean[RenderType.values().length];

        pps[RenderType.screen.index] = newPostProcessor(getWidth(RenderType.screen), getHeight(RenderType.screen), manager);
        if (GlobalConf.screenshot.isRedrawMode())
            pps[RenderType.screenshot.index] = newPostProcessor(getWidth(RenderType.screenshot), getHeight(RenderType.screenshot), manager);
        if (GlobalConf.frame.isRedrawMode())
            pps[RenderType.frame.index] = newPostProcessor(getWidth(RenderType.frame), getHeight(RenderType.frame), manager);

        EventManager.instance.subscribe(this, Events.SCREENSHOT_SIZE_UDPATE, Events.FRAME_SIZE_UDPATE, Events.BLOOM_CMD, Events.LENS_FLARE_CMD, Events.MOTION_BLUR_CMD, Events.LIGHT_POS_2D_UPDATED, Events.LIGHT_SCATTERING_CMD, Events.FISHEYE_CMD, Events.CAMERA_MOTION_UPDATED, Events.CUBEMAP360_CMD, Events.ANTIALIASING_CMD, Events.BRIGHTNESS_CMD, Events.CONTRAST_CMD, Events.HUE_CMD, Events.SATURATION_CMD, Events.GAMMA_CMD, Events.STEREO_PROFILE_CMD, Events.STEREOSCOPIC_CMD, Events.FPS_INFO, Events.FOV_CHANGE_NOTIFICATION);
    }

    private int getWidth(RenderType type) {
        switch (type) {
        case screen:
            return Gdx.graphics.getWidth();
        case screenshot:
            return GlobalConf.screenshot.SCREENSHOT_WIDTH;
        case frame:
            return GlobalConf.frame.RENDER_WIDTH;
        }
        return 0;
    }

    private int getHeight(RenderType type) {
        switch (type) {
        case screen:
            return Gdx.graphics.getHeight();
        case screenshot:
            return GlobalConf.screenshot.SCREENSHOT_HEIGHT;
        case frame:
            return GlobalConf.frame.RENDER_HEIGHT;
        }
        return 0;
    }

    private PostProcessBean newPostProcessor(int width, int height, AssetManager manager) {
        PostProcessBean ppb = new PostProcessBean();

        float ar = (float) width / (float) height;

        ppb.pp = new PostProcessor(width, height, true, false, true);

        // LIGHT GLOW
        int lgw, lgh;
        Texture glow;
        // TODO Listen to GRAPHICS_QUALITY_CHANGED and apply new settings on the fly
        if (GlobalConf.scene.isHighQuality()) {
            lglowNSamples = 10;
            lgw = width;
            lgh = Math.round(lgw / ar);
            glow = manager.get(GlobalConf.data.dataFile("tex/base/star_glow.png"));
            Glow.N = 30;
        } else if (GlobalConf.scene.isNormalQuality()) {
            lglowNSamples = 5;
            lgw = 1280;
            lgh = Math.round(lgw / ar);
            glow = manager.get(GlobalConf.data.dataFile("tex/base/star_glow_s.png"));
            Glow.N = 20;
        } else {
            lglowNSamples = 1;
            lgw = 1000;
            lgh = Math.round(lgw / ar);
            glow = manager.get(GlobalConf.data.dataFile("tex/base/star_glow_s.png"));
            Glow.N = 10;
        }
        glow.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        ppb.lglow = new LightGlow(lgw, lgh);
        ppb.lglow.setLightGlowTexture(glow);
        ppb.lglow.setNSamples(lglowNSamples);
        ppb.lglow.setTextureScale(0.7f / GaiaSky.instance.cam.getFovFactor());
        ppb.lglow.setEnabled(GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING);
        ppb.pp.addEffect(ppb.lglow);

        // LENS FLARE
        float lensFboScale = 0.2f;
        Texture lcol = manager.get(GlobalConf.data.dataFile("tex/base/lenscolor.png"));
        lcol.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Texture ldirt = GlobalConf.scene.isHighQuality() ? manager.get(GlobalConf.data.dataFile("data/tex/base/lensdirt.jpg")) : manager.get(GlobalConf.data.dataFile("tex/base/lensdirt_s.jpg"));
        ldirt.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Texture lburst = manager.get(GlobalConf.data.dataFile("tex/base/lensstarburst.jpg"));
        lburst.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        ppb.lens = new LensFlare2((int) (width * lensFboScale), (int) (height * lensFboScale));
        ppb.lens.setGhosts(nghosts);
        ppb.lens.setHaloWidth(0.5f);
        ppb.lens.setLensColorTexture(lcol);
        ppb.lens.setLensDirtTexture(ldirt);
        ppb.lens.setLensStarburstTexture(lburst);
        ppb.lens.setFlareIntesity(GlobalConf.postprocess.POSTPROCESS_LENS_FLARE ? flareIntensity : 0f);
        ppb.lens.setFlareSaturation(0.6f);
        ppb.lens.setBaseIntesity(1f);
        ppb.lens.setBias(-0.98f);
        ppb.lens.setBlurPasses(35);
        ppb.lens.setEnabled(true);
        ppb.pp.addEffect(ppb.lens);

        // HDR
        //        ppb.hdr = new HDR();
        //        ppb.hdr.setExposure(1.0f);
        //        ppb.hdr.setGamma(2.2f);
        //        ppb.pp.addEffect(ppb.hdr);

        // BLOOM
        ppb.bloom = new Bloom((int) (width * bloomFboScale), (int) (height * bloomFboScale));
        ppb.bloom.setBloomIntesity(GlobalConf.postprocess.POSTPROCESS_BLOOM_INTENSITY);
        ppb.bloom.setThreshold(0.3f);
        ppb.bloom.setEnabled(GlobalConf.postprocess.POSTPROCESS_BLOOM_INTENSITY > 0);
        ppb.pp.addEffect(ppb.bloom);

        // DISTORTION (STEREOSCOPIC MODE)
        ppb.curvature = new Curvature();
        ppb.curvature.setDistortion(1.2f);
        ppb.curvature.setZoom(0.75f);
        ppb.curvature.setEnabled(GlobalConf.program.STEREOSCOPIC_MODE && GlobalConf.program.STEREO_PROFILE == StereoProfile.VR_HEADSET);
        ppb.pp.addEffect(ppb.curvature);

        // FISHEYE DISTORTION (DOME)
        ppb.fisheye = new Fisheye();
        ppb.fisheye.setEnabled(GlobalConf.postprocess.POSTPROCESS_FISHEYE);
        ppb.pp.addEffect(ppb.fisheye);

        // ANTIALIAS
        initAntiAliasing(GlobalConf.postprocess.POSTPROCESS_ANTIALIAS, width, height, ppb);

        // LEVELS - BRIGHTNESS & CONTRAST
        initLevels(ppb);

        // MOTION BLUR
        initMotionBlur(width, height, ppb);

        return ppb;
    }

    private void initLevels(PostProcessBean ppb) {
        ppb.levels = new Levels();
        ppb.levels.setBrightness(GlobalConf.postprocess.POSTPROCESS_BRIGHTNESS);
        ppb.levels.setContrast(GlobalConf.postprocess.POSTPROCESS_CONTRAST);
        ppb.levels.setHue(GlobalConf.postprocess.POSTPROCESS_HUE);
        ppb.levels.setSaturation(GlobalConf.postprocess.POSTPROCESS_SATURATION);
        ppb.levels.setGamma(GlobalConf.postprocess.POSTPROCESS_GAMMA);
        ppb.pp.addEffect(ppb.levels);
    }

    private void initMotionBlur(int width, int height, PostProcessBean ppb) {
        ppb.motionblur = new MotionBlur(width, height);
        ppb.motionblur.setBlurRadius(0.0f);
        ppb.motionblur.setBlurOpacity(GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR);
        ppb.motionblur.setEnabled(GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR > 0);
        ppb.pp.addEffect(ppb.motionblur);
    }

    private void initAntiAliasing(Antialias aavalue, int width, int height, PostProcessBean ppb) {
        if (aavalue.equals(Antialias.FXAA)) {
            ppb.antialiasing = new Fxaa(width, height);
            ((Fxaa) ppb.antialiasing).setSpanMax(8f);
            ((Fxaa) ppb.antialiasing).setReduceMin(1f / 8f);
            ((Fxaa) ppb.antialiasing).setReduceMul(1f / 4f);
            Logger.getLogger(this.getClass()).debug(I18n.bundle.format("notif.selected", "FXAA"));
        } else if (aavalue.equals(Antialias.NFAA)) {
            ppb.antialiasing = new Nfaa(width, height);
            Logger.getLogger(this.getClass()).debug(I18n.bundle.format("notif.selected", "NFAA"));
        }
        if (ppb.antialiasing != null) {
            ppb.antialiasing.setEnabled(GlobalConf.postprocess.POSTPROCESS_ANTIALIAS.isPostProcessAntialias());
            ppb.pp.addEffect(ppb.antialiasing);
        }
    }

    @Override
    public PostProcessBean getPostProcessBean(RenderType type) {
        return pps[type.index];
    }

    @Override
    public void resize(final int width, final int height) {
        Gdx.app.postRunnable(() -> replace(RenderType.screen.index, width, height));
    }

    @Override
    public void resizeImmediate(final int width, final int height) {
        replace(RenderType.screen.index, width, height);
    }

    @Override
    public void dispose() {
        if (pps != null)
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.dispose();
                }
            }
    }

    @Override
    public void notify(Events event, final Object... data) {
        switch (event) {
        case FOV_CHANGE_NOTIFICATION:
            float newfov = (Float) data[0];
            Gdx.app.postRunnable(() -> {
                for (int i = 0; i < RenderType.values().length; i++) {
                    if (pps[i] != null) {
                        PostProcessBean ppb = pps[i];
                        ppb.lglow.setNSamples(newfov > 65 ? 1 : lglowNSamples);
                    }
                }
            });
            break;
        case SCREENSHOT_SIZE_UDPATE:
            if (pps != null && GlobalConf.screenshot.isRedrawMode()) {
                int neww = (Integer) data[0];
                int newh = (Integer) data[1];
                if (pps[RenderType.screenshot.index] != null) {
                    if (changed(pps[RenderType.screenshot.index].pp, neww, newh)) {
                        Gdx.app.postRunnable(() -> {
                            replace(RenderType.screenshot.index, neww, newh);
                        });
                    }
                } else {
                    pps[RenderType.screenshot.index] = newPostProcessor(neww, newh, manager);
                }
            }
            break;
        case FRAME_SIZE_UDPATE:
            if (pps != null && GlobalConf.frame.isRedrawMode()) {
                int neww = (Integer) data[0];
                int newh = (Integer) data[1];
                if (pps[RenderType.frame.index] != null) {
                    if (changed(pps[RenderType.frame.index].pp, neww, newh)) {
                        Gdx.app.postRunnable(() -> {
                            replace(RenderType.frame.index, neww, newh);
                        });
                    }
                } else {
                    pps[RenderType.frame.index] = newPostProcessor(neww, newh, manager);
                }
            }
            break;
        case BLOOM_CMD:
            Gdx.app.postRunnable(() -> {
                float intensity = (float) data[0];
                for (int i = 0; i < RenderType.values().length; i++) {
                    if (pps[i] != null) {
                        PostProcessBean ppb = pps[i];
                        ppb.bloom.setBloomIntesity(intensity);
                        ppb.bloom.setEnabled(intensity > 0);
                    }
                }
            });
            break;
        case LENS_FLARE_CMD:
            boolean active = (Boolean) data[0];
            int nnghosts = active ? nghosts : 0;
            float intensity = active ? flareIntensity : 0;
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.lens.setGhosts(nnghosts);
                    ppb.lens.setFlareIntesity(intensity);
                    // ppb.lens.setEnabled(active);
                }
            }
            break;
        case LIGHT_SCATTERING_CMD:
            active = (Boolean) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.lglow.setEnabled(active);
                }
            }
            break;
        case FISHEYE_CMD:
            active = (Boolean) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.fisheye.setEnabled(active);
                    ppb.lglow.setNSamples(active ? 1 : lglowNSamples);
                }
            }
            break;
        case CAMERA_MOTION_UPDATED:
            Gdx.app.postRunnable(() -> {
                Vector3d campos = (Vector3d) data[0];
                PerspectiveCamera cam = (PerspectiveCamera) data[3];

                for (int i = 0; i < RenderType.values().length; i++) {
                    if (pps[i] != null) {
                        PostProcessBean ppb = pps[i];

                        // Motion blur
                        ppb.motionblur.setEnabled(GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR != 0);
                        ppb.motionblur.setBlurOpacity(0.94f);

                    }
                }
                prevCombined.set(cam.combined);
                prevCampos.set(campos);
            });
            break;
        case MOTION_BLUR_CMD:
            Gdx.app.postRunnable(() -> {
                float opacity = (float) data[0];
                for (int i = 0; i < RenderType.values().length; i++) {
                    if (pps[i] != null) {
                        PostProcessBean ppb = pps[i];
                        ppb.motionblur.setBlurOpacity(opacity);
                        ppb.motionblur.setEnabled(opacity > 0);
                    }
                }
            });
            break;
        case CUBEMAP360_CMD:
            boolean c360 = (Boolean) data[0];
            boolean enabled = !c360 && GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR > 0;
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.motionblur.setBlurOpacity(!enabled ? 0 : GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR);
                    ppb.motionblur.setEnabled(enabled);
                    ppb.lglow.setNSamples(enabled ? 1 : lglowNSamples);
                }
            }

            break;

        case LIGHT_POS_2D_UPDATED:
            Integer nLights = (Integer) data[0];
            float[] lightpos = (float[]) data[1];
            float[] angles = (float[]) data[2];
            float[] colors = (float[]) data[3];
            Texture prePass = (Texture) data[4];

            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.lglow.setLightPositions(nLights, lightpos);
                    ppb.lglow.setLightViewAngles(angles);
                    ppb.lglow.setLightColors(colors);
                    ppb.lglow.setTextureScale(0.7f / GaiaSky.instance.cam.getFovFactor());
                    if (prePass != null)
                        ppb.lglow.setPrePassTexture(prePass);
                }
            }
            break;
        case STEREOSCOPIC_CMD:
            updateStereo((boolean) data[0], GlobalConf.program.STEREO_PROFILE);
            break;
        case STEREO_PROFILE_CMD:
            updateStereo(GlobalConf.program.STEREOSCOPIC_MODE, StereoProfile.values()[(Integer) data[0]]);
            break;
        case ANTIALIASING_CMD:
            final Antialias aavalue = (Antialias) data[0];
            Gdx.app.postRunnable(() -> {
                for (int i = 0; i < RenderType.values().length; i++) {
                    if (pps[i] != null) {
                        PostProcessBean ppb = pps[i];
                        if (aavalue.isPostProcessAntialias()) {
                            // clean
                            if (ppb.antialiasing != null) {
                                ppb.antialiasing.setEnabled(false);
                                ppb.pp.removeEffect(ppb.antialiasing);
                                ppb.antialiasing = null;
                            }
                            // update
                            initAntiAliasing(aavalue, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), ppb);
                            // ensure motion blur and levels go after
                            ppb.pp.removeEffect(ppb.levels);
                            initLevels(ppb);
                            ppb.pp.removeEffect(ppb.motionblur);
                            initMotionBlur(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), ppb);

                        } else {
                            // remove
                            if (ppb.antialiasing != null) {
                                ppb.antialiasing.setEnabled(false);
                                ppb.pp.removeEffect(ppb.antialiasing);
                                ppb.antialiasing = null;
                            }
                        }
                    }
                }
            });
            break;
        case BRIGHTNESS_CMD:
            float br = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.levels.setBrightness(br);
                }
            }
            break;
        case CONTRAST_CMD:
            float cn = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.levels.setContrast(cn);
                }
            }
            break;
        case HUE_CMD:
            float hue = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.levels.setHue(hue);
                }
            }
            break;
        case SATURATION_CMD:
            float sat = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.levels.setSaturation(sat);
                }
            }
            break;
        case GAMMA_CMD:
            float gamma = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.levels.setGamma(gamma);
                    ;
                }
            }
            break;
        case FPS_INFO:
            Float fps = (Float) data[0];
            for (int i = 0; i < RenderType.values().length; i++) {
                if (pps[i] != null) {
                    PostProcessBean ppb = pps[i];
                    ppb.motionblur.setBlurOpacity(MathUtils.clamp(fps * 1.5f / 60f, 0.2f, 0.95f));
                }
            }
            break;
        default:
            break;
        }

    }

    /**
     * Reloads the postprocessor at the given index with the given width and
     * height.new Runnable() {
     * 
     * @Override public void run()
     * 
     * @param index
     * @param width
     * @param height
     */
    private void replace(int index, final int width, final int height) {
        // Dispose of old post processor
        pps[index].dispose();
        // Create new
        pps[index] = newPostProcessor(width, height, manager);
    }

    private boolean changed(PostProcessor postProcess, int width, int height) {
        return (postProcess.getCombinedBuffer().width != width || postProcess.getCombinedBuffer().height != height);
    }

    @Override
    public boolean isLightScatterEnabled() {
        return pps[RenderType.screen.index].lglow.isEnabled();
    }

    private void updateStereo(boolean stereo, StereoProfile profile) {
        boolean curvatureEnabled = stereo && profile == StereoProfile.VR_HEADSET;
        boolean viewportHalved = stereo && profile != StereoProfile.ANAGLYPHIC && profile != StereoProfile.HD_3DTV_HORIZONTAL;

        for (int i = 0; i < RenderType.values().length; i++) {
            if (pps[i] != null) {
                PostProcessBean ppb = pps[i];
                ppb.curvature.setEnabled(curvatureEnabled);

                RenderType currentRenderType = RenderType.values()[i];
                ppb.lglow.setViewportSize(getWidth(currentRenderType) / (viewportHalved ? 2 : 1), getHeight(currentRenderType));
            }
        }
    }

}
