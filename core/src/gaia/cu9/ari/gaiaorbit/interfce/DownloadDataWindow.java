/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaia.cu9.ari.gaiaorbit.desktop.GaiaSkyDesktop;
import gaia.cu9.ari.gaiaorbit.desktop.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.io.FileInfoInputStream;
import gaia.cu9.ari.gaiaorbit.util.scene2d.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

/**
 * Download manager. It gets a descriptor file from the server containing all
 * available datasets, detects them in the current system and offers and manages
 * their downloads.
 *
 * @author tsagrista
 */
public class DownloadDataWindow extends GenericDialog {
    private static final Log logger = Logger.getLogger(DownloadDataWindow.class);

    private static final Map<String, String> iconMap;

    static {
        iconMap = new HashMap<String, String>();
        iconMap.put("other", "icon-elem-others");
        iconMap.put("data-pack", "icon-elem-others");
        iconMap.put("catalog-lod", "icon-elem-stars");
        iconMap.put("catalog-gaia", "icon-elem-stars");
        iconMap.put("catalog", "icon-elem-stars");
        iconMap.put("mesh", "icon-elem-meshes");
        iconMap.put("texture-pack", "icon-elem-moons");
    }

    private static String getIcon(String type) {
        if (type != null && iconMap.containsKey(type))
            return iconMap.get(type);
        return "icon-elements-other";
    }

    private OwnTextButton downloadButton;
    private OwnProgressBar downloadProgress;
    private OwnLabel currentDownloadFile, downloadSpeed;
    private OwnScrollPane datasetsScroll;
    private float scrollX = 0, scrollY = 0;

    // Whether to show the data location chooser
    private boolean dataLocation;

    private INumberFormat nf;
    private JsonReader reader;
    private List<Trio<JsonValue, OwnCheckBox, OwnLabel>> choiceList;
    private Array<Trio<JsonValue, OwnCheckBox, OwnLabel>> toDownload;
    private Array<OwnImageButton> rubbishes;
    private int current = -1;

    public DownloadDataWindow(Stage stage, Skin skin) {
        this(stage, skin, true, I18n.txt("gui.start"), I18n.txt("gui.exit"));
    }

