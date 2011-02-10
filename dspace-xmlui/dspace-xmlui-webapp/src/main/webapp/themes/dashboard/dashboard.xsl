<?xml version="1.0" encoding="UTF-8"?>

<!--
  template.xsl

  Version: $Revision: 3705 $
 
  Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
 
  Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
  Institute of Technology.  All rights reserved.
 
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:
 
  - Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.
 
  - Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.
 
  - Neither the name of the Hewlett-Packard Company nor the name of the
  Massachusetts Institute of Technology nor the names of their
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  DAMAGE.
-->

<!--
    TODO: Describe this XSL file    
    Author: Alexey Maslov
    
-->    

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:import href="../dri2xhtml.xsl"/>
    <xsl:output indent="yes"/>

    <xsl:template match="dri:div[@n='chart_div']" priority="2">
        <div id="chart_div" style="width: 700px; height: 240px;"/>
    </xsl:template>

    <xsl:template name="extraHead-top">
        <script type="text/javascript" src="https://www.google.com/jsapi"/>
        <script type="text/javascript">
            google.load('visualization', '1',
                {   'packages':['annotatedtimeline']});
                    google.setOnLoadCallback(drawChart);
                    function drawChart()
                    {
                        var data = new google.visualization.DataTable();
                        data.addColumn('date', 'Date');
                        data.addColumn('number', 'Items Added');
                        data.addColumn('number', 'Total Items');
                        data.addRows([
                            [new Date(2003, 07 ,1), 8, 8],[new Date(2003, 09 ,1), 3, 11],
                            [new Date(2004, 02 ,1), 31, 42],[new Date(2004, 03 ,1), 1, 43],[new Date(2004, 04 ,1), 1, 44],[new Date(2004, 07 ,1), 1, 45],[new Date(2004, 08 ,1), 11, 56],[new Date(2004, 09 ,1), 4, 60],[new Date(2004, 10 ,1), 2, 62],[new Date(2004, 11 ,1), 4, 66],[new Date(2004, 12 ,1), 32, 98],
                            [new Date(2005, 01 ,1), 2, 100],[new Date(2005, 02 ,1), 4, 104],[new Date(2005, 03 ,1), 1, 105],[new Date(2005, 04 ,1), 4, 109],[new Date(2005, 05 ,1), 54, 163],[new Date(2005, 06 ,1), 109, 272],[new Date(2005, 07 ,1), 31, 303],[new Date(2005, 08 ,1), 108, 411],[new Date(2005, 09 ,1), 3465, 3876],[new Date(2005, 10 ,1), 1197, 5073],[new Date(2005, 11 ,1), 14, 5087],[new Date(2005, 12 ,1), 37, 5124],
                            [new Date(2006, 01 ,1), 15, 5139],[new Date(2006, 02 ,1), 9, 5148],[new Date(2006, 03 ,1), 86, 5234],[new Date(2006, 04 ,1), 113, 5347],[new Date(2006, 05 ,1), 194, 5541],[new Date(2006, 06 ,1), 14814, 20355],[new Date(2006, 07 ,1), 1880, 22235],[new Date(2006, 08 ,1), 22, 22257],[new Date(2006, 09 ,1), 40, 22297],[new Date(2006, 10 ,1), 51, 22348],[new Date(2006, 11 ,1), 36, 22384],[new Date(2006, 12 ,1), 79, 22463],
                            [new Date(2007, 01 ,1), 166, 22629],[new Date(2007, 02 ,1), 110, 22739],[new Date(2007, 03 ,1), 80, 22819],[new Date(2007, 04 ,1), 125, 22944],[new Date(2007, 05 ,1), 382, 23326],[new Date(2007, 06 ,1), 3316, 26642],[new Date(2007, 07 ,1), 225, 26867],[new Date(2007, 08 ,1), 377, 27244],[new Date(2007, 09 ,1), 36, 27280],[new Date(2007, 10 ,1), 51, 27331],[new Date(2007, 11 ,1), 644, 27975],[new Date(2007, 12 ,1), 90, 28065],
                            [new Date(2008, 01 ,1), 1526, 29591],[new Date(2008, 02 ,1), 90, 29681],[new Date(2008, 03 ,1), 96, 29777],[new Date(2008, 04 ,1), 41, 29818],[new Date(2008, 05 ,1), 199, 30017],[new Date(2008, 06 ,1), 317, 30334],[new Date(2008, 07 ,1), 982, 31316],[new Date(2008, 08 ,1), 158, 31474],[new Date(2008, 09 ,1), 271, 31745],[new Date(2008, 10 ,1), 831, 32576],[new Date(2008, 11 ,1), 598, 33174],[new Date(2008, 12 ,1), 390, 33564],
                            [new Date(2009, 01 ,1), 26, 33590],[new Date(2009, 02 ,1), 102, 33692],[new Date(2009, 03 ,1), 181, 33873],[new Date(2009, 04 ,1), 149, 34022],[new Date(2009, 05 ,1), 271, 34293],[new Date(2009, 06 ,1), 486, 34779],[new Date(2009, 07 ,1), 870, 35649],[new Date(2009, 08 ,1), 432, 36081],[new Date(2009, 09 ,1), 62, 36143],[new Date(2009, 10 ,1), 4486, 40629],[new Date(2009, 11 ,1), 43, 40672],[new Date(2009, 12 ,1), 75, 40747],
                            [new Date(2010, 01 ,1), 79, 40826],[new Date(2010, 02 ,1), 274, 41100],[new Date(2010, 03 ,1), 115, 41215],[new Date(2010, 04 ,1), 152, 41367],[new Date(2010, 05 ,1), 170, 41537],[new Date(2010, 06 ,1), 373, 41910],[new Date(2010, 07 ,1), 669, 42579],[new Date(2010, 08 ,1), 184, 42763],[new Date(2010, 09 ,1), 108, 42871],[new Date(2010, 10 ,1), 291, 43162],[new Date(2010, 11 ,1), 85, 43247],[new Date(2010, 12 ,1), 74, 43321],
                            [new Date(2011, 01 ,1), 146, 43467]
                            ]);
                        var chart = new google.visualization.AnnotatedTimeLine(document.getElementById('chart_div'));
                        chart.draw(data, {displayAnnotations: true});
                    }
         </script>
         <!--<div id="chart_div" style="width: 700px; height: 240px;"/>-->


    </xsl:template>

    <!-- Just a plain old table cell -->
    <xsl:template match="dri:cell" priority="1">
        <td>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-cell
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                    <xsl:if test="@n='date'"> date</xsl:if>
                    <xsl:if test="@n='items_added'"> items_added</xsl:if>
                    <xsl:if test="@n='items_total'"> items_total</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:if test="@rows">
                <xsl:attribute name="rowspan">
                    <xsl:value-of select="@rows"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@cols">
                <xsl:attribute name="colspan">
                    <xsl:value-of select="@cols"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </td>
    </xsl:template>
    
</xsl:stylesheet>
