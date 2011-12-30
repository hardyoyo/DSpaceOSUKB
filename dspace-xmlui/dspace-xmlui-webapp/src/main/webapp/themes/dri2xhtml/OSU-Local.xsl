<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : OSU-local.xsl
    Created on : July 6, 2010, 2:56 PM
    Author     : stamper.10
    Description:
        Contains templates that only exist in our local customization, as
        opposed to overrides that are found within the other xsl in this folder.
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
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:cc="http://creativecommons.org/ns#"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc rdf cc">

    <xsl:output indent="yes"/>


    <!-- 2010-05-04 PMBMD - Adds required head CSS/js for osu header navbar -->
    <xsl:template name="buildHeadOSU">
        <!-- Skipping the reset <link rel="stylesheet" type="text/css" href="/xmlui/static/osu-navbar-media/css-optional/reset.css" />-->
        <link rel="stylesheet" type="text/css">
            <xsl:attribute name="href">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/osu-navbar-media/css/navbar.css</xsl:text>
            </xsl:attribute>
        </link>
        <!-- TODO not currently calling IE specific navbar css files. -->
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/osu-navbar-media/js/searchform.js</xsl:text>
            </xsl:attribute>
            <xsl:text>var x=0;</xsl:text>
        </script>

        <!-- Grab Google CDN jQuery. fall back to local if necessary. Also use same http / https as site -->
        <script type="text/javascript">
            var JsHost = (("https:" == document.location.protocol) ? "https://" : "http://");
            document.write(unescape("%3Cscript src='" + JsHost + "ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.min.js' type='text/javascript'%3E%3C/script%3E"));

            if(!window.jQuery) {
                document.write(unescape("%3Cscript src='<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>/static/js/jquery-1.4.4.min.js' type='text/javascript'%3E%3C/script%3E"));
            }
        </script>

        <!-- bds: text-field-prompt.js for global search box, uses jQuery -->
        <!-- see http://kyleschaeffer.com/best-practices/input-prompt-text/ -->
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/js/text-field-prompt.js</xsl:text>
            </xsl:attribute>
            <xsl:text> </xsl:text>
        </script>
        <link rel="icon" type="image/x-icon">
            <xsl:attribute name="href">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/osu-navbar-media/img/favicon.ico</xsl:text>
            </xsl:attribute>
        </link>
        <link rel="shortcut icon" type="image/x-icon">
            <xsl:attribute name="href">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/osu-navbar-media/img/favicon.ico</xsl:text>
            </xsl:attribute>
        </link>
        <!-- bds: jQuery breadcrumb trail shrinker, uses easing plugin -->
        <!-- http://www.comparenetworks.com/developers/jqueryplugins/jbreadcrumb.html -->
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/js/jquery.easing.1.3.js</xsl:text>
            </xsl:attribute>
            <xsl:text> </xsl:text>
        </script>
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/js/jquery.jBreadCrumb.1.1.js</xsl:text>
            </xsl:attribute>
            <xsl:text> </xsl:text>
        </script>
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/static/js/ba-linkify.min.js</xsl:text>
            </xsl:attribute>
            <xsl:text> </xsl:text>
        </script>
        <script type="text/javascript">
            $(document).ready(function() {
                $("#breadCrumb0").jBreadCrumb();

                /* Linkify All Item Metadata content */
                $('#aspect_artifactbrowser_ItemViewer_div_item-view table.ds-includeSet-table tr.ds-table-row td span').each(function(){
                    var that = $(this),
                    text = that.html(),
                    options = {callback: function( text, href ) {return href ? '<a href="' + href + '" title="' + text + '">' + text + '</a>' : text;}};
                    that.html(linkify(text, options ));
                });
            });
        </script>
    </xsl:template>

    <!-- 2010-05-04 PMBMD - Adds the html pieces of the osu navbar -->
    <xsl:template name="buildBodyOSU">
        <div id="osu-Navbar">
            <p>
                <a href="#ds-main" id="skip" class="osu-semantic">skip to main content</a>
            </p>
            <h2 class="osu-semantic">OSU Navigation Bar</h2>
            <div id="osu-NavbarBreadcrumb">
                <p id="osu">
                    <a title="The Ohio State University homepage" href="http://www.osu.edu/">The Ohio State University</a>
                </p>
                <p id="site-name">
                    <a title="University Libraries at The Ohio State University" href="http://library.osu.edu/">University Libraries</a>
                </p>
                <p id="site-name">
                    <a title="Knowledge Bank of University Libraries at The Ohio State University" href="http://kb.osu.edu/">Knowledge Bank</a>
                </p>
            </div>
            <div id="osu-NavbarLinks">
                <ul>
                    <li><a href="http://www.osu.edu/help.php" title="OSU Help">Help</a></li>
                    <li><a href="http://buckeyelink.osu.edu/" title="Buckeye Link">Buckeye Link</a></li>
                    <li><a href="http://www.osu.edu/map/" title="Campus map">Map</a></li>
                    <li><a href="http://www.osu.edu/findpeople.php" title="Find people at OSU">Find People</a></li>
                    <li><a href="https://webmail.osu.edu" title="OSU Webmail">Webmail</a></li>
                    <li id="searchbox">
                        <form action="http://www.osu.edu/search/index.php" method="post">
                            <div class="osu-semantic">
                            </div>
                            <fieldset>
                                <legend><span class="osu-semantic">Search</span></legend>
                                <label class="osu-semantic" for="search-field">Search Ohio State</label>
                                <input type="text" alt-attribute="Search Ohio State" value="" name="searchOSU" class="textfield headerSearchInput" id="search-field"/>
                                <button name="go" type="submit">Go</button>
                            </fieldset>
                        </form>
                    </li>
                </ul>
            </div>
        </div>
    </xsl:template>
    <!-- This is a named template to be an easy way to override to add something to the buildHead -->
    <xsl:template name="extraHead-top"></xsl:template>
    <xsl:template name="extraHead-bottom"></xsl:template>
    <xsl:template name="extraBody-end"></xsl:template>


    <!-- Peter's RSS code for options box -->
    <xsl:template name="addRSSLinks">
        <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
            <li><a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="."/>
                    </xsl:attribute>

                    <xsl:choose>
                        <xsl:when test="contains(., 'rss_1.0')"><img src="/dspace/static/icons/feed.png" alt="Icon for RSS 1.0 feed" />RSS 1.0</xsl:when>
                        <xsl:when test="contains(., 'rss_2.0')"><img src="/dspace/static/icons/feed.png" alt="Icon for RSS 2.0 feed" />RSS 2.0</xsl:when>
                        <xsl:when test="contains(., 'atom_1.0')"><img src="/dspace/static/icons/feed.png" alt="Icon for Atom feed" />Atom</xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="@qualifier"/>
                        </xsl:otherwise>
                    </xsl:choose>
            </a></li>
        </xsl:for-each>
    </xsl:template>


    <!-- bds: this adds "Please use this URL to cite.." to "Show full item" link section
    copied from structural.xsl, with a more specific match pattern added -->
    <xsl:template match="dri:p[@rend='item-view-toggle item-view-toggle-top']">
        <div class="notice">
            <p>
                Please use this identifier to cite or link to this item:
                <!-- bds: first get the METS URL where we can find the item metadata -->
                <xsl:variable name="metsURL">
                    <xsl:text>cocoon:/</xsl:text>
                    <xsl:value-of select="/dri:document/dri:body/dri:div/dri:referenceSet/dri:reference[@type='DSpace Item']/@url"/>
                    <xsl:text>?sections=dmdSec</xsl:text>
                </xsl:variable>
                <!-- bds: now grab the specific piece of metadata from that METS document -->
                <xsl:variable name="handleURI" select="document($metsURL)/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='identifier'][@qualifier='uri']"/>
                <a href="{$handleURI}">
                    <xsl:value-of select="$handleURI"/>
                </a>
            </p>
        </div>
        <p>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-paragraph</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </p>
    </xsl:template>




