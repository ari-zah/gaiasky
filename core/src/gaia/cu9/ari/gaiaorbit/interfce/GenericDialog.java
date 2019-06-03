/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.scene2d.CollapsibleWindow;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnScrollPane;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextButton;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

public abstract class GenericDialog extends CollapsibleWindow {
    protected static float pad;
    protected static float pad5;

    static {
        updatePads();
    }

    public static void updatePads() {
        pad = 10f * GlobalConf.SCALE_FACTOR;
        pad5 = 5f * GlobalConf.SCALE_FACTOR;
    }

    final protected Stage stage;
    final protected Skin skin;
    protected GenericDialog me;
    protected Table content;
    private String acceptText = null, cancelText = null;
    protected boolean modal = true;

    protected HorizontalGroup buttonGroup;
    protected TextButton acceptButton, cancelButton;

    private Runnable acceptRunnable, cancelRunnable;

    private Actor previousKeyboardFocus, previousScrollFocus;

    protected InputListener ignoreTouchDown = new InputListener() {
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            event.cancel();
            return false;
        }
    };

    protected Array<OwnScrollPane> scrolls;

    public GenericDialog(String title, Skin skin, Stage stage) {
        super(title, skin);
        this.skin = skin;
        this.stage = stage;
        this.me = this;
        this.content = new Table(skin);
        this.scrolls = new Array<>(5);
    }

    protected void setAcceptText(String acceptText) {
        this.acceptText = acceptText;
        if (acceptButton != null) {
            acceptButton.setText(acceptText);
            recalculateButtonSize();
        }
    }

    protected void setCancelText(String cancelText) {
        this.cancelText = cancelText;
        if (cancelButton != null) {
            cancelButton.setText(cancelText);
            recalculateButtonSize();
        }
    }

    public void setModal(boolean modal) {
        this.modal = modal;
        super.setModal(modal);
    }

    protected void recalculateButtonSize() {
        float w = 80 * GlobalConf.SCALE_FACTOR;
        for (Actor button : buttonGroup.getChildren()) {
            w = Math.max(button.getWidth() + pad * 4f, w);
        }
        for (Actor button : buttonGroup.getChildren()) {
            button.setWidth(w);
        }
    }

    public void buildSuper() {

        /** BUTTONS **/
        buttonGroup = new HorizontalGroup();
        buttonGroup.space(pad5);

        if (acceptText != null) {
            acceptButton = new OwnTextButton(acceptText, skin, "default");
            acceptButton.setName("accept");
            acceptButton.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    accept();
                    if (acceptRunnable != null)
                        acceptRunnable.run();
                    me.hide();
                    return true;
                }
                return false;

            });
            buttonGroup.addActor(acceptButton);
        }
        if (cancelText != null) {
            cancelButton = new OwnTextButton(cancelText, skin, "default");
            cancelButton.setName("cancel");
            cancelButton.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    cancel();
                    if (cancelRunnable != null)
                        cancelRunnable.run();
                    me.hide();
                    return true;
                }

                return false;
            });
            buttonGroup.addActor(cancelButton);
        }
        recalculateButtonSize();

        add(content).pad(pad).row();
        add().expandY().bottom().row();
        add(buttonGroup).pad(pad).bottom().right();
        getTitleTable().align(Align.left);

        // Align top left
        align(Align.top | Align.left);

        pack();

        // Add keys for ESC, ENTER, 'y', 'n', and TAB
        me.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ievent = (InputEvent) event;
                if (ievent.getType() == Type.keyUp) {
                    int key = ievent.getKeyCode();
                    switch (key) {
                    case Keys.N:
                    case Keys.ESCAPE:
                        // Exit
                        cancel();
                        if (cancelRunnable != null)
                            cancelRunnable.run();
                        me.hide();
                        return true;
                    case Keys.Y:
                    case Keys.ENTER:
                        // Exit
                        accept();
                        if (acceptRunnable != null)
                            acceptRunnable.run();
                        me.hide();
                        return true;
                    case Keys.TAB:
                        // Next focus

                        return true;
                    default:
                        // Nothing
                        break;
                    }
                }
            }
            return false;
        });

        /** CAPTURE SCROLL FOCUS **/
        stage.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;

                if (ie.getType() == Type.mouseMoved) {
                    for (OwnScrollPane scroll : scrolls) {
                        if (ie.getTarget().isDescendantOf(scroll)) {
                            stage.setScrollFocus(scroll);
                        }
                    }
                    return true;
                }
            }
            return false;
        });

        // Build actual content
        build();

        // Set position
        setPosition(Math.round(stage.getWidth() / 2f - this.getWidth() / 2f), Math.round(stage.getHeight() / 2f - this.getHeight() / 2f));

        // Modal
        setModal(this.modal);
    }

    /**
     * Build the content here
     */
    protected abstract void build();

    /**
     * The accept function, if any
     */
    protected abstract void accept();

    /**
     * The cancel function, if any
     */
    protected abstract void cancel();

    /**
     * {@link #pack() Packs} the dialog and adds it to the stage with custom
     * action which can be null for instant show
     */
    public GenericDialog show(Stage stage, Action action) {
        clearActions();
        removeCaptureListener(ignoreTouchDown);

        previousKeyboardFocus = null;
        Actor actor = stage.getKeyboardFocus();
        if (actor != null && !actor.isDescendantOf(this))
            previousKeyboardFocus = actor;

        previousScrollFocus = null;
        actor = stage.getScrollFocus();
        if (actor != null && !actor.isDescendantOf(this))
            previousScrollFocus = actor;

        pack();
        stage.addActor(this);
        stage.setKeyboardFocus(this);
        stage.setScrollFocus(this);
        if (action != null)
            addAction(action);

        if (this.modal)
            // Disable input
            EventManager.instance.post(Events.INPUT_ENABLED_CMD, false);

        return this;
    }

    /**
     * {@link #pack() Packs} the dialog and adds it to the stage, centered with
     * default fadeIn action
     */
    public GenericDialog show(Stage stage) {
        show(stage, sequence(Actions.alpha(0), Actions.fadeIn(0.4f, Interpolation.fade)));
        setPosition(Math.round((stage.getWidth() - getWidth()) / 2f), Math.round((stage.getHeight() - getHeight()) / 2f));
        setKeyboardFocus();
        return this;
    }

    /**
     * {@link #pack() Packs} the dialog and adds it to the stage at the specified position
     */
    public GenericDialog show(Stage stage, float x, float y) {
        show(stage, sequence(Actions.alpha(0), Actions.fadeIn(0.4f, Interpolation.fade)));
        setPosition(x, y);
        setKeyboardFocus();
        return this;
    }

    /**
     * Sets the keyboard focus, override in case you want to set the focus to a
     * specific item
     **/
    public void setKeyboardFocus() {

    }

    /**
     * Hides the dialog with the given action and then removes it from the
     * stage.
     */
    public void hide(Action action) {
        Stage stage = getStage();
        if (stage != null) {
            if (previousKeyboardFocus != null && previousKeyboardFocus.getStage() == null)
                previousKeyboardFocus = null;
            Actor actor = stage.getKeyboardFocus();
            if (actor == null || actor.isDescendantOf(this))
                stage.setKeyboardFocus(previousKeyboardFocus);

            if (previousScrollFocus != null && previousScrollFocus.getStage() == null)
                previousScrollFocus = null;
            actor = stage.getScrollFocus();
            if (actor == null || actor.isDescendantOf(this))
                stage.setScrollFocus(previousScrollFocus);
        }
        if (action != null) {
            addCaptureListener(ignoreTouchDown);
            addAction(sequence(action, Actions.removeListener(ignoreTouchDown, true), Actions.removeActor()));
        } else
            remove();

        if (this.modal)
            // Enable input
            EventManager.instance.post(Events.INPUT_ENABLED_CMD, true);
    }

    /**
     * Sets the runnable which runs when accept is clicked
     *
     * @param r The runnable
     */
    public void setAcceptRunnable(Runnable r) {
        this.acceptRunnable = r;
    }

    public boolean hasAcceptRunnable() {
        return acceptRunnable != null;
    }

    /**
     * Sets the runnable which runs when cancel is clicked
     *
     * @param r The runnable
     */
    public void setCancelRunnable(Runnable r) {
        this.cancelRunnable = r;
    }

    public boolean hasCancelRunnable() {
        return cancelRunnable != null;
    }

    /**
     * Hides the dialog. Called automatically when a button is clicked. The
     * default implementation fades out the dialog over 400 milliseconds and
     * then removes it from the stage.
     */
    public void hide() {
        hide(Actions.fadeOut(0.4f, Interpolation.fade));
    }

    /**
     * Sets the enabled property on the given components
     *
     * @param enabled
     * @param components
     */
    protected void enableComponents(boolean enabled, Disableable... components) {
        for (Disableable c : components) {
            if (c != null)
                c.setDisabled(!enabled);
        }
    }

}
