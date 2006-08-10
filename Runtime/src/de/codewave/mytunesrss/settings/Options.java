/*
 * Copyright (c) 2006, Codewave Software. All Rights Reserved.
 */

package de.codewave.mytunesrss.settings;

import de.codewave.mytunesrss.*;
import de.codewave.mytunesrss.datastore.statement.*;
import de.codewave.mytunesrss.task.*;
import org.apache.commons.logging.*;

import javax.swing.*;
import java.awt.event.*;
import java.sql.*;
import java.text.*;
import java.util.Date;

/**
 * de.codewave.mytunesrss.settings.Options
 */
public class Options {
    private static final Log LOG = LogFactory.getLog(Options.class);

    private JPanel myRootPanel;
    private boolean myUpdateOnStartInputCache;
    private JLabel myLastUpdatedLabel;
    private Settings mySettingsForm;
    private JCheckBox myUpdateOnStartInput;
    private JCheckBox myAutoStartServerInput;
    private JButton myProgramUpdateButton;
    private JSpinner myAutoUpdateDatabaseIntervalInput;
    private JCheckBox myAutoUpdateDatabaseInput;
    private JCheckBox myIgnoreTimestampsInput;
    private JButton myDeleteDatabaseButton;

    public void init(Settings settingsForm) {
        mySettingsForm = settingsForm;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                refreshLastUpdate();
            }
        });
        myUpdateOnStartInput.setSelected(MyTunesRss.CONFIG.isCheckUpdateOnStart());
        myAutoStartServerInput.setSelected(MyTunesRss.CONFIG.isAutoStartServer());
        if (myAutoStartServerInput.isSelected()) {
            myUpdateOnStartInput.setSelected(false);
            myUpdateOnStartInput.setEnabled(false);
        }
        myProgramUpdateButton.addActionListener(new ProgramUpdateButtonListener());
        myAutoStartServerInput.addActionListener(new AutoStartServerInputListener());
        SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(MyTunesRss.CONFIG.getAutoUpdateDatabaseInterval(), 1, 60, 1);
        myAutoUpdateDatabaseIntervalInput.setModel(spinnerNumberModel);
        myAutoUpdateDatabaseInput.setSelected(MyTunesRss.CONFIG.isAutoUpdateDatabase());
        myIgnoreTimestampsInput.setSelected(MyTunesRss.CONFIG.isIgnoreTimestamps());
        SwingUtils.enableElementAndLabel(myAutoUpdateDatabaseIntervalInput, MyTunesRss.CONFIG.isAutoUpdateDatabase());
        myAutoUpdateDatabaseInput.addActionListener(new AutoUpdateDatabaseInputListener());
        myDeleteDatabaseButton.addActionListener(new DeleteDatabaseButtonListener());
    }

    public void refreshLastUpdate() {
        SystemInformation systemInformation = null;
        try {
            systemInformation = MyTunesRss.STORE.executeQuery(new GetSystemInformationQuery());
        } catch (SQLException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Could not get last update time from database.", e);
            }

        }
        if (systemInformation.getLastUpdate() > 0) {
            Date date = new Date(systemInformation.getLastUpdate());
            myLastUpdatedLabel.setText(
                    MyTunesRss.BUNDLE.getString("settings.lastDatabaseUpdate") + " " + new SimpleDateFormat(MyTunesRss.BUNDLE.getString(
                            "settings.lastDatabaseUpdateDateFormat")).format(date));
        } else {
            myLastUpdatedLabel.setText(MyTunesRss.BUNDLE.getString("settings.databaseNotYetCreated"));
        }
        myRootPanel.validate();
    }

    public void updateConfigFromGui() {
        MyTunesRss.CONFIG.setCheckUpdateOnStart(myUpdateOnStartInput.isSelected());
        MyTunesRss.CONFIG.setAutoStartServer(myAutoStartServerInput.isSelected());
        MyTunesRss.CONFIG.setAutoUpdateDatabase(myAutoUpdateDatabaseInput.isSelected());
        MyTunesRss.CONFIG.setAutoUpdateDatabaseInterval((Integer)myAutoUpdateDatabaseIntervalInput.getValue());
        MyTunesRss.CONFIG.setIgnoreTimestamps(myIgnoreTimestampsInput.isSelected());
    }

    public void setGuiMode(GuiMode mode) {
        switch (mode) {
            case ServerRunning:
                myAutoStartServerInput.setEnabled(false);
                myUpdateOnStartInput.setEnabled(false);
                myProgramUpdateButton.setEnabled(false);
                myAutoUpdateDatabaseInput.setEnabled(false);
                myIgnoreTimestampsInput.setEnabled(false);
                myDeleteDatabaseButton.setEnabled(false);
                SwingUtils.enableElementAndLabel(myAutoUpdateDatabaseIntervalInput, false);
                break;
            case ServerIdle:
                myAutoStartServerInput.setEnabled(true);
                myUpdateOnStartInput.setEnabled(!myAutoStartServerInput.isSelected());
                myProgramUpdateButton.setEnabled(true);
                myAutoUpdateDatabaseInput.setEnabled(true);
                myIgnoreTimestampsInput.setEnabled(true);
                myDeleteDatabaseButton.setEnabled(true);
                SwingUtils.enableElementAndLabel(myAutoUpdateDatabaseIntervalInput, myAutoUpdateDatabaseInput.isSelected());
                break;
        }
    }

    public class ProgramUpdateButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    new Updater(mySettingsForm.getFrame()).checkForUpdate(false);
                }
            });
        }
    }

    public class AutoStartServerInputListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (myAutoStartServerInput.isSelected()) {
                myUpdateOnStartInputCache = myUpdateOnStartInput.isSelected();
                myUpdateOnStartInput.setSelected(false);
                myUpdateOnStartInput.setEnabled(false);
            } else {
                myUpdateOnStartInput.setSelected(myUpdateOnStartInputCache);
                myUpdateOnStartInput.setEnabled(true);
            }
            myRootPanel.validate();
        }
    }

    public class AutoUpdateDatabaseInputListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            SwingUtils.enableElementAndLabel(myAutoUpdateDatabaseIntervalInput, myAutoUpdateDatabaseInput.isSelected());
            myRootPanel.validate();
        }
    }

    public class DeleteDatabaseButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            String optionOk = MyTunesRss.BUNDLE.getString("ok");
            String optionCancel = MyTunesRss.BUNDLE.getString("cancel");
            Object option = SwingUtils.showOptionsMessage(mySettingsForm.getFrame(), JOptionPane.QUESTION_MESSAGE, null, MyTunesRss.BUNDLE.getString(
                    "question.deleteDatabase"), new Object[] {optionCancel, optionOk});
            if (optionOk.equals(option)) {
                PleaseWait.start(mySettingsForm.getFrame(),
                                 null,
                                 MyTunesRss.BUNDLE.getString("pleaseWait.recreatingDatabase"),
                                 false,
                                 false,
                                 new RecreateDatabaseTask());
            }
        }
    }
}