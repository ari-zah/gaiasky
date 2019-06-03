/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.scene2d;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ArraySelection;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaia.cu9.ari.gaiaorbit.interfce.GenericDialog;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.TextUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;

public class FileChooser extends GenericDialog {

    public interface ResultListener {
        boolean result(boolean success, FileHandle result);
    }

    /**
     * The type of files that can be chosen with this file chooser
     */
    public enum FileChooserTarget {
        FILES,
        DIRECTORIES,
        ALL
    }

    /** Target of this file chooser **/
    final private FileChooserTarget target;
    private boolean fileNameEnabled;
    private TextField fileNameInput;
    private Label fileNameLabel, acceptedFiles;
    private FileHandle baseDir;
    private Label fileListLabel;
    private List<FileListItem> fileList;
    private HorizontalGroup driveButtonsList;
    private Array<TextButton> driveButtons;
    private float scrollPaneWidth, maxPathLength;
    private ScrollPane scrollPane;
    private CheckBox hidden;

    private FileHandle currentDir;
    protected String result;

    private boolean showHidden = false;

    protected ResultListener resultListener;
    protected EventListener selectionListener;

    private static final Comparator<FileListItem> dirListComparator = (file1, file2) -> {
        if (file1.file.isDirectory() && !file2.file.isDirectory()) {
            return -1;
        }
        if (file1.file.isDirectory() && file2.file.isDirectory()) {
            return file1.name.compareTo(file2.name);
        }
        if (!file1.file.isDirectory() && !file2.file.isDirectory()) {
            return file1.name.compareTo(file2.name);
        }
        return 1;
    };
    /** Filters files that appear in the file chooser. For internal use only! **/
    private FileFilter filter;
    /** Enables directories in the file chooser **/
    private final boolean directoryBrowsingEnabled;
    /** Enables files in the file chooser **/
    private final boolean fileBrowsingEnabled;
    /** Allows setting filters on the files which are to be selected **/
    private PathnameFilter pathnameFilter;

    public FileChooser(String title, final Skin skin, Stage stage, FileHandle baseDir, FileChooserTarget target) {
        this(title, skin, stage, baseDir, target, null);
    }



    public FileChooser(String title, final Skin skin, Stage stage, FileHandle baseDir, FileChooserTarget target, EventListener selectionListener) {
        this(title, skin, stage, baseDir, target, selectionListener, true);
    }
    public FileChooser(String title, final Skin skin, Stage stage, FileHandle baseDir, FileChooserTarget target, EventListener selectionListener, boolean directoryBrowsingEnabled) {
        super(title, skin, stage);
        this.baseDir = baseDir;
        this.selectionListener = selectionListener;
        this.target = target;
        // Browse files if we are picking files
        this.fileBrowsingEnabled = target == FileChooserTarget.FILES;
        // Browse directories
        this.directoryBrowsingEnabled = directoryBrowsingEnabled;

        setCancelText(I18n.txt("gui.close"));
        setAcceptText(I18n.txt("gui.select"));

        buildSuper();
    }

