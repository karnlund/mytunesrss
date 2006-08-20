/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.datastore.statement;

import org.apache.commons.lang.*;
import org.apache.commons.logging.*;

import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * de.codewave.mytunesrss.datastore.statement.FindTrackQueryry
 */
public class FindPlaylistTracksQuery extends DataStoreQuery<Collection<Track>> {
    private static final Log LOG = LogFactory.getLog(FindPlaylistTracksQuery.class);
    
    private static final String BASE_SQL =
            "SELECT ltp.index AS index, t.id AS id, t.name AS name, t.artist AS artist, t.album AS album, t.time AS time, t.track_number AS track_number, t.file AS file, t.protected AS protected, t.video AS video FROM link_track_playlist ltp, track t WHERE t.id = ltp.track_id AND ltp.playlist_id = ?";
    private static final String LIMIT = " LIMIT ? OFFSET ?";
    private static final String PLAYLIST_ORDER = BASE_SQL + " ORDER BY index";
    private static final String PLAYLIST_ORDER_WITH_LIMIT = PLAYLIST_ORDER + LIMIT;
    private static final String ALBUM_ORDER = BASE_SQL + " ORDER BY album, track_number, name";
    private static final String ALBUM_ORDER_WITH_LIMIT = ALBUM_ORDER + LIMIT;
    private static final String ARTIST_ORDER = BASE_SQL + " ORDER BY artist, album, track_number, name";
    private static final String ARTIST_ORDER_WITH_LIMIT = ARTIST_ORDER + LIMIT;
    private FindPlaylistTracksQuery.TrackResultBuilder myBuilder = new FindPlaylistTracksQuery.TrackResultBuilder();
    private String mySql;
    private Object[] myParameters;

    public FindPlaylistTracksQuery(String id) {
        myParameters = getParameters(id);
        mySql = myParameters.length == 3 ? PLAYLIST_ORDER_WITH_LIMIT : PLAYLIST_ORDER;
    }

    private String[] getParameters(String id) {
        String[] splitId = StringUtils.split(id, "_");
        if (splitId.length != 3) {
            return splitId;
        }
        try {
            String[] params = new String[3];
            params[0] = splitId[0];
            params[1] = Integer.toString(Integer.parseInt(splitId[2]) - Integer.parseInt(splitId[1]) + 1);
            params[2] = splitId[1];
            return params;
        } catch (NumberFormatException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Could not parse playlist ID parameters.");
            }
            return splitId;
        }
    }

    public FindPlaylistTracksQuery(String id, boolean sortByArtist) {
        myParameters = getParameters(id);
        mySql = sortByArtist ? (myParameters.length == 3 ? ARTIST_ORDER_WITH_LIMIT : ARTIST_ORDER) : (myParameters.length == 3 ? ALBUM_ORDER_WITH_LIMIT : ALBUM_ORDER);
    }

    public Collection<Track> execute(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(mySql);
        return execute(statement, myBuilder, myParameters);
    }

    public static class TrackResultBuilder implements ResultBuilder<Track> {
        private TrackResultBuilder() {
            // intentionally left blank
        }

        public Track create(ResultSet resultSet) throws SQLException {
            Track track = new Track();
            track.setId(resultSet.getString("ID"));
            track.setName(resultSet.getString("NAME"));
            track.setArtist(resultSet.getString("ARTIST"));
            track.setAlbum(resultSet.getString("ALBUM"));
            track.setTime(resultSet.getInt("TIME"));
            track.setTrackNumber(resultSet.getInt("TRACK_NUMBER"));
            track.setFile(new File(resultSet.getString("FILE")));
            track.setProtected(resultSet.getBoolean("PROTECTED"));
            track.setVideo(resultSet.getBoolean("VIDEO"));
            return track;
        }
    }
}