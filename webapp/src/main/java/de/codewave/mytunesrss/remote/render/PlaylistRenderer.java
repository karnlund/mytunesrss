package de.codewave.mytunesrss.remote.render;

import de.codewave.mytunesrss.datastore.statement.Playlist;
import de.codewave.mytunesrss.remote.MyTunesRssRemoteEnv;
import de.codewave.mytunesrss.command.MyTunesRssCommand;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * de.codewave.mytunesrss.remote.render.PlaylistRenderer
 */
public class PlaylistRenderer implements Renderer<Map<String, Object>, Playlist> {
    public Map<String, Object> render(Playlist playlist) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("id", StringUtils.trimToEmpty(playlist.getId()));
        result.put("name", StringUtils.trimToEmpty(playlist.getName()));
        result.put("count", playlist.getTrackCount());
        result.put("userPrivate", playlist.isUserPrivate());
        result.put("downloadUrl", MyTunesRssRemoteEnv.getServerCall(MyTunesRssCommand.GetZipArchive, "playlist=" + playlist.getId()));
        result.put("m3uUrl", MyTunesRssRemoteEnv.getServerCall(MyTunesRssCommand.CreatePlaylist, "playlist=" + playlist.getId() + "/type=M3u"));
        result.put("xspfUrl", MyTunesRssRemoteEnv.getServerCall(MyTunesRssCommand.CreatePlaylist, "playlist=" + playlist.getId() + "/type=Xspf"));
        result.put("rssUrl", MyTunesRssRemoteEnv.getServerCall(MyTunesRssCommand.CreateRss, "playlist=" + playlist.getId()));
        return result;
    }
}