package de.codewave.mytunesrss.datastore.itunes;

import de.codewave.camel.mp4.CodecAtom;
import de.codewave.camel.mp4.Mp4Atom;
import de.codewave.mytunesrss.*;
import de.codewave.mytunesrss.config.*;
import de.codewave.mytunesrss.datastore.statement.InsertOrUpdateTrackStatement;
import de.codewave.mytunesrss.datastore.statement.InsertTrackStatement;
import de.codewave.mytunesrss.datastore.statement.TrackSource;
import de.codewave.mytunesrss.datastore.statement.UpdateTrackStatement;
import de.codewave.mytunesrss.datastore.updatequeue.DataStoreStatementEvent;
import de.codewave.mytunesrss.datastore.updatequeue.DatabaseUpdateQueue;
import de.codewave.utils.xml.PListHandlerListener;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * de.codewave.mytunesrss.datastore.itunes.TrackListenerr
 */
public class TrackListener implements PListHandlerListener {
    private static final Logger LOG = LoggerFactory.getLogger(TrackListener.class);
    private static final String MP4_CODEC_NOT_CHECKED = new String();

    private DatabaseUpdateQueue myQueue;
    private LibraryListener myLibraryListener;
    private int myUpdatedCount;
    private Map<Long, String> myTrackIdToPersId;
    private Collection<String> myTrackIds;
    private long myMissingFiles;
    private String[] myDisabledMp4Codecs;
    private Thread myWatchdogThread;
    private Set<CompiledReplacementRule> myPathReplacements;
    private ItunesDatasourceConfig myDatasourceConfig;

    public TrackListener(ItunesDatasourceConfig datasourceConfig, Thread watchdogThread, DatabaseUpdateQueue queue, LibraryListener libraryListener, Map<Long, String> trackIdToPersId,
                         Collection<String> trackIds) throws SQLException {
        myDatasourceConfig = datasourceConfig;
        myWatchdogThread = watchdogThread;
        myQueue = queue;
        myLibraryListener = libraryListener;
        myTrackIdToPersId = trackIdToPersId;
        myTrackIds = trackIds;
        myDisabledMp4Codecs = StringUtils.split(StringUtils.lowerCase(StringUtils.trimToEmpty(MyTunesRss.CONFIG.getDisabledMp4Codecs())), ",");
        myPathReplacements = new HashSet<CompiledReplacementRule>();
        for (ReplacementRule pathReplacement : myDatasourceConfig.getPathReplacements()) {
            myPathReplacements.add(new CompiledReplacementRule(pathReplacement));
        }
    }

    public int getUpdatedCount() {
        return myUpdatedCount;
    }

    public long getMissingFiles() {
        return myMissingFiles;
    }

