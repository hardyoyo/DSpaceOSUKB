/*
 * DashboardViewer.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.xmlui.aspect.dashboard;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cocoon.environment.Request;

import org.apache.commons.lang.StringUtils;

import org.apache.commons.validator.routines.DateValidator;

import org.apache.log4j.Logger;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import org.dspace.app.xmlui.wing.element.*;

import org.dspace.app.xmlui.wing.WingException;

import org.dspace.content.DSpaceObject;

/**
 * Use a form to dynamically generate a variety of reports.
 *
 * @author "Ryan McGowan" ("mcgowan.98@osu.edu")
 * @version
 */
public class ReportGenerator
{
    /**
     * A logger for this class.
     */
    private static Logger log = Logger.getLogger(ReportGenerator.class);
    /**
     * The minimum date for the from or to field to be. (e.g. The beginning of DSpace)
     */
    private static String MINIMUM_DATE = "2008-01-01";
    /**
     * A set containing all of the fields that must exist for a request to generate a report.
     */
    private static Set<String> REQUIRED_FIELDS;
    /**
     * Valid values for gaplength parameter.
     */
    private static Set<String> VALID_GAP_LENGTHS;
    /**
     * Valid values for report_name parameter.
     */
    private static Set<String> VALID_REPORTS;

    private Map<String, String> params;

    public Date getDateStart() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if(! params.containsKey("from")) {
            return null;
        }

