package de.codewave.mytunesrss.jmx;

import de.codewave.mytunesrss.*;

/**
 * de.codewave.mytunesrss.jmx.ErrorQueue
 */
public class ErrorQueue {
    private String myLastError;

    public ErrorQueue() {
        clear();
    }

    public void setLastError(String lastError) {
        myLastError = lastError;
    }

    public void clear() {
        myLastError = null;
    }

    public String popLastError() {
        if (!isEmpty()) {
            try {
                return myLastError;
            } finally {
                clear();
            }
        }
        return MyTunesRss.BUNDLE.getString("ok");
    }

    public boolean isEmpty() {
        return myLastError == null;
    }
}