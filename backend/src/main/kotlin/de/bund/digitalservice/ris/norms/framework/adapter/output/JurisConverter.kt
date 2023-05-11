package de.bund.digitalservice.ris.norms.framework.adapter.output

import de.bund.digitalservice.ris.norms.application.port.output.GenerateNormFileOutputPort
import de.bund.digitalservice.ris.norms.application.port.output.ParseJurisXmlOutputPort
import de.bund.digitalservice.ris.norms.domain.entity.Article
import de.bund.digitalservice.ris.norms.domain.entity.FileReference
import de.bund.digitalservice.ris.norms.domain.entity.MetadataSection
import de.bund.digitalservice.ris.norms.domain.entity.Metadatum
import de.bund.digitalservice.ris.norms.domain.entity.Norm
import de.bund.digitalservice.ris.norms.domain.entity.Paragraph
import de.bund.digitalservice.ris.norms.domain.entity.getHashFromContent
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.AGE_OF_MAJORITY_INDICATION
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.ANNOUNCEMENT_GAZETTE
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.ANNOUNCEMENT_MEDIUM
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.DATE
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.DECIDING_BODY
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.DEFINITION
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.DIVERGENT_DOCUMENT_NUMBER
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.EDITION
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.ENTITY
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.KEYWORD
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.LEAD_JURISDICTION
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.LEAD_UNIT
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.NORM_CATEGORY
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.NUMBER
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.PAGE
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.PARTICIPATION_INSTITUTION
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.PARTICIPATION_TYPE
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.RANGE_START
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.REFERENCE_NUMBER
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.RESOLUTION_MAJORITY
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.RIS_ABBREVIATION_INTERNATIONAL_LAW
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.SUBJECT_FNA
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.SUBJECT_GESTA
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.TEMPLATE_NAME
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.TYPE_NAME
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.UNOFFICIAL_ABBREVIATION
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.UNOFFICIAL_LONG_TITLE
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.UNOFFICIAL_REFERENCE
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.UNOFFICIAL_SHORT_TITLE
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.VALIDITY_RULE
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.YEAR
import de.bund.digitalservice.ris.norms.domain.value.NormCategory
import de.bund.digitalservice.ris.norms.domain.value.UndefinedDate
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.encodeLocalDate
import de.bund.digitalservice.ris.norms.juris.converter.extractor.extractData
import de.bund.digitalservice.ris.norms.juris.converter.generator.generateZip
import de.bund.digitalservice.ris.norms.juris.converter.model.DigitalAnnouncement
import de.bund.digitalservice.ris.norms.juris.converter.model.NormProvider
import de.bund.digitalservice.ris.norms.juris.converter.model.PrintAnnouncement
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName as Section
import de.bund.digitalservice.ris.norms.juris.converter.model.Article as ArticleData
import de.bund.digitalservice.ris.norms.juris.converter.model.Norm as NormData
import de.bund.digitalservice.ris.norms.juris.converter.model.Paragraph as ParagraphData

@Component
class JurisConverter() : ParseJurisXmlOutputPort, GenerateNormFileOutputPort {
    override fun parseJurisXml(query: ParseJurisXmlOutputPort.Query): Mono<Norm> {
        val data = extractData(ByteBuffer.wrap(query.zipFile))
        val norm = mapDataToDomain(query.newGuid, data)
        norm.files = listOf(FileReference(query.filename, getHashFromContent(query.zipFile)))
        return Mono.just(norm)
    }

    override fun generateNormFile(command: GenerateNormFileOutputPort.Command): Mono<ByteArray> {
        return Mono.just(generateZip(mapDomainToData(command.norm), ByteBuffer.wrap(command.previousFile)))
    }
}

fun mapDomainToData(norm: Norm): NormData {
    val keywords = extractStringValues(norm, MetadataSectionName.NORM, KEYWORD)
    val divergentNumber = extractFirstStringValue(norm, MetadataSectionName.NORM, DIVERGENT_DOCUMENT_NUMBER)
    val citationDates = extractLocalDateValues(norm, MetadataSectionName.CITATION_DATE, DATE)
    val citationYears = extractStringValues(norm, MetadataSectionName.CITATION_DATE, YEAR)

    val normProviders: List<NormProvider> = norm.metadataSections.filter { section -> section.name == MetadataSectionName.NORM_PROVIDER }.map {
        val entity = it.metadata.find { metadatum -> metadatum.type == ENTITY }?.let { found -> found.value as String }
        val decidingBody = it.metadata.find { metadatum -> metadatum.type == DECIDING_BODY }?.let { found -> found.value as String }
        val isResolutionMajority = it.metadata.find { metadatum -> metadatum.type == RESOLUTION_MAJORITY }?.let { found -> found.value as Boolean }
        NormProvider(entity, decidingBody, isResolutionMajority)
    }

    return NormData(
        announcementDate = encodeLocalDate(norm.announcementDate),
        citationDateList = citationDates.filterNotNull() + citationYears,
        documentCategory = norm.documentCategory,
        divergentDocumentNumber = divergentNumber,
        entryIntoForceDate = encodeLocalDate(norm.entryIntoForceDate),
        expirationDate = encodeLocalDate(norm.expirationDate),
        frameKeywordList = keywords,
        officialAbbreviation = norm.officialAbbreviation,
        officialLongTitle = norm.officialLongTitle,
        officialShortTitle = norm.officialShortTitle,
        normProviderList = normProviders,
        printAnnouncementList = extractPrintAnnouncementList(norm),
        digitalAnnouncementList = extractDigitalAnnouncementList(norm),
        risAbbreviation = norm.risAbbreviation,
    )
}

