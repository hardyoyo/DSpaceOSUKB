package org.dspace.app.report;


import au.com.bytecode.opencsv.CSVWriter;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Exporting Community's in CSV format
 * User: peterdietz
 * Date: 7/28/11
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class CommunityInfo extends HttpServlet
{
    protected static final Logger log = Logger.getLogger(CommunityInfo.class);

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; encoding='UTF-8'");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Disposition", "attachment; filename=community-list.csv") ;
        CSVWriter writer = new CSVWriter(response.getWriter());

        String[] firstRow = new String[4];
        firstRow[0] = "Community Name";
        firstRow[1] = "communityID";
        firstRow[2] = "Handle";
        firstRow[3] = "community_item_count";
        writer.writeNext(firstRow);

        TableRowIterator tri = null;
        try {
            tri = itemGrowth();
            while(tri.hasNext()) {
                TableRow row = tri.next();
                String[] rowString = new String[4];

                rowString[0] = row.getStringColumn("name");
                rowString[1] = String.valueOf(row.getIntColumn("community_id"));
                rowString[2] = row.getStringColumn("handle");
                rowString[3] = String.valueOf(row.getIntColumn("count"));

                writer.writeNext(rowString);
            }
        } catch (SQLException e) {
            log.error("Error fetching row" + e.getMessage());
        }

        writer.close();
    }

    protected TableRowIterator itemGrowth() throws SQLException {
        Context context = new Context();

        String query = "SELECT community.\"name\", community.community_id, handle.handle, community_item_count.count " +
                "FROM public.community, public.handle, public.community_item_count " +
                "WHERE community.community_id = community_item_count.community_id AND handle.resource_id = community.community_id AND handle.resource_type_id = 4 " +
                "order by community.community_id asc;";

        TableRowIterator tri = DatabaseManager.query(context, query);

        return tri;

    }
}