package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;

public class VRControllerInfoGui extends AbstractGui {

    protected Container<Table> container;
    protected Table contents;

    public VRControllerInfoGui() {
    }

    @Override
    public void initialize(AssetManager assetManager) {
        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
        skin = GlobalResources.skin;

        container = new Container<Table>();
        container.setFillParent(true);
        container.bottom().left();
        container.padLeft(200 - hoffset);
        container.padBottom(400);

        contents = new Table();
        Texture vrctrl_tex = new Texture(Gdx.files.internal("img/controller/controller-info.png"));
        vrctrl_tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Image vrctrl = new Image(vrctrl_tex);
        vrctrl.setScale(0.3f);
        contents.addActor(vrctrl);

        container.setActor(contents);
        contents.setVisible(false);

        rebuildGui();

        EventManager.instance.subscribe(this, Events.DISPLAY_VR_CONTROLLER_HINT_CMD);
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
        case DISPLAY_VR_CONTROLLER_HINT_CMD:
            contents.setVisible((Boolean) data[0]);
            break;
        default:
            break;
        }
    }
}
