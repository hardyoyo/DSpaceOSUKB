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

package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;

import java.sql.SQLException;

import java.text.DateFormat;
import java.text.ParseException;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;

import org.apache.commons.lang.StringUtils;

import org.apache.commons.validator.routines.DateValidator;

import org.apache.log4j.Logger;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import org.apache.solr.client.solrj.SolrServerException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;

import org.dspace.app.xmlui.utils.UIException;

import org.dspace.app.xmlui.wing.element.*;

import org.dspace.app.xmlui.wing.WingException;

import org.dspace.authorize.AuthorizeException;

import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;

import org.xml.sax.SAXException;

/**
 * Use a form to dynamically generate a variety of reports.
 *
 * @author "Ryan McGowan" ("mcgowan.98@osu.edu")
 * @version
 */
public class ReportGenerator extends AbstractDSpaceTransformer
{
    /**
     * A logger for this class.
     */
    private static Logger log = Logger.getLogger(DashboardViewer.class);
    /**
     * The minimum date for the from or to field to be. (e.g. The beggining of
     * DSpace)
     */
    private static String MINIMUM_DATE = "2008-01-01";
    /**
     * A set containing all of the fields that must exist for a request to
     * generate a report.
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

    static {
        //Add required fields to the REQUIRED_FIELDS set
        ReportGenerator.REQUIRED_FIELDS = new HashSet<String>();
        ReportGenerator.REQUIRED_FIELDS.add("report_name");
        ReportGenerator.REQUIRED_FIELDS.add("gaplength");
        //Add valid field values to VALID_REPORTS
        ReportGenerator.VALID_REPORTS = new HashSet<String>();
        ReportGenerator.VALID_REPORTS.add("some_yearly_thing");
        ReportGenerator.VALID_REPORTS.add("basic");
        //Add valid field values to VALID_GAP_LENGTHS
        ReportGenerator.VALID_GAP_LENGTHS = new HashSet<String>();
        ReportGenerator.VALID_GAP_LENGTHS.add("monthly");
        ReportGenerator.VALID_GAP_LENGTHS.add("yearly");
    }

    /**
     * {@inheritDoc}
     * @see org.dspace.app.xmlui.cocoon.DSpaceTransformer#addPageMeta(PageMeta)
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        // Set the page title
        pageMeta.addMetadata("title").addContent("Report Generator");

        pageMeta.addTrailLink(contextPath + "/","KB Home");
        pageMeta.addTrailLink(contextPath + "/report-generator", "Report Generator");
    }

    /**
     * {@inheritDoc}
     * @see org.dspace.app.xmlui.cocoon.DSpaceTransformer#addBody(Body)
     */
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        Division division = body.addDivision("report-generator", "primary");
        division.setHead("Report Generator");
        division.addPara("Used to generate reports with an arbitrary date range that can be split yearly or monthly.");
        Division search = body.addInteractiveDivision("choose-report", contextPath+"/report-generator", Division.METHOD_GET, "primary");
        org.dspace.app.xmlui.wing.element.List actionsList = search.addList("actions", "form");

        Request request = ObjectModelHelper.getRequest(objectModel);
        Map<String, String> params = new HashMap<String, String>();
        for (Enumeration<String> paramNames = (Enumeration<String>) request.getParameterNames();
                paramNames.hasMoreElements();) {
            String param = paramNames.nextElement();
            params.put(param, request.getParameter(param));
        }
        try {
            params = ReportGenerator.checkParameters(params);
        } catch (Exception e) {
            log.error("Bad Request. Could may not have all required parameters: " + e.getMessage());
        }
        if (params != null) {
            //Run the report
            boolean success = false;
            try {
                success = this.runReport(params);
            } catch (Exception e) {
                log.error("Failed to run report with given params: " +
                        params.toString() + "\n" + e.getMessage());
            }

            if (success) {
                this.displayResults();
            }
        }

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
            boolean isDef = false;
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
        if (params.containsKey("from")) {
            //Set default value if it exists
            from.setValue(params.get("from"));
        }

