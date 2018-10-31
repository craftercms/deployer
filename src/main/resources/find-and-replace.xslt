<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="pattern"/>
    <xsl:param name="replacement"/>
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="//*[text()[matches(., $pattern)]]">
        <xsl:copy>
            <xsl:value-of select="replace(., $pattern, $replacement)"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>