package de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.controller

import com.fasterxml.jackson.annotation.JsonProperty
import de.bund.digitalservice.ris.norms.application.port.input.EditNormFrameUseCase
import de.bund.digitalservice.ris.norms.domain.entity.MetadataSection
import de.bund.digitalservice.ris.norms.domain.entity.Metadatum
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType
import de.bund.digitalservice.ris.norms.domain.value.UndefinedDate
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.ApiConfiguration
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.decodeGuid
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.decodeLocalDate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping(ApiConfiguration.API_NORMS_PATH)
class EditNormFrameController(private val editNormFrameService: EditNormFrameUseCase) {

    @PutMapping(path = ["/{guid}"])
    fun editNormFrame(
        @PathVariable guid: String,
        @RequestBody request: NormFramePropertiesRequestSchema,
    ): Mono<ResponseEntity<Void>> {
        val properties = request.toUseCaseData()
        val command = EditNormFrameUseCase.Command(decodeGuid(guid), properties)

        return editNormFrameService
            .editNormFrame(command)
            .map { ResponseEntity.noContent().build<Void>() }
            .onErrorReturn(ResponseEntity.internalServerError().build())
    }

    class NormFramePropertiesRequestSchema {
        lateinit var officialLongTitle: String
        lateinit var metadataSections: List<MetadataSectionRequestSchema>
        var risAbbreviation: String? = null
        var documentNumber: String? = null
        var documentCategory: String? = null

        var documentTypeName: String? = null
        var documentNormCategory: String? = null
        var documentTemplateName: String? = null

        var providerEntity: String? = null
        var providerDecidingBody: String? = null
        var providerIsResolutionMajority: Boolean? = null

        var officialShortTitle: String? = null
        var officialAbbreviation: String? = null

        var entryIntoForceDate: String? = null
        var entryIntoForceDateState: UndefinedDate? = null
        var principleEntryIntoForceDate: String? = null
        var principleEntryIntoForceDateState: UndefinedDate? = null
        var divergentEntryIntoForceDate: String? = null
        var divergentEntryIntoForceDateState: UndefinedDate? = null
        var entryIntoForceNormCategory: String? = null

        var expirationDate: String? = null
        var expirationDateState: UndefinedDate? = null

        @get:JsonProperty("isExpirationDateTemp")
        var isExpirationDateTemp: Boolean? = null
        var principleExpirationDate: String? = null
        var principleExpirationDateState: UndefinedDate? = null
        var divergentExpirationDate: String? = null
        var divergentExpirationDateState: UndefinedDate? = null
        var expirationNormCategory: String? = null

        var announcementDate: String? = null
        var publicationDate: String? = null

        var citationDate: String? = null
        var citationYear: String? = null

        var printAnnouncementGazette: String? = null
        var printAnnouncementYear: String? = null
        var printAnnouncementNumber: String? = null
        var printAnnouncementPage: String? = null
        var printAnnouncementInfo: String? = null
        var printAnnouncementExplanations: String? = null
        var digitalAnnouncementMedium: String? = null
        var digitalAnnouncementDate: String? = null
        var digitalAnnouncementEdition: String? = null
        var digitalAnnouncementYear: String? = null
        var digitalAnnouncementPage: String? = null
        var digitalAnnouncementArea: String? = null
        var digitalAnnouncementAreaNumber: String? = null
        var digitalAnnouncementInfo: String? = null
        var digitalAnnouncementExplanations: String? = null
        var euAnnouncementGazette: String? = null
        var euAnnouncementYear: String? = null
        var euAnnouncementSeries: String? = null
        var euAnnouncementNumber: String? = null
        var euAnnouncementPage: String? = null
        var euAnnouncementInfo: String? = null
        var euAnnouncementExplanations: String? = null
        var otherOfficialAnnouncement: String? = null

        var completeCitation: String? = null

        var statusNote: String? = null
        var statusDescription: String? = null
        var statusDate: String? = null
        var statusReference: String? = null
        var repealNote: String? = null
        var repealArticle: String? = null
        var repealDate: String? = null
        var repealReferences: String? = null
        var reissueNote: String? = null
        var reissueArticle: String? = null
        var reissueDate: String? = null
        var reissueReference: String? = null
        var otherStatusNote: String? = null

        var documentStatusWorkNote: String? = null
        var documentStatusDescription: String? = null
        var documentStatusDate: String? = null
        var documentStatusReference: String? = null
        var documentStatusEntryIntoForceDate: String? = null
        var documentStatusProof: String? = null
        var documentTextProof: String? = null
        var otherDocumentNote: String? = null

        var applicationScopeArea: String? = null
        var applicationScopeStartDate: String? = null
        var applicationScopeEndDate: String? = null

        var categorizedReference: String? = null

        var otherFootnote: String? = null
        var footnoteChange: String? = null
        var footnoteComment: String? = null
        var footnoteDecision: String? = null
        var footnoteStateLaw: String? = null
        var footnoteEuLaw: String? = null

