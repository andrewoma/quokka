<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml">

    <xsl:import href="http://quokka.ws/xmlcats/site-naut05-custom/1.0-m01-ss/site.xsl"/>
    <xsl:import href="common.xslt"/>

    <xsl:variable name="layout" select="'2col-narrow-right'"/>

    <xsl:template name="layout-left">
        <!--        <xsl:copy-of select="//h:div[@class = 'chapter']"/> -->
        <xsl:copy-of select="//h:body/*"/>
    </xsl:template>

    <xsl:template name="html-head-standard">
        <meta http-equiv="content-type" content="application/xhtml+xml; charset=UTF-8"/>
        <meta name="robots" content="index, follow, noarchive"/>
        <meta name="googlebot" content="noarchive"/>

        <link rel="stylesheet" type="text/css" href="../css/layout.css" media="screen, projection, tv "/>
        <link rel="stylesheet" type="text/css" href="../css/html.css" media="screen, projection, tv "/>
    </xsl:template>


</xsl:stylesheet>