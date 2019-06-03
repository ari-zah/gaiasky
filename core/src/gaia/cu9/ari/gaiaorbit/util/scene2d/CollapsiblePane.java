/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;

/**
 * A collapsible pane with a detach-to-window button.
 *
 * @author Toni Sagrista
 */
public class CollapsiblePane extends Table {

    CollapsibleWindow dialogWindow;
    ImageButton expandIcon, detachIcon;
    float lastx = -1, lasty = -1;
    Actor content;
    String labelText;
    Skin skin;
    Stage stage;
    float space;
    Cell<?> contentCell;

    /** Collapse speed in pixels per second **/
    protected float collapseSpeed;
    float targetHeight;
    boolean expanding = false;
    boolean collapsing = false;

    /**
     * Creates a collapsible pane.
     *
     * @param stage             The main stage.
     * @param labelText         The text of the label.
     * @param content           The content actor.
     * @param skin              The skin to use.
     * @param labelStyle        The style of the label.
     * @param expandButtonStyle The style of the expand icon.
     * @param detachButtonStyle The style of the detach icon.
     * @param shortcut          The shortcut to expand/collapse. Shown in a tooltip.
     * @param topIcons          List of top icons that will be added between the label and the
     *                          expand/detach icons.
     */
    public CollapsiblePane(final Stage stage, final String labelText, final Actor content, final Skin skin, String labelStyle, String expandButtonStyle, String detachButtonStyle, boolean expanded, String shortcut, Actor... topIcons) {
        super();
        this.stage = stage;
        this.labelText = labelText;
        this.content = content;
        this.skin = skin;
        this.space = 4 * GlobalConf.SCALE_FACTOR;
        this.collapseSpeed = 1000;

        Label mainLabel = new Label(labelText, skin, labelStyle);

        // Expand icon
        expandIcon = new OwnImageButton(skin, expandButtonStyle);
        expandIcon.setName("expand-collapse");
        expandIcon.setChecked(expanded);
        expandIcon.addListener(event -> {
            if (event instanceof ChangeEvent) {
                toggleExpandCollapse();
                return true;
            }
            return false;
        });
        expandIcon.addListener(new TextTooltip(I18n.bundle.get("gui.tooltip.expandcollapse.group") + (shortcut != null ? " (" + shortcut + ")" : ""), skin));

        // Detach icon
        detachIcon = new OwnImageButton(skin, detachButtonStyle);
        detachIcon.setName("expand-collapse");
        detachIcon.setChecked(false);
        detachIcon.addListener(event -> {
            if (event instanceof ChangeEvent) {
                detach();
                return true;
            }
            return false;
        });
        detachIcon.addListener(new TextTooltip(I18n.bundle.get("gui.tooltip.detach.group"), skin));

        // Question icon
        Label questionLabel = new OwnLabel("(?)", skin, "question");
        if (shortcut != null && !shortcut.isEmpty())
            questionLabel.addListener(new TextTooltip(shortcut, skin));

        Table headerTable = new Table();

        HorizontalGroup titleGroup = new HorizontalGroup();
        titleGroup.space(3 * GlobalConf.SCALE_FACTOR);
        titleGroup.addActor(mainLabel);
        if (shortcut != null && !shortcut.isEmpty())
            titleGroup.addActor(questionLabel);

        HorizontalGroup headerGroupLeft = new HorizontalGroup();
        headerGroupLeft.space(space).align(Align.left);

        if (topIcons != null && topIcons.length > 0) {
            for (Actor topIcon : topIcons) {
                if (topIcon != null)
                    headerGroupLeft.addActor(topIcon);
            }
        }

        HorizontalGroup headerGroupRight = new HorizontalGroup();
        headerGroupRight.space(space).align(Align.right);
        headerGroupRight.addActor(expandIcon);
        headerGroupRight.addActor(detachIcon);

        headerTable.add(titleGroup).left().padRight(4 * GlobalConf.SCALE_FACTOR);
        headerTable.add(headerGroupLeft).left().pad(4 * GlobalConf.SCALE_FACTOR);
        headerTable.add().expandX();
        headerTable.add(headerGroupRight).right();

        add(headerTable).padBottom(this.space).prefWidth(195 * GlobalConf.SCALE_FACTOR).row();
        contentCell = add().prefHeight(0).prefWidth(195 * GlobalConf.SCALE_FACTOR);

        if (expanded)
            contentCell.setActor(content);

        layout();
        targetHeight = getHeight();

    }

    public boolean expandPane() {
        if (!expandIcon.isChecked()) {
            expandIcon.setChecked(true);
            expanding = true;
            collapsing = false;
            return true;
        }
        return false;
    }

    public boolean collapsePane() {
        if (expandIcon.isChecked()) {
            expandIcon.setChecked(false);
            expanding = false;
            collapsing = true;
            return true;
        }
        return false;
    }

    public void togglePane() {
        if (!expandPane())
            collapsePane();
    }

    private void toggleExpandCollapse() {
        if (expandIcon.isChecked() && dialogWindow == null) {
            contentCell.setActor(content);
            expanding = true;
            collapsing = false;
        } else {
            contentCell.clearActor();
            expanding = false;
            collapsing = true;
        }
        EventManager.instance.post(Events.RECALCULATE_OPTIONS_SIZE);
    }

    public void act(float dt) {
        super.act(dt);

        if (expanding) {

        } else if (collapsing) {

        }
    }

    public void detach() {
        dialogWindow = createWindow(labelText, content, skin, stage, lastx, lasty);

        // Display
        if (!stage.getActors().contains(dialogWindow, true))
            stage.addActor(dialogWindow);

        expandIcon.setChecked(false);
        expandIcon.setDisabled(true);
        detachIcon.setDisabled(true);
    }

    /**
     * Creates a collapsible pane.
     *
     * @param stage     The main stage.
     * @param labelText The text of the label.
     * @param content   The content actor.
     * @param skin      The skin to use.
     * @param shortcut  The keyboard shortcut to use.
     * @param topIcons  List of top icons that will be added between the label and the
     *                  expand/detach icons.
     */
    public CollapsiblePane(Stage stage, String labelText, final Actor content, Skin skin, boolean expanded, String shortcut, Actor... topIcons) {
        this(stage, labelText, content, skin, "header", "expand-collapse", "detach", expanded, shortcut, topIcons);
    }

    private CollapsibleWindow createWindow(String labelText, final Actor content, Skin skin, Stage stage, float x, float y) {
        final CollapsibleWindow window = new CollapsibleWindow(labelText, skin);
        window.align(Align.center);

        window.add(content).row();

        /** Close button **/
        TextButton close = new OwnTextButton(I18n.bundle.get("gui.close"), skin, "default");
        close.setName("close");
        close.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof ChangeEvent) {
                    lastx = window.getX();
                    lasty = window.getY();
                    window.remove();
                    dialogWindow = null;
                    expandIcon.setDisabled(false);
                    detachIcon.setDisabled(false);
                    return true;
                }

                return false;
            }

        });
        Container<Button> closeContainer = new Container<Button>(close);
        close.setSize(70, 20);
        closeContainer.align(Align.right);

        window.add(closeContainer).pad(5, 0, 0, 0).bottom().right();
        window.getTitleTable().align(Align.left);
        window.align(Align.left);
        window.pack();

        x = x < 0 ? stage.getWidth() / 2f - window.getWidth() / 2f : x;
        y = y < 0 ? stage.getHeight() / 2f - window.getHeight() / 2f : y;
        window.setPosition(Math.round(x), Math.round(y));
        window.pack();

        return window;
    }

}