fun mapDataToDomain(guid: UUID, data: NormData): Norm {
    val divergentDocumentNumber = data.divergentDocumentNumber?.let { listOf(Metadatum(data.divergentDocumentNumber, DIVERGENT_DOCUMENT_NUMBER, 1)) } ?: listOf()
    val frameKeywords = createMetadataForType(data.frameKeywordList, KEYWORD)
    val risAbbreviationInternationalLaw = createMetadataForType(data.risAbbreviationInternationalLawList, RIS_ABBREVIATION_INTERNATIONAL_LAW)
    val unofficialLongTitle = createMetadataForType(data.unofficialLongTitleList, UNOFFICIAL_LONG_TITLE)
    val unofficialShortTitle = createMetadataForType(data.unofficialShortTitleList, UNOFFICIAL_SHORT_TITLE)
    val unofficialAbbreviation = createMetadataForType(data.unofficialAbbreviationList, UNOFFICIAL_ABBREVIATION)
    val unofficialReference = createMetadataForType(data.unofficialReferenceList, UNOFFICIAL_REFERENCE)
    val referenceNumber = createMetadataForType(data.referenceNumberList, REFERENCE_NUMBER)
    val definition = createMetadataForType(data.definitionList, DEFINITION)
    val ageOfMajorityIndication = createMetadataForType(data.ageOfMajorityIndicationList, AGE_OF_MAJORITY_INDICATION)
    val validityRule = createMetadataForType(data.validityRuleList, VALIDITY_RULE)

    val participationType = createMetadataForType(data.participationList.map { it.type.toString() }, PARTICIPATION_TYPE)
    val participationInstitution = createMetadataForType(data.participationList.map { it.institution.toString() }, PARTICIPATION_INSTITUTION)

    val leadJurisdiction = createMetadataForType(data.leadList.map { it.jurisdiction.toString() }, LEAD_JURISDICTION)
    val leadUnit = createMetadataForType(data.leadList.map { it.unit.toString() }, LEAD_UNIT)

    val subjectFna = createMetadataForType(data.subjectAreaList.filter { it.fna != null }.map { it.fna.toString() }, SUBJECT_FNA)
    val subjectGesta = createMetadataForType(data.subjectAreaList.filter { it.gesta != null }.map { it.gesta.toString() }, SUBJECT_GESTA)

    val printAnnouncementGazette = createMetadataForType(data.printAnnouncementList.map { it.gazette.toString() }, ANNOUNCEMENT_GAZETTE)
    val printAnnouncementPage = createMetadataForType(data.printAnnouncementList.map { it.page.toString() }, PAGE)
    val printAnnouncementYear = createMetadataForType(data.printAnnouncementList.map { it.year.toString() }, YEAR)
    val digitalAnnouncementYear = createMetadataForType(data.digitalAnnouncementList.map { it.year.toString() }, YEAR)
    val digitalAnnouncementNumber = createMetadataForType(data.digitalAnnouncementList.map { it.number.toString() }, EDITION)
    val digitalAnnouncementMedium = createMetadataForType(data.digitalAnnouncementList.map { it.medium.toString() }, ANNOUNCEMENT_MEDIUM)

    val documentTypeName = createMetadataForType(data.documentTypeList.map { it.name.toString() }, TYPE_NAME)
    val documentNormCategory = createMetadataForType(data.documentTypeList.mapNotNull { parseNormCategory(it.category) }, NORM_CATEGORY)
    val documentTemplateName = createMetadataForType(data.documentTypeList.map { it.templateName.toString() }, TEMPLATE_NAME)

    val citationDateSections = data.citationDateList.mapIndexed { index, value ->
        if (value.length == 4 && value.toIntOrNull() != null) {
            MetadataSection(MetadataSectionName.CITATION_DATE, listOf(Metadatum(value, YEAR, 1)), index)
        } else if (value.length > 4 && parseDateString(value) != null) {
            MetadataSection(MetadataSectionName.CITATION_DATE, listOf(Metadatum(parseDateString(value), DATE, 1)), index)
        } else {
            null
        }
    }

    val referenceSections = createSectionsFromMetadata(Section.PRINT_ANNOUNCEMENT, printAnnouncementGazette + printAnnouncementYear + printAnnouncementPage) +
        createSectionsFromMetadata(Section.DIGITAL_ANNOUNCEMENT, digitalAnnouncementNumber + digitalAnnouncementMedium + digitalAnnouncementYear)

    val ageIndicationSections = data.ageIndicationStartList.mapIndexed { index, value ->
        MetadataSection(MetadataSectionName.AGE_INDICATION, listOf(Metadatum(value, RANGE_START, 1)), index)
    }
    val sections = listOf(
        MetadataSection(Section.NORM, frameKeywords + divergentDocumentNumber + risAbbreviationInternationalLaw + unofficialAbbreviation + unofficialShortTitle + unofficialLongTitle + unofficialReference + referenceNumber + definition + ageOfMajorityIndication + validityRule),
    ) + createSectionsWithoutGrouping(Section.SUBJECT_AREA, subjectFna + subjectGesta) +
        createSectionsFromMetadata(Section.LEAD, leadJurisdiction + leadUnit) +
        createSectionsFromMetadata(Section.PARTICIPATION, participationInstitution + participationType) +
        createSectionsFromMetadata(Section.DOCUMENT_TYPE, documentTypeName + documentNormCategory + documentTemplateName) +
        referenceSections.mapIndexed { index, section -> MetadataSection(MetadataSectionName.OFFICIAL_REFERENCE, listOf(), index, listOf(section)) } +
        citationDateSections + ageIndicationSections +
        addProviderSections(data.normProviderList)

    return Norm(
        guid = guid,
        articles = mapArticlesToDomain(data.articles),
        metadataSections = sections.filterNotNull(),
        officialLongTitle = data.officialLongTitle ?: "",
        risAbbreviation = data.risAbbreviation,
        documentCategory = data.documentCategory,
        officialShortTitle = data.officialShortTitle,
        officialAbbreviation = data.officialAbbreviation,
        entryIntoForceDate = parseDateString(data.entryIntoForceDate),
        entryIntoForceDateState = parseDateStateString(data.entryIntoForceDateState ?: ""),
        principleEntryIntoForceDate = parseDateString(data.principleEntryIntoForceDate),
        principleEntryIntoForceDateState =
        parseDateStateString(data.principleEntryIntoForceDateState),
        divergentEntryIntoForceDate = parseDateString(data.divergentEntryIntoForceDate),
        divergentEntryIntoForceDateState =
        parseDateStateString(data.divergentEntryIntoForceDateState),
        entryIntoForceNormCategory = data.entryIntoForceNormCategory,
        expirationDate = parseDateString(data.expirationDate),
        expirationDateState = parseDateStateString(data.expirationDateState),
        principleExpirationDate = parseDateString(data.principleExpirationDate),
        principleExpirationDateState = parseDateStateString(data.principleExpirationDateState),
        divergentExpirationDate = parseDateString(data.divergentExpirationDate),
        divergentExpirationDateState = parseDateStateString(data.divergentExpirationDateState),
        expirationNormCategory = data.expirationNormCategory,
        announcementDate = parseDateString(data.announcementDate),
        statusNote = data.statusNote,
        statusDescription = data.statusDescription,
        statusDate = parseDateString(data.statusDate),
        statusReference = data.statusReference,
        repealNote = data.repealNote,
        repealArticle = data.repealArticle,
        repealDate = parseDateString(data.repealDate),
        repealReferences = data.repealReferences,
        reissueNote = data.reissueNote,
        reissueArticle = data.reissueArticle,
        reissueDate = parseDateString(data.reissueDate),
        reissueReference = data.reissueReference,
        otherStatusNote = data.otherStatusNote,
        documentStatusWorkNote = data.documentStatusWorkNote,
        documentStatusDescription = data.documentStatusDescription,
        documentStatusDate = parseDateString(data.documentStatusDate),
        applicationScopeArea = data.applicationScopeArea,
        applicationScopeStartDate = parseDateString(data.applicationScopeStartDate),
        applicationScopeEndDate = parseDateString(data.applicationScopeEndDate),
        categorizedReference = data.categorizedReference,
        otherFootnote = data.otherFootnote,
        celexNumber = data.celexNumber,
        text = data.text,
    )
}

