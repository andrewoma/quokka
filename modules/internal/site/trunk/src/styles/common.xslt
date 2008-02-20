<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml">

    <xsl:template name="layout-header">
        <img src="images/logo.gif" alt="Your logo goes here"/>
        <ul>
            <!--<li>-->
            <!--<a href="index.html">Back to Main page</a>-->
            <!--</li>-->
            <!--<li>-->
            <!--<a href="">About us</a>-->
            <!--</li>-->
            <!--<li>-->
            <!--<a href="">Streaming Video</a>-->
            <!--</li>-->
            <!--<li>-->
            <!--<a href="" class="last">RSS Feeds</a>-->
            <!--</li>-->
        </ul>
    </xsl:template>

    <xsl:template name="layout-tab-menu">
        <ul>
            <li>
                <a href="index.html" onfocus="blur()">
                    <span class="title ">About</span>
                    <!--<span class="desc">View the included layout</span>-->
                </a>
            </li>
            <li>
                <a href="site-map.html" class="forum" onfocus="blur()">
                    <span class="title ">Site Map</span>
                    <!--<span class="desc style3">View the included layout</span>-->
                </a>
            </li>
            <li>
                <a href="getting-started.html" class="forum" onfocus="blur()">
                    <span class="title ">Getting Started</span>
                    <!--<span class="desc style3">View the included layout</span>-->
                </a>
            </li>
            <li>
                <a href="downloads.html" onfocus="blur()">
                    <span class="title ">Downloads</span>
                    <!--<span class="desc">View the included layout</span>-->
                </a>
            </li>
            <li>
                <a href="extensions.html" onfocus="blur()">
                    <span class="title ">Extensions</span>
                    <!--<span class="desc">View the included layout</span>-->
                </a>
            </li>
            <li>
                <a href="user-guide.html" class="forum" onfocus="blur()">
                    <span class="title ">User Guide</span>
                    <!--<span class="desc style3">View the included layout</span>-->
                </a>
            </li>
            <li>
                <a href="dev-guide.html" onfocus="blur()">
                    <span class="title ">Developer Guide</span>
                    <!--<span class="desc">View the included layout</span>-->
                </a>
            </li>
        </ul>
    </xsl:template>

    <xsl:template name="layout-right">
        <h2>About Quokka</h2>
        <ul class="submenu1">
            <li>
                <a href="site-map.html">Site Map</a>
            </li>
            <li>
                <a href="index.html#news">News</a>
            </li>
            <li>
                <a href="roadmap.html">Roadmap</a>
            </li>
            <li>
                <a href="mailing-lists.html">Mailing Lists</a>
            </li>
            <li>
                <a href="issue-tracker.html">Issue Tracker</a>
            </li>
            <li>
                <a href="faq.html">FAQ</a>
            </li>
            <li>
                <a href="license.html">License</a>
            </li>
            <li>
                <a href="team.html">The Quokka Team</a>
            </li>
            <li>
                <a href="acknowledgements.html">Acknowledgments</a>
            </li>
        </ul>

        <h2>Downloads</h2>
        <ul class="submenu1">
            <li>
                <a href="downloads.html#stable">Releases</a>
            </li>
            <li>
                <a href="downloads.html#milestone">Milestones</a>
            </li>
        </ul>

        <h2>Guides</h2>
        <ul class="submenu1">
            <li>
                <a href="getting-started.html">Getting Started</a>
            </li>
            <li>
                <a href="user-guide.html">User Guide</a>
            </li>
            <li>
                <a href="dev-guide.html">Developer Guide</a>
            </li>
        </ul>

        <h2>Extensions</h2>
        <ul class="submenu1">
            <li>
                <a href="extensions.html#plugins">Plugins</a>
            </li>
            <li>
                <a href="extensions.html#dependency-sets">Dependency Sets</a>
            </li>
            <li>
                <a href="extensions.html#xml-catalogues">XML Catalogues</a>
            </li>
            <li>
                <a href="extensions.html#archetypes">Archetypes</a>
            </li>
        </ul>

        <h2>Global Repository</h2>
        <ul class="submenu1">
            <li>
                <a href="global-repository.html#accessing">Accessing</a>
            </li>
            <li>
                <a href="global-repository.html#standards">Standards</a>
            </li>
        </ul>

        <h2>Developers</h2>
        <ul class="submenu1">
            <li>
                <a href="building.html">Building from Source</a>
            </li>
            <!--<li>-->
                <!--<a href="">SPI Javadocs</a>-->
            <!--</li>-->
            <li>
                <a href="reports/index.html">Nightly Build Reports</a>
            </li>
        </ul>

    </xsl:template>

    <xsl:template name="layout-footer">
        <img src="images/logo.gif" alt="Your logo goes here"/>
        <ul>
            <li>
                <a href="http://ant.apache.org/" class="last">Apache ANT</a>
            </li>
        </ul>
    </xsl:template>

    <xsl:template name="html-head-before">
        <title>Quokka - Reproducible, modular builds</title>
        <meta name="author" content="Andrew O'Malley"/>
        <meta name="description" content="Site Description Here"/>
        <meta name="keywords" content="keywords, here"/>
    </xsl:template>

    <xsl:template name="html-head-after">
        <!--<link rel="stylesheet" type="text/css" href="css/docbook.css" media="screen, projection, tv "/>-->
    </xsl:template>

    <xsl:template name="before-body-end">
        <script type="text/javascript">
        var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
        document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
        </script>
        <script type="text/javascript">
        var pageTracker = _gat._getTracker("UA-3316895-1");
        pageTracker._initData();
        pageTracker._trackPageview();
        </script>
    </xsl:template>
</xsl:stylesheet>