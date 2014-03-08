/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssSendCounter;
import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.MyTunesRssWebUtils;
import de.codewave.mytunesrss.datastore.statement.FindTrackQuery;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.utils.MiscUtils;
import de.codewave.utils.servlet.ServletUtils;
import de.codewave.utils.servlet.SessionManager;
import de.codewave.utils.servlet.StreamSender;
import de.codewave.utils.sql.DataStoreQuery;
import de.codewave.utils.sql.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * de.codewave.mytunesrss.command.PlayTrackCommandHandler
 */
public class PlayTrackCommandHandler extends BandwidthThrottlingCommandHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PlayTrackCommandHandler.class);

    @Override
    public void executeAuthorized() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug(ServletUtils.getRequestInfo(getRequest()));
        }
        StreamSender streamSender;
        String trackId = getRequest().getParameter("track");
        try {
            QueryResult<Track> tracks = getTransaction().executeQuery(FindTrackQuery.getForIds(new String[] {trackId}));
            if (tracks.getResultSize() > 0) {
                final Track track = tracks.nextResult();
                if (!getAuthUser().isQuotaExceeded()) {
                    File file = track.getFile();
                    if (!file.exists()) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Requested file \"" + MiscUtils.getUtf8UrlEncoded(file.getAbsolutePath()) + "\" not found, trying harder to find file.");
                        }
                        file = MyTunesRssUtils.searchFile(file);
                    }
                    if (!file.exists()) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Requested file \"" + MiscUtils.getUtf8UrlEncoded(file.getAbsolutePath()) + "\" does not exist.");
                        }
                        MyTunesRss.ADMIN_NOTIFY.notifyMissingFile(track);
                        streamSender = new StatusCodeSender(HttpServletResponse.SC_NOT_FOUND);
                    } else {
                        streamSender = MyTunesRssWebUtils.getMediaStreamSender(getRequest(), track, file);
                        MyTunesRssUtils.asyncPlayCountAndDateUpdate(trackId);
                        getAuthUser().playLastFmTrack(track);
                        streamSender.setCounter(new MyTunesRssSendCounter(getAuthUser(), track.getId(), SessionManager.getSessionInfo(getRequest())));
                    }
                } else {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("User limit exceeded, sending response code SC_CONFLICT instead.");
                    }
                    MyTunesRss.ADMIN_NOTIFY.notifyQuotaExceeded(getAuthUser());
                    streamSender = new StatusCodeSender(HttpServletResponse.SC_CONFLICT, "QUOTA_EXCEEDED");
                }
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("No tracks recognized in request, sending response code SC_NOT_FOUND instead.");
                }
                streamSender = new StatusCodeSender(HttpServletResponse.SC_NOT_FOUND);
            }
        } finally {
            getTransaction().commit();
        }
        if (ServletUtils.isHeadRequest(getRequest())) {
            sendHeadResponse(streamSender);
        } else {
            sendGetResponse(streamSender);
        }
    }

    protected void sendGetResponse(StreamSender streamSender) throws IOException {
        streamSender.sendGetResponse(getRequest(), getResponse(), false);
    }

    protected void sendHeadResponse(StreamSender streamSender) {
        streamSender.sendHeadResponse(getRequest(), getResponse());
    }

}