fun addProviderSections(normProviders: List<NormProvider>): List<MetadataSection> {
    return normProviders.mapIndexed { index, normProvider ->
        val metadata = mutableListOf<Metadatum<*>>()
        if (normProvider.entity !== null) {
            metadata.add(Metadatum(normProvider.entity, ENTITY, 1))
        }
        if (normProvider.decidingBody !== null) {
            metadata.add(Metadatum(normProvider.decidingBody, DECIDING_BODY, 1))
        }
        if (normProvider.isResolutionMajority !== null) {
            metadata.add(Metadatum(normProvider.isResolutionMajority, RESOLUTION_MAJORITY, 1))
        }
        if (metadata.size > 0) MetadataSection(Section.NORM_PROVIDER, metadata, index + 1) else null
    }.filterNotNull()
}

private fun createMetadataForType(data: List<*>, type: MetadatumType): List<Metadatum<*>> = data
    .mapIndexed { index, value -> Metadatum(value, type, index + 1) }

fun mapArticlesToDomain(articles: List<ArticleData>): List<Article> {
    return articles.map { article ->
        Article(
            guid = UUID.randomUUID(),
            title = article.title,
            marker = article.marker,
            paragraphs = mapParagraphsToDomain(article.paragraphs),
        )
    }
}

fun mapParagraphsToDomain(paragraphs: List<ParagraphData>): List<Paragraph> {
    return paragraphs.map { paragraph ->
        Paragraph(
            guid = UUID.randomUUID(),
            marker = paragraph.marker,
            text = paragraph.text,
        )
    }
}

