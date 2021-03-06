/*
 * Copyright (c) 2014. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.mediaserver;

import com.google.common.collect.ImmutableList;
import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssBase64Utils;
import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.config.User;
import de.codewave.mytunesrss.config.transcoder.TranscoderConfig;
import de.codewave.mytunesrss.datastore.statement.Album;
import de.codewave.mytunesrss.datastore.statement.Photo;
import de.codewave.mytunesrss.datastore.statement.PhotoAlbum;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.utils.MiscUtils;
import de.codewave.utils.sql.DataStoreSession;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.binding.xml.Descriptor;
import org.fourthline.cling.support.model.*;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.MusicAlbum;
import org.fourthline.cling.support.model.dlna.*;
import org.fourthline.cling.support.model.item.Movie;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.seamless.util.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;

public abstract class MyTunesRssDIDL extends DIDLContent {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyTunesRssDIDL.class);
    public static final DLNAProfiles[] DLNA_JPEG_PROFILES = new DLNAProfiles[]{DLNAProfiles.JPEG_MED, DLNAProfiles.JPEG_SM, DLNAProfiles.JPEG_TN};
    public static final DLNAProfiles[] DLNA_PNG_PROFILES = new DLNAProfiles[]{DLNAProfiles.PNG_LRG, DLNAProfiles.PNG_LRG, DLNAProfiles.PNG_TN};

    final void initDirectChildren(String oidParams, long firstResult, long maxResults) throws SQLException {
        DataStoreSession tx = MyTunesRss.STORE.getTransaction();
        try {
            createDirectChildren(getUser(), tx, oidParams, firstResult, maxResults);
        } finally {
            tx.rollback();
        }
    }

    final void initMetaData(String oidParams) throws SQLException {
        DataStoreSession tx = MyTunesRss.STORE.getTransaction();
        try {
            createMetaData(getUser(), tx, oidParams);
        } finally {
            tx.rollback();
        }
    }

    abstract void createDirectChildren(User user, DataStoreSession tx, String oidParams, long firstResult, long maxResults) throws SQLException;

    protected Container createSimpleContainer(String id , String parentId, String title, int childCount) {
        Container container = createSimpleContainer(id, parentId, childCount);
        container.setTitle(title);
        return container;
    }

    protected Container createSimpleContainer(String id , String parentId, int childCount) {
        Container container = new Container();
        container.setId(id);
        container.setParentID(parentId);
        container.setChildCount(childCount);
        container.setClazz(new DIDLObject.Class("object.container"));
        return container;
    }

    protected Res createTrackResource(Track track, User user) {
        StringBuilder builder = createWebAppCall(user, "playTrack"); // TODO hard-coded command name is not nice
        StringBuilder pathInfo = new StringBuilder("track=");
        pathInfo.append(MiscUtils.getUtf8UrlEncoded(track.getId()));
        TranscoderConfig transcoder = null;
        for (TranscoderConfig config : getClientProfile().getTranscodersConfigs()) {
            transcoder = MyTunesRssUtils.getTranscoder(config.getName(), track);
            if (transcoder != null) {
                pathInfo.append("/tc=").append(transcoder.getName());
            }
        }
        builder.append("/").
                append(MyTunesRssUtils.encryptPathInfo(pathInfo.toString()));
        builder.append("/").
                append(MiscUtils.getUtf8UrlEncoded(MyTunesRssUtils.virtualTrackName(track))).
                append(".").
                append(MiscUtils.getUtf8UrlEncoded(transcoder != null ? transcoder.getTargetSuffix() : FilenameUtils.getExtension(track.getFilename())));
        Res res = new Res();
        MimeType mimeType = MimeType.valueOf(transcoder != null ? transcoder.getTargetContentType() : track.getContentType());
        EnumMap<DLNAAttribute.Type, DLNAAttribute> attributes = new EnumMap<>(DLNAAttribute.Type.class);
        attributes.put(DLNAAttribute.Type.DLNA_ORG_OP, new DLNAOperationsAttribute(DLNAOperations.RANGE));
        attributes.put(DLNAAttribute.Type.DLNA_ORG_CI, new DLNAConversionIndicatorAttribute(transcoder != null ? DLNAConversionIndicator.TRANSCODED : DLNAConversionIndicator.NONE));
        attributes.put(DLNAAttribute.Type.DLNA_ORG_FLAGS, new DLNAFlagsAttribute(DLNAFlags.STREAMING_TRANSFER_MODE, DLNAFlags.BACKGROUND_TRANSFERT_MODE, DLNAFlags.DLNA_V15));
        DLNAProtocolInfo protocolInfo = new DLNAProtocolInfo(Protocol.HTTP_GET, "*", mimeType.toStringNoParameters(), attributes);
        res.setProtocolInfo(protocolInfo);
        if (transcoder == null) {
            res.setSize(track.getContentLength());
        }
        res.setDuration(toHumanReadableTime(track.getTime()));
        res.setValue(builder.toString());
        LOGGER.debug("Resource value is \"" + res.getValue() + "\" with protocol info \"" + protocolInfo + "\".");
        return res;
    }

    private StringBuilder createWebAppCall(User user, String command) {
        StringBuilder builder = new StringBuilder("http://");
        String hostAddress = StringUtils.defaultIfBlank(MyTunesRss.CONFIG.getHost(), AbstractContentDirectoryService.REMOTE_CLIENT_INFO.get().getLocalAddress().getHostAddress());
        builder.append(hostAddress).
                append(":").append(MyTunesRss.CONFIG.getPort());
        String context = StringUtils.trimToEmpty(MyTunesRss.CONFIG.getWebappContext());
        if (!context.startsWith("/")) {
            builder.append("/");
        }
        builder.append(context);
        if (context.length() > 0 && !context.endsWith("/")) {
            builder.append("/");
        }
        builder.append("mytunesrss/").
                append(command).
                append("/").
                append(MyTunesRssUtils.createAuthToken(user));
        return builder;
    }

    protected URI getImageUri(User user, int size, String imageHash) {
        File image = MyTunesRssUtils.getImage(imageHash, size);
        if (StringUtils.isNotBlank(imageHash) && image != null && image.isFile() && image.canRead()) {
            StringBuilder builder = createWebAppCall(user, "showImage"); // TODO hard-coded command name is not nice
            StringBuilder pathInfo = new StringBuilder();
            pathInfo.append("hash=").append(MiscUtils.getUtf8UrlEncoded(imageHash));
            if (size > 0) {
                pathInfo.append("/size=").append(Integer.toString(size));
            }
            builder.append("/").append(MyTunesRssUtils.encryptPathInfo(pathInfo.toString()));
            try {
                return new URI(builder.toString());
            } catch (URISyntaxException e) {
                LOGGER.warn("Could not create URI for image.", e);
            }
        }
        return null;
    }

    abstract void createMetaData(User user, DataStoreSession tx, String oidParams) throws SQLException;

    abstract long getTotalMatches();

    String encode(String... strings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : strings) {
            stringBuilder.append(MyTunesRssBase64Utils.encode(s)).append(";");
        }
        return stringBuilder.length() > 0 ? stringBuilder.substring(0, stringBuilder.length() - 1) : stringBuilder.toString();
    }

    List<String> decode(String s) {
        List<String> decoded = new ArrayList<>();
        for (String encoded : StringUtils.split(s, ";")) {
            decoded.add(MyTunesRssBase64Utils.decodeToString(encoded));
        }
        return decoded;
    }

    protected String toHumanReadableTime(int time) {
        int seconds = time % 60;
        int minutes = (time / 60) % 60;
        int hours = time / 3600;
        StringBuilder builder = new StringBuilder();
        builder.append(hours).append(":");
        builder.append(StringUtils.leftPad(Integer.toString(minutes), 2, '0')).append(":");
        builder.append(StringUtils.leftPad(Integer.toString(seconds), 2, '0')).append(".000");
        LOGGER.debug("Human readable of \"" + time + "\" is \"" + builder + "\".");
        return builder.toString();
    }

    protected MusicTrack createMusicTrack(User user, Track track, String objectId, String parentId) {
        MusicTrack musicTrack = new MusicTrack();
        musicTrack.setId(objectId);
        musicTrack.setParentID(parentId);
        musicTrack.setTitle(mapUnknown(track.getName()));
        musicTrack.setArtists(new PersonWithRole[]{new PersonWithRole(mapUnknown(track.getArtist()), "Performer")});
        musicTrack.setAlbum(mapUnknown(track.getAlbum()));
        musicTrack.setCreator("MyTunesRSS");
        musicTrack.setDescription(track.getName());
        musicTrack.setOriginalTrackNumber(track.getTrackNumber());
        musicTrack.setGenres(new String[]{track.getGenre()});
        List<Res> resources = new ArrayList<>();
        resources.add(createTrackResource(track, user));
        musicTrack.setResources(resources);
        addUpnpAlbumArtUri(user, track.getImageHash(), musicTrack);
        return musicTrack;
    }

    protected Movie createMovieTrack(User user, Track track, String objectId, String parentId) {
        Movie movie = new Movie();
        movie.setId(objectId);
        movie.setParentID(parentId);
        movie.setTitle(mapUnknown(track.getName()));
        movie.setCreator("MyTunesRSS");
        movie.setResources(Collections.singletonList(createTrackResource(track, user)));
        addUpnpAlbumArtUri(user, track.getImageHash(), movie);
        return movie;
    }

    protected Res createPhotoResource(User user, Photo photo, int size) {
        File file = new File(photo.getFile());
        String filename = file.getName();
        MimeType mimeType = MimeType.valueOf(MyTunesRssUtils.guessContentType(file));
        try {
            int maxSize = MyTunesRssUtils.getImageSize(photo).getMaxSize();
            if (size > 0 && size < maxSize) {
                filename = FilenameUtils.getBaseName(file.getName()) + ".jpg";
                mimeType = MimeType.valueOf("image/jpeg");
            }
        } catch (IOException ignored) {
            size = 0;
        }
        StringBuilder builder = createWebAppCall(user, "showPhoto");
        StringBuilder pathInfo = new StringBuilder("photo=").append(MiscUtils.getUtf8UrlEncoded(photo.getId()));
        if (size > 0) {
            pathInfo.append("/size=").append(size);
        }
        builder.append("/").append(MyTunesRssUtils.encryptPathInfo(pathInfo.toString())).append("/").append(MiscUtils.getUtf8UrlEncoded(filename));
        return new Res(mimeType, file.length(), builder.toString());
    }

    protected void addUpnpAlbumArtUri(User user, String imageHash, DIDLObject target) {
        DIDLObject.Property imageProperty = null;
        int maxSize = MyTunesRssUtils.getMaxSizedImageSize(imageHash);
        File maxSizedImage = MyTunesRssUtils.getMaxSizedImage(imageHash);
        if (maxSizedImage != null && maxSizedImage.isFile() && maxSizedImage.canRead()) {
            String originalImageContentType = MyTunesRssUtils.guessContentType(maxSizedImage);
            boolean jpeg = "image/jpeg".equalsIgnoreCase(originalImageContentType);
            boolean png = "image/png".equalsIgnoreCase(originalImageContentType);
            if (maxSize <= 768 && jpeg || png) {
                DLNAProfiles[] profiles = jpeg ? DLNA_JPEG_PROFILES : DLNA_PNG_PROFILES;
                URI mediumImage = getImageUri(user, maxSize, imageHash);
                if (mediumImage != null) {
                    String type = profiles[0].getCode();
                    if (maxSize <= 160) {
                        type = profiles[1].getCode();
                    } else if (maxSize <= 480) {
                        type = profiles[2].getCode();
                    }
                    DIDLObject.Property<DIDLAttribute> profileId = new DIDLObject.Property.DLNA.PROFILE_ID(
                            new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, Descriptor.Device.DLNA_PREFIX, type)
                    );
                    List<DIDLObject.Property<DIDLAttribute>> props = Collections.singletonList(profileId);
                    imageProperty = new DIDLObject.Property.UPNP.ALBUM_ART_URI(mediumImage, props);
                }
            }
        }
        if (imageProperty == null) {
            URI smallImage = getImageUri(user, 256, imageHash);
            if (smallImage != null) {
                DIDLObject.Property<DIDLAttribute> profileId = new DIDLObject.Property.DLNA.PROFILE_ID(
                        new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, Descriptor.Device.DLNA_PREFIX, DLNAProfiles.JPEG_SM.getCode())
                );
                List<DIDLObject.Property<DIDLAttribute>> props = Collections.singletonList(profileId);
                imageProperty = new DIDLObject.Property.UPNP.ALBUM_ART_URI(smallImage, props);
            }
        }
        if (imageProperty == null) {
            URI thumbnailImage = getImageUri(user, 128, imageHash);
            if (thumbnailImage != null) {
                DIDLObject.Property<DIDLAttribute> profileId = new DIDLObject.Property.DLNA.PROFILE_ID(
                        new DIDLAttribute(DIDLObject.Property.DLNA.NAMESPACE.URI, Descriptor.Device.DLNA_PREFIX, DLNAProfiles.JPEG_TN.getCode())
                );
                List<DIDLObject.Property<DIDLAttribute>> props = Collections.singletonList(profileId);
                imageProperty = new DIDLObject.Property.UPNP.ALBUM_ART_URI(thumbnailImage, props);
            }
        }
        if (imageProperty != null) {
            target.addProperty(imageProperty);
        }
    }

    protected MusicAlbum createMusicAlbum(User user, Album album, String objectId, String parentId) {
        MusicAlbum musicAlbum = new MusicAlbum(objectId, parentId, mapUnknown(album.getName()), mapUnknown(album.getArtist()), album.getTrackCount());
        musicAlbum.setArtists(new PersonWithRole[] {new PersonWithRole(mapUnknown(album.getArtist()), "Performer")});
        addUpnpAlbumArtUri(user, album.getImageHash(), musicAlbum);
        return musicAlbum;
    }

    protected String mapUnknown(String name) {
        return MyTunesRssUtils.isUnknown(name) ? "<unknown>" : name;
    }

    protected org.fourthline.cling.support.model.item.Photo createPhotoItem(Photo photo, PhotoAlbum photoAlbum, DateFormat dateFormat, User user, int size) {
        org.fourthline.cling.support.model.item.Photo photoItem = new org.fourthline.cling.support.model.item.Photo();
        photoItem.setId(ObjectID.Photo.getValue() + ";" + encode(photoAlbum.getId(), photo.getId()));
        photoItem.setParentID(ObjectID.PhotoAlbum.getValue() + ";" + encode(photoAlbum.getId()));
        photoItem.setTitle(photo.getName());
        photoItem.setAlbum(photoAlbum.getName());
        photoItem.setDate(dateFormat.format(new Date(photo.getDate())));
        photoItem.addResource(createPhotoResource(user, photo, size));
        return photoItem;
    }

    static MediaServerClientProfile getClientProfile() {
        String userAgent = AbstractContentDirectoryService.REMOTE_CLIENT_INFO.get().getRequestUserAgent();
        String clientIp = AbstractContentDirectoryService.REMOTE_CLIENT_INFO.get().getRemoteAddress().getHostAddress();
        MediaServerClientProfile clientProfile = MyTunesRss.MEDIA_SERVER_CONFIG.getClientProfile(userAgent, clientIp);
        if (clientProfile == null || clientProfile.getUser() == null) {
            // fallback to default if no user
            clientProfile = MyTunesRss.MEDIA_SERVER_CONFIG.getDefaultClientProfile();
        }
        return clientProfile;
    }

    static User getUser() {
        MediaServerClientProfile clientProfile = getClientProfile();
        User user = clientProfile.getUser();
        if (user == null) {
            // If the profile has no user name or the corresponding user does not exist anymore
            // and we only have a single user in the config, use that one.
            List<User> users = ImmutableList.copyOf(MyTunesRss.CONFIG.getUsers());
            if (users.size() == 1) {
                user = users.get(0);
            }
        }
        return user;
    }

    protected int getInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            LOGGER.warn("Could not parse int value.", e);
        }
        return defaultValue;
    }
}
