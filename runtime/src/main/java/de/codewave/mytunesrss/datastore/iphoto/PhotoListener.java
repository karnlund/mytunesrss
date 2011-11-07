/*
 * Copyright (c) 2011. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.datastore.iphoto;

import de.codewave.mytunesrss.*;
import de.codewave.mytunesrss.datastore.statement.HandlePhotoImagesStatement;
import de.codewave.mytunesrss.datastore.statement.InsertOrUpdatePhotoStatement;
import de.codewave.mytunesrss.datastore.statement.InsertPhotoStatement;
import de.codewave.mytunesrss.datastore.statement.UpdatePhotoStatement;
import de.codewave.mytunesrss.datastore.updatequeue.DataStoreStatementEvent;
import de.codewave.mytunesrss.datastore.updatequeue.DatabaseUpdateQueue;
import de.codewave.mytunesrss.task.DatabaseBuilderCallable;
import de.codewave.utils.sql.DataStoreSession;
import de.codewave.utils.xml.PListHandlerListener;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

/**
 * de.codewave.mytunesrss.datastore.itunes.TrackListenerr
 */
public class PhotoListener implements PListHandlerListener {
    private static final Logger LOG = LoggerFactory.getLogger(PhotoListener.class);

    private DatabaseUpdateQueue myQueue;
    private LibraryListener myLibraryListener;
    private int myUpdatedCount;
    private Map<Long, String> myPhotoIdToPersId;
    private Collection<String> myPhotoIds;
    private Thread myWatchdogThread;
    private Set<CompiledReplacementRule> myPathReplacements;
    private IphotoDatasourceConfig myDatasourceConfig;
    private long myXmlModDate;

    public PhotoListener(IphotoDatasourceConfig datasourceConfig, Thread watchdogThread, DatabaseUpdateQueue queue, LibraryListener libraryListener, Map<Long, String> photoIdToPersId,
                         Collection<String> photoIds) throws SQLException {
        myDatasourceConfig = datasourceConfig;
        myWatchdogThread = watchdogThread;
        myQueue = queue;
        myLibraryListener = libraryListener;
        myPhotoIdToPersId = photoIdToPersId;
        myPhotoIds = photoIds;
        myPathReplacements = new HashSet<CompiledReplacementRule>();
        for (ReplacementRule pathReplacement : myDatasourceConfig.getPathReplacements()) {
            myPathReplacements.add(new CompiledReplacementRule(pathReplacement));
        }
        myXmlModDate = new File(myDatasourceConfig.getDefinition(), IphotoDatasourceConfig.XML_FILE_NAME).lastModified();
    }

    public int getUpdatedCount() {
        return myUpdatedCount;
    }

    public boolean beforeDictPut(Map dict, String key, Object value) {
        Map photo = (Map) value;
        String photoId = calculatePhotoId(key, photo);
        if (photoId != null) {
            if (processPhoto(key, photo, photoId, myPhotoIds.remove(photoId))) {
                myUpdatedCount++;
            }
        }
        return false;
    }

    private String calculatePhotoId(String key, Map photo) {
        if (myLibraryListener.getLibraryId() == null) {
            return null;
        }
        String photoId = myLibraryListener.getLibraryId() + "_";
        photoId += photo.get("GUID") != null ? MyTunesRssBase64Utils.encode((String) photo.get("GUID")) : "PhotoID" + key;
        return photoId;
    }

    public boolean beforeArrayAdd(List array, Object value) {
        throw new UnsupportedOperationException("method beforeArrayAdd of class ItunesLoader$TrackListener is not supported!");
    }

    private boolean processPhoto(String key, Map photo, String photoId, boolean existing) {
        if (myWatchdogThread.isInterrupted()) {
            throw new ShutdownRequestedException();
        }
        String name = (String) photo.get("Caption");
        String mediaType = (String) photo.get("MediaType");
        if ("Image".equals(mediaType)) {
            String filename = applyReplacements((String) photo.get("ImagePath"));
            if (StringUtils.isNotBlank(filename)) {
                File file = new File(filename);
                if (file.isFile() && (!existing || myXmlModDate >= myLibraryListener.getTimeLastUpate() || file.lastModified() >= myLibraryListener.getTimeLastUpate())) {
                    InsertOrUpdatePhotoStatement statement = existing ? new UpdatePhotoStatement() : new InsertPhotoStatement();
                    statement.clear();
                    statement.setId(photoId);
                    statement.setName(MyTunesRssUtils.normalize(name.trim()));
                    //Long createDate = MyTunesRssExifUtils.getCreateDate(file);
                    Long createDate = (((Double)photo.get("DateAsTimerInterval")).longValue() * 1000) + 978303600000L;
                    statement.setDate(createDate != null ? createDate.longValue() : -1);
                    statement.setFile(filename);
                    myQueue.offer(new DataStoreStatementEvent(statement, "Could not insert photo \"" + name + "\" into database"));
                    HandlePhotoImagesStatement handlePhotoImagesStatement = new HandlePhotoImagesStatement(file, photoId, 0);
                    myQueue.offer(new DataStoreStatementEvent(handlePhotoImagesStatement, "Could not insert photo \"" + name + "\" into database"));
                    myPhotoIdToPersId.put(Long.valueOf(key), photoId);
                    return true;
                } else if (existing) {
                    myPhotoIdToPersId.put(Long.valueOf(key), photoId);
                }
                return false;
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
}
