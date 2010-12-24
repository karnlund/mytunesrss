package de.codewave.mytunesrss;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for a flash player.
 */
public class FlashPlayerConfig implements Comparable<FlashPlayerConfig>, Cloneable {
    public static final String DEFAULT_PRE = "<html><head><title>MyTunesRSS Jukebox</title></head><body style=\"padding:0 0 0 0; margin:0 0 0 0\">";
    public static final String DEFAULT_POST = "</body></html>";
    public static final String DEFAULT_HTML = DEFAULT_PRE + "\n" + DEFAULT_POST;

    private static final FlashPlayerConfig JW46 = new FlashPlayerConfig("mytunesrss_jwmediaplayer", "JW Media Player 4.6", DEFAULT_PRE + "<embed src=\"mediaplayer-4-6.swf\" width=\"100%\" height=\"100%\" allowscriptaccess=\"always\" allowfullscreen=\"true\" flashvars=\"file={PLAYLIST_URL}&amp;linktarget=_blank&amp;playlist=right&amp;autostart=true&amp;playlistsize=350&amp;repeat=list\"/>" + DEFAULT_POST, PlaylistFileType.Xspf, 600, 276, TimeUnit.SECONDS);
    private static final FlashPlayerConfig JW46_SHUFFLE = new FlashPlayerConfig("mytunesrss_jwmediaplayer_shuffle", "JW Media Player 4.6 (Shuffle)", DEFAULT_PRE + "<embed src=\"mediaplayer-4-6.swf\" width=\"100%\" height=\"100%\" allowscriptaccess=\"always\" allowfullscreen=\"true\" flashvars=\"file={PLAYLIST_URL}&amp;linktarget=_blank&amp;playlist=right&amp;autostart=true&amp;playlistsize=350&amp;repeat=list&amp;shuffle=true\"/>" + DEFAULT_POST, PlaylistFileType.Xspf, 600, 276, TimeUnit.SECONDS);
    private static final FlashPlayerConfig SIMPLE = new FlashPlayerConfig("mytunesrss_simple", "XSPF Player", DEFAULT_PRE + "<embed src=\"xspf_player.swf?autoplay=true&amp;autoload=true&amp;playlist_url={PLAYLIST_URL}\" width=\"100%\" height=\"100%\" allowscriptaccess=\"always\" allowfullscreen=\"true\" flashvars=\"displaywidth=256\"/>" + DEFAULT_POST, PlaylistFileType.Xspf, 600, 276, TimeUnit.MILLISECONDS);

    public static final FlashPlayerConfig ABSOLUTE_DEFAULT = JW46;

    private static final Set<FlashPlayerConfig> DEFAULTS = new HashSet<FlashPlayerConfig>();

    static {
        DEFAULTS.add(JW46);
        DEFAULTS.add(JW46_SHUFFLE);
        DEFAULTS.add(SIMPLE);
    }

    public static Set<FlashPlayerConfig> getDefaults() {
        return new HashSet<FlashPlayerConfig>(DEFAULTS);
    }

    public static FlashPlayerConfig getDefault(String id) {
        for (FlashPlayerConfig flashPlayer : DEFAULTS) {
            if (flashPlayer.getId().equals(id)) {
                return flashPlayer;
            }
        }
        return JW46;
    }

    private String myId;
    private String myName;
    private String myHtml;
    private PlaylistFileType myPlaylistFileType;
    private int myWidth;
    private int myHeight;
    private TimeUnit myTimeUnit;

    public FlashPlayerConfig(String id, String name, String html, PlaylistFileType playlistFileType, int width, int height, TimeUnit timeUnit) {
        myId = id;
        myName = name;
        myHtml = html;
        myPlaylistFileType = playlistFileType;
        myWidth = width;
        myHeight = height;
        myTimeUnit = timeUnit;
    }

    public String getId() {
        return myId;
    }

    public void setId(String id) {
        myId = id;
    }

    public String getName() {
        return myName;
    }

    public void setName(String name) {
        myName = name;
    }

    public String getHtml() {
        return myHtml;
    }

    public void setHtml(String html) {
        myHtml = html;
    }

    public PlaylistFileType getPlaylistFileType() {
        return myPlaylistFileType;
    }

    public void setPlaylistFileType(PlaylistFileType playlistFileType) {
        myPlaylistFileType = playlistFileType;
    }

    public int getWidth() {
        return myWidth;
    }

    public void setWidth(int width) {
        myWidth = width;
    }

    public int getHeight() {
        return myHeight;
    }

    public void setHeight(int height) {
        myHeight = height;
    }

    public TimeUnit getTimeUnit() {
        return myTimeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        myTimeUnit = timeUnit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FlashPlayerConfig that = (FlashPlayerConfig) o;

        if (myId != null ? !myId.equals(that.myId) : that.myId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return myId != null ? myId.hashCode() : 0;
    }

    public int compareTo(FlashPlayerConfig o) {
        if (o == null || o.myName == null) {
            return 1;
        } else if (myName == null) {
            return -1;
        } else {
            return myName.compareTo(o.myName);
        }
    }

    @Override
    public Object clone() {
        return new FlashPlayerConfig(myId, myName, myHtml, myPlaylistFileType, myWidth, myHeight, myTimeUnit);
    }

    public File getBaseDir() throws IOException {
        return new File(MyTunesRssUtils.getPreferencesDataPath() + "/flashplayer", myId);
    }

}
