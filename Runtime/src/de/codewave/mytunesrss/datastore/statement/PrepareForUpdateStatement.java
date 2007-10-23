/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.datastore.statement;

import de.codewave.utils.sql.*;

import java.sql.*;

/**
 * de.codewave.mytunesrss.datastore.statement.PrepareForUpdateStatement
 */
public class PrepareForUpdateStatement implements DataStoreStatement {
    public void execute(Connection connection) throws SQLException {
        connection.createStatement().execute("DELETE FROM playlist WHERE type = '" + PlaylistType.ITunes + "'");
        connection.createStatement().execute("DELETE FROM playlist WHERE type = '" + PlaylistType.M3uFile + "'");
        connection.createStatement().execute("DELETE FROM album");
        connection.createStatement().execute("DELETE FROM artist");
        connection.createStatement().execute("DELETE FROM genre");
        connection.createStatement().execute("DELETE FROM pager");
    }
}