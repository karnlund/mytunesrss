/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.servlet;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssBase64Utils;
import de.codewave.mytunesrss.MyTunesRssWebUtils;
import de.codewave.mytunesrss.UserAgent;
import de.codewave.mytunesrss.config.FlashPlayerConfig;
import de.codewave.mytunesrss.config.User;
import de.codewave.mytunesrss.jsp.MyTunesRssResource;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * de.codewave.mytunesrss.servlet.WebConfig
 */
public class WebConfig {
    private static final Logger LOG = LoggerFactory.getLogger(WebConfig.class);

    private static final String CONFIG_COOKIE_NAME = MyTunesRss.APPLICATION_IDENTIFIER + "Cookie";
    private static final String CFG_FEED_TYPE_RSS = "feedTypeRss";
    private static final String CFG_FEED_TYPE_PLAYLIST = "feedTypePlaylist";
    private static final String CFG_PAGE_SIZE = "pageSize";
    private static final String CFG_PHOTO_PAGE_SIZE = "photoPageSize";
    private static final String CFG_SHOW_DOWNLOAD = "showDownload";
    private static final String CFG_SHOW_PLAYER = "showPlayer";
    private static final String CFG_PLAYLIST_TYPE = "playlistType";
    private static final String CFG_THEME = "theme";
    private static final String CFG_FLASH_PLAYER = "flashplayer";
    private static final String CFG_YAHOO_MEDIAPLAYER = "yahooMediaPlayer";
    private static final String CFG_BROWSER_START_INDEX = "browserStartIndex";
    private static final String CFG_MYTUNESRSSCOM_ADDRESS = "myTunesRssComAddress";
    private static final String CFG_ALBUM_IMAGE_SIZE = "albImgSize";
    private static final String CFG_SHOW_REMOTE_CONTROL = "rmCtrl";
    private static final String CFG_SHOW_ADD_REMOTE_CONTROL = "addRmCtrl";
    private static final String CFG_ACTIVE_TRANSCODERS = "actTra";
    private static final String CFG_KEEP_ALIVE = "keepAlive";
    private static final String CFG_SEARCH_FUZZINESS = "searchFuzziness";
    private static final String CFG_SHOW_EXTERNAL_SITES = "showExtSites";
    private static final String CFG_SHOW_ADD_TO_PLAYLIST = "showAddToPlaylist";
    private static final String CFG_PHOTO_SIZE = "photoSize";
    private static final String CFG_MAX_SEARCH_RESULTS = "maxSearchResults";
    private static final String CFG_PHOTO_JPEG_QUALITY = "pjq";
    private static Map<String, String> FEED_FILE_SUFFIXES = new HashMap<>();

    private static final String[] VALID_NAMES = {CFG_FEED_TYPE_RSS, CFG_FEED_TYPE_PLAYLIST, CFG_PAGE_SIZE, CFG_PHOTO_PAGE_SIZE,
            CFG_SHOW_DOWNLOAD, CFG_SHOW_PLAYER, CFG_PLAYLIST_TYPE, CFG_THEME,
            CFG_FLASH_PLAYER, CFG_YAHOO_MEDIAPLAYER, CFG_BROWSER_START_INDEX, CFG_MYTUNESRSSCOM_ADDRESS,
            CFG_ALBUM_IMAGE_SIZE, CFG_SHOW_REMOTE_CONTROL, CFG_SHOW_ADD_REMOTE_CONTROL, CFG_ACTIVE_TRANSCODERS, CFG_SEARCH_FUZZINESS,
            CFG_SHOW_EXTERNAL_SITES, CFG_KEEP_ALIVE, CFG_SHOW_ADD_TO_PLAYLIST, CFG_PHOTO_SIZE,
            CFG_MAX_SEARCH_RESULTS, CFG_PHOTO_JPEG_QUALITY};

    public enum PlaylistType {
        M3u(), Xspf(), Json(), JwMediaRss();

