<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:akn="http://docs.oasis-open.org/legaldocml/ns/akn/3.0/WD17"
                xmlns:ris="http://example.com/0.1/"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://docs.oasis-open.org/legaldocml/ns/akn/3.0/WD17 https://docs.oasis-open.org/legaldocml/akn-core/v1.0/csprd02/part2-specs/schemas/akomantoso30.xsd">
    <xsl:output indent="yes" method="xml" encoding="utf-8"/>
    <xsl:strip-space elements="*"/>

    <!--***************************************************************************************-->
    <!--First pass:                                                                            -->
    <!--Move unsupported attributes to children elements and delete some unsupported elements. -->
    <!--***************************************************************************************-->
    <xsl:variable name="firstPassResult">
        <xsl:apply-templates select="/" mode="firstPass"/>
    </xsl:variable>

    <xsl:template match="/" mode="firstPass">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" mode="firstPass"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*| node()" mode="firstPass">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" mode="firstPass"/>
        </xsl:copy>
    </xsl:template>

    <!--Deleted elements-->

    <!--noindex is for internal spellchecking. It doesn't belong in the ldml-->
    <xsl:template match="noindex" mode="firstPass">
        <xsl:apply-templates mode="firstPass"/>
    </xsl:template>

    <!--Case Law team handover: hlj is internal only-->
    <xsl:template match="hlj" mode="firstPass">
        <xsl:text> </xsl:text>
        <xsl:apply-templates mode="firstPass"/>
        <xsl:text> </xsl:text>
    </xsl:template>

    <!--Case Law team handover: nohlj is internal only-->
    <xsl:template match="nohlj" mode="firstPass">
        <xsl:apply-templates mode="firstPass"/>
    </xsl:template>

    <!--Case Law team handover: decide how to transform these internal Juris image markers into html links -->
    <xsl:template match="hfj" mode="firstPass">
        <xsl:apply-templates mode="firstPass"/>
    </xsl:template>

    <!--Case Law team handover: Validate that removing this tag is okay. Example caselas is WBRE410020500
    The current web view https://eur-lex.europa.eu/legal-content/DE/TXT/?uri=CELEX%3A62013CA0192
    doesn't have the <small> tag-->
    <xsl:template match="small" mode="firstPass">
        <xsl:apply-templates mode="firstPass"/>
    </xsl:template>

    <!--Case Law team handover: Empty blockquote seem to be used as a newline.
    This needs a confirmation if this is okay.-->
    <xsl:template match="blockquote[not(*|@*)]" mode="firstPass">
        <xsl:copy>
            <br/>
        </xsl:copy>
    </xsl:template>

    <!--***************************************************************************************-->
    <!--Second pass:                                                                           -->
    <!--The main part of the transformation.                                                   -->
    <!--***************************************************************************************-->
    <xsl:template match="/" >
        <xsl:apply-templates select="$firstPassResult" mode="secondPass"/>
    </xsl:template>

    <xsl:template match="/" mode="secondPass">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <!--Unsupported attributes are renamed to the ris namespace. These attributes are verified to not be
        supported by checking the xsd since the website documentation has no easy way to check this-->
    <xsl:template match="@id|@scope|@span|@valign|@headers|@nr|@frame|colgroup/col/@width|span/@lang|
            table/@cellpadding|table/@cellspacing|a/@name|a/@shape|@cite|@datetime|blockquote/@dir">
        <xsl:attribute name="ris:{name()}">
            <xsl:value-of select="." />
        </xsl:attribute>
    </xsl:template>

    <!--Some locations don't support some html elements. This template allows defining these relations only once-->
    <xsl:template match="*[self::akn:arguments or self::akn:background or self::akn:decision or
                      self::akn:embeddedStructure or self::akn:introduction or self::akn:motivation or
                      self::akn:subFlow or self::blockquote or self::th or self::div]/
                      *[self::strong or self::b or self::em or self::i or self::br or self::ins or self::u or
                      self::del or self::span or self::fussnote or self::jurimg or self::sup or self::sub or
                      self::border-number-link]">

        <xsl:variable name="tagName">
            <xsl:choose>
                <xsl:when test="name() ='strong'">
                    <xsl:value-of select="'b'"/>
                </xsl:when>
                <xsl:when test="name() ='em'">
                    <xsl:value-of select="'i'"/>
                </xsl:when>
                <xsl:when test="name() ='ins'">
                    <xsl:value-of select="'u'"/>
                </xsl:when>
                <xsl:when test="name() ='fussnote'">
                    <xsl:value-of select="'span'"/>
                </xsl:when>
                <xsl:when test="name() ='jurimg'">
                    <xsl:value-of select="'img'"/>
                </xsl:when>
                <xsl:when test="name() ='border-number-link'">
                    <xsl:value-of select="'borderNumberLink'"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="name()"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <!-- Create a block with a dynamic tag name -->
        <akn:block name="{$tagName}Wrapper">
            <xsl:choose>
                <xsl:when test="name() ='jurimg'">
                    <xsl:call-template name="jurimgTemplate"/>
                </xsl:when>
                <xsl:when test="name() ='border-number-link'">
                    <xsl:call-template name="borderNumberLinkTemplate"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:element name="akn:{$tagName}">
                        <xsl:apply-templates select="@* | node()"/>
                    </xsl:element>
                </xsl:otherwise>
            </xsl:choose>
        </akn:block>

    </xsl:template>

    <!--Unfortunately the common parent select can't be shared with text.
    Unsupported text locations are wrapped in a <akn:p>-->
    <xsl:template match="akn:header/text()|akn:motivation/text()|akn:introduction/text()|akn:background/text()
    |akn:decision/text()|akn:embeddedStructure/text()|akn:subFlow/text()|div/text()">
        <akn:p alternativeTo="textWrapper">
            <xsl:value-of select="."/>
        </akn:p>
    </xsl:template>


    <!--blockquote and comment are converted into akn:block/akn:embeddedStructure because they
        can contain top level structural elements (randnummer)-->
    <xsl:template match="blockquote|comment">
        <akn:block name="{name()}">
            <xsl:apply-templates select="@*" />
            <akn:embeddedStructure>
                <xsl:apply-templates/>
            </akn:embeddedStructure>
        </akn:block>
    </xsl:template>

    <!--<akn:div> does not allow some children types so they need to be wrapped-->
    <xsl:template match="div/div">
        <akn:block name="divInDivWrapper">
            <akn:embeddedStructure>
                <xsl:element name="akn:div">
                    <xsl:apply-templates select="@* | node()"/>
                </xsl:element>
            </akn:embeddedStructure>
        </akn:block>
    </xsl:template>

    <!--border-number related parts-->
    <xsl:template match="border-number">
        <xsl:call-template name="borderNumberTemplate"/>
    </xsl:template>

    <xsl:template match="*[self::p or self::strong or self::b or self::em or self::i or self::ins or self::u
            or self::sup or self::li or self::h1 or self::h2 or self::h3 or self::h4 or self::h5 or self::h6
            or self::del or self::span]/border-number">
        <akn:embeddedStructure refersTo="borderNumberWrapper">
            <xsl:call-template name="borderNumberTemplate"/>
        </akn:embeddedStructure>
    </xsl:template>

    <xsl:template match="div/border-number">
        <akn:block name="borderNumberInDivWrapper">
            <akn:embeddedStructure refersTo="borderNumberWrapper">
                <xsl:call-template name="borderNumberTemplate"/>
            </akn:embeddedStructure>
        </akn:block>
    </xsl:template>

    <xsl:template name="borderNumberTemplate">
        <akn:hcontainer  name="randnummer">
            <xsl:apply-templates select="@* | node()"/>
        </akn:hcontainer>
    </xsl:template>

    <xsl:template match="border-number/number">
        <akn:num>
            <xsl:apply-templates select="@* | node()"/>
        </akn:num>
    </xsl:template>

    <xsl:template match="border-number/content">
        <akn:content>
            <xsl:apply-templates select="@* | node()"/>
        </akn:content>
    </xsl:template>

    <xsl:template match="border-number-link">
        <xsl:call-template name="borderNumberLinkTemplate"/>
    </xsl:template>

    <xsl:template name="borderNumberLinkTemplate">
        <!--Any other existing attributes for border-number-link will be lost -->
        <akn:a class="border-number-link" href="{concat('#border-number-link-',@nr)}">
            <xsl:apply-templates/>
        </akn:a>
    </xsl:template>

    <!--Case Law team handover: akn doesn't support <pre>. Maybe something works better than adding foreign?
    KVRE403221301 is an example of an empty pre tag-->
    <xsl:template match="pre">
        <akn:foreign>
            <ris:pre>
                <xsl:apply-templates select="@* | node()"/>
            </ris:pre>
        </akn:foreign>
    </xsl:template>

    <!--table are wrapped in akn:foreign because they deviate too much from akn:table-->
    <xsl:template match="table">
        <akn:foreign>
            <ris:table>
                <xsl:apply-templates select="@* | node()"/>
            </ris:table>
        </akn:foreign>
    </xsl:template>

    <xsl:template match="colgroup|col|tbody|th|tr|td">
        <xsl:element name="ris:{name()}">
            <xsl:apply-templates select="@* | node()"/>
        </xsl:element>
    </xsl:template>

    <!--Conversion of jurimg elements:
    *The element is converted to an akn:img element
    *path attribute is converted to src attribute and is mandatory
    *name attribute is converted to title attribute
    *linktext attribute dropped when equal to alt attribute and causes a validation error when not -->
    <xsl:template match="jurimg">
        <xsl:call-template name="jurimgTemplate"/>
    </xsl:template>
    <xsl:template name="jurimgTemplate">
        <xsl:if test="not(@path)">
            <akn:invalidImgMissingSrc/>
        </xsl:if>
        <xsl:if test="@linktext and not(@linktext=@alt)">
            <akn:invalidLinkTextAttribute/>
        </xsl:if>
        <akn:img src="{@path}">
            <xsl:apply-templates select="@*[not(name()='path')][not(name()='name')][not(name()='linktext')] | node()"/>
            <xsl:if test="@name">
                <xsl:attribute name="title">
                    <xsl:value-of select="@name" />
                </xsl:attribute>
            </xsl:if>
        </akn:img>
    </xsl:template>

    <!--Case Law team handover: akn doesn't support <cite>. Make sure <b> is appropriate.
    JURE110023066 is an example-->
    <xsl:template match="cite">
        <akn:b>
            <xsl:apply-templates select="@* | node()"/>
        </akn:b>
    </xsl:template>

    <!--Simple renames to akn namespace. Note: some of these (like br) only apply when they don't match
    the more specific rule wrapping them when they occur in an unsupported location-->
    <xsl:template match="a|sub|sup|span|p|div|i|b|u|del|br|ol|ul|li|img">
        <xsl:element name="akn:{name()}">
            <xsl:apply-templates select="@* | node()"/>
        </xsl:element>
    </xsl:template>

    <!--Miscellaneous elements    -->
    <!--akn:a elements require the href attribute, but it's actually not required in html -->
    <xsl:template match="a[not(@href)]">
        <xsl:element name="akn:a">
            <xsl:attribute name="href">
                <xsl:value-of select="hrefUndefined" />
            </xsl:attribute>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="strong">
        <akn:b>
            <xsl:apply-templates select="@* | node()"/>
        </akn:b>
    </xsl:template>

    <xsl:template match="em">
        <akn:i>
            <xsl:apply-templates select="@* | node()"/>
        </akn:i>
    </xsl:template>

    <xsl:template match="ins">
        <akn:u>
            <xsl:apply-templates select="@* | node()"/>
        </akn:u>
    </xsl:template>

    <xsl:template match="fussnote">
        <akn:span alternativeTo="fussnote">
            <xsl:apply-templates select="@* | node()"/>
        </akn:span>
    </xsl:template>

    <xsl:template match="h1|h2|h3|h4|h5|h6|hr">
        <akn:p alternativeTo="{name()}TagReplacement">
            <xsl:apply-templates select="@* | node()"/>
        </akn:p>
    </xsl:template>

    <!--Identity transformation for known cases-->
    <xsl:template match="akn:akomaNtoso|akn:judgment|akn:judgmentBody|akn:meta|akn:header|akn:introduction
    |akn:identification|akn:classification|akn:proprietary|akn:block|akn:keyword|akn:background
    |akn:decision|akn:motivation|akn:opinion|akn:embeddedStructure|akn:subFlow
    |akn:FRBRWork|akn:FRBRExpression|akn:FRBRManifestation
    |akn:FRBRthis|akn:FRBRuri|akn:FRBRalias|akn:FRBRdate|akn:FRBRauthor|akn:FRBRcountry|akn:FRBRlanguage
    |ris:meta|ris:fileNumbers|ris:fileNumber|ris:documentType|ris:courtLocation|ris:courtType|ris:legalEffect
    |ris:fieldOfLaws|ris:fieldOfLaw|ris:judicialBody|ris:publicationStatus|ris:error|ris:documentationOffice
    |ris:documentNumber|ris:previousDecisions|ris:previousDecision|ris:ensuingDecisions|ris:ensuingDecision
    |ris:procedures|ris:procedure|ris:decisionNames|ris:decisionName|ris:deviatingFileNumbers
    |ris:deviatingFileNumber|ris:deviatingDocumentNumbers|ris:deviatingDocumentNumber|ris:legalForces|ris:legalForce
    |ris:yearOfDispute|ris:deviatingCourts|ris:deviatingCourt|ris:deviatingDates|ris:deviatingDate
    |ris:deviatingEclis|ris:deviatingEcli
    |@xsi:schemaLocation|@name|@source|@dictionary|@showAs|@value|@date|@href|@language|@class|@colspan|@rowspan
    |@style|@alt|@height|@width|@src|@title|@xml:space|@border">

        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="text()">
        <xsl:copy>
            <xsl:apply-templates select="."/>
        </xsl:copy>
    </xsl:template>

    <!--Fail fast strategy for unknown cases (add something that will fail xsd for sure)-->
    <xsl:template match="@*">
        <xsl:attribute name="akn:unknownUseCaseDiscoveredFor{name()}"/>
    </xsl:template>

    <xsl:template match="*">
        <akn:unknownUseCaseDiscovered name="{name()}"/>
    </xsl:template>

</xsl:stylesheet>
