/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.jsp.*;
import de.codewave.mytunesrss.meta.*;
import de.codewave.utils.*;
import org.apache.commons.lang.*;
import org.apache.commons.logging.*;

import javax.servlet.http.*;
import java.text.*;
import java.util.*;

/**
 * de.codewave.mytunesrss.command.CreateRssCommandHandler
 */
public class CreateRssCommandHandler extends CreatePlaylistBaseCommandHandler {
    private static final Log LOG = LogFactory.getLog(CreateRssCommandHandler.class);
    private static final SimpleDateFormat PUBLISH_DATE_FORMAT = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z", Locale.US);

    @Override
    public void executeAuthorized() throws Exception {
        if (getAuthUser().isRss()) {
            String feedUrl = getRequest().getRequestURL().toString();
            String channel = feedUrl.substring(feedUrl.lastIndexOf('/') + 1);
            channel = channel.substring(0, channel.lastIndexOf('.'));
            getRequest().setAttribute("channel", MiscUtils.decodeUrl(channel.replace('_', ' ')));
            getRequest().setAttribute("pubDate", PUBLISH_DATE_FORMAT.format(new Date()));
            getRequest().setAttribute("feedUrl", feedUrl);
            getRequest().setAttribute("userAgentPsp", isUserAgentPSP());
            Collection<Track> tracks = getTracks().getResults();
            if (tracks != null && !tracks.isEmpty()) {
                if (getWebConfig().isRssArtwork()) {
                    for (Track track : tracks) {
                        try {
                            Image image = MyTunesRssMp3Utils.getImage(track);
                            if (image != null && image.getData() != null && image.getData().length > 0 &&
                                    StringUtils.isNotEmpty(image.getMimeType())) {
                                getRequest().setAttribute("imageTrackId", track.getId());
                                break;// use first available image
                            } else {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Could not extract valid artwork from mp3 file.");
                                }
                            }
                        } catch (Exception e) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Could not extract valid artwork from mp3 file.", e);
                            }
                        }
                    }
                }
                getRequest().setAttribute("tracks", tracks);
                forward(MyTunesRssResource.TemplateRss);
            } else {
                addError(new BundleError("error.emptyFeed"));
                forward(MyTunesRssCommand.ShowPortal);// todo: redirect to backUrl
            }
        } else {
            getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }
}