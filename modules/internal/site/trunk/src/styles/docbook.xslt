<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml">

    <xsl:import href="http://quokka.ws/xmlcats/site-naut05-custom/0.1/site.xsl"/>
    <!--<xsl:import href="file:///C:/Data/Dev/Projects/quokka/xmlcat/site_naut05_custom/src/main/resources/catalog/styles/site.xslt"/>-->
    <xsl:import href="common.xslt"/>

    <xsl:variable name="layout" select="'2col-narrow-right'"/>

    <xsl:template name="layout-left">
        <!--        <xsl:copy-of select="//h:div[@class = 'chapter']"/> -->
        <xsl:apply-templates select="//h:div[@class = 'chapter']"/>
    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- This hack inserts a span inside anchors to force a closing </a> tag. Otherwise many browsers render incorrectly -->
    <!--
        <xsl:template match="//h:a">
            <a>
                <xsl:for-each select="@*">
                    <xsl:attribute name="{name()}">
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                </xsl:for-each>
                <span/>
                <xsl:apply-templates select="@*|node()"/>
            </a>
        </xsl:template>
    -->
    <xsl:template match="h:a">
        <a>
            <xsl:copy>
                <xsl:apply-templates select="@*"/>
            </xsl:copy>
            <span/>
            <xsl:apply-templates select="node()"/>
        </a>
    </xsl:template>
</xsl:stylesheet>