package de.codewave.mytunesrss.remote.service;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssBase64Utils;
import de.codewave.mytunesrss.User;
import de.codewave.mytunesrss.command.MyTunesRssCommand;
import de.codewave.mytunesrss.remote.MyTunesRssRemoteEnv;
import de.codewave.mytunesrss.servlet.WebConfig;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

/**
 * de.codewave.mytunesrss.remote.service.VideoLanClientService
 */
public class VideoLanClientService implements RemoteControlService {
    private void assertAuthenticated() throws IllegalAccessException {
        User user = MyTunesRssRemoteEnv.getSession().getUser();
        if (user == null) {
            throw new IllegalAccessException("Unauthorized");
        }
    }

    private VideoLanClient getVideoLanClient() throws IllegalAccessException, IOException, InterruptedException {
        VideoLanClient videoLanClient = new VideoLanClient();
        videoLanClient.connect(MyTunesRss.CONFIG.getVideoLanClientHost(), MyTunesRss.CONFIG.getVideoLanClientPort());
        return videoLanClient;
    }

    public void loadPlaylist(String playlistId, boolean start) throws IllegalAccessException, IOException, InterruptedException {
        loadItem("playlist=" + playlistId, start);
    }

    private void loadItem(String pathInfo, boolean start) throws IllegalAccessException, IOException, InterruptedException {
        assertAuthenticated();
        String url = MyTunesRssRemoteEnv.getServerCall(MyTunesRssCommand.CreatePlaylist, pathInfo + "/type=" + WebConfig.PlaylistType.M3u) + "/mytunesrss.m3u";
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            if (start) {
                videoLanClient.sendCommands("clear", "add " + url);
            } else {
                videoLanClient.sendCommands("clear", "add " + url, "stop");
            }
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void loadAlbum(String albumName, boolean start) throws IllegalAccessException, IOException, InterruptedException {
        loadItem("album=" + MyTunesRssBase64Utils.encode(albumName), start);
    }

    public void loadArtist(String artistName, boolean fullAlbums, boolean start) throws IllegalAccessException, IOException, InterruptedException {
        if (fullAlbums) {
            loadItem("fullAlbums=true/artist=" + MyTunesRssBase64Utils.encode(artistName), start);
        } else {
            loadItem("artist=" + MyTunesRssBase64Utils.encode(artistName), start);
        }
    }

    public void loadGenre(String genreName, boolean start) throws IllegalAccessException, IOException, InterruptedException {
        loadItem("genre=" + MyTunesRssBase64Utils.encode(genreName), start);
    }

    public void loadTrack(String trackId, boolean start) throws IllegalAccessException, IOException, InterruptedException {
        loadItem("track=" + trackId, start);
    }

    public void loadTracks(String[] trackIds, boolean start) throws IllegalAccessException, IOException, InterruptedException {
        loadItem("tracklist=" + StringUtils.join(trackIds, ","), start);
    }

    public void clearPlaylist() throws IllegalAccessException, IOException, InterruptedException {
        assertAuthenticated();
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            videoLanClient.sendCommands("clear");
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void play(int index) throws IllegalAccessException, IOException, InterruptedException {
        assertAuthenticated();
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            videoLanClient.sendCommands("goto " + index);
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void pause() throws IllegalAccessException, IOException, InterruptedException {
        assertAuthenticated();
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            videoLanClient.sendCommands("pause");
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void stop() throws IllegalAccessException, IOException, InterruptedException {
        assertAuthenticated();
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            videoLanClient.sendCommands("stop");
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void next() throws IllegalAccessException, IOException, InterruptedException {
        assertAuthenticated();
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            videoLanClient.sendCommands("next");
        } finally {
            videoLanClient.disconnect();
        }
    }

    public void prev() throws IllegalAccessException, IOException, InterruptedException {
        assertAuthenticated();
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            videoLanClient.sendCommands("prev");
        } finally {
            videoLanClient.disconnect();
        }
    }

    public String sendCommand(String command) throws IOException, IllegalAccessException, InterruptedException {
        assertAuthenticated();
        VideoLanClient videoLanClient = getVideoLanClient();
        try {
            return videoLanClient.sendCommands(command);
        } finally {
            videoLanClient.disconnect();
        }
    }
}