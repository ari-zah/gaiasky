/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.TextureWidget;

public class MinimapWindow extends GenericDialog {
    private FrameBuffer tfb, sfb;
    private TextureWidget topProjection, sideProjection;
    int side, side2;
    int sideshort, sideshort2;

    private Array<IMinimapScale> scales;

    public MinimapWindow(Stage stage, Skin skin) {
        super(I18n.txt("gui.minimap.title"), skin, stage);
        side = (int) (GlobalConf.SCALE_FACTOR * 225);
        side2 = side / 2;
        sideshort = (int) (GlobalConf.SCALE_FACTOR * 112.5);
        sideshort2 = sideshort / 2;

        setModal(false);

        OrthographicCamera ortho = new OrthographicCamera();

        ShapeRenderer sr = new ShapeRenderer();
        sr.setAutoShapeType(true);

        SpriteBatch sb = new SpriteBatch(1000, GlobalResources.spriteShader);

        BitmapFont font = skin.getFont(GlobalConf.SCALE_FACTOR != 1 ? "ui-20" : "ui-11");

        tfb = new FrameBuffer(Format.RGBA8888, side, side, true);
        sfb = new FrameBuffer(Format.RGBA8888, side, sideshort, true);

        topProjection = new TextureWidget(tfb);
        sideProjection = new TextureWidget(sfb);

        setCancelText(I18n.txt("gui.close"));

        // Init scales
        scales = new Array<>();

        MilkyWayMinimapScale mmms = new MilkyWayMinimapScale();
        mmms.initialize(ortho, sb, sr, font, side, sideshort);

        scales.add(mmms);

        // Build
        buildSuper();

        // Pack
        pack();

    }

    @Override
    protected void build() {
        float pb = 10 * GlobalConf.SCALE_FACTOR;
        OwnLabel headerSide = new OwnLabel(I18n.txt("gui.minimap.side"), skin, "header");
        Container<TextureWidget> mapSide = new Container<TextureWidget>();
        mapSide.setActor(sideProjection);
        OwnLabel headerTop = new OwnLabel(I18n.txt("gui.minimap.top"), skin, "header");
        Container<TextureWidget> mapTop = new Container<TextureWidget>();
        mapTop.setActor(topProjection);

        content.add(headerSide).left().padBottom(pb).row();
        content.add(sideProjection).left().padBottom(pb).row();

        content.add(headerTop).left().padBottom(pb).row();
        content.add(topProjection).left();

    }

    @Override
    protected void accept() {
    }

    @Override
    protected void cancel() {
    }

    public void act(float delta) {
        super.act(delta);
        for (IMinimapScale mms : scales) {
            if (mms.isActive(GaiaSky.instance.cam.getPos())) {
                mms.renderSideProjection(sfb);
                mms.renderTopProjection(tfb);
                break;
            }
        }
    }


}
