/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.jsp.*;

import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.sql.*;

/**
 * de.codewave.mytunesrss.command.ShowPortalCommandHandler
 */
public class ShowPortalCommandHandler extends MyTunesRssCommandHandler {
    public void executeAuthorized() throws SQLException, IOException, ServletException {
            List<Playlist> playlists = new ArrayList<Playlist>();
            for (Playlist playlist : getDataStore().executeQuery(new FindPlaylistsQuery())) {
                playlists.add(playlist);
                playlists.addAll(createSplittedPlaylists(playlist));
            }
            getRequest().setAttribute("playlists", playlists);
            forward(MyTunesRssResource.Portal);
    }

    private List<Playlist> createSplittedPlaylists(Playlist playlist) {
        List<Playlist> splittedPlaylists = new ArrayList<Playlist>();
        int maxCount = getWebConfig().getRssFeedLimit();
        if (maxCount > 0 && playlist.getTrackCount() > maxCount) {
            int startIndex = 0;
            while (startIndex < playlist.getTrackCount()) {
                int endIndex = Math.min(startIndex + maxCount - 1, playlist.getTrackCount() - 1);
                Playlist splittedPlaylist = new Playlist();
                splittedPlaylist.setId(playlist.getId() + "_" + startIndex + "_" + endIndex);
                splittedPlaylist.setName(playlist.getName() + " [" + createFixedLengthNumber(startIndex + 1, playlist.getTrackCount()) + "-" +
                        createFixedLengthNumber(endIndex + 1, playlist.getTrackCount()) + "]");
                splittedPlaylist.setTrackCount(endIndex - startIndex + 1);
                splittedPlaylists.add(splittedPlaylist);
                startIndex = endIndex + 1;
            }
        }
        return splittedPlaylists;
    }

    private String createFixedLengthNumber(int number, int maxNumber) {
        String fixedLengthNumber = Integer.toString(number);
        int length = Integer.toString(maxNumber).length();
        while (fixedLengthNumber.length() < length) {
            fixedLengthNumber = "0" + fixedLengthNumber;
        }
        return fixedLengthNumber;
    }
}