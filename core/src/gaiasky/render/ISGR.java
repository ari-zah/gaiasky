/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.render.IPostProcessor.PostProcessBean;
import gaiasky.scenegraph.camera.ICamera;

/**
 * Interface that must be extended by all types of scene graph renderers
 *
 * @author tsagrista
 */
public interface ISGR extends Disposable {
    /**
     * Renders the scene
     *
     * @param sgr    The scene graph renderer object
     * @param camera The camera.
     * @param t      The time in seconds since the start
     * @param rw     The width of the buffer
     * @param rh     The height of the buffer
     * @param tw     The final target width, usually of the screen
     * @param th     The final target height, usually of the screen
     * @param fb     The frame buffer, if any
     * @param ppb    The post processing bean
     */
    void render(SceneGraphRenderer sgr, ICamera camera, double t, int rw, int rh, int tw, int th, FrameBuffer fb, PostProcessBean ppb);

    /**
     * Resizes the assets of this renderer to the given new size
     *
     * @param w New width
     * @param h New height
     */
    void resize(final int w, final int h);

    RenderingContext getRenderingContext();

    FrameBuffer getResultBuffer();

}
