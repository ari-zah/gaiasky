/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.*;
import gaiasky.desktop.GaiaSkyDesktop;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.datadesc.DatasetDesc;
import gaiasky.util.datadesc.DatasetType;
import gaiasky.util.scene2d.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static gaiasky.interafce.DownloadDataWindow.getIcon;

/**
 * Widget which lists all detected catalogs and offers a way to select them.
 */
public class DatasetsWidget {

    private final Stage ui;
    private final Skin skin;
    public OwnCheckBox[] cbs;
    public Map<Button, String> candidates;
    private OwnScrollPane scroll;
    public Array<DatasetType> types;
    public Array<DatasetDesc> datasets;

    public DatasetsWidget(Stage ui, Skin skin) {
        super();
        this.ui = ui;
        this.skin = skin;
        candidates = new HashMap<>();
    }

    public void reloadLocalCatalogs() {
        // Discover data sets, add as buttons
        Array<FileHandle> catalogLocations = new Array<>();
        catalogLocations.add(Gdx.files.absolute(GlobalConf.data.DATA_LOCATION));

        Array<FileHandle> catalogFiles = new Array<>();

        for (FileHandle catalogLocation : catalogLocations) {
            FileHandle[] cfs = catalogLocation.list(pathname -> pathname.getName().startsWith("catalog-") && pathname.getName().endsWith(".json"));
            catalogFiles.addAll(cfs);
        }

        JsonReader reader = new JsonReader();
        Map<String, DatasetType> typeMap = new HashMap<>();
        types = new Array<>();
        datasets = new Array<>();
        for (FileHandle catalogFile : catalogFiles) {
            JsonValue val = reader.parse(catalogFile);
            DatasetDesc dd = new DatasetDesc(reader, val);
            dd.path = Path.of(catalogFile.path());
            dd.catalogFile = catalogFile;

            if (dd.description == null)
                dd.description = dd.path.toString();
            if (dd.name == null)
                dd.name = dd.catalogFile.nameWithoutExtension();

            DatasetType dt;
            if (typeMap.containsKey(dd.type)) {
                dt = typeMap.get(dd.type);
            } else {
                dt = new DatasetType(dd.type);
                typeMap.put(dd.type, dt);
                types.add(dt);
            }

            dt.datasets.add(dd);
            datasets.add(dd);
        }

        Comparator<DatasetType> byType = Comparator.comparing(datasetType -> DownloadDataWindow.getTypeWeight(datasetType.typeStr));
        types.sort(byType);
    }

    public Actor buildDatasetsWidget() {
        return buildDatasetsWidget(true);
    }

    public Actor buildDatasetsWidget(boolean scrollOn) {
        return buildDatasetsWidget(scrollOn, 40);
    }

