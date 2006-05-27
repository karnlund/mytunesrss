package de.codewave.mytunesrss.settings;

import de.codewave.mytunesrss.*;
import org.apache.commons.logging.*;

import javax.swing.*;
import java.util.prefs.*;

/**
 * de.codewave.mytunesrss.settings.Settings
 */
public class Settings {
    private static final Log LOG = LogFactory.getLog(Settings.class);

    private JFrame myFrame;
    private JPanel myRootPanel;
    private JLabel myStatusLabel;
    private General myGeneralForm;
    private Options myOptionsForm;

    public JFrame getFrame() {
        return myFrame;
    }

    public General getGeneralForm() {
        return myGeneralForm;
    }

    public JPanel getRootPanel() {
        return myRootPanel;
    }

    public void init(JFrame frame) {
        myGeneralForm.init(this);
        myOptionsForm.init(this);
        myFrame = frame;
        setStatus(MyTunesRss.BUNDLE.getString("info.server.idle"), null);
    }

    public void setStatus(String text, String tooltipText) {
        if (text != null) {
            myStatusLabel.setText(text);
        }
        if (tooltipText != null) {
            myStatusLabel.setToolTipText(tooltipText);
        }
    }

    public void doQuitApplication() {
        if (MyTunesRss.WEBSERVER.isRunning()) {
            myGeneralForm.doStopServer();
        }
        if (!MyTunesRss.WEBSERVER.isRunning()) {
            Preferences.userRoot().node("/de/codewave/mytunesrss").putInt("window_x", myFrame.getLocation().x);
            Preferences.userRoot().node("/de/codewave/mytunesrss").putInt("window_y", myFrame.getLocation().y);
            updateConfigFromGui();
            MyTunesRss.CONFIG.save();
            PleaseWait.start(myFrame, null, "Shutting down database... please wait.", false, false, new PleaseWait.NoCancelTask() {
                public void execute() {
                    try {
                        MyTunesRss.STORE.destroy();
                    } catch (Exception e) {
                        if (LOG.isErrorEnabled()) {
                            LOG.error("Could not destroy the store.", e);
                        }
                    }
                }
            });
            System.exit(0);
        }
    }

    public void updateConfigFromGui() {
        myGeneralForm.updateConfigFromGui();
        myOptionsForm.updateConfigFromGui();
    }

    public void setGuiMode(GuiMode mode) {
        myGeneralForm.setGuiMode(mode);
        myOptionsForm.setGuiMode(mode);
        myRootPanel.validate();
    }
}