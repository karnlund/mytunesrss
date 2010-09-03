/*
 * Copyright (c) 2010. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.*;
import de.codewave.mytunesrss.datastore.statement.FindTrackQuery;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.mytunesrss.datastore.statement.UpdatePlayCountAndDateStatement;
import de.codewave.mytunesrss.httplivestreaming.HttpLiveStreamingCacheItem;
import de.codewave.mytunesrss.jsp.MyTunesFunctions;
import de.codewave.mytunesrss.transcoder.Transcoder;
import de.codewave.utils.io.LogStreamCopyThread;
import de.codewave.utils.io.StreamCopyThread;
import de.codewave.utils.servlet.FileSender;
import de.codewave.utils.servlet.SessionManager;
import de.codewave.utils.sql.DataStoreQuery;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class HttpLiveStreamingCommandHandler extends MyTunesRssCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpLiveStreamingCommandHandler.class);

    @Override
    public void executeAuthorized() throws IOException, SQLException {
        String trackId = getRequestParameter("track", null);
        String cacheKey = getRequestParameter("cacheKey", null);
        if (StringUtils.isBlank(trackId)) {
            getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "missing track id");
        }
        String[] pathInfo = StringUtils.split(getRequest().getPathInfo(), '/');
        if (pathInfo.length > 1) {
            if (StringUtils.endsWithIgnoreCase(pathInfo[pathInfo.length - 1], ".ts")) {
                sendMediaFile(cacheKey, pathInfo[pathInfo.length - 1]);
            } else if (StringUtils.isNotBlank(cacheKey)) {
                sendPlaylist(cacheKey, trackId);
            } else {
                redirectToPlaylist(trackId);
            }
        } else {
            getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void sendMediaFile(String cacheKey, String filename) throws IOException {
        HttpLiveStreamingCacheItem cacheItem = MyTunesRss.HTTP_LIVE_STREAMING_CACHE.get(cacheKey);
        if (cacheItem.isFailed()) {
            MyTunesRss.HTTP_LIVE_STREAMING_CACHE.remove(cacheKey);
            cacheItem = null;
        }
        if (cacheItem == null) {
            getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "media file not found");
        } else {
            File mediaFile = new File(getBaseDir(), filename);
            if (mediaFile.isFile()) {
                if (getAuthUser().isQuotaExceeded()) {
                    getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);
                } else {
                    FileSender fileSender = new FileSender(mediaFile, "video/MP2T", mediaFile.length());
                    fileSender.setCounter(new MyTunesRssSendCounter(getAuthUser(), SessionManager.getSessionInfo(getRequest())));
                    fileSender.sendGetResponse(getRequest(), getResponse(), false);
                }
            } else {
                getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "media file not found");
            }
        }
    }

    private File getBaseDir() {
        try {
            return new File(MyTunesRssUtils.getCacheDataPath(), MyTunesRss.CACHEDIR_HTTP_LIVE_STREAMING);
        } catch (IOException e) {
            throw new RuntimeException("Could not get cache data path.");
        }
    }

    private synchronized void sendPlaylist(String cacheKey, String trackId) throws SQLException, IOException {
        HttpLiveStreamingCacheItem cacheItem = MyTunesRss.HTTP_LIVE_STREAMING_CACHE.get(cacheKey);
        if (cacheItem == null) {
            DataStoreQuery.QueryResult<Track> tracks = getTransaction().executeQuery(FindTrackQuery.getForIds(new String[]{trackId}));
            if (tracks.getResultSize() > 0) {
                Track track = tracks.nextResult();
                if (track.getMediaType() == MediaType.Video) {
                    cacheItem = new HttpLiveStreamingCacheItem(cacheKey, 3600000); // TODO: timeout configuration?
                    InputStream mediaStream = MyTunesRssWebUtils.getMediaStream(getRequest(), track);
                    MyTunesRss.EXECUTOR_SERVICE.schedule(new HttpLiveStreamingSegmenterRunnable(cacheItem, mediaStream), 0, TimeUnit.MILLISECONDS);
                    MyTunesRss.HTTP_LIVE_STREAMING_CACHE.add(cacheItem);
                    getTransaction().executeStatement(new UpdatePlayCountAndDateStatement(new String[]{trackId}));
                    getAuthUser().playLastFmTrack(track);
                } else {
                    getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "requested track is not a video");
                    return;
                }
            } else {
                getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "track not found");
                return;
            }
        }
        // wait for at least 1 playlist item
        try {
            while (!cacheItem.isFailed() && !cacheItem.isDone() && cacheItem.getPlaylistSize() == 0) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            // we have been interrupted, so send the playlist file or an error now
        }
        if (cacheItem.isFailed()) {
            MyTunesRss.HTTP_LIVE_STREAMING_CACHE.remove(cacheKey);
            getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "playlist file not found");
        } else {
            byte[] playlistBytes = cacheItem.getPlaylist().getBytes("ISO-8859-1");
            getResponse().setContentType("application/x-mpegURL");
            getResponse().setContentLength(playlistBytes.length);
            getResponse().getOutputStream().write(playlistBytes);
        }
    }

    private void redirectToPlaylist(String trackId) throws SQLException, IOException {
            DataStoreQuery.QueryResult<Track> tracks = getTransaction().executeQuery(FindTrackQuery.getForIds(new String[]{trackId}));
            if (tracks.getResultSize() > 0) {
                Track track = tracks.nextResult();
                if (track.getMediaType() == MediaType.Video) {
                    Transcoder transcoder = MyTunesRssWebUtils.getTranscoder(getRequest(), track);
                    String contentType = transcoder != null ? transcoder.getTargetContentType() : track.getContentType();
                    if (contentType.equalsIgnoreCase("video/MP2T")) {
                        String cacheKey = transcoder != null ? transcoder.getTranscoderId() + "_" + trackId : trackId;
                        redirect(MyTunesFunctions.httpLiveStreamUrl(getRequest(), track, "cacheKey=" + cacheKey));
                    } else {
                        getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "video content-type is not \"video/MP2T\"");
                    }
                } else {
                    getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "requested track is not a video");
                }
            } else {
                getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "track not found");
            }
    }


    public class HttpLiveStreamingSegmenterRunnable implements Runnable {

        private HttpLiveStreamingCacheItem myCacheItem;

        private InputStream myStream;

        public HttpLiveStreamingSegmenterRunnable(HttpLiveStreamingCacheItem cacheItem, InputStream stream) {
            myCacheItem = cacheItem;
            myStream = stream;
        }

        public void run() {
            String[] command = new String[6];
            command[0] = getJavaExecutablePath();
            try {
                command[1] = "-Djna.library.path=" + MyTunesRssUtils.getPreferencesDataPath() + "/lib";
            } catch (IOException e) {
                throw new RuntimeException("Could not get prefs data path.", e);
            }
            command[2] = "-cp";
            command[3] = getClasspath();
            command[4] = "de.codewave.jna.ffmpeg.HttpLiveStreamingSegmenter";
            command[5] = getBaseDir().getAbsolutePath();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing HTTP Live Streaming command \"" + StringUtils.join(command, " ") + "\".");
            }
            BufferedReader reader = null;
            try {
                Process process = Runtime.getRuntime().exec(command);
                new LogStreamCopyThread(process.getErrorStream(), false, LoggerFactory.getLogger(getClass()), LogStreamCopyThread.LogLevel.Debug).start();
                new StreamCopyThread(myStream, true, process.getOutputStream(), true).start();
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                for (String responseLine = reader.readLine(); responseLine != null; responseLine = reader.readLine()) {
                    if (responseLine.startsWith(getBaseDir().getAbsolutePath())) {
                        myCacheItem.addFile(new File(StringUtils.trimToEmpty(responseLine)));
                    }
                }
                process.waitFor();
                if (process.exitValue() == 0) {
                    myCacheItem.setDone(true);
                } else {
                    myCacheItem.setFailed(true);
                }
            } catch (IOException e) {
                myCacheItem.setFailed(true);
            } catch (InterruptedException e) {
                myCacheItem.setFailed(true);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }

        private String getJavaExecutablePath() {
            return System.getProperty("java.home") + "/bin/java";
        }

        private String getClasspath() {
            StringBuilder sb = new StringBuilder();
            for (String cpElement : StringUtils.split(System.getProperty("java.class.path"), System.getProperty("path.separator"))) {
                if (!cpElement.startsWith(System.getProperty("java.home"))) {
                    sb.append(cpElement).append(System.getProperty("path.separator"));
                }
            }
            return sb.substring(0, sb.length() - 1);
        }
    }
}
