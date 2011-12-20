/*
 * Copyright (c) 2010. Codewave Software Michael Descher.
 * All rights reserved.
 */

package de.codewave.mytunesrss.webadmin;

import com.vaadin.terminal.Sizeable;
import com.vaadin.ui.*;
import de.codewave.mytunesrss.ItunesDatasourceConfig;
import de.codewave.mytunesrss.MyTunesRss;
import de.codewave.mytunesrss.MyTunesRssUtils;
import de.codewave.mytunesrss.datastore.statement.Track;
import de.codewave.mytunesrss.statistics.GetStatisticsEventsQuery;
import de.codewave.mytunesrss.statistics.SessionStartEvent;
import de.codewave.mytunesrss.statistics.StatEventType;
import de.codewave.mytunesrss.statistics.StatisticsEvent;
import de.codewave.mytunesrss.webadmin.statistics.DownVolumePerDayChartGenerator;
import de.codewave.mytunesrss.webadmin.statistics.ReportChartGenerator;
import de.codewave.mytunesrss.webadmin.statistics.SessionsPerDayChartGenerator;
import de.codewave.utils.sql.*;
import de.codewave.vaadin.SmartTextField;
import de.codewave.vaadin.VaadinUtils;
import de.codewave.vaadin.component.ProgressWindow;
import de.codewave.vaadin.component.SinglePanelWindow;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultXYDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addon.JFreeChartWrapper;
import sun.rmi.log.LogInputStream;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class StatisticsConfigPanel extends MyTunesRssConfigPanel {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsConfigPanel.class);

    private Form myConfigForm;
    private SmartTextField myStatisticsKeepTime;
    private DateField myReportFromDate;
    private DateField myReportToDate;
    private Select myReportType;
    private Button myGenerateReport;

    public void attach() {
        super.attach();
        init(getBundleString("statisticsConfigPanel.caption"), getComponentFactory().createGridLayout(1, 3, true, true));
        myConfigForm = getComponentFactory().createForm(null, true);
        myStatisticsKeepTime = getComponentFactory().createTextField("statisticsConfigPanel.statisticsKeepTime", getApplication().getValidatorFactory().createMinMaxValidator(1, 720));
        Calendar from = new GregorianCalendar();
        from.set(Calendar.DAY_OF_MONTH, from.getActualMinimum(Calendar.DAY_OF_MONTH));
        myReportFromDate = new DateField(getBundleString("statisticsConfigPanel.reportFrom"), from.getTime());
        myReportFromDate.setLenient(false);
        myReportFromDate.setDateFormat(MyTunesRssUtils.getBundleString(Locale.getDefault(), "common.dateFormat"));
        myReportFromDate.setResolution(DateField.RESOLUTION_DAY);
        Calendar to = new GregorianCalendar();
        to.set(Calendar.DAY_OF_MONTH, to.getActualMaximum(Calendar.DAY_OF_MONTH));
        myReportToDate = new DateField(getBundleString("statisticsConfigPanel.reportTo"), to.getTime());
        myReportToDate.setLenient(false);
        myReportToDate.setDateFormat(MyTunesRssUtils.getBundleString(Locale.getDefault(), "common.dateFormat"));
        myReportToDate.setResolution(DateField.RESOLUTION_DAY);
        myReportType = getComponentFactory().createSelect("statisticsConfigPanel.reportType", Arrays.asList(
                new SessionsPerDayChartGenerator(),
                new DownVolumePerDayChartGenerator()
        ));
        myGenerateReport = getComponentFactory().createButton("statisticsConfigPanel.createReport", this);
        myConfigForm.addField(myStatisticsKeepTime, myStatisticsKeepTime);
        addComponent(getComponentFactory().surroundWithPanel(myConfigForm, FORM_PANEL_MARGIN_INFO, getBundleString("statisticsConfigPanel.config.caption")));
        Form sendForm = getComponentFactory().createForm(null, true);
        sendForm.addField(myReportFromDate, myReportFromDate);
        sendForm.addField(myReportToDate, myReportToDate);
        sendForm.addField(myReportType, myReportType);
        sendForm.addField(myGenerateReport, myGenerateReport);
        addComponent(getComponentFactory().surroundWithPanel(sendForm, FORM_PANEL_MARGIN_INFO, getBundleString("statisticsConfigPanel.send.caption")));

        addDefaultComponents(0, 2, 0, 2, false);

        initFromConfig();
    }

    protected void initFromConfig() {
        myStatisticsKeepTime.setValue(MyTunesRss.CONFIG.getStatisticKeepTime(), 0, 365, 60);
    }

    protected void writeToConfig() {
        MyTunesRss.CONFIG.setStatisticKeepTime(myStatisticsKeepTime.getIntegerValue(60));
        MyTunesRss.CONFIG.save();
    }

    @Override
    protected boolean beforeSave() {
        boolean valid = VaadinUtils.isValid(myConfigForm);
        if (!valid) {
            ((MainWindow) VaadinUtils.getApplicationWindow(this)).showError("error.formInvalid");
        }
        return valid;
    }

    @Override
    public void buttonClick(Button.ClickEvent clickEvent) {
        if (clickEvent.getButton() == myGenerateReport) {
            //generateReport();
            try {
                ReportChartGenerator generator = (ReportChartGenerator) myReportType.getValue();
                Map<Day, List<StatisticsEvent>> eventsPerDay = createEmptyEventsPerDayMap();
                for (StatisticsEvent event : selectData(generator.getEventTypes())) {
                    eventsPerDay.get(new Day(new Date(event.getEventTime()))).add(event);
                }
                JFreeChartWrapper wrapper = new JFreeChartWrapper(generator.generate(eventsPerDay));
                Window chartWindow = new Window("@todo chart");
                Panel chartPanel = new Panel();
                chartPanel.addComponent(wrapper);
                chartPanel.setWidth((wrapper.getGraphWidth() + 20) + "px");
                chartWindow.setContent(chartPanel);
                getWindow().addWindow(chartWindow);
                chartWindow.center();
            } catch (SQLException e) {
                LOG.error("Could not create report chart.", e);
            }
        } else {
            super.buttonClick(clickEvent);
        }
    }

    private Map<Day, List<StatisticsEvent>> createEmptyEventsPerDayMap() {
        Map<Day, List<StatisticsEvent>> eventsPerDay = new HashMap<Day, List<StatisticsEvent>>();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime((Date)myReportFromDate.getValue());
        while (calendar.getTime().compareTo((Date)myReportToDate.getValue()) <= 0) {
            eventsPerDay.put(new Day(calendar.getTime()), new ArrayList<StatisticsEvent>());
            calendar.add(Calendar.DATE, 1);
        }
        return eventsPerDay;
    }

    private List<StatisticsEvent> selectData(StatEventType... types) throws SQLException {
        DataStoreSession tx = MyTunesRss.STORE.getTransaction();
        try {
            return tx.executeQuery(new GetStatisticsEventsQuery(
                    ((Date)myReportFromDate.getValue()).getTime(),
                    ((Date)myReportToDate.getValue()).getTime() + (1000L * 3600L * 24L) - 1L,
                    types
            )).getResults();
        } finally {
            tx.rollback();
        }
    }
}