    @Override
    public void build() {
        scrollPaneWidth = 400 * GlobalConf.SCALE_FACTOR;
        maxPathLength = GlobalConf.SCALE_FACTOR > 1.5f ? 9.5f : 5.5f;

        content.top().left();
        content.defaults().space(5 * GlobalConf.SCALE_FACTOR);
        this.padLeft(10 * GlobalConf.SCALE_FACTOR);
        this.padRight(10 * GlobalConf.SCALE_FACTOR);

        // In windows, we need to be able to change drives
        driveButtonsList = new HorizontalGroup();
        driveButtonsList.left().space(10 * GlobalConf.SCALE_FACTOR);
        File[] drives = File.listRoots();
        driveButtons = new Array<>(drives.length);
        for (File drive : drives) {
            TextButton driveButton = new OwnTextIconButton(drive.toString(), skin, "drive");
            driveButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    changeDirectory(new FileHandle(drive));
                    lastClick = 0;
                }
            });
            driveButtons.add(driveButton);
            driveButtonsList.addActor(driveButton);
        }

        fileListLabel = new Label("", skin);
        fileListLabel.setAlignment(Align.left);

        acceptedFiles = new Label("", skin, "sc-header");
        acceptedFiles.setAlignment(Align.right);

        fileList = new List<>(skin, "light");
        scrollPane = new OwnScrollPane(fileList, skin, "minimalist");
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setSmoothScrolling(true);
        scrollPane.setFadeScrollBars(false);
        fileList.getSelection().setProgrammaticChangeEvents(false);
        if (selectionListener != null)
            fileList.addListener(selectionListener);
        // Lookup with keyboard
        fileList.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.keyTyped) {
                    char ch = ie.getCharacter();
                    Array<FileListItem> l = fileList.getItems();
                    FileListItem toSelect = null;
                    for (FileListItem fli : l) {
                        if (Character.toUpperCase(fli.name.charAt(0)) == Character.toUpperCase(ch)) {
                            toSelect = fli;
                            break;
                        }
                    }
                    if (toSelect != null) {
                        fileList.setSelected(toSelect);
                        int si = fileList.getSelectedIndex();
                        float px = si * fileList.getItemHeight();
                        scrollPane.setScrollY(px);
                    }
                }
            }
            return false;
        });
        // Select items
        fileList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                final FileListItem selected = fileList.getSelected();
                result = selected.name;
                fileNameInput.setText(result);
                lastClick = 0;
            }
        });
        // Double-click behaviour
        fileList.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                final FileListItem selected = fileList.getSelected();
                if (TimeUtils.millis() - lastClick < 250) {
                    FileHandle sel = selected.file;
                    // Double click
                    if (directoryBrowsingEnabled && sel.isDirectory()) {
                        // Change directory
                        changeDirectory(sel);
                        lastClick = 0;
                    } else if (target == FileChooserTarget.FILES && (filter == null || filter.accept(sel.file()))) {
                        // Accept
                        acceptButton.fire(new ChangeListener.ChangeEvent());

                        lastClick = 0;
                    }
                } else if (event.getType() == Type.touchUp) {
                    lastClick = TimeUtils.millis();
                }
            }
        });

        fileNameInput = new TextField("", skin);
        fileNameLabel = new Label("File name:", skin);
        fileNameInput.setTextFieldListener((textField, c) -> result = textField.getText());

        hidden = new OwnCheckBox("Show hidden", skin, 5 * GlobalConf.SCALE_FACTOR);
        hidden.setChecked(false);
        hidden.addListener(event -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                this.showHidden = hidden.isChecked();
                changeDirectory(currentDir);
                return true;
            }
            return false;
        });

        filter = pathname -> {
            boolean root = (pathname.isDirectory() && directoryBrowsingEnabled) || (pathname.isFile() && fileBrowsingEnabled);
            if (root && pathnameFilter != null && pathname.isFile()) {
                root = pathnameFilter.accept(pathname);
            }
            return root;
        };
        setTargetListener();

        content.add(acceptedFiles).top().left().row();
        content.add(driveButtonsList).top().left().expandX().fillX().row();
        content.add(fileListLabel).top().left().expandX().fillX().row();
        content.add(scrollPane).size(scrollPaneWidth, 250 * GlobalConf.SCALE_FACTOR).left().fill().expand().row();
        content.add(hidden).top().left().row();
        if (fileNameEnabled) {
            content.add(fileNameLabel).fillX().expandX().row();
            content.add(fileNameInput).fillX().expandX().row();
            stage.setKeyboardFocus(fileNameInput);
        }

        changeDirectory(baseDir);
    }

    private void changeDirectory(FileHandle directory) {
        FileHandle lastDir = currentDir;
        currentDir = directory;
        String path = currentDir.path();

        maxPathLength = 6.5f;
        while (path.length() * maxPathLength > scrollPaneWidth * 0.9f) {
            path = TextUtils.capString(path, path.length() - 4, true);
        }
        fileListLabel.setText(path);

        final Array<FileListItem> items = new Array<>();

        final FileHandle[] list = directory.list(filter);
        for (final FileHandle handle : list) {
            // Only list hidden if user chose it
            if (showHidden || !handle.name().startsWith(".")) {
                FileListItem fli = new FileListItem(handle);
                items.add(fli);
            }
        }

        items.sort(dirListComparator);

        if (directory.file().getParentFile() != null) {
            items.insert(0, new FileListItem("..", directory.parent()));
        }

        acceptButton.setDisabled(true);
        fileList.setItems(items);
        scrollPane.layout();

        if (lastDir != null && lastDir.parent().equals(currentDir)) {
            // select last if we're going back
            Array<FileListItem> l = fileList.getItems();
            for (FileListItem fli : l) {
                if (fli.file.equals(lastDir)) {
                    fileList.setSelected(fli);
                    acceptButton.setDisabled(!isTargetOk(fli.file.file()));
                    break;
                }
            }
            float px = fileList.getItemHeight() * fileList.getSelectedIndex();
            scrollPane.setScrollY(px);
        } else {
            fileList.setSelected(null);
            scrollPane.setScrollY(0);
        }
    }

    private boolean isTargetOk(File file) {
        switch (target) {
        case FILES:
            return file.isFile();
        case DIRECTORIES:
            return file.isDirectory();
        default:
            return true;
        }
    }

    private void setTargetListener() {
        setSelectionListener(event1 -> {
            if (event1 instanceof ChangeListener.ChangeEvent) {
                List<FileChooser.FileListItem> list = (List<FileChooser.FileListItem>) event1.getListenerActor();
                if (list != null) {
                    ArraySelection<FileListItem> as = list.getSelection();
                    if (as != null && as.hasItems()) {
                        FileChooser.FileListItem selected = as.getLastSelected();
                        acceptButton.setDisabled(!isTargetOk(selected.file.file()));
                    }
                }
                return true;
            }
            return false;
        });
    }

    public void setAcceptedFiles(String accepted) {
        acceptedFiles.setText("Accepted: " + accepted);
    }

    public FileHandle getResult() {
        String path = currentDir.path() + "/";
        if (result != null && result.length() > 0) {
            String folder = currentDir.file().getName();
            if (folder.equals(result)) {
                if ((new FileHandle(path + result)).exists()) {
                    path += result;
                } else {
                    // Nothing
                }
            } else {
                path += result;
            }
        }
        return new FileHandle(path);
    }

    /**
     * Overrides the default filter. If you use this, the attributes {@link FileChooser#directoryBrowsingEnabled} and
     * {@file FileChooser#fileBrowsingEnabled} won't have effect anymore. To set additional filters on the
     * path names, use {@link FileChooser#setFileFilter(PathnameFilter)} instead.
     *
     * @param filter The new file filter
     * @return This file chooser
     */
    public FileChooser setFilter(FileFilter filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets the file filter. This filter will be used to check whether file pathnames are accepted or not. It works
     * in conjunction with {@file FileChooser#fileBrowsingEnabled},
     * so you do not need to check whether the pathname is a file.
     *
     * @param f The file filter
     * @return This file chooser
     */
    public FileChooser setFileFilter(PathnameFilter f) {
        this.pathnameFilter = f;
        return this;
    }

    /**
     * Sets a listener which runs when an entry is selected. Useful to show
     * some text, disable items, etc.
     *
     * @param listener The listener
     * @return This file chooser
     */
    private FileChooser setSelectionListener(EventListener listener) {
        if (listener != null) {
            if (this.selectionListener != null)
                fileList.removeListener(this.selectionListener);

            this.selectionListener = listener;
            if (!fileList.getListeners().contains(selectionListener, true)) {
                fileList.addListener(selectionListener);
            }
        }
        return this;
    }

    public FileChooser setFileNameEnabled(boolean fileNameEnabled) {
        this.fileNameEnabled = fileNameEnabled;
        return this;
    }

    public FileChooser setResultListener(ResultListener result) {
        this.resultListener = result;
        return this;
    }

    long lastClick = 0l;

    @Override
    public void accept() {
        if (resultListener != null) {
            resultListener.result(true, getResult());
        }
    }

    @Override
    public void cancel() {

    }

    public class FileListItem {

        public FileHandle file;
        public String name;

        public FileListItem(FileHandle file) {
            this.file = file;
            this.name = file.name();
        }

        public FileListItem(String name, FileHandle file) {
            this.file = file;
            this.name = name;
        }

        public String toString() {
            return name;
        }

    }

    public interface PathnameFilter {
        boolean accept(File pathname);
    }

}