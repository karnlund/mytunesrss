/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.task;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssTask;
import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.datastore.statement.CreateAllTablesStatement;
import de.codewave.mytunesrss.datastore.statement.MigrationStatement;
import de.codewave.utils.Version;
import de.codewave.utils.sql.DataStoreQuery;
import de.codewave.utils.sql.DataStoreSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * de.codewave.mytunesrss.task.InitializeDatabaseTask
 */
public class InitializeDatabaseTask extends MyTunesRssTask {
    private Version myVersion;

    public void execute() throws IOException, SQLException {
        MyTunesRss.STORE.init();
        DataStoreSession session = MyTunesRss.STORE.getTransaction();
        try {
            myVersion = session.executeQuery(new DataStoreQuery<Version>() {
                public Version execute(Connection connection) throws SQLException {
                    try {
                        ResultSet resultSet = MyTunesRssUtils.createStatement(connection, "initialize").executeQuery();
                        if (resultSet.next()) {
                            resultSet = MyTunesRssUtils.createStatement(connection, "getSystemInformation").executeQuery();
                            if (resultSet.next()) {
                                return new Version(resultSet.getString("VERSION"));
                            }
                        }
                    } catch (SQLException e) {
                        // intentionally left blank
                    }
                    return null;
                }
            });
        } finally {
            DatabaseBuilderTask.doCheckpoint(session, true);
        }
        if (myVersion == null) {
            DataStoreSession storeSession = MyTunesRss.STORE.getTransaction();
            storeSession.executeStatement(new CreateAllTablesStatement());
            DatabaseBuilderTask.doCheckpoint(storeSession, true);
        } else {
            DataStoreSession storeSession = MyTunesRss.STORE.getTransaction();
            if (myVersion.compareTo(new Version(MyTunesRss.VERSION)) < 0) {
                storeSession.executeStatement(new MigrationStatement());
                DatabaseBuilderTask.doCheckpoint(storeSession, true);
            }
        }
    }

    public Version getDatabaseVersion() {
        return myVersion;
    }
}

