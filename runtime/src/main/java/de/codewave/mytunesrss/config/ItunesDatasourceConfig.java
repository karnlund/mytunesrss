/*
 * Copyright (c) 2010. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.config;

import de.codewave.mytunesrss.ImageImportType;
import de.codewave.mytunesrss.datastore.itunes.ItunesLoader;
import de.codewave.mytunesrss.datastore.itunes.ItunesPlaylistType;
import de.codewave.utils.xml.PListHandler;
import de.codewave.utils.xml.PListHandlerListener;
import de.codewave.utils.xml.XmlUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class ItunesDatasourceConfig extends DatasourceConfig implements CommonTrackDatasourceConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItunesDatasourceConfig.class);

    private static final String[] AUTO_ADD_NAMES = new String[] {
            "Automatically Add to iTunes",
            "Automatically Add to iTunes.localized"
    };

    private static class StopParsingException extends RuntimeException {
        // Exception thrown when the parser requests to stop parsing
    }

    private static class MusicFolderListener implements PListHandlerListener {
        private String myMusicFolder;

        @Override
        public boolean beforeDictPut(Map dict, String key, Object value) {
            if ("Music Folder".equals(key)) {
                myMusicFolder = value.toString();
                throw new StopParsingException();
            }
            return true;
        }

        @Override
        public boolean beforeArrayAdd(List array, Object value) {
            return false;
        }

        public String getMusicFolder() {
            return myMusicFolder;
        }
    }

    private Set<ReplacementRule> myPathReplacements = new HashSet<>();
    private Set<ItunesPlaylistType> myIgnorePlaylists = new HashSet<>();
    private String myArtistDropWords;
    private String myDisabledMp4Codecs = "";
    private List<String> myTrackImagePatterns = new ArrayList<>();
    private ImageImportType myTrackImageImportType = ImageImportType.Auto;
    private String myMusicFolderFilename;
    private boolean myUseSingleImageInFolder;

    public ItunesDatasourceConfig(String id, String name, String definition) {
        super(id, StringUtils.defaultIfBlank(name, "iTunes"), definition);
        myMusicFolderFilename = extractMusicFolderFilename();
    }

    public ItunesDatasourceConfig(ItunesDatasourceConfig source) {
        super(source);
        myPathReplacements = new HashSet<>(source.getPathReplacements());
        myIgnorePlaylists = new HashSet<>(source.getIgnorePlaylists());
        myArtistDropWords = source.getArtistDropWords();
        myDisabledMp4Codecs = source.getDisabledMp4Codecs();
        myTrackImagePatterns = new ArrayList<>(source.getTrackImagePatterns());
        myTrackImageImportType = source.getTrackImageImportType();
        myMusicFolderFilename = extractMusicFolderFilename();
        myUseSingleImageInFolder = source.isUseSingleImageInFolder();
    }

    @Override
    public DatasourceType getType() {
        return DatasourceType.Itunes;
    }

    public Set<ReplacementRule> getPathReplacements() {
        return new HashSet<>(myPathReplacements);
    }

    public void clearPathReplacements() {
        myPathReplacements.clear();
    }

    public void addPathReplacement(ReplacementRule pathReplacement) {
        myPathReplacements.add(pathReplacement);
    }

    public Set<ItunesPlaylistType> getIgnorePlaylists() {
        return new HashSet<>(myIgnorePlaylists);
    }

    public void addIgnorePlaylist(ItunesPlaylistType type) {
        myIgnorePlaylists.add(type);
    }

    public void removeIgnorePlaylist(ItunesPlaylistType type) {
        myIgnorePlaylists.remove(type);
    }

    public void clearIgnorePlaylists() {
        myIgnorePlaylists.clear();
    }

    @Override
    public String getArtistDropWords() {
        return myArtistDropWords;
    }

    @Override
    public void setArtistDropWords(String artistDropWords) {
        myArtistDropWords = artistDropWords;
    }

    @Override
    public String getDisabledMp4Codecs() {
        return myDisabledMp4Codecs;
    }

    @Override
    public void setDisabledMp4Codecs(String disabledMp4Codecs) {
        myDisabledMp4Codecs = disabledMp4Codecs;
    }

    @Override
    public List<String> getTrackImagePatterns() {
        return new ArrayList<>(myTrackImagePatterns);
    }

    @Override
    public void setTrackImagePatterns(List<String> trackImageMappings) {
        this.myTrackImagePatterns = new ArrayList<>(trackImageMappings);
    }

    @Override
    public ImageImportType getTrackImageImportType() {
        return myTrackImageImportType;
    }

    @Override
    public void setTrackImageImportType(ImageImportType trackImageImportType) {
        myTrackImageImportType = trackImageImportType;
    }

    public File getAutoAddToItunesFolder() {
        List<CompiledReplacementRule> pathReplacements = new ArrayList<>();
        for (ReplacementRule pathReplacement : getPathReplacements()) {
            pathReplacements.add(new CompiledReplacementRule(pathReplacement));
        }
        String musicFolderFilename = myMusicFolderFilename;
        for (CompiledReplacementRule pathReplacement : pathReplacements) {
            if (pathReplacement.matches(musicFolderFilename)) {
                musicFolderFilename = pathReplacement.replace(musicFolderFilename);
                break;
            }
        }
        for (String name : AUTO_ADD_NAMES) {
            File file = new File(musicFolderFilename, name);
            if (file.isDirectory()) {
                LOGGER.debug("Found iTunes auto-add folder \"" + file.getAbsolutePath() + "\".");
                return file;
            }
        }
        LOGGER.debug("Could not find iTunes auto-add folder.");
        return null;
    }

    /**
     * Get the "Automatically add to iTunes" folder.
     *
     * @return The file representing the auto-add folder or NULL if no such folder could be found.
     */
    private String extractMusicFolderFilename() {
        PListHandler handler = new PListHandler();
        MusicFolderListener listener = new MusicFolderListener();
        handler.addListener("/plist/dict", listener);
        try {
            LOGGER.debug("Parsing iTunes XML to find music folder.");
            XmlUtils.parseApplePList(new File(getDefinition()).toURI().toURL(), handler);
            LOGGER.debug("Finished parsing iTunes XML without stop-exception!");
        } catch (StopParsingException ignored) {
            LOGGER.debug("Finished parsing iTunes XML with stop-exception!");
        } catch (Exception e) {
            LOGGER.warn("Could not find iTunes auto-add folder.", e);
            return null;
        }
        return ItunesLoader.getFileNameForLocation(listener.getMusicFolder());
    }

    @Override
    public boolean isUploadable() {
        return getAutoAddToItunesFolder() != null;
    }

    @Override
    public boolean isUseSingleImageInFolder() {
        return myUseSingleImageInFolder;
    }

    public void setUseSingleImageInFolder(boolean useSingleImageInFolder) {
        myUseSingleImageInFolder = useSingleImageInFolder;
    }
}
