package de.codewave.mytunesrss.command;

import de.codewave.mytunesrss.meta.Image;
import de.codewave.utils.graphics.ImageUtils;
import de.codewave.utils.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * de.codewave.mytunesrss.command.ShowTrackImageCommandHandler
 */
public class ShowImageCommandHandler extends MyTunesRssCommandHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ShowImageCommandHandler.class);
    private Map<Integer, byte[]> myDefaultImages = new HashMap<Integer,byte[]>();

    private byte[] getDefaultImage(int size) {
        if (myDefaultImages.get(size) == null) {
            synchronized(myDefaultImages) {
                if (myDefaultImages.get(size) == null) {
                    InputStream inputStream = MyTunesRssCommandHandler.class.getClassLoader().getResourceAsStream(
                            "de/codewave/mytunesrss/default_rss_image.png");
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        for (int dataByte = inputStream.read(); dataByte > -1 && dataByte < 256; dataByte = inputStream.read()) {
                            outputStream.write(dataByte);
                        }
                        myDefaultImages.put(size, ImageUtils.resizeImageWithMaxSize(outputStream.toByteArray(), size));
                    } catch (IOException e) {
                        if (LOG.isErrorEnabled()) {
                            LOG.error("Could not copy default image data into byte array.", e);
                        }
                    } finally {
                        IOUtils.close(inputStream);
                        IOUtils.close(outputStream);
                    }
                }
            }
        }
        return myDefaultImages.get(size);
    }

    protected void sendDefaultImage(int size) throws IOException {
        byte[] defaultImage = getDefaultImage(size);
        if (defaultImage != null) {
            getResponse().setContentType("image/jpeg");
            getResponse().setContentLength(defaultImage.length);
            getResponse().getOutputStream().write(defaultImage);
        } else {
            getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    protected void sendImage(Image image) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending image with mime type \"" + image.getMimeType() + "\".");
        }
        getResponse().setContentType(image.getMimeType());
        getResponse().setContentLength(image.getData().length);
        getResponse().getOutputStream().write(image.getData());
    }
}