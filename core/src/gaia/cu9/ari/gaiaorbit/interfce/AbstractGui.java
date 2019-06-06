/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;

/**
 * Provides general methods and attributes that all GUIs should have
 * 
 * @author tsagrista
 *
 */
public abstract class AbstractGui implements IObserver, IGui {

    /**
     * The user interface stage
     */
    protected Stage ui;
    /**
     * The skin to use
     */
    protected Skin skin;
    /**
     * The GUI interfaces, if any
     */
    protected Array<IGuiInterface> interfaces;

    /**
     * The name of this GUI
     */
    protected String name;

    /**
     * Horizontal offset, for VR
     */
    protected int hoffset;

    /** Lock for sync **/
    protected Object lock;

    public AbstractGui() {
        lock = new Object();
        name = this.getClass().getSimpleName();
    }

    @Override
    public void update(double dt) {
        ui.act((float) dt);
    }

    @Override
    public void render(int rw, int rh) {
        synchronized (lock) {
            ui.draw();
        }
    }

    @Override
    public Stage getGuiStage() {
        return ui;
    }

    public String getName() {
        return name;
    }

    @Override
    public void dispose() {
        if (interfaces != null)
            for (IGuiInterface iface : interfaces)
                iface.dispose();

        if (ui != null)
            ui.dispose();
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void resize(final int width, final int height) {
        Gdx.app.postRunnable(() -> resizeImmediate(width, height));
    }

    @Override
    public void resizeImmediate(final int width, final int height) {
        ui.getViewport().update(width, height, true);
        rebuildGui();
    }

    /**
     * Adds the already created GUI objects to the stage.
     */
    protected abstract void rebuildGui();

    @Override
    public boolean cancelTouchFocus() {
        if (ui.getKeyboardFocus() != null || ui.getScrollFocus() != null) {
            ui.setScrollFocus(null);
            ui.setKeyboardFocus(null);
            return true;
        }
        return false;
    }

    @Override
    public Actor findActor(String name) {
        return ui.getRoot().findActor(name);
    }

    @Override
    public void setVisibilityToggles(ComponentType[] entities, ComponentTypes visible) {
        // Empty by default
    }

    @Override
    public void setSceneGraph(ISceneGraph sg) {
        // Empty by default
    }

    @Override
    public void notify(Events event, Object... data) {
        // Empty by default
    }

    @Override
    public void setHoffset(int hoffset) {
        this.hoffset = hoffset;
    }

    @Override
    public boolean mustDraw() {
        return true;
    }
}