        try {
            return dateFormat.parse(params.get("from"));
        } catch (ParseException e) {
            log.error("fromDate parseError"+e.getMessage());
            return null;
        }
    }

    public Date getDateEnd() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        if(! params.containsKey("to")) {
            return null;
        }

        try {
            return dateFormat.parse(params.get("to"));
        } catch (ParseException e) {
            log.error("toDate parseError"+e.getMessage());
            return null;
        }
    }



    public Boolean getParamsValid() {
        return (getDateStart() != null && getDateEnd() != null);
    }


    static {
        //Add required fields to the REQUIRED_FIELDS set
        ReportGenerator.REQUIRED_FIELDS = new HashSet<String>();
        ReportGenerator.REQUIRED_FIELDS.add("report_name");
        ReportGenerator.REQUIRED_FIELDS.add("gaplength");
        //Add valid field values to VALID_REPORTS
        ReportGenerator.VALID_REPORTS = new HashSet<String>();
        ReportGenerator.VALID_REPORTS.add("advanced");
        ReportGenerator.VALID_REPORTS.add("arl");
        ReportGenerator.VALID_REPORTS.add("basic");
        //Add valid field values to VALID_GAP_LENGTHS
        ReportGenerator.VALID_GAP_LENGTHS = new HashSet<String>();
        ReportGenerator.VALID_GAP_LENGTHS.add("monthly");
        ReportGenerator.VALID_GAP_LENGTHS.add("yearly");
    }


    /**
     * {@inheritDoc}
     * @see org.dspace.app.xmlui.cocoon.DSpaceTransformer#addBody(Body)
     */
    public void addReportGeneratorForm(Division parentDivision, DSpaceObject dso, Request request) {
        try {
            Division division = parentDivision.addDivision("report-generator", "primary");

            division.setHead("Report Generator");
            division.addPara("Used to generate reports with an arbitrary date range"
                    + " that can be split yearly or monthly.");


            Division search = parentDivision.addInteractiveDivision("choose-report", request.getRequestURI(), Division.METHOD_GET, "primary");
            org.dspace.app.xmlui.wing.element.List actionsList = search.addList("actions", "form");

            params = new HashMap<String, String>();
            for (Enumeration<String> paramNames = (Enumeration<String>) request.getParameterNames(); paramNames.hasMoreElements(); ) {
                String param = paramNames.nextElement();
                params.put(param, request.getParameter(param));
            }

            params = ReportGenerator.checkAndNormalizeParameters(params);

            //Create radio buttons to select report type
            actionsList.addLabel("Label for action list");
            Item actionSelectItem = actionsList.addItem();
            Radio actionSelect = actionSelectItem.addRadio("report_name");
            actionSelect.setRequired();
            actionSelect.setLabel("Generate a report of type");

            //Set up report_name options with the correct default
            boolean hasReportName = params.containsKey("report_name");
            for (String rep : ReportGenerator.VALID_REPORTS) {
                String prettyRep = StringUtils.capitalize(rep.replaceAll("_", " "));
                if (prettyRep.equals("Arl")) prettyRep = "ARL"; //ARL gets special treatment
                boolean isDef;
                if (hasReportName) {
                    isDef = params.get("report_name").equals(rep);
                } else {
                    isDef = rep.equals("basic");
                }
                actionSelect.addOption(isDef, rep, prettyRep);
            }

            //Create Date Range part of form
            Item dateFrom = actionsList.addItem();
            Text from = dateFrom.addText("from");
            from.setLabel("From");
            from.setHelp("The start date of the report, ex 2008-01-01");
            if (params.containsKey("from")) {
                //Set default value if it exists
                from.setValue(params.get("from"));
            }

            Item dateTo = actionsList.addItem();
            Text to = dateTo.addText("to");
            to.setLabel("To");
            to.setHelp("The end date of the report, ex 2010-12-31");
            if (params.containsKey("to")) {
                //Set default value if it exists
                to.setValue(params.get("to"));
            }

            //Add whether it is fiscal or not
            Item fiscality = actionsList.addItem();
            CheckBox isFiscal = fiscality.addCheckBox("fiscal");
            isFiscal.setLabel("Use Fiscal Years?");
            //Set up fiscal option with the correct default
            isFiscal.addOption(params.containsKey("fiscal") && params.get("fiscal").equals("1"), 1, "");

            //Add drop down to select gap size
            Item gapLengthItem = actionsList.addItem();
            Select gapLength = gapLengthItem.addSelect("gaplength");
            gapLength.setRequired();
            gapLength.setLabel("Gap Length");

            //Set up gap length options with the correct default
            boolean hasGapLength = params.containsKey("gaplength");
            for (String gap : ReportGenerator.VALID_GAP_LENGTHS) {
                String prettyGap = StringUtils.capitalize(gap.replaceAll("_", " "));
                boolean isDef = false;
                if (hasGapLength) {
                    isDef = params.get("gaplength").equals(gap);
                } else {
                    isDef = gap.equals("monthly");
                }
                gapLength.addOption(isDef, gap, prettyGap);
            }

            Para buttons = search.addPara();
            buttons.addButton("submit_add").setValue("Generate");
        } catch (WingException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Checks the parameters of the given request to see if they fit the
     * necessary criteria to run generate a report. The following must be true:
     *
     *  * from - Must be convertable to a valid date that is greater than the
     *    miniumum date and also less than or equal to the current date.
     *  * to - Must be convertable to a valid date that is greater than from
     *    and equal to or less than the current date.
     *
     * @return A map of valid parameters to their values.
     * @throws InvalidFormatException
     * @throws ParseException
     */
    private static Map<String,String> checkAndNormalizeParameters(Map<String,String> params)  {
        try {

            //Create dateValidator and min and max dates
            DateValidator dateValidator = new DateValidator(false, DateFormat.SHORT);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            Date maximumDate = new Date();
            Date minimumDate = dateFormat.parse(ReportGenerator.MINIMUM_DATE);



            if (!params.keySet().containsAll(ReportGenerator.REQUIRED_FIELDS)) {
                log.error("Request did not contain all required fields.");
            }

            //Check the to and from dates
            Date fromDate = null;
            Date toDate = null;
            boolean validToAndFrom = true;
            boolean hasFrom = params.containsKey("from") && params.get("from").length() > 0;
            boolean hasTo = params.containsKey("to") && params.get("to").length() > 0;

            if (hasFrom || hasTo) {
                if (hasFrom) {
                    fromDate = dateFormat.parse(params.get("from"));
                    params.put("from", dateFormat.format(fromDate));
                    validToAndFrom = validToAndFrom && dateValidator.compareDates(minimumDate, fromDate, null) <= 0;
                }
                if (hasTo) {
                    toDate = dateFormat.parse(params.get("to"));
                    params.put("to", dateFormat.format(toDate));
                    validToAndFrom = validToAndFrom && dateValidator.compareDates(toDate, maximumDate, null) <= 0;
                }
                if (hasFrom && hasTo) {
                    //Make sure hasFrom <= hasTo
                    validToAndFrom = validToAndFrom && dateValidator.compareDates(fromDate, toDate, null) <= 0;
                } else if (hasFrom && !hasTo) {
                    //Make sure hasFrom <= the max date
                    validToAndFrom = validToAndFrom && dateValidator.compareDates(fromDate, maximumDate, null) <= 0;
                } else {
                    //hasTo && !hasFrom
                    //Make sure hasTo >= the min date
                    validToAndFrom = validToAndFrom && dateValidator.compareDates(minimumDate, toDate, null) <= 0;
                }
                // Short circuit if the to and from dates are not valid
                if (!validToAndFrom) {
                    log.error("To and from dates are not within max/min or are not in order. "+ params.get("from") + " -> " + params.get("to"));
                    return null;
                }

                //Check fiscal
                if (params.containsKey("fiscal")) {
                    log.debug("fiscal: " + params.get("fiscal"));
                    if (Integer.parseInt(params.get("fiscal")) != 1) {
                        log.error("Fiscal field did not contain a proper value: " + params.get("fiscal"));
                    }
                }

                //Check report_name
                log.debug("report_name: " + params.get("report_name"));
                if (!ReportGenerator.VALID_REPORTS.contains(params.get("report_name"))) {
                    log.error("Invalid report name: " + params.get("report_name"));
                }

                //Check gap length
                log.debug("gaplength: " + params.get("gaplength"));
                if (!ReportGenerator.VALID_GAP_LENGTHS.contains(params.get("gaplength"))) {
                    log.error("Invalid gaplength: " + params.get("gaplength"));
                }
            }
            return params;
        } catch (ParseException e) {
            log.error("ParseFormatException likely means a date format failed. "+e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }

    /**
     * Runs a report based on the given parameters. These parameters were
     * gathered by the form described in ReportGenerator.
     *
     * @param params A map describing a set of key value pairs used to determine
     * what report to run and what values to use when running it.
     * @return Successfully generated the specified report.
     * @throws Exception
     */
    private boolean runReport(Map<String,String> params, Division division) throws Exception {
        String report = params.get("report_name");
        //TODO: Get data for reports

        //Print out what report should run for reference.
        String runningReport = ReportGenerator.describeRunningReport(params);
        division.addPara(runningReport);

        if (report.equals("basic")) {
            //String query = "type:0 AND owningComm:[0 TO 9999999]"
                //+ " AND -dns:msnbot-* AND -isBot:true"
                //+ " AND ";
            //log.info(StringUtils.capitalize(report) + " Report Query: " + query);
            //ObjectCount[] resultCounts;
            try {
                //resultCounts = SolrLogger.queryFacetField(query, "", "id", 50, true, null);
                //this.addFilesInContainer(division, this.collection, params.get("from"), params.get("to"));
            } catch (Exception e) {
                //log.error(StringUtils.capitalize(report) + " Report Query Failed: \""
                        //+ query + "\"\n" + e.getMessage());
            }
        } else if (report.equals("advanced")) {
        } else if (report.equals("arl")) {
        } else {
            throw new Exception("Report name (\"" + report
                    + "\") is not valid.");
        }
        return false;
    }

    public static String describeRunningReport(Map<String,String> params) {
        String report = params.get("report_name");
        String from = params.get("from");
        String to = params.get("to");
        String fiscal = "";
        if (params.get("fiscal") == "1") {
            fiscal = " (rounded to fiscal years)";
        }
        String gapLength = params.get("gaplength");
        
        return "Running " + report + " report from " + from + " to " + to +
            fiscal + " compounded " + gapLength + ".";
    }
}