        public String getFileSuffix() {
            switch (this) {
                case M3u:
                    return "m3u";
                case Xspf:
                    return "xspf";
                case Json:
                    return "json";
                default:
                    throw new IllegalArgumentException("illegal playlist type: " + this.name());
            }
        }

        public MyTunesRssResource getTemplateResource() {
            switch (this) {
                case M3u:
                    return MyTunesRssResource.TemplateM3u;
                case Xspf:
                    return MyTunesRssResource.TemplateXspf;
                case Json:
                    return MyTunesRssResource.TemplateJson;
                case JwMediaRss:
                    return MyTunesRssResource.TemplateJwMediaRss;
                default:
                    throw new IllegalArgumentException("illegal playlist type: " + this.name());
            }
        }
    }

    private Map<String, String> myConfigValues = new HashMap<>();

    public void clear() {
        myConfigValues.clear();
    }

    public void initWithDefaults(HttpServletRequest request) {
        initWithDefaults();
        if (MyTunesRssWebUtils.getUserAgent(request) == UserAgent.Iphone) {
            initWithIphoneDefaults();
        } else if (MyTunesRssWebUtils.getUserAgent(request) == UserAgent.NintendoWii) {
            initWithNintendoWiiDefaults();
        }
    }

    private void initWithDefaults() {
        myConfigValues.put(CFG_FEED_TYPE_RSS, "true");
        myConfigValues.put(CFG_FEED_TYPE_PLAYLIST, "true");
        myConfigValues.put(CFG_PAGE_SIZE, "30");
        myConfigValues.put(CFG_PHOTO_PAGE_SIZE, "20");
        myConfigValues.put(CFG_SHOW_DOWNLOAD, "true");
        myConfigValues.put(CFG_SHOW_PLAYER, "true");
        myConfigValues.put(CFG_PLAYLIST_TYPE, PlaylistType.M3u.name());
        myConfigValues.put(CFG_YAHOO_MEDIAPLAYER, "false");
        myConfigValues.put(CFG_BROWSER_START_INDEX, "1");
        myConfigValues.put(CFG_MYTUNESRSSCOM_ADDRESS, "true");
        myConfigValues.put(CFG_ALBUM_IMAGE_SIZE, "128");
        myConfigValues.put(CFG_SHOW_REMOTE_CONTROL, "true");
        myConfigValues.put(CFG_KEEP_ALIVE, "false");
        myConfigValues.put(CFG_SEARCH_FUZZINESS, "50");
        myConfigValues.put(CFG_SHOW_EXTERNAL_SITES, "false");
        myConfigValues.put(CFG_SHOW_ADD_TO_PLAYLIST, "false");
        myConfigValues.put(CFG_PHOTO_SIZE, "50");
        myConfigValues.put(CFG_MAX_SEARCH_RESULTS, "250");
        myConfigValues.put(CFG_PHOTO_JPEG_QUALITY, "80");
    }

    private void initWithIphoneDefaults() {
        myConfigValues.put(CFG_FEED_TYPE_RSS, "false");
        myConfigValues.put(CFG_FEED_TYPE_PLAYLIST, "false");
        myConfigValues.put(CFG_SHOW_DOWNLOAD, "false");
        myConfigValues.put(CFG_ALBUM_IMAGE_SIZE, "256");
        myConfigValues.put(CFG_SHOW_REMOTE_CONTROL, "false");
        myConfigValues.put(CFG_SHOW_ADD_REMOTE_CONTROL, "false");
        myConfigValues.put(CFG_PHOTO_SIZE, "600");
    }

    private void initWithNintendoWiiDefaults() {
        myConfigValues.put(CFG_FEED_TYPE_RSS, "false");
        myConfigValues.put(CFG_FEED_TYPE_PLAYLIST, "false");
        myConfigValues.put(CFG_SHOW_DOWNLOAD, "false");
        myConfigValues.put(CFG_SHOW_PLAYER, "true");
        myConfigValues.put(CFG_FLASH_PLAYER, FlashPlayerConfig.SIMPLE.getId());
        myConfigValues.put(CFG_PHOTO_SIZE, "600");
    }

