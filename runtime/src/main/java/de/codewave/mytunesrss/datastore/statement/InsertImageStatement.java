package de.codewave.mytunesrss.datastore.statement;

import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.utils.sql.SmartStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * de.codewave.mytunesrss.datastore.statement.InsertImageStatement
 */
public class InsertImageStatement {
    private static final Logger LOG = LoggerFactory.getLogger(InsertImageStatement.class);

    private String myHash;
    private int mySize;
    private String myMimeType;
    private byte[] myData;
    private SmartStatement myStatement;

    public InsertImageStatement(String hash, int size, String mimeType,  byte[] data) {
        myHash = hash;
        mySize = size;
        myMimeType = mimeType;
        myData = data;
        if (LOG.isDebugEnabled()) {
            if (data != null) {
                LOG.debug("Image data size for \"" + mimeType + "\" is " + data.length + " bytes.");
            } else {
                LOG.debug("Image data is NULL.");
            }
        }
    }

    public synchronized void execute(Connection connection) throws SQLException {
        if (myData != null) {
            try {
                if (myStatement == null) {
                    myStatement = MyTunesRssUtils.createStatement(connection, "insertImage");
                }
                myStatement.clearParameters();
                myStatement.setString("hash", myHash);
                myStatement.setInt("size", mySize);
                myStatement.setString("mimetype", myMimeType);
                myStatement.setObject("data", myData);
                myStatement.setLong("now", System.currentTimeMillis());
                myStatement.execute();
            } catch (SQLException e) {
                LOG.error("Could not insert image for hash \"" + myHash + "\".", e);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping image insert for hash \"" + myHash + "\" because image data is NULL.");
            }
        }
    }

    public void clear() {
        mySize = 0;
        myHash = null;
        myMimeType = null;
        myData = null;
    }
}
