package de.codewave.mytunesrss;

import de.codewave.mytunesrss.datastore.statement.SystemInformation;
import de.codewave.mytunesrss.datastore.statement.Track;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class AdminNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminNotifier.class);

    public void notifyDatabaseUpdate(long time, Map<String, Long> missingItunesFiles, SystemInformation systemInformation) {
        if (MyTunesRss.CONFIG.isNotifyOnDatabaseUpdate() && StringUtils.isNotBlank(MyTunesRss.CONFIG.getAdminEmail())) {
            String subject = "Database has been updated";
            String body =
                    "The database has been updated. Update took " + (time / 1000L) + " seconds.\n\nTracks: " + systemInformation.getTrackCount() +
                            "\nAlbums: " + systemInformation.getAlbumCount() + "\nArtists: " + systemInformation.getArtistCount() + "\nGenres: " +
                            systemInformation.getGenreCount();
            if (!missingItunesFiles.isEmpty()) {
                body += "\n\nMissing files from iTunes libraries:\n";
                for (Map.Entry<String, Long> entry : missingItunesFiles.entrySet()) {
                    body += entry.getKey() + ": " + entry.getValue() + "\n";
                }
            }
            sendAdminMail(subject, body);
        }
    }

    public void notifyEmailChange(User user, String oldEmail) {
        if (MyTunesRss.CONFIG.isNotifyOnEmailChange() && StringUtils.isNotBlank(MyTunesRss.CONFIG.getAdminEmail())) {
            String subject = "User has changed his email address";
            String body =
                    "User \"" + user.getName() + "\" has changed his email " + "address from \"" + oldEmail + "\" to \"" + user.getEmail() + "\".";
            sendAdminMail(subject, body);
        }
    }

    public void notifyInternalError(Throwable t) {
        if (MyTunesRss.CONFIG.isNotifyOnInternalError() && StringUtils.isNotBlank(MyTunesRss.CONFIG.getAdminEmail())) {
            String subject = "Internal error";
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            String body = "Please go to the configuration panel of your MyTunesRSS server and send a support request to Codewave Software. " +
                    "Alternatively forward this mail to support@codewave.de if you like.\n\n" + sw.toString();
            sendAdminMail(subject, body);
        }
    }

    public void notifyLoginFailure(String username, String remoteAddress) {
        if (MyTunesRss.CONFIG.isNotifyOnLoginFailure() && StringUtils.isNotBlank(MyTunesRss.CONFIG.getAdminEmail())) {
            String subject = "Login failure";
            String body = "There was an unsuccessful login attempt for user name \"" + username + "\" from remote address \"" + remoteAddress + "\".";
            sendAdminMail(subject, body);
        }
    }

    public void notifyOutdatedItunesXml(File iTunesMaster, File iTunesXml) {
        if (MyTunesRss.CONFIG.isNotifyOnOutdatedItunesXml() && StringUtils.isNotBlank(MyTunesRss.CONFIG.getAdminEmail())) {
            String subject = "Outdated iTunes XML file";
            String body = "The iTunes XML file \"" + iTunesXml.getAbsolutePath() + "\" is older than the corresponding iTunes master file \"" + iTunesMaster.getAbsolutePath() + "\". Deleting the XML file, then starting and stopping iTunes should correct the issue. The problem are most likely characters in the name/author/album/etc information in your iTunes library which cause iTunes to stop updating the XML file. Unless you find and change to offending character(s) in your library, deleting the XML file will fix the problem only once but not permanently.";
            sendAdminMail(subject, body);
        }
    }

    public void notifyLoginExpired(String username, String remoteAddress) {
        if (MyTunesRss.CONFIG.isNotifyOnLoginFailure() && StringUtils.isNotBlank(MyTunesRss.CONFIG.getAdminEmail())) {
            String subject = "Expired login failure";
            String body = "There was login attempt for expired user name \"" + username + "\" from remote address \"" + remoteAddress + "\".";
            sendAdminMail(subject, body);
        }
    }

    public void notifyPasswordChange(User user) {
        if (MyTunesRss.CONFIG.isNotifyOnPasswordChange() && StringUtils.isNotBlank(MyTunesRss.CONFIG.getAdminEmail())) {
            String subject = "User has changed his password";
            String body = "User \"" + user.getName() + "\" has changed his password.";
            sendAdminMail(subject, body);
        }
    }

    public void notifyQuotaExceeded(User user) {
        if (MyTunesRss.CONFIG.isNotifyOnQuotaExceeded() && StringUtils.isNotBlank(MyTunesRss.CONFIG.getAdminEmail())) {
            String subject = "User download quota has been exceeded";
            String body = "User download quota of " + user.getQuotaDownBytes() + " bytes has been exceeded.";
            sendAdminMail(subject, body);
        }
    }

    public void notifyTranscodingFailure(String[] sourceCommand, String[] targetCommand, Exception e) {
        if (MyTunesRss.CONFIG.isNotifyOnTranscodingFailure() && StringUtils.isNotBlank(MyTunesRss.CONFIG.getAdminEmail())) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String subject = "Transcoding failure";
            String body = "Transcoding failed.\n\n" + "Source command was \"" + StringUtils.join(sourceCommand, ' ') + "\"\n\n" +
                    "Target command was \"" + StringUtils.join(targetCommand, ' ') + "\"\n\n" + sw.toString();
            sendAdminMail(subject, body);

        }
    }

    public void notifyWebUpload(User user, String fileInfos) {
        if (MyTunesRss.CONFIG.isNotifyOnWebUpload() && StringUtils.isNotBlank(MyTunesRss.CONFIG.getAdminEmail())) {
            String subject = "Web interface file upload";
            String body = "User " + user.getName() + " has uploaded the following files to your server:\n\n" + fileInfos;
            sendAdminMail(subject, body);
        }
    }

    public void notifyMissingFile(Track track) {
        if (MyTunesRss.CONFIG.isNotifyOnMissingFile() && StringUtils.isNotBlank(MyTunesRss.CONFIG.getAdminEmail())) {
            String subject = "Missing track file";
            String body = "The file \"" + track.getFile() + "\" was requested but is missing.";
            sendAdminMail(subject, body);
        }
    }

    private void sendAdminMail(String subject, String body) {
        try {
            MyTunesRss.MAILER.sendMail(MyTunesRss.CONFIG.getAdminEmail(), "MyTunesRSS: " + subject, body);
        } catch (MailException e) {
            LOGGER.error("Could not send admin email.", e);
        }
    }
}
