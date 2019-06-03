/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.screenshot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.IGui;
import gaia.cu9.ari.gaiaorbit.interfce.RenderGui;
import gaia.cu9.ari.gaiaorbit.render.IMainRenderer;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.PostProcessBean;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.RenderType;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ImageFormat;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotsManager implements IObserver {
    public static ScreenshotsManager system;

    public static void initialize() {
        system = new ScreenshotsManager();
    }

    /** Command to take screenshot **/
    private class ScreenshotCmd {
        public static final String FILENAME = "screenshot";
        public String folder;
        public int width, height;
        public boolean active = false;

        public ScreenshotCmd() {
            super();
        }

        public void takeScreenshot(int width, int height, String folder) {
            this.folder = folder;
            this.width = width;
            this.height = height;
            this.active = true;
        }

    }

    public IFileImageRenderer frameRenderer, screenshotRenderer;
    private ScreenshotCmd screenshot;
    private IGui renderGui;

    public ScreenshotsManager() {
        super();
        //frameRenderer = new BufferedFileImageRenderer(GlobalConf.runtime.OUTPUT_FRAME_BUFFER_SIZE);
        frameRenderer = new BasicFileImageRenderer();
        screenshotRenderer = new BasicFileImageRenderer();
        screenshot = new ScreenshotCmd();

        EventManager.instance.subscribe(this, Events.RENDER_FRAME, Events.RENDER_SCREENSHOT, Events.RENDER_FRAME_BUFFER, Events.FLUSH_FRAMES, Events.SCREENSHOT_CMD, Events.UPDATE_GUI, Events.DISPOSE);
    }

    public void renderFrame(IMainRenderer mr) {
        if (GlobalConf.frame.RENDER_OUTPUT) {
            switch (GlobalConf.frame.FRAME_MODE) {
            case simple:
                frameRenderer.saveScreenshot(GlobalConf.frame.RENDER_FOLDER, GlobalConf.frame.RENDER_FILE_NAME, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true, GlobalConf.frame.FRAME_FORMAT, GlobalConf.frame.FRAME_QUALITY);
                break;
            case redraw:
                // Do not resize post processor
                GaiaSky.instance.resizeImmediate(GlobalConf.frame.RENDER_WIDTH, GlobalConf.frame.RENDER_HEIGHT, false, true, false);
                renderToImage(mr, mr.getCameraManager(), mr.getT(), mr.getPostProcessor().getPostProcessBean(RenderType.frame), GlobalConf.frame.RENDER_WIDTH, GlobalConf.frame.RENDER_HEIGHT, GlobalConf.frame.RENDER_FOLDER, GlobalConf.frame.RENDER_FILE_NAME, frameRenderer, GlobalConf.frame.FRAME_FORMAT, GlobalConf.frame.FRAME_QUALITY);
                GaiaSky.instance.resizeImmediate(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false, true, false);
                break;
            }
        }
    }

    public void renderScreenshot(IMainRenderer mr) {
        if (screenshot.active) {
            String file = null;
            String filename = getCurrentTimeStamp() + "_" + ScreenshotCmd.FILENAME;
            switch (GlobalConf.screenshot.SCREENSHOT_MODE) {
            case simple:
                file = ImageRenderer.renderToImageGl20(screenshot.folder, filename, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), GlobalConf.screenshot.SCREENSHOT_FORMAT, GlobalConf.screenshot.SCREENSHOT_QUALITY);
                break;
            case redraw:
                // Do not resize post processor
                GaiaSky.instance.resizeImmediate(screenshot.width, screenshot.height, false, true, false);
                file = renderToImage(mr, mr.getCameraManager(), mr.getT(), mr.getPostProcessor().getPostProcessBean(RenderType.screenshot), screenshot.width, screenshot.height, screenshot.folder, filename, screenshotRenderer, GlobalConf.screenshot.SCREENSHOT_FORMAT, GlobalConf.screenshot.SCREENSHOT_QUALITY);
                GaiaSky.instance.resizeImmediate(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false, true, false);
                break;
            }
            if (file != null) {
                screenshot.active = false;
                EventManager.instance.post(Events.SCREENSHOT_INFO, file);
            }

        }
    }

    public void renderCurrentFrameBuffer(String folder, String file, int w, int h) {
        String f = ImageRenderer.renderToImageGl20(folder, file, w, h, GlobalConf.screenshot.SCREENSHOT_FORMAT, GlobalConf.screenshot.SCREENSHOT_QUALITY);
        if (f != null) {
            EventManager.instance.post(Events.SCREENSHOT_INFO, f);
        }
    }

    /**
     * Renders the current scene to an image and returns the file name where it
     * has been written to
     *
     * @param camera
     * @param width    The width of the image.
     * @param height   The height of the image.
     * @param folder   The folder to save the image to.
     * @param filename The file name prefix.
     * @param renderer the {@link IFileImageRenderer} to use.
     * @return String with the path to the screenshot image file.
     */
    public String renderToImage(IMainRenderer mr, ICamera camera, double dt, PostProcessBean ppb, int width, int height, String folder, String filename, IFileImageRenderer renderer, ImageFormat type, float quality) {
        FrameBuffer frameBuffer = mr.getFrameBuffer(width, height);
        // TODO That's a dirty trick, we should find a better way (i.e. making
        // buildEnabledEffectsList() method public)
        boolean postprocessing = ppb.pp.captureNoClear();
        ppb.pp.captureEnd();
        if (!postprocessing) {
            // If post processing is not active, we must start the buffer now.
            // Otherwise, it is used in the render method to write the results
            // of the pp.
            frameBuffer.begin();
        }

        // this is the main render function
        mr.preRenderScene();
        // sgr.render(camera, width, height, postprocessing ? m_fbo : null,
        // ppb);
        mr.renderSgr(camera, dt, width, height, frameBuffer, ppb);

        if (postprocessing) {
            // If post processing is active, we have to start now again because
            // the renderScene() has closed it.
            frameBuffer.begin();
        }
        if (GlobalConf.frame.RENDER_SCREENSHOT_TIME) {
            // Timestamp
            renderGui().resize(width, height);
            renderGui().render(width, height);
        }

        String res = renderer.saveScreenshot(folder, filename, width, height, false, type, quality);

        frameBuffer.end();
        return res;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case RENDER_FRAME:
            IMainRenderer mr = (IMainRenderer) data[0];
            renderFrame(mr);
            break;
        case RENDER_SCREENSHOT:
            mr = (IMainRenderer) data[0];
            renderScreenshot(mr);
            break;
        case RENDER_FRAME_BUFFER:
            String folder = (String) data[0];
            String file = (String) data[1];
            Integer w = (Integer) data[2];
            Integer h = (Integer) data[3];
            renderCurrentFrameBuffer(folder, file, w, h);
            break;
        case FLUSH_FRAMES:
            frameRenderer.flush();
            break;
        case SCREENSHOT_CMD:
            screenshot.takeScreenshot((int) data[0], (int) data[1], (String) data[2]);
            break;
        case UPDATE_GUI:
            renderGui().update((Double) data[0]);
            break;
        case DISPOSE:
            if (renderGui != null)
                renderGui.dispose();
            break;
        }

    }

    private IGui renderGui() {
        // Lazy initialised
        if (renderGui == null) {
            renderGui = new RenderGui();
            renderGui.initialize(null);
            renderGui.doneLoading(null);
        }
        return renderGui;
    }

    private static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }
}
