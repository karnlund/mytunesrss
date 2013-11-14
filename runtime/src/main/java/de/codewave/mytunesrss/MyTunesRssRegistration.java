package de.codewave.mytunesrss;

import de.codewave.utils.Version;
import de.codewave.utils.registration.RegistrationUtils;
import de.codewave.utils.xml.JXPathUtils;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * de.codewave.mytunesrss.MyTunesRssRegistration
 */
public class MyTunesRssRegistration {
    private static final Logger LOG = LoggerFactory.getLogger(MyTunesRssRegistration.class);
    private static final long TRIAL_PERIOD_MILLIS = 1000L * 3600L * 24L * 30L; // 30 days

    private String myName;
    private long myExpiration;
    private boolean myReleaseVersion;
    private boolean myValid;
    private Version myMaxVersion;
    private JXPathContext mySettings;
    private static final String UNREGISTERED = "unregistered";

    public static MyTunesRssRegistration register(File registrationFile) throws MyTunesRssRegistrationException {
        try {
            if (MyTunesRssRegistration.isValidRegistration(registrationFile)) {
                MyTunesRssRegistration registration = new MyTunesRssRegistration();
                registration.init(registrationFile, false);
                if (registration.isExpired()) {
                    throw new MyTunesRssRegistrationException(MyTunesRssRegistrationException.Error.LicenseExpired);
                } else if (!registration.myValid) {
                    throw new MyTunesRssRegistrationException(MyTunesRssRegistrationException.Error.InvalidFile);
                } else {
                    copyFile(registrationFile, new File(MyTunesRss.PREFERENCES_DATA_PATH + "/MyTunesRSS.key"));
                    return registration;
                }
            } else {
                throw new MyTunesRssRegistrationException(MyTunesRssRegistrationException.Error.InvalidFile);
            }
        } catch (IOException e) {
            throw new MyTunesRssRegistrationException(MyTunesRssRegistrationException.Error.InvalidFile);
        }
    }

    private static void copyFile(File source, File destination) throws IOException {
        BufferedInputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        byte[] buffer = new byte[8196];
        try {
            inputStream = new BufferedInputStream(new FileInputStream(source));
            outputStream = new BufferedOutputStream(new FileOutputStream(destination));
            for (int byteRead = inputStream.read(buffer); byteRead != -1; byteRead = inputStream.read(buffer)) {
                if (byteRead > 0) {
                    outputStream.write(buffer, 0, byteRead);
                }
            }
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        }
    }

    private static boolean isValidRegistration(File file) {
        try {
            return RegistrationUtils.getRegistrationData(file.toURL(), getPublicKey()) != null;
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Could not check registration file, assuming it is invalid.", e);
            }
        }
        return false;
    }

    private static URL getPublicKey() throws IOException {
        return MyTunesRssRegistration.class.getResource("/MyTunesRSS.public");
    }

    public void init(File file, boolean allowDefaultLicense) throws IOException {
        if (allowDefaultLicense) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Checking if default license specifies that this is a pre-release version.");
            }
            handleRegistration(RegistrationUtils.getRegistrationData(getDefaultLicenseFile(), getPublicKey()));
            if (!myReleaseVersion) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("This is a pre-release version, so no other licenses are checked.");
                }
                return; // do not care about external license if this is a pre-release version
            }
        }

        String path = MyTunesRss.PREFERENCES_DATA_PATH;
        String registration = RegistrationUtils.getRegistrationData(file != null ? file.toURL() : new File(path + "/MyTunesRSS.key").toURL(),
                getPublicKey());
        if (registration != null) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Using registration data from preferences.");
            }
            handleRegistration(registration);
            if (isExpired() || isUnregistered()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("License expired or unregistered license.");
                }
            }
        } else if (allowDefaultLicense) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Using default registration data.");
            }
            handleRegistration(RegistrationUtils.getRegistrationData(getDefaultLicenseFile(), getPublicKey()));
            myExpiration = MyTunesRss.CONFIG.getConfigCreationTime() + TRIAL_PERIOD_MILLIS;
            LOG.info("Set expiration to " + new SimpleDateFormat("yyyy-MM-dd").format(new Date(myExpiration)));
        }
    }

    private URL getDefaultLicenseFile() {
        return getClass().getResource("/MyTunesRSS.key");
    }

    private void handleRegistration(String registration) {
        if (StringUtils.isNotEmpty(registration)) {
            JXPathContext registrationContext = JXPathUtils.getContext(registration);
            myReleaseVersion = JXPathUtils.getBooleanValue(registrationContext, "/registration/release-version", true);
            myName = JXPathUtils.getStringValue(registrationContext, "/registration/name", UNREGISTERED);
            String versionString = JXPathUtils.getStringValue(registrationContext, "/registration/max-version", Integer.toString(Integer.MAX_VALUE));
            myMaxVersion = new Version(versionString + "." + Integer.MAX_VALUE + "." + Integer.MAX_VALUE);
            String expirationDate = JXPathUtils.getStringValue(registrationContext, "/registration/expiration", null);
            String settingsText = JXPathUtils.getStringValue(registrationContext, "/registration/settings", null);
            if (settingsText != null) {
                for (String token : MyTunesRssUtils.substringsBetween(settingsText, "<!--", "-->")) {
                    String[] values = MyTunesRss.COMMAND_LINE_ARGS.get(StringUtils.trimToEmpty(token));
                    settingsText = settingsText.replace("<!--" + token + "-->", values != null && values.length > 0 ? values[0] : "");
                }
                mySettings = JXPathUtils.getContext(JXPathUtils.getContext(settingsText), "settings");
            } else {
                mySettings = null;
            }
            if (expirationDate != null) {
                try {
                    myExpiration = new SimpleDateFormat("yyyy-MM-dd").parse(expirationDate).getTime();
                } catch (ParseException e) {
                    // intentionally left blank
                }
            } else {
                myExpiration = 0;
            }
            myValid = true;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Registration data:");
                LOG.debug("name=" + getName());
                LOG.debug("expiration=" + getExpiration(MyTunesRssUtils.getBundleString(Locale.getDefault(), "common.dateFormat")));
                LOG.debug("max-version=" + myMaxVersion);
            }
        }
    }

    public long getExpiration() {
        return myExpiration;
    }

    public String getExpiration(String dateFormat) {
        if (getExpiration() > 0) {
            return new SimpleDateFormat(dateFormat).format(new Date(getExpiration()));
        }
        return "";
    }

    public boolean isExpired() {
        return isExpiredVersion() || (myExpiration > 0 && myExpiration <= System.currentTimeMillis());
    }

    public boolean isExpirationDate() {
        return myExpiration > 0;
    }

    public String getName() {
        return myName;
    }

    public boolean isUnregistered() {
        return StringUtils.equals(myName, UNREGISTERED);
    }

    public boolean isExpiredPreReleaseVersion() {
        return !myReleaseVersion && isExpired();
    }

    public boolean isReleaseVersion() {
        return myReleaseVersion;
    }

    public JXPathContext getSettings() {
        return mySettings;
    }

    public boolean isExpiredVersion() {
        return myMaxVersion.compareTo(new Version(MyTunesRss.VERSION)) < 0;
    }

    public boolean isValidVersion(Version version) {
        return myMaxVersion.compareTo(version) >= 0;
    }
}
