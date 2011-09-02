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
 * Exporting Collection's in CSV format
 * User: peterdietz
 * Date: 7/28/11
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class CollectionInfo extends HttpServlet
{
    protected static final Logger log = Logger.getLogger(CollectionInfo.class);

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; encoding='UTF-8'");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Disposition", "attachment; filename=collection-list.csv") ;
        CSVWriter writer = new CSVWriter(response.getWriter());

        String[] firstRow = new String[4];
        firstRow[0] = "Collection Name";
        firstRow[1] = "collectionID";
        firstRow[2] = "Handle";
        firstRow[3] = "collection_item_count";
        writer.writeNext(firstRow);

        TableRowIterator tri = null;
        try {
            tri = itemGrowth();
            while(tri.hasNext()) {
                TableRow row = tri.next();
                String[] rowString = new String[4];

                rowString[0] = row.getStringColumn("name");
                rowString[1] = String.valueOf(row.getIntColumn("collection_id"));
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

        String query = "SELECT collection.\"name\", collection.collection_id, handle.handle, collection_item_count.count " +
                "FROM public.handle, public.collection, public.collection_item_count "+
                "WHERE handle.resource_id = collection.collection_id AND collection_item_count.collection_id = collection.collection_id AND handle.resource_type_id = 3 " +
                "ORDER BY collection.collection_id ASC;";

        TableRowIterator tri = DatabaseManager.query(context, query);

        return tri;

    }
}