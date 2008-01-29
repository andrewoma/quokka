<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml">

    <xsl:import href="http://quokka.ws/xmlcats/site-naut05-custom/0.1/site.xsl"/>
    <!--<xsl:import href="file:///C:/Data/Dev/Projects/quokka/xmlcat/site_naut05_custom/src/main/resources/catalog/styles/site.xslt"/>-->
    <xsl:import href="common.xslt"/>

    <xsl:variable name="layout" select="'2col-narrow-right'"/>

    <xsl:template name="layout-left">
        <!--        <xsl:copy-of select="//h:div[@class = 'chapter']"/> -->
        <div id="main-col">
            <xsl:copy-of select="//h:body/*"/>
        </div>
    </xsl:template>

</xsl:stylesheet>