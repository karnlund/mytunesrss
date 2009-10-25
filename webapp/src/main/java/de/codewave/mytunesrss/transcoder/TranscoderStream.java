package de.codewave.mytunesrss.transcoder;

import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.TranscoderConfig;
import de.codewave.mytunesrss.datastore.statement.FindTrackImageQuery;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.mytunesrss.meta.Image;
import de.codewave.utils.io.LogStreamCopyThread;
import de.codewave.utils.sql.DataStoreSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * de.codewave.mytunesrss.transcoder.TranscoderStream
 */
public class TranscoderStream extends InputStream {
    private static final Logger LOG = LoggerFactory.getLogger(TranscoderStream.class);

    private Process myProcess;
    private TranscoderConfig myTranscoderConfig;

    public TranscoderStream(TranscoderConfig transcoderConfig, Track track)
            throws IOException {
        myTranscoderConfig = transcoderConfig;
        final String[] transcoderCommand = new String[getArguments().split(" ").length + 1];
        transcoderCommand[0] = transcoderConfig.getBinary();
        int i = 1;
        for (String part : getArguments().split(" ")) {
            transcoderCommand[i++] = part;
        }
        //final File tempFile = File.createTempFile("mytunesrss-", "." + FilenameUtils.getExtension(track.getFile().getName()));
        //tempFile.deleteOnExit();
        //FileUtils.copyFile(track.getFile(), tempFile);
        //track.setFile(tempFile);
        replaceTokens(transcoderCommand, track);
        if (LOG.isDebugEnabled()) {
            LOG.debug("executing " + getName() + " command \"" + StringUtils.join(transcoderCommand, " ") + "\".");
        }
        myProcess = Runtime.getRuntime().exec(transcoderCommand);
        new LogStreamCopyThread(myProcess.getErrorStream(), false, LoggerFactory.getLogger(getClass()), LogStreamCopyThread.LogLevel.Debug)
                .start();
    }

    public int read() throws IOException {
        return myProcess.getInputStream().read();
    }

    @Override
    public void close() throws IOException {
        myProcess.destroy();
        super.close();
    }

    protected String getName() {
        return myTranscoderConfig.getName();
    }

    protected String getArguments() {
        return myTranscoderConfig.getOptions();
    }

    public static void replaceTokens(String[] command, Track track) {
        for (int i = 0; i < command.length; i++) {
            if ("{info.album}".equals(command[i])) {
                command[i] = track.getAlbum();
            } else if ("{info.artist}".equals(command[i])) {
                command[i] = track.getOriginalArtist();
            } else if ("{info.track}".equals(command[i])) {
                command[i] = track.getName();
            } else if ("{info.genre}".equals(command[i])) {
                command[i] = track.getGenre();
            } else if ("{info.comment}".equals(command[i])) {
                command[i] = track.getComment();
            } else if ("{info.pos.number}".equals(command[i])) {
                command[i] = Integer.toString(track.getPosNumber());
            } else if ("{info.pos.size}".equals(command[i])) {
                command[i] = Integer.toString(track.getPosSize());
            } else if ("{info.time}".equals(command[i])) {
                command[i] = Integer.toString(track.getTime());
            } else if ("{info.track.number}".equals(command[i])) {
                command[i] = Integer.toString(track.getTrackNumber());
            } else if ("{info.image.file}".equals(command[i])) {
                replaceImageToken(track, command, i);
            } else if ("{infile}".equals(command[i])) {
                try {
                    command[i] = track.getFile().getCanonicalPath();
                } catch (IOException e) {
                    LOG.warn("Could not get canonical path for track file \"" + track.getFile().getName() + "\", trying absolute path instead.");
                    command[i] = track.getFile().getAbsolutePath();
                }
            }
        }
    }

    private static void replaceImageToken(Track track, String[] command, int i) {
        try {
            File imageFile = File.createTempFile("mytunesrss-temp-image", ".jpg");
            imageFile.deleteOnExit();
            DataStoreSession transaction = MyTunesRss.STORE.getTransaction();
            byte[] data = new byte[0];
            try {
                data = transaction.executeQuery(new FindTrackImageQuery(track.getId(), 128));
                if (data != null && data.length > 0) {
                    Image image = new Image("image/jpeg", data);
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(imageFile);
                        fos.write(image.getData());
                    } catch (IOException e) {
                        if (LOG.isErrorEnabled()) {
                            LOG.error("Could not create image file.", e);
                        }

                    } finally {
                        IOUtils.closeQuietly(fos);
                    }
                }
            } catch (SQLException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Could not query image data.", e);
                }
            } finally {
                transaction.commit();
            }
            try {
                command[i] = imageFile.getCanonicalPath();
            } catch (IOException e) {
                command[i] = imageFile.getAbsolutePath();
            }
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Could not create temp file for image.", e);
            }
        }
    }
}