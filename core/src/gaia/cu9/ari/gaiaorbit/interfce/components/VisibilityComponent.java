/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.beans.ComboBoxBean;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.render.SceneGraphRenderer;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.TextUtils;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.scene2d.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VisibilityComponent extends GuiComponent implements IObserver {
    protected Map<String, Button> buttonMap;
    /**
     * Entities that will go in the visibility check boxes
     */
    private ComponentType[] visibilityEntities;
    private boolean[] visible;
    private CheckBox pmArrowheads;
    private Slider pmNumFactorSlider, pmLenFactorSlider;
    private SelectBox<ComboBoxBean> pmColorMode;
    private Button pmToggleButton;
    private Label pmLabel, pmNumFactor, pmLenFactor, pmNumFactorLabel, pmLenFactorLabel, pmColorModeLabel;
    private VerticalGroup pmNumFactorGroup, pmLenFactorGroup, pmColorModeGroup;
    private VerticalGroup pmGroup;
    private boolean sendEvents = true;

    public VisibilityComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Events.TOGGLE_VISIBILITY_CMD, Events.PM_LEN_FACTOR_CMD, Events.PM_NUM_FACTOR_CMD, Events.PM_COLOR_MODE_CMD, Events.PM_ARROWHEADS_CMD);
    }

    public void setVisibilityEntitites(ComponentType[] ve, boolean[] v) {
        visibilityEntities = ve;
        visible = v;
    }

    public void initialize() {
        float space4 = 4 * GlobalConf.SCALE_FACTOR;
        float space2 = 2 * GlobalConf.SCALE_FACTOR;
        float sliderWidth = 120 * GlobalConf.SCALE_FACTOR;
        int visTableCols = 5;
        final Table visibilityTable = new Table(skin);
        visibilityTable.setName("visibility table");
        visibilityTable.top().left();
        buttonMap = new HashMap<>();
        Set<Button> buttons = new HashSet<>();
        if (visibilityEntities != null) {
            for (int i = 0; i < visibilityEntities.length; i++) {
                final ComponentType ct = visibilityEntities[i];
                final String name = ct.getName();
                if (name != null) {
                    Button button;
                    if (ct.style != null) {
                        Image icon = new Image(skin.getDrawable(ct.style));
                        button = new OwnTextIconButton("", icon, skin, "toggle");
                    } else {
                        button = new OwnTextButton(name, skin, "toggle");
                    }
                    // Name is the key
                    button.setName(ct.key);
                    // Tooltip
                    button.addListener(new TextTooltip(TextUtils.capitalise(ct.getName()), skin));

                    buttonMap.put(name, button);
                    if (!ct.key.equals(name))
                        buttonMap.put(ct.key, button);

                    button.setChecked(visible[i]);
                    button.addListener(event -> {
                        if (event instanceof ChangeEvent) {
                            EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, ct.key, true, ((Button) event.getListenerActor()).isChecked());
                            return true;
                        }
                        return false;
                    });
                    visibilityTable.add(button).pad(GlobalConf.SCALE_FACTOR).left();
                    if ((i + 1) % visTableCols == 0) {
                        visibilityTable.row().padBottom(2 * GlobalConf.SCALE_FACTOR);
                    }
                    buttons.add(button);
                }
            }
        }

        /** Proper motions **/

        // LABEL
        pmLabel = new OwnLabel(I18n.txt("gui.velocityvectors"), skin, "header");

        // ARROWHEADS
        pmArrowheads = new OwnCheckBox(I18n.txt("gui.pm.arrowheads"), skin, space2);
        pmArrowheads.setName("pm arrow caps");
        pmArrowheads.setChecked(GlobalConf.scene.PM_ARROWHEADS);
        pmArrowheads.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (sendEvents)
                    EventManager.instance.post(Events.PM_ARROWHEADS_CMD, pmArrowheads.isChecked(), true);
                return true;
            }
            return false;
        });


        // NUM FACTOR
        pmNumFactorLabel = new Label(I18n.txt("gui.pmnumfactor"), skin, "default");
        pmNumFactor = new OwnLabel(Integer.toString((int) (MathUtilsd.lint(GlobalConf.scene.PM_NUM_FACTOR, Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR, Constants.MIN_SLIDER_1, Constants.MAX_SLIDER))), skin);

        pmNumFactorSlider = new OwnSlider(Constants.MIN_SLIDER_1, Constants.MAX_SLIDER, 1, false, skin);
        pmNumFactorSlider.setName("proper motion vectors number factor");
        pmNumFactorSlider.setWidth(sliderWidth);
        pmNumFactorSlider.setValue(MathUtilsd.lint(GlobalConf.scene.PM_NUM_FACTOR, Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR, Constants.MIN_SLIDER_1, Constants.MAX_SLIDER));
        pmNumFactorSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (sendEvents) {
                    EventManager.instance.post(Events.PM_NUM_FACTOR_CMD, MathUtilsd.lint(pmNumFactorSlider.getValue(), Constants.MIN_SLIDER_1, Constants.MAX_SLIDER, Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR), true);
                    pmNumFactor.setText(Integer.toString((int) pmNumFactorSlider.getValue()));
                }
                return true;
            }
            return false;
        });

        pmNumFactorGroup = new VerticalGroup();
        pmNumFactorGroup.space(space2);
        pmNumFactorGroup.align(Align.left).columnAlign(Align.left);
        HorizontalGroup pnfg = new HorizontalGroup();
        pnfg.space(space4);
        pnfg.addActor(pmNumFactorSlider);
        pnfg.addActor(pmNumFactor);
        pmNumFactorGroup.addActor(pmNumFactorLabel);
        pmNumFactorGroup.addActor(pnfg);

        // LEN FACTOR
        pmLenFactorLabel = new Label(I18n.txt("gui.pmlenfactor"), skin, "default");
        pmLenFactor = new OwnLabel(Integer.toString(Math.round(GlobalConf.scene.PM_LEN_FACTOR)), skin);

        pmLenFactorSlider = new OwnSlider(Constants.MIN_PM_LEN_FACTOR, Constants.MAX_PM_LEN_FACTOR, 0.5f, false, skin);
        pmLenFactorSlider.setName("proper motion vectors number factor");
        pmLenFactorSlider.setWidth(sliderWidth);
        pmLenFactorSlider.setValue(GlobalConf.scene.PM_LEN_FACTOR);
        pmLenFactorSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (sendEvents) {
                    EventManager.instance.post(Events.PM_LEN_FACTOR_CMD, pmLenFactorSlider.getValue(), true);
                    pmLenFactor.setText(Integer.toString(Math.round(pmLenFactorSlider.getValue())));
                }
                return true;
            }
            return false;
        });
        pmLenFactorGroup = new VerticalGroup();
        pmLenFactorGroup.space(space2);
        pmLenFactorGroup.align(Align.left).columnAlign(Align.left);
        HorizontalGroup plfg = new HorizontalGroup();
        plfg.space(space4);
        plfg.addActor(pmLenFactorSlider);
        plfg.addActor(pmLenFactor);
        pmLenFactorGroup.addActor(pmLenFactorLabel);
        pmLenFactorGroup.addActor(plfg);

        // PM COLOR MODE
        pmColorModeLabel = new Label(I18n.txt("gui.pm.colormode"), skin, "default");

        ComboBoxBean[] cms = new ComboBoxBean[] {
                new ComboBoxBean(I18n.txt("gui.pm.colormode.dir"), 0),
                new ComboBoxBean(I18n.txt("gui.pm.colormode.speed"), 1),
                new ComboBoxBean(I18n.txt("gui.pm.colormode.hasrv"), 2),
                new ComboBoxBean(I18n.txt("gui.pm.colormode.redshift"), 3),
                new ComboBoxBean(I18n.txt("gui.pm.colormode.redshift.cam"), 4),
                new ComboBoxBean(I18n.txt("gui.pm.colormode.single"), 5)
        };
        pmColorMode = new OwnSelectBox<>(skin);
        pmColorMode.setItems(cms);
        pmColorMode.setSelectedIndex(GlobalConf.scene.PM_COLOR_MODE);
        pmColorMode.setName("proper motion color mode");
        pmColorMode.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (sendEvents)
                    EventManager.instance.post(Events.PM_COLOR_MODE_CMD, pmColorMode.getSelectedIndex(), true);
                return true;
            }
            return false;
        });
        pmColorModeGroup = new VerticalGroup();
        pmColorModeGroup.space(space2);
        pmColorModeGroup.align(Align.left).columnAlign(Align.left);
        pmColorModeGroup.addActor(pmColorModeLabel);
        pmColorModeGroup.addActor(pmColorMode);

        // PM BUTTON
        pmGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left);
        pmGroup.space(space4);

        pmToggleButton = buttonMap.get(ComponentType.VelocityVectors.key);
        // Overwrite listeners
        pmToggleButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (pmGroup != null) {
                    if (pmToggleButton.isChecked()) {
                        velocityVectorsEnabled(true);
                    } else {
                        velocityVectorsEnabled(false);
                    }
                }
                return true;
            }
            return false;
        });
        velocityVectorsEnabled(SceneGraphRenderer.instance.isOn(ComponentType.VelocityVectors));

        // Set button width to max width
        visibilityTable.pack();

        visibilityTable.row().padBottom(3 * GlobalConf.SCALE_FACTOR);
        visibilityTable.add(pmGroup).padTop(4 * GlobalConf.SCALE_FACTOR).align(Align.left).colspan(visTableCols);

        component = visibilityTable;
    }


    private void velocityVectorsEnabled(boolean state){
        if(state) {
            pmGroup.addActor(pmLabel);
            pmGroup.addActor(pmNumFactorGroup);
            pmGroup.addActor(pmLenFactorGroup);
            pmGroup.addActor(pmColorModeGroup);
            pmGroup.addActor(pmArrowheads);
        }else{
            pmGroup.removeActor(pmLabel);
            pmGroup.removeActor(pmNumFactorGroup);
            pmGroup.removeActor(pmLenFactorGroup);
            pmGroup.removeActor(pmColorModeGroup);
            pmGroup.removeActor(pmArrowheads);
        }
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case TOGGLE_VISIBILITY_CMD:
            boolean interf = (Boolean) data[1];
            if (!interf) {
                String key = (String) data[0];
                Button b = buttonMap.get(key);

                if (b != null) {
                    b.setProgrammaticChangeEvents(false);
                    if (data.length == 3) {
                        b.setChecked((Boolean) data[2]);
                    } else {
                        b.setChecked(!b.isChecked());
                    }
                    b.setProgrammaticChangeEvents(true);
                }
            }
            break;
        case PM_LEN_FACTOR_CMD:
            interf = (Boolean) data[1];
            if (!interf) {
                sendEvents = false;
                float value = (Float) data[0];
                pmLenFactorSlider.setValue(value);
                pmLenFactorLabel.setText(Integer.toString(Math.round(value)));
                sendEvents = true;
            }
            break;
        case PM_NUM_FACTOR_CMD:
            interf = (Boolean) data[1];
            if (!interf) {
                sendEvents = false;
                float value = (Float) data[0];
                float val = MathUtilsd.lint(value, Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR, Constants.MIN_SLIDER_1, Constants.MAX_SLIDER);
                pmNumFactorSlider.setValue(val);
                pmNumFactor.setText(Integer.toString((int) val));
                sendEvents = true;
            }
            break;
        case PM_COLOR_MODE_CMD:
            interf = (Boolean) data[1];
            if (!interf) {
                sendEvents = false;
                pmColorMode.setSelectedIndex((Integer) data[0]);
                sendEvents = true;
            }
            break;
        case PM_ARROWHEADS_CMD:
            interf = (Boolean) data[1];
            if (!interf) {
                sendEvents = false;
                pmArrowheads.setChecked((boolean) data[0]);
                sendEvents = true;
            }

            break;
        default:
            break;
        }

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