    /**
     * Load web config from server-side user profile.
     *
     * @param request Servlet request.
     * @param user User.
     */
    public void load(HttpServletRequest request, User user) {
        if (user != null && !user.isSharedUser() && StringUtils.isNotEmpty(user.getWebConfig(MyTunesRssWebUtils.getUserAgent(request)))) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Initializing web configuration from user settings.");
            }
            initFromString(MyTunesRssBase64Utils.decodeToString(user.getWebConfig(MyTunesRssWebUtils.getUserAgent(request))));
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Initializing web configuration from cookie.");
            }
            try {
                initFromString(MyTunesRssBase64Utils.decodeToString(getCookieValue(request)));
            } catch (Exception ignored) {
                // intentionally left blank
            }
        }
    }

    public void clearWithDefaults(HttpServletRequest request) {
        clear();
        initWithDefaults(request);
    }

    private String getCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (CONFIG_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return "";
    }

    private void initFromString(String cookieValue) {
        for (String keyValueToken : StringUtils.split(cookieValue, ';')) {
            int k = keyValueToken.indexOf('=');
            if (k > 0) {
                String keyName = keyValueToken.substring(0, k);
                if (ArrayUtils.contains(VALID_NAMES, keyName)) {
                    myConfigValues.put(keyName, k < keyValueToken.length() - 1 ? keyValueToken.substring(k + 1) : "");
                }
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Illegal configuration token found in cookie: \"" + keyValueToken + "\".");
                }
            }
        }
    }

    public void save(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(CONFIG_COOKIE_NAME, createCookieValue());
        cookie.setVersion(1);
        cookie.setComment("MyTunesRSS settings cookie");
        cookie.setMaxAge(3600 * 24 * 365);// one year
        String servletUrl = MyTunesRssWebUtils.getServletUrl(request);
        cookie.setPath(servletUrl.substring(servletUrl.lastIndexOf("/")));
        response.addCookie(cookie);
    }

    public void removeCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(CONFIG_COOKIE_NAME, createCookieValue());
        cookie.setVersion(1);
        cookie.setComment("MyTunesRSS settings cookie");
        cookie.setMaxAge(0); // delete cookie
        String servletUrl = MyTunesRssWebUtils.getServletUrl(request);
        cookie.setPath(servletUrl.substring(servletUrl.lastIndexOf("/")));
        response.addCookie(cookie);
    }

    public String createCookieValue() {
        StringBuilder value = new StringBuilder();
        for (Map.Entry<String, String> entry : myConfigValues.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                value.append(";").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        return MyTunesRssBase64Utils.encode(value.substring(1));
    }

    public Map<String, String> getMap() {
        return Collections.unmodifiableMap(myConfigValues);
    }

    public void setShowDownload(boolean showDownload) {
        myConfigValues.put(CFG_SHOW_DOWNLOAD, Boolean.toString(showDownload));
    }

    public boolean isShowDownload() {
        return Boolean.valueOf(myConfigValues.get(CFG_SHOW_DOWNLOAD));
    }

    public void setShowPlayer(boolean showPlayer) {
        myConfigValues.put(CFG_SHOW_PLAYER, Boolean.toString(showPlayer));
    }

    public boolean isShowPlayer() {
        return Boolean.valueOf(myConfigValues.get(CFG_SHOW_PLAYER));
    }

    public String getTheme() {
        return myConfigValues.get(CFG_THEME);
    }

    public void setTheme(String theme) {
        if (StringUtils.isNotEmpty(theme)) {
            myConfigValues.put(CFG_THEME, theme);
        } else {
            myConfigValues.remove(CFG_THEME);
        }
    }

    public boolean isShowRss() {
        return Boolean.valueOf(myConfigValues.get(CFG_FEED_TYPE_RSS));
    }

    public void setShowRss(boolean showRss) {
        myConfigValues.put(CFG_FEED_TYPE_RSS, Boolean.toString(showRss));
    }

    public boolean isShowPlaylist() {
        return Boolean.valueOf(myConfigValues.get(CFG_FEED_TYPE_PLAYLIST));
    }

    public void setShowPlaylist(boolean showPlaylist) {
        myConfigValues.put(CFG_FEED_TYPE_PLAYLIST, Boolean.toString(showPlaylist));
    }

    public String getPlaylistType() {
        String type = myConfigValues.get(CFG_PLAYLIST_TYPE);
        if (StringUtils.isNotEmpty(type)) {
            try {
                PlaylistType.valueOf(type);
                return type;
            } catch (IllegalArgumentException ignored) {
                // set default value and return it
            }
            setPlaylistType(PlaylistType.M3u.name());
        }
        return PlaylistType.M3u.name();
    }

    public void setPlaylistType(String playlistType) {
        myConfigValues.put(CFG_PLAYLIST_TYPE, playlistType);
    }

    public int getFeedTypeCount() {
        int count = isShowRss() ? 1 : 0;
        count += (isShowPlaylist() ? 1 : 0);
        return count;
    }

    public Map<String, String> getFeedFileSuffix() {
        return FEED_FILE_SUFFIXES;
    }

    public int getPageSize() {
        return Integer.parseInt(myConfigValues.get(CFG_PAGE_SIZE));
    }

    public int getEffectivePageSize() {
        int pageSize = getPageSize();
        return pageSize > 0 ? pageSize : 1000;
    }

    public void setPageSize(int pageSize) {
        myConfigValues.put(CFG_PAGE_SIZE, Integer.toString(pageSize));
    }

    public int getPhotoPageSize() {
        return Integer.parseInt(myConfigValues.get(CFG_PHOTO_PAGE_SIZE));
    }

    public int getEffectivePhotoPageSize() {
        int pageSize = getPhotoPageSize();
        return pageSize > 0 ? pageSize : 1000;
    }


    public void setPhotoPageSize(int photoPageSize) {
        myConfigValues.put(CFG_PHOTO_PAGE_SIZE, Integer.toString(photoPageSize));
    }

    public String getPlaylistFileSuffix() {
        return PlaylistType.valueOf(getPlaylistType()).getFileSuffix();
    }

    public MyTunesRssResource getPlaylistTemplateResource() {
        return PlaylistType.valueOf(getPlaylistType()).getTemplateResource();
    }

    public String getFlashplayer() {
        return myConfigValues.get(CFG_FLASH_PLAYER);
    }

    public void setFlashplayer(String type) {
        myConfigValues.put(CFG_FLASH_PLAYER, type);
    }

    public boolean isYahooMediaPlayer() {
        return Boolean.parseBoolean(myConfigValues.get(CFG_YAHOO_MEDIAPLAYER));
    }

    public void setYahooMediaPlayer(boolean yahooMediaPlayer) {
        myConfigValues.put(CFG_YAHOO_MEDIAPLAYER, Boolean.toString(yahooMediaPlayer));
    }

    public String getBrowserStartIndex() {
        return myConfigValues.get(CFG_BROWSER_START_INDEX);
    }

    public void setBrowserStartIndex(String browserStartIndex) {
        myConfigValues.put(CFG_BROWSER_START_INDEX, browserStartIndex);
    }

    public boolean isMyTunesRssComAddress() {
        return Boolean.parseBoolean(myConfigValues.get(CFG_MYTUNESRSSCOM_ADDRESS));
    }

    public void setMyTunesRssComAddress(boolean myTunesRssComAddress) {
        myConfigValues.put(CFG_MYTUNESRSSCOM_ADDRESS, Boolean.toString(myTunesRssComAddress));
    }

    public int getAlbumImageSize() {
        return Integer.parseInt(myConfigValues.get(CFG_ALBUM_IMAGE_SIZE));
    }

    public void setAlbumImageSize(int imageSize) {
        myConfigValues.put(CFG_ALBUM_IMAGE_SIZE, Integer.toString(imageSize));
    }

    public boolean isRemoteControl() {
        return Boolean.parseBoolean(myConfigValues.get(CFG_SHOW_REMOTE_CONTROL));
    }

    public void setRemoteControl(boolean remoteControl) {
        myConfigValues.put(CFG_SHOW_REMOTE_CONTROL, Boolean.toString(remoteControl));
    }

    public boolean isAddRemoteControl() {
        return Boolean.parseBoolean(myConfigValues.get(CFG_SHOW_ADD_REMOTE_CONTROL));
    }

    public void setAddRemoteControl(boolean addRemoteControl) {
        myConfigValues.put(CFG_SHOW_ADD_REMOTE_CONTROL, Boolean.toString(addRemoteControl));
    }

    public String getActiveTranscoders() {
        return myConfigValues.get(CFG_ACTIVE_TRANSCODERS);
    }

    public void setActiveTranscoders(String activeTranscoders) {
        myConfigValues.put(CFG_ACTIVE_TRANSCODERS, activeTranscoders);
    }

    public boolean isKeepAlive() {
        return Boolean.valueOf(myConfigValues.get(CFG_KEEP_ALIVE));
    }

    public void setKeepAlive(boolean keepAlive) {
        myConfigValues.put(CFG_KEEP_ALIVE, Boolean.toString(keepAlive));
    }

    public int getSearchFuzziness() {
        return Integer.parseInt(myConfigValues.get(CFG_SEARCH_FUZZINESS));
    }

    public void setSearchFuzziness(int searchFuzziness) {
        myConfigValues.put(CFG_SEARCH_FUZZINESS, Integer.toString(searchFuzziness));
    }

    public boolean isShowExternalSites() {
        return Boolean.parseBoolean(myConfigValues.get(CFG_SHOW_EXTERNAL_SITES));
    }

    public void setShowExternalSites(boolean showExternalSites) {
        myConfigValues.put(CFG_SHOW_EXTERNAL_SITES, Boolean.toString(showExternalSites));
    }

    public boolean isShowAddToPlaylist() {
        return Boolean.parseBoolean(myConfigValues.get(CFG_SHOW_ADD_TO_PLAYLIST));
    }

    public void setShowAddToPlaylist(boolean showAddToPlaylist) {
        myConfigValues.put(CFG_SHOW_ADD_TO_PLAYLIST, Boolean.toString(showAddToPlaylist));
    }

    public int getPhotoSize() {
        int size = Integer.parseInt(myConfigValues.get(CFG_PHOTO_SIZE));
        switch (size) {
            case 25:
            case 50:
            case 75:
                return size;
            default:
                return 100;
        }
    }

    public void setPhotoSize(int photoSize) {
        myConfigValues.put(CFG_PHOTO_SIZE, Integer.toString(photoSize));
    }

    public int getMaxSearchResults() {
        return Integer.parseInt(myConfigValues.get(CFG_MAX_SEARCH_RESULTS));
    }

    public void setMaxSearchResults(int maxSearchResults) {
        myConfigValues.put(CFG_MAX_SEARCH_RESULTS, Integer.toString(maxSearchResults));
    }

    public int getPhotoJpegQuality() {
        return Integer.parseInt(myConfigValues.get(CFG_PHOTO_JPEG_QUALITY));
    }

    public void setPhotoJpegQuality(int photoJpegQuality) {
        myConfigValues.put(CFG_PHOTO_JPEG_QUALITY, Integer.toString(photoJpegQuality));
    }
}
