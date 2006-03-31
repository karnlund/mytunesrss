/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.jsp;

import java.io.*;
import java.util.*;

/**
 * de.codewave.mytunesrss.jsp.Section
 */
public class Section implements Serializable {
    private List<SectionItem> myItems = new ArrayList<SectionItem>();

    public List<SectionItem> getItems() {
        return myItems;
    }

    public void addItem(SectionItem item) {
        myItems.add(item);
    }

    public boolean isCommonAlbum() {
        String album = getFirstAlbum();
        for (SectionItem item : myItems) {
            if (!item.getFile().getAlbum().equals(album)) {
                return false;
            }
        }
        return true;
    }

    public String getFirstAlbum() {
        return myItems != null && !myItems.isEmpty() ? myItems.get(0).getFile().getAlbum() : null;
    }

    public boolean isCommonArtist() {
        String artist = getFirstArtist();
        for (SectionItem item : myItems) {
            if (!item.getFile().getArtist().equals(artist)) {
                return false;
            }
        }
        return true;
    }

    public String getFirstArtist() {
        return myItems != null && !myItems.isEmpty() ? myItems.get(0).getFile().getArtist() : null;
    }

    public String getSectionIds() {
        StringBuffer ids = new StringBuffer();
        for (Iterator<SectionItem> iterator = myItems.iterator(); iterator.hasNext(); ) {
            ids.append(iterator.next().getFile().getId());
            if (iterator.hasNext()) {
                ids.append(";");
            }
        }
        return ids.toString();
    }
}