package de.codewave.mytunesrss.datastore.statement;

import de.codewave.mytunesrss.*;
import de.codewave.utils.sql.*;
import org.apache.commons.logging.*;

import java.sql.*;

/**
 * de.codewave.mytunesrss.datastore.statement.InsertImageStatement
 */
public class InsertImageStatement extends InsertOrUpdateImageStatement {
    public InsertImageStatement(String trackId, int size, byte[] data) {
        super(trackId, size, data);
    }

    protected String getStatementName() {
        return "insertImage";
    }
}