<!-- bds: needed this replace-string to do some character escaping -->
<!-- from http://www.dpawson.co.uk/xsl/sect2/replace.html#d8763e61 -->
  <xsl:template name="replace-string">
    <xsl:param name="text"/>
    <xsl:param name="replace"/>
    <xsl:param name="with"/>
    <xsl:choose>
      <xsl:when test="contains($text,$replace)">
        <xsl:value-of select="substring-before($text,$replace)"/>
        <xsl:value-of select="$with"/>
        <xsl:call-template name="replace-string">
          <xsl:with-param name="text" select="substring-after($text,$replace)"/>
          <xsl:with-param name="replace" select="$replace"/>
          <xsl:with-param name="with" select="$with"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


<!-- bds: remove search box from community/collection pages -->
<xsl:template match="dri:div[@id='aspect.artifactbrowser.CollectionSearch.div.collection-search'] | dri:div[@id='aspect.artifactbrowser.CommunitySearch.div.community-search']">
</xsl:template>

<!-- bds: recent submissions box -->
<!-- adding the 'View all' link -->
<xsl:template match="dri:div[@n='collection-recent-submission'] | dri:div[@n='community-recent-submission']">
    <xsl:apply-templates select="./dri:head"/>
    <ul class="ds-artifact-list">
        <xsl:apply-templates select="./dri:referenceSet" mode="summaryList"/>
    </ul>
    <div id="more-link">
        <a>
            <xsl:attribute name="href">
                <xsl:value-of select="/dri:document/dri:body/dri:div/dri:div/dri:div/dri:list/dri:item[3]/dri:xref/@target"/>
            </xsl:attribute>
            <xsl:text>View all submissions ></xsl:text>
        </a>
    </div>
</xsl:template>


</xsl:stylesheet>
