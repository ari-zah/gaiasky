package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;

public class VRInfoGui extends AbstractGui {
    protected Container<Table> container;
    protected Table contents, infoFocus, infoFree;
    protected Cell<Table> infoCell;

    public VRInfoGui() {
        EventManager.instance.subscribe(this, Events.CAMERA_MODE_CMD);
    }

    @Override
    public void initialize(AssetManager assetManager) {
        // User interface
        float h = GlobalConf.screen.SCREEN_HEIGHT;
        float w = GlobalConf.screen.SCREEN_WIDTH;
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
        skin = GlobalResources.skin;

        container = new Container<>();
        container.setFillParent(true);
        container.bottom().right();
        container.padRight((w / 3f) - hoffset);
        container.padBottom(h / 3f);

        contents = new Table();
        contents.setFillParent(false);

        // FOUCS INFO
        contents.add(new FocusInfoInterface(skin, true)).left().padBottom(15f).row();
        infoCell = contents.add();

        infoFocus = new Table(skin);
        infoFocus.setBackground("table-bg");
        infoFocus.pad(5f);
        OwnLabel focusLabel = new OwnLabel("You are in focus mode", skin, "ui-15");
        focusLabel.setColor(1, 1, 0, 1);
        infoFocus.add(focusLabel).left().row();
        infoFocus.add(new OwnLabel("Push the joystick to get back", skin, "ui-15")).left().row();
        infoFocus.add(new OwnLabel("to free mode", skin, "ui-15")).left().row();

        infoFree = new Table(skin);
        infoFree.setBackground("table-bg");
        infoFree.pad(5f);
        OwnLabel freeLabel = new OwnLabel("You are in free mode", skin, "ui-15");
        freeLabel.setColor(1, 1, 0, 1);
        infoFree.add(freeLabel).left().row();
        infoFree.add(new OwnLabel("You can select an object by", skin, "ui-15")).left().row();
        infoFree.add(new OwnLabel("pointing at it and pressing", skin, "ui-15")).left().row();
        infoFree.add(new OwnLabel("the trigger", skin, "ui-15")).left().row();

        if (GaiaSky.instance.cam.mode.isFocus()) {
            infoCell.setActor(infoFocus);
        } else if (GaiaSky.instance.cam.mode.isFree()) {
            infoCell.setActor(infoFree);
        }

        container.setActor(contents);

        rebuildGui();
    }

    @Override
    public void doneLoading(AssetManager assetManager) {

    }

    @Override
    public void update(double dt) {
        super.update(dt);
    }

    @Override
    protected void rebuildGui() {
        if (ui != null) {
            ui.clear();
            if (container != null)
                ui.addActor(container);
        }
    }

    @Override
    public void notify(Events event, Object... data) {

        switch (event) {
            case CAMERA_MODE_CMD:
                CameraMode cm = (CameraMode) data[0];
                if (cm.isFocus()) {
                    infoCell.setActor(infoFocus);
                } else if (cm.isFree()) {
                    infoCell.setActor(infoFree);
                } else {
                    infoCell.clearActor();
                }
                break;
            default:
                break;
        }
    }

}
