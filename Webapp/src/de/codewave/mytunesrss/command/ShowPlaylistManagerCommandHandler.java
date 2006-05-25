/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.jsp.*;
import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.*;

import java.util.*;

/**
 * de.codewave.mytunesrss.command.ShowPlaylistManagerCommandHandler
 */
public class ShowPlaylistManagerCommandHandler extends MyTunesRssCommandHandler {

    @Override
    public void executeAuthorized() throws Exception {
        List<Playlist> playlists = (List<Playlist>)getDataStore().executeQuery(new FindPlaylistQuery(PlaylistType.MyTunes));
        int pageSize = getWebConfig().getPageSize();
        if (playlists.size() > pageSize) {
            int current = Integer.parseInt(getRequestParameter("index", "0"));
            Pager pager = createPager(playlists.size(), current);
            getRequest().setAttribute("pager", pager);
            playlists = playlists.subList(current * pageSize, Math.min((current * pageSize) + pageSize, playlists.size()));
        }
        getRequest().setAttribute("playlists", playlists);
        forward(MyTunesRssResource.PlaylistManager);
    }
}