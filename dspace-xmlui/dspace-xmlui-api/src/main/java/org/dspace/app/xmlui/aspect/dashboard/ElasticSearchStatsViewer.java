package org.dspace.app.xmlui.aspect.dashboard;

import edu.osu.library.dspace.statistics.ElasticSearchLogger;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.statistics.ReportGenerator;
import org.dspace.app.xmlui.aspect.statistics.StatisticsTransformer;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.terms.TermsFacet;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: peterdietz
 * Date: 3/7/12
 * Time: 11:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class ElasticSearchStatsViewer extends AbstractDSpaceTransformer {
    private static Logger log = Logger.getLogger(ElasticSearchStatsViewer.class);

    private static SimpleDateFormat monthAndYearFormat = new SimpleDateFormat("MMMMM yyyy");

    public void addPageMeta(PageMeta pageMeta) throws WingException {
        pageMeta.addMetadata("title").addContent("Elastic Search Data Display");
    }

    public void addBody(Body body) throws WingException, SQLException {
        Client client = ElasticSearchLogger.createElasticClient(false);
        try {
            //Try to find our dspace object
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            Division division = body.addDivision("elastic-stats");
            division.setHead("Elastic Data Display");
            division.addPara(dso.getType() + " " + dso.getName());

            ReportGenerator reportGenerator = new ReportGenerator();
            reportGenerator.addReportGeneratorForm(division, ObjectModelHelper.getRequest(objectModel));
            Date dateStart = reportGenerator.getDateStart();
            Date dateEnd = reportGenerator.getDateEnd();

            // Show some non-usage-stats.
            // @TODO Refactor the non-usage stats out of the StatsTransformer
            StatisticsTransformer statisticsTransformerInstance = new StatisticsTransformer(dateStart, dateEnd);

            // 1 - Number of Items in The Container (Community/Collection) (monthly and cumulative for the year)
            if(dso instanceof org.dspace.content.Collection || dso instanceof Community) {
                statisticsTransformerInstance.addItemsInContainer(dso, division);
            }

            // 2 - Number of Files in The Container (monthly and cumulative)
            if(dso instanceof org.dspace.content.Collection || dso instanceof Community) {
                statisticsTransformerInstance.addFilesInContainer(dso, division);
            }




            String owningObjectType = "";
            switch (dso.getType()) {
                case Constants.COLLECTION:
                    owningObjectType = "owningColl";
                    break;
                case Constants.COMMUNITY:
                    owningObjectType = "owningComm";
                    break;
            }

            TermQueryBuilder termQuery = QueryBuilders.termQuery(owningObjectType, dso.getID());

            // Show Previous Whole Month
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);

            Integer humanMonthNumber = calendar.get(Calendar.MONTH)+1;
            String lowerBound = calendar.get(Calendar.YEAR) + "-" + humanMonthNumber + "-" + calendar.getActualMinimum(Calendar.DAY_OF_MONTH);
            String upperBound = calendar.get(Calendar.YEAR) + "-" + humanMonthNumber + "-" + calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

            SearchResponse resp = client.prepareSearch(ElasticSearchLogger.indexName)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setQuery(termQuery)
                    .setSize(0)
                    .addFacet(FacetBuilders.termsFacet("top_types").field("type"))
                    .addFacet(FacetBuilders.termsFacet("top_unique_ips").field("ip"))
                    .addFacet(FacetBuilders.termsFacet("top_countries").field("countryCode"))
                    .addFacet(FacetBuilders.termsFacet("top_bitstreams_lastmonth").field("id")
                            .facetFilter(FilterBuilders.termFilter("type", "bitstream"))
                            .facetFilter(FilterBuilders.rangeFilter("time").from(lowerBound).to(upperBound)))
                    .addFacet(FacetBuilders.termsFacet("top_bitstreams_alltime").field("id")
                            .facetFilter(FilterBuilders.termFilter("type", "bitstream")))
                    .addFacet(FacetBuilders.dateHistogramFacet("monthly_downloads").field("time").interval("month").facetFilter(FilterBuilders.termFilter("type", "bitstream")))
                    .execute()
                    .actionGet();


            //division.addPara(resp.toString());


            division.addPara("Querying bitstreams for elastic, Took " + resp.tookInMillis() + " ms to get " + resp.getHits().totalHits() + " hits.");

            // Number of File Downloads Per Month
            DateHistogramFacet monthlyDownloadsFacet = resp.getFacets().facet(DateHistogramFacet.class, "monthly_downloads");
            addDateHistogramToTable(monthlyDownloadsFacet, division, "MonthlyDownloads", "Number of Downloads (per month)");

            // Number of Unique Visitors per Month
            TermsFacet uniquesFacet = resp.getFacets().facet(TermsFacet.class, "top_unique_ips");
            addTermFacetToTable(uniquesFacet, division, "Uniques", "Unique Visitors (per year)");

            TermsFacet countryFacet = resp.getFacets().facet(TermsFacet.class, "top_countries");
            addTermFacetToTable(countryFacet, division, "Country", "Top Country Views (all time)");

            // Need to cast the facets to a TermsFacet so that we can get things like facet count. I think this is obscure.
            TermsFacet termsFacet = resp.getFacets().facet(TermsFacet.class, "top_types");
            addTermFacetToTable(termsFacet, division, "types", "Facetting of Hits to this owningObject by resource type");

            // Top Downloads to Owning Object
            TermsFacet bitstreamsFacet = resp.getFacets().facet(TermsFacet.class, "top_bitstreams_lastmonth");
            addTermFacetToTable(bitstreamsFacet, division, "Bitstream", "Top Downloads for " + monthAndYearFormat.format(calendar.getTime()));

            TermsFacet bitstreamsAllTimeFacet = resp.getFacets().facet(TermsFacet.class, "top_bitstreams_alltime");
            addTermFacetToTable(bitstreamsAllTimeFacet, division, "Bitstream", "Top Downloads (all time)");


        } finally {
            client.close();
        }
    }



    private void addTermFacetToTable(TermsFacet termsFacet, Division division, String termName, String tableHeader) throws WingException, SQLException {
        List<? extends TermsFacet.Entry> termsFacetEntries = termsFacet.getEntries();

        if(termsFacetEntries.size() == 0) {
            division.addPara("Empty result set for: "+termName);
            return;
        }

        Table facetTable = division.addTable("facet-"+termName, termsFacetEntries.size(), 10);
        facetTable.setHead(tableHeader);

        Row facetTableHeaderRow = facetTable.addRow(Row.ROLE_HEADER);
        if(termName.equalsIgnoreCase("bitstream")) {
            facetTableHeaderRow.addCellContent("Title");
            facetTableHeaderRow.addCellContent("Creator");
            facetTableHeaderRow.addCellContent("Publisher");
            facetTableHeaderRow.addCellContent("Date");
        } else {
            facetTableHeaderRow.addCell().addContent(termName);
        }

        facetTableHeaderRow.addCell().addContent("Count");

        for(TermsFacet.Entry facetEntry : termsFacetEntries) {
            Row row = facetTable.addRow();

            if(termName.equalsIgnoreCase("bitstream")) {
                Bitstream bitstream = Bitstream.find(context, Integer.parseInt(facetEntry.getTerm()));
                Item item = (Item) bitstream.getParentObject();
                row.addCell().addXref(contextPath + "/handle/" + item.getHandle(), item.getName());
                row.addCellContent(getFirstMetadataValue(item, "dc.creator"));
                row.addCellContent(getFirstMetadataValue(item, "dc.publisher"));
                row.addCellContent(getFirstMetadataValue(item, "dc.date.issued"));
            } else if(termName.equalsIgnoreCase("country")) {
                row.addCell().addContent(new Locale("en", facetEntry.getTerm()).getDisplayCountry());
            } else {
                row.addCell().addContent(facetEntry.getTerm());
            }
            row.addCell().addContent(facetEntry.getCount());
        }
    }

    private void addDateHistogramToTable(DateHistogramFacet monthlyDownloadsFacet, Division division, String termName, String termDescription) throws WingException {
        List<? extends DateHistogramFacet.Entry> monthlyFacetEntries = monthlyDownloadsFacet.getEntries();

        if(monthlyFacetEntries.size() == 0) {
            division.addPara("Empty result set for: "+termName);
            return;
        }

        Table monthlyTable = division.addTable(termName, monthlyFacetEntries.size(), 10);
        monthlyTable.setHead(termDescription);
        Row tableHeaderRow = monthlyTable.addRow(Row.ROLE_DATA);
        tableHeaderRow.addCell().addContent("Month/Date");
        tableHeaderRow.addCell().addContent("Count");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for(DateHistogramFacet.Entry histogramEntry : monthlyFacetEntries) {
            Row dataRow = monthlyTable.addRow();
            Date facetDate = new Date(histogramEntry.getTime());
            dataRow.addCell().addContent(dateFormat.format(facetDate));
            dataRow.addCell().addContent("" + histogramEntry.getCount());
        }
    }
    
    private String getFirstMetadataValue(Item item, String metadataKey) {
        DCValue[] dcValue = item.getMetadata(metadataKey);
        if(dcValue.length > 0) {
            return dcValue[0].value;
        } else {
            return "";
        }
    }
}