        var digitalEvidenceLink: String? = null
        var digitalEvidenceRelatedData: String? = null
        var digitalEvidenceExternalDataNote: String? = null
        var digitalEvidenceAppendix: String? = null

        var eli: String? = null

        var celexNumber: String? = null

        var ageIndicationStart: String? = null
        var ageIndicationEnd: String? = null

        var text: String? = null

        fun toUseCaseData(): EditNormFrameUseCase.NormFrameProperties {
            val metadataSections = this.metadataSections.map { it.toUseCaseData() }

            return EditNormFrameUseCase.NormFrameProperties(
                this.officialLongTitle,
                metadataSections,
                this.risAbbreviation,
                this.documentNumber,
                this.documentCategory,
                this.documentTypeName,
                this.documentNormCategory,
                this.documentTemplateName,
                this.providerEntity,
                this.providerDecidingBody,
                this.providerIsResolutionMajority,
                this.officialShortTitle,
                this.officialAbbreviation,
                decodeLocalDate(this.entryIntoForceDate),
                this.entryIntoForceDateState,
                decodeLocalDate(this.principleEntryIntoForceDate),
                this.principleEntryIntoForceDateState,
                decodeLocalDate(this.divergentEntryIntoForceDate),
                this.divergentEntryIntoForceDateState,
                this.entryIntoForceNormCategory,
                decodeLocalDate(this.expirationDate),
                this.expirationDateState,
                this.isExpirationDateTemp,
                decodeLocalDate(this.principleExpirationDate),
                this.principleExpirationDateState,
                decodeLocalDate(this.divergentExpirationDate),
                this.divergentExpirationDateState,
                this.expirationNormCategory,
                decodeLocalDate(this.announcementDate),
                decodeLocalDate(this.publicationDate),
                decodeLocalDate(this.citationDate),
                this.citationYear,
                this.printAnnouncementGazette,
                this.printAnnouncementYear,
                this.printAnnouncementNumber,
                this.printAnnouncementPage,
                this.printAnnouncementInfo,
                this.printAnnouncementExplanations,
                this.digitalAnnouncementMedium,
                decodeLocalDate(this.digitalAnnouncementDate),
                this.digitalAnnouncementEdition,
                this.digitalAnnouncementYear,
                this.digitalAnnouncementPage,
                this.digitalAnnouncementArea,
                this.digitalAnnouncementAreaNumber,
                this.digitalAnnouncementInfo,
                this.digitalAnnouncementExplanations,
                this.euAnnouncementGazette,
                this.euAnnouncementYear,
                this.euAnnouncementSeries,
                this.euAnnouncementNumber,
                this.euAnnouncementPage,
                this.euAnnouncementInfo,
                this.euAnnouncementExplanations,
                this.otherOfficialAnnouncement,
                this.completeCitation,
                this.statusNote,
                this.statusDescription,
                decodeLocalDate(this.statusDate),
                this.statusReference,
                this.repealNote,
                this.repealArticle,
                decodeLocalDate(this.repealDate),
                this.repealReferences,
                this.reissueNote,
                this.reissueArticle,
                decodeLocalDate(this.reissueDate),
                this.reissueReference,
                this.otherStatusNote,
                this.documentStatusWorkNote,
                this.documentStatusDescription,
                decodeLocalDate(this.documentStatusDate),
                this.documentStatusReference,
                decodeLocalDate(this.documentStatusEntryIntoForceDate),
                this.documentStatusProof,
                this.documentTextProof,
                this.otherDocumentNote,
                this.applicationScopeArea,
                decodeLocalDate(this.applicationScopeStartDate),
                decodeLocalDate(this.applicationScopeEndDate),
                this.categorizedReference,
                this.otherFootnote,
                this.footnoteChange,
                this.footnoteComment,
                this.footnoteDecision,
                this.footnoteStateLaw,
                this.footnoteEuLaw,
                this.digitalEvidenceLink,
                this.digitalEvidenceRelatedData,
                this.digitalEvidenceExternalDataNote,
                this.digitalEvidenceAppendix,
                this.celexNumber,
                this.ageIndicationStart,
                this.ageIndicationEnd,
                this.text,
            )
        }
    }

    class MetadataSectionRequestSchema {
        lateinit var name: MetadataSectionName
        lateinit var metadata: List<MetadatumRequestSchema>
        var sections: List<MetadataSectionRequestSchema>? = null

        fun toUseCaseData(): MetadataSection {
            val metadata = this.metadata.map { it.toUseCaseData() }
            val childSections = this.sections?.map { it.toUseCaseData() }
            return MetadataSection(name = this.name, metadata = metadata, sections = childSections)
        }
    }

    class MetadatumRequestSchema {
        lateinit var value: String
        lateinit var type: MetadatumType
        var order: Int = 0

        fun toUseCaseData(): Metadatum<*> = Metadatum(value = this.value, type = this.type, order = this.order)
    }
}
