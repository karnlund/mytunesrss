package de.codewave.mytunesrss;

import com.ibm.icu.text.Normalizer;
import de.codewave.camel.mp4.Mp4Atom;
import de.codewave.mytunesrss.config.*;
import de.codewave.mytunesrss.datastore.DatabaseBackup;
import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.statistics.RemoveOldEventsStatement;
import de.codewave.mytunesrss.task.DeleteDatabaseFilesCallable;
import de.codewave.mytunesrss.vlc.VlcPlayerException;
import de.codewave.utils.MiscUtils;
import de.codewave.utils.io.LogStreamCopyThread;
import de.codewave.utils.io.ZipUtils;
import de.codewave.utils.sql.DataStoreSession;
import de.codewave.utils.sql.DataStoreStatement;
import de.codewave.utils.sql.ResultSetType;
import de.codewave.utils.sql.SmartStatement;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggerRepository;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.h2.mvstore.FileStore;
import org.h2.mvstore.MVStore;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.swing.*;
import java.awt.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * de.codewave.mytunesrss.MyTunesRssUtils
 */
public class MyTunesRssUtils {

    public static Map<String, String> IMAGE_TO_MIME = new HashMap<String, String>();

    static {
        IMAGE_TO_MIME.put("jpg", "image/jpeg");
        IMAGE_TO_MIME.put("gif", "image/gif");
        IMAGE_TO_MIME.put("png", "image/png");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(MyTunesRssUtils.class);
    private static RandomAccessFile LOCK_FILE;

    public static boolean equals(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return true;
        } else if (o1 == null || o2 == null) {
            return false;
        } else if (o1 instanceof byte[] && o2 instanceof byte[]) {
            return Arrays.equals((byte[]) o1, (byte[]) o2);
        } else if (o1 instanceof char[] && o2 instanceof char[]) {
            return Arrays.equals((char[]) o1, (char[]) o2);
        } else if (o1 instanceof short[] && o2 instanceof short[]) {
            return Arrays.equals((short[]) o1, (short[]) o2);
        } else if (o1 instanceof int[] && o2 instanceof int[]) {
            return Arrays.equals((int[]) o1, (int[]) o2);
        } else if (o1 instanceof long[] && o2 instanceof long[]) {
            return Arrays.equals((long[]) o1, (long[]) o2);
        } else if (o1 instanceof float[] && o2 instanceof float[]) {
            return Arrays.equals((float[]) o1, (float[]) o2);
        } else if (o1 instanceof double[] && o2 instanceof double[]) {
            return Arrays.equals((double[]) o1, (double[]) o2);
        } else if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
            return Arrays.equals((boolean[]) o1, (boolean[]) o2);
        } else if (o1.getClass().isArray() && o2.getClass().isArray()) {
            return Arrays.equals((Object[]) o1, (Object[]) o2);
        }
        return o1.equals(o2);
    }

    public static void showErrorMessageWithDialog(String message) {
        if (!isHeadless()) {
            JOptionPane.showMessageDialog(null, message);
        } else {
            System.err.println(message);
        }
    }

    public static boolean isHeadless() {
        return (MyTunesRss.CONFIG != null && MyTunesRss.CONFIG.isHeadless()) || MyTunesRss.COMMAND_LINE_ARGS.containsKey(MyTunesRss.CMD_HEADLESS) || GraphicsEnvironment.isHeadless();
    }

    public static boolean isFakeHeadless() {
        return ((MyTunesRss.CONFIG != null && MyTunesRss.CONFIG.isHeadless()) || MyTunesRss.COMMAND_LINE_ARGS.containsKey(MyTunesRss.CMD_HEADLESS)) && !GraphicsEnvironment.isHeadless() && SystemUtils.IS_OS_MAC_OSX;
    }

    public static void showErrorMessage(String message) {
        LOGGER.error(message);
        System.err.println(message);
    }

    public static String getBundleString(Locale locale, String key, Object... parameters) {
        if (key == null) {
            return "";
        }
        if (locale == null) {
            locale = Locale.ENGLISH; // default in case of NULL locale
        }
        ResourceBundle bundle = MyTunesRss.RESOURCE_BUNDLE_MANAGER.getBundle("de.codewave.mytunesrss.MyTunesRss", locale);
        if (parameters == null || parameters.length == 0) {
            return bundle.getString(key);
        }
        return MessageFormat.format(bundle.getString(key), parameters);
    }

    public static HttpClient createHttpClient() {
        HttpClient httpClient = new HttpClient();
        DefaultHttpMethodRetryHandler retryhandler = new DefaultHttpMethodRetryHandler(1, true);
        httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryhandler);
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.getParams().setSoTimeout(10000);
        httpClient.setHttpConnectionManager(connectionManager);
        if (MyTunesRss.CONFIG.isProxyServer()) {
            HostConfiguration hostConfiguration = new HostConfiguration();
            hostConfiguration.setProxy(MyTunesRss.CONFIG.getProxyHost(), MyTunesRss.CONFIG.getProxyPort());
            httpClient.setHostConfiguration(hostConfiguration);
        }
        return httpClient;
    }

    public static void shutdownGracefully() {
        MyTunesRss.SHUTDOWN_IN_PROGRESS.set(true);
        LOGGER.debug("Shutting down gracefully.");
        MyTunesRss.CONFIG.save();
        try {
            if (MyTunesRss.VLC_PLAYER != null) {
                MyTunesRss.VLC_PLAYER.destroy();
            }
        } catch (VlcPlayerException e) {
            LOGGER.error("Could not destroy VLC player.", e);
        }
        LOGGER.debug("Destroying streaming cache.");
        MyTunesRss.TRANSCODER_CACHE.destroy();
        if (MyTunesRss.FORM != null) {
            MyTunesRss.FORM.hide();
        }
        try {
            LOGGER.info("Cancelling database jobs.");
            if (MyTunesRss.WEBSERVER != null && MyTunesRss.WEBSERVER.isRunning()) {
                LOGGER.info("Stopping user interface server.");
                MyTunesRss.stopWebserver();
            }
            if (MyTunesRss.ADMIN_SERVER != null) {
                LOGGER.info("Stopping admin interface server.");
                MyTunesRss.stopAdminServer();
            }
            LOGGER.info("Shutting down executor services.");
            MyTunesRss.ROUTER_CONFIG.deleteUserPortMappings();
            MyTunesRss.ROUTER_CONFIG.deleteAdminPortMapping();
            MyTunesRss.EXECUTOR_SERVICE.shutdown();
            if (MyTunesRss.QUARTZ_SCHEDULER != null) {
                try {
                    LOGGER.info("Shutting down quartz scheduler.");
                    MyTunesRss.QUARTZ_SCHEDULER.shutdown();
                } catch (SchedulerException e) {
                    LOGGER.error("Could not shutdown quartz scheduler.", e);
                }
            }
            if (MyTunesRss.STORE != null && MyTunesRss.STORE.isInitialized()) {
                DataStoreSession session = MyTunesRss.STORE.getTransaction();
                try {
                    LOGGER.debug("Removing old temporary playlists.");
                    session.executeStatement(new RemoveOldTempPlaylistsStatement());
                    session.commit();
                } catch (SQLException e) {
                    LOGGER.error("Could not remove old temporary playlists.", e);
                } finally {
                    session.rollback();                    }
                try {
                    LOGGER.debug("Removing old statistic events.");
                    session.executeStatement(new RemoveOldEventsStatement());
                    session.commit();
                } catch (SQLException e) {
                    LOGGER.error("Could not remove old statistic events.", e);
                } finally {
                    session.rollback();                    }
                LOGGER.debug("Destroying store.");
                MyTunesRss.STORE.destroy();
            }
            MyTunesRss.LUCENE_TRACK_SERVICE.shutdown();
            if (MyTunesRss.CONFIG.isDefaultDatabase() && MyTunesRss.CONFIG.isDeleteDatabaseOnExit()) {
                try {
                    new DeleteDatabaseFilesCallable().call();
                } catch (IOException e) {
                    LOGGER.error("Could not delete default database files.", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception during shutdown.", e);
        } finally {
            LOGGER.info("Very last log message before shutdown.");
            System.exit(0);
        }
    }

    private static final double KBYTE = 1024;
    private static final double MBYTE = 1024 * KBYTE;
    private static final double GBYTE = 1024 * MBYTE;
    private static final NumberFormat BYTE_STREAMED_FORMAT = new DecimalFormat("0");
    private static final NumberFormat KBYTE_STREAMED_FORMAT = new DecimalFormat("0");
    private static final DecimalFormat MBYTE_STREAMED_FORMAT = new DecimalFormat("0.##");
    private static final DecimalFormat GBYTE_STREAMED_FORMAT = new DecimalFormat("0.#");

    static {
        MBYTE_STREAMED_FORMAT.setDecimalSeparatorAlwaysShown(false);
        GBYTE_STREAMED_FORMAT.setDecimalSeparatorAlwaysShown(false);
    }

    public static String getMemorySizeForDisplay(long bytes) {
        if (bytes >= GBYTE) {
            return GBYTE_STREAMED_FORMAT.format(bytes / GBYTE) + " GB";
        } else if (bytes >= MBYTE) {
            return MBYTE_STREAMED_FORMAT.format(bytes / MBYTE) + " MB";
        } else if (bytes >= KBYTE) {
            return KBYTE_STREAMED_FORMAT.format(bytes / KBYTE) + " KB";
        }
        return BYTE_STREAMED_FORMAT.format(bytes) + " Byte";
    }

    public static boolean deleteRecursivly(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    if (!deleteRecursivly(subFile)) {
                        return false;
                    }
                }
            }
            file.delete();
        } else if (file.isFile()) {
            return file.delete();
        }
        return true;
    }

    public static SmartStatement createStatement(Connection connection, String name) throws SQLException {
        return createStatement(connection, name, Collections.<String, Boolean>emptyMap());
    }

    public static SmartStatement createStatement(Connection connection, String name, final Map<String, Boolean> conditionals) throws SQLException {
        return MyTunesRss.STORE.getSmartStatementFactory().createStatement(connection, name, (Map<String, Boolean>) Proxy.newProxyInstance(MyTunesRss.class.getClassLoader(), new Class[]{Map.class}, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("get".equals(method.getName()) && args.length == 1 && args[0] instanceof String) {
                    return conditionals.containsKey(args[0]) ? conditionals.get(args[0]) : Boolean.FALSE;
                } else {
                    return method.invoke(conditionals, args);
                }
            }
        }));
    }

    public static void setCodewaveLogLevel(Level level) {
        if (level == Level.OFF) {
            LOGGER.error("Setting codewave log to level \"" + level + "\".");
        }
        LoggerRepository repository = org.apache.log4j.Logger.getRootLogger().getLoggerRepository();
        for (Enumeration loggerEnum = repository.getCurrentLoggers(); loggerEnum.hasMoreElements();) {
            org.apache.log4j.Logger logger = (org.apache.log4j.Logger) loggerEnum.nextElement();
            if (logger.getName().startsWith("de.codewave.")) {
                logger.setLevel(level);
            }
        }
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("de.codewave");
        logger.setLevel(level);
        LOGGER.error("Setting codewave log to level \"" + level + "\".");
    }

    public static String getBaseType(String contentType) {
        try {
            ContentType type = new ContentType(StringUtils.trimToEmpty(contentType));
            return type.getBaseType();
        } catch (ParseException e) {
            LOGGER.warn("Could not get base type from content type \"" + contentType + "\".", e);
        }
        return "application/octet-stream";
    }

    public static String getBuiltinAddonsPath() {
        return System.getProperty("de.codewave.mytunesrss.addons.builtin", ".");
    }

    public static boolean lockInstance(long timeoutMillis) {
        try {
            File file = new File(MyTunesRss.CACHE_DATA_PATH + "/MyTunesRSS.lck");
            file.deleteOnExit();
            LOCK_FILE = new RandomAccessFile(file, "rw");
        } catch (IOException e) {
            return false;
        }
        long endTime = System.currentTimeMillis() + timeoutMillis;
        do {
            try {
                if (LOCK_FILE.getChannel().tryLock() != null) {
                    return false;
                }
                Thread.sleep(500);
            } catch (IOException e) {
                // intentionally left blank
            } catch (InterruptedException e) {
                // intentionally left blank
            }
        } while (System.currentTimeMillis() < endTime);
        return true;
    }

    /**
     * Check if the specified index is a valid letter pager index. A valid index is
     * in the range from 0 to 8.
     *
     * @param index An index.
     * @return TRUE if the index is a valid letter pager index or FALSE otherwise.
     */
    public static Boolean isLetterPagerIndex(int index) {
        return index >= 0 && index <= 8;
    }

    public static boolean loginLDAP(String userName, String password) {
        LOGGER.debug("Checking authorization with LDAP server.");
        LdapConfig ldapConfig = MyTunesRss.CONFIG.getLdapConfig();
        if (ldapConfig.isValid()) {
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://" + ldapConfig.getHost() + ":" + ldapConfig.getPort());
            env.put(Context.SECURITY_AUTHENTICATION, ldapConfig.getAuthMethod().name());
            env.put(Context.SECURITY_PRINCIPAL, MessageFormat.format(ldapConfig.getAuthPrincipal(), userName));
            env.put(Context.SECURITY_CREDENTIALS, password);
            try {
                DirContext ctx = new InitialDirContext(env);
                LOGGER.debug("Checking authorization with LDAP server: authorized!");
                User user = MyTunesRss.CONFIG.getUser(userName);
                if (user == null) {
                    LOGGER.debug("Corresponding user for LDAP \"" + userName + "\" not found.");
                    User template = MyTunesRss.CONFIG.getUser(ldapConfig.getTemplateUser());
                    if (template != null) {
                        LOGGER.debug("Using LDAP template user \"" + template.getName() + "\".");
                        user = (User) template.clone();
                        user.setName(userName);
                        user.setPasswordHash(MyTunesRss.SHA1_DIGEST.get().digest(UUID.randomUUID().toString().getBytes("UTF-8")));
                        user.setChangePassword(false);
                        LOGGER.debug("Storing new user with name \"" + user.getName() + "\".");
                        MyTunesRss.CONFIG.addUser(user);
                    }
                }
                if (user == null) {
                    LOGGER.error("Could not create new user \"" + userName + "\" from template user \"" + ldapConfig.getTemplateUser() + "\".");
                    return false;
                }
                if (ldapConfig.isFetchEmail()) {
                    LOGGER.debug("Fetching email for user \"" + userName + "\" from LDAP.");
                    SearchControls searchControls = new SearchControls(SearchControls.SUBTREE_SCOPE, 1, ldapConfig.getSearchTimeout(), new String[]{ldapConfig.getMailAttributeName()}, false, false);
                    NamingEnumeration<SearchResult> namingEnum = ctx.search(StringUtils.defaultString(ldapConfig.getSearchRoot()), MessageFormat.format(ldapConfig.getSearchExpression(), userName), searchControls);
                    if (namingEnum.hasMore()) {
                        String email = namingEnum.next().getAttributes().get(ldapConfig.getMailAttributeName()).get().toString();
                        LOGGER.debug("Setting email \"" + email + "\" for user \"" + user.getName() + "\".");
                        user.setEmail(email);
                    }
                }
                return !user.isGroup() && user.isActive();
            } catch (AuthenticationException e) {
                LOGGER.info("LDAP login failed for \"" + userName + "\".");
            } catch (Exception e) {
                LOGGER.error("Could not validate username/password with LDAP server.", e);
            }
        }
        return false;
    }

    public static RegistrationFeedback getRegistrationFeedback(Locale locale) {
        if (MyTunesRss.REGISTRATION.isExpiredPreReleaseVersion()) {
            return new RegistrationFeedback(MyTunesRssUtils.getBundleString(locale, "error.preReleaseVersionExpired"), false);
        } else if (MyTunesRss.REGISTRATION.isExpiredVersion()) {
            return new RegistrationFeedback(MyTunesRssUtils.getBundleString(locale, "error.registrationExpiredVersion"), false);
        } else if (MyTunesRss.REGISTRATION.isExpired()) {
            return new RegistrationFeedback(MyTunesRssUtils.getBundleString(locale, "error.registrationExpired"), false);
        } else if (MyTunesRss.REGISTRATION.isExpirationDate() && !MyTunesRss.REGISTRATION.isReleaseVersion()) {
            return new RegistrationFeedback(MyTunesRssUtils.getBundleString(locale, "info.preReleaseExpiration",
                    MyTunesRss.REGISTRATION.getExpiration(MyTunesRssUtils.getBundleString(
                            locale, "common.dateFormat"))), true);
        } else if (MyTunesRss.REGISTRATION.isExpirationDate() && !MyTunesRss.REGISTRATION.isExpired()) {
            return new RegistrationFeedback(MyTunesRssUtils.getBundleString(locale, "info.expirationInfo",
                    MyTunesRss.REGISTRATION.getExpiration(MyTunesRssUtils.getBundleString(
                            locale, "common.dateFormat"))), true);
        }
        return null;
    }

    public static Playlist[] getPlaylistPath(Playlist playlist, List<Playlist> playlists) {
        List<Playlist> path = new ArrayList<Playlist>();
        for (; playlist != null; playlist = findPlaylistWithId(playlists, playlist.getContainerId())) {
            path.add(0, playlist);
        }
        return path.toArray(new Playlist[path.size()]);
    }

    private static Playlist findPlaylistWithId(List<Playlist> playlists, String containerId) {
        for (Playlist playlist : playlists) {
            if (StringUtils.equals(playlist.getId(), containerId)) {
                return playlist;
            }
        }
        return null;
    }

    public static Playlist findParentPlaylist(Playlist playlist, List<Playlist> playlists) {
        if (playlist.getContainerId() == null) {
            return null;
        }
        return findPlaylistWithId(playlists, playlist.getContainerId());
    }

    public static boolean hasChildPlaylists(Playlist playlist, List<Playlist> playlists) {
        for (Playlist each : playlists) {
            if (playlist.getId().equals(each.getContainerId())) {
                return true;
            }
        }
        return false;
    }

    public static int getRootPlaylistCount(List<Playlist> playlists) {
        int count = 0;
        if (playlists != null) {
            for (Playlist playlist : playlists) {
                if (playlist.getContainerId() == null) {
                    count++;
                }
            }
        }
        return count;
    }

    public static File createTempFile(String suffix) throws IOException {
        return File.createTempFile("mytunesrss_", suffix, MyTunesRss.TEMP_CACHE.getBaseDir());
    }

    /**
     * Get a sub list.
     *
     * @param fullList
     * @param first
     * @param count If count is less than 1, the rest of the list is returned.
     * @param <T>
     * @return
     */
    public static <T> List<T> getSubList(List<T> fullList, int first, int count) {
        if (count > 0) {
            return fullList.subList(first, Math.min(first + count, fullList.size()));
        } else {
            return fullList.subList(first, fullList.size());
        }
    }

    public static void backupDatabase() throws IOException, SQLException {
        LOGGER.info("Creating database backup.");
        if (!MyTunesRss.CONFIG.isDefaultDatabase()) {
            throw new IllegalStateException("Cannot backup non-default database.");
        }
        if (!MyTunesRss.STORE.isInitialized()) {
            throw new IllegalStateException("Database must already be initialized for starting a backup.");
        }
        LOGGER.debug("Destroying store before backup.");
        MyTunesRss.STORE.destroy();
        try {
            File databaseDir = new File(MyTunesRss.CACHE_DATA_PATH + "/" + "h2");
            File backupFile = DatabaseBackup.createBackupFile();
            LOGGER.info("Creating H2 database backup \"" + backupFile.getAbsolutePath() + "\".");
            ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(backupFile);
            try {
                ZipUtils.addFilesToZipRecursively("", databaseDir, new FileFilter() {
                    public boolean accept(File file) {
                        if (file.getName().toLowerCase(Locale.ENGLISH).contains(".lock.db")) {
                            return false;
                        }
                        return true;
                    }
                }, zipOutputStream);
            } finally {
                zipOutputStream.close();
            }
        } finally {
            LOGGER.debug("Restarting store after backup.");
            MyTunesRss.STORE.init();
        }
    }

    public static void restoreDatabaseBackup(DatabaseBackup backup) throws IOException {
        LOGGER.info("Restoring database backup from file \"" + backup.getFile().getAbsolutePath() + "\".");
        if (!MyTunesRss.CONFIG.isDefaultDatabase()) {
            throw new IllegalStateException("Cannot restore non-default database.");
        }
        if (MyTunesRss.STORE.isInitialized()) {
            throw new IllegalStateException("Database must not be initialized for restoring a backup.");
        }
        File databaseDir = new File(MyTunesRss.CACHE_DATA_PATH + "/" + "h2");
        FileUtils.deleteDirectory(databaseDir);
        databaseDir.mkdir();
        ZipUtils.unzip(backup.getFile(), databaseDir);
    }

    public static List<DatabaseBackup> findDatabaseBackups() throws IOException {
        List<DatabaseBackup> backups = new ArrayList<DatabaseBackup>();
        File[] files = new File(MyTunesRss.CACHE_DATA_PATH).listFiles();
        if (files != null) {
            for (File file : files) {
                if (DatabaseBackup.isBackupFile(file)) {
                    LOGGER.debug("Found backup file \"" + file + "\".");
                    backups.add(new DatabaseBackup(file));
                }
            }
        }
        Collections.sort(backups);
        return backups;
    }

    public static void removeAllButLatestDatabaseBackups(int numberOfBackupsToKeep) throws IOException {
        List<DatabaseBackup> backups = findDatabaseBackups();
        if (backups.size() > numberOfBackupsToKeep) {
            LOGGER.info("Deleting " + (backups.size() - numberOfBackupsToKeep) + " old database backup files.");
            for (int i = numberOfBackupsToKeep; i < backups.size(); i++) {
                LOGGER.debug("Deleting backup file \"" + backups.get(i).getFile() + "\".");
                backups.get(i).getFile().delete();
            }
        }
    }

    public static Map<String, Mp4Atom> toMap(Collection<Mp4Atom> atoms) {
        Map<String, Mp4Atom> result = new HashMap<String, Mp4Atom>();
        for (Mp4Atom atom : atoms) {
            result.put(atom.getPath(), atom);
            result.putAll(toMap(atom.getChildren()));
        }
        return result;
    }

    public static int getMaxImageSize(de.codewave.mytunesrss.meta.Image source) throws IOException {
        if (isExecutableGraphicsMagick() && source.getImageFile() != null) {
            return getMaxImageSizeExternalProcess(source.getImageFile());
        } else {
            return getMaxImageSizeJava(source);
        }
    }

    public static int getMaxImageSize(File source) throws IOException {
        if (isExecutableGraphicsMagick()) {
            return getMaxImageSizeExternalProcess(source);
        } else {
            String mimeType = IMAGE_TO_MIME.get(FilenameUtils.getExtension(source.getName()).toLowerCase());
            de.codewave.mytunesrss.meta.Image image = new de.codewave.mytunesrss.meta.Image(mimeType, FileUtils.readFileToByteArray(source));
            return getMaxImageSizeJava(image);
        }
    }

    private static int getMaxImageSizeJava(de.codewave.mytunesrss.meta.Image source) throws IOException {
        ByteArrayInputStream imageInputStream = new ByteArrayInputStream(source.getData());
        try {
            BufferedImage original = ImageIO.read(imageInputStream);
            int width = original.getWidth();
            int height = original.getHeight();
            return Math.max(width, height);
        } finally {
            imageInputStream.close();
        }
    }

    private static int getMaxImageSizeExternalProcess(File source) throws IOException {
        List<String> resizeCommand = Arrays.asList(MyTunesRss.CONFIG.getGmExecutable().getAbsolutePath(), "identify", "-format",  "%w %h", source.getAbsolutePath());
        String msg = "Executing command \"" + StringUtils.join(resizeCommand, " ") + "\".";
        LOGGER.debug(msg);
        Process process = new ProcessBuilder(resizeCommand).start();
        MyTunesRss.SPAWNED_PROCESSES.add(process);
        InputStream is = process.getInputStream();
        try {
            LogStreamCopyThread stderrCopyThread = new LogStreamCopyThread(process.getErrorStream(), false, LoggerFactory.getLogger("GM"), LogStreamCopyThread.LogLevel.Error, msg, null);
            stderrCopyThread.setDaemon(true);
            stderrCopyThread.start();
            String[] dimensions = StringUtils.split(StringUtils.trim(IOUtils.toString(is, "UTF-8")));
            if (dimensions.length == 2) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Image dimensions for \"" + source.getAbsolutePath() + "\" are \"" + dimensions[0] + "x" + dimensions[1] + "\".");
                }
                return Math.max(Integer.parseInt(dimensions[0]), Integer.parseInt(dimensions[1]));
            } else {
                throw new IOException("Could not get dimension from images using external process.");
            }
        } finally {
            waitForProcess(process, 100);
            MyTunesRss.SPAWNED_PROCESSES.remove(process);
        }
    }

    public static boolean isImageUsable(de.codewave.mytunesrss.meta.Image image) {
        if (!isExecutableGraphicsMagick()) {
            ByteArrayInputStream imageInputStream = new ByteArrayInputStream(image.getData());
            try {
                ImageIO.read(imageInputStream);
            } catch (IOException e) {
                LOGGER.debug("Could not create buffered image.", e);
                return false;
            } finally {
                IOUtils.closeQuietly(imageInputStream);
            }
        }
        return true;
    }

    public static de.codewave.mytunesrss.meta.Image resizeImageWithMaxSize(de.codewave.mytunesrss.meta.Image source, int maxSize, float jpegQuality, String debugInfo) throws IOException {
        if (isExecutableGraphicsMagick() && source.getImageFile() != null) {
            return resizeImageWithMaxSizeExternalProcess(source.getImageFile(), maxSize, jpegQuality, debugInfo);
        } else {
            return resizeImageWithMaxSizeJava(source, maxSize, jpegQuality, debugInfo);
        }
    }

    public static de.codewave.mytunesrss.meta.Image resizeImageWithMaxSize(File source, int maxSize, float jpegQuality, String debugInfo) throws IOException {
        if (isExecutableGraphicsMagick()) {
            return resizeImageWithMaxSizeExternalProcess(source, maxSize, jpegQuality, debugInfo);
        } else {
            String mimeType = IMAGE_TO_MIME.get(FilenameUtils.getExtension(source.getName()).toLowerCase());
            de.codewave.mytunesrss.meta.Image image = new de.codewave.mytunesrss.meta.Image(mimeType, FileUtils.readFileToByteArray(source));
            return resizeImageWithMaxSizeJava(image, maxSize, jpegQuality, debugInfo);
        }
    }

    private static boolean isExecutableGraphicsMagick() {
        return MyTunesRss.CONFIG.isGmEnabled() && MyTunesRss.CONFIG.getGmExecutable() != null && MyTunesRss.CONFIG.getGmExecutable().isFile() && MyTunesRss.CONFIG.getGmExecutable().canExecute();
    }

    private static void waitForProcess(final Process process, long maxWaitMillis) {
        try {
            Thread waitForProcessThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        process.waitFor();
                    } catch (InterruptedException e) {
                        process.destroy();
                    }
                }
            });
            waitForProcessThread.start();
            waitForProcessThread.join(maxWaitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            process.destroy();
        }
    }

    private static de.codewave.mytunesrss.meta.Image resizeImageWithMaxSizeExternalProcess(File source, int maxSize, float jpegQuality, String debugInfo) throws IOException {
        long start = System.currentTimeMillis();
        try {
            File tempOutFile = createTempFile(".jpg");
            try {
                List<String> resizeCommand = Arrays.asList(MyTunesRss.CONFIG.getGmExecutable().getAbsolutePath(), "convert", source.getAbsolutePath(), "-resize", maxSize + "x" + maxSize, "-quality", Float.toString(jpegQuality), tempOutFile.getAbsolutePath());
                String msg = "Executing command \"" + StringUtils.join(resizeCommand, " ") + "\".";
                LOGGER.debug(msg);
                final Process process = new ProcessBuilder(resizeCommand).redirectErrorStream(true).start();
                MyTunesRss.SPAWNED_PROCESSES.add(process);
                try {
                    LogStreamCopyThread stdoutCopyThread = new LogStreamCopyThread(process.getInputStream(), false, LoggerFactory.getLogger("GM"), LogStreamCopyThread.LogLevel.Info, msg, null);
                    stdoutCopyThread.setDaemon(true);
                    stdoutCopyThread.start();
                    waitForProcess(process, 10000);
                } finally {
                    MyTunesRss.SPAWNED_PROCESSES.remove(process);
                }
                FileInputStream is = FileUtils.openInputStream(tempOutFile);
                try {
                    return new de.codewave.mytunesrss.meta.Image("image/jpg", is);
                } finally {
                    is.close();
                }
            } finally {
                tempOutFile.delete();
            }
        } finally {
            LOGGER.debug("Resizing (external process) [" + debugInfo  + "] to max " + maxSize + " with jpegQuality " + jpegQuality + " took " + (System.currentTimeMillis() - start) + " ms.");
        }
    }

    private static de.codewave.mytunesrss.meta.Image resizeImageWithMaxSizeJava(de.codewave.mytunesrss.meta.Image source, int maxSize, float jpegQuality, String debugInfo) throws IOException {
        long start = System.currentTimeMillis();
        ByteArrayInputStream imageInputStream = new ByteArrayInputStream(source.getData());
        try {
            BufferedImage original = ImageIO.read(imageInputStream);
            int width = original.getWidth();
            int height = original.getHeight();
            if (Math.max(width, height) <= maxSize) {
                return source; // original does not exceed max size
            }
            if (width > height) {
                height = (height * maxSize) / width;
                width = maxSize;
            } else {
                width = (width * maxSize) / height;
                height = maxSize;
            }
            Image scaledImage = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            BufferedImage targetImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            targetImage.getGraphics().drawImage(scaledImage, 0, 0, null);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                try {
                    writer.setOutput(new MemoryCacheImageOutputStream(byteArrayOutputStream));
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(jpegQuality / 100f);
                    param.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
                    writer.write(null, new IIOImage(targetImage, null, null), param);
                } finally {
                    writer.dispose();
                }
                return new de.codewave.mytunesrss.meta.Image("image/jpeg", byteArrayOutputStream.toByteArray());
            } finally {
                byteArrayOutputStream.close();
            }
        } finally {
            imageInputStream.close();
            LOGGER.debug("Resizing (java) [" + debugInfo  + "] to max " + maxSize + " with jpegQuality " + jpegQuality + " took " + (System.currentTimeMillis() - start) + " ms.");
        }
    }

    /**
     * Return best file for the specified name. First the file is created using the composed form of the
     * file name. If this file does not exist, the decomposed form is used. If this file does not exist
     * either, the original name is used. This file is returned no matter whether or not it exists.
     *
     * @param filename A filename.
     *
     * @return The best file for the specified name.
     */
    public static File searchFile(String filename) {
        return searchFile(new File(filename));
    }

    public static File searchFile(File file) {
        String filename = file.getAbsolutePath();
        LOGGER.debug("Trying to find " + MiscUtils.getUtf8UrlEncoded(file.getAbsolutePath()) + ".");
        if (file.exists()) {
            return file;
        }
        File composedFile = new File(MiscUtils.compose(filename));
        LOGGER.debug("Trying to find " + MiscUtils.getUtf8UrlEncoded(composedFile.getAbsolutePath()) + ".");
        if (composedFile.exists()) {
            return composedFile;
        }
        File decomposedFile = new File(MiscUtils.decompose(filename));
        LOGGER.debug("Trying to find " + MiscUtils.getUtf8UrlEncoded(decomposedFile.getAbsolutePath()) + ".");
        if (decomposedFile.exists()) {
            return decomposedFile;
        }
        if (file.getParentFile() != null) {
            LOGGER.debug("File not found, trying to find parent.");
            File parent = searchFile(file.getParentFile());
            if (parent != null && parent.isDirectory()) {
                LOGGER.debug("Found parent, listing files.");
                File[] files = parent.listFiles();
                if (files != null) {
                    for (File each : files) {
                        LOGGER.debug("Comparing " + MiscUtils.getUtf8UrlEncoded(file.getName()) + " to " + MiscUtils.getUtf8UrlEncoded(each.getName()) +  ".");
                        if (Normalizer.compare(file.getName(), each.getName(), Normalizer.FOLD_CASE_DEFAULT) == 0) {
                            LOGGER.debug("Match.");
                            return each;
                        }
                    }
                }
            }
        }
        LOGGER.debug("File not found.");
        return file;
    }

    public static void updateUserDatabaseReferences(DataStoreSession session) throws SQLException {
        Set<String> playlistIds = new HashSet<String>();
        for (Playlist playlist : session.executeQuery(new FindPlaylistQuery(null, null, null, true)).getResults()) {
            playlistIds.add(playlist.getId());
        }
        Set<String> photoAlbumIds = new HashSet<String>(session.executeQuery(new FindPhotoAlbumIdsQuery()));
        for (User user : MyTunesRss.CONFIG.getUsers()) {
            user.retainPlaylists(playlistIds);
            user.retainPhotoAlbums(photoAlbumIds);
        }
    }

    public static List<String> getDefaultVlcCommand(File inputFile) {
        List<String> command = new ArrayList<String>();
        command.add(MyTunesRss.CONFIG.getVlcExecutable().getAbsolutePath());
        command.add(inputFile.getAbsolutePath());
        command.add("vlc://quit");
        command.add("--intf=dummy");
        if (SystemUtils.IS_OS_WINDOWS) {
            command.add("--dummy-quiet");
        }
        return command;
    }

    /**
     * Try to find a VLC executable. Depending on the operating system some standard paths are searched.
     *
     * @return The path of a VLC executable or NULL if none was found.
     */
    public static String findVlcExecutable() {
        File[] files;
        if (SystemUtils.IS_OS_MAC_OSX) {
            files = new File[] {
                    new File("/Applications/VLC.app/Contents/MacOS/VLC")
            };
        } else if (SystemUtils.IS_OS_WINDOWS) {
            files = new File[] {
                    new File(System.getenv("ProgramFiles") + "/VideoLAN/VLC/vlc.exe"),
                    new File(System.getenv("ProgramFiles") + " (x86)/VideoLAN/VLC/vlc.exe")
            };
        } else {
            files = new File[] {
                    new File("/usr/bin/vlc")
            };
        }
        for (File file : files) {
            if (isExecutable(file)) {
                LOGGER.info("Found VLC executable \"" + file.getAbsolutePath() + "\".");
                try {
                    return file.getCanonicalPath();
                } catch (IOException e) {
                    LOGGER.warn("Could not get canonical path for VLC file. Using absolute path instead.");
                }
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Try to find a GraphicsMagick executable. Depending on the operating system some standard paths are searched.
     *
     * @return The path of a GraphicsMagick executable or NULL if none was found.
     */
    public static String findGraphicsMagickExecutable() {
        File[] files;
        if (SystemUtils.IS_OS_WINDOWS) {
            files = new File[] {
                    /*new File(System.getenv("ProgramFiles") + "/gm.exe"),
                    new File(System.getenv("ProgramFiles") + " (x86)/gm.exe")*/
            };
        } else {
            files = new File[] {
                    new File("/usr/bin/gm"),
                    new File("/opt/local/bin/gm")
            };
        }
        for (File file : files) {
            if (isExecutable(file)) {
                LOGGER.info("Found GraphicsMagick executable \"" + file.getAbsolutePath() + "\".");
                try {
                    return file.getCanonicalPath();
                } catch (IOException e) {
                    LOGGER.warn("Could not get canonical path for GM file. Using absolute path instead.");
                }
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    public static Collection<String> getAvailableListenAddresses() {
        Set<String> result = new HashSet<String>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces != null && networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                for (Enumeration<InetAddress> addEnum = networkInterface.getInetAddresses(); addEnum.hasMoreElements(); ) {
                    InetAddress inetAddress = addEnum.nextElement();
                    result.add(inetAddress.getHostAddress());
                }
            }
        } catch (SocketException e) {
            LOGGER.warn("Could not get network interfaces.", e);
        }
        return result;
    }

    public static Collection<String> toDatasourceIds(Collection<DatasourceConfig> configs) {
        Set<String> ids = new HashSet<String>();
        if (configs != null) {
            for (DatasourceConfig datasourceConfig : configs) {
                ids.add(datasourceConfig.getId());
            }
        }
        return ids;
    }

    public static String toSqlLikeExpression(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    public static void removeDataForSources(DataStoreSession session, final Set<String> sourceIds) throws SQLException {
        LOGGER.debug("Removing data for " + sourceIds.size() + " datasource(s).");
        session.executeStatement(new DataStoreStatement() {
            public void execute(Connection connection) throws SQLException {
                SmartStatement statement = MyTunesRssUtils.createStatement(connection, "removeDataForSourceIds");
                statement.setItems("sourceIds", sourceIds);
                statement.execute();
            }
        });
        try {
            MyTunesRss.LUCENE_TRACK_SERVICE.deleteTracksForSourceIds(sourceIds);
        } catch (IOException e) {
            LOGGER.warn("Could not delete tracks from lucene index.", e);
        }
        LOGGER.debug("Recreating help tables.");
        session.executeStatement(new RecreateHelpTablesStatement());
        LOGGER.debug("Removing orphaned images.");
        session.executeStatement(new DataStoreStatement() {
            public void execute(Connection connection) throws SQLException {
                MyTunesRssUtils.createStatement(connection, "removeOrphanedImages").execute();
            }
        });
        LOGGER.debug("Updating statistics.");
        session.executeStatement(new UpdateStatisticsStatement());
    }

    public static Collection<DatasourceConfig> deepClone(Collection<DatasourceConfig> datasourceConfigs) {
        Collection<DatasourceConfig> deepClone = new ArrayList<DatasourceConfig>();
        for (DatasourceConfig datasourceConfig : datasourceConfigs) {
            deepClone.add(DatasourceConfig.copy(datasourceConfig));
        }
        return deepClone;
    }

    public static boolean canExecute(File file) {
        if (!SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_6)) {
            return file.exists() && file.isFile(); // the best we can check for if we don't have Java 6 or better
        }
        return file.canExecute();
    }

    public static String getLegalFileName(String name) {
        name = name.replace('/', '_');
        name = name.replace('\\', '_');
        name = name.replace('?', '_');
        name = name.replace('*', '_');
        name = name.replace(':', '_');
        name = name.replace('|', '_');
        name = name.replace('\"', '_');
        name = name.replace('<', '_');
        name = name.replace('>', '_');
        name = name.replace('`', '_');
        name = name.replace('\'', '_');
        return name;
    }

    public static String[] substringsBetween(String s, String left, String right) {
        List<String> tokens = new ArrayList<String>();
        if (StringUtils.isNotEmpty(s) && StringUtils.isNotEmpty(left) && StringUtils.isNotEmpty(right) && s.length() >= left.length() + right.length()) {
            int k;
            for (int i = s.indexOf(left); i > -1; i = s.indexOf(left, k + right.length())) {
                k = s.indexOf(right, i + 1);
                if (k == -1) {
                    break; // no more end tokens, we are done
                }
                // go right as far as possible while keeping the end boundary
                while (k + 1 + right.length() <= s.length() && right.equals(s.substring(k + 1, k + 1 + right.length()))) {
                    k++;
                }
                tokens.add(s.substring(i + left.length(), k));
                if (k + right.length() >= s.length()) {
                    break; // end of input string reached, we are done
                }
            }
        }
        return tokens.toArray(new String[tokens.size()]);
    }

    public static RequestLogHandler createJettyAccessLogHandler(String prefix, int retainDays, boolean extended, String tz) {
        File accessLogDir = new File(MyTunesRss.CACHE_DATA_PATH + "/accesslogs");
        if (!accessLogDir.exists()) {
            accessLogDir.mkdirs();
        }
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        NCSARequestLog requestLog = new NCSARequestLog(new File(accessLogDir, prefix + "-yyyy_mm_dd.log").getAbsolutePath());
        requestLog.setRetainDays(retainDays);
        requestLog.setAppend(true);
        requestLog.setExtended(extended);
        requestLog.setLogTimeZone(tz);
        requestLogHandler.setRequestLog(requestLog);
        return requestLogHandler;
    }

    public static boolean isExecutable(File executable) {
        return executable != null && executable.isFile() && executable.canExecute();
    }

    public static MVStore.Builder getMvStoreBuilder(String filename) {
        File dir = new File(MyTunesRss.CACHE_DATA_PATH, "mvstore");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, filename);
        if (file.exists()) {
            file.delete();
        }
        return new MVStore.Builder().fileStore(new FileStore()).fileName(file.getAbsolutePath());
    }

    public static <K, V> Map<K, V> openMvMap(MVStore store, String name) {
        return new InterruptSafeMvMap<K, V>(store.<K, V>openMap(name));
    }
}