    public DownloadDataWindow(Stage stage, Skin skin, boolean dataLocation, String acceptText, String cancelText) {
        super(I18n.txt("gui.download.title"), skin, stage);
        this.nf = NumberFormatFactory.getFormatter("##0.0");
        this.reader = new JsonReader();
        this.choiceList = new LinkedList<>();
        this.rubbishes = new Array<>();

        this.dataLocation = dataLocation;

        setCancelText(cancelText);
        setAcceptText(acceptText);

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        me.acceptButton.setDisabled(false);
        float pad = 2f * GlobalConf.SCALE_FACTOR;
        float padl = 9f * GlobalConf.SCALE_FACTOR;
        float minw = GlobalConf.SCALE_FACTOR == 1 ? 550f : 650f;

        float buttonpad = 1f * GlobalConf.SCALE_FACTOR;

        Cell<Actor> topCell = content.add((Actor) null);
        topCell.row();

        // Offer downloads
        Table downloadTable = new Table(skin);

        OwnLabel catalogsLocLabel = new OwnLabel(I18n.txt("gui.download.location") + ":", skin);

        HorizontalGroup hg = new HorizontalGroup();
        hg.space(15f * GlobalConf.SCALE_FACTOR);
        Image system = new Image(skin.getDrawable("tooltip-icon"));
        OwnLabel downloadInfo = new OwnLabel(I18n.txt("gui.download.info"), skin);
        hg.addActor(system);
        hg.addActor(downloadInfo);

        downloadTable.add(hg).left().colspan(2).padBottom(padl).row();

        SysUtils.getLocalDataDir().mkdirs();
        String catLoc = GlobalConf.data.DATA_LOCATION;

        if (dataLocation) {
            OwnTextButton catalogsLoc = new OwnTextButton(catLoc, skin);
            catalogsLoc.pad(buttonpad * 4f);
            catalogsLoc.setMinWidth(minw);
            downloadTable.add(catalogsLocLabel).left().padBottom(padl);
            downloadTable.add(catalogsLoc).left().padLeft(pad).padBottom(padl).row();
            Cell<Actor> notice = downloadTable.add((Actor) null).colspan(2).padBottom(padl);
            notice.row();

            catalogsLoc.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    FileChooser fc = new FileChooser(I18n.txt("gui.download.pickloc"), skin, stage, Gdx.files.absolute(GlobalConf.data.DATA_LOCATION), FileChooser.FileChooserTarget.DIRECTORIES);
                    fc.setResultListener((success, result) -> {
                        if (success) {
                            if (result.file().canRead() && result.file().canWrite()) {
                                // do stuff with result
                                catalogsLoc.setText(result.path());
                                GlobalConf.data.DATA_LOCATION = result.path();
                                me.pack();
                                Gdx.app.postRunnable(() -> {
                                    me.content.clear();
                                    me.build();
                                    // Reset datasets
                                    GlobalConf.data.CATALOG_JSON_FILES = "";
                                });
                            } else {
                                Label warn = new OwnLabel(I18n.txt("gui.download.pickloc.permissions"), skin);
                                warn.setColor(1f, .4f, .4f, 1f);
                                notice.setActor(warn);
                                return false;
                            }
                        }
                        notice.clearActor();
                        return true;
                    });
                    fc.show(stage);

                    return true;
                }
                return false;
            });
        }

        // Parse available files
        JsonValue dataDesc = reader.parse(Gdx.files.absolute(SysUtils.getDefaultTmpDir() + "/gaiasky-data.json"));

        Map<String, JsonValue> bestDs = new HashMap<>();
        Map<String, List<JsonValue>> typeMap = new HashMap<>();
        // We don't want repeated elements but want to keep insertion order
        Set<String> types = new LinkedHashSet<>();

        JsonValue dst = dataDesc.child().child();
        while (dst != null) {
            boolean hasMinGsVersion = dst.has("mingsversion");
            int minGsVersion = dst.getInt("mingsversion", 0);
            int thisVersion = dst.getInt("version", 0);
            if (!hasMinGsVersion || minGsVersion <= GaiaSkyDesktop.SOURCE_CONF_VERSION) {
                // Dataset type
                String type = dst.getString("type");

                // Check if better option already exists
                String dsName = dst.getString("name");
                if (bestDs.containsKey(dsName)) {
                    JsonValue other = bestDs.get(dsName);
                    int otherVersion = other.getInt("version", 0);
                    if (otherVersion >= thisVersion) {
                        // Ignore this version
                        dst = dst.next();
                        continue;
                    } else {
                        // Remove other version, use this
                        typeMap.get(type).remove(other);
                        bestDs.remove(dsName);
                    }
                }

                // Add to map
                if (typeMap.containsKey(type)) {
                    typeMap.get(type).add(dst);
                } else {
                    List<JsonValue> aux = new ArrayList<>();
                    aux.add(dst);
                    typeMap.put(type, aux);
                }

                // Add to set
                types.add(type);
                // Add to bestDs
                bestDs.put(dsName, dst);
            }
            // Next
            dst = dst.next();
        }

        Table datasetsTable = new Table(skin);

        for (String typeStr : types) {
            List<JsonValue> datasets = typeMap.get(typeStr);

            datasetsTable.add(new OwnLabel(I18n.txt("gui.download.type." + typeStr), skin, "hud-header")).colspan(6).left().padBottom(pad * 3f).padTop(padl * 2f).row();

            for (JsonValue dataset : datasets) {
                // Check if dataset requires a minimum version of Gaia Sky

                // Check if we have it
                final Path check = Paths.get(GlobalConf.data.DATA_LOCATION, dataset.getString("check"));
                boolean exists = Files.exists(check) && Files.isReadable(check);
                int myVersion = checkJsonVersion(check);
                int serverVersion = dataset.getInt("version", 0);
                boolean outdated = serverVersion > myVersion;

                String name = dataset.getString("name");
                // Add dataset to desc table
                OwnCheckBox cb = new OwnCheckBox(name, skin, "title", pad * 2f);
                boolean baseData = name.equals("default-data");
                cb.setChecked((!exists || outdated) && baseData);
                cb.setDisabled(baseData || (exists && !outdated));
                OwnLabel haveit = new OwnLabel("", skin);
                if (exists) {
                    if (outdated) {
                        setStatusOutdated(haveit);
                    } else {
                        setStatusFound(haveit);
                    }
                } else {
                    setStatusNotFound(haveit);
                }

                // Can't proceed without base data - force download
                if (baseData && !exists) {
                    me.acceptButton.setDisabled(true);
                }

                // Description
                String description = dataset.getString("description");
                String shortDescription;
                HorizontalGroup descGroup = new HorizontalGroup();
                descGroup.space(padl);
                if (description.contains("-")) {
                    shortDescription = description.substring(0, description.indexOf("-"));
                } else {
                    shortDescription = description;
                }
                OwnLabel desc = new OwnLabel(shortDescription, skin);
                // Info
                OwnImageButton imgTooltip = new OwnImageButton(skin, "tooltip");
                imgTooltip.addListener(new OwnTextTooltip(description, skin, 10));
                descGroup.addActor(imgTooltip);
                descGroup.addActor(desc);
                // Link
                if (dataset.has("link")) {
                    String link = dataset.getString("link");
                    if (!link.isEmpty()) {
                        LinkButton imgLink = new LinkButton(link, skin);
                        descGroup.addActor(imgLink);
                    }
                }

                // Version
                OwnLabel vers = new OwnLabel(exists && outdated ? myVersion + " -> v-" + serverVersion : "v-" + myVersion, skin);
                if (!exists) {
                    vers.addListener(new OwnTextTooltip(I18n.txt("gui.download.version.server", Integer.toString(serverVersion)), skin, 10));
                } else if (outdated) {
                    // New version!
                    vers.setColor(1, 1, 0, 1);
                    vers.addListener(new OwnTextTooltip(I18n.txt("gui.download.version.new", Integer.toString(serverVersion), Integer.toString(myVersion)), skin, 10));
                } else {
                    vers.addListener(new OwnTextTooltip(I18n.txt("gui.download.version.ok"), skin, 10));
                }

                // Type icon
                Image typeImage = new OwnImage(skin.getDrawable(getIcon(dataset.getString("type"))));
                float scl = 0.7f;
                float iw = typeImage.getWidth();
                float ih = typeImage.getHeight();
                typeImage.setSize(iw * scl, ih * scl);
                typeImage.addListener(new OwnTextTooltip(dataset.getString("type"), skin, 10));

                // Size
                String size;
                try {
                    long bytes = dataset.getLong("size");
                    size = GlobalResources.humanReadableByteCount(bytes, true);
                } catch (IllegalArgumentException e) {
                    size = "?";
                }

                // Delete
                final JsonValue ds = dataset;
                OwnImageButton rubbish = null;
                if (exists) {
                    rubbish = new OwnImageButton(skin, "rubbish-bin");
                    rubbish.addListener(new TextTooltip(I18n.txt("gui.tooltip.dataset.remove"), skin));
                    rubbish.addListener((event) -> {
                        if (event instanceof ChangeEvent) {
                            // Remove dataset
                            if (ds.has("data")) {
                                JsonValue data = ds.get("data");
                                String[] filesToDelete = data.asStringArray();
                                for (String fileToDelete : filesToDelete) {
                                    try {
                                        if (fileToDelete.endsWith("/")) {
                                            fileToDelete = fileToDelete.substring(0, fileToDelete.length() - 1);
                                        }
                                        // Expand possible wildcards
                                        String basePath = "";
                                        String baseName = fileToDelete;
                                        if (fileToDelete.contains("/")) {
                                            basePath = fileToDelete.substring(0, fileToDelete.lastIndexOf('/'));
                                            baseName = fileToDelete.substring(fileToDelete.lastIndexOf('/') + 1);
                                        }
                                        File dataLoc = new File(GlobalConf.data.DATA_LOCATION);
                                        File directory = new File(dataLoc, basePath);
                                        Collection<File> files = FileUtils.listFilesAndDirs(directory, new WildcardFileFilter(baseName), new WildcardFileFilter(baseName));
                                        for (File file : files) {
                                            if (!file.equals(directory) && file.exists()) {
                                                FileUtils.forceDelete(file);
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.error(e);
                                    }
                                }
                            } else {
                                // Only remove "check"
                                try {
                                    FileUtils.forceDelete(check.toFile());
                                } catch (IOException e) {
                                    logger.error(e);
                                }
                            }
                            // RELOAD DATASETS VIEW
                            Gdx.app.postRunnable(() -> {
                                reloadAll();
                            });

                            return true;
                        }
                        return false;
                    });
                    rubbishes.add(rubbish);
                }

                datasetsTable.add(cb).left().padRight(padl).padBottom(pad);
                datasetsTable.add(descGroup).left().padRight(padl).padBottom(pad);
                datasetsTable.add(vers).center().padRight(padl).padBottom(pad);
                datasetsTable.add(typeImage).center().padRight(padl).padBottom(pad);
                datasetsTable.add(size).left().padRight(padl).padBottom(pad);
                datasetsTable.add(haveit).center().padBottom(pad);
                if (exists) {
                    datasetsTable.add(rubbish).center().padLeft(padl * 2.5f);
                }
                datasetsTable.row();

                choiceList.add(new Trio<>(dataset, cb, haveit));
            }

        }

        datasetsScroll = new OwnScrollPane(datasetsTable, skin, "minimalist-nobg");
        datasetsScroll.setScrollingDisabled(true, false);
        datasetsScroll.setForceScroll(false, false);
        datasetsScroll.setSmoothScrolling(false);
        datasetsScroll.setFadeScrollBars(false);
        datasetsScroll.setHeight(Math.min(Gdx.graphics.getHeight() * 0.45f, 750f * GlobalConf.SCALE_FACTOR));
        datasetsScroll.setWidth(Math.min(Gdx.graphics.getWidth() * 0.9f, GlobalConf.SCALE_FACTOR > 1.4f ? 600f * GlobalConf.SCALE_FACTOR : 750f * GlobalConf.SCALE_FACTOR));

        downloadTable.add(datasetsScroll).center().padBottom(padl).colspan(2).row();

        // Current dataset info
        currentDownloadFile = new OwnLabel(I18n.txt("gui.download.idle"), skin);
        downloadTable.add(currentDownloadFile).center().colspan(2).padBottom(padl).row();

        // Download button
        downloadButton = new OwnTextButton(I18n.txt("gui.download.download"), skin, "download");
        downloadButton.pad(buttonpad * 4f);
        downloadButton.setMinWidth(minw);
        downloadButton.setMinHeight(50f * GlobalConf.SCALE_FACTOR);
        downloadTable.add(downloadButton).center().colspan(2).padBottom(0f).row();

        // Progress bar
        downloadProgress = new OwnProgressBar(0, 100, 0.1f, false, skin, "default-horizontal");
        downloadProgress.setValue(0);
        downloadProgress.setVisible(false);
        downloadProgress.setPrefWidth(minw);
        downloadTable.add(downloadProgress).center().colspan(2).padBottom(padl).row();

        // Download info
        downloadSpeed = new OwnLabel("", skin);
        downloadSpeed.setVisible(false);
        downloadTable.add(downloadSpeed).center().colspan(2).padBottom(padl).row();

        downloadButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                downloadAndExtractFiles(choiceList);
            }
            return true;
        });

        // Progress

        // External download link
        Link manualDownload = new Link("Manual download", skin, "link", "http://gaia.ari.uni-heidelberg.de/gaiasky/files/autodownload");
        downloadTable.add(manualDownload).center().colspan(2);

        topCell.setActor(downloadTable);

    }

    private synchronized void downloadAndExtractFiles(List<Trio<JsonValue, OwnCheckBox, OwnLabel>> choices) {
        toDownload = new Array<>();

        for (Trio<JsonValue, OwnCheckBox, OwnLabel> entry : choices) {
            if (entry.getSecond().isChecked())
                toDownload.add(entry);
        }

        // Disable all checkboxes
        setDisabled(choices, true);
        // Disable all rubbishes
        setDisabled(rubbishes, true);

        logger.info(toDownload.size + " new data files selected to download");

        current = -1;
        downloadNext();

    }

    private void downloadNext() {
        current++;
        downloadCurrent();
    }

    private void downloadCurrent() {
        if (current >= 0 && current < toDownload.size) {
            // Download next
            Trio<JsonValue, OwnCheckBox, OwnLabel> trio = toDownload.get(current);
            JsonValue currentJson = trio.getFirst();
            String name = currentJson.getString("name");
            String url = currentJson.getString("file");
            String type = currentJson.getString("type");

            FileHandle tempDownload = Gdx.files.absolute(GlobalConf.data.DATA_LOCATION + "/temp.tar.gz");

            ProgressRunnable pr = (read, total, progress, speed) -> {
                double readMb = (double) read / 1e6d;
                double totalMb = (double) total / 1e6d;
                final String progressString = progress >= 100 ? I18n.txt("gui.done") : I18n.txt("gui.download.downloading", nf.format(progress));
                double mbPerSecond = speed / 1000d;
                final String speedString = nf.format(readMb) + "/" + nf.format(totalMb) + " MB   -   " + nf.format(mbPerSecond) + " MB/s";
                // Since we are downloading on a background thread, post a runnable to touch UI
                Gdx.app.postRunnable(() -> {
                    downloadButton.setText(progressString);
                    downloadProgress.setValue((float) progress);
                    downloadSpeed.setText(speedString);
                });
            };

            ChecksumRunnable finish = (md5sum) -> {
                // Unpack
                int errors = 0;
                logger.info("Extracting: " + tempDownload.path());
                String dataLocation = GlobalConf.data.DATA_LOCATION + File.separatorChar;
                // Checksum
                if (md5sum != null && currentJson.has("md5")) {
                    String serverMd5 = currentJson.getString("md5");
                    try {
                        boolean ok = serverMd5.equals(md5sum);
                        if (ok) {
                            logger.info("MD5 checksum ok: " + name);
                        } else {
                            logger.error("MD5 checkum failed: " + name);
                            errors++;
                        }
                    } catch (Exception e) {
                        logger.info("Error checking MD5: " + name);
                        errors++;
                    }
                } else {
                    logger.info("No checksum found for dataset: " + name);
                }

                if (errors == 0)
                    try {
                        // Extract
                        decompress(tempDownload.path(), new File(dataLocation), downloadButton, downloadProgress);
                        // Remove archive
                        cleanupTempFiles();
                    } catch (Exception e) {
                        logger.error(e, "Error decompressing: " + name);
                        errors++;
                    }

                // Done
                Gdx.app.postRunnable(() -> {
                    downloadButton.setText(I18n.txt("gui.download.download"));
                    downloadProgress.setValue(0);
                    downloadProgress.setVisible(false);
                    downloadSpeed.setText("");
                    downloadSpeed.setVisible(false);
                    me.acceptButton.setDisabled(false);
                    // Enable all
                    setDisabled(choiceList, false);
                    // Enable rubbishes
                    setDisabled(rubbishes, false);
                });

                if (errors == 0) {
                    // Select dataset if needed
                    if (type.startsWith("catalog-")) {
                        // Descriptor file
                        FileHandle descFile = Gdx.files.absolute(GlobalConf.data.DATA_LOCATION + File.separator + currentJson.getString("check"));
                        GlobalConf.data.CATALOG_JSON_FILES = descFile.path();
                    }

                    setMessageOk(I18n.txt("gui.download.idle"));
                    setStatusFound(trio.getThird());

                    Gdx.app.postRunnable(() -> {
                        downloadNext();
                    });
                } else {
                    logger.info("Error getting dataset: " + name);
                    setStatusError(trio.getThird());
                    setMessageError(I18n.txt("gui.download.failed", name));
                }

            };

            Runnable fail = () -> {
                logger.error("Download failed: " + name);
                setStatusError(trio.getThird());
                setMessageError(I18n.txt("gui.download.failed", name));
                me.acceptButton.setDisabled(false);
                downloadProgress.setVisible(false);
                downloadSpeed.setVisible(false);
                Gdx.app.postRunnable(() -> downloadNext());
            };

            Runnable cancel = () -> {
                logger.error("Download cancelled: " + name);
                setStatusCancelled(trio.getThird());
                setMessageError(I18n.txt("gui.download.failed", name));
                me.acceptButton.setDisabled(false);
                downloadProgress.setVisible(false);
                downloadSpeed.setVisible(false);
                Gdx.app.postRunnable(() -> downloadNext());
            };

            // Download
            downloadButton.setDisabled(true);
            me.acceptButton.setDisabled(true);
            downloadProgress.setVisible(true);
            downloadSpeed.setVisible(true);
            setStatusProgress(trio.getThird());
            setMessageOk(I18n.txt("gui.download.downloading.info", (current + 1), toDownload.size, currentJson.getString("name")));
            DownloadHelper.downloadFile(url, tempDownload, pr, finish, fail, cancel);
        } else {
            // Finished all downloads!
            // RELOAD DATASETS VIEW
            Gdx.app.postRunnable(() -> reloadAll());
        }

    }

    /**
     * Checks the version file of the given path, if it is a correct JSON
     * file and contains a top-level "version" attribute. Otherwise, it
     * returns the default lowest version (0)
     *
     * @param path The path with the file to check
     * @return The version, if it exists, or 0
     */
    private int checkJsonVersion(Path path) throws RuntimeException {
        if (path != null) {
            File file = path.toFile();
            if (file.exists() && file.canRead() && file.isFile()) {
                String fname = file.getName();
                String extension = fname.substring(fname.lastIndexOf(".") + 1);
                if (extension.equalsIgnoreCase("json")) {
                    JsonValue jf = reader.parse(Gdx.files.absolute(file.getAbsolutePath()));
                    return jf.getInt("version", 0);
                }
            }

            return 0;
        } else {
            throw new RuntimeException("Path is null");
        }
    }

    private void setDisabled(Array<OwnImageButton> l, boolean disabled) {
        for (OwnImageButton b : l)
            b.setDisabled(disabled);
    }

    private void setDisabled(List<Trio<JsonValue, OwnCheckBox, OwnLabel>> choices, boolean disabled) {
        for (Trio<JsonValue, OwnCheckBox, OwnLabel> t : choices) {
            // Uncheck all if enabling again
            if (!disabled)
                t.getSecond().setChecked(false);
            // Only enable datasets which we don't have
            if (disabled || (!t.getThird().getText().toString().equals(I18n.txt("gui.download.status.found"))))
                t.getSecond().setDisabled(disabled);
        }
    }

    private void decompress(String in, File out, OwnTextButton b, ProgressBar p) throws Exception {
        FileInfoInputStream fin = new FileInfoInputStream(in);
        GzipCompressorInputStream gzin = new GzipCompressorInputStream(fin);
        TarArchiveInputStream tarin = new TarArchiveInputStream(gzin);
        double sizeKb = fileSize(in) / 1000d;
        String sizeKbStr = nf.format(sizeKb);
        TarArchiveEntry entry;
        long last = 0;
        while ((entry = tarin.getNextTarEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }
            File curFile = new File(out, entry.getName());
            File parent = curFile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }

            IOUtils.copy(tarin, new FileOutputStream(curFile));

            // Every 250 ms we update the view
            long current = System.currentTimeMillis();
            long elapsed = current - last;
            if (elapsed > 250) {
                Gdx.app.postRunnable(() -> {
                    float val = (float) ((fin.getBytesRead() / 1000d) / sizeKb);
                    b.setText(I18n.txt("gui.download.extracting", nf.format(fin.getBytesRead() / 1000d) + "/" + sizeKbStr + " Kb"));
                    p.setValue(val * 100);
                });
                last = current;
            }

        }
    }

    /**
     * Returns the file size
     *
     * @param inputFilePath A file
     * @return The size in bytes
     */
    private long fileSize(String inputFilePath) {
        return new File(inputFilePath).length();
    }

    /**
     * Returns the GZ uncompressed size
     *
     * @param inputFilePath A gzipped file
     * @return The uncompressed size in bytes
     * @throws Exception
     */
    private long fileSizeGZUncompressed(String inputFilePath) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(inputFilePath, "r");
        raf.seek(raf.length() - 4);
        byte[] bytes = new byte[4];
        raf.read(bytes);
        long fileSize = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).getLong();
        if (fileSize < 0)
            fileSize += (1L << 32);
        raf.close();
        return fileSize;
    }

    private void cleanupTempFiles() {
        Path tempDownload = Paths.get(GlobalConf.data.DATA_LOCATION, "temp.tar.gz");
        if (Files.exists(tempDownload)) {
            //try {
            //Files.delete(tempDownload);
            //} catch (IOException e) {
            //    logger.error(e, "Failed cleaning up file: " + tempDownload.toString());
            //}
        }
    }

    private void setStatusOutdated(OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.outdated"));
        label.setColor(1, 1, 0, 1);
    }

    private void setStatusFound(OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.found"));
        label.setColor(0, 1, 0, 1);
    }

    private void setStatusNotFound(OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.notfound"));
        label.setColor(1, 0.3f, 0, 1);
    }

    private void setStatusError(OwnLabel label) {
        label.setText("Failed");
        label.setText(I18n.txt("gui.download.status.failed"));
        label.setColor(1, 0, 0, 1);
    }

    private void setStatusCancelled(OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.cancelled"));
        label.setColor(1, 0, 0, 1);
    }

    private void setStatusProgress(OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.working"));
        label.setColor(.3f, .3f, 1, 1);
    }

    private void setMessageOk(String text) {
        currentDownloadFile.setText(text);
        currentDownloadFile.setColor(currentDownloadFile.getStyle().fontColor);
    }

    private void setMessageError(String text) {
        currentDownloadFile.setText(text);
        currentDownloadFile.setColor(1, 0, 0, 1);
    }

    @Override
    protected void accept() {
        cleanupTempFiles();
    }

    @Override
    protected void cancel() {
        cleanupTempFiles();
    }

    private void backupScrollValues() {
        if (datasetsScroll != null) {
            this.scrollY = datasetsScroll.getScrollY();
            this.scrollX = datasetsScroll.getScrollX();
        }
    }

    private void restoreScrollValues() {
        if (datasetsScroll != null) {
            Gdx.app.postRunnable(() -> {
                datasetsScroll.setScrollX(scrollX);
                datasetsScroll.setScrollY(scrollY);
            });

        }
    }

    /**
     * Drops the current view and regenerates all window content
     */
    private void reloadAll() {
        backupScrollValues();
        content.clear();
        build();
        pack();
        restoreScrollValues();
    }

}
