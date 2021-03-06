package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scenegraph.*;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.scene2d.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This window controls the visibility of individual objects
 */
public class IndividualVisibilityWindow extends GenericDialog implements IObserver {

    protected float space8, space4, space2;
    protected ISceneGraph sg;
    protected Cell<?> elementsCell;
    // Component type currently selected
    protected String currentComponentType = null;
    protected ComponentType currentCt = null;
    protected Map<String, CheckBox> cbMap;

    public IndividualVisibilityWindow(ISceneGraph sg, Stage stage, Skin skin) {
        super(I18n.txt("gui.visibility.individual"), skin, stage);

        this.sg = sg;
        space8 = 12.8f;
        space4 = 6.4f;
        space2 = 3.2f;

        cbMap = new HashMap<>();

        setAcceptText(I18n.txt("gui.close"));
        setModal(false);

        // Build
        buildSuper();
        // Pack
        pack();

        EventManager.instance.subscribe(this, Events.PER_OBJECT_VISIBILITY_CMD);
    }

    @Override
    protected void build() {
        content.clear();

        final String cct = currentComponentType;
        // Components
        float buttonPadHor = 6f;
        int visTableCols = 7;
        Table buttonTable = new Table(skin);
        // Always one button checked
        ButtonGroup<Button> buttonGroup = new ButtonGroup<>();
        buttonGroup.setMinCheckCount(1);
        buttonGroup.setMaxCheckCount(1);

        content.add(buttonTable).top().left().padBottom(pad10).row();
        elementsCell = content.add().top().left();

        ComponentType[] visibilityEntities = ComponentType.values();
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
                // Tooltip (with or without hotkey)
                String hk = KeyBindings.instance.getStringKeys("action.toggle/" + ct.key);
                if (hk != null) {
                    button.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(ct.getName()), hk, skin));
                } else {
                    button.addListener(new OwnTextTooltip(TextUtils.capitalise(ct.getName()), skin));
                }

                button.addListener(event -> {
                    if (event instanceof ChangeListener.ChangeEvent && button.isChecked()) {
                        // Change content only when button is checked!
                        Group elementsList = visibilitySwitcher(ct, TextUtils.capitalise(ct.getName()), ct.getName());
                        elementsCell.clearActor();
                        elementsCell.setActor(elementsList);
                        content.pack();
                        currentComponentType = name;
                        currentCt = ct;
                        return true;
                    }
                    return false;
                });

                if (name.equals(cct)) {
                    button.setChecked(true);
                }
                Cell<?> c = buttonTable.add(button);
                if ((i + 1) % visTableCols == 0) {
                    buttonTable.row();
                } else {
                    c.padRight(buttonPadHor);
                }
                buttonGroup.add(button);
            }
        }
        if (cct != null)
            buttonGroup.setChecked(cct);
        content.pack();
    }

    private Group visibilitySwitcher(ComponentType ct, String title, String id) {

        float componentWidth = 400f;
        VerticalGroup objectsGroup = new VerticalGroup();
        objectsGroup.space(space4);
        objectsGroup.left();
        objectsGroup.columnLeft();
        Array<SceneGraphNode> objects = new Array<>();
        List<OwnCheckBox> cbs = new ArrayList<>();
        sg.getRoot().getChildrenByComponentType(ct, objects);
        Array<String> names = new Array<>(false, objects.size);
        Map<String, IVisibilitySwitch> objMap = new HashMap<>();
        cbMap.clear();

        for (SceneGraphNode object : objects) {
            // Omit stars with no proper names and particle groups
            if (object.getName() != null && !GlobalResources.isNumeric(object.getName()) && !exception(ct, object)) {
                names.add(object.getName());
                objMap.put(object.getName(), object);
            }
        }
        names.sort();

        if (names.isEmpty()) {
            objectsGroup.addActor(new OwnLabel(I18n.txt("gui.elements.type.none"), skin));
        } else {
            for (String name : names) {
                HorizontalGroup objectHgroup = new HorizontalGroup();
                objectHgroup.space(space4);
                objectHgroup.left();
                OwnCheckBox cb = new OwnCheckBox(name, skin, space4);
                IVisibilitySwitch obj = objMap.get(name);
                cb.setChecked(obj.isVisible(true));
                cbMap.put(name, cb);

                cb.addListener((event) -> {
                    if (event instanceof ChangeListener.ChangeEvent && objMap.containsKey(name)) {
                        GaiaSky.postRunnable(() -> EventManager.instance.post(Events.PER_OBJECT_VISIBILITY_CMD, obj, obj.getName(), cb.isChecked(), this));
                        return true;
                    }
                    return false;
                });

                objectHgroup.addActor(cb);
                // Tooltips
                if (obj.getDescription() != null) {
                    ImageButton meshDescTooltip = new OwnImageButton(skin, "tooltip");
                    meshDescTooltip.addListener(new OwnTextTooltip((obj.getDescription() == null || obj.getDescription().isEmpty() ? "No description" : obj.getDescription()), skin));
                    objectHgroup.addActor(meshDescTooltip);
                }

                objectsGroup.addActor(objectHgroup);
                cbs.add(cb);
            }
        }

        objectsGroup.pack();
        OwnScrollPane scrollPane = new OwnScrollPane(objectsGroup, skin, "minimalist-nobg");
        scrollPane.setName(id + " scroll");

        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        scrollPane.setHeight(360f);
        scrollPane.setWidth(componentWidth);

        HorizontalGroup buttons = new HorizontalGroup();
        buttons.space(pad5);
        OwnTextIconButton selAll = new OwnTextIconButton("", skin, "audio");
        selAll.addListener(new OwnTextTooltip(I18n.txt("gui.select.all"), skin));
        selAll.pad(space2);
        selAll.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                GaiaSky.postRunnable(() -> cbs.forEach((i) -> i.setChecked(true)));
                return true;
            }
            return false;
        });
        OwnTextIconButton selNone = new OwnTextIconButton("", skin, "ban");
        selNone.addListener(new OwnTextTooltip(I18n.txt("gui.select.none"), skin));
        selNone.pad(space2);
        selNone.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                GaiaSky.postRunnable(() -> cbs.forEach((i) -> i.setChecked(false)));
                return true;
            }
            return false;
        });
        buttons.addActor(selAll);
        buttons.addActor(selNone);

        VerticalGroup group = new VerticalGroup();
        group.left();
        group.columnLeft();
        group.space(space8);

        group.addActor(new OwnLabel(TextUtils.trueCapitalise(title), skin, "header"));
        group.addActor(scrollPane);
        group.addActor(buttons);

        return group;
    }

    /**
     * Implements the exception code. Returns true if the given object should not be listed
     * under the given component type.
     *
     * @param ct     The component type
     * @param object The object
     * @return Whether this object is an exception (should not be listed) or not
     */
    private boolean exception(ComponentType ct, SceneGraphNode object) {
        return ct == ComponentType.Planets && object instanceof Orbit
                || object instanceof ParticleGroup;
    }

    @Override
    protected void accept() {

    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public void notify(Events event, Object... data) {
        if (event == Events.PER_OBJECT_VISIBILITY_CMD) {
            IVisibilitySwitch obj = (IVisibilitySwitch) data[0];
            String name = (String) data[1];
            boolean checked = (Boolean) data[2];
            Object source = data[3];
            if (source != this) {
                // Update checkbox if necessary
                if (currentCt != null && obj.hasCt(currentCt)) {
                    CheckBox cb = cbMap.get(name);
                    if (cb != null) {
                        cb.setProgrammaticChangeEvents(false);
                        cb.setChecked(checked);
                        cb.setProgrammaticChangeEvents(true);
                    }
                }
            }
        }

    }
}
