/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Disposable;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;

/**
 * An interface to be implemented by all top-level GUIs in Gaia Sky
 *
 * @author tsagrista
 */
public interface IGui extends Disposable {

    /**
     * Initializes the GUI, adding all the resources to the asset manager queue
     * for loading
     *
     * @param assetManager The asset manager to load the resources with
     */
    void initialize(AssetManager assetManager);

    /**
     * Hook that runs after the assets have been loaded. Completes the
     * initialization process
     *
     * @param assetManager The asset manager
     */
    void doneLoading(AssetManager assetManager);

    /**
     * Updates the GUI
     *
     * @param dt Time in seconds since the last frame
     */
    void update(double dt);

    /**
     * Renders this GUI
     *
     * @param rw The render width
     * @param rh The render height
     */
    void render(int rw, int rh);

    /**
     * Resizes this GUI to the given values at the end of the current loop
     *
     * @param width  The new width
     * @param height The new height
     */
    void resize(int width, int height);

    /**
     * Resizes without waiting for the current loop to finish
     *
     * @param width  The new width
     * @param height The new height
     */
    void resizeImmediate(int width, int height);

    /**
     * Removes the focus from this GUI and returns true if the focus was in the
     * GUI, false otherwise.
     *
     * @return true if the focus was in the GUI, false otherwise.
     */
    boolean cancelTouchFocus();

    /**
     * Returns the stage
     *
     * @return The stage
     */
    Stage getGuiStage();

    /**
     * Sets the scene graph to this GUI
     *
     * @param sg The scene graph
     */
    void setSceneGraph(ISceneGraph sg);

    /**
     * Sets the visibility state of the component entities
     *
     * @param entities The entities
     * @param visible  The states
     */
    void setVisibilityToggles(ComponentType[] entities, ComponentTypes visible);

    /**
     * Returns the first actor found with the specified name. Note this
     * recursively compares the name of every actor in the GUI.
     *
     * @return The actor if it exists, null otherwise.
     **/
    Actor findActor(String name);

    /**
     * Sets the horizontal offset, for VR
     * 
     * @param hoffset
     *            The horizontal offset in pixels
     */
    void setHoffset(int hoffset);

    /**
     * Returns whether this GUI must be drawn or not
     * @return Whether this is visible
     */
    boolean mustDraw();

}