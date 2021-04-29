/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.util.GlobalResources;
import gaiasky.util.format.DateFormatFactory;
import gaiasky.util.format.IDateFormat;
import gaiasky.util.scene2d.OwnLabel;

import java.time.Instant;

/**
 * Only for frame output mode, it displays the current time.
 * 
 * @author Toni Sagrista
 *
 */
public class RenderGui extends AbstractGui {
    protected Label time;
    protected Table mainTable;

    protected MessagesInterface messagesInterface;

    protected IDateFormat df;

    public RenderGui(Lwjgl3Graphics graphics, Float unitsPerPixel) {
        super(graphics, unitsPerPixel);
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        ui = new Stage(vp, sb);
        df = DateFormatFactory.getFormatter("dd/MM/yyyy HH:mm:ss");
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        skin = GlobalResources.skin;

        mainTable = new Table(skin);
        time = new OwnLabel("", skin, "ui-17");
        mainTable.add(time);
        mainTable.setFillParent(true);
        mainTable.right().bottom();
        mainTable.pad(5);

        // MESSAGES INTERFACE - LOW CENTER
        messagesInterface = new MessagesInterface(skin, lock);
        messagesInterface.setFillParent(true);
        messagesInterface.left().bottom();
        messagesInterface.pad(0, 300, 150, 0);

        // Add to GUI
        rebuildGui();

        EventManager.instance.subscribe(this, Events.TIME_CHANGE_INFO);
    }

    protected void rebuildGui() {
        if (ui != null) {
            ui.clear();
            ui.addActor(mainTable);
            ui.addActor(messagesInterface);
        }
    }

    @Override
    public void notify(final Events event, final Object... data) {
        synchronized (lock) {
            switch (event) {
            case TIME_CHANGE_INFO:
                time.setText(df.format((Instant) data[0]));
                break;
            default:
                break;
            }
        }
    }
}
