/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.camel.mp3.Mp3Info;
import de.codewave.camel.mp3.Mp3Utils;
import de.codewave.mytunesrss.FileSupportUtils;
import de.codewave.mytunesrss.datastore.statement.FindAllTagsForTrackQuery;
import de.codewave.mytunesrss.datastore.statement.FindTrackQuery;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.mytunesrss.jsp.MyTunesRssResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;

/**
 * de.codewave.mytunesrss.command.ShowTrackInfoCommandHandler
 */
public class ShowTrackInfoCommandHandler extends MyTunesRssCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShowTrackInfoCommandHandler.class);

    @Override
    public void executeAuthorized() throws Exception {
        String trackId = getRequest().getParameter("track");
        Collection<Track> tracks = getTransaction().executeQuery(FindTrackQuery.getForIds(new String[] {trackId})).getResults();
        if (!tracks.isEmpty()) {
            Track track = tracks.iterator().next();
            getRequest().setAttribute("track", track);
            getRequest().setAttribute("tags", getTransaction().executeQuery(new FindAllTagsForTrackQuery(track.getId())).getResults());
            if (FileSupportUtils.isMp3(track.getFile())) {
                try {
                    Mp3Info info = Mp3Utils.getMp3Info(new FileInputStream(track.getFile()));
                    getRequest().setAttribute("mp3info", Boolean.TRUE);
                    getRequest().setAttribute("avgBitRate", info.getAvgBitrate());
                    getRequest().setAttribute("avgSampleRate", info.getAvgSampleRate());
                } catch (IOException e) {
                    LOGGER.info("Could not get MP3 info from track.", e);
                }
            }
        }
        forward(MyTunesRssResource.TrackInfo);
    }
}