fun parseDateString(value: String?): LocalDate? = value?.let { try { LocalDate.parse(value) } catch (e: DateTimeParseException) { null } }

fun parseNormCategory(value: String?): NormCategory? = value?.let { try { NormCategory.valueOf(value) } catch (e: IllegalArgumentException) { null } }

fun parseDateStateString(value: String?): UndefinedDate? =
    if (value.isNullOrEmpty()) null else UndefinedDate.valueOf(value)

fun createSectionsFromMetadata(sectionName: MetadataSectionName, metadata: List<Metadatum<*>>) = metadata
    .groupBy { it.order }
    .mapValues {
        MetadataSection(
            sectionName,
            it.value.map { metadatum -> Metadatum(metadatum.value, metadatum.type, 1) },
            it.key,
        )
    }.values

private fun createSectionsWithoutGrouping(sectionName: MetadataSectionName, metadata: List<Metadatum<*>>) = metadata
    .mapIndexed { index, metadatum ->
        MetadataSection(sectionName, listOf(Metadatum(metadatum.value, metadatum.type, 1)), index)
    }

private fun extractStringValues(norm: Norm, sectionName: MetadataSectionName, metadatumType: MetadatumType): List<String> {
    return norm.metadataSections
        .filter { it.name == sectionName }
        .flatMap { it.metadata }
        .filter { it.type == metadatumType }
        .sortedBy { it.order }
        .map { it.value.toString() }
}

private fun extractLocalDateValues(norm: Norm, sectionName: MetadataSectionName, metadatumType: MetadatumType): List<String?> {
    return norm.metadataSections
        .filter { it.name == sectionName }
        .flatMap { it.metadata }
        .filter { it.type == metadatumType }
        .sortedBy { it.order }
        .map { encodeLocalDate(it.value as LocalDate) }
}

private fun extractFirstStringValue(norm: Norm, sectionName: MetadataSectionName, metadatumType: MetadatumType): String {
    return norm.metadataSections
        .filter { it.name == sectionName }
        .flatMap { it.metadata }
        .filter { it.type == metadatumType }
        .minByOrNull { it.order }?.value.toString()
}

private fun extractPrintAnnouncementList(norm: Norm): List<PrintAnnouncement> = norm
    .metadataSections
    .filter { it.name == MetadataSectionName.PRINT_ANNOUNCEMENT }
    .map { section ->
        PrintAnnouncement(
            section.metadata.find { it.type == YEAR }?.value.toString(),
            section.metadata.find { it.type == PAGE }?.value.toString(),
            section.metadata.find { it.type == ANNOUNCEMENT_GAZETTE }?.value.toString(),
        )
    }

private fun extractDigitalAnnouncementList(norm: Norm): List<DigitalAnnouncement> = norm
    .metadataSections
    .filter { it.name == MetadataSectionName.DIGITAL_ANNOUNCEMENT }
    .map { section ->
        DigitalAnnouncement(
            section.metadata.find { it.type == YEAR }?.value.toString(),
            section.metadata.find { it.type == NUMBER }?.value.toString(),
            section.metadata.find { it.type == ANNOUNCEMENT_MEDIUM }?.value.toString(),
        )
    }
