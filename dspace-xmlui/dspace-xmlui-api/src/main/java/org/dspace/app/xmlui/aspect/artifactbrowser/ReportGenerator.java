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

import java.util.Map.Entry;

import java.util.Map;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;

import org.apache.commons.;

import org.apache.log4j.Logger;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;

import org.dspace.app.xmlui.utils.UIException;

import org.dspace.app.xmlui.wing.element.*;

import org.dspace.app.xmlui.wing.WingException;

import org.dspace.authorize.AuthorizeException;

import org.xml.sax.SAXException;


/**
 * Display a dashboard of information about the site.
 *
 *
 * @author Peter Dietz
 */
public class ReportGenerator extends AbstractDSpaceTransformer
{
    private static Logger log = Logger.getLogger(DashboardViewer.class);

    /**
     * Add a page title and trail links.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        // Set the page title
        pageMeta.addMetadata("title").addContent("Report Generator");

        pageMeta.addTrailLink(contextPath + "/","KB Home");
        pageMeta.addTrailLink(contextPath + "/report-generator", "Report Generator");
    }

    /**
     * Add a community-browser division that includes refrences to community and
     * collection metadata.
     */
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        Division division = body.addDivision("report-generator", "primary");
        division.setHead("Report Generator");
        division.addPara("Used to generate reports with an arbitrary date range that can be split yearly or monthly.");
        Division search = body.addInteractiveDivision("choose-report", contextPath+"/report-generator", Division.METHOD_GET, "primary");
        org.dspace.app.xmlui.wing.element.List actionsList = search.addList("actions", "form");

        Request request = ObjectModelHelper.getRequest(objectModel);
        Map<String, String> params = ReportGenerator.checkParameters(request);
        String reportName = request.getParameter("report_name");

        //Create radio buttons to select report type
        actionsList.addLabel("Label for action list");
        Item actionSelectItem = actionsList.addItem();
        Radio actionSelect = actionSelectItem.addRadio("report_name");
        actionSelect.setRequired();
        actionSelect.setLabel("Generate a report of type");
        actionSelect.addOption(true, "basic", "Basic");
        actionSelect.addOption(false, "atl", "Yearly Library thing");

        //Create Date Range part of form
        Item dateFrom = actionsList.addItem();
        Text from = dateFrom.addText("from");
        from.setLabel("From");
        Item dateTo = actionsList.addItem();
        Text to = dateTo.addText("to");
        to.setLabel("To");

        //Add whether it is fiscal or not
        Item fiscality = actionsList.addItem();
        CheckBox isFiscal = fiscality.addCheckBox("fiscal");
        isFiscal.setLabel("Round date range to fiscal years?");
        isFiscal.addOption(false, 1, "");

        //Add drop down to select gap size
        Item gapLengthItem = actionsList.addItem();
        Select gapLength = gapLengthItem.addSelect("gaplength");
        gapLength.setRequired();
        gapLength.setLabel("Gap Length");
        gapLength.addOption(true, "monthly", "Monthly");
        gapLength.addOption(false, "yearly", "Yearly");

        Para buttons = search.addPara();
        buttons.addButton("submit_add").setValue("Generate");
    }

    public static Map<String,String> checkParameters(Request request) {
        Map<String,String> params = (Map<String,String>) request.getParameterMap();
        int validCount = 0;
        DateValidator dateValidator = new DateValidator();
        for (Entry<String,String> pair : params.entrySet()) {
            if (pair.getKey().equals("to") || pair.getKey().equals("from")) {
            }
        }

        return params;
    }
}
