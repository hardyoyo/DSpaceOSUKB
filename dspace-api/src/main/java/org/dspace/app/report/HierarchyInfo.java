package org.dspace.app.report;


import au.com.bytecode.opencsv.CSVWriter;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Context;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Exporting Community and hierarchy in CSV format
 */
public class HierarchyInfo extends HttpServlet
{
    protected static final Logger log = Logger.getLogger(HierarchyInfo.class);

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; encoding='UTF-8'");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Disposition", "attachment; filename=hierarchy-info.csv") ;
        CSVWriter writer = new CSVWriter(response.getWriter());

        String[] firstRow = initHierarchyLine();
        writer.writeNext(firstRow);

        try {
            Context context = new Context();
            Community[] topCommunities = Community.findAllTop(context);
            String[] rowString = new String[Hierarchy.values().length];

            for(Community community : topCommunities) {
                addCommunityHierarchy(community, 0, writer, rowString);
            }

        } catch (SQLException e) {
            log.error(e.getMessage());
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        writer.close();
    }

    protected void addCommunityHierarchy(Community community, Integer communityDepth, CSVWriter csvWriter, String[] rowString) throws SQLException {
        rowString[communityDepth] = community.getName();
        rowString = cleanHierarchyLine(rowString, communityDepth);

        //Get my collections and add to writer
        Collection[] collections = community.getCollections();
        for(Collection collection : collections) {
            writeCollection(collection, csvWriter, rowString);
        }

        //Get my subcommunities and recurse
        Community[] subCommunities = community.getSubcommunities();
        for(Community subCommunity : subCommunities) {
            addCommunityHierarchy(subCommunity, communityDepth+1, csvWriter, rowString);
        }



    }

    protected void writeCollection(Collection collection, CSVWriter csvWriter, String[] rowString) {
        rowString[Hierarchy.Collection.ordinal()] = collection.getName();
        try {
            rowString[Hierarchy.Items.ordinal()] = String.valueOf(collection.countItems());

            //Any other interesting reports we can get?
            /* @TODO Why is the stats stuff not available to DSpace-API
            String childrenOfCollectionQuery = "type:0 AND owningComm:[0 TO 9999999] AND -dns:msnbot-* AND -isBot:true AND time:[2011-08-01T00:00:00.000Z TO 2011-09-01T00:00:00.000Z]";

            ObjectCount[] objectCounts = new ObjectCount[0];
            try {
                objectCounts = SolrLogger.queryFacetField(query, "", "id", 50, true, null);

            } catch (SolrServerException e) {
                log.error("Top Downloads query failed.");
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            */



        } catch (SQLException e) {
            log.error("Error counting number of items for collection: "+collection.getName() + " -- " + collection.getHandle());
            rowString[Hierarchy.Items.ordinal()] = "Unknown";
        }
        csvWriter.writeNext(rowString);
    }

    /**
     * Create a String Array to write values for each field of our hierarchy report
     *  firstRow[0] = "Top Level community";
     *  ...
     *  firstRow[7] = "Number of Items";
     * @return
     */
    protected String[] initHierarchyLine(){
        String[] line = new String[Hierarchy.values().length];
        for(Hierarchy entry : Hierarchy.values()) {
            line[entry.ordinal()] = entry.toString();
        }
        return line;
    }

    /**
     * The hierarchy line can get dirty, so we need to clean it when we get to a new community.
     * @param hierarchyLine
     * @param depth
     * @return
     */
    protected String[] cleanHierarchyLine(String[] hierarchyLine, int depth) {
        Integer scrub = depth+1;

        // depth < scrub < collection.ordinal
        while((depth < scrub) && (scrub < Hierarchy.Collection.ordinal())) {
            hierarchyLine[scrub] = "";
            scrub++;
        }

        return hierarchyLine;
    }

    protected enum Hierarchy {
        TopCommunity,
        SubCom1,
        SubCom2,
        SubCom3,
        SubCom4,
        SubCom5,
        SubCom6,
        Collection,
        Items;
    }


}