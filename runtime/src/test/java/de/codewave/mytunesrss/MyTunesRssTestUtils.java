package de.codewave.mytunesrss;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssConfig;
import de.codewave.mytunesrss.datastore.statement.CreateAllTablesStatement;
import de.codewave.mytunesrss.datastore.MyTunesRssDataStore;
import de.codewave.utils.sql.DataStoreSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * de.codewave.mytunesrss.datastore.MyTunesRssDatastoreTestUtils
 */
public class MyTunesRssTestUtils {

    public static void initMyTunesRss() {
        MyTunesRss.HEADLESS = true;
        MyTunesRss.VERSION = "1.0.0";
        MyTunesRss.CONFIG = new MyTunesRssConfig();
    }

    public static void initDatabase() throws ClassNotFoundException, IOException, SQLException {
        MyTunesRss.CONFIG.setDatabaseType("h2");
        MyTunesRss.CONFIG.setDatabaseConnection("jdbc:h2:mem:mytunesrss;DB_CLOSE_DELAY=-1");
        MyTunesRss.CONFIG.setDatabaseUser("sa");
        MyTunesRss.CONFIG.setDatabasePassword("");
        Class.forName("org.h2.Driver");
        MyTunesRss.STORE = new MyTunesRssDataStore();
        MyTunesRss.STORE.init();
        DataStoreSession session = MyTunesRss.STORE.getTransaction();
        session.executeStatement(new CreateAllTablesStatement());
        session.commit();
    }
}