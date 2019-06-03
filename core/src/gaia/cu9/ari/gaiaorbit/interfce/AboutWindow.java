/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.BufferUtils;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;
import gaia.cu9.ari.gaiaorbit.util.scene2d.*;
import gaia.cu9.ari.gaiaorbit.util.update.VersionCheckEvent;
import gaia.cu9.ari.gaiaorbit.util.update.VersionChecker;

import java.nio.IntBuffer;
import java.time.Instant;
import java.util.Date;

/**
 * The help window with About, Help and System sections.
 *
 * @author tsagrista
 */
public class AboutWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(AboutWindow.class);

    private LabelStyle linkStyle;
    private Table checkTable;
    private OwnLabel checkLabel;

    public AboutWindow(Stage stage, Skin skin) {
        super(I18n.txt("gui.help.help") + " - " + GlobalConf.version.version + " - " + I18n.txt("gui.build", GlobalConf.version.build), skin, stage);
        this.linkStyle = skin.get("link", LabelStyle.class);

        setCancelText(I18n.txt("gui.close"));

        // Build
        buildSuper();

    }

    @Override
    protected void build() {
        float taWidth = GlobalConf.SCALE_FACTOR > 1.5f ? 700 : 440;
        float taWidth2 = 800 * GlobalConf.SCALE_FACTOR;
        float taHeight = 100 * GlobalConf.SCALE_FACTOR;
        float tabWidth = 110 * GlobalConf.SCALE_FACTOR;

        // Create the tab buttons
        HorizontalGroup group = new HorizontalGroup();
        group.align(Align.left);

        final Button tabHelp = new OwnTextButton(I18n.txt("gui.help.help"), skin, "toggle-big");
        tabHelp.pad(pad5);
        tabHelp.setWidth(tabWidth);
        final Button tabAbout = new OwnTextButton(I18n.txt("gui.help.about"), skin, "toggle-big");
        tabAbout.pad(pad5);
        tabAbout.setWidth(tabWidth);
        final Button tabSystem = new OwnTextButton(I18n.txt("gui.help.system"), skin, "toggle-big");
        tabSystem.pad(pad5);
        tabSystem.setWidth(tabWidth);
        final Button tabUpdates = new OwnTextButton(I18n.txt("gui.newversion"), skin, "toggle-big");
        tabUpdates.pad(pad5);
        tabUpdates.setWidth(tabWidth);

        group.addActor(tabHelp);
        group.addActor(tabAbout);
        group.addActor(tabSystem);
        group.addActor(tabUpdates);
        content.add(group).align(Align.left).padLeft(pad5);
        content.row();
        content.pad(pad);

        // Create the tab content. Just using images here for simplicity.
        Stack tabContent = new Stack();

        /* CONTENT 1 - HELP */
        final Table contentHelp = new Table(skin);
        contentHelp.align(Align.top);

        FileHandle gslogo = Gdx.files.internal(GlobalConf.SCALE_FACTOR > 1.5f ? "img/gaiasky-logo.png" : "img/gaiasky-logo-s.png");
        Texture logotex = new Texture(gslogo);
        logotex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Image gaiasky = new Image(logotex);
        gaiasky.setOrigin(Align.center);

        // User manual
        Label homepageTitle = new OwnLabel(I18n.txt("gui.help.homepage"), skin);
        Label homepageTxt = new OwnLabel(I18n.txt("gui.help.help1"), skin);
        Link homepageLink = new Link(GlobalConf.WEBPAGE, linkStyle, GlobalConf.WEBPAGE);

        // Wiki
        Label docsTitle = new OwnLabel("Docs", skin);
        Label docsTxt = new OwnLabel(I18n.txt("gui.help.help2"), skin);
        Link docsLink = new Link(GlobalConf.DOCUMENTATION, linkStyle, GlobalConf.DOCUMENTATION);

        // Icon
        FileHandle gsIcon = Gdx.files.internal("icon/ic_launcher.png");
        Texture iconTex = new Texture(gsIcon);
        iconTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Image gaiaskyIcon = new Image(iconTex);
        gaiaskyIcon.setOrigin(Align.center);

        // Add all to content
        contentHelp.add(gaiasky).pad(pad * 2).colspan(2);
        contentHelp.row();
        contentHelp.add(homepageTitle).align(Align.left).padRight(pad);
        contentHelp.add(homepageTxt).align(Align.left);
        contentHelp.row();
        contentHelp.add(new OwnLabel("", skin));
        contentHelp.add(homepageLink).align(Align.left).padBottom(pad);
        contentHelp.row();
        contentHelp.add(docsTitle).align(Align.left).padRight(pad).padBottom(pad);
        contentHelp.add(docsTxt).align(Align.left);
        contentHelp.row();
        contentHelp.add(new OwnLabel("", skin));
        contentHelp.add(docsLink).align(Align.left).padBottom(pad * 4f);
        contentHelp.row();
        contentHelp.add(gaiaskyIcon).colspan(2).align(Align.center);

        /* CONTENT 2 - ABOUT */
        final Table contentAbout = new Table(skin);
        contentAbout.top().left();

        // Intro
        TextArea intro = new OwnTextArea(I18n.txt("gui.help.gscredits", GlobalConf.version.version), skin.get("regular", TextFieldStyle.class));
        intro.setDisabled(true);
        intro.setPrefRows(3);
        intro.setWidth(taWidth + 100 * GlobalConf.SCALE_FACTOR);

        // Home page
        Label homepagetitle = new OwnLabel(I18n.txt("gui.help.homepage"), skin);
        Link homepage = new Link(GlobalConf.WEBPAGE, linkStyle, GlobalConf.WEBPAGE);

        // Author
        Label authortitle = new OwnLabel(I18n.txt("gui.help.author"), skin);

        VerticalGroup author = new VerticalGroup();
        author.align(Align.left);
        Label authorname = new OwnLabel(GlobalConf.AUTHOR_NAME, skin);
        Link authormail = new Link(GlobalConf.AUTHOR_EMAIL, linkStyle, "mailto:" + GlobalConf.AUTHOR_EMAIL);
        Link authorpage = new Link("www.tonisagrista.com", linkStyle, "https://tonisagrista.com");
        author.addActor(authorname);
        author.addActor(authormail);
        author.addActor(authorpage);

        // Contributor
        Label contribtitle = new OwnLabel(I18n.txt("gui.help.contributors"), skin);

        VerticalGroup contrib = new VerticalGroup();
        contrib.align(Align.left);
        Label contribname = new OwnLabel("Apl. Prof. Dr. Stefan Jordan", skin);
        Link contribmail = new Link("jordan@ari.uni-heidelberg.de", linkStyle, "mailto:jordan@ari.uni-heidelberg.de");
        contrib.addActor(contribname);
        contrib.addActor(contribmail);

        // License
        HorizontalGroup licenseh = new HorizontalGroup();
        licenseh.space(pad * 2f);

        VerticalGroup licensev = new VerticalGroup();
        TextArea licensetext = new OwnTextArea(I18n.txt("gui.help.license"), skin.get("regular", TextFieldStyle.class));
        licensetext.setDisabled(true);
        licensetext.setPrefRows(3);
        licensetext.setWidth(taWidth2 / 2f);
        Link licenselink = new Link("https://opensource.org/licenses/MPL-2.0", linkStyle, "https://opensource.org/licenses/MPL-2.0");

        licensev.addActor(licensetext);
        licensev.addActor(licenselink);

        licenseh.addActor(licensev);

        // Thanks
        HorizontalGroup thanks = new HorizontalGroup();
        thanks.space(pad * 2f).pad(pad);
        Container<Actor> thanksc = new Container<>(thanks);
        thanksc.setBackground(skin.getDrawable("bg-clear"));

        Image zah = new Image(getSpriteDrawable(Gdx.files.internal("img/zah.png")));
        Image dlr = new Image(getSpriteDrawable(Gdx.files.internal("img/dlr.png")));
        Image bwt = new Image(getSpriteDrawable(Gdx.files.internal("img/bwt.png")));
        Image dpac = new Image(getSpriteDrawable(Gdx.files.internal("img/dpac.png")));

        thanks.addActor(zah);
        thanks.addActor(dlr);
        thanks.addActor(bwt);
        thanks.addActor(dpac);

        contentAbout.add(intro).colspan(2).align(Align.left).padTop(pad * 2f);
        contentAbout.row();
        contentAbout.add(homepagetitle).align(Align.topLeft).padRight(pad * 2f).padTop(pad * 2f);
        contentAbout.add(homepage).align(Align.left).padTop(pad * 2f);
        contentAbout.row();
        contentAbout.add(authortitle).align(Align.topLeft).padRight(pad).padTop(pad * 2f);
        contentAbout.add(author).align(Align.left).padTop(pad5).padTop(pad * 2f);
        contentAbout.row();
        contentAbout.add(contribtitle).align(Align.topLeft).padRight(pad).padTop(pad * 2f);
        contentAbout.add(contrib).align(Align.left).padTop(pad * 2f);
        contentAbout.row();
        contentAbout.add(licenseh).colspan(2).align(Align.center).padTop(pad * 2f);
        contentAbout.row();
        contentAbout.add(thanksc).colspan(2).align(Align.center).padTop(pad * 4f);

        /* CONTENT 3 - SYSTEM */
        final Table contentSystem = new Table(skin);
        contentSystem.top().left();

        // Build info
        Label buildinfo = new OwnLabel(I18n.txt("gui.help.buildinfo"), skin, "help-title");

        Label versiontitle = new OwnLabel(I18n.txt("gui.help.version", GlobalConf.APPLICATION_NAME), skin);
        Label version = new OwnLabel(GlobalConf.version.version, skin);

        Label revisiontitle = new OwnLabel(I18n.txt("gui.help.buildnumber"), skin);
        Label revision = new OwnLabel(GlobalConf.version.build, skin);

        Label timetitle = new OwnLabel(I18n.txt("gui.help.buildtime"), skin);
        Label time = new OwnLabel(GlobalConf.version.buildtime.toString(), skin);

        Label systemtitle = new OwnLabel(I18n.txt("gui.help.buildsys"), skin);
        TextArea system = new OwnTextArea(GlobalConf.version.system, skin.get("regular", TextFieldStyle.class));
        system.setDisabled(true);
        system.setPrefRows(3);
        system.setWidth(taWidth * 2f / 3f);

        Label buildertitle = new OwnLabel(I18n.txt("gui.help.builder"), skin);
        Label builder = new OwnLabel(GlobalConf.version.builder, skin);

        // Java info
        Label javainfo = new OwnLabel(I18n.txt("gui.help.javainfo"), skin, "help-title");

        Label javaversiontitle = new OwnLabel(I18n.txt("gui.help.javaversion"), skin);
        Label javaversion = new OwnLabel(System.getProperty("java.version"), skin);

        Label javaruntimetitle = new OwnLabel(I18n.txt("gui.help.javaname"), skin);
        Label javaruntime = new OwnLabel(System.getProperty("java.runtime.name"), skin);

        Label javavmnametitle = new OwnLabel(I18n.txt("gui.help.javavmname"), skin);
        Label javavmname = new OwnLabel(System.getProperty("java.vm.name"), skin);

        Label javavmversiontitle = new OwnLabel(I18n.txt("gui.help.javavmversion"), skin);
        Label javavmversion = new OwnLabel(System.getProperty("java.vm.version"), skin);

        Label javavmvendortitle = new OwnLabel(I18n.txt("gui.help.javavmvendor"), skin);
        Label javavmvendor = new OwnLabel(System.getProperty("java.vm.vendor"), skin);

        TextButton memoryinfobutton = new OwnTextButton(I18n.txt("gui.help.meminfo"), skin, "default");
        memoryinfobutton.setName("memoryinfo");
        memoryinfobutton.setSize(150f * GlobalConf.SCALE_FACTOR, 20f * GlobalConf.SCALE_FACTOR);
        memoryinfobutton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.DISPLAY_MEM_INFO_WINDOW);
                return true;
            }
            return false;
        });

        // System info
        Label sysinfo = new OwnLabel(I18n.txt("gui.help.sysinfo"), skin, "help-title");

        Label sysostitle = new OwnLabel(I18n.txt("gui.help.os"), skin);
        Label sysos = new OwnLabel(System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"), skin);

        Label glrenderertitle = new OwnLabel(I18n.txt("gui.help.graphicsdevice"), skin);
        Label glrenderer = new OwnLabel(Gdx.gl.glGetString(GL20.GL_RENDERER), skin);

        // OpenGL info
        Label glinfo = new OwnLabel(I18n.txt("gui.help.openglinfo"), skin, "help-title");

        Label glvendortitle = new OwnLabel(I18n.txt("gui.help.glvendor"), skin);
        Label glvendor = new OwnLabel(Gdx.gl.glGetString(GL20.GL_VENDOR), skin);

        Label glversiontitle = new OwnLabel(I18n.txt("gui.help.openglversion"), skin);
        Label glversion = new OwnLabel(Gdx.gl.glGetString(GL20.GL_VERSION), skin);

        Label glslversiontitle = new OwnLabel(I18n.txt("gui.help.glslversion"), skin);
        Label glslversion = new OwnLabel(Gdx.gl.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION), skin);

        Label glextensionstitle = new OwnLabel(I18n.txt("gui.help.glextensions"), skin);
        String extensions = Gdx.gl.glGetString(GL20.GL_EXTENSIONS);
        IntBuffer buf = BufferUtils.newIntBuffer(16);
        if (extensions == null || extensions.isEmpty()) {
            Gdx.gl.glGetIntegerv(GL30.GL_NUM_EXTENSIONS, buf);
            int next = buf.get(0);
            String[] extensionsstr = new String[next];
            for (int i = 0; i < next; i++) {
                extensionsstr[i] = Gdx.gl30.glGetStringi(GL30.GL_EXTENSIONS, i);
            }
            extensions = arrayToStr(extensionsstr);
        } else {
            extensions = extensions.replaceAll(" ", "\n");
        }
        Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, buf);
        int maxSize = buf.get(0);
        int lines = GlobalResources.countOccurrences(extensions, '\n');
        OwnTextArea glextensions = new OwnTextArea("Max texture size: " + maxSize + '\n' + extensions, skin, "no-disabled");
        glextensions.setDisabled(true);
        glextensions.setPrefRows(lines);
        glextensions.clearListeners();

        OwnScrollPane glextensionsscroll = new OwnScrollPane(glextensions, skin, "minimalist-nobg");
        glextensionsscroll.setWidth(taWidth);
        glextensionsscroll.setHeight(taHeight);
        glextensionsscroll.setForceScroll(false, true);
        glextensionsscroll.setSmoothScrolling(true);
        glextensionsscroll.setFadeScrollBars(false);
        scrolls.add(glextensionsscroll);

        contentSystem.add(buildinfo).colspan(2).align(Align.left).padTop(pad * 2f).padBottom(pad);
        contentSystem.row();
        contentSystem.add(versiontitle).align(Align.topLeft).padRight(pad);
        contentSystem.add(version).align(Align.left);
        contentSystem.row();
        contentSystem.add(revisiontitle).align(Align.topLeft).padRight(pad).padTop(pad5);
        contentSystem.add(revision).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(timetitle).align(Align.topLeft).padRight(pad).padTop(pad5);
        contentSystem.add(time).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(buildertitle).align(Align.topLeft).padRight(pad).padTop(pad5);
        contentSystem.add(builder).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(systemtitle).align(Align.topLeft).padRight(pad).padTop(pad5);
        contentSystem.add(system).align(Align.left).padTop(pad5);
        contentSystem.row();

        contentSystem.add(javainfo).colspan(2).align(Align.left).padTop(pad).padBottom(pad);
        contentSystem.row();
        contentSystem.add(javaversiontitle).align(Align.topLeft).padRight(pad);
        contentSystem.add(javaversion).align(Align.left);
        contentSystem.row();
        contentSystem.add(javaruntimetitle).align(Align.topLeft).padRight(pad).padTop(pad5);
        contentSystem.add(javaruntime).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(javavmnametitle).align(Align.topLeft).padRight(pad).padTop(pad5);
        contentSystem.add(javavmname).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(javavmversiontitle).align(Align.topLeft).padRight(pad).padTop(pad5);
        contentSystem.add(javavmversion).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(javavmvendortitle).align(Align.topLeft).padRight(pad).padTop(pad5);
        contentSystem.add(javavmvendor).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(memoryinfobutton).colspan(2).align(Align.left).padTop(pad);
        contentSystem.row();

        contentSystem.add(sysinfo).colspan(2).align(Align.left).padTop(pad).padBottom(pad);
        contentSystem.row();
        contentSystem.add(sysostitle).align(Align.topLeft).padRight(pad);
        contentSystem.add(sysos).align(Align.left);
        contentSystem.row();
        contentSystem.add(glrenderertitle).align(Align.topLeft).padRight(pad).padTop(pad5);
        contentSystem.add(glrenderer).align(Align.left).padTop(pad5);
        contentSystem.row();

        contentSystem.add(glinfo).colspan(2).align(Align.left).padTop(pad).padBottom(pad);
        contentSystem.row();
        contentSystem.add(glversiontitle).align(Align.topLeft).padRight(pad);
        contentSystem.add(glversion).align(Align.left);
        contentSystem.row();
        contentSystem.add(glvendortitle).align(Align.topLeft).padRight(pad).padTop(pad5);
        contentSystem.add(glvendor).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(glslversiontitle).align(Align.topLeft).padRight(pad).padTop(pad5);
        contentSystem.add(glslversion).align(Align.left).padTop(pad5);
        contentSystem.row();
        contentSystem.add(glextensionstitle).align(Align.topLeft).padRight(pad).padTop(pad5);
        contentSystem.add(glextensionsscroll).align(Align.left).padTop(pad5);

        /* CONTENT 4 - UPDATES */
        final Table contentUpdates = new Table(skin);
        contentUpdates.align(Align.top);

        // This is the table that displays it all
        checkTable = new Table(skin);
        checkLabel = new OwnLabel("", skin);

        checkTable.add(checkLabel).top().left().padBottom(pad5).row();
        if (GlobalConf.program.VERSION_LAST_TIME == null || new Date().getTime() - GlobalConf.program.VERSION_LAST_TIME.toEpochMilli() > GlobalConf.ProgramConf.VERSION_CHECK_INTERVAL_MS) {
            // Check!
            checkLabel.setText(I18n.txt("gui.newversion.checking"));
            getCheckVersionThread().start();
        } else {
            // Inform latest
            newVersionCheck(GlobalConf.version.version, GlobalConf.version.versionNumber, GlobalConf.version.buildtime, false);
        }

        contentUpdates.add(checkTable).left().top().padTop(pad * 2f);

        /** ADD ALL CONTENT **/
        tabContent.addActor(contentHelp);
        tabContent.addActor(contentAbout);
        tabContent.addActor(contentSystem);
        tabContent.addActor(contentUpdates);

        content.add(tabContent).expand().fill();

        // Listen to changes in the tab button checked states
        // Set visibility of the tab content to match the checked state
        ChangeListener tabListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                contentHelp.setVisible(tabHelp.isChecked());
                contentAbout.setVisible(tabAbout.isChecked());
                contentSystem.setVisible(tabSystem.isChecked());
                contentUpdates.setVisible(tabUpdates.isChecked());
            }
        };
        tabHelp.addListener(tabListener);
        tabAbout.addListener(tabListener);
        tabSystem.addListener(tabListener);
        tabUpdates.addListener(tabListener);

        // Let only one tab button be checked at a time
        ButtonGroup<Button> tabs = new ButtonGroup<>();
        tabs.setMinCheckCount(1);
        tabs.setMaxCheckCount(1);
        tabs.add(tabHelp);
        tabs.add(tabAbout);
        tabs.add(tabSystem);
        tabs.add(tabUpdates);

    }

    private boolean exists(FileHandle fh) {
        try {
            fh.read().close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String arrayToStr(String[] arr) {
        String buff = new String();
        for (int i = 0; i < arr.length; i++) {
            buff += arr[i] + '\n';
        }
        return buff;
    }

    @Override
    protected void accept() {
    }

    @Override
    protected void cancel() {
    }

    private SpriteDrawable getSpriteDrawable(FileHandle fh) {
        Texture tex = new Texture(fh);
        return new SpriteDrawable(new Sprite(tex));
    }

    /**
     * Checks the given tag time against the current version time and:
     * <ul>
     * <li>Displays a "new version available" message if the given version is
     * newer than the current.</li>
     * <li>Display a "you have the latest version" message and a "check now"
     * button if the given version is older.</li>
     * </ul>
     *
     * @param tagVersion    The version to check
     * @param versionNumber The version number
     * @param tagDate       The date
     */
    private void newVersionCheck(String tagVersion, Integer versionNumber, Instant tagDate, boolean log) {
        GlobalConf.program.VERSION_LAST_TIME = Instant.now();
        if (versionNumber > GlobalConf.version.versionNumber) {
            IDateFormat df = DateFormatFactory.getFormatter(I18n.locale, DateFormatFactory.DateType.DATETIME);
            if (log) {
                logger.info(I18n.txt("gui.newversion.available", GlobalConf.version.version, tagVersion + " [" + df.format(tagDate) + "]"));
            }
            // There's a new version!
            checkLabel.setText(I18n.txt("gui.newversion.available", GlobalConf.version, tagVersion + " [" + df.format(tagDate) + "]"));
            final String uri = GlobalConf.WEBPAGE_DOWNLOADS;

            OwnTextButton getNewVersion = new OwnTextButton(I18n.txt("gui.newversion.getit"), skin);
            getNewVersion.pad(pad5, pad, pad5, pad);
            getNewVersion.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    Gdx.net.openURI(GlobalConf.WEBPAGE_DOWNLOADS);
                    return true;
                }
                return false;
            });
            checkTable.add(getNewVersion).center().padTop(pad).padBottom(pad5).row();

            Link link = new Link(uri, linkStyle, uri);
            checkTable.add(link).center();

        } else {
            if (log)
                logger.info(I18n.txt("gui.newversion.nonew", GlobalConf.program.getLastCheckedString()));
            checkLabel.setText(I18n.txt("gui.newversion.nonew", GlobalConf.program.getLastCheckedString()));
            // Add check now button
            OwnTextButton checkNewVersion = new OwnTextButton(I18n.txt("gui.newversion.checknow"), skin);
            checkNewVersion.pad(pad5, pad, pad5, pad);
            checkNewVersion.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    checkLabel.setText(I18n.txt("gui.newversion.checking"));
                    logger.info(I18n.txt("gui.newversion.checking"));
                    getCheckVersionThread().start();
                    return true;
                }
                return false;
            });
            checkTable.add(checkNewVersion).center().padTop(pad);
        }
    }

    private Thread getCheckVersionThread() {
        // Start version check
        VersionChecker vc = new VersionChecker(GlobalConf.program.VERSION_CHECK_URL);
        vc.setListener(event -> {
            if (event instanceof VersionCheckEvent) {
                VersionCheckEvent vce = (VersionCheckEvent) event;
                if (!vce.isFailed()) {
                    checkTable.clear();
                    checkTable.add(checkLabel).top().left().padBottom(pad5).row();
                    // All is fine
                    newVersionCheck(vce.getTag(), vce.getVersionNumber(), vce.getTagTime(), true);

                } else {
                    // Handle failed case
                    logger.info(I18n.txt("gui.newversion.fail"));
                    checkLabel.setText(I18n.txt("notif.error", "Could not get last version"));
                    checkLabel.setColor(Color.RED);
                }
            }
            return false;
        });
        return new Thread(vc);
    }

}
