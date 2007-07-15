/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.datastore.statement;

import java.sql.*;

/**
 * de.codewave.mytunesrss.datastore.statement.SaveITunesPlaylistStatement
 */
public class SaveITunesPlaylistStatement extends SavePlaylistStatement {
    public SaveITunesPlaylistStatement() {
        setType(PlaylistType.ITunes);
    }

    public void execute(Connection connection) throws SQLException {
        executeInsert(connection);
    }
}
