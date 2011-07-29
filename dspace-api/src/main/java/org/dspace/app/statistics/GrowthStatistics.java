package org.dspace.app.statistics;



import au.com.bytecode.opencsv.CSVWriter;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;


import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Exporting Tchera's stats in CSV format.
 * User: peterdietz
 * Date: 7/28/11
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class GrowthStatistics extends HttpServlet
{
    protected static final Logger log = Logger.getLogger(GrowthStatistics.class);

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; encoding='UTF-8'");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Disposition", "attachment; filename=growth-sample.csv") ;
        CSVWriter writer = new CSVWriter(response.getWriter());

        // feed in your array (or convert your data to an array)
        String[] entries = "first#second#third".split("#");
        writer.writeNext(entries);



        writer.close();


    }

    protected void itemGrowth() throws SQLException {
        Context context = new Context();

        String query = "SELECT to_char(date_trunc('month', t1.ts), 'YYYY-MM') AS yearmo, count(*) as countitem " +
                "FROM ( SELECT to_timestamp(text_value, 'YYYY-MM-DD') AS ts FROM metadatavalue, item " +
                "WHERE metadata_field_id = 12 AND metadatavalue.item_id = item.item_id AND item.in_archive=true	) t1 " +
                "GROUP BY date_trunc('month', t1.ts) order by yearmo asc;";
        TableRowIterator tri = DatabaseManager.query(context, query);
        //DatabaseManager.query().toList().toArray()
    }
}