    public Actor buildDatasetsWidget(boolean scrollOn, int maxCharsDescription) {
        float pad = 4.8f;


        // Containers
        Table dsTable = new Table(skin);
        dsTable.align(Align.top);

        Actor result;

        scroll = null;
        if (scrollOn) {
            scroll = new OwnScrollPane(dsTable, skin, "minimalist-nobg");
            scroll.setFadeScrollBars(false);
            scroll.setScrollingDisabled(true, false);
            scroll.setSmoothScrolling(true);

            result = scroll;
        } else {
            result = dsTable;
        }

        cbs = new OwnCheckBox[datasets.size];
        Array<String> currentSetting = GlobalConf.data.CATALOG_JSON_FILES;

        int i = 0;
        for (DatasetType type : types) {
            String typeKey = "gui.download.type." + type.typeStr;
            try{
                I18n.txt(typeKey);
            } catch (Exception e){
               typeKey = "gui.download.type.unknown";
            }
            OwnLabel dsType = new OwnLabel(I18n.txt(typeKey), skin, "hud-header");
            dsTable.add(dsType).colspan(5).left().padTop(pad * 4f).padBottom(pad * 4f).row();

            // Sort datasets
            Comparator<DatasetDesc> byName = Comparator.comparing(datasetDesc -> datasetDesc.name.toLowerCase());
            Collections.sort(type.datasets, byName);

            for (DatasetDesc dataset : type.datasets) {
                OwnCheckBox cb = new OwnCheckBox(TextUtils.capString(dataset.name, 33), skin, "large", pad * 2f);
                boolean gsVersionTooSmall = false;
                if (dataset.minGsVersion >= 0 && dataset.minGsVersion > GaiaSkyDesktop.SOURCE_VERSION) {
                    // Can't select! minimum GS version larger than current
                    cb.setChecked(false);
                    cb.setDisabled(true);
                    cb.setColor(ColorUtils.gRedC);
                    cb.addListener(new OwnTextTooltip("Required Gaia Sky version (" + dataset.minGsVersion + ") larger than current (" + GaiaSkyDesktop.SOURCE_VERSION + ")", skin, 10));
                    cb.getStyle().disabledFontColor = ColorUtils.gRedC;
                    gsVersionTooSmall = true;
                } else {
                    cb.setChecked(contains(dataset.catalogFile.path(), currentSetting));
                    cb.addListener(new OwnTextTooltip(dataset.path.toString(), skin));
                }
                cb.bottom().left();

                dsTable.add(cb).left().padRight(pad * 6f).padBottom(pad);

                // Description
                HorizontalGroup descGroup = new HorizontalGroup();
                descGroup.space(pad * 2f);
                String shortDesc = TextUtils.capString(dataset.description != null ? dataset.description : "", maxCharsDescription);
                OwnLabel description = new OwnLabel(shortDesc, skin);
                if(gsVersionTooSmall)
                    description.setColor(ColorUtils.gRedC);
                description.addListener(new OwnTextTooltip(dataset.description, skin, 10));
                // Info
                OwnImageButton imgTooltip = new OwnImageButton(skin, "tooltip");
                imgTooltip.addListener(new OwnTextTooltip(dataset.description, skin, 10));
                descGroup.addActor(imgTooltip);
                descGroup.addActor(description);
                dsTable.add(descGroup).left().padRight(pad * 6f).padBottom(pad);

                // Link
                if (dataset.link != null) {
                    LinkButton imgLink = new LinkButton(dataset.link, skin);
                    dsTable.add(imgLink).left().padRight(pad * 6f).padBottom(pad);
                } else {
                    dsTable.add().left().padRight(pad * 6f).padBottom(pad);
                }

                // Version
                String vers = "v-0";
                if (dataset.myVersion >= 0) {
                    vers = "v-" + dataset.myVersion;
                }
                OwnLabel versionLabel = new OwnLabel(vers, skin);
                dsTable.add(versionLabel).left().padRight(pad * 6f).padBottom(pad);

                // Type
                Image typeImage = new OwnImage(skin.getDrawable(getIcon(dataset.type)));
                float scl = 0.7f;
                float iw = typeImage.getWidth();
                float ih = typeImage.getHeight();
                typeImage.setSize(iw * scl, ih * scl);
                typeImage.setScaling(Scaling.none);
                typeImage.addListener(new OwnTextTooltip(dataset.type, skin, 10));
                dsTable.add(typeImage).left().padRight(pad * 4f).padBottom(pad);

                // Size
                OwnLabel sizeLabel = new OwnLabel(dataset.size, skin);
                sizeLabel.addListener(new OwnTextTooltip(I18n.txt("gui.dschooser.size.tooltip"), skin, 10));
                dsTable.add(sizeLabel).left().padRight(pad * 6f).padBottom(pad);

                // # objects
                OwnLabel nobjsLabel = new OwnLabel(dataset.nObjectsStr, skin);
                nobjsLabel.addListener(new OwnTextTooltip(I18n.txt("gui.dschooser.nobjects.tooltip"), skin, 10));
                dsTable.add(nobjsLabel).left().padBottom(pad).row();

                datasets.add(dataset);
                candidates.put(cb, dataset.catalogFile.path());

                cbs[i++] = cb;
            }

        }

        ButtonGroup<OwnCheckBox> bg = new ButtonGroup<>();
        bg.setMinCheckCount(0);
        bg.setMaxCheckCount(datasets.size);
        bg.add(cbs);

        dsTable.pack();
        if (scroll != null) {

            scroll.setWidth(Math.min(1520f, dsTable.getWidth() + pad * 15f));
            scroll.setHeight(Math.min(ui.getHeight() * 0.7f, 1500f));
        }

        // No files
        if (datasets.size == 0) {
            dsTable.add(new OwnLabel(I18n.txt("gui.dschooser.nodatasets"), skin)).center();
        }

        float maxw = 0;
        for (Button b : cbs) {
            if (b.getWidth() > maxw)
                maxw = b.getWidth();
        }
        for (Button b : cbs)
            b.setWidth(maxw + 16f);

        return result;
    }

    private boolean contains(String name, Array<String> list) {
        for (String candidate : list)
            if (candidate != null && !candidate.isEmpty() && name.contains(candidate))
                return true;
        return false;
    }

    /**
     * Finds the dataset with the given descriptor file in the dataset descriptor list.
     *
     * @param descriptorFile The filename of the descriptor file.
     * @return The dataset descriptor or null if it was not found.
     */
    public DatasetDesc findDatasetByDescriptor(Path descriptorFile) throws IOException {
        if (datasets != null && Files.exists(descriptorFile))
            for (DatasetDesc dd : datasets) {
                if (Files.exists(dd.path) && Files.isSameFile(dd.path, descriptorFile))
                    return dd;
            }
        return null;
    }
}
