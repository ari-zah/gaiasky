/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.desktop.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.interfce.KeyBindings.ProgramAction;
import gaia.cu9.ari.gaiaorbit.interfce.beans.ComboBoxBean;
import gaia.cu9.ari.gaiaorbit.interfce.beans.FileComboBoxBean;
import gaia.cu9.ari.gaiaorbit.interfce.beans.LangComboBoxBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.PostprocessConf.Antialias;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ScreenshotMode;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.scene2d.*;
import gaia.cu9.ari.gaiaorbit.util.validator.IValidator;
import gaia.cu9.ari.gaiaorbit.util.validator.IntValidator;
import gaia.cu9.ari.gaiaorbit.util.validator.RegexpValidator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * The default preferences window.
 *
 * @author tsagrista
 */
public class PreferencesWindow extends GenericDialog {
    private Array<Actor> contents;
    private Array<OwnLabel> labels;

    private IValidator widthValidator, heightValidator, screenshotsSizeValidator, frameoutputSizeValidator, limitfpsValidator;

    private INumberFormat nf3, nf1;

    private CheckBox fullscreen, windowed, vsync, limitfpsCb, multithreadCb, lodFadeCb, cbAutoCamrec, real, nsl, report, inverty, highAccuracyPositions, shadowsCb, pointerCoords, datasetChooser, debugInfo;
    private OwnSelectBox<DisplayMode> fullscreenResolutions;
    private OwnSelectBox<ComboBoxBean> gquality, aa, orbitRenderer, lineRenderer, numThreads, screenshotMode, frameoutputMode, nshadows;
    private OwnSelectBox<LangComboBoxBean> lang;
    private OwnSelectBox<String> theme;
    private OwnSelectBox<FileComboBoxBean> controllerMappings;
    private OwnTextField widthField, heightField, sswidthField, ssheightField, frameoutputPrefix, frameoutputFps, fowidthField, foheightField, camrecFps, cmResolution, smResolution, limitFps;
    private OwnSlider lodTransitions;
    private OwnTextButton screenshotsLocation, frameoutputLocation;
    private DatasetsWidget dw;

    // Backup values
    private float brightnessBak, contrastBak, hueBak, saturationBak, gammaBak, motionblurBak, bloomBak;
    private boolean lensflareBak, lightglowBak, debugInfoBak;

    public PreferencesWindow(Stage stage, Skin skin) {
        super(I18n.txt("gui.settings") + " - " + GlobalConf.version.version + " - " + I18n.txt("gui.build", GlobalConf.version.build), skin, stage);

        this.contents = new Array<>();
        this.labels = new Array<>();

        this.nf1 = NumberFormatFactory.getFormatter("0.0");
        this.nf3 = NumberFormatFactory.getFormatter("0.000");

        setAcceptText(I18n.txt("gui.saveprefs"));
        setCancelText(I18n.txt("gui.cancel"));

        // Build UI
        buildSuper();
    }

