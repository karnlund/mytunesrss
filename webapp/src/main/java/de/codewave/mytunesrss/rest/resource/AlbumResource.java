/*
 * Copyright (c) 2012. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.rest.resource;

import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.rest.representation.AlbumRepresentation;
import de.codewave.mytunesrss.rest.representation.TrackRepresentation;
import de.codewave.mytunesrss.servlet.TransactionFilter;
import de.codewave.utils.sql.DataStoreQuery;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.spi.validation.ValidateRequest;

import javax.ws.rs.*;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ValidateRequest
@Path("artist/{artist}/album/{album}")
public class AlbumResource extends RestResource {

    /**
     * Get the representation of a single album specified by albuma artist and album name.
     *
     * @param artist The album artist.
     * @param album The album name.
     *
     * @return The representation of the album.
     *
     * @throws SQLException
     */
    @GET
    @Produces({"application/json"})
    @GZIP
    public AlbumRepresentation getAlbum(
            @PathParam("artist") String artist,
            @PathParam("album") String album
    ) throws SQLException {
        DataStoreQuery.QueryResult<Album> queryResult = TransactionFilter.getTransaction().executeQuery(new FindAlbumQuery(getAuthUser(), album, artist, true, null, -1, -1, -1, true, FindAlbumQuery.AlbumType.ALL));
        for (Album result = queryResult.nextResult(); result != null; result = queryResult.nextResult()) {
            if (result.getName().equals(album)) {
                return toAlbumRepresentation(result);
            }
        }
        return null;
    }

    /**
     * Get a list of tracks of an album specified by album artist and album name.
     *
     * @param artist The album artist.
     * @param album The album name.
     * @param sortOrder Sort order of the results (One of "Album", "Artist", "KeepOrder").
     *
     * @return List of track representations.
     *
     * @throws SQLException
     */
    @GET
    @Path("tracks")
    @Produces({"application/json"})
    @GZIP
    public List<TrackRepresentation> getAlbumTracks(
            @PathParam("artist") String artist,
            @PathParam("album") String album,
            @QueryParam("sort") @DefaultValue("KeepOrder") SortOrder sortOrder
    ) throws SQLException {
        DataStoreQuery.QueryResult<Track> queryResult = TransactionFilter.getTransaction().executeQuery(FindTrackQuery.getForAlbum(getAuthUser(), new String[]{album}, new String[]{artist}, sortOrder));
        return toTrackRepresentations(queryResult.getResults());
    }

    /**
     * Get a list of tags for the album, i.e. a list of tags for all tracks. All tags of all tracks of the album are returned,
     * i.e. not all tracks have all the tags returned but only at least one track has each of the tags.
     *
     * @param artist The album artist.
     * @param album The album name.
     *
     * @return List of all tags.
     *
     * @throws SQLException
     */
    @GET
    @Path("tags")
    @Produces({"application/json"})
    public List<String> getTags(
            @PathParam("artist") String artist,
            @PathParam("album") String album
    ) throws SQLException {
        DataStoreQuery.QueryResult<String> queryResult = TransactionFilter.getTransaction().executeQuery(new FindAllTagsForAlbumQuery(artist, album));
        return queryResult.getResults();
    }

    /**
     * Set a tag to all tracks of the album.
     *
     * @param artist The album artist.
     * @param album The album name.
     * @param tag The tag to set.
     *
     * @return List of all tags after adding the specified one.
     *
     * @throws SQLException
     */
    @PUT
    @Path("tag/{tag}")
    @Produces({"application/json"})
    public List<String> setTag(
            @PathParam("artist") String artist,
            @PathParam("album") String album,
            @PathParam("tag") String tag
    ) throws SQLException {
        TransactionFilter.getTransaction().executeStatement(new SetTagToTracksStatement(getTrackIds(artist, album), tag));
        return getTags(artist, album);
    }

    private String[] getTrackIds(String artist, String album) throws SQLException {
        List<TrackRepresentation> tracks = getAlbumTracks(artist, album, SortOrder.KeepOrder);
        Set<String> trackIds = new HashSet<String>();
        for (TrackRepresentation track : tracks) {
            trackIds.add(track.getId());
        }
        return trackIds.toArray(new String[trackIds.size()]);
    }

    /**
     * Delete a tag from all tracks of an album.
     *
     * @param artist The album artist.
     * @param album The album name.
     * @param tag The tag to delete.
     *
     * @return List of all tags after deleting the specified one.
     *
     * @throws SQLException
     */
    @DELETE
    @Path("tag/{tag}")
    @Produces({"application/json"})
    public List<String> deleteTag(
            @PathParam("artist") String artist,
            @PathParam("album") String album,
            @PathParam("tag") String tag
    ) throws SQLException {
        TransactionFilter.getTransaction().executeStatement(new RemoveTagFromTracksStatement(getTrackIds(artist, album), tag));
        return getTags(artist, album);
    }
}
