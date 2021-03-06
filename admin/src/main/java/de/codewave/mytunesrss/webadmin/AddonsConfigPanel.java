/*
 * Copyright (c) 2010. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.webadmin;

import com.vaadin.data.validator.AbstractStringValidator;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.Sizeable;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.*;
import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.addons.AddonsUtils;
import de.codewave.mytunesrss.addons.LanguageDefinition;
import de.codewave.mytunesrss.addons.ThemeDefinition;
import de.codewave.mytunesrss.config.ExternalSiteDefinition;
import de.codewave.mytunesrss.config.FlashPlayerConfig;
import de.codewave.mytunesrss.config.PlaylistFileType;
import de.codewave.utils.io.FileProcessor;
import de.codewave.utils.io.ZipUtils;
import de.codewave.vaadin.SmartTextField;
import de.codewave.vaadin.VaadinUtils;
import de.codewave.vaadin.component.OptionWindow;
import de.codewave.vaadin.component.SelectWindow;
import de.codewave.vaadin.component.SinglePanelWindow;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AddonsConfigPanel extends MyTunesRssConfigPanel implements Upload.Receiver, Upload.SucceededListener, Upload.FailedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddonsConfigPanel.class);

    private static final String PREFIX = "upload_addon_";

    private static final Object DEFAULT_UI_THEME_ID = UUID.randomUUID();

    private Table myThemesTable;
    private Table myLanguagesTable;
    private Table mySitesTable;
    private Table myFlashPlayersTable;
    private Upload myUploadTheme;
    private Button myAddSite;
    private Button myAddFlashPlayer;
    private Button myRestoreDefaultJukeboxes;
    private Button myAddLanguage;

    @Override
    public void attach() {
        super.attach();
        init(getApplication().getBundleString("addonsConfigPanel.caption"), getApplication().getComponentFactory().createGridLayout(1, 5, true, true));
        Panel themesPanel = new Panel(getBundleString("addonsConfigPanel.caption.themes"), getComponentFactory().createVerticalLayout(true, true));
        myThemesTable = new Table();
        myThemesTable.setCacheRate(50);
        myThemesTable.addContainerProperty("defmarker", Embedded.class, null, getBundleString("addonsConfigPanel.themes.defmarker"), null, Table.ALIGN_CENTER);
        myThemesTable.addContainerProperty("name", String.class, null, getBundleString("addonsConfigPanel.themes.name"), null, null);
        myThemesTable.addContainerProperty("default", Button.class, null, "", null, null);
        myThemesTable.addContainerProperty("delete", Button.class, null, "", null, null);
        myThemesTable.addContainerProperty("export", Button.class, null, "", null, null);
        themesPanel.addComponent(myThemesTable);
        Panel themeButtons = new Panel();
        themeButtons.addStyleName("light");
        themeButtons.setContent(getApplication().getComponentFactory().createHorizontalLayout(false, true));
        myUploadTheme = new Upload(null, this);
        myUploadTheme.setButtonCaption(getBundleString("addonsConfigPanel.addTheme"));
        myUploadTheme.setImmediate(true);
        myUploadTheme.addListener((Upload.SucceededListener) this);
        myUploadTheme.addListener((Upload.FailedListener) this);
        themesPanel.addComponent(myUploadTheme);
        Panel languagesPanel = new Panel(getBundleString("addonsConfigPanel.caption.languages"), getComponentFactory().createVerticalLayout(true, true));
        myLanguagesTable = new Table();
        myLanguagesTable.setCacheRate(50);
        myLanguagesTable.addContainerProperty("name", String.class, null, getBundleString("addonsConfigPanel.languages.name"), null, null);
        myLanguagesTable.addContainerProperty("edit", Button.class, null, "", null, null);
        myLanguagesTable.addContainerProperty("delete", Button.class, null, "", null, null);
        myLanguagesTable.addContainerProperty("export", Button.class, null, "", null, null);
        languagesPanel.addComponent(myLanguagesTable);
        Panel languageButtons = new Panel();
        languageButtons.addStyleName("light");
        languageButtons.setContent(getApplication().getComponentFactory().createHorizontalLayout(false, true));
        myAddLanguage = getComponentFactory().createButton("addonsConfigPanel.addNewLanguage", this);
        languageButtons.addComponent(myAddLanguage);
        Upload uploadLanguage = new Upload(null, this);
        uploadLanguage.setButtonCaption(getBundleString("addonsConfigPanel.addLanguage"));
        uploadLanguage.setImmediate(true);
        uploadLanguage.addListener((Upload.SucceededListener) this);
        uploadLanguage.addListener((Upload.FailedListener) this);
        languageButtons.addComponent(uploadLanguage);
        languagesPanel.addComponent(languageButtons);
        Panel sitesPanel = new Panel(getBundleString("addonsConfigPanel.caption.sites"), getComponentFactory().createVerticalLayout(true, true));
        mySitesTable = new Table();
        mySitesTable.setWidth(100, UNITS_PERCENTAGE);
        mySitesTable.setCacheRate(50);
        mySitesTable.addContainerProperty("name", TextField.class, null, getBundleString("addonsConfigPanel.sites.name"), null, null);
        mySitesTable.addContainerProperty("type", Select.class, null, getBundleString("addonsConfigPanel.sites.type"), null, null);
        mySitesTable.addContainerProperty("url", TextField.class, null, getBundleString("addonsConfigPanel.sites.url"), null, null);
        mySitesTable.setColumnExpandRatio("url", 1);
        mySitesTable.addContainerProperty("delete", Button.class, null, "", null, null);
        sitesPanel.addComponent(mySitesTable);
        myAddSite = getComponentFactory().createButton("addonsConfigPanel.addSite", this);
        sitesPanel.addComponent(getComponentFactory().createHorizontalButtons(false, true, myAddSite));
        Panel flashPlayersPanel = new Panel(getBundleString("addonsConfigPanel.caption.flash"), getComponentFactory().createVerticalLayout(true, true));
        myFlashPlayersTable = new Table();
        myFlashPlayersTable.setCacheRate(50);
        myFlashPlayersTable.addContainerProperty("name", String.class, null, getBundleString("addonsConfigPanel.flash.name"), null, null);
        myFlashPlayersTable.addContainerProperty("edit", Button.class, null, "", null, null);
        myFlashPlayersTable.addContainerProperty("delete", Button.class, null, "", null, null);
        myFlashPlayersTable.addContainerProperty("download", Button.class, null, "", null, null);
        flashPlayersPanel.addComponent(myFlashPlayersTable);
        myAddFlashPlayer = getComponentFactory().createButton("addonsConfigPanel.addFlashPlayer", this);
        myRestoreDefaultJukeboxes = getComponentFactory().createButton("addonsConfigPanel.restoreDefaultJukeboxes", this);
        flashPlayersPanel.addComponent(getComponentFactory().createHorizontalButtons(false, true, myAddFlashPlayer, myRestoreDefaultJukeboxes));
        addComponent(themesPanel);
        addComponent(languagesPanel);
        addComponent(sitesPanel);
        addComponent(flashPlayersPanel);

        addDefaultComponents(0, 4, 0, 4, false);

        initFromConfig();
    }

    @Override
    protected void initFromConfig() {
        refreshThemes();
        refreshLanguages();
        refreshExternalSites();
        refreshFlashPlayers();
        setTablePageLengths();
    }

    private void refreshExternalSites() {
        mySitesTable.removeAllItems();
        for (ExternalSiteDefinition site : MyTunesRss.CONFIG.getExternalSites()) {
            addSite(site);
        }
    }

    private void refreshFlashPlayers() {
        myFlashPlayersTable.removeAllItems();
        List<FlashPlayerConfig> flashPlayers = new ArrayList<>(MyTunesRss.CONFIG.getFlashPlayers());
        Collections.sort(flashPlayers);
        for (FlashPlayerConfig flashPlayer : flashPlayers) {
            File jukeboxDir = new File(MyTunesRss.PREFERENCES_DATA_PATH + "/flashplayer/" + flashPlayer.getId());
            Button downloadButton = null;
            if (jukeboxDir.isDirectory()) {
                downloadButton = createTableRowButton("button.download", this, flashPlayer.getId(), "DownloadPlayer");
            }
            myFlashPlayersTable.addItem(new Object[]{flashPlayer.getName(), createTableRowButton("button.edit", this, flashPlayer.getId(), "EditPlayer"), createTableRowButton("button.delete", this, flashPlayer.getId(), "DeletePlayer"), downloadButton}, flashPlayer.getId());
        }
    }

    private void refreshLanguages() {
        myLanguagesTable.removeAllItems();
        List<LanguageDefinition> languages = new ArrayList<>(AddonsUtils.getLanguages(false));
        Collections.sort(languages, new Comparator<LanguageDefinition>() {
            @Override
            public int compare(LanguageDefinition languageDefinition1, LanguageDefinition languageDefinition2) {
                Locale adminLocale = AddonsConfigPanel.this.getApplication().getLocale();
                return languageDefinition1.getLocale().getDisplayName(adminLocale).compareTo(languageDefinition2.getLocale().getDisplayName(adminLocale));
            }
        });
        for (LanguageDefinition languageDefinition : languages) {
            if (languageDefinition != null) {
                myLanguagesTable.addItem(
                        new Object[] {
                            languageDefinition.getLocale().getDisplayName(getApplication().getLocale()),
                            createTableRowButton("button.edit", this, languageDefinition.getCode(), "EditLanguage"),
                            createTableRowButton("button.delete", this, languageDefinition.getCode(), "DeleteLanguage"),
                            createTableRowButton("button.export", this, languageDefinition.getCode(), "ExportLanguage")
                        },
                        languageDefinition.getCode()
                );
            }
        }
    }

    private void refreshThemes() {
        myThemesTable.removeAllItems();
        List<ThemeDefinition> themes = new ArrayList<>(AddonsUtils.getThemes(false));
        Collections.sort(themes);
        boolean isDefault = StringUtils.isEmpty(MyTunesRss.CONFIG.getDefaultUserInterfaceTheme());
        Embedded checkmark = new Embedded("", new ThemeResource("img/checkmark.png"));
        myThemesTable.addItem(new Object[]{isDefault ? checkmark : null, getBundleString("addonsConfigPanel.themes.defname"), createTableRowButton("button.default", this, DEFAULT_UI_THEME_ID, "DefaultTheme", !isDefault), createTableRowButton("button.delete", this, DEFAULT_UI_THEME_ID, "DeleteTheme", false), createTableRowButton("button.export", this, DEFAULT_UI_THEME_ID, "ExportTheme")}, DEFAULT_UI_THEME_ID);
        for (ThemeDefinition theme : themes) {
            isDefault = StringUtils.equals(MyTunesRss.CONFIG.getDefaultUserInterfaceTheme(), theme.getName());
            myThemesTable.addItem(new Object[] {
                    isDefault ? checkmark : null,
                    theme.getName(),
                    createTableRowButton("button.default", this, theme.getName(), "DefaultTheme", !isDefault),
                    createTableRowButton("button.delete", this, theme.getName(), "DeleteTheme", !isDefault),
                    createTableRowButton("button.export", this, theme.getName(), "ExportTheme")
            },
            theme.getName());
        }
    }

    private void addSite(ExternalSiteDefinition site) {
        SmartTextField name = new SmartTextField(null, site.getName());
        name.setImmediate(true);
        name.addValidator(new SiteValidator("name", "type"));
        Select type = getComponentFactory().createSelect(null, Arrays.asList("album", "artist", "title"));
        type.setValue(site.getType());
        type.setImmediate(true);
        type.addValidator(new SiteValidator("type", "name"));
        SmartTextField url = new SmartTextField(null, site.getUrl());
        url.setWidth(100, UNITS_PERCENTAGE);
        url.setImmediate(true);
        url.addValidator(new AbstractStringValidator(getBundleString("addonsConfigPanel.error.invalidSiteUrl")) {
            @Override
            protected boolean isValidString(String s) {
                if (s.contains("{KEYWORD}")) {
                    try {
                        new URL(s.replace("{KEYWORD}", "dummy"));
                        return true;
                    } catch (MalformedURLException e) {
                        LOGGER.info("Invalid site definition.", e);
                        // ignore, will return false in the end
                    }
                }
                return false;
            }
        });
        Object itemId = UUID.randomUUID().toString();
        mySitesTable.addItem(new Object[]{name, type, url, createTableRowButton("button.delete", this, itemId, "DeleteSite")}, itemId);
        setTablePageLengths();
    }

    private void setTablePageLengths() {
        myThemesTable.setPageLength(Math.min(myThemesTable.getItemIds().size(), 10));
        myLanguagesTable.setPageLength(Math.min(myLanguagesTable.getItemIds().size(), 10));
        mySitesTable.setPageLength(Math.min(mySitesTable.getItemIds().size(), 10));
        myFlashPlayersTable.setPageLength(Math.min(myFlashPlayersTable.getItemIds().size(), 10));
    }

    @Override
    protected void writeToConfig() {
        for (ExternalSiteDefinition site : MyTunesRss.CONFIG.getExternalSites()) {
            MyTunesRss.CONFIG.removeExternalSite(site);
        }
        for (Object itemId : mySitesTable.getItemIds()) {
            MyTunesRss.CONFIG.addExternalSite(new ExternalSiteDefinition((String) getTableCellPropertyValue(mySitesTable, itemId, "type"), (String) getTableCellPropertyValue(mySitesTable, itemId, "name"), (String) getTableCellPropertyValue(mySitesTable, itemId, "url")));
        }
        MyTunesRss.CONFIG.save();
    }

    @Override
    protected boolean beforeSave() {
        if (!VaadinUtils.isValid(mySitesTable)) {
            ((MainWindow) VaadinUtils.getApplicationWindow(this)).showError("error.formInvalid");
            return false;
        }
        return true;
    }

    @Override
    public void buttonClick(final Button.ClickEvent clickEvent) {
        if (clickEvent.getSource() instanceof TableRowButton) {
            final TableRowButton tableRowButton = (TableRowButton) clickEvent.getSource();
            if ("EditPlayer".equals(tableRowButton.getData())) {
                FlashPlayerEditPanel flashPlayerEditPanel = new FlashPlayerEditPanel(this, MyTunesRss.CONFIG.getFlashPlayer((String) tableRowButton.getItemId()));
                SinglePanelWindow flashPlayerEditWindow = new SinglePanelWindow(50, Sizeable.UNITS_EM, null, getBundleString("flashPlayerEditPanel.caption"), flashPlayerEditPanel);
                flashPlayerEditWindow.show(getWindow());
            } else if ("DownloadPlayer".equals(tableRowButton.getData())) {
                FlashPlayerConfig flashPlayerConfig = MyTunesRss.CONFIG.getFlashPlayer((String) tableRowButton.getItemId());
                try {
                    sendJukebox(flashPlayerConfig);
                } catch (IOException e) {
                    ((MainWindow) VaadinUtils.getApplicationWindow(this)).showError("addonsConfigPanel.error.jukeboxDownloadFailed", e.getMessage());
                }
            } else if ("DefaultTheme".equals(tableRowButton.getData())) {
                String name = tableRowButton.getItemId() == DEFAULT_UI_THEME_ID ? null : tableRowButton.getItem().getItemProperty("name").getValue().toString();
                MyTunesRss.CONFIG.setDefaultUserInterfaceTheme(name);
                refreshThemes();
            } else if ("EditLanguage".equals(tableRowButton.getData())) {
                String lc = tableRowButton.getItemId().toString();
                ((MainWindow) VaadinUtils.getApplicationWindow(this)).showComponent(new EditLanguagePanel(this, LanguageDefinition.getLocaleFromCode(lc)));
            } else if ("ExportLanguage".equals(tableRowButton.getData())) {
                String code = tableRowButton.getItemId().toString();
                sendLanguageFile(AddonsUtils.getUserLanguageFile(LanguageDefinition.getLocaleFromCode(code)));
            } else if ("ExportTheme".equals(tableRowButton.getData())) {
                String name = tableRowButton.getItemId() == DEFAULT_UI_THEME_ID ? null : tableRowButton.getItem().getItemProperty("name").getValue().toString();
                sendThemeFile(name);
            } else {
                final Button yes = new Button(getBundleString("button.yes"));
                Button no = new Button(getBundleString("button.no"));
                new OptionWindow(30, Sizeable.UNITS_EM, null, getBundleString("addonsConfigPanel.optionWindow" + tableRowButton.getData() + ".caption"), getBundleString("addonsConfigPanel.optionWindow" + tableRowButton.getData() + ".message", tableRowButton.getItem().getItemProperty("name").getValue().toString()), yes, no) {
                    @Override
                    public void clicked(Button button) {
                        if (button == yes) {
                            if ("DeleteTheme".equals(tableRowButton.getData().toString())) {
                                AddonsUtils.deleteTheme(tableRowButton.getItem().getItemProperty("name").getValue().toString());
                            } else if ("DeleteLanguage".equals(tableRowButton.getData().toString())) {
                                AddonsUtils.deleteLanguage(tableRowButton.getItemId().toString());
                            } else if ("DeletePlayer".equals(tableRowButton.getData().toString())) {
                                FlashPlayerConfig removedConfig = MyTunesRss.CONFIG.removeFlashPlayer((String) tableRowButton.getItemId());
                                if (removedConfig != null) {
                                    FileUtils.deleteQuietly(removedConfig.getBaseDir());
                                }
                            }
                            tableRowButton.deleteTableRow();
                            setTablePageLengths();
                        }
                    }
                }.show(getWindow());
            }
        } else if (clickEvent.getSource() == myAddSite) {
            addSite(new ExternalSiteDefinition("album", "new site", "http://"));
            setTablePageLengths();
        } else if (clickEvent.getSource() == myRestoreDefaultJukeboxes) {
            for (FlashPlayerConfig defaultConfig : FlashPlayerConfig.getDefaults()) {
                MyTunesRss.CONFIG.removeFlashPlayer(defaultConfig.getId());
                MyTunesRss.CONFIG.addFlashPlayer(defaultConfig);
            }
            refreshFlashPlayers();
            setTablePageLengths();
            ((MainWindow) VaadinUtils.getApplicationWindow(this)).showInfo("addonsConfigPanel.info.defaultJukeboxesRestored");
        } else if (clickEvent.getSource() == myAddFlashPlayer) {
            FlashPlayerEditPanel flashPlayerEditPanel = new FlashPlayerEditPanel(this, new FlashPlayerConfig(UUID.randomUUID().toString(), "", PlaylistFileType.Xspf, 640, 480, TimeUnit.SECONDS, 256));
            SinglePanelWindow flashPlayerEditWindow = new SinglePanelWindow(50, Sizeable.UNITS_EM, null, getBundleString("flashPlayerEditPanel.caption"), flashPlayerEditPanel);
            flashPlayerEditWindow.show(getWindow());
        } else if (clickEvent.getSource() == myAddLanguage) {
            List<LocaleRepresentation> localeRepresentations = new ArrayList<>();
            for (Locale locale : Locale.getAvailableLocales()) {
                if (AddonsUtils.getUserLanguageFile(locale) == null) {
                    localeRepresentations.add(new LocaleRepresentation(locale));
                }
            }
            Collections.sort(localeRepresentations, new Comparator<LocaleRepresentation>() {
                @Override
                public int compare(LocaleRepresentation o1, LocaleRepresentation o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            });
            new SelectWindow<LocaleRepresentation>(50, Sizeable.UNITS_EM, localeRepresentations, localeRepresentations.get(0), null, getBundleString("addonsConfigPanel.selectAddNewLanguage.caption"), getBundleString("addonsConfigPanel.selectAddNewLanguage.caption"), getBundleString("addonsConfigPanel.selectAddNewLanguage.buttonCreate"), getBundleString("button.cancel")) {
                @Override
                protected void onOk(LocaleRepresentation representation) {
                    getParent().removeWindow(this);
                    ((MainWindow) VaadinUtils.getApplicationWindow(AddonsConfigPanel.this)).showComponent(new EditLanguagePanel(AddonsConfigPanel.this, representation.getLocale()));
                }
            }.show(getWindow());
        } else {
            super.buttonClick(clickEvent);
        }
    }

    private void sendLanguageFile(File languageFile) {
        LOGGER.debug("Compressing and sending language file \"" + languageFile.getAbsolutePath() + "\".");
        String baseName = FilenameUtils.getBaseName(languageFile.getName());
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipArchiveOutputStream zos = new ZipArchiveOutputStream(baos);
        try {
            zos.putArchiveEntry(new ZipArchiveEntry(baseName + ".properties"));
            IOUtils.copy(new FileInputStream(languageFile), zos);
            zos.closeArchiveEntry();
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not find language file \"" + languageFile.getName() + "\".", e);
        } catch (IOException e) {
            LOGGER.error("Could not create zip archive for language file \"" + languageFile.getName() + "\".", e);
        } finally {
            try {
                zos.close();
            } catch (IOException e) {
                LOGGER.error("Could not close zip output stream.", e);
            }
        }
        Resource streamResource = new StreamResource(new StreamResource.StreamSource() {
            @Override
            public InputStream getStream() {
                return new ByteArrayInputStream(baos.toByteArray());
            }
        }, baseName + ".zip", getApplication());
        getWindow().open(streamResource);
    }

    private void sendJukebox(FlashPlayerConfig flashPlayerConfig) throws IOException {
        File jukeboxDir = new File(MyTunesRss.PREFERENCES_DATA_PATH + "/flashplayer/" + flashPlayerConfig.getId());
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(baos)) {
            ZipUtils.addFilesToZipRecursively("", jukeboxDir, null, zipOutputStream);
        }
        Resource streamResource = new StreamResource(new StreamResource.StreamSource() {
            @Override
            public InputStream getStream() {
                return new ByteArrayInputStream(baos.toByteArray());
            }
        }, flashPlayerConfig.getName() + ".zip", getApplication());
        getWindow().open(streamResource);
    }

    private void sendThemeFile(String theme) {
        LOGGER.debug("Compressing and sending theme \"" + StringUtils.defaultString(theme, "MYTUNESRSS_DEFAULT") + "\".");
        File themeDir = null;
        try {
            themeDir = getThemeDir(theme);
        } catch (IOException e) {
            LOGGER.error("Could not find theme dir for \"" + StringUtils.defaultString(theme, "MYTUNESRSS_DEFAULT") + "\".", e);
            ((MainWindow) VaadinUtils.getApplicationWindow(this)).showError("addonsConfigPanel.error.couldNotExportTheme");
            return;
        }
        final File finalThemeDir = themeDir;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ZipArchiveOutputStream zos = new ZipArchiveOutputStream(baos);
        final AtomicBoolean error = new AtomicBoolean();
        try {
            de.codewave.utils.io.IOUtils.processFiles(themeDir, new FileProcessor() {
                @Override
                public void process(File file) {
                    if (!error.get() && file.isFile()) {
                        try {
                            String relativePath = getRelativePath(finalThemeDir, file);
                            if (relativePath != null) {
                                zos.putArchiveEntry(new ZipArchiveEntry(relativePath));
                                IOUtils.copy(new FileInputStream(file), zos);
                                zos.closeArchiveEntry();
                            } else {
                                error.set(true);
                            }
                        } catch (IOException e) {
                            LOGGER.error("Could not add file \"" + file.getName() + "\" to archive.", e);
                            error.set(true);
                        }
                    }
                }
            }, new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return "images".equals(file.getName()) || "styles".equals(file.getName()) || "images".equals(file.getParentFile().getName()) || "styles".equals(file.getParentFile().getName());
                }
            });
        } finally {
            try {
                zos.close();
            } catch (IOException e) {
                LOGGER.error("Could not close zip output stream.", e);
            }
        }
        if (!error.get()) {
            Resource streamResource = new StreamResource(new StreamResource.StreamSource() {
                @Override
                public InputStream getStream() {
                    return new ByteArrayInputStream(baos.toByteArray());
                }
            }, "MyTunesRSS_" + StringUtils.defaultString(theme, "DEFAULT_THEME") + ".zip", getApplication());
            getWindow().open(streamResource);
        } else {
            ((MainWindow) VaadinUtils.getApplicationWindow(this)).showError("addonsConfigPanel.error.couldNotExportTheme");
        }
    }

    private String getRelativePath(File dir, File file) {
        try {
            String dirName = dir.getCanonicalPath();
            String fileName = file.getCanonicalPath();
            if (fileName.startsWith(dirName)) {
                String relativePath = fileName.substring(dirName.length());
                return StringUtils.stripStart(relativePath, "/\\");
            }
        } catch (IOException e) {
            LOGGER.error("Could not get canonical path.", e);
        }
        return null;
    }

    private File getThemeDir(String name) throws IOException {
        File file;
        if (name == null) {
            file = new File(getApplication().getContext().getBaseDirectory().getParentFile(), "ROOT");
        } else {
            file = new File(MyTunesRss.PREFERENCES_DATA_PATH + "/themes/" + name);
        }
        if (file.exists()) {
            // addon theme found
            return file;
        }
        throw new IOException("Could not find theme dir for \"" + StringUtils.defaultString(name, "MYTUNESRSS_DEFAULT") + "\".");
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        try {
            return new FileOutputStream(new File(getUploadDir(), PREFIX + filename));
        } catch (IOException e) {
            throw new RuntimeException("Could not receive upload.", e);
        }
    }

    @Override
    public void uploadFailed(Upload.FailedEvent event) {
        ((MainWindow) VaadinUtils.getApplicationWindow(this)).showError("addonsConfigPanel.error.uploadFailed");
        FileUtils.deleteQuietly(new File(getUploadDir(), PREFIX + event.getFilename()));
    }

    @Override
    public void uploadSucceeded(Upload.SucceededEvent event) {
        if (event.getSource() == myUploadTheme) {
            AddonsUtils.AddFileResult result = AddonsUtils.addTheme(FilenameUtils.getBaseName(event.getFilename()), new File(getUploadDir(), PREFIX + event.getFilename()));
            if (result == AddonsUtils.AddFileResult.InvalidFile) {
                ((MainWindow) VaadinUtils.getApplicationWindow(this)).showError("addonsConfigPanel.error.invalidTheme");
            } else if (result == AddonsUtils.AddFileResult.ExtractFailed) {
                ((MainWindow) VaadinUtils.getApplicationWindow(this)).showError("addonsConfigPanel.error.extractFailed");
            } else {
                ((MainWindow) VaadinUtils.getApplicationWindow(this)).showInfo("addonsConfigPanel.info.themeOk");
                refreshThemes();
            }
        } else {
            AddonsUtils.AddFileResult result = AddonsUtils.addLanguage(new File(getUploadDir(), PREFIX + event.getFilename()), event.getFilename());
            if (result == AddonsUtils.AddFileResult.InvalidFile) {
                ((MainWindow) VaadinUtils.getApplicationWindow(this)).showError("addonsConfigPanel.error.invalidLanguage");
            } else if (result == AddonsUtils.AddFileResult.ExtractFailed) {
                ((MainWindow) VaadinUtils.getApplicationWindow(this)).showError("addonsConfigPanel.error.extractFailed");
            } else if (result == AddonsUtils.AddFileResult.SaveFailed) {
                ((MainWindow) VaadinUtils.getApplicationWindow(this)).showError("addonsConfigPanel.error.saveFailed");
            } else {
                ((MainWindow) VaadinUtils.getApplicationWindow(this)).showInfo("addonsConfigPanel.info.languageOk");
                refreshLanguages();
            }
        }
        FileUtils.deleteQuietly(new File(getUploadDir(), PREFIX + event.getFilename()));
        setTablePageLengths();
    }

    void saveFlashPlayer(FlashPlayerConfig flashPlayerConfig) {
        if (MyTunesRss.CONFIG.getFlashPlayer(flashPlayerConfig.getName()) == null) {
            MyTunesRss.CONFIG.addFlashPlayer(flashPlayerConfig);
        }
        refreshFlashPlayers();
        setTablePageLengths();
    }

    public class SiteValidator extends AbstractStringValidator {

        private String myProperty;
        private String myOtherProperty;

        public SiteValidator(String property, String otherProperty) {
            super(getBundleString("addonsConfigPanel.error.duplicateSite"));
            myProperty = property;
            myOtherProperty = otherProperty;
        }

        @Override
        protected boolean isValidString(String s) {
            Set<String> others = new HashSet<>();
            for (Object itemId : mySitesTable.getItemIds()) {
                if (StringUtils.equals((CharSequence) getTableCellPropertyValue(mySitesTable, itemId, myProperty), s)) {
                    String other = (String) getTableCellPropertyValue(mySitesTable, itemId, myOtherProperty);
                    if (others.contains(other)) {
                        return false;
                    }
                    others.add(other);
                }
            }
            return true;
        }
    }

    private File getUploadDir() {
        return MyTunesRss.TEMP_CACHE.getBaseDir();
    }

    /**
     * Representation class for select UI element.
     */
    private class LocaleRepresentation {
        private Locale myLocale;

        private LocaleRepresentation(Locale locale) {
            myLocale = locale;
        }

        public Locale getLocale() {
            return myLocale;
        }

        @Override
        public String toString() {
            return myLocale.getDisplayName(getApplication().getLocale());
        }
    }
}
