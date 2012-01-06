package org.dspace.app.statistics;



import au.com.bytecode.opencsv.CSVWriter;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;


import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Result;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
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
        CSVWriter writer = new CSVWriter(response.getWriter());

        TableRowIterator tri;

        //Allow for the content type to be passed in. 0 = BITSTREAM, 1 = ITEM, ...
        String typeParam = request.getParameter("type");
        String typeString = null;

        Integer type = null;
        if(typeParam == null) {
            type = 1;
        } else {
            type = Integer.parseInt(typeParam);
        }
        log.info("Got parameter type:"+type);

        switch(type) {
            case 0:
                typeString = "bitstream";
                tri = bitstreamGrowth();
                break;
            case 1:
            default:
                typeString = "item";
                tri = itemGrowth();
        }

        response.setHeader("Content-Disposition", "attachment; filename=growth-"+typeString+".csv");


        // feed in your array (or convert your data to an array)
        //String[] entries = "first#second#third".split("#");
        //writer.writeNext(entries);

        String[] firstRow = new String[3];
        firstRow[0] = "YYYY-MM";
        firstRow[1] = "count";
        firstRow[2] = "total"+typeString;
        writer.writeNext(firstRow);


        try {

            Integer total = 0;
            while(tri.hasNext()) {
                TableRow row = tri.next();
                //log.debug(row.toString());
                String date = row.getStringColumn("yearmo");
                Long count = row.getLongColumn("count");
                total += count.intValue();
                String[] rowString = new String[3];
                rowString[0] = date;
                rowString[1] = count.toString();
                rowString[2] = total.toString();

                writer.writeNext(rowString);
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        writer.close();


    }

    protected TableRowIterator itemGrowth() {
        String query = "SELECT to_char(date_trunc('month', t1.ts), 'YYYY-MM') AS yearmo, count(*) as count " +
                "FROM ( SELECT to_timestamp(text_value, 'YYYY-MM-DD') AS ts FROM metadatavalue, item " +
                "WHERE metadata_field_id = 12 AND metadatavalue.item_id = item.item_id AND item.in_archive=true	) t1 " +
                "GROUP BY date_trunc('month', t1.ts) order by yearmo asc;";

        TableRowIterator tri = null;

        try {
            tri = DatabaseManager.query(new Context(), query);
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return tri;
    }

    protected TableRowIterator bitstreamGrowth() {
        String query = "select to_char(date_trunc('month', t1.ts), 'YYYY-MM') as yearmo, count(*) as count from\n" +
                "(SELECT to_timestamp(text_value, 'YYYY-MM-DD') as ts \n" +
                "FROM public.metadatavalue, public.item, public.item2bundle, public.bundle, public.bitstream, public.bundle2bitstream\n" +
                "WHERE metadatavalue.item_id = item.item_id AND item.item_id = item2bundle.item_id AND bundle.bundle_id = item2bundle.bundle_id AND\n" +
                "  bundle.bundle_id = bundle2bitstream.bundle_id AND bundle2bitstream.bitstream_id = bitstream.bitstream_id AND\n" +
                "  metadatavalue.metadata_field_id = 12 AND bundle.\"name\" = 'ORIGINAL' AND item.in_archive = true\n" +
                ") t1 group by date_trunc('month', t1.ts) order by yearmo asc;";

        TableRowIterator tri = null;

        try {
            tri = DatabaseManager.query(new Context(), query);
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return tri;
    }
}