    @Override
    protected void build() {
        float contentw = 700 * GlobalConf.SCALE_FACTOR;
        float contenth = 700 * GlobalConf.SCALE_FACTOR;
        final float tawidth = 400 * GlobalConf.SCALE_FACTOR;
        float tabwidth = (GlobalConf.SCALE_FACTOR > 1.5 ? 180 : 220) * GlobalConf.SCALE_FACTOR;
        float textwidth = 65 * GlobalConf.SCALE_FACTOR;
        float scrollh = 330 * GlobalConf.SCALE_FACTOR;
        float controlsscrollw = 450 * GlobalConf.SCALE_FACTOR;
        float controllsscrollh = 250 * GlobalConf.SCALE_FACTOR;
        float sliderWidth = textwidth * 3;

        // Create the tab buttons
        VerticalGroup group = new VerticalGroup();
        group.align(Align.left | Align.top);

        final Button tabGraphics = new OwnTextIconButton(I18n.txt("gui.graphicssettings"), new Image(skin.getDrawable("icon-p-graphics")), skin, "toggle-big");
        tabGraphics.pad(pad5);
        tabGraphics.setWidth(tabwidth);
        final Button tabUI = new OwnTextIconButton(I18n.txt("gui.ui.interfacesettings"), new Image(skin.getDrawable("icon-p-interface")), skin, "toggle-big");
        tabUI.pad(pad5);
        tabUI.setWidth(tabwidth);
        final Button tabPerformance = new OwnTextIconButton(I18n.txt("gui.performance"), new Image(skin.getDrawable("icon-p-performance")), skin, "toggle-big");
        tabPerformance.pad(pad5);
        tabPerformance.setWidth(tabwidth);
        final Button tabControls = new OwnTextIconButton(I18n.txt("gui.controls"), new Image(skin.getDrawable("icon-p-controls")), skin, "toggle-big");
        tabControls.pad(pad5);
        tabControls.setWidth(tabwidth);
        final Button tabScreenshots = new OwnTextIconButton(I18n.txt("gui.screenshots"), new Image(skin.getDrawable("icon-p-screenshots")), skin, "toggle-big");
        tabScreenshots.pad(pad5);
        tabScreenshots.setWidth(tabwidth);
        final Button tabFrames = new OwnTextIconButton(I18n.txt("gui.frameoutput.title"), new Image(skin.getDrawable("icon-p-frameoutput")), skin, "toggle-big");
        tabFrames.pad(pad5);
        tabFrames.setWidth(tabwidth);
        final Button tabCamera = new OwnTextIconButton(I18n.txt("gui.camerarec.title"), new Image(skin.getDrawable("icon-p-camera")), skin, "toggle-big");
        tabCamera.pad(pad5);
        tabCamera.setWidth(tabwidth);
        final Button tab360 = new OwnTextIconButton(I18n.txt("gui.360.title"), new Image(skin.getDrawable("icon-p-360")), skin, "toggle-big");
        tab360.pad(pad5);
        tab360.setWidth(tabwidth);
        final Button tabData = new OwnTextIconButton(I18n.txt("gui.data"), new Image(skin.getDrawable("icon-p-data")), skin, "toggle-big");
        tabData.pad(pad5);
        tabData.setWidth(tabwidth);
        final Button tabGaia = new OwnTextIconButton(I18n.txt("gui.gaia"), new Image(skin.getDrawable("icon-p-gaia")), skin, "toggle-big");
        tabGaia.pad(pad5);
        tabGaia.setWidth(tabwidth);
        final Button tabSystem = new OwnTextIconButton(I18n.txt("gui.system"), new Image(skin.getDrawable("icon-p-system")), skin, "toggle-big");
        tabSystem.pad(pad5);
        tabSystem.setWidth(tabwidth);

        group.addActor(tabGraphics);
        group.addActor(tabUI);
        group.addActor(tabPerformance);
        group.addActor(tabControls);
        group.addActor(tabScreenshots);
        group.addActor(tabFrames);
        group.addActor(tabCamera);
        group.addActor(tab360);
        group.addActor(tabData);
        group.addActor(tabGaia);
        group.addActor(tabSystem);
        content.add(group).align(Align.left | Align.top).padLeft(pad5);

        // Create the tab content. Just using images here for simplicity.
        Stack tabContent = new Stack();
        tabContent.setSize(contentw, contenth);

        /*
         * ==== GRAPHICS ====
         */
        final Table contentGraphicsTable = new Table(skin);
        final OwnScrollPane contentGraphics = new OwnScrollPane(contentGraphicsTable, skin, "minimalist-nobg");
        contentGraphics.setHeight(scrollh);
        contentGraphics.setScrollingDisabled(true, false);
        contentGraphics.setFadeScrollBars(false);
        contents.add(contentGraphics);
        contentGraphicsTable.align(Align.top | Align.left);

        // RESOLUTION/MODE
        Label titleResolution = new OwnLabel(I18n.txt("gui.resolutionmode"), skin, "help-title");
        Table mode = new Table();

        // Full screen mode resolutions
        Array<DisplayMode> modes = new Array<DisplayMode>(Gdx.graphics.getDisplayModes());
        modes.sort((o1, o2) -> Integer.compare(o2.height * o2.width, o1.height * o1.width));
        fullscreenResolutions = new OwnSelectBox<>(skin);
        fullscreenResolutions.setWidth(textwidth * 3.3f);
        fullscreenResolutions.setItems(modes);

        DisplayMode selectedMode = null;
        for (DisplayMode dm : modes) {
            if (dm.width == GlobalConf.screen.FULLSCREEN_WIDTH && dm.height == GlobalConf.screen.FULLSCREEN_HEIGHT) {
                selectedMode = dm;
                break;
            }
        }
        if (selectedMode != null)
            fullscreenResolutions.setSelected(selectedMode);

        // Get current resolution
        Table windowedResolutions = new Table(skin);
        DisplayMode nativeMode = Gdx.graphics.getDisplayMode();
        widthValidator = new IntValidator(100, nativeMode.width);
        widthField = new OwnTextField(Integer.toString(MathUtils.clamp(GlobalConf.screen.SCREEN_WIDTH, 100, nativeMode.width)), skin, widthValidator);
        widthField.setWidth(textwidth);
        heightValidator = new IntValidator(100, nativeMode.height);
        heightField = new OwnTextField(Integer.toString(MathUtils.clamp(GlobalConf.screen.SCREEN_HEIGHT, 100, nativeMode.height)), skin, heightValidator);
        heightField.setWidth(textwidth);
        final OwnLabel widthLabel = new OwnLabel(I18n.txt("gui.width") + ":", skin);
        final OwnLabel heightLabel = new OwnLabel(I18n.txt("gui.height") + ":", skin);

        windowedResolutions.add(widthLabel).left().padRight(pad5);
        windowedResolutions.add(widthField).left().padRight(pad5);
        windowedResolutions.add(heightLabel).left().padRight(pad5);
        windowedResolutions.add(heightField).left().row();

        // Radio buttons
        fullscreen = new OwnCheckBox(I18n.txt("gui.fullscreen"), skin, "radio", pad5);
        fullscreen.addListener(event -> {
            if (event instanceof ChangeEvent) {
                selectFullscreen(fullscreen.isChecked(), widthField, heightField, fullscreenResolutions, widthLabel, heightLabel);
                return true;
            }
            return false;
        });
        fullscreen.setChecked(GlobalConf.screen.FULLSCREEN);

        windowed = new OwnCheckBox(I18n.txt("gui.windowed"), skin, "radio", pad5);
        windowed.addListener(event -> {
            if (event instanceof ChangeEvent) {
                selectFullscreen(!windowed.isChecked(), widthField, heightField, fullscreenResolutions, widthLabel, heightLabel);
                return true;
            }
            return false;
        });
        windowed.setChecked(!GlobalConf.screen.FULLSCREEN);
        selectFullscreen(GlobalConf.screen.FULLSCREEN, widthField, heightField, fullscreenResolutions, widthLabel, heightLabel);

        new ButtonGroup<>(fullscreen, windowed);

        // VSYNC
        vsync = new OwnCheckBox(I18n.txt("gui.vsync"), skin, "default", pad5);
        vsync.setChecked(GlobalConf.screen.VSYNC);

        // LIMIT FPS
        limitfpsValidator = new IntValidator(1, 1000);
        limitFps = new OwnTextField(Integer.toString(MathUtils.clamp(GlobalConf.screen.LIMIT_FPS, 1, 1000)), skin, limitfpsValidator);
        limitFps.setDisabled(GlobalConf.screen.LIMIT_FPS == 0);

        limitfpsCb = new OwnCheckBox(I18n.txt("gui.limitfps"), skin, "default", pad5);
        limitfpsCb.setChecked(GlobalConf.screen.LIMIT_FPS > 0);
        limitfpsCb.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                enableComponents(limitfpsCb.isChecked(), limitFps);
                return true;
            }
            return false;
        });

        mode.add(fullscreen).left().padRight(pad5 * 2);
        mode.add(fullscreenResolutions).left().row();
        mode.add(windowed).left().padRight(pad5 * 2).padTop(pad5 * 2);
        mode.add(windowedResolutions).left().padTop(pad5 * 2).row();
        mode.add(vsync).left().padTop(pad5 * 2).colspan(2).row();
        mode.add(limitfpsCb).left().padRight(pad5 * 2);
        mode.add(limitFps).left();

        // Add to content
        contentGraphicsTable.add(titleResolution).left().padBottom(pad5 * 2).row();
        contentGraphicsTable.add(mode).left().padBottom(pad5 * 4).row();

        // GRAPHICS SETTINGS
        Label titleGraphics = new OwnLabel(I18n.txt("gui.graphicssettings"), skin, "help-title");
        Table graphics = new Table();

        OwnLabel gqualityLabel = new OwnLabel(I18n.txt("gui.gquality"), skin);
        gqualityLabel.addListener(new TextTooltip(I18n.txt("gui.gquality.info"), skin));

        ComboBoxBean[] gqs = new ComboBoxBean[] { new ComboBoxBean(I18n.txt("gui.gquality.high"), 0), new ComboBoxBean(I18n.txt("gui.gquality.normal"), 1), new ComboBoxBean(I18n.txt("gui.gquality.low"), 2) };
        gquality = new OwnSelectBox<>(skin);
        gquality.setItems(gqs);
        gquality.setWidth(textwidth * 3f);
        gquality.setSelected(gqs[GlobalConf.scene.GRAPHICS_QUALITY]);

        OwnImageButton gqualityTooltip = new OwnImageButton(skin, "tooltip");
        gqualityTooltip.addListener(new TextTooltip(I18n.txt("gui.gquality.info"), skin));

        // AA
        OwnLabel aaLabel = new OwnLabel(I18n.txt("gui.aa"), skin);
        aaLabel.addListener(new TextTooltip(I18n.txt("gui.aa.info"), skin));

        ComboBoxBean[] aas = new ComboBoxBean[] { new ComboBoxBean(I18n.txt("gui.aa.no"), 0), new ComboBoxBean(I18n.txt("gui.aa.fxaa"), -1), new ComboBoxBean(I18n.txt("gui.aa.nfaa"), -2) };
        aa = new OwnSelectBox<>(skin);
        aa.setItems(aas);
        aa.setWidth(textwidth * 3f);
        aa.setSelected(aas[idxAa(2, GlobalConf.postprocess.POSTPROCESS_ANTIALIAS)]);

        OwnImageButton aaTooltip = new OwnImageButton(skin, "tooltip");
        aaTooltip.addListener(new TextTooltip(I18n.txt("gui.aa.info"), skin));

        // ORBITS
        OwnLabel orbitsLabel = new OwnLabel(I18n.txt("gui.orbitrenderer"), skin);
        ComboBoxBean[] orbitItems = new ComboBoxBean[] { new ComboBoxBean(I18n.txt("gui.orbitrenderer.line"), 0), new ComboBoxBean(I18n.txt("gui.orbitrenderer.gpu"), 1) };
        orbitRenderer = new OwnSelectBox<>(skin);
        orbitRenderer.setItems(orbitItems);
        orbitRenderer.setWidth(textwidth * 3f);
        orbitRenderer.setSelected(orbitItems[GlobalConf.scene.ORBIT_RENDERER]);

        // LINE RENDERER
        OwnLabel lrLabel = new OwnLabel(I18n.txt("gui.linerenderer"), skin);
        ComboBoxBean[] lineRenderers = new ComboBoxBean[] { new ComboBoxBean(I18n.txt("gui.linerenderer.normal"), 0), new ComboBoxBean(I18n.txt("gui.linerenderer.quad"), 1) };
        lineRenderer = new OwnSelectBox<>(skin);
        lineRenderer.setItems(lineRenderers);
        lineRenderer.setWidth(textwidth * 3f);
        lineRenderer.setSelected(lineRenderers[GlobalConf.scene.LINE_RENDERER]);

        // BLOOM
        bloomBak = GlobalConf.postprocess.POSTPROCESS_BLOOM_INTENSITY;
        OwnLabel bloomLabel = new OwnLabel(I18n.txt("gui.bloom"), skin, "default");
        OwnLabel bloom = new OwnLabel(Integer.toString((int) (GlobalConf.postprocess.POSTPROCESS_BLOOM_INTENSITY * 10)), skin);
        Slider bloomEffect = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, false, skin);
        bloomEffect.setName("bloom effect");
        bloomEffect.setWidth(sliderWidth);
        bloomEffect.setValue(GlobalConf.postprocess.POSTPROCESS_BLOOM_INTENSITY * 10f);
        bloomEffect.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.BLOOM_CMD, bloomEffect.getValue() / 10f, true);
                bloom.setText(Integer.toString((int) bloomEffect.getValue()));
                return true;
            }
            return false;
        });

        // LABELS
        labels.addAll(gqualityLabel, aaLabel, orbitsLabel, lrLabel, bloomLabel);

        // LENS FLARE
        lensflareBak = GlobalConf.postprocess.POSTPROCESS_LENS_FLARE;
        CheckBox lensFlare = new CheckBox(" " + I18n.txt("gui.lensflare"), skin);
        lensFlare.setName("lens flare");
        lensFlare.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.LENS_FLARE_CMD, lensFlare.isChecked(), true);
                return true;
            }
            return false;
        });
        lensFlare.setChecked(GlobalConf.postprocess.POSTPROCESS_LENS_FLARE);

        // LIGHT GLOW
        lightglowBak = GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING;
        CheckBox lightGlow = new CheckBox(" " + I18n.txt("gui.lightscattering"), skin);
        lightGlow.setName("light scattering");
        lightGlow.setChecked(GlobalConf.postprocess.POSTPROCESS_LIGHT_SCATTERING);
        lightGlow.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.LIGHT_SCATTERING_CMD, lightGlow.isChecked(), true);
                return true;
            }
            return false;
        });

        // MOTION BLUR
        motionblurBak = GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR;
        CheckBox motionBlur = new CheckBox(" " + I18n.txt("gui.motionblur"), skin);
        motionBlur.setName("motion blur");
        motionBlur.setChecked(GlobalConf.postprocess.POSTPROCESS_MOTION_BLUR != 0);
        motionBlur.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.MOTION_BLUR_CMD, motionBlur.isChecked() ? Constants.MOTION_BLUR_VALUE : 0.0f, true);
                return true;
            }
            return false;
        });

        graphics.add(gqualityLabel).left().padRight(pad5 * 4).padBottom(pad5);
        graphics.add(gquality).left().padRight(pad5 * 2).padBottom(pad5);
        graphics.add(gqualityTooltip).left().padBottom(pad5).row();
        final Cell<Actor> noticeGraphicsCell = graphics.add((Actor) null);
        noticeGraphicsCell.colspan(3).left().row();
        graphics.add(aaLabel).left().padRight(pad5 * 4).padBottom(pad5);
        graphics.add(aa).left().padRight(pad5 * 2).padBottom(pad5);
        graphics.add(aaTooltip).left().padBottom(pad5).row();
        graphics.add(orbitsLabel).left().padRight(pad5 * 4).padBottom(pad5);
        graphics.add(orbitRenderer).colspan(2).left().padBottom(pad5).row();
        graphics.add(lrLabel).left().padRight(pad5 * 4).padBottom(pad5);
        graphics.add(lineRenderer).colspan(2).left().padBottom(pad5).row();
        graphics.add(bloomLabel).left().padRight(pad5 * 4).padBottom(pad5);
        graphics.add(bloomEffect).left().padBottom(pad5);
        graphics.add(bloom).left().padBottom(pad5).row();
        graphics.add(lensFlare).colspan(3).left().padBottom(pad5).row();
        graphics.add(lightGlow).colspan(3).left().padBottom(pad5).row();
        graphics.add(motionBlur).colspan(3).left().padBottom(pad5).row();

        // Add to content
        contentGraphicsTable.add(titleGraphics).left().padBottom(pad5 * 2).row();
        contentGraphicsTable.add(graphics).left().padBottom(pad5 * 4).row();

        // SHADOWS
        Label titleShadows = new OwnLabel(I18n.txt("gui.graphics.shadows"), skin, "help-title");
        Table shadows = new Table();

        // SHADOW MAP RESOLUTION
        OwnLabel smResolutionLabel = new OwnLabel(I18n.txt("gui.graphics.shadows.resolution"), skin);
        smResolutionLabel.setDisabled(!GlobalConf.scene.SHADOW_MAPPING);
        IntValidator smResValidator = new IntValidator(128, 2048);
        smResolution = new OwnTextField(Integer.toString(MathUtils.clamp(GlobalConf.scene.SHADOW_MAPPING_RESOLUTION, 128, 2048)), skin, smResValidator);
        smResolution.setWidth(textwidth * 3f);
        smResolution.setDisabled(!GlobalConf.scene.SHADOW_MAPPING);

        // N SHADOWS
        OwnLabel nShadowsLabel = new OwnLabel("#" + I18n.txt("gui.graphics.shadows"), skin);
        nShadowsLabel.setDisabled(!GlobalConf.scene.SHADOW_MAPPING);
        ComboBoxBean[] nsh = new ComboBoxBean[] { new ComboBoxBean("1", 1), new ComboBoxBean("2", 2), new ComboBoxBean("3", 3), new ComboBoxBean("4", 4) };
        nshadows = new OwnSelectBox<ComboBoxBean>(skin);
        nshadows.setItems(nsh);
        nshadows.setWidth(textwidth * 3f);
        nshadows.setSelected(nsh[GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS - 1]);
        nshadows.setDisabled(!GlobalConf.scene.SHADOW_MAPPING);

        // ENABLE SHADOWS
        shadowsCb = new OwnCheckBox(I18n.txt("gui.graphics.shadows.enable"), skin, "default", pad5);
        shadowsCb.setChecked(GlobalConf.scene.SHADOW_MAPPING);
        shadowsCb.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                // Enable or disable resolution
                enableComponents(shadowsCb.isChecked(), smResolution, smResolutionLabel, nshadows, nShadowsLabel);
                return true;
            }
            return false;
        });

        // LABELS
        labels.add(smResolutionLabel);

        shadows.add(shadowsCb).left().padRight(pad5 * 2).padBottom(pad5).row();
        shadows.add(smResolutionLabel).left().padRight(pad5 * 4).padBottom(pad5);
        shadows.add(smResolution).left().padRight(pad5 * 2).padBottom(pad5).row();
        shadows.add(nShadowsLabel).left().padRight(pad5 * 4).padBottom(pad5);
        shadows.add(nshadows).left().padRight(pad5 * 2).padBottom(pad5);

        // Add to content
        contentGraphicsTable.add(titleShadows).left().padBottom(pad5 * 2).row();
        contentGraphicsTable.add(shadows).left().padBottom(pad5 * 4).row();

        // DISPLAY SETTINGS
        Label titleDisplay = new OwnLabel(I18n.txt("gui.graphics.imglevels"), skin, "help-title");
        Table display = new Table();

        brightnessBak = GlobalConf.postprocess.POSTPROCESS_BRIGHTNESS;
        contrastBak = GlobalConf.postprocess.POSTPROCESS_CONTRAST;
        hueBak = GlobalConf.postprocess.POSTPROCESS_HUE;
        saturationBak = GlobalConf.postprocess.POSTPROCESS_SATURATION;
        gammaBak = GlobalConf.postprocess.POSTPROCESS_GAMMA;

        /* Brightness */
        OwnLabel brightnessl = new OwnLabel(I18n.txt("gui.brightness"), skin, "default");
        Label brightnessLabel = new OwnLabel(Integer.toString((int) MathUtilsd.lint(GlobalConf.postprocess.POSTPROCESS_BRIGHTNESS, Constants.MIN_BRIGHTNESS, Constants.MAX_BRIGHTNESS, Constants.MIN_SLIDER, Constants.MAX_SLIDER)), skin);
        Slider brightness = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, false, skin);
        brightness.setName("brightness");
        brightness.setWidth(sliderWidth);
        brightness.setValue(MathUtilsd.lint(GlobalConf.postprocess.POSTPROCESS_BRIGHTNESS, Constants.MIN_BRIGHTNESS, Constants.MAX_BRIGHTNESS, Constants.MIN_SLIDER, Constants.MAX_SLIDER));
        brightnessLabel.setText(Integer.toString((int) brightness.getValue()));
        brightness.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.BRIGHTNESS_CMD, MathUtilsd.lint(brightness.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_BRIGHTNESS, Constants.MAX_BRIGHTNESS), true);
                brightnessLabel.setText(Integer.toString((int) brightness.getValue()));
                return true;
            }
            return false;
        });

        display.add(brightnessl).left().padRight(pad5 * 4).padBottom(pad5);
        display.add(brightness).left().padRight(pad5 * 2).padBottom(pad5);
        display.add(brightnessLabel).row();

        /* Contrast */
        OwnLabel contrastl = new OwnLabel(I18n.txt("gui.contrast"), skin, "default");
        Label contrastLabel = new OwnLabel(Integer.toString((int) MathUtilsd.lint(GlobalConf.postprocess.POSTPROCESS_CONTRAST, Constants.MIN_CONTRAST, Constants.MAX_CONTRAST, Constants.MIN_SLIDER, Constants.MAX_SLIDER)), skin);
        Slider contrast = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, false, skin);
        contrast.setName("contrast");
        contrast.setWidth(sliderWidth);
        contrast.setValue(MathUtilsd.lint(GlobalConf.postprocess.POSTPROCESS_CONTRAST, Constants.MIN_CONTRAST, Constants.MAX_CONTRAST, Constants.MIN_SLIDER, Constants.MAX_SLIDER));
        contrastLabel.setText(Integer.toString((int) contrast.getValue()));
        contrast.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.CONTRAST_CMD, MathUtilsd.lint(contrast.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_CONTRAST, Constants.MAX_CONTRAST), true);
                contrastLabel.setText(Integer.toString((int) contrast.getValue()));
                return true;
            }
            return false;
        });

        display.add(contrastl).left().padRight(pad5 * 4).padBottom(pad5);
        display.add(contrast).left().padRight(pad5 * 2).padBottom(pad5);
        display.add(contrastLabel).row();

        /* Hue */
        OwnLabel huel = new OwnLabel(I18n.txt("gui.hue"), skin, "default");
        Label hueLabel = new OwnLabel(Integer.toString((int) MathUtilsd.lint(GlobalConf.postprocess.POSTPROCESS_HUE, Constants.MIN_HUE, Constants.MAX_HUE, Constants.MIN_SLIDER, Constants.MAX_SLIDER)), skin);
        Slider hue = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, false, skin);
        hue.setName("hue");
        hue.setWidth(sliderWidth);
        hue.setValue(MathUtilsd.lint(GlobalConf.postprocess.POSTPROCESS_HUE, Constants.MIN_HUE, Constants.MAX_HUE, Constants.MIN_SLIDER, Constants.MAX_SLIDER));
        hueLabel.setText(Integer.toString((int) hue.getValue()));
        hue.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.HUE_CMD, MathUtilsd.lint(hue.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_HUE, Constants.MAX_HUE), true);
                hueLabel.setText(Integer.toString((int) hue.getValue()));
                return true;
            }
            return false;
        });

        display.add(huel).left().padRight(pad5 * 4).padBottom(pad5);
        display.add(hue).left().padRight(pad5 * 2).padBottom(pad5);
        display.add(hueLabel).row();

        /* Saturation */
        OwnLabel saturationl = new OwnLabel(I18n.txt("gui.saturation"), skin, "default");
        Label saturationLabel = new OwnLabel(Integer.toString((int) MathUtilsd.lint(GlobalConf.postprocess.POSTPROCESS_SATURATION, Constants.MIN_SATURATION, Constants.MAX_SATURATION, Constants.MIN_SLIDER, Constants.MAX_SLIDER)), skin);
        Slider saturation = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, false, skin);
        saturation.setName("saturation");
        saturation.setWidth(sliderWidth);
        saturation.setValue(MathUtilsd.lint(GlobalConf.postprocess.POSTPROCESS_SATURATION, Constants.MIN_SATURATION, Constants.MAX_SATURATION, Constants.MIN_SLIDER, Constants.MAX_SLIDER));
        saturationLabel.setText(Integer.toString((int) saturation.getValue()));
        saturation.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.SATURATION_CMD, MathUtilsd.lint(saturation.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_SATURATION, Constants.MAX_SATURATION), true);
                saturationLabel.setText(Integer.toString((int) saturation.getValue()));
                return true;
            }
            return false;
        });

        display.add(saturationl).left().padRight(pad5 * 4).padBottom(pad5);
        display.add(saturation).left().padRight(pad5 * 2).padBottom(pad5);
        display.add(saturationLabel).row();

        /* Gamma */
        OwnLabel gammal = new OwnLabel(I18n.txt("gui.gamma"), skin, "default");
        Label gammaLabel = new OwnLabel(nf1.format(GlobalConf.postprocess.POSTPROCESS_GAMMA), skin);
        Slider gamma = new OwnSlider(Constants.MIN_GAMMA, Constants.MAX_GAMMA, 0.1f, false, skin);
        gamma.setName("gamma");
        gamma.setWidth(sliderWidth);
        gamma.setValue(GlobalConf.postprocess.POSTPROCESS_GAMMA);
        gammaLabel.setText(nf1.format(gamma.getValue()));
        gamma.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.GAMMA_CMD, gamma.getValue(), true);
                gammaLabel.setText(nf1.format(gamma.getValue()));
                return true;
            }
            return false;
        });

        display.add(gammal).left().padRight(pad5 * 4).padBottom(pad5);
        display.add(gamma).left().padRight(pad5 * 2).padBottom(pad5);
        display.add(gammaLabel).row();

        // LABELS
        labels.addAll(brightnessl, contrastl, huel, saturationl, gammal);

        // Add to content
        contentGraphicsTable.add(titleDisplay).left().padBottom(pad5 * 2).row();
        contentGraphicsTable.add(display).left();
        /*
         * ==== UI ====
         */
        final Table contentUI = new Table(skin);
        contents.add(contentUI);
        contentUI.align(Align.top | Align.left);

        OwnLabel titleUI = new OwnLabel(I18n.txt("gui.ui.interfacesettings"), skin, "help-title");

        Table ui = new Table();

        // LANGUAGE
        OwnLabel langLabel = new OwnLabel(I18n.txt("gui.ui.language"), skin);
        File i18nfolder = new File(GlobalConf.ASSETS_LOC + File.separator + "i18n");
        String i18nname = "gsbundle";
        String[] files = i18nfolder.list();
        LangComboBoxBean[] langs = new LangComboBoxBean[files.length];
        int i = 0;
        for (String file : files) {
            if (file.startsWith("gsbundle") && file.endsWith(".properties")) {
                String locale = file.substring(i18nname.length(), file.length() - ".properties".length());
                // Default locale
                if (locale.isEmpty())
                    locale = "-en-GB";

                // Remove underscore _
                locale = locale.substring(1).replace("_", "-");
                Locale loc = Locale.forLanguageTag(locale);
                langs[i] = new LangComboBoxBean(loc);
            }
            i++;
        }
        Arrays.sort(langs);

        lang = new OwnSelectBox<>(skin);
        lang.setWidth(textwidth * 3f);
        lang.setItems(langs);
        if (!GlobalConf.program.LOCALE.isEmpty()) {
            lang.setSelected(langs[idxLang(GlobalConf.program.LOCALE, langs)]);
        } else {
            // Empty locale
            int lidx = idxLang(null, langs);
            if (lidx < 0 || lidx >= langs.length) {
                lidx = idxLang(Locale.getDefault().toLanguageTag(), langs);
                if (lidx < 0 || lidx >= langs.length) {
                    // Default is en_GB
                    lidx = 2;
                }
            }
            lang.setSelected(langs[lidx]);
        }

        // THEME
        OwnLabel themeLabel = new OwnLabel(I18n.txt("gui.ui.theme"), skin);
        String[] themes = new String[] { "dark-green", "dark-green-x2", "dark-blue", "dark-blue-x2", "dark-orange", "dark-orange-x2", "bright-green", "bright-green-x2", "night-red", "night-red-x2" };
        theme = new OwnSelectBox<>(skin);
        theme.setWidth(textwidth * 3f);
        theme.setItems(themes);
        theme.setSelected(GlobalConf.program.UI_THEME);

        // POINTER COORDINATES
        pointerCoords = new OwnCheckBox(I18n.txt("gui.ui.pointercoordinates"), skin, "default", pad5);
        pointerCoords.setChecked(GlobalConf.program.DISPLAY_POINTER_COORDS);

        // LABELS
        labels.addAll(langLabel, themeLabel);

        // Add to table
        ui.add(langLabel).left().padRight(pad5 * 4).padBottom(pad5);
        ui.add(lang).left().padBottom(pad5).row();
        ui.add(themeLabel).left().padRight(pad5 * 4).padBottom(pad5);
        ui.add(theme).left().padBottom(pad5).row();
        ui.add(pointerCoords).left().padRight(pad5 * 2).padBottom(pad5).row();

        // Add to content
        contentUI.add(titleUI).left().padBottom(pad5 * 2).row();
        contentUI.add(ui).left();

        /*
         * ==== PERFORMANCE ====
         */
        final Table contentPerformance = new Table(skin);
        contents.add(contentPerformance);
        contentPerformance.align(Align.top | Align.left);

        // MULTITHREADING
        OwnLabel titleMultithread = new OwnLabel(I18n.txt("gui.multithreading"), skin, "help-title");

        Table multithread = new Table(skin);

        OwnLabel numThreadsLabel = new OwnLabel(I18n.txt("gui.thread.number"), skin);
        int maxthreads = Runtime.getRuntime().availableProcessors();
        ComboBoxBean[] cbs = new ComboBoxBean[maxthreads + 1];
        cbs[0] = new ComboBoxBean(I18n.txt("gui.letdecide"), 0);
        for (i = 1; i <= maxthreads; i++) {
            cbs[i] = new ComboBoxBean(I18n.txt("gui.thread", i), i);
        }
        numThreads = new OwnSelectBox<>(skin);
        numThreads.setWidth(textwidth * 3f);
        numThreads.setItems(cbs);
        numThreads.setSelectedIndex(GlobalConf.performance.NUMBER_THREADS);

        multithreadCb = new OwnCheckBox(I18n.txt("gui.thread.enable"), skin, "default", pad5);
        multithreadCb.addListener(event -> {
            if (event instanceof ChangeEvent) {
                numThreads.setDisabled(!multithreadCb.isChecked());
                // Add notice
                return true;
            }
            return false;
        });
        multithreadCb.setChecked(GlobalConf.performance.MULTITHREADING);
        numThreads.setDisabled(!multithreadCb.isChecked());

        // Add to table
        multithread.add(multithreadCb).colspan(2).left().padBottom(pad5).row();
        multithread.add(numThreadsLabel).left().padRight(pad5 * 4).padBottom(pad5);
        multithread.add(numThreads).left().padBottom(pad5).row();
        final Cell<Actor> noticeMulithreadCell = multithread.add((Actor) null);
        noticeMulithreadCell.colspan(2).left();

        multithreadCb.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (noticeMulithreadCell.getActor() == null) {
                    String nextinfostr = I18n.txt("gui.ui.info") + '\n';
                    int lines = GlobalResources.countOccurrences(nextinfostr, '\n');
                    TextArea nextTimeInfo = new OwnTextArea(nextinfostr, skin, "info");
                    nextTimeInfo.setDisabled(true);
                    nextTimeInfo.setPrefRows(lines + 1);
                    nextTimeInfo.setWidth(tawidth);
                    nextTimeInfo.clearListeners();
                    noticeMulithreadCell.setActor(nextTimeInfo);
                }
                return true;
            }
            return false;
        });

        // Add to content
        contentPerformance.add(titleMultithread).left().padBottom(pad5 * 2).row();
        contentPerformance.add(multithread).left().padBottom(pad5 * 4).row();

        // DRAW DISTANCE
        OwnLabel titleLod = new OwnLabel(I18n.txt("gui.lod"), skin, "help-title");

        Table lod = new Table(skin);

        // Smooth transitions
        lodFadeCb = new OwnCheckBox(I18n.txt("gui.lod.fade"), skin, "default", pad5);
        lodFadeCb.setChecked(GlobalConf.scene.OCTREE_PARTICLE_FADE);

        // Draw distance
        OwnLabel ddLabel = new OwnLabel(I18n.txt("gui.lod.thresholds"), skin);
        lodTransitions = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 0.1f, false, skin);
        lodTransitions.setValue(Math.round(MathUtilsd.lint(GlobalConf.scene.OCTANT_THRESHOLD_0 * MathUtilsd.radDeg, Constants.MIN_LOD_TRANS_ANGLE_DEG, Constants.MAX_LOD_TRANS_ANGLE_DEG, Constants.MIN_SLIDER, Constants.MAX_SLIDER)));

        final OwnLabel lodValueLabel = new OwnLabel(nf3.format(GlobalConf.scene.OCTANT_THRESHOLD_0 * MathUtilsd.radDeg), skin);

        lodTransitions.addListener(event -> {
            if (event instanceof ChangeEvent) {
                OwnSlider slider = (OwnSlider) event.getListenerActor();
                lodValueLabel.setText(nf3.format(MathUtilsd.lint(slider.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_LOD_TRANS_ANGLE_DEG, Constants.MAX_LOD_TRANS_ANGLE_DEG)));
                return true;
            }
            return false;
        });

        OwnImageButton lodTooltip = new OwnImageButton(skin, "tooltip");
        lodTooltip.addListener(new TextTooltip(I18n.txt("gui.lod.thresholds.info"), skin));

        // LABELS
        labels.addAll(numThreadsLabel, ddLabel);

        // Add to table
        lod.add(lodFadeCb).colspan(4).left().padBottom(pad5).row();
        lod.add(ddLabel).left().padRight(pad5 * 4).padBottom(pad5);
        lod.add(lodTransitions).left().padRight(pad5 * 4).padBottom(pad5);
        lod.add(lodValueLabel).left().padRight(pad5 * 4).padBottom(pad5);
        lod.add(lodTooltip).left().padBottom(pad5);

        // Add to content
        contentPerformance.add(titleLod).left().padBottom(pad5 * 2).row();
        contentPerformance.add(lod).left();

        /*
         * ==== CONTROLS ====
         */
        final Table contentControls = new Table(skin);
        contents.add(contentControls);
        contentControls.align(Align.top | Align.left);

        OwnLabel titleController = new OwnLabel(I18n.txt("gui.controller"), skin, "help-title");

        // DETECTED CONTROLLER NAMES
        OwnLabel detectedLabel = new OwnLabel(I18n.txt("gui.controller.detected"), skin);
        Array<Controller> controllers = Controllers.getControllers();

        Array<OwnLabel> controllerNames = new Array<>();
        for (Controller c : controllers) {
            OwnLabel cl = new OwnLabel(c.getName(), skin);
            if (GlobalConf.controls.isControllerBlacklisted(c.getName())) {
                cl.setText(cl.getText() + " [*]");
                cl.setColor(1, 0, 0, 1);
                cl.addListener(new TextTooltip(I18n.txt("gui.tooltip.controller.blacklist"), skin));
            }
            controllerNames.add(cl);
        }
        if (controllerNames.isEmpty())
            controllerNames.add(new OwnLabel(I18n.txt("gui.controller.nocontrollers"), skin));

        // CONTROLLER MAPPINGS
        OwnLabel mappingsLabel = new OwnLabel(I18n.txt("gui.controller.mappingsfile"), skin);
        Array<FileComboBoxBean> controllerMappingsFiles = new Array<>();
        FileHandle externalfolder = new FileHandle(GlobalConf.ASSETS_LOC + File.separator + "mappings");
        FileHandle homefolder = Gdx.files.absolute(SysUtils.getDefaultMappingsDir().getPath());
        Array<FileHandle> mappingFiles = new Array<>();
        GlobalResources.listRec(externalfolder, mappingFiles, ".controller");
        GlobalResources.listRec(homefolder, mappingFiles, ".controller");
        FileComboBoxBean selected = null;
        for (FileHandle fh : mappingFiles) {
            FileComboBoxBean fcbb = new FileComboBoxBean(fh);
            controllerMappingsFiles.add(fcbb);
            if (GlobalConf.controls.CONTROLLER_MAPPINGS_FILE.endsWith(fh.name())) {
                selected = fcbb;
            }
        }

        controllerMappings = new OwnSelectBox<>(skin);
        controllerMappings.setItems(controllerMappingsFiles);
        controllerMappings.setSelected(selected);

        // INVERT Y
        inverty = new OwnCheckBox("Invert look y axis", skin, "default", pad5);
        inverty.setChecked(GlobalConf.controls.INVERT_LOOK_Y_AXIS);

        // KEY BINDINGS
        OwnLabel titleKeybindings = new OwnLabel(I18n.txt("gui.keymappings"), skin, "help-title");

        Map<TreeSet<Integer>, ProgramAction> keyboardMappings = KeyBindings.instance.getSortedMappings();
        Set<TreeSet<Integer>> keymaps = keyboardMappings.keySet();
        String[][] data = new String[keyboardMappings.size()][2];

        i = 0;
        for (TreeSet<Integer> keys : keymaps) {
            ProgramAction action = keyboardMappings.get(keys);
            data[i][0] = action.actionName;
            data[i][1] = keysToString(keys);
            i++;
        }

        Table controls = new Table(skin);
        controls.align(Align.left | Align.top);
        // Header
        controls.add(new OwnLabel(I18n.txt("gui.keymappings.action"), skin, "header")).left();
        controls.add(new OwnLabel(I18n.txt("gui.keymappings.keys"), skin, "header")).left().row();

        controls.add(new OwnLabel(I18n.txt("action.forward"), skin)).left();
        controls.add(new OwnLabel(Keys.toString(Keys.UP).toUpperCase(), skin)).left().row();
        controls.add(new OwnLabel(I18n.txt("action.backward"), skin)).left();
        controls.add(new OwnLabel(Keys.toString(Keys.DOWN).toUpperCase(), skin)).left().row();
        controls.add(new OwnLabel(I18n.txt("action.left"), skin)).left();
        controls.add(new OwnLabel(Keys.toString(Keys.LEFT).toUpperCase(), skin)).left().row();
        controls.add(new OwnLabel(I18n.txt("action.right"), skin)).left();
        controls.add(new OwnLabel(Keys.toString(Keys.RIGHT).toUpperCase(), skin)).left().row();

        // Controls
        for (String[] pair : data) {
            controls.add(new OwnLabel(pair[0], skin)).left();
            controls.add(new OwnLabel(pair[1], skin)).left().row();
        }

        OwnScrollPane controlsScroll = new OwnScrollPane(controls, skin, "minimalist-nobg");
        controlsScroll.setWidth(controlsscrollw);
        controlsScroll.setHeight(controllsscrollh);
        controlsScroll.setScrollingDisabled(true, false);
        controlsScroll.setSmoothScrolling(true);
        controlsScroll.setFadeScrollBars(false);
        scrolls.add(controlsScroll);

        // Add to content
        contentControls.add(titleController).colspan(2).left().padBottom(pad5 * 2).row();
        contentControls.add(detectedLabel).left().padBottom(pad5 * 2).padRight(pad5);
        int ci = 0;
        for (OwnLabel cn : controllerNames) {
            if (ci > 0)
                contentControls.add();
            contentControls.add(cn).left().padBottom(pad5 * 2).row();
            ci++;
        }
        contentControls.add(mappingsLabel).left().padBottom(pad5 * 2).padRight(pad5);
        contentControls.add(controllerMappings).left().padBottom(pad5 * 2).row();
        contentControls.add(inverty).left().colspan(2).padBottom(pad5 * 2).row();
        contentControls.add(titleKeybindings).colspan(2).left().padBottom(pad5 * 2).row();
        contentControls.add(controlsScroll).colspan(2).left();

        /*
         * ==== SCREENSHOTS ====
         */
        final Table contentScreenshots = new Table(skin);
        contents.add(contentScreenshots);
        contentScreenshots.align(Align.top | Align.left);

        // SCREEN CAPTURE
        OwnLabel titleScreenshots = new OwnLabel(I18n.txt("gui.screencapture"), skin, "help-title");

        Table screenshots = new Table(skin);

        // Info
        String ssInfoStr = I18n.txt("gui.screencapture.info") + '\n';
        int ssLines = GlobalResources.countOccurrences(ssInfoStr, '\n');
        TextArea screenshotsInfo = new OwnTextArea(ssInfoStr, skin, "info");
        screenshotsInfo.setDisabled(true);
        screenshotsInfo.setPrefRows(ssLines + 1);
        screenshotsInfo.setWidth(tawidth);
        screenshotsInfo.clearListeners();

        // Save location
        OwnLabel screenshotsLocationLabel = new OwnLabel(I18n.txt("gui.screenshots.save"), skin);
        screenshotsLocationLabel.pack();
        screenshotsLocation = new OwnTextButton(GlobalConf.screenshot.SCREENSHOT_FOLDER, skin);
        screenshotsLocation.pad(pad5);
        screenshotsLocation.addListener(event -> {
            if (event instanceof ChangeEvent) {
                FileChooser fc = new FileChooser(I18n.txt("gui.screenshots.directory.choose"), skin, stage, Gdx.files.absolute(GlobalConf.screenshot.SCREENSHOT_FOLDER), FileChooser.FileChooserTarget.DIRECTORIES);
                fc.setResultListener((success, result) -> {
                    if (success) {
                        // do stuff with result
                        screenshotsLocation.setText(result.path());
                    }
                    return true;
                });
                fc.show(stage);

                return true;
            }
            return false;
        });

        // Size
        final OwnLabel screenshotsSizeLabel = new OwnLabel(I18n.txt("gui.screenshots.size"), skin);
        screenshotsSizeLabel.setDisabled(GlobalConf.screenshot.isSimpleMode());
        final OwnLabel xLabel = new OwnLabel("x", skin);
        screenshotsSizeValidator = new IntValidator(GlobalConf.ScreenshotConf.MIN_SCREENSHOT_SIZE, GlobalConf.ScreenshotConf.MAX_SCREENSHOT_SIZE);
        sswidthField = new OwnTextField(Integer.toString(MathUtils.clamp(GlobalConf.screenshot.SCREENSHOT_WIDTH, GlobalConf.ScreenshotConf.MIN_SCREENSHOT_SIZE, GlobalConf.ScreenshotConf.MAX_SCREENSHOT_SIZE)), skin, screenshotsSizeValidator);
        sswidthField.setWidth(textwidth);
        sswidthField.setDisabled(GlobalConf.screenshot.isSimpleMode());
        ssheightField = new OwnTextField(Integer.toString(MathUtils.clamp(GlobalConf.screenshot.SCREENSHOT_HEIGHT, GlobalConf.ScreenshotConf.MIN_SCREENSHOT_SIZE, GlobalConf.ScreenshotConf.MAX_SCREENSHOT_SIZE)), skin, screenshotsSizeValidator);
        ssheightField.setWidth(textwidth);
        ssheightField.setDisabled(GlobalConf.screenshot.isSimpleMode());
        HorizontalGroup ssSizeGroup = new HorizontalGroup();
        ssSizeGroup.space(pad5 * 2);
        ssSizeGroup.addActor(sswidthField);
        ssSizeGroup.addActor(xLabel);
        ssSizeGroup.addActor(ssheightField);

        // Mode
        OwnLabel ssModeLabel = new OwnLabel(I18n.txt("gui.screenshots.mode"), skin);
        ComboBoxBean[] screenshotModes = new ComboBoxBean[] { new ComboBoxBean(I18n.txt("gui.screenshots.mode.simple"), 0), new ComboBoxBean(I18n.txt("gui.screenshots.mode.redraw"), 1) };
        screenshotMode = new OwnSelectBox<>(skin);
        screenshotMode.setItems(screenshotModes);
        screenshotMode.setWidth(textwidth * 3f);
        screenshotMode.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (screenshotMode.getSelected().value == 0) {
                    // Simple
                    enableComponents(false, sswidthField, ssheightField, screenshotsSizeLabel, xLabel);
                } else {
                    // Redraw
                    enableComponents(true, sswidthField, ssheightField, screenshotsSizeLabel, xLabel);
                }
                return true;
            }
            return false;
        });
        screenshotMode.setSelected(screenshotModes[GlobalConf.screenshot.SCREENSHOT_MODE.ordinal()]);
        screenshotMode.addListener(new TextTooltip(I18n.txt("gui.tooltip.screenshotmode"), skin));

        OwnImageButton screenshotsModeTooltip = new OwnImageButton(skin, "tooltip");
        screenshotsModeTooltip.addListener(new TextTooltip(I18n.txt("gui.tooltip.screenshotmode"), skin));

        HorizontalGroup ssModeGroup = new HorizontalGroup();
        ssModeGroup.space(pad5);
        ssModeGroup.addActor(screenshotMode);
        ssModeGroup.addActor(screenshotsModeTooltip);

        // LABELS
        labels.addAll(screenshotsLocationLabel, ssModeLabel, screenshotsSizeLabel);

        // Add to table
        screenshots.add(screenshotsInfo).colspan(2).left().padBottom(pad5).row();
        screenshots.add(screenshotsLocationLabel).left().padRight(pad5 * 4).padBottom(pad5);
        screenshots.add(screenshotsLocation).left().expandX().padBottom(pad5).row();
        screenshots.add(ssModeLabel).left().padRight(pad5 * 4).padBottom(pad5);
        screenshots.add(ssModeGroup).left().expandX().padBottom(pad5).row();
        screenshots.add(screenshotsSizeLabel).left().padRight(pad5 * 4).padBottom(pad5);
        screenshots.add(ssSizeGroup).left().expandX().padBottom(pad5).row();

        // Add to content
        contentScreenshots.add(titleScreenshots).left().padBottom(pad5 * 2).row();
        contentScreenshots.add(screenshots).left();

        /*
         * ==== FRAME OUTPUT ====
         */
        final Table contentFrames = new Table(skin);
        contents.add(contentFrames);
        contentFrames.align(Align.top | Align.left);

        // FRAME OUTPUT CONFIG
        OwnLabel titleFrameoutput = new OwnLabel(I18n.txt("gui.frameoutput"), skin, "help-title");

        Table frameoutput = new Table(skin);

        // Info
        String foinfostr = I18n.txt("gui.frameoutput.info") + '\n';
        ssLines = GlobalResources.countOccurrences(foinfostr, '\n');
        TextArea frameoutputInfo = new OwnTextArea(foinfostr, skin, "info");
        frameoutputInfo.setDisabled(true);
        frameoutputInfo.setPrefRows(ssLines + 1);
        frameoutputInfo.setWidth(tawidth);
        frameoutputInfo.clearListeners();

        // Save location
        OwnLabel frameoutputLocationLabel = new OwnLabel(I18n.txt("gui.frameoutput.location"), skin);
        frameoutputLocation = new OwnTextButton(GlobalConf.frame.RENDER_FOLDER, skin);
        frameoutputLocation.pad(pad5);
        frameoutputLocation.addListener(event -> {
            if (event instanceof ChangeEvent) {
                FileChooser fc = new FileChooser(I18n.txt("gui.frameoutput.directory.choose"), skin, stage, Gdx.files.absolute(GlobalConf.frame.RENDER_FOLDER), FileChooser.FileChooserTarget.DIRECTORIES);
                fc.setResultListener((success, result) -> {
                    if (success) {
                        // do stuff with result
                        frameoutputLocation.setText(result.path());
                    }
                    return true;
                });
                fc.show(stage);

                return true;
            }
            return false;
        });

        // Prefix
        OwnLabel prefixLabel = new OwnLabel(I18n.txt("gui.frameoutput.prefix"), skin);
        frameoutputPrefix = new OwnTextField(GlobalConf.frame.RENDER_FILE_NAME, skin, new RegexpValidator("^\\w+$"));
        frameoutputPrefix.setWidth(textwidth * 3f);

        // FPS
        OwnLabel fpsLabel = new OwnLabel(I18n.txt("gui.target.fps"), skin);
        frameoutputFps = new OwnTextField(Integer.toString(GlobalConf.frame.RENDER_TARGET_FPS), skin, new IntValidator(1, 200));
        frameoutputFps.setWidth(textwidth * 3f);

        // Size
        final OwnLabel frameoutputSizeLabel = new OwnLabel(I18n.txt("gui.frameoutput.size"), skin);
        frameoutputSizeLabel.setDisabled(GlobalConf.frame.isSimpleMode());
        final OwnLabel xLabelfo = new OwnLabel("x", skin);
        frameoutputSizeValidator = new IntValidator(GlobalConf.FrameConf.MIN_FRAME_SIZE, GlobalConf.FrameConf.MAX_FRAME_SIZE);
        fowidthField = new OwnTextField(Integer.toString(MathUtils.clamp(GlobalConf.frame.RENDER_WIDTH, GlobalConf.FrameConf.MIN_FRAME_SIZE, GlobalConf.FrameConf.MAX_FRAME_SIZE)), skin, frameoutputSizeValidator);
        fowidthField.setWidth(textwidth);
        fowidthField.setDisabled(GlobalConf.frame.isSimpleMode());
        foheightField = new OwnTextField(Integer.toString(MathUtils.clamp(GlobalConf.frame.RENDER_HEIGHT, GlobalConf.FrameConf.MIN_FRAME_SIZE, GlobalConf.FrameConf.MAX_FRAME_SIZE)), skin, frameoutputSizeValidator);
        foheightField.setWidth(textwidth);
        foheightField.setDisabled(GlobalConf.frame.isSimpleMode());
        HorizontalGroup foSizeGroup = new HorizontalGroup();
        foSizeGroup.space(pad5 * 2);
        foSizeGroup.addActor(fowidthField);
        foSizeGroup.addActor(xLabelfo);
        foSizeGroup.addActor(foheightField);

        // Mode
        OwnLabel fomodeLabel = new OwnLabel(I18n.txt("gui.screenshots.mode"), skin);
        ComboBoxBean[] frameoutputModes = new ComboBoxBean[] { new ComboBoxBean(I18n.txt("gui.screenshots.mode.simple"), 0), new ComboBoxBean(I18n.txt("gui.screenshots.mode.redraw"), 1) };
        frameoutputMode = new OwnSelectBox<>(skin);
        frameoutputMode.setItems(frameoutputModes);
        frameoutputMode.setWidth(textwidth * 3f);
        frameoutputMode.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (frameoutputMode.getSelected().value == 0) {
                    // Simple
                    enableComponents(false, fowidthField, foheightField, frameoutputSizeLabel, xLabelfo);
                } else {
                    // Redraw
                    enableComponents(true, fowidthField, foheightField, frameoutputSizeLabel, xLabelfo);
                }
                return true;
            }
            return false;
        });
        frameoutputMode.setSelected(frameoutputModes[GlobalConf.frame.FRAME_MODE.ordinal()]);
        frameoutputMode.addListener(new TextTooltip(I18n.txt("gui.tooltip.screenshotmode"), skin));

        OwnImageButton frameoutputModeTooltip = new OwnImageButton(skin, "tooltip");
        frameoutputModeTooltip.addListener(new TextTooltip(I18n.txt("gui.tooltip.screenshotmode"), skin));

        HorizontalGroup foModeGroup = new HorizontalGroup();
        foModeGroup.space(pad5);
        foModeGroup.addActor(frameoutputMode);
        foModeGroup.addActor(frameoutputModeTooltip);

        // LABELS
        labels.addAll(frameoutputLocationLabel, prefixLabel, fpsLabel, fomodeLabel, frameoutputSizeLabel);

        // Add to table
        frameoutput.add(frameoutputInfo).colspan(2).left().padBottom(pad5).row();
        frameoutput.add(frameoutputLocationLabel).left().padRight(pad5 * 4).padBottom(pad5);
        frameoutput.add(frameoutputLocation).left().expandX().padBottom(pad5).row();
        frameoutput.add(prefixLabel).left().padRight(pad5 * 4).padBottom(pad5);
        frameoutput.add(frameoutputPrefix).left().padBottom(pad5).row();
        frameoutput.add(fpsLabel).left().padRight(pad5 * 4).padBottom(pad5);
        frameoutput.add(frameoutputFps).left().padBottom(pad5).row();
        frameoutput.add(fomodeLabel).left().padRight(pad5 * 4).padBottom(pad5);
        frameoutput.add(foModeGroup).left().expandX().padBottom(pad5).row();
        frameoutput.add(frameoutputSizeLabel).left().padRight(pad5 * 4).padBottom(pad5);
        frameoutput.add(foSizeGroup).left().expandX().padBottom(pad5).row();

        // Add to content
        contentFrames.add(titleFrameoutput).left().padBottom(pad5 * 2).row();
        contentFrames.add(frameoutput).left();

        /*
         * ==== CAMERA ====
         */
        final Table contentCamera = new Table(skin);
        contents.add(contentCamera);
        contentCamera.align(Align.top | Align.left);

        // CAMERA RECORDING
        Table camrec = new Table(skin);

        OwnLabel titleCamrec = new OwnLabel(I18n.txt("gui.camerarec.title"), skin, "help-title");

        // fps
        OwnLabel camfpsLabel = new OwnLabel(I18n.txt("gui.target.fps"), skin);
        camrecFps = new OwnTextField(Integer.toString(GlobalConf.frame.CAMERA_REC_TARGET_FPS), skin, new IntValidator(1, 200));
        camrecFps.setWidth(textwidth * 3f);


        // Keyframe preferences
        Button keyframePrefs = new OwnTextIconButton(I18n.txt("gui.keyframes.preferences"), skin, "preferences");
        keyframePrefs.setName("keyframe preferences");
        keyframePrefs.pad(pad5);
        keyframePrefs.addListener(new TextTooltip(I18n.txt("gui.tooltip.kf.editprefs"), skin));
        keyframePrefs.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                KeyframePreferencesWindow kpw = new KeyframePreferencesWindow(stage, skin);
                kpw.setAcceptRunnable(()->{
                    if(kpw.camrecFps != null && kpw.camrecFps.isValid()) {
                        camrecFps.setText(kpw.camrecFps.getText());
                    }
                });
                kpw.show(stage);
                return true;
            }
            return false;
        });

        // Activate automatically
        cbAutoCamrec = new OwnCheckBox(I18n.txt("gui.camerarec.frameoutput"), skin, "default", pad5);
        cbAutoCamrec.setChecked(GlobalConf.frame.AUTO_FRAME_OUTPUT_CAMERA_PLAY);
        cbAutoCamrec.addListener(new TextTooltip(I18n.txt("gui.tooltip.playcamera.frameoutput"), skin));
        OwnImageButton camrecTooltip = new OwnImageButton(skin, "tooltip");
        camrecTooltip.addListener(new TextTooltip(I18n.txt("gui.tooltip.playcamera.frameoutput"), skin));

        HorizontalGroup cbGroup = new HorizontalGroup();
        cbGroup.space(pad5);
        cbGroup.addActor(cbAutoCamrec);
        cbGroup.addActor(camrecTooltip);

        // LABELS
        labels.add(camfpsLabel);

        // Add to table
        camrec.add(camfpsLabel).left().padRight(pad5 * 4).padBottom(pad5);
        camrec.add(camrecFps).left().expandX().padBottom(pad5).row();
        camrec.add(cbGroup).colspan(2).left().padBottom(pad5 * 2).row();
        camrec.add(keyframePrefs).colspan(2).left().row();

        // Add to content
        contentCamera.add(titleCamrec).left().padBottom(pad5 * 2).row();
        contentCamera.add(camrec).left();

        /*
         * ==== 360 ====
         */
        final Table content360 = new Table(skin);
        contents.add(content360);
        content360.align(Align.top | Align.left);

        // CUBEMAP
        OwnLabel titleCubemap = new OwnLabel(I18n.txt("gui.360"), skin, "help-title");
        Table cubemap = new Table(skin);

        // Info
        String cminfostr = I18n.txt("gui.360.info") + '\n';
        ssLines = GlobalResources.countOccurrences(cminfostr, '\n');
        TextArea cmInfo = new OwnTextArea(cminfostr, skin, "info");
        cmInfo.setDisabled(true);
        cmInfo.setPrefRows(ssLines + 1);
        cmInfo.setWidth(tawidth);
        cmInfo.clearListeners();

        // Resolution
        OwnLabel cmResolutionLabel = new OwnLabel(I18n.txt("gui.360.resolution"), skin);
        cmResolution = new OwnTextField(Integer.toString(GlobalConf.scene.CUBEMAP_FACE_RESOLUTION), skin, new IntValidator(20, 15000));
        cmResolution.setWidth(textwidth * 3f);

        // LABELS
        labels.add(cmResolutionLabel);

        // Add to table
        cubemap.add(cmInfo).colspan(2).left().padBottom(pad5).row();
        cubemap.add(cmResolutionLabel).left().padRight(pad5 * 4).padBottom(pad5);
        cubemap.add(cmResolution).left().expandX().padBottom(pad5).row();

        // Add to content
        content360.add(titleCubemap).left().padBottom(pad5 * 2).row();
        content360.add(cubemap).left();

        /*
         * ==== DATA ====
         */
        final Table contentDataTable = new Table(skin);
        contentDataTable.align(Align.top | Align.left);
        final OwnScrollPane contentData = new OwnScrollPane(contentDataTable, skin, "minimalist-nobg");
        contentData.setHeight(scrollh);
        contentData.setScrollingDisabled(true, false);
        contentData.setFadeScrollBars(false);
        contents.add(contentData);

        // GENERAL OPTIONS
        OwnLabel titleGeneralData = new OwnLabel(I18n.txt("gui.data.options"), skin, "help-title");
        highAccuracyPositions = new OwnCheckBox(I18n.txt("gui.data.highaccuracy"), skin, pad5);
        highAccuracyPositions.setChecked(GlobalConf.data.HIGH_ACCURACY_POSITIONS);
        highAccuracyPositions.addListener(new TextTooltip(I18n.txt("gui.tooltip.data.highaccuracy"), skin));
        OwnImageButton highAccTooltip = new OwnImageButton(skin, "tooltip");
        highAccTooltip.addListener(new TextTooltip(I18n.txt("gui.tooltip.data.highaccuracy"), skin));

        HorizontalGroup haGroup = new HorizontalGroup();
        haGroup.space(pad5);
        haGroup.addActor(highAccuracyPositions);
        haGroup.addActor(highAccTooltip);

        // DATA SOURCE
        OwnLabel titleData = new OwnLabel(I18n.txt("gui.data.source"), skin, "help-title");

        // Info
        String dsInfoStr = I18n.txt("gui.data.source.info") + '\n';
        int dsLines = GlobalResources.countOccurrences(dsInfoStr, '\n');
        TextArea dataSourceInfo = new OwnTextArea(dsInfoStr, skin, "info");
        dataSourceInfo.setDisabled(true);
        dataSourceInfo.setPrefRows(dsLines + 1);
        dataSourceInfo.setWidth(tawidth);
        dataSourceInfo.clearListeners();

        String assetsLoc = GlobalConf.ASSETS_LOC;
        dw = new DatasetsWidget(skin, assetsLoc);
        Array<FileHandle> catalogFiles = dw.buildCatalogFiles();
        Actor dataSource = dw.buildDatasetsWidget(catalogFiles, false);

        datasetChooser = new OwnCheckBox(I18n.txt("gui.data.dschooser"), skin, pad5);
        datasetChooser.setChecked(GlobalConf.program.DISPLAY_DATASET_DIALOG);

        OwnTextButton dataDownload = new OwnTextButton(I18n.txt("gui.download.title"), skin);
        dataDownload.setSize(150 * GlobalConf.SCALE_FACTOR, 25 * GlobalConf.SCALE_FACTOR);
        dataDownload.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                DownloadDataWindow ddw = new DownloadDataWindow(stage, skin, false, I18n.txt("gui.close"), null);
                ddw.setModal(true);
                ddw.show(stage);
                return true;
            }
            return false;
        });

        // Add to content
        contentDataTable.add(titleGeneralData).left().padBottom(pad5 * 2).row();
        contentDataTable.add(haGroup).left().padBottom(pad5 * 2).row();
        contentDataTable.add(titleData).left().padBottom(pad5 * 2).row();
        contentDataTable.add(dataSourceInfo).left().padBottom(pad5).row();
        contentDataTable.add(dataSource).left().padBottom(pad5 * 2).row();
        contentDataTable.add(datasetChooser).left().padBottom(pad5 * 2).row();
        contentDataTable.add(dataDownload).left();

        /*
         * ==== GAIA ====
         */
        final Table contentGaia = new Table(skin);
        contents.add(contentGaia);
        contentGaia.align(Align.top | Align.left);

        // ATTITUDE
        OwnLabel titleAttitude = new OwnLabel(I18n.txt("gui.gaia.attitude"), skin, "help-title");
        Table attitude = new Table(skin);

        real = new OwnCheckBox(I18n.txt("gui.gaia.real"), skin, "radio", pad5);
        real.setChecked(GlobalConf.data.REAL_GAIA_ATTITUDE);
        nsl = new OwnCheckBox(I18n.txt("gui.gaia.nsl"), skin, "radio", pad5);
        nsl.setChecked(!GlobalConf.data.REAL_GAIA_ATTITUDE);

        new ButtonGroup<CheckBox>(real, nsl);

        // Add to table
        attitude.add(nsl).left().padBottom(pad5).row();
        attitude.add(real).left().padBottom(pad5).row();
        final Cell<Actor> noticeAttCell = attitude.add((Actor) null);
        noticeAttCell.colspan(2).left();

        EventListener attNoticeListener = event -> {
            if (event instanceof ChangeEvent) {
                if (noticeAttCell.getActor() == null) {
                    String nextinfostr = I18n.txt("gui.ui.info") + '\n';
                    int lines1 = GlobalResources.countOccurrences(nextinfostr, '\n');
                    TextArea nextTimeInfo = new OwnTextArea(nextinfostr, skin, "info");
                    nextTimeInfo.setDisabled(true);
                    nextTimeInfo.setPrefRows(lines1 + 1);
                    nextTimeInfo.setWidth(tawidth);
                    nextTimeInfo.clearListeners();
                    noticeAttCell.setActor(nextTimeInfo);
                }
                return true;
            }
            return false;
        };
        real.addListener(attNoticeListener);
        nsl.addListener(attNoticeListener);

        // Add to content
        contentGaia.add(titleAttitude).left().padBottom(pad5 * 2).row();
        contentGaia.add(attitude).left();

        /*
         * ==== SYSTEM ====
         */
        final Table contentSystem = new Table(skin);
        contents.add(contentSystem);
        contentSystem.align(Align.top | Align.left);

        // STATS
        OwnLabel titleStats = new OwnLabel(I18n.txt("gui.system.reporting"), skin, "help-title");
        Table stats = new Table(skin);

        debugInfoBak = GlobalConf.program.SHOW_DEBUG_INFO;
        debugInfo = new OwnCheckBox(I18n.txt("gui.system.debuginfo"), skin, pad5);
        debugInfo.setChecked(GlobalConf.program.SHOW_DEBUG_INFO);
        debugInfo.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.SHOW_DEBUG_CMD, !GlobalConf.program.SHOW_DEBUG_INFO);
                return true;
            }
            return false;
        });

        // RELOAD DEFAULTS
        OwnTextButton reloadDefaults = new OwnTextButton(I18n.txt("gui.system.reloaddefaults"), skin);
        reloadDefaults.addListener(event -> {
            if (event instanceof ChangeEvent) {
                reloadDefaultPreferences();
                me.hide();
                // Prevent saving current state
                GaiaSky.instance.saveState = false;
                Gdx.app.exit();
                return true;
            }

            return false;
        });
        reloadDefaults.setWidth(180 * GlobalConf.SCALE_FACTOR);

        OwnLabel warningLabel = new OwnLabel(I18n.txt("gui.system.reloaddefaults.warn"), skin, "default-red");

        // Add to table
        stats.add(debugInfo).left().padBottom(pad5).row();
        stats.add(report).left().padBottom(pad5 * 5).row();
        stats.add(warningLabel).left().padBottom(pad5).row();
        stats.add(reloadDefaults).left();

        // Add to content
        contentSystem.add(titleStats).left().padBottom(pad5 * 2).row();
        contentSystem.add(stats).left();

        /* COMPUTE LABEL WIDTH */
        float maxLabelWidth = 0;
        for (OwnLabel l : labels) {
            l.pack();
            if (l.getWidth() > maxLabelWidth)
                maxLabelWidth = l.getWidth();
        }
        maxLabelWidth = Math.max(textwidth * 2, maxLabelWidth);
        for (OwnLabel l : labels)
            l.setWidth(maxLabelWidth);

        /* ADD ALL CONTENT */
        tabContent.addActor(contentGraphics);
        tabContent.addActor(contentUI);
        tabContent.addActor(contentPerformance);
        tabContent.addActor(contentControls);
        tabContent.addActor(contentScreenshots);
        tabContent.addActor(contentFrames);
        tabContent.addActor(contentCamera);
        tabContent.addActor(content360);
        tabContent.addActor(contentData);
        tabContent.addActor(contentGaia);
        tabContent.addActor(contentSystem);

        /* ADD TO MAIN TABLE */
        content.add(tabContent).left().padLeft(10).expand().fill();

        // Listen to changes in the tab button checked states
        // Set visibility of the tab content to match the checked state
        ChangeListener tab_listener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                contentGraphics.setVisible(tabGraphics.isChecked());
                contentUI.setVisible(tabUI.isChecked());
                contentPerformance.setVisible(tabPerformance.isChecked());
                contentControls.setVisible(tabControls.isChecked());
                contentScreenshots.setVisible(tabScreenshots.isChecked());
                contentFrames.setVisible(tabFrames.isChecked());
                contentCamera.setVisible(tabCamera.isChecked());
                content360.setVisible(tab360.isChecked());
                contentData.setVisible(tabData.isChecked());
                contentGaia.setVisible(tabGaia.isChecked());
                contentSystem.setVisible(tabSystem.isChecked());
            }
        };
        tabGraphics.addListener(tab_listener);
        tabUI.addListener(tab_listener);
        tabPerformance.addListener(tab_listener);
        tabControls.addListener(tab_listener);
        tabScreenshots.addListener(tab_listener);
        tabFrames.addListener(tab_listener);
        tabCamera.addListener(tab_listener);
        tab360.addListener(tab_listener);
        tabData.addListener(tab_listener);
        tabGaia.addListener(tab_listener);
        tabSystem.addListener(tab_listener);

        // Let only one tab button be checked at a time
        ButtonGroup<Button> tabs = new ButtonGroup<Button>();
        tabs.setMinCheckCount(1);
        tabs.setMaxCheckCount(1);
        tabs.add(tabGraphics);
        tabs.add(tabUI);
        tabs.add(tabPerformance);
        tabs.add(tabControls);
        tabs.add(tabScreenshots);
        tabs.add(tabFrames);
        tabs.add(tabCamera);
        tabs.add(tab360);
        tabs.add(tabData);
        tabs.add(tabGaia);
        tabs.add(tabSystem);

    }

    @Override
    protected void accept() {
        saveCurrentPreferences();
    }

    @Override
    protected void cancel() {
        revertLivePreferences();
    }

    private void reloadDefaultPreferences() {
        // User config file
        File userFolder = SysUtils.getConfigDir();
        File userFolderConfFile = new File(userFolder, "global.properties");

        // Internal config
        File confFolder = new File("conf" + File.separator);
        File internalFolderConfFile = new File(confFolder, "global.properties");

        // Delete current conf
        if (userFolderConfFile.exists()) {
            userFolderConfFile.delete();
        }

        // Copy file
        try {
            if (confFolder.exists() && confFolder.isDirectory()) {
                // Running released package
                copyFile(internalFolderConfFile, userFolderConfFile, true);
            } else {
                // Running from code?
                if (!new File("../android/assets/conf" + File.separator).exists()) {
                    throw new IOException("File ../android/assets/conf does not exist!");
                }
                copyFile(new File("../android/assets/conf" + File.separator + "global.properties"), userFolderConfFile, true);
            }

        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e, "Error copying default preferences file to user folder: " + userFolderConfFile.getAbsolutePath());
        }

    }

    private static void copyFile(File sourceFile, File destFile, boolean ow) throws IOException {
        if (destFile.exists()) {
            if (ow) {
                // Overwrite, delete file
                destFile.delete();
            } else {
                return;
            }
        }
        // Create new
        destFile.createNewFile();

        FileChannel source = null;
        FileChannel destination = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            source = (fis = new FileInputStream(sourceFile)).getChannel();
            destination = (fos = new FileOutputStream(destFile)).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
                fis.close();
            }
            if (destination != null) {
                destination.close();
                fos.close();
            }
        }
    }

    private void saveCurrentPreferences() {
        // Add all properties to GlobalConf.instance

        final boolean reloadFullscreenMode = fullscreen.isChecked() != GlobalConf.screen.FULLSCREEN;
        final boolean reloadScreenMode = reloadFullscreenMode || (GlobalConf.screen.FULLSCREEN && (GlobalConf.screen.FULLSCREEN_WIDTH != fullscreenResolutions.getSelected().width || GlobalConf.screen.FULLSCREEN_HEIGHT != fullscreenResolutions.getSelected().height)) || (!GlobalConf.screen.FULLSCREEN && (GlobalConf.screen.SCREEN_WIDTH != Integer.parseInt(widthField.getText())) || GlobalConf.screen.SCREEN_HEIGHT != Integer.parseInt(heightField.getText()));

        GlobalConf.screen.FULLSCREEN = fullscreen.isChecked();

        // Fullscreen options
        GlobalConf.screen.FULLSCREEN_WIDTH = fullscreenResolutions.getSelected().width;
        GlobalConf.screen.FULLSCREEN_HEIGHT = fullscreenResolutions.getSelected().height;

        // Windowed options
        GlobalConf.screen.SCREEN_WIDTH = Integer.parseInt(widthField.getText());
        GlobalConf.screen.SCREEN_HEIGHT = Integer.parseInt(heightField.getText());

        // Graphics
        ComboBoxBean bean = gquality.getSelected();
        if (GlobalConf.scene.GRAPHICS_QUALITY != bean.value) {
            GlobalConf.scene.GRAPHICS_QUALITY = bean.value;
            EventManager.instance.post(Events.GRAPHICS_QUALITY_UPDATED, bean.value);
        }

        bean = aa.getSelected();
        Antialias newaa = GlobalConf.postprocess.getAntialias(bean.value);
        if (GlobalConf.postprocess.POSTPROCESS_ANTIALIAS != newaa) {
            GlobalConf.postprocess.POSTPROCESS_ANTIALIAS = GlobalConf.postprocess.getAntialias(bean.value);
            EventManager.instance.post(Events.ANTIALIASING_CMD, GlobalConf.postprocess.POSTPROCESS_ANTIALIAS);
        }

        GlobalConf.screen.VSYNC = vsync.isChecked();
        try {
            // Windows backend crashes for some reason
            Gdx.graphics.setVSync(GlobalConf.screen.VSYNC);
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }

        if (limitfpsCb.isChecked()) {
            GlobalConf.screen.LIMIT_FPS = Integer.parseInt(limitFps.getText());
        } else {
            GlobalConf.screen.LIMIT_FPS = 0;
        }

        // Orbit renderer
        GlobalConf.scene.ORBIT_RENDERER = orbitRenderer.getSelected().value;

        // Line renderer
        boolean reloadLineRenderer = GlobalConf.scene.LINE_RENDERER != lineRenderer.getSelected().value;
        bean = lineRenderer.getSelected();
        GlobalConf.scene.LINE_RENDERER = bean.value;

        // Shadow mapping
        GlobalConf.scene.SHADOW_MAPPING = shadowsCb.isChecked();
        int newshadowres = Integer.parseInt(smResolution.getText());
        int newnshadows = nshadows.getSelected().value;
        final boolean reloadShadows = shadowsCb.isChecked() && (GlobalConf.scene.SHADOW_MAPPING_RESOLUTION != newshadowres || GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS != newnshadows);

        // Interface
        LangComboBoxBean lbean = lang.getSelected();
        boolean reloadUI = !GlobalConf.program.UI_THEME.equals(theme.getSelected()) || !lbean.locale.toLanguageTag().equals(GlobalConf.program.LOCALE);
        GlobalConf.program.LOCALE = lbean.locale.toLanguageTag();
        I18n.forceInit(new FileHandle(GlobalConf.ASSETS_LOC + File.separator + "i18n/gsbundle"));
        GlobalConf.program.UI_THEME = theme.getSelected();
        boolean previousPointerCoords = GlobalConf.program.DISPLAY_POINTER_COORDS;
        GlobalConf.program.DISPLAY_POINTER_COORDS = pointerCoords.isChecked();
        if (previousPointerCoords != GlobalConf.program.DISPLAY_POINTER_COORDS) {
            EventManager.instance.post(Events.DISPLAY_POINTER_COORDS_CMD, GlobalConf.program.DISPLAY_POINTER_COORDS);
        }
        // Update scale factor according to theme - for HiDPI screens
        GlobalConf.updateScaleFactor(GlobalConf.program.UI_THEME.endsWith("x2") ? 2f : 1f);

        // Performance
        bean = numThreads.getSelected();
        GlobalConf.performance.NUMBER_THREADS = bean.value;
        GlobalConf.performance.MULTITHREADING = multithreadCb.isChecked();

        GlobalConf.scene.OCTREE_PARTICLE_FADE = lodFadeCb.isChecked();
        GlobalConf.scene.OCTANT_THRESHOLD_0 = MathUtilsd.lint(lodTransitions.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_LOD_TRANS_ANGLE_DEG, Constants.MAX_LOD_TRANS_ANGLE_DEG) * (float) MathUtilsd.degRad;
        // Here we use a 0.4 rad between the thresholds
        GlobalConf.scene.OCTANT_THRESHOLD_1 = GlobalConf.scene.OCTREE_PARTICLE_FADE ? GlobalConf.scene.OCTANT_THRESHOLD_0 + 0.4f : GlobalConf.scene.OCTANT_THRESHOLD_0;

        // Data
        boolean hapos = GlobalConf.data.HIGH_ACCURACY_POSITIONS;
        GlobalConf.data.HIGH_ACCURACY_POSITIONS = highAccuracyPositions.isChecked();

        if (hapos != GlobalConf.data.HIGH_ACCURACY_POSITIONS) {
            // Event
            EventManager.instance.post(Events.HIGH_ACCURACY_CMD, GlobalConf.data.HIGH_ACCURACY_POSITIONS);
        }
        GlobalConf.data.CATALOG_JSON_FILES = "";
        boolean first = true;
        for (Button b : dw.cbs) {
            if (b.isChecked()) {
                // Add all selected to list
                if (!first) {
                    GlobalConf.data.CATALOG_JSON_FILES += "," + dw.candidates.get(b);
                } else {
                    GlobalConf.data.CATALOG_JSON_FILES += dw.candidates.get(b);
                    first = false;
                }
            }
        }
        GlobalConf.program.DISPLAY_DATASET_DIALOG = datasetChooser.isChecked();

        // Screenshots
        File ssfile = new File(screenshotsLocation.getText().toString());
        if (ssfile.exists() && ssfile.isDirectory())
            GlobalConf.screenshot.SCREENSHOT_FOLDER = ssfile.getAbsolutePath();
        ScreenshotMode prev = GlobalConf.screenshot.SCREENSHOT_MODE;
        GlobalConf.screenshot.SCREENSHOT_MODE = GlobalConf.ScreenshotMode.values()[screenshotMode.getSelectedIndex()];
        int ssw = Integer.parseInt(sswidthField.getText());
        int ssh = Integer.parseInt(ssheightField.getText());
        boolean ssupdate = ssw != GlobalConf.screenshot.SCREENSHOT_WIDTH || ssh != GlobalConf.screenshot.SCREENSHOT_HEIGHT || !prev.equals(GlobalConf.screenshot.SCREENSHOT_MODE);
        GlobalConf.screenshot.SCREENSHOT_WIDTH = ssw;
        GlobalConf.screenshot.SCREENSHOT_HEIGHT = ssh;
        if (ssupdate)
            EventManager.instance.post(Events.SCREENSHOT_SIZE_UDPATE, GlobalConf.screenshot.SCREENSHOT_WIDTH, GlobalConf.screenshot.SCREENSHOT_HEIGHT);

        // Frame output
        File fofile = new File(frameoutputLocation.getText().toString());
        if (fofile.exists() && fofile.isDirectory())
            GlobalConf.frame.RENDER_FOLDER = fofile.getAbsolutePath();
        String text = frameoutputPrefix.getText();
        if (text.matches("^\\w+$")) {
            GlobalConf.frame.RENDER_FILE_NAME = text;
        }
        prev = GlobalConf.frame.FRAME_MODE;
        GlobalConf.frame.FRAME_MODE = GlobalConf.ScreenshotMode.values()[frameoutputMode.getSelectedIndex()];
        int fow = Integer.parseInt(fowidthField.getText());
        int foh = Integer.parseInt(foheightField.getText());
        boolean foupdate = fow != GlobalConf.frame.RENDER_WIDTH || foh != GlobalConf.frame.RENDER_HEIGHT || !prev.equals(GlobalConf.frame.FRAME_MODE);
        GlobalConf.frame.RENDER_WIDTH = fow;
        GlobalConf.frame.RENDER_HEIGHT = foh;
        GlobalConf.frame.RENDER_TARGET_FPS = Integer.parseInt(frameoutputFps.getText());
        if (foupdate)
            EventManager.instance.post(Events.FRAME_SIZE_UDPATE, GlobalConf.frame.RENDER_WIDTH, GlobalConf.frame.RENDER_HEIGHT);

        // Camera recording
        GlobalConf.frame.CAMERA_REC_TARGET_FPS = Integer.parseInt(camrecFps.getText());
        GlobalConf.frame.AUTO_FRAME_OUTPUT_CAMERA_PLAY = cbAutoCamrec.isChecked();

        // Cube map resolution
        int newres = Integer.parseInt(cmResolution.getText());
        if (newres != GlobalConf.scene.CUBEMAP_FACE_RESOLUTION)
            EventManager.instance.post(Events.CUBEMAP_RESOLUTION_CMD, newres);

        // Controllers
        if(controllerMappings.getSelected() != null) {
            String mappingsFile = controllerMappings.getSelected().file;
            if (!mappingsFile.equals(GlobalConf.controls.CONTROLLER_MAPPINGS_FILE)) {
                GlobalConf.controls.CONTROLLER_MAPPINGS_FILE = mappingsFile;
                EventManager.instance.post(Events.RELOAD_CONTROLLER_MAPPINGS, mappingsFile);
            }
        }
        GlobalConf.controls.INVERT_LOOK_Y_AXIS = inverty.isChecked();

        // Gaia attitude
        GlobalConf.data.REAL_GAIA_ATTITUDE = real.isChecked();

        // System
        if (GlobalConf.program.SHOW_DEBUG_INFO != debugInfoBak) {
            EventManager.instance.post(Events.SHOW_DEBUG_CMD, !debugInfoBak);
        }

        // Save configuration
        ConfInit.instance.persistGlobalConf(new File(System.getProperty("properties.file")));

        EventManager.instance.post(Events.PROPERTIES_WRITTEN);

        if (reloadScreenMode) {
            Gdx.app.postRunnable(() -> {
                EventManager.instance.post(Events.SCREEN_MODE_CMD);
            });
        }

        if (reloadLineRenderer) {
            Gdx.app.postRunnable(() -> {
                EventManager.instance.post(Events.LINE_RENDERER_UPDATE);
            });
        }

        if (reloadShadows) {
            Gdx.app.postRunnable(() -> {
                GlobalConf.scene.SHADOW_MAPPING_RESOLUTION = newshadowres;
                GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS = newnshadows;

                EventManager.instance.post(Events.REBUILD_SHADOW_MAP_DATA_CMD);
            });
        }

        if (reloadUI) {
            reloadUI();
        }

    }

    /**
     * Reverts preferences which have been modified live. It needs backup values.
     */
    private void revertLivePreferences() {
        EventManager.instance.post(Events.BRIGHTNESS_CMD, brightnessBak, true);
        EventManager.instance.post(Events.CONTRAST_CMD, contrastBak, true);
        EventManager.instance.post(Events.HUE_CMD, hueBak, true);
        EventManager.instance.post(Events.SATURATION_CMD, saturationBak, true);
        EventManager.instance.post(Events.GAMMA_CMD, gammaBak, true);
        EventManager.instance.post(Events.MOTION_BLUR_CMD, motionblurBak, true);
        EventManager.instance.post(Events.LENS_FLARE_CMD, lensflareBak, true);
        EventManager.instance.post(Events.LIGHT_SCATTERING_CMD, lightglowBak, true);
        EventManager.instance.post(Events.BLOOM_CMD, bloomBak, true);
        EventManager.instance.post(Events.SHOW_DEBUG_CMD, debugInfoBak);
    }

    private void reloadUI() {
        // Reinitialise user interface
        Gdx.app.postRunnable(() -> {
            // Reinitialise GUI system
            GlobalResources.updateSkin();
            GenericDialog.updatePads();
            GaiaSky.instance.reinitialiseGUI1();
            EventManager.instance.post(Events.SPACECRAFT_LOADED, GaiaSky.instance.sg.getNode("Spacecraft"));
            GaiaSky.instance.reinitialiseGUI2();
            // Time init
            EventManager.instance.post(Events.TIME_CHANGE_INFO, GaiaSky.instance.time.getTime());
            if (GaiaSky.instance.cam.mode == CameraManager.CameraMode.Focus)
                // Refocus
                EventManager.instance.post(Events.FOCUS_CHANGE_CMD, GaiaSky.instance.cam.getFocus());
            // Update names with new language
            GaiaSky.instance.sg.getRoot().updateNamesRec();
            // UI theme reload broadcast
            EventManager.instance.post(Events.UI_THEME_RELOAD_INFO, GlobalResources.skin);
        });
    }

    private void selectFullscreen(boolean fullscreen, OwnTextField widthField, OwnTextField heightField, SelectBox<DisplayMode> fullScreenResolutions, OwnLabel widthLabel, OwnLabel heightLabel) {
        if (fullscreen) {
            GlobalConf.screen.SCREEN_WIDTH = fullScreenResolutions.getSelected().width;
            GlobalConf.screen.SCREEN_HEIGHT = fullScreenResolutions.getSelected().height;
        } else {
            GlobalConf.screen.SCREEN_WIDTH = Integer.parseInt(widthField.getText());
            GlobalConf.screen.SCREEN_HEIGHT = Integer.parseInt(heightField.getText());
        }

        enableComponents(!fullscreen, widthField, heightField, widthLabel, heightLabel);
        enableComponents(fullscreen, fullScreenResolutions);
    }

    private int idxAa(int base, Antialias x) {
        if (x.getAACode() == -1)
            return 1;
        if (x.getAACode() == -2)
            return 2;
        if (x.getAACode() == 0)
            return 0;
        return (int) (Math.log(x.getAACode()) / Math.log(base) + 1e-10) + 2;
    }

    private int idxLang(String code, LangComboBoxBean[] langs) {
        if (code == null || code.isEmpty()) {
            code = I18n.bundle.getLocale().toLanguageTag();
        }
        for (int i = 0; i < langs.length; i++) {
            if (langs[i].locale.toLanguageTag().equals(code)) {
                return i;
            }
        }
        return -1;
    }

    private String keysToString(TreeSet<Integer> keys) {
        String s = "";

        int i = 0;
        int n = keys.size();
        for (Integer key : keys) {
            s += Keys.toString(key).toUpperCase();
            if (i < n - 1) {
                s += " + ";
            }

            i++;
        }

        return s;
    }

}
