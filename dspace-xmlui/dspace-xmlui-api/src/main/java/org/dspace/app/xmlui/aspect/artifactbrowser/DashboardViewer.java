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
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.core.Constants;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.xml.sax.SAXException;




/**
 * Display a dashboard of information about the site.
 *
 *
 * @author Peter Dietz
 */
public class DashboardViewer extends AbstractDSpaceTransformer
{
    private static Logger log = Logger.getLogger(DashboardViewer.class);

    /** Language Strings */
    public static final Message T_dspace_home =
        message("xmlui.general.dspace_home");

    public static final Message T_title =
        message("xmlui.ArtifactBrowser.CommunityBrowser.title");

    public static final Message T_trail =
        message("xmlui.ArtifactBrowser.CommunityBrowser.trail");
    
    public static final Message T_head =
        message("xmlui.ArtifactBrowser.CommunityBrowser.head");

    public static final Message T_select =
        message("xmlui.ArtifactBrowser.CommunityBrowser.select");



    /**
     * Add a page title and trail links.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }

    /**
     * Add a community-browser division that includes refrences to community and
     * collection metadata.
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division division = body.addDivision("comunity-browser", "primary");
        division.setHead("Knowledge Bank Dashboard");
        division.addPara("A collection of statistical queries about the size and traffic of the KB.");

        queryItemGrowthPerMonth(division);
        queryNumberOfItemsPerComm(division);
        //MaureenQuery1()
        //TscheraQuery1()


    }

    private void queryItemGrowthPerMonth(Division division) throws SQLException, WingException
    {
        String query = "SELECT to_char(date_trunc('month', t1.ts), 'YYYY-MM') AS yearmo, count(*) as countitem " +
            "FROM ( SELECT to_timestamp(text_value, 'YYYY-MM-DD') AS ts FROM metadatavalue, item " +
            "WHERE metadata_field_id = 12 AND metadatavalue.item_id = item.item_id AND item.in_archive=true	) t1 " +
            "GROUP BY date_trunc('month', t1.ts) order by yearmo asc;";
        TableRowIterator tri = DatabaseManager.query(context, query);
        List itemStatRows = tri.toList();

        division.addDivision("chart_div");
        Button toggleButton = division.addPara().addButton("items_added_dataset_button", "show_hide_dataset");
        toggleButton.setValue("Show/Hide Dataset");
        
        Table itemTable = division.addTable("items_added_monthly", itemStatRows.size(), 3);
        Row headerRow = itemTable.addRow(Row.ROLE_HEADER);
        headerRow.addCell().addContent("Date");
        headerRow.addCell().addContent("#Items Added");
        headerRow.addCell().addContent("Total #Items");
        Integer totalItems = 0;

        String html = "<script type='text/javascript' src='https://www.google.com/jsapi'></script>" +
            "<script type='text/javascript'>" +
            " google.load('visualization', '1', {'packages':['annotatedtimeline']});" +
            " google.setOnLoadCallback(drawChart);" +
            " function drawChart() {" +
            "  var data = new google.visualization.DataTable();" +
            "  data.addColumn('date', 'Date');" +
            "  data.addColumn('number', 'Items Added');" +
            "  data.addColumn('number', 'Total Items');" +
            "  data.addRows([";

        for(int i=0; i<itemStatRows.size();i++)
        {
            TableRow row = (TableRow) itemStatRows.get(i);
            log.debug(row.toString());
            String date = row.getStringColumn("yearmo");
            Long numItems = row.getLongColumn("countitem");
            totalItems += numItems.intValue();
            Row dataRow = itemTable.addRow();
            dataRow.addCell("date",Cell.ROLE_DATA,null).addContent(date);
            dataRow.addCell("items_added",Cell.ROLE_DATA,null).addContent(numItems.intValue());
            dataRow.addCell("items_total",Cell.ROLE_DATA, null).addContent(totalItems);

            String[] yearMonthSplit = date.split("-");
            if(i>0)
            {
                html = html + ",";
            }
            html = html + "[new Date("+yearMonthSplit[0]+", "+yearMonthSplit[1]+" ,1), "+numItems.toString()+", "+totalItems+"]";
        }
        html = html + "]); var chart = new google.visualization.AnnotatedTimeLine(document.getElementById('chart_div'));" +
            " chart.draw(data, {displayAnnotations: true}); }</script>";
        

        //division.addSimpleHTMLFragment(false, "&lt;![CDATA["+ html + " <div id='chart_div' style='width: 700px; height: 240px;'></div> ]]&gt;");
    }

    /**
     * In monthly intervals, find out the number that were in the KB.
     */
    private void queryNumberOfItemsPerComm(Division division) throws SQLException, WingException
    {
        String query = "SELECT to_char(date_trunc('month', t1.ts), 'YYYY-MM') AS yearmo, community_id," +
            "count(*) as numitems FROM 	(	SELECT to_timestamp(text_value, 'YYYY-MM-DD') AS ts, community2item.community_id " +
            "FROM metadatavalue, community2item, item	WHERE metadata_field_id = 12 AND community2item.item_id = metadatavalue.item_id " +
            "AND metadatavalue.item_id = item.item_id AND item.in_archive=true 	) t1 GROUP BY date_trunc('month', t1.ts), " +
            "community_id order by community_id asc, yearmo desc;";
        TableRowIterator tri = DatabaseManager.query(context, query);
        List itemStatRows = tri.toList();
        
        Table itemTable = division.addTable("items_added_per_comm", itemStatRows.size(), 3);
        Row headerRow = itemTable.addRow(Row.ROLE_HEADER);
        headerRow.addCell().addContent("YearMonth");
        headerRow.addCell().addContent("community_id");
        headerRow.addCell().addContent("num items");

        for(int i=0; i<itemStatRows.size();i++)
        {
            TableRow row = (TableRow) itemStatRows.get(i);
            log.debug(row.toString());
            String date = row.getStringColumn("yearmo");
            Integer community_id = row.getIntColumn("community_id");
            Long numItems = row.getLongColumn("numitems");
            Row dataRow = itemTable.addRow();
            dataRow.addCell().addContent(date);
            dataRow.addCell().addContent(community_id);

            dataRow.addCell().addContent(numItems.intValue());
        }
    }
}
