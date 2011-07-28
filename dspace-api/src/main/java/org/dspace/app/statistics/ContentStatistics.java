/* ContentStatistics -- expose simple measures of repository size as a web document
 *
 * Copyright (c) 2002-2008, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.statistics;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.*;

/**
 * Expose some simple measures of the repository's size as an XML document via a
 * web service.
 *
 * <p><em>NOTE WELL:</em>  we go straight to the database for much of this
 * information.  This could break if there are significant changes in the
 * schema.  The object model doesn't provide these statistics, though.</p>
 * 
 * @author Mark H. Wood
 */
public class ContentStatistics extends HttpServlet
{
	private static final TimeZone utcZone = TimeZone.getTimeZone("UTC");

	protected static final Logger log
    	= Logger.getLogger(ContentStatistics.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        log.debug("Entering ContentStatistics.doGet");
        Context dsContext = null;
        TableRow row;

        // Response header
        resp.setContentType("text/xml; encoding='UTF-8'");
        resp.setStatus(HttpServletResponse.SC_OK);

        // Response body
        PrintWriter responseWriter = resp.getWriter();
        responseWriter.print("<?xml version='1.0' encoding='UTF-8'?>");

        responseWriter.print("<dspace-repository-statistics date='");
        log.debug("Ready to write date");
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        df.setTimeZone(utcZone);
        SimpleDateFormat tf = new SimpleDateFormat("HHmmss");
        tf.setTimeZone(utcZone);
        Date now = new Date();
        responseWriter.print(df.format(now));
        responseWriter.print('T');
        responseWriter.print(tf.format(now));
        responseWriter.print("Z'>");
        log.debug("Wrote the date");

        try
        {
            dsContext = new Context();
            
            row = DatabaseManager.querySingle(dsContext,
                    "SELECT count(community_id) AS communities FROM community;");
            if (null != row)
                responseWriter.printf(
                        " <statistic name='communities'>%d</statistic>",
                        row.getLongColumn("communities"));
            
            row = DatabaseManager.querySingle(dsContext,
                    "SELECT count(collection_id) AS collections FROM collection;");
            if (null != row)
                responseWriter.printf(
                        " <statistic name='collections'>%d</statistic>",
                        row.getLongColumn("collections"));
            
            row = DatabaseManager.querySingle(dsContext,
                    "SELECT count(item_id) AS items FROM item WHERE NOT withdrawn;");
            if (null != row)
                responseWriter.printf(
                        " <statistic name='items'>%d</statistic>",
                        row.getLongColumn("items"));

            log.debug("Counting, summing bitstreams");
            // Get # bitstreams, and MB
            row = DatabaseManager.querySingle(dsContext,
                    "SELECT count(bitstream_id) AS bitstreams," +
                            " sum(size_bytes)/1048576 AS totalMBytes" +
                            " FROM bitstream" +
                    		"  JOIN bundle2bitstream USING(bitstream_id)" +
                    		"  JOIN bundle USING(bundle_id)" +
                    		"  JOIN item2bundle USING(bundle_id)" +
                    		"  JOIN item USING(item_id)" +
                    		" WHERE NOT withdrawn" +
                    		"  AND NOT deleted" +
                    		"  AND bundle.name = 'ORIGINAL';");
            if (null != row)
            {
                log.debug("Writing count");
                responseWriter.printf(" <statistic name='bitstreams'>%d</statistic>",
                        row.getLongColumn("bitstreams"));
                log.debug("Writing total size");
                responseWriter.printf(" <statistic name='totalMBytes'>%d</statistic>",
                        row.getLongColumn("totalMBytes"));
                log.debug("Completed writing count, size");
            }
            
            log.debug("Counting, summing image bitstreams");
            row = DatabaseManager.querySingle(dsContext,
                    "SELECT count(bitstream_id) AS images," +
                    " sum(size_bytes)/1048576 AS imageMBytes" +
                    " FROM bitstream" +
                    " JOIN bitstreamformatregistry USING(bitstream_format_id)" +
                    " JOIN bundle2bitstream USING(bitstream_id)" +
                    " JOIN bundle USING(bundle_id)" +
                    " JOIN item2bundle USING(bundle_id)" +
                    " JOIN item USING(item_id)" +
                    " WHERE bundle.name = 'ORIGINAL'" +
                    "  AND mimetype LIKE 'image/%'" +
                    "  AND NOT deleted" +
                    "  AND NOT withdrawn;"
                    );
            if (null != row)
            {
                responseWriter.printf(" <statistic name='images'>%d</statistic>",
                        row.getLongColumn("images"));
                responseWriter.printf(" <statistic name='imageMBytes'>%d</statistic>",
                        row.getLongColumn("imageMBytes"));
            }
            
            dsContext.abort();	// nothing to commit
        }
        catch (SQLException e)
        {
            log.debug("caught SQLException");
            if (null != dsContext) dsContext.abort();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }

        responseWriter.print("</dspace-repository-statistics>");
        log.debug("Finished report");
    }

    /** HttpServlet implements Serializable for some strange reason */
    private static final long serialVersionUID = -98582768658080267L;
}
