package de.codewave.mytunesrss;

import de.codewave.mytunesrss.command.MyTunesRssCommand;
import de.codewave.mytunesrss.config.MediaType;
import de.codewave.mytunesrss.config.TranscoderConfig;
import de.codewave.mytunesrss.config.User;
import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.jsp.Error;
import de.codewave.mytunesrss.jsp.MyTunesRssResource;
import de.codewave.mytunesrss.remote.MyTunesRssRemoteEnv;
import de.codewave.mytunesrss.remote.Session;
import de.codewave.mytunesrss.servlet.WebConfig;
import de.codewave.mytunesrss.transcoder.Transcoder;
import de.codewave.utils.Base64Utils;
import de.codewave.utils.servlet.ServletUtils;
import de.codewave.utils.servlet.StreamSender;
import de.codewave.utils.sql.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * <b>Description:</b>   <br> <b>Copyright:</b>     Copyright (c) 2006<br> <b>Company:</b>       daGama Business Travel GmbH<br> <b>Creation Date:</b>
 * 08.11.2006
 *
 * @author Michael Descher
 * @version $Id:$
 */
public class MyTunesRssWebUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyTunesRssWebUtils.class);

    public static String getApplicationUrl(HttpServletRequest request) {
        return ServletUtils.getApplicationUrl(request);
    }

    public static String getServletUrl(HttpServletRequest request) {
        return getApplicationUrl(request) + "/mytunesrss";
    }

    public static User getAuthUser(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("authUser");
        if (user == null) {
            user = (User) request.getAttribute("authUser");
            if (user == null) {
                Session session = MyTunesRssRemoteEnv.getSession();
                user = session != null ? session.getUser() : null;
            }
        }
        return user;
    }

    /**
     * Encrypt the path info. The parts of the path info are expected to be url encoded already.
     * Any %2F and %5C will be replaced by %01 and %02 since tomcat does not like those characters in the path info.
     * So the path info decoder will have to replace %01 and %02 with %2F and %5C.
     *
     * @param request
     * @param pathInfo
     * @return
     */
    public static String encryptPathInfo(HttpServletRequest request, String pathInfo) {
        String result = pathInfo;
        try {
            if (MyTunesRss.CONFIG.getPathInfoKey() != null) {
                Cipher cipher = Cipher.getInstance(MyTunesRss.CONFIG.getPathInfoKey().getAlgorithm());
                cipher.init(Cipher.ENCRYPT_MODE, MyTunesRss.CONFIG.getPathInfoKey());
                result = "%7B" + MyTunesRssBase64Utils.encode(cipher.doFinal(pathInfo.getBytes("UTF-8"))) + "%7D";
            }
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Could not encrypt path info.", e);
            }
        }
        // replace %2F and %5C with %01 and %02 for the reason specified in the java doc
        return result.replace("%2F", "%01").replace("%2f", "%01").replace("%5C", "%02").replace("%5c", "%02");
    }

    public static WebConfig getWebConfig(HttpServletRequest httpServletRequest) {
        WebConfig webConfig = (WebConfig) httpServletRequest.getAttribute("config");
        if (webConfig == null) {
            webConfig = (WebConfig) httpServletRequest.getSession().getAttribute("config");
            if (webConfig == null) {
                webConfig = new WebConfig();
                webConfig.clearWithDefaults(httpServletRequest);
                webConfig.load(httpServletRequest, getAuthUser(httpServletRequest));
                httpServletRequest.getSession().setAttribute("config", webConfig);
                LOGGER.debug("Created session configuration.");
            }
            httpServletRequest.setAttribute("config", webConfig);
            LOGGER.debug("Created request configuration: " + new HashMap<String, String>(webConfig.getMap()).toString());
        }
        String activeTranscodersFromRequest = MyTunesRssWebUtils.getActiveTranscodingFromRequest(httpServletRequest);
        if (activeTranscodersFromRequest != null) {
            webConfig.setActiveTranscoders(activeTranscodersFromRequest);
        }
        return webConfig;
    }

    public static void addError(HttpServletRequest request, Error error, String holderName) {
        Set<Error> errors = (Set<Error>) request.getSession().getAttribute(holderName);
        if (errors == null) {
            synchronized (request.getSession()) {
                errors = (Set<Error>) request.getSession().getAttribute(holderName);
                if (errors == null) {
                    errors = new LinkedHashSet<Error>();
                    request.getSession().setAttribute(holderName, errors);
                }
            }
        }
        errors.add(error);
    }

    public static boolean isError(HttpServletRequest request, String holderName) {
        Set<Error> errors = (Set<Error>) request.getSession().getAttribute(holderName);
        return errors != null && !errors.isEmpty();
    }

    private static boolean isUserAgentPsp(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return StringUtils.isNotEmpty(userAgent) && userAgent.contains("PSP");
    }

    private static boolean isUserAgentIphone(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return StringUtils.isNotEmpty(userAgent) && userAgent.contains("iPhone");
    }

    private static boolean isUserAgentSafari(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return StringUtils.isNotEmpty(userAgent) && userAgent.contains("Safari");
    }

    private static boolean isUserAgentNintendoWii(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return StringUtils.isNotEmpty(userAgent) && userAgent.contains("Nintendo Wii");
    }

    public static UserAgent getUserAgent(HttpServletRequest request) {
        if (isUserAgentPsp(request)) {
            return UserAgent.Psp;
        } else if (isUserAgentIphone(request)) {
            return UserAgent.Iphone;
        } else if (isUserAgentNintendoWii(request)) {
            return UserAgent.NintendoWii;
        }
        return UserAgent.Unknown;
    }

    public static String getCommandCall(HttpServletRequest request, MyTunesRssCommand command) {
        String servletUrl = getServletUrl(request);
        return MyTunesRssWebUtils.getApplicationUrl(request) + servletUrl.substring(servletUrl.lastIndexOf("/")) + "/" + command.getName();
    }

    public static String getResourceCommandCall(HttpServletRequest request, MyTunesRssResource resource) {
        String servletUrl = getServletUrl(request);
        return MyTunesRssWebUtils.getApplicationUrl(request) + servletUrl.substring(servletUrl.lastIndexOf("/")) + "/" + MyTunesRssCommand.ShowResource.getName() + "/resource=" + resource.name();
    }

    public static String createTranscodingPathInfo(WebConfig config) {
        return createTranscodingParamValue(StringUtils.split(StringUtils.trimToEmpty(config.getActiveTranscoders()), ','));
    }

    public static String createTranscodingParamValue(String[] transcoderNames) {
        StringBuilder tc = new StringBuilder();
        for (String tcName : transcoderNames) {
            tc.append(tcName).append(",");
        }
        return tc.length() > 0 ? tc.substring(0, tc.length() - 1) : "";
    }

    public static String getActiveTranscodingFromRequest(HttpServletRequest request) {
        return request.getParameter("tc");
    }

    /**
     * Move tracks in the playlist to another position.
     *
     * @param playlistTracks List of tracks.
     * @param first          Index of first track to move (0-based).
     * @param count          Number of tracks to move.
     * @param offset         Offset to move, can be positive to move downwards or negative to move upwards.
     */
    public static void movePlaylistTracks(List<Track> playlistTracks, int first, int count, int offset) {
        for (int i = 0; i < Math.abs(offset); i++) {
            for (int k = 0; k < count; k++) {
                int swapLeft;
                if (offset < 0) {
                    swapLeft = first + k - 1;
                } else {
                    swapLeft = first + count - k - 1;
                }
                if (swapLeft >= 0 && swapLeft + 1 < playlistTracks.size()) {
                    Track tempTrack = playlistTracks.get(swapLeft);
                    playlistTracks.set(swapLeft, playlistTracks.get(swapLeft + 1));
                    playlistTracks.set(swapLeft + 1, tempTrack);
                } else {
                    break;
                }
            }
            first += Math.signum(offset);
        }
    }

    public static void createParameterModel(HttpServletRequest request, String... parameterNames) {
        for (String parameterName : parameterNames) {
            String[] parts = parameterName.split("\\.");
            Map map = null;
            for (int i = 0; i < parts.length; i++) {
                if (i < parts.length - 1) {
                    if (map == null) {
                        map = (Map) request.getAttribute(parts[i]);
                        if (map == null) {
                            map = new HashMap();
                            request.setAttribute(parts[i], map);
                        }
                    } else {
                        if (!map.containsKey(parts[i])) {
                            map.put(parts[i], new HashMap());
                        }
                        map = (Map) map.get(parts[i]);
                    }
                } else {
                    if (map == null) {
                        request.setAttribute(parts[i], request.getParameter(parameterName));
                    } else {
                        map.put(parts[i], request.getParameter(parameterName));
                    }
                }
            }
        }
    }

    public static String getBundleString(HttpServletRequest request, String key) {
        LocalizationContext context = (LocalizationContext) request.getSession().getAttribute(Config.FMT_LOCALIZATION_CONTEXT + ".session");
        ResourceBundle bundle = context != null ? context.getResourceBundle() : ResourceBundle.getBundle("de/codewave/mytunesrss/MyTunesRssWeb",
                request.getLocale());
        return bundle.getString(key);
    }

    /**
     * Get language from cookie.
     *
     * @param request Servlet request.
     * @return Language from cookie or NULL if no cookie was found.
     */
    public static String getCookieLanguage(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if ((MyTunesRss.APPLICATION_IDENTIFIER + "Language").equals(cookie.getName())) {
                    return StringUtils.trimToNull(Base64Utils.decodeToString(cookie.getValue()));
                }
            }
        }
        return null;
    }

    /**
     * Set the language cookie.
     *
     * @param request      Servlet request.
     * @param response     Servlet response.
     * @param languageCode Language code.
     */
    public static void setCookieLanguage(HttpServletRequest request, HttpServletResponse response, String languageCode) {
        Cookie cookie;
        if (StringUtils.isNotBlank(languageCode)) {
            cookie = new Cookie(MyTunesRss.APPLICATION_IDENTIFIER + "Language", Base64Utils.encode(StringUtils.trim(languageCode)));
            cookie.setVersion(1);
            cookie.setMaxAge(3600 * 24 * 365); // one year
        } else {
            cookie = new Cookie(MyTunesRss.APPLICATION_IDENTIFIER + "Language", "");
            cookie.setVersion(1);
            cookie.setMaxAge(0); // expire now
        }
        cookie.setComment("MyTunesRSS language cookie");
        String servletUrl = MyTunesRssWebUtils.getServletUrl(request);
        cookie.setPath(servletUrl.substring(servletUrl.lastIndexOf("/")));
        response.addCookie(cookie);
    }

    public static void saveWebConfig(HttpServletRequest request, HttpServletResponse response, User user, WebConfig webConfig) {
        if (user != null && !user.isSharedUser()) {
            // save in user profile on server
            user.setWebConfig(MyTunesRssWebUtils.getUserAgent(request), webConfig.createCookieValue());
            MyTunesRss.CONFIG.save(); // save new user settings
            webConfig.removeCookie(request, response); // remove cookie if it exists
        } else {
            // save in cookie
            webConfig.save(request, response);
        }
    }

    public static Transcoder getTranscoder(HttpServletRequest request, Track track) {
        boolean notranscode = "true".equals(request.getParameter("notranscode"));
        boolean tempFile = ServletUtils.isRangeRequest(request) || ServletUtils.isHeadRequest(request);
        User authUser = getAuthUser(request);
        return (authUser != null && authUser.isForceTranscoders()) || !notranscode ? Transcoder.createTranscoder(track, authUser, MyTunesRssWebUtils.getActiveTranscodingFromRequest(request), tempFile) : null;
    }

    public static InputStream getMediaStream(HttpServletRequest request, Track track, InputStream inputStream) throws IOException {
        Transcoder transcoder = getTranscoder(request, track);
        if (transcoder != null) {
            return transcoder.getStream(inputStream);
        } else {
            return inputStream;
        }
    }

    public static StreamSender getMediaStreamSender(HttpServletRequest request, Track track, InputStream inputStream) throws IOException {
        Transcoder transcoder = getTranscoder(request, track);
        if (transcoder != null) {
            return transcoder.getStreamSender(inputStream);
        } else {
            return new StreamSender(inputStream, track.getContentType(), track.getContentLength());
        }
    }

    public static boolean isHttpLiveStreaming(HttpServletRequest request, Track track, boolean ignoreContentType) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Checking for HTTP Live Streaming.");
        }
        if (MyTunesRss.HTTP_LIVE_STREAMING_AVAILABLE && getUserAgent(request) == UserAgent.Iphone && track.getMediaType() == MediaType.Video) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("HTTP Live Streaming available, user agent is iPhone and media type is video.");
            }
            if (ignoreContentType) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Ignoring content type of track or transcoder.");
                }
                return true;
            }
            Transcoder transcoder = getTranscoder(request, track);
            if (transcoder != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Transcoder content type is \"" + transcoder.getTargetContentType() + "\".");
                }
                return "video/MP2T".equalsIgnoreCase(transcoder.getTargetContentType());
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Track content type is \"" + track.getContentType() + "\".");
                }
                return "video/MP2T".equalsIgnoreCase(track.getContentType());
            }
        }
        return false;
    }

    public static void rememberLogin(HttpServletRequest request, HttpServletResponse response, String username, byte[] passwordHash) {
        try {
            StringBuilder cookieValue = new StringBuilder(Base64.encodeBase64String(username.getBytes("UTF-8")).trim());
            cookieValue.append(";").append(new String(Base64.encodeBase64(passwordHash), "UTF-8").trim());
            response.addCookie(createLoginCookie(request, cookieValue.toString()));
        } catch (UnsupportedEncodingException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Character set UTF-8 not found.");
            }

        }
    }

    private static Cookie createLoginCookie(HttpServletRequest request, String cookieValue) {
        Cookie cookie = new Cookie(MyTunesRss.APPLICATION_IDENTIFIER + "User", Base64Utils.encode(cookieValue));
        cookie.setVersion(1);
        cookie.setComment("MyTunesRSS user cookie");
        cookie.setMaxAge(3600 * 24 * 60);// 60 days
        String servletUrl = MyTunesRssWebUtils.getServletUrl(request);
        cookie.setPath(servletUrl.substring(servletUrl.lastIndexOf("/")));
        return cookie;
    }

    public static void forgetLogin(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = createLoginCookie(request, "");
        cookie.setMaxAge(0); // delete cookie
        response.addCookie(cookie);
    }

    public static String getRememberedUsername(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (StringUtils.equals(cookie.getName(), MyTunesRss.APPLICATION_IDENTIFIER + "User")) {
                    try {
                        return new String(Base64.decodeBase64(Base64Utils.decodeToString(cookie.getValue()).split(";")[0]), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        if (LOGGER.isErrorEnabled()) {
                            LOGGER.error("Character set UTF-8 not found.");
                        }
                    }
                }
            }
        }
        return null;
    }

    public static byte[] getRememberedPasswordHash(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (StringUtils.equals(cookie.getName(), MyTunesRss.APPLICATION_IDENTIFIER + "User")) {
                    try {
                        return Base64.decodeBase64(Base64Utils.decodeToString(cookie.getValue()).split(";")[1]);
                    } catch (Exception e) {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("Could not get password from user cookie value.", e);
                        }
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public static TranscoderConfig getTranscoder(String activeTranscoders, Track track) {
        for (TranscoderConfig config : MyTunesRss.CONFIG.getTranscoderConfigs()) {
            if (isActiveTranscoder(activeTranscoders, config.getName()) && config.isValidFor(track.getFilename(), track.getMp4Codec())) {
                return config;
            }
        }
        return null;
    }

    public static boolean isActiveTranscoder(String activeTranscoders, String transcoder) {
        return ArrayUtils.contains(StringUtils.split(activeTranscoders, ','), transcoder);
    }

    /**
     * Find the user's random playlist.
     *
     * @param transaction
     * @param user
     *
     * @return The user's random playlist or NULL if none exists.
     *
     * @throws SQLException
     */
    public static Playlist findRandomPlaylist(DataStoreSession transaction, final User user) throws SQLException {
        return transaction.executeQuery(new DataStoreQuery<Playlist>() {
            @Override
            public Playlist execute(Connection connection) throws SQLException {
                SmartStatement query = MyTunesRssUtils.createStatement(connection, "findRandomPlaylist");
                query.setString("username", user.getName());
                return execute(query, new FindPlaylistQuery.PlaylistResultBuilder()).getResult(0);
            }
        });

    }

    /**
     * Create a random playlist in the database and return the playlist representation.
     *
     * @param transaction
     * @param user
     * @param webConfig
     * @param playlistName
     *
     * @return
     *
     * @throws SQLException
     */
    public static Playlist createRandomPlaylist(DataStoreSession transaction, final User user, final WebConfig webConfig, String playlistName) throws SQLException {
        List<String> trackIds = transaction.executeQuery(new DataStoreQuery<List<String>>() {
            @Override
            public List<String> execute(Connection connection) throws SQLException {
                Map<String, Boolean> conditionals = new HashMap<String, Boolean>();
                conditionals.put("restricted", !user.getRestrictedPlaylistIds().isEmpty());
                conditionals.put("excluded", !user.getExcludedPlaylistIds().isEmpty());
                conditionals.put("sourceplaylist", StringUtils.isNotBlank(webConfig.getRandomSource()));
                conditionals.put("mediatype", StringUtils.isNotBlank(webConfig.getRandomMediaType()));
                conditionals.put("unprotectedonly", !webConfig.isRandomProtected());
                SmartStatement query = MyTunesRssUtils.createStatement(connection, "findRandomTracks", conditionals, ResultSetType.TYPE_SCROLL_INSENSITIVE);
                query.setString("mediatype", webConfig.getRandomMediaType());
                query.setInt("maxCount", webConfig.getRandomPlaylistSize());
                query.setString("sourcePlaylistId", webConfig.getRandomSource());
                query.setItems("restrictedPlaylistIds", user.getRestrictedPlaylistIds());
                query.setItems("excludedPlaylistIds", user.getExcludedPlaylistIds());
                return execute(query, new ResultBuilder<String>() {
                    public String create(ResultSet resultSet) throws SQLException {
                        return resultSet.getString("ID");
                    }
                }).getResults();
            }
        });
        Playlist randomPlaylist = findRandomPlaylist(transaction, user);
        SaveRandomPlaylistStatement statement = new SaveRandomPlaylistStatement();
        String playlistId = randomPlaylist != null ? randomPlaylist.getId() : "RANDOM_" + UUID.randomUUID();
        statement.setUpdate(randomPlaylist != null);
        statement.setId(playlistId);
        statement.setName(playlistName);
        statement.setTrackIds(trackIds);
        statement.setUserName(user.getName());
        statement.setUserPrivate(true);
        transaction.executeStatement(statement);
        return new Playlist(playlistId, PlaylistType.Random, playlistName, webConfig.getRandomPlaylistSize());
    }
}