    public boolean beforeDictPut(Map dict, String key, Object value) {
        Map track = (Map) value;
        String trackId = calculateTrackId(track);
        try {
            if (processTrack(track, myTrackIds.remove(trackId))) {
                myUpdatedCount++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private String calculateTrackId(Map track) {
        String trackId = myLibraryListener.getLibraryId() + "_";
        trackId += track.get("Persistent ID") != null ? track.get("Persistent ID").toString() : "TrackID" + track.get("Track ID").toString();
        return trackId;
    }

    public boolean beforeArrayAdd(List array, Object value) {
        throw new UnsupportedOperationException("method beforeArrayAdd of class ItunesLoader$TrackListener is not supported!");
    }

    private boolean processTrack(Map track, boolean existing) throws InterruptedException {
        if (myWatchdogThread.isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new ShutdownRequestedException();
        }
        String trackId = calculateTrackId(track);
        String name = (String) track.get("Name");
        String trackType = (String) track.get("Track Type");
        if (trackType == null || "File".equals(trackType)) {
            String filename = ItunesLoader.getFileNameForLocation(applyReplacements((String) track.get("Location")));
            if (StringUtils.isNotBlank(filename)) {
                String mp4Codec = getMp4Codec(track, filename, myLibraryListener.getTimeLastUpate());
                if (trackId != null && StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(filename) && FileSupportUtils.isSupported(filename) && !isMp4CodecDisabled(mp4Codec)) {
                    if (!new File(filename).isFile()) {
                        myMissingFiles++;
                    }
                    if (!myDatasourceConfig.isDeleteMissingFiles() || new File(filename).isFile()) {
                        Date dateModified = ((Date) track.get("Date Modified"));
                        long dateModifiedTime = dateModified != null ? dateModified.getTime() : Long.MIN_VALUE;
                        Date dateAdded = ((Date) track.get("Date Added"));
                        long dateAddedTime = dateAdded != null ? dateAdded.getTime() : Long.MIN_VALUE;
                        if (!existing || dateModifiedTime >= myLibraryListener.getTimeLastUpate() || dateAddedTime >= myLibraryListener.getTimeLastUpate()) {
                            InsertOrUpdateTrackStatement statement =
                                    existing ? new UpdateTrackStatement(TrackSource.ITunes) : new InsertTrackStatement(TrackSource.ITunes);
                            statement.clear();
                            statement.setId(trackId);
                            statement.setName(MyTunesRssUtils.normalize(name.trim()));
                            String artist = MyTunesRssUtils.normalize(StringUtils.trimToNull((String) track.get("Artist")));
                            statement.setArtist(artist);
                            String albumArtist = MyTunesRssUtils.normalize(StringUtils.trimToNull(StringUtils.defaultIfEmpty((String) track.get("Album Artist"), (String) track.get("Artist"))));
                            statement.setAlbumArtist(albumArtist);
                            statement.setAlbum(MyTunesRssUtils.normalize(StringUtils.trimToNull((String) track.get("Album"))));
                            statement.setTime((int) (track.get("Total Time") != null ? (Long) track.get("Total Time") / 1000 : 0));
                            statement.setTrackNumber((int) (track.get("Track Number") != null ? (Long) track.get("Track Number") : 0));
                            statement.setFileName(filename);
                            statement.setProtected(FileSupportUtils.isProtected(filename));
                            boolean video = track.get("Has Video") != null && ((Boolean) track.get("Has Video")).booleanValue();
                            statement.setMediaType(video ? MediaType.Video : MediaType.Audio);
                            if (video) {
                                statement.setAlbum(null);
                                statement.setArtist(null);
                                boolean tvshow = track.get("TV Show") != null && ((Boolean) track.get("TV Show")).booleanValue();
                                statement.setVideoType(tvshow ? VideoType.TvShow : VideoType.Movie);
                                if (tvshow) {
                                    statement.setSeries(MyTunesRssUtils.normalize(StringUtils.trimToNull((String) track.get("Series"))));
                                    statement.setSeason((int) (track.get("Season") != null ? (Long) track.get("Season") : 0));
                                    statement.setEpisode((int) (track.get("Episode Order") != null ? (Long) track.get("Episode Order") : 0));
                                }
                            }
                            statement.setGenre(StringUtils.trimToNull((String) track.get("Genre")));
                            statement.setComposer(StringUtils.trimToNull((String) track.get("Composer")));
                            boolean compilation = track.get("Compilation") != null && ((Boolean) track.get("Compilation")).booleanValue();
                            statement.setCompilation(compilation || !StringUtils.equalsIgnoreCase(artist, albumArtist));
                            statement.setComment(MyTunesRssUtils.normalize(StringUtils.trimToNull((String) track.get("Comments"))));
                            statement.setPos((int) (track.get("Disc Number") != null ? ((Long) track.get("Disc Number")).longValue() : 0),
                                    (int) (track.get("Disc Count") != null ? ((Long) track.get("Disc Count")).longValue() : 0));
                            statement.setYear(track.get("Year") != null ? ((Long) track.get("Year")).intValue() : -1);
                            statement.setMp4Codec(mp4Codec == MP4_CODEC_NOT_CHECKED ? getMp4Codec(track, filename, 0) : mp4Codec);
                            myQueue.offer(new DataStoreStatementEvent(statement, true, "Could not insert track \"" + name + "\" into database."));
                            myTrackIdToPersId.put((Long) track.get("Track ID"), trackId);
                            return true;
                        } else if (existing) {
                            myTrackIdToPersId.put((Long) track.get("Track ID"), trackId);
                        }
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private String applyReplacements(String originalFileName) {
        for (CompiledReplacementRule pathReplacement : myPathReplacements) {
            if (pathReplacement.matches(originalFileName)) {
                return pathReplacement.replace(originalFileName);
            }
        }
        return originalFileName;
    }

    private boolean isMp4CodecDisabled(String mp4Codec) {
        return mp4Codec != null && ArrayUtils.contains(myDisabledMp4Codecs, mp4Codec.toLowerCase());
    }

    private String getMp4Codec(Map track, String filename, long lastUpdateTime) {
        if (new File(filename).lastModified() < lastUpdateTime) {
            return MP4_CODEC_NOT_CHECKED;
        }
        if (FileSupportUtils.isMp4(filename)) {
            String kind = (String) track.get("Kind");
            if (StringUtils.isNotEmpty(kind)) {
                kind = kind.toLowerCase();
                if (kind.contains("aac")) {
                    return "mp4a";
                } else if (kind.contains("apple lossless")) {
                    return "alac";
                } else {
                    File file = new File(filename);
                    if (file.exists()) {
                        return getMp4Codec(file);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the MP4 codec of the specified MP4 file.
     *
     * @param file The file.
     * @return The MP4 codec used in the file.
     */
    private String getMp4Codec(File file) {
        Map<String, Mp4Atom> atoms = null;
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Reading ATOM information from file \"" + file.getAbsolutePath() + "\".");
            }
            CodecAtom atom = (CodecAtom)MyTunesRss.MP4_PARSER.parseAndGet(file, "moov.trak.mdia.minf.stbl.stsd");
            if (atom != null) {
                return atom.getCodec();
            }
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Could not get ATOM information from file \"" + file.getAbsolutePath() + "\".", e);
            }
        }
        return null;
    }
}
