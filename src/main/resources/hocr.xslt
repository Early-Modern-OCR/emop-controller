<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml" >
    <xsl:output method="text" indent="no"/>
    <xsl:variable name="display-linebreak" select="'&#10;'" />

    <!-- add a linebreak after every ocr_line -->
    <xsl:template match="xhtml:span[@class='ocr_line'] ">
      <xsl:call-template name="line-break"/>
    </xsl:template>

    <!-- dump stripped txt content for words and styled text -->
    <xsl:template match="xhtml:span[@class='ocrx_word']/text() | xhtml:strong/text() | xhtml:em/text() | xhtml:u/text() | xhtml:b/text() | xhtml:i/text() ">
        <xsl:variable name="a" select="replace(., '\s*$', ' ')"/>
        <xsl:variable name="b" select="replace($a, '^[\n]\s*', ' ')"/>
        <xsl:variable name="c" select="replace($b, '\n+', '')"/>
        <xsl:variable name="d" select="replace($c, '\s+', ' ')"/>
        <xsl:value-of select="$d"/>
    </xsl:template>

    <!-- skip the head and text of all tags except those above -->
    <xsl:template match="xhtml:head"/>
    <xsl:template match="text()"/>

    <xsl:template name="line-break">
        <xsl:apply-templates/>
        <xsl:value-of select="$display-linebreak"/>
    </xsl:template>
</xsl:stylesheet>
