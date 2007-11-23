package de.codewave.mytunesrss.xmlrpc.render;

import de.codewave.mytunesrss.datastore.statement.*;

import java.util.*;

import org.apache.commons.lang.*;

/**
 * de.codewave.mytunesrss.xmlrpc.render.PlaylistRenderer
 */
public class PlaylistRenderer implements XmlRpcRenderer<Map<String, Object>, Playlist> {
    public Map<String, Object> render(Playlist playlist) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("id", StringUtils.trimToEmpty(playlist.getId()));
        result.put("name", StringUtils.trimToEmpty(playlist.getName()));
        result.put("count", playlist.getTrackCount());
        return result;
    }
}