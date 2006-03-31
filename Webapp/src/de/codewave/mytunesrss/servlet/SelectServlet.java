/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.servlet;

import de.codewave.mytunesrss.itunes.*;
import de.codewave.mytunesrss.musicfile.*;
import de.codewave.utils.servlet.*;
import org.apache.commons.lang.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

public class SelectServlet extends BaseServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doCommand(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doCommand(request, response);
    }

    private void doCommand(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Collection<String> requestSelection = getRequestSelection(request);
        Collection<MusicFile> playlist = (Collection<MusicFile>)request.getSession().getAttribute("playlist");
        if (playlist == null) {
            playlist = new ArrayList<MusicFile>();
            request.getSession().setAttribute("playlist", playlist);
        }
        boolean finalSelection = "true".equalsIgnoreCase(request.getParameter("final"));
        if (!requestSelection.isEmpty() || !finalSelection || !playlist.isEmpty()) {
            ITunesLibrary library = ITunesLibraryContextListener.getLibrary(request);
            List<MusicFile> selectedFiles = new ArrayList<MusicFile>();
            for (String id : requestSelection) {
                List<MusicFile> matches = library.getMatchingFiles(new MusicFileIdSearch(id));
                SortOrder sortOrder = SortOrder.valueOf(request.getParameter("sortOrder"));
                switch (sortOrder) {
                    case Album:
                        Collections.sort(matches, new AlbumComparator());
                        break;
                    case Artist:
                        Collections.sort(matches, new ArtistComparator());
                        break;
                    default:
                        // intentionally left blank
                }
                selectedFiles.addAll(matches);
            }
            playlist.addAll(selectedFiles);
            if (finalSelection) {
                String channel = request.getParameter("channel");
                if (StringUtils.isEmpty(channel)) {
                    channel = "Codewave MyTunesRSS v" + System.getProperty("mytunesrss.version");
                }
                Map<String, String> urls = (Map<String, String>)request.getSession().getAttribute("urlMap");
                StringBuffer url = new StringBuffer(urls.get("rss")).append("/channel=").append(channel);
                for (MusicFile musicFile : playlist) {
                    url.append("/").append(musicFile.getId());
                }
                response.sendRedirect(url.toString());
            } else {
                request.getRequestDispatcher("/index.jsp").forward(request, response);
            }
        } else {
            request.setAttribute("error", "You must select at least one title for your feed!");
            SortOrder sortOrder = SortOrder.valueOf(request.getParameter("sortOrder"));
            createSectionsAndForward(request, response, sortOrder);
        }
    }
}