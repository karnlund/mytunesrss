package de.codewave.mytunesrss.datastore.filesystem;

import de.codewave.mytunesrss.*;
import de.codewave.mytunesrss.datastore.statement.DeleteTrackStatement;
import de.codewave.mytunesrss.datastore.statement.FindTrackIdsQuery;
import de.codewave.mytunesrss.datastore.statement.TrackSource;
import de.codewave.utils.io.FileProcessor;
import de.codewave.utils.io.IOUtils;
import de.codewave.utils.sql.DataStoreSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * de.codewave.mytunesrss.datastore.filesystem.FileSystemLoaderr
 */
public class FileSystemLoader {
    private static final Log LOG = LogFactory.getLog(FileSystemLoader.class);

    public static void loadFromFileSystem(List<File> baseDirs, DataStoreSession storeSession, long lastUpdateTime)
        throws IOException, SQLException {
        Set<String> databaseIds = (Set<String>)storeSession.executeQuery(new FindTrackIdsQuery(TrackSource.FileSystem.name()));
        int trackCount = 0;
        MyTunesRssFileProcessor fileProcessor = null;
        if (baseDirs != null) {
            for (File baseDir : baseDirs) {
                if (baseDir != null && baseDir.exists() && baseDir.isDirectory()) {
                    fileProcessor = new MyTunesRssFileProcessor(baseDir, storeSession, lastUpdateTime);
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Processing files from: \"" + baseDir + "\".");
                    }
                    IOUtils.processFiles(baseDir, fileProcessor, new FileFilter() {
                        public boolean accept(File file) {
                            return file.isDirectory() || FileSupportUtils.isSupported(file.getName());
                        }
                    });
                    FileProcessor playlistFileProcessor = new PlaylistFileProcessor(baseDir, storeSession);
                    IOUtils.processFiles(baseDir, playlistFileProcessor, new FileFilter() {
                        public boolean accept(File file) {
                            return file.isDirectory() || file.getName().toLowerCase().endsWith(".m3u");
                        }
                    });
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Inserted/updated " + fileProcessor.getUpdatedCount() + " file system tracks.");
                    }
                    databaseIds.removeAll(fileProcessor.getExistingIds());
                    trackCount += fileProcessor.getExistingIds().size();
                }
            }
        }
        if (!databaseIds.isEmpty()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Removing " + databaseIds.size() + " obsolete file system tracks.");
            }
            DeleteTrackStatement deleteTrackStatement = new DeleteTrackStatement(storeSession);
            int count = 0;
            for (String id : databaseIds) {
                deleteTrackStatement.setId(id);
                storeSession.executeStatement(deleteTrackStatement);
                count++;
                if (count == 5000) {
                    count = 0;
                    storeSession.commitAndContinue();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Committing transaction after 5000 deleted tracks.");
                    }
                }
            }
        }
        if (fileProcessor != null && LOG.isDebugEnabled()) {
            LOG.info(trackCount + " file system tracks in the database.");
        }
    }
}