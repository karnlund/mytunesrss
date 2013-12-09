/*
 * Copyright (c) 2012. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.rest.representation;

import de.codewave.mytunesrss.rest.IncludeExcludeInterceptor;
import de.codewave.utils.Version;

/**
 * Representation of the MyTunesRSS server version.
 */
public class VersionRepresentation implements RestRepresentation {
    private int myMajor;
    private int myMinor;
    private int myBugfix;
    private String myText;

    public VersionRepresentation() {
        // nothing to do here
    }

    public VersionRepresentation(Version version) {
        if (IncludeExcludeInterceptor.isAttr("major")) {
            myMajor = version.getMajor();
        }
        if (IncludeExcludeInterceptor.isAttr("minor")) {
            myMinor = version.getMinor();
        }
        if (IncludeExcludeInterceptor.isAttr("bugfix")) {
            myBugfix = version.getBugfix();
        }
        if (IncludeExcludeInterceptor.isAttr("text")) {
            myText = version.toString();
        }
    }

    /**
     * The major number of the version (useful for comparisons).
     */
    public int getMajor() {
        return myMajor;
    }

    public void setMajor(int major) {
        myMajor = major;
    }

    /**
     * The minor number of the version (useful for comparisons).
     */
    public int getMinor() {
        return myMinor;
    }

    public void setMinor(int minor) {
        myMinor = minor;
    }

    /**
     * The bugfix number of the version (useful for comparisons).
     */
    public int getBugfix() {
        return myBugfix;
    }

    public void setBugfix(int bugfix) {
        myBugfix = bugfix;
    }

    /**
     * The full version text.
     */
    public String getText() {
        return myText;
    }

    public void setText(String text) {
        myText = text;
    }
}
