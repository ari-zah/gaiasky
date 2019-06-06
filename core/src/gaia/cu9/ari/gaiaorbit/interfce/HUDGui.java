/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;

/**
 * Head-up display GUI which only displays information and has no options
 * window.
 * 
 * @author Toni Sagrista
 *
 */
public class HUDGui implements IGui {
    private Skin skin;
    /**
     * The user interface stage
     */
    protected Stage ui;

    protected FocusInfoInterface focusInterface;
    protected NotificationsInterface notificationsInterface;
    protected MessagesInterface messagesInterface;
    protected DebugInterface debugInterface;
    protected RunStateInterface inputInterface;

    protected Array<IGuiInterface> interfaces;

    /** Lock object for synchronization **/
    private Object lock;

    @Override
    public void initialize(AssetManager assetManager) {
        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
        lock = new Object();
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        skin = GlobalResources.skin;
        interfaces = new Array<>();
        buildGui();
    }

    private void buildGui() {
        float pad = 5 * GlobalConf.SCALE_FACTOR;

        // FOCUS INFORMATION - BOTTOM RIGHT
        focusInterface = new FocusInfoInterface(skin);
        focusInterface.setFillParent(true);
        focusInterface.right().bottom();
        focusInterface.pad(0, 0, pad, pad);
        interfaces.add(focusInterface);

        // DEBUG INFO - TOP RIGHT
        debugInterface = new DebugInterface(skin, lock);
        debugInterface.setFillParent(true);
        debugInterface.right().top();
        interfaces.add(debugInterface);

        // NOTIFICATIONS INTERFACE - BOTTOM LEFT
        notificationsInterface = new NotificationsInterface(skin, lock, true);
        notificationsInterface.setFillParent(true);
        notificationsInterface.left().bottom();
        notificationsInterface.pad(0, pad, pad, 0);
        interfaces.add(notificationsInterface);

        // MESSAGES INTERFACE - LOW CENTER
        messagesInterface = new MessagesInterface(skin, lock);
        messagesInterface.setFillParent(true);
        messagesInterface.left().bottom();
        messagesInterface.pad(0, 300 * GlobalConf.SCALE_FACTOR, 150 * GlobalConf.SCALE_FACTOR, 0);
        interfaces.add(messagesInterface);

        // INPUT STATE
        inputInterface = new RunStateInterface(skin);
        inputInterface.setFillParent(true);
        inputInterface.right().top();
        inputInterface.pad(50 * GlobalConf.SCALE_FACTOR, 0, 0, pad);
        interfaces.add(inputInterface);

        // Add to GUI
        rebuildGui();
    }

    public void rebuildGui() {
        if (ui != null) {
            ui.clear();

            if (debugInterface != null) {
                ui.addActor(debugInterface);
            }
            if (notificationsInterface != null) {
                ui.addActor(notificationsInterface);
            }
            if (messagesInterface != null) {
                ui.addActor(messagesInterface);
            }
            if (focusInterface != null) {
                ui.addActor(focusInterface);
            }
            if (inputInterface != null) {
                ui.addActor(inputInterface);
            }
        }
    }

    @Override
    public void dispose() {
        for (IGuiInterface iface : interfaces)
            iface.dispose();

        ui.dispose();
    }

    @Override
    public void update(double dt) {
        ui.act((float) dt);
        notificationsInterface.update();
    }

    @Override
    public void render(int rw, int rh) {
        synchronized (lock) {
            ui.draw();
        }
    }

    @Override
    public void resize(final int width, final int height) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                resizeImmediate(width, height);
            }
        });
    }

    @Override
    public void resizeImmediate(final int width, final int height) {
        ui.getViewport().update(width, height, true);
        rebuildGui();
    }

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
    public Stage getGuiStage() {
        return ui;
    }

    @Override
    public void setSceneGraph(ISceneGraph sg) {
    }

    @Override
    public void setVisibilityToggles(ComponentType[] entities, ComponentTypes visible) {
    }

    @Override
    public Actor findActor(String name) {
        return ui.getRoot().findActor(name);
    }

    @Override
    public void setHoffset(int hoffset) {
    }

    @Override
    public boolean mustDraw() {
        return true;
    }

}