        Item dateTo = actionsList.addItem();
        Text to = dateTo.addText("to");
        to.setLabel("To");
        if (params.containsKey("to")) {
            //Set default value if it exists
            to.setValue(params.get("to"));
        }

        //Add whether it is fiscal or not
        Item fiscality = actionsList.addItem();
        CheckBox isFiscal = fiscality.addCheckBox("fiscal");
        isFiscal.setLabel("Round date range to fiscal years?");
        //Set up fiscal option with the correct default
        isFiscal.addOption(params.containsKey("fiscal") &&
                params.get("fiscal").equals("1"), 1, "");

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
    private static Map<String,String> checkParameters(Map<String,String> params) throws InvalidFormatException, ParseException {
        //Create dateValidator and min and max dates
        DateValidator dateValidator = new DateValidator();
        DateFormat df = DateFormat.getDateTimeInstance();
        Date maximumDate = new Date();
        Date minimumDate = null;
        if (dateValidator.isValid(ReportGenerator.MINIMUM_DATE)) {
            minimumDate = df.parse(ReportGenerator.MINIMUM_DATE);
        } else {
            throw new InvalidFormatException("Given date (\""
                    + ReportGenerator.MINIMUM_DATE + "\") is not valid.");
        }
        if (!params.keySet().containsAll(ReportGenerator.REQUIRED_FIELDS)) {
            return null;
        } else {
            //Check the to and from dates
            Date fromDate = null;
            Date toDate = null;
            boolean validToAndFrom = true;
            boolean hasFrom = params.containsKey("from");
            boolean hasTo = params.containsKey("to");
            if (hasFrom || hasTo) {
                if (hasFrom) {
                    fromDate = df.parse(params.get("from"));
                    log.debug("from: " + params.get("from"));
                    validToAndFrom = validToAndFrom && dateValidator.compareDates(minimumDate, fromDate, null) <= 0;
                }
                if (hasTo) {
                    toDate = df.parse(params.get("to"));
                    log.debug("to: " + params.get("to"));
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
                    return null;
                }
            }

            //Check fiscal
            if (params.containsKey("fiscal")) {
                log.debug("fiscal: " + params.get("fiscal"));
                if (Integer.parseInt(params.get("fiscal")) != 1) {
                    return null;
                }
            }

            //Check report_name
            log.debug("report_name: " + params.get("report_name"));
            if (!ReportGenerator.VALID_REPORTS.contains(params.get("report_name"))) {
                return null;
            }

            //Check gap length
            log.debug("gaplength: " + params.get("gaplength"));
            if (!ReportGenerator.VALID_GAP_LENGTHS.contains(params.get("report_name"))) {
                return null;
            }
            return params;
        }
    }

    /**
     * Runs a report based on the given parameters. These parameters were
     * gathered by the form descirbed in {@link ReportGenerator.addBody()}.
     *
     * @param params A map describing a set of key value pairs used to determine
     * what report to run and what values to use when running it.
     * @return Successfully generated the specified report.
     * @throws Exception
     */
    private boolean runReport(Map<String,String> params) throws Exception {
        String report = params.get("report_name");
        //TODO: Get data for reports
        if (report.equals("basic")) {
            String query = "type:0 AND owningComm:[0 TO 9999999]"
                + " AND -dns:msnbot-* AND -isBot:true"
                + " AND ";
            log.info(StringUtils.capitalize(report) + " Report Query: " + query);
            ObjectCount[] resultCounts;
            try {
                resultCounts = SolrLogger.queryFacetField(query, "", "id", 50, true, null);
            } catch (SolrServerException e) {
                log.error(StringUtils.capitalize(report) + " Report Query Failed: \""
                        + query + "\"\n" + e.getMessage());
            }
        } else if (report.equals("")) {
        } else {
            throw new Exception("Report name (\"" + report
                    + "\") is not valid.");
        }
        return false;
    }

    private void displayResults() {
        //TODO: Display Results
    }
}
