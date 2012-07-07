package de.codewave.mytunesrss.rest.representation;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.List;

/**
 * Settings of the current session.
 */
@XmlRootElement
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class SessionRepresentation implements RestRepresentation {

    private URI myLibraryUri;

    private List<String> myTranscoders;

    private List<BonjourDeviceRepresentation> myAirtunesTargets;

    private List<String> myPermissions;

    /**
     * Main library URI.
     */
    public URI getLibraryUri() {
        return myLibraryUri;
    }

    public void setLibraryUri(URI libraryUri) {
        myLibraryUri = libraryUri;
    }

    /**
     * List of available transcoders.
     */
    public List<String> getTranscoders() {
        return myTranscoders;
    }

    public void setTranscoders(List<String> transcoders) {
        myTranscoders = transcoders;
    }

    /**
     * List of available airtunes targets that can be used for the server local media player.
     */
    public List<BonjourDeviceRepresentation> getAirtunesTargets() {
        return myAirtunesTargets;
    }

    public void setAirtunesTargets(List<BonjourDeviceRepresentation> airtunesTargets) {
        myAirtunesTargets = airtunesTargets;
    }

    /**
     * List of permissions of the current user.
     */
    public List<String> getPermissions() {
        return myPermissions;
    }

    public void setPermissions(List<String> permissions) {
        myPermissions = permissions;
    }
}