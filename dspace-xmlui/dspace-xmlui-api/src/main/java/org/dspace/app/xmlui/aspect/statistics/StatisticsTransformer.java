/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statistics;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.content.*;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.xml.sax.SAXException;

public class StatisticsTransformer extends AbstractDSpaceTransformer {

    private static Logger log = Logger.getLogger(StatisticsTransformer.class);

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_head_title = message("xmlui.statistics.title");
    private static final Message T_statistics_trail = message("xmlui.statistics.trail");
    private static final String T_head_visits_total = "xmlui.statistics.visits.total";
    private static final String T_head_visits_month = "xmlui.statistics.visits.month";
    private static final String T_head_visits_views = "xmlui.statistics.visits.views";
    private static final String T_head_visits_countries = "xmlui.statistics.visits.countries";
    private static final String T_head_visits_cities = "xmlui.statistics.visits.cities";
    private static final String T_head_visits_bitstream = "xmlui.statistics.visits.bitstreams";

    /**
     * Add a page title and trail links
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        //Try to find our dspace object
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        if (dso != null) {
            HandleUtil.buildHandleTrail(dso, pageMeta, contextPath);
        }
        pageMeta.addTrailLink(contextPath + "/handle" + (dso != null && dso.getHandle() != null ? "/" + dso.getHandle() : "/statistics"), T_statistics_trail);

        // Add the page title
        pageMeta.addMetadata("title").addContent(T_head_title);
    }

    /**
     * What to add at the end of the body
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {

        //Try to find our dspace object
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

		try
		{
			if(dso != null)
			{
				renderViewer(body, dso);
			}
			else
			{
				renderHome(body);
			}

        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
        }

    }

    public void renderHome(Body body) throws WingException {

        Division home = body.addDivision("home", "primary repository");
        Division division = home.addDivision("stats", "secondary stats");
        division.setHead(T_head_title);
        /*
		try {

			StatisticsTable statisticsTable = new StatisticsTable(
					new StatisticsDataVisits());

			statisticsTable.setTitle(T_head_visits_month);
			statisticsTable.setId("tab1");

			DatasetTimeGenerator timeAxis = new DatasetTimeGenerator();
			timeAxis.setDateInterval("month", "-6", "+1");
			statisticsTable.addDatasetGenerator(timeAxis);

			addDisplayTable(division, statisticsTable);

		} catch (Exception e) {
			log.error("Error occurred while creating statistics for home page",
					e);
		}
		*/
        try {
            /** List of the top 10 items for the entire repository **/
            StatisticsListing statListing = new StatisticsListing( new StatisticsDataVisits());

            statListing.setTitle(T_head_visits_total);
            statListing.setId("list1");

            //Adding a new generator for our top 10 items without a name length delimiter
            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
            dsoAxis.addDsoChild(Constants.ITEM, 10, false, -1);
            statListing.addDatasetGenerator(dsoAxis);

            //Render the list as a table
            addDisplayListing(division, statListing);

        } catch (Exception e) {
            log.error("Error occurred while creating statistics for home page", e);
        }

    }

    public void renderViewer(Body body, DSpaceObject dso) throws WingException {

        Division home = body.addDivision(
                Constants.typeText[dso.getType()].toLowerCase() + "-home",
                "primary repository " + Constants.typeText[dso.getType()].toLowerCase());

        // Build the collection viewer division.
        Division division = home.addDivision("stats", "secondary stats");
        division.setHead(T_head_title);

        // 1 - Number of Items in The Container (Community/Collection) (monthly and cumulative for the year)
        if(dso instanceof Collection || dso instanceof Community) {
            addItemsInContainer(dso, division);
        }

        // 2 - Number of Files in The Container (monthly and cumulative)
        if(dso instanceof Collection || dso instanceof Community) {
            addFilesInContainer(dso, division);
        }

        // 3 - Number of File Downloads in the container (monthly and cumulative)
        if(dso instanceof Collection || dso instanceof Community) {
            addFileDownloadsInContainer(dso, division);
        }

        // 4 - Unique visitors
        if(dso instanceof Collection || dso instanceof Community) {
            addUniqueVisitorsToContainer(dso, division);
        }

        // 5 - Visits to the Collection by Type of Domain (i.e. .com. .net. .org. .edu. .gov.)
        //@TODO Cannot search solr with a leading wildcard *.com., so need to add a reversed field to index .com.google.bot.12345
        Division visitsToDomain = division.addDivision("visits-by-domain");
        visitsToDomain.setHead("Visits to the Collection by type of domain");
        visitsToDomain.addPara("Not Yet Implemented! Need to change the data type of DNS to either reverse the field, or tokenize where dots are delimiter.");


        // 6 Visits to the collection by Geography
        addCountryViews(dso, division);



        // 6++IDEA: Map of the world hits


        //
        // Default DSpace Standard Stats Queries Below
        //

        /*
        // Total Visits
        addVisitsTotal(dso, division);

        // Total Visits per Month
        addVisitsMonthly(dso, division);

        // Top Items
        addTopItems(dso, division);
        division.addPara().addXref(contextPath + "/usage-report?owningType=" + dso.getType() + "&owningID=" + dso.getID() + "&reportType=" + Constants.ITEM, "CSV of All Items");

        // Top Bitstreams
        addTopBitstreams(dso, division);
        division.addPara().addXref(contextPath + "/usage-report?owningType=" + dso.getType() + "&owningID=" + dso.getID() + "&reportType=" + Constants.BITSTREAM, "CSV of All Bitstreams");

        // File Visits (for Items)
        addBitstreamViewsToItem(dso, division);



        if(dso instanceof Collection) {
            addGrowthItemsPerYear(dso, division);
        }
        */
    }

    /**
     * Provide a list of the top 10 viewed Items if possible
     *
     * @param dso
     * @param division
     */
    public void addTopItems(DSpaceObject dso, Division division) {
        if ((dso instanceof org.dspace.content.Collection) || (dso instanceof org.dspace.content.Community)) {
            try {

                StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataVisits(dso));

                statisticsTable.setTitle("Top Items");
                statisticsTable.setId("tab1");

                DatasetTimeGenerator timeAxis = new DatasetTimeGenerator();
                timeAxis.setIncludeTotal(true);
                timeAxis.setDateInterval("day", "-14", "+1");
                statisticsTable.addDatasetGenerator(timeAxis);

                DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
                dsoAxis.addDsoChild(org.dspace.core.Constants.ITEM, 100, false, -1);
                statisticsTable.addDatasetGenerator(dsoAxis);

                addDisplayTable(division, statisticsTable);

            } catch (Exception e) {
                log.error("Error occurred while creating top-items statistics for dso with ID: " + dso.getID()
                        + " and type " + dso.getType() + " and handle: " + dso.getHandle(), e);
            }

        }
    }

    /**
     * Provide a list of the top 10 viewed bitstreams if possible
     *
     * @param dso
     * @param division
     */
    public void addTopBitstreams(DSpaceObject dso, Division division) {
        if ((dso instanceof org.dspace.content.Collection) || (dso instanceof org.dspace.content.Community)) {
            try {

                StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataVisits(dso));

                statisticsTable.setTitle("Top Files");
                statisticsTable.setId("last-bit");

                DatasetTimeGenerator timeAxis = new DatasetTimeGenerator();
                timeAxis.setIncludeTotal(true);
                timeAxis.setDateInterval("day", "-21", "+1");
                statisticsTable.addDatasetGenerator(timeAxis);

                DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
                dsoAxis.addDsoChild(Constants.BITSTREAM, 100, false, -1);
                statisticsTable.addDatasetGenerator(dsoAxis);

                addDisplayTable(division, statisticsTable);

            } catch (Exception e) {
                log.error("Error occured while creating top-bits statistics for dso with ID: " + dso.getID()
                        + " and type " + dso.getType() + " and handle: " + dso.getHandle(), e);
            }

        }
    }

    public void addVisitsTotal(DSpaceObject dso, Division division) {
        try {
            StatisticsListing statListing = new StatisticsListing(
                    new StatisticsDataVisits(dso));

            statListing.setTitle(T_head_visits_total);
            statListing.setId("list1");

            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
            dsoAxis.addDsoChild(dso.getType(), 10, false, -1);
            statListing.addDatasetGenerator(dsoAxis);

            addDisplayListing(division, statListing);

        } catch (Exception e) {
            log.error("Error occured while creating statistics for dso with ID: " + dso.getID()
                    + " and type " + dso.getType() + " and handle: " + dso.getHandle(), e);
        }
    }

    public void addVisitsMonthly(DSpaceObject dso, Division division) {
        try {

            StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataVisits(dso));

            statisticsTable.setTitle(T_head_visits_month);
            statisticsTable.setId("tab1");

            DatasetTimeGenerator timeAxis = new DatasetTimeGenerator();
            timeAxis.setDateInterval("month", "-6", "+1");
            statisticsTable.addDatasetGenerator(timeAxis);

            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
            dsoAxis.addDsoChild(dso.getType(), 10, false, -1);
            statisticsTable.addDatasetGenerator(dsoAxis);

            addDisplayTable(division, statisticsTable);

        } catch (Exception e) {
            log.error("Error occured while creating statistics for dso with ID: " + dso.getID()
                    + " and type " + dso.getType() + " and handle: " + dso.getHandle(), e);
        }
    }

    public void addBitstreamViewsToItem(DSpaceObject dso, Division division) {
        if (dso instanceof org.dspace.content.Item) {
            //Make sure our item has at least one bitstream
            org.dspace.content.Item item = (org.dspace.content.Item) dso;
            try {
                if (item.hasUploadedFiles()) {
                    StatisticsListing statsList = new StatisticsListing(new StatisticsDataVisits(dso));

                    statsList.setTitle(T_head_visits_bitstream);
                    statsList.setId("list-bit");

                    DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
                    dsoAxis.addDsoChild(Constants.BITSTREAM, 10, false, -1);
                    statsList.addDatasetGenerator(dsoAxis);

                    addDisplayListing(division, statsList);
                }
            } catch (Exception e) {
                log.error("Error occured while creating statistics for dso with ID: " + dso.getID()
                        + " and type " + dso.getType() + " and handle: " + dso.getHandle(), e);
            }
        }

    }

    public String getTypeAsString(DSpaceObject dso) {
        switch (dso.getType()) {
            case 0:
                return "bitstream";
            case 2:
                return "item";
            case 3:
                return "collection";
            case 4:
                return "community";
            default:
                return "";

        }
    }

    /**
     * Only call this on a container object (collection or community).
     * @param dso
     * @param division
     */
    public void addItemsInContainer(DSpaceObject dso, Division division) {
        // Must be either collection or community.
        if(!(dso instanceof Collection || dso instanceof Community)) {
            return;
        }

        String querySpecifyContainer = "SELECT to_char(date_trunc('month', t1.ts), 'YYYY-MM') AS yearmo, count(*) as countitem " +
                "FROM ( SELECT to_timestamp(text_value, 'YYYY-MM-DD') AS ts FROM metadatavalue, item, " +
                getTypeAsString(dso) + "2item " +
                "WHERE metadata_field_id = 12 AND metadatavalue.item_id = item.item_id AND item.in_archive=true AND "+
                getTypeAsString(dso) + "2item.item_id = item.item_id AND "+
                getTypeAsString(dso) + "2item."+getTypeAsString(dso)+"_id = ? " +
                ") t1 GROUP BY date_trunc('month', t1.ts) order by yearmo asc";
        try {
            TableRowIterator tri = DatabaseManager.query(context, querySpecifyContainer, dso.getID());

            java.util.List<TableRow> tableRowList = tri.toList();
            
            displayAsGrid(division, tableRowList, "yearmo", "countitem", "Number of Items Added to the "+getTypeAsString(dso));
            //displayAsTableRows(division, tableRowList, "Number of Items in the Container");
            
            


        } catch (SQLException e) {
            log.error(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        } catch (WingException e) {
            log.error(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        }
    }
    
    public void displayAsGrid(Division division, java.util.List<TableRow> tableRowList, String dateColumn, String valueColumn, String header) throws WingException {
        String yearmoStart = tableRowList.get(0).getStringColumn(dateColumn);
        Integer yearStart = Integer.valueOf(yearmoStart.split("-")[0]);
        String yearmoLast = tableRowList.get(tableRowList.size()-1).getStringColumn(dateColumn);
        Integer yearLast = Integer.valueOf(yearmoLast.split("-")[0]);
        int numberOfYears = yearLast-yearStart;

        Table gridTable = division.addTable("itemsInContainer-grid", numberOfYears+1, 14);
        gridTable.setHead(header);
        Row gridHeader = gridTable.addRow(Row.ROLE_HEADER);
        gridHeader.addCell().addContent("Year");
        gridHeader.addCell().addContent("JAN");
        gridHeader.addCell().addContent("FEB");
        gridHeader.addCell().addContent("MAR");
        gridHeader.addCell().addContent("APR");
        gridHeader.addCell().addContent("MAY");
        gridHeader.addCell().addContent("JUN");
        gridHeader.addCell().addContent("JUL");
        gridHeader.addCell().addContent("AUG");
        gridHeader.addCell().addContent("SEP");
        gridHeader.addCell().addContent("OCT");
        gridHeader.addCell().addContent("NOV");
        gridHeader.addCell().addContent("DEC");
        gridHeader.addCell().addContent("Total YR");

        ArrayList<Row> yearlyRows = new ArrayList<Row>();
        for(int yearIndex = 0; yearIndex <= numberOfYears; yearIndex++) {
            Row gridRow = gridTable.addRow(Row.ROLE_DATA);
            gridRow.addCell(Row.ROLE_HEADER).addContent(yearStart+yearIndex);
            yearlyRows.add(gridRow);
        }

        Integer latestYear=0;
        Long yearCumulative=0L;
        Integer latestMonth=0;
        for(TableRow row: tableRowList) {
            String yearmo = row.getStringColumn(dateColumn);
            long monthlyHits = row.getLongColumn(valueColumn);
            String[] yearMonthSplit = yearmo.split("-");
            Integer currentYear = Integer.parseInt(yearMonthSplit[0]);
            Integer currentMonth = Integer.parseInt(yearMonthSplit[1]);

            Row thisYearRow = yearlyRows.get(currentYear - yearStart);
            Cell thisYearMonthCell = null;

            if (latestYear.equals(currentYear))
            {
                while(latestMonth < currentMonth)
                {
                    thisYearMonthCell = thisYearRow.addCell();
                    latestMonth++;
                }
            } else {
                //latestYear and latestMonth are invalid references, so, this year is fresh.
                // Try to write cumulative to final cell of previous year
                if(currentYear > yearStart) {
                    Row lastYearRow = yearlyRows.get(currentYear - yearStart -1);
                    Cell cumulativeCell = null;
                    while(latestMonth <= 12) {
                        cumulativeCell = lastYearRow.addCell();
                        latestMonth++;
                    }
                    cumulativeCell.addContent(yearCumulative+"");
                    yearCumulative=0L;
                }

                latestMonth = 0;
                while(latestMonth < currentMonth)
                {
                    thisYearMonthCell = thisYearRow.addCell();
                    latestMonth++;
                }

            }

            thisYearMonthCell.addContent(monthlyHits+"");
            yearCumulative += monthlyHits;

            latestYear = currentYear;
            latestMonth = currentMonth;
        }


        //After all is said and done, we need to fill in the final cumulative for the final year.
        // Try to write cumulative to final cell of previous year
        Row finalYearRow = yearlyRows.get(yearlyRows.size()-1);
        Cell cumulativeCell = null;
        while(latestMonth <= 12) {
            cumulativeCell = finalYearRow.addCell();
            latestMonth++;
        }
        cumulativeCell.addContent(yearCumulative+"");
        yearCumulative=0L;

    }
    
    public void displayAsTableRows(Division division, java.util.List<TableRow> tableRowList, String title) throws WingException {
        Table table = division.addTable("itemsInContainer", tableRowList.size()+1, 3);
        table.setHead(title);

        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent("Month");
        header.addCell().addContent("Added During Month");
        header.addCell().addContent("Total Cumulative");

        int cumulativeHits = 0;
        for(TableRow row : tableRowList) {
            Row htmlRow = table.addRow(Row.ROLE_DATA);

            String yearmo = row.getStringColumn("yearmo");
            htmlRow.addCell().addContent(yearmo);

            long monthlyHits = row.getLongColumn("countitem");
            htmlRow.addCell().addContent(""+monthlyHits);

            cumulativeHits += monthlyHits;
            htmlRow.addCell().addContent(""+cumulativeHits);
        }
    }

    public void addFilesInContainer(DSpaceObject dso, Division division) {
        // Must be either collection or community.
        if(!(dso instanceof Collection || dso instanceof Community)) {
            return;
        }

        String querySpecifyContainer = "SELECT to_char(date_trunc('month', t1.ts), 'YYYY-MM') AS yearmo, count(*) as countitem " +
                "FROM ( SELECT to_timestamp(text_value, 'YYYY-MM-DD') AS ts FROM metadatavalue, item, item2bundle, bundle, bundle2bitstream, " +
                getTypeAsString(dso) + "2item " +
                "WHERE metadata_field_id = 12 AND metadatavalue.item_id = item.item_id AND item.in_archive=true AND " +
                    "item2bundle.bundle_id = bundle.bundle_id AND item2bundle.item_id = item.item_id AND bundle.bundle_id = bundle2bitstream.bundle_id AND bundle.\"name\" = 'ORIGINAL' AND "+
                getTypeAsString(dso) + "2item.item_id = item.item_id AND "+
                getTypeAsString(dso) + "2item."+getTypeAsString(dso)+"_id = ? " +
                ") t1 GROUP BY date_trunc('month', t1.ts) order by yearmo asc";
        try {
            TableRowIterator tri = DatabaseManager.query(context, querySpecifyContainer, dso.getID());

            java.util.List<TableRow> tableRowList = tri.toList();

            displayAsGrid(division, tableRowList, "yearmo", "countitem", "Number of Files in the "+getTypeAsString(dso));
            //displayAsTableRows(division, tableRowList, "Number of Files in the "+getTypeAsString(dso));

        } catch (SQLException e) {
            log.error(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        } catch (WingException e) {
            log.error(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void addFileDownloadsInContainer(DSpaceObject dso, Division division) {
        // Must be either collection or community.
        if(!(dso instanceof Collection || dso instanceof Community)) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Integer humanMonthNumber = calendar.get(Calendar.MONTH)+1;

        // We have a hard-limit to our stats Data of Jan 1, 2008. So locally we can start 1/1/2008
        // 2011-08-01T00:00:00.000Z TO 2011-08-31T23:59:59.999Z
        String monthStart = "2008-01-01T00:00:00.000Z";
        String monthEnd =  calendar.get(Calendar.YEAR) + "-" + humanMonthNumber + "-" + calendar.getActualMaximum(Calendar.DAY_OF_MONTH)   + "T23:59:59.999Z";

        String query = "type:0 AND -isBot:true AND "
                + ((dso instanceof Collection) ? "owningColl:" : "owningComm:")
                + dso.getID();

        log.info("addFileDownloadsInContainer Query: "+query);
        log.info("addFileDownloadsInContainer monthEnd:" + monthEnd);

        try {
            ObjectCount[] objectCounts = SolrLogger.queryFacetDate(query, "", -1, "MONTH", monthStart, monthEnd, false);

            Table table = division.addTable("addFileDownloadsInContainer", objectCounts.length+1, 3);
            table.setHead("Number of File Downloads in the " + getTypeAsString(dso));

            Row headerRow = table.addRow(Row.ROLE_HEADER);
            headerRow.addCell().addContent("Month");
            headerRow.addCell().addContent("Monthly Downloads");
            headerRow.addCell().addContent("Cumulative Total");

            long totalCount = 0;
            for(ObjectCount facetEntry : objectCounts) {
                long count = facetEntry.getCount();
                totalCount += count;

                if(count == 0) {
                    continue;
                }

                Row dataRow = table.addRow(Row.ROLE_DATA);
                dataRow.addCell().addContent(facetEntry.getValue());
                dataRow.addCell().addContent(String.valueOf(facetEntry.getCount()));
                dataRow.addCell().addContent(String.valueOf(totalCount));
            }

        } catch (SolrServerException e) {
            log.error("addFileDownloadsInContainer Solr Query Failed: " + e.getMessage());
        } catch (WingException e) {
            log.error("addFileDownloadsInContainer WingException: " + e.getMessage());
        }
    }

    public void addUniqueVisitorsToContainer(DSpaceObject dso, Division division) {
        // Must be either collection or community.
        if(!(dso instanceof Collection || dso instanceof Community)) {
            return;
        }

        // We have a hard-limit to our stats Data of Jan 1, 2008. So locally we can start 1/1/2008
        // 2011-08-01T00:00:00.000Z TO 2011-08-31T23:59:59.999Z
        try {
            GregorianCalendar startCalendar = new GregorianCalendar();
            startCalendar.set(2008, Calendar.JANUARY, 1, 0, 0, 0);

            Calendar endCalendar = Calendar.getInstance();
            endCalendar.add(Calendar.MONTH, -1);

            GregorianCalendar copyStartCalendar = new GregorianCalendar();
            copyStartCalendar.set(2008, Calendar.JANUARY, 1, 0, 0, 0);

            int monthsGap = 0;
            while(copyStartCalendar.before(endCalendar)) {
                monthsGap++;
                copyStartCalendar.add(Calendar.MONTH, 1);
            }

            Table table = division.addTable("addUniqueVisitorsToContainer", monthsGap, 3);
            table.setHead("Number of Unique Visitors to the " + getTypeAsString(dso));

            Row headerRow = table.addRow(Row.ROLE_HEADER);
            headerRow.addCell().addContent("Month");
            headerRow.addCell().addContent("Monthly Unique Visitors");

            while(startCalendar.before(endCalendar)) {
                Integer humanMonthNumber = startCalendar.get(Calendar.MONTH)+1;

                String monthStart = startCalendar.get(Calendar.YEAR) + "-" + humanMonthNumber + "-" + startCalendar.getActualMinimum(Calendar.DAY_OF_MONTH)   + "T00:00:00.000Z";
                String monthEnd =  startCalendar.get(Calendar.YEAR) + "-" + humanMonthNumber + "-" + startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)   + "T23:59:59.999Z";

                String query = "type:0 AND -isBot:true AND time:[" + monthStart + " TO " + monthEnd + "]"
                    + ((dso instanceof Collection) ? "owningColl:" : "owningComm:")
                    + dso.getID();

                log.info("addUniqueVisitorsToContainer Query: "+query);
                log.info("addUniqueVisitorsToContainer monthEnd:" + monthEnd);



                ObjectCount[] objectCounts = SolrLogger.queryFacetField(query, "", "ip", -1, true, null);
                //ObjectCount[] objectCounts = SolrLogger.queryFacetDate(query, "", -1, "MONTH", monthStart, monthEnd, false);

                Row dataRow = table.addRow(Row.ROLE_DATA);

                if(objectCounts != null && objectCounts.length > 0) {
                    ObjectCount lastEntry = objectCounts[objectCounts.length-1];
                    dataRow.addCell().addContent(monthStart);
                    dataRow.addCell().addContent(String.valueOf(lastEntry.getCount()));
                } else {
                    dataRow.addCell().addContent(monthStart);
                    dataRow.addCell().addContent(0);
                }

                //Then Increment the lower month
                startCalendar.add(Calendar.MONTH, 1);
            }

        } catch (SolrServerException e) {
            log.error("addFileDownloadsInContainer Solr Query Failed: " + e.getMessage());
        } catch (WingException e) {
            log.error("addFileDownloadsInContainer WingException: " + e.getMessage());
        }

    }

    public void addGrowthItemsPerYear(DSpaceObject dso, Division division) {
        Collection collection = (Collection) dso;
        try {
            TableRowIterator yearCountIterator = collection.getItemsAvailablePerYear();
            java.util.List<TableRow> yearCountList = yearCountIterator.toList();
            Table table = division.addTable("YearCounts", yearCountList.size(), 2);
            table.setHead("Item Growth Per Year");
            Row headerRow = table.addRow(Row.ROLE_HEADER);
            headerRow.addCell().addContent("Year");
            headerRow.addCell().addContent("Count");
            //add cumulative

            for(TableRow row : yearCountList) {
                Row dataRow = table.addRow(Row.ROLE_DATA);

                Double year =  row.getDoubleColumn("year");
                dataRow.addCellContent(year.toString());

                String countString = row.getStringColumn("count");
                dataRow.addCellContent(countString);
            }

        } catch (SQLException e) {
            log.error(e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        } catch (WingException e) {
            log.error(e.getMessage());
        }


    }

    public void addCountryViews(DSpaceObject dso, Division division) {
        try {
            StatisticsListing statListing = new StatisticsListing(new StatisticsDataVisits(dso));

            statListing.setTitle(T_head_visits_countries);
            statListing.setId("list2");

//            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
//            dsoAxis.addDsoChild(dso.getType(), 10, false, -1);

            DatasetTypeGenerator typeAxis = new DatasetTypeGenerator();
            typeAxis.setType("countryCode");
            typeAxis.setMax(10);
            statListing.addDatasetGenerator(typeAxis);

            addDisplayListing(division, statListing);
        } catch (Exception e) {
            log.error("Error occurred while creating statistics for dso with ID: " + dso.getID()
                    + " and type " + dso.getType() + " and handle: " + dso.getHandle(), e);
        }

    }

    public void addCountryViewsMonthlyToContainer(DSpaceObject dso, Division division) {
        // Must be either collection or community.
        if(!(dso instanceof Collection || dso instanceof Community)) {
            return;
        }

        // We have a hard-limit to our stats Data of Jan 1, 2008. So locally we can start 1/1/2008
        // 2011-08-01T00:00:00.000Z TO 2011-08-31T23:59:59.999Z
        try {
            GregorianCalendar startCalendar = new GregorianCalendar();
            startCalendar.set(2008, Calendar.JANUARY, 1, 0, 0, 0);

            Calendar endCalendar = Calendar.getInstance();
            endCalendar.add(Calendar.MONTH, -1);

            GregorianCalendar copyStartCalendar = new GregorianCalendar();
            copyStartCalendar.set(2008, Calendar.JANUARY, 1, 0, 0, 0);

            int monthsGap = 0;
            while(copyStartCalendar.before(endCalendar)) {
                monthsGap++;
                copyStartCalendar.add(Calendar.MONTH, 1);
            }

            Table table = division.addTable("addGeorgraphyVisitorsToContainer", monthsGap, 3);
            table.setHead("Number of Geography Visitors to the " + getTypeAsString(dso));

            Row headerRow = table.addRow(Row.ROLE_HEADER);
            headerRow.addCell().addContent("Month");
            headerRow.addCell().addContent("Monthly Unique Visitors");

            while(startCalendar.before(endCalendar)) {
                Integer humanMonthNumber = startCalendar.get(Calendar.MONTH)+1;

                String monthStart = startCalendar.get(Calendar.YEAR) + "-" + humanMonthNumber + "-" + startCalendar.getActualMinimum(Calendar.DAY_OF_MONTH)   + "T00:00:00.000Z";
                String monthEnd =  startCalendar.get(Calendar.YEAR) + "-" + humanMonthNumber + "-" + startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)   + "T23:59:59.999Z";

                String query = "type:0 AND -isBot:true AND time:[" + monthStart + " TO " + monthEnd + "]"
                        + ((dso instanceof Collection) ? "owningColl:" : "owningComm:")
                        + dso.getID();

                log.info("addUniqueVisitorsToContainer Query: "+query);
                log.info("addUniqueVisitorsToContainer monthEnd:" + monthEnd);



                ObjectCount[] objectCounts = SolrLogger.queryFacetField(query, "", "ip", -1, true, null);
                //ObjectCount[] objectCounts = SolrLogger.queryFacetDate(query, "", -1, "MONTH", monthStart, monthEnd, false);

                Row dataRow = table.addRow(Row.ROLE_DATA);

                if(objectCounts != null && objectCounts.length > 0) {
                    ObjectCount lastEntry = objectCounts[objectCounts.length-1];
                    dataRow.addCell().addContent(monthStart);
                    dataRow.addCell().addContent(String.valueOf(lastEntry.getCount()));
                } else {
                    dataRow.addCell().addContent(monthStart);
                    dataRow.addCell().addContent(0);
                }

                //Then Increment the lower month
                startCalendar.add(Calendar.MONTH, 1);
            }

        } catch (SolrServerException e) {
            log.error("addFileDownloadsInContainer Solr Query Failed: " + e.getMessage());
        } catch (WingException e) {
            log.error("addFileDownloadsInContainer WingException: " + e.getMessage());
        }

    }


    public void addCityViews(DSpaceObject dso, Division division) {
        try {
            StatisticsListing statListing = new StatisticsListing(new StatisticsDataVisits(dso));

            statListing.setTitle(T_head_visits_cities);
            statListing.setId("list3");

//            DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
//            dsoAxis.addDsoChild(dso.getType(), 10, false, -1);

            DatasetTypeGenerator typeAxis = new DatasetTypeGenerator();
            typeAxis.setType("city");
            typeAxis.setMax(10);
            statListing.addDatasetGenerator(typeAxis);

            addDisplayListing(division, statListing);
        } catch (Exception e) {
            log.error("Error occurred while creating statistics for dso with ID: " + dso.getID()
                    + " and type " + dso.getType() + " and handle: " + dso.getHandle(), e);
        }
    }


    /**
     * Adds a table layout to the page
     *
     * @param mainDiv the div to add the table to
     * @param display
     * @throws SAXException
     * @throws WingException
     * @throws ParseException
     * @throws IOException
     * @throws SolrServerException
     * @throws SQLException
     */
    private void addDisplayTable(Division mainDiv, StatisticsTable display)
            throws SAXException, WingException, SQLException,
            SolrServerException, IOException, ParseException {

        String title = display.getTitle();

        Dataset dataset = display.getDataset();

        if (dataset == null) {
            /** activate dataset query */
            dataset = display.getDataset(context);
        }

        if (dataset != null) {

            String[][] matrix = dataset.getMatrixFormatted();

            /** Generate Table */
            Division wrapper = mainDiv.addDivision("tablewrapper");
            Table table = wrapper.addTable("list-table", 1, 1,
                    title == null ? "" : "tableWithTitle");
            if (title != null) {
                table.setHead(message(title));
            }

            /** Generate Header Row */
            Row headerRow = table.addRow();
            headerRow.addCell("spacer", Cell.ROLE_DATA, "labelcell");

            String[] cLabels = dataset.getColLabels().toArray(new String[0]);
            for (int row = 0; row < cLabels.length; row++) {
                Cell cell = headerRow.addCell(0 + "-" + row + "-h", Cell.ROLE_DATA, "labelcell");
                cell.addContent(cLabels[row]);
            }

            /** Generate Table Body */
            for (int row = 0; row < matrix.length; row++) {
                Row valListRow = table.addRow();

                /** Add Row Title */
                valListRow.addCell("" + row, Cell.ROLE_DATA, "labelcell").
                        addXref(dataset.getRowLabelsAttrs().get(row).get("url"), dataset.getRowLabels().get(row));

                /** Add Rest of Row */
                for (int col = 0; col < matrix[row].length; col++) {
                    Cell cell = valListRow.addCell(row + "-" + col, Cell.ROLE_DATA, "datacell");
                    cell.addContent(matrix[row][col]);
                }
            }
        }

    }

    private void addDisplayListing(Division mainDiv, StatisticsListing display) throws SAXException, WingException,
            SQLException, SolrServerException, IOException, ParseException {

        String title = display.getTitle();

        Dataset dataset = display.getDataset();

        if (dataset == null) {
            /** activate dataset query */
            dataset = display.getDataset(context);
        }

        if (dataset != null) {

            String[][] matrix = dataset.getMatrixFormatted();

            // String[] rLabels = dataset.getRowLabels().toArray(new String[0]);

            Table table = mainDiv.addTable("list-table", matrix.length, 2,
                    title == null ? "" : "tableWithTitle");
            if (title != null) {
                table.setHead(message(title));
            }

            Row headerRow = table.addRow();

            headerRow.addCell("", Cell.ROLE_DATA, "labelcell");

            headerRow.addCell("", Cell.ROLE_DATA, "labelcell").addContent(message(T_head_visits_views));

            /** Generate Table Body */
            for (int col = 0; col < matrix[0].length; col++) {
                Row valListRow = table.addRow();

                Cell catCell = valListRow.addCell(col + "1", Cell.ROLE_DATA, "labelcell");
                catCell.addContent(dataset.getColLabels().get(col));

                Cell valCell = valListRow.addCell(col + "2", Cell.ROLE_DATA, "datacell");
                valCell.addContent(matrix[0][col]);

            }

            if (!"".equals(display.getCss())) {
                List attrlist = mainDiv.addList("divattrs");
                attrlist.addItem("style", display.getCss());
            }

        }

    }
}
