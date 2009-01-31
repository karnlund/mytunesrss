package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.MyTunesRssEvent;
import de.codewave.mytunesrss.MyTunesRssEventManager;
import de.codewave.mytunesrss.datastore.statement.SaveMyTunesSmartPlaylistStatement;
import de.codewave.mytunesrss.datastore.statement.SmartInfo;
import de.codewave.mytunesrss.datastore.statement.RefreshSmartPlaylistsStatement;
import de.codewave.mytunesrss.jsp.BundleError;
import de.codewave.mytunesrss.jsp.MyTunesRssResource;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;

/**
 * de.codewave.mytunesrss.command.SaveSmartPlaylistCommandHandler
 */
public class SaveSmartPlaylistCommandHandler extends MyTunesRssCommandHandler {

    @Override
    public void executeAuthorized() throws Exception {
        if (StringUtils.isBlank(getRequestParameter("smartPlaylist.playlist.name", null))) {
            addError(new BundleError("error.needPlaylistNameForSave"));
        }
        if (!isError()) {
            SmartInfo smartInfo = new SmartInfo();
            smartInfo.setAlbumPattern(getRequestParameter("smartPlaylist.smartInfo.albumPattern", null));
            smartInfo.setArtistPattern(getRequestParameter("smartPlaylist.smartInfo.artistPattern", null));
            smartInfo.setGenrePattern(getRequestParameter("smartPlaylist.smartInfo.genrePattern", null));
            smartInfo.setTitlePattern(getRequestParameter("smartPlaylist.smartInfo.titlePattern", null));
            smartInfo.setFilePattern(getRequestParameter("smartPlaylist.smartInfo.filePattern", null));
            if (StringUtils.isNotBlank(getRequestParameter("smartPlaylist.smartInfo.timeMin", null))) {
                smartInfo.setTimeMin(getIntegerRequestParameter("smartPlaylist.smartInfo.timeMin", 0));
            }
            if (StringUtils.isNotBlank(getRequestParameter("smartPlaylist.smartInfo.timeMax", null))) {
                smartInfo.setTimeMax(getIntegerRequestParameter("smartPlaylist.smartInfo.timeMax", 0));
            }
            if (StringUtils.isNotBlank(getRequestParameter("smartPlaylist.smartInfo.video", null))) {
                smartInfo.setVideo(getBooleanRequestParameter("smartPlaylist.smartInfo.video", false));
            }
            if (StringUtils.isNotBlank(getRequestParameter("smartPlaylist.smartInfo.protected", null))) {
                smartInfo.setProtected(getBooleanRequestParameter("smartPlaylist.smartInfo.protected", false));
            }
            SaveMyTunesSmartPlaylistStatement statement = new SaveMyTunesSmartPlaylistStatement(getAuthUser().getName(), getBooleanRequestParameter(
                    "smartPlaylist.playlist.userPrivate",
                    false), smartInfo);
            statement.setId(getRequestParameter("smartPlaylist.playlist.id", null));
            statement.setName(getRequestParameter("smartPlaylist.playlist.name", null));
            statement.setTrackIds(Collections.<String>emptyList());
            getTransaction().executeStatement(statement);
            getTransaction().executeStatement(new RefreshSmartPlaylistsStatement());
            forward(MyTunesRssCommand.ShowPlaylistManager);
        } else {
            createParameterModel("smartPlaylist.playlist.id",
                                 "smartPlaylist.playlist.name",
                                 "smartPlaylist.playlist.userPrivate",
                                 "smartPlaylist.smartInfo.albumPattern",
                                 "smartPlaylist.smartInfo.artistPattern",
                                 "smartPlaylist.smartInfo.genrePattern",
                                 "smartPlaylist.smartInfo.titlePattern",
                                 "smartPlaylist.smartInfo.filePattern",
                                 "smartPlaylist.smartInfo.timeMin",
                                 "smartPlaylist.smartInfo.timeMax",
                                 "smartPlaylist.smartInfo.protected",
                                 "smartPlaylist.smartInfo.video");
            getRequest().setAttribute("fields", EditSmartPlaylistCommandHandler.getFields());
            forward(MyTunesRssResource.EditSmartPlaylist);
        }
    }
}