package de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.controller

import de.bund.digitalservice.ris.caselaw.config.FlywayConfig
import de.bund.digitalservice.ris.norms.application.port.output.SaveNormOutputPort
import de.bund.digitalservice.ris.norms.application.service.LoadNormService
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType
import de.bund.digitalservice.ris.norms.domain.value.NormCategory
import de.bund.digitalservice.ris.norms.framework.adapter.output.database.NormsService
import de.bund.digitalservice.ris.norms.framework.adapter.output.database.PostgresTestcontainerIntegrationTest
import de.bund.digitalservice.ris.norms.framework.adapter.output.database.dto.NormDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.AutoConfigureDataR2dbc
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.dialect.PostgresDialect
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.test.StepVerifier
import utils.factory.norm
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(SpringExtension::class)
@Import(FlywayConfig::class, NormsService::class, LoadNormService::class)
@WebFluxTest(controllers = [LoadNormController::class])
@WithMockUser
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureDataR2dbc
class LoadNormControllerIntegrationTest : PostgresTestcontainerIntegrationTest() {
    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var client: DatabaseClient

    @Autowired
    lateinit var normsService: NormsService

    @Autowired
    lateinit var loadNormFrameService: LoadNormService

    private lateinit var template: R2dbcEntityTemplate

    @BeforeAll
    fun setup() {
        template = R2dbcEntityTemplate(client, PostgresDialect.INSTANCE)
    }

    @AfterEach
    fun cleanUp() {
        template.delete(NormDto::class.java).all().block(Duration.ofSeconds(1))
    }

    @Test
    fun `it correctly loads a norm with metadata sections via api`() {
        val date = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)

        val norm = norm {
            files {
                file {
                    name = "norm.zip"
                    hash = "hash"
                    createdAt = date
                }
            }
            metadataSections {
                metadataSection {
                    name = MetadataSectionName.CITATION_DATE
                    metadata {
                        metadatum {
                            value = date.toLocalDate()
                            type = MetadatumType.DATE
                        }
                    }
                }
                metadataSection {
                    name = MetadataSectionName.DOCUMENT_TYPE
                    metadata {
                        metadatum {
                            value = NormCategory.BASE_NORM
                            type = MetadatumType.NORM_CATEGORY
                        }
                        metadatum {
                            value = "documentTypeName"
                            type = MetadatumType.TYPE_NAME
                        }
                        metadatum {
                            value = "documentTemplateName"
                            type = MetadatumType.TEMPLATE_NAME
                        }
                    }
                }
            }
        }

        val saveCommand = SaveNormOutputPort.Command(norm)
        normsService.saveNorm(saveCommand)
            .`as`(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete()

        webClient
            .mutateWith(csrf())
            .get()
            .uri("/api/v1/norms/" + norm.guid.toString())
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
                """
                {
                  "guid":"${norm.guid}",
                  "articles":[],
                  "metadataSections":[{"name":"CITATION_DATE","order":1,"metadata":[{"value":"${date.toLocalDate()}","type":"DATE","order":1}],"sections":null}, {"name":"DOCUMENT_TYPE","order":1,"metadata":[{"value":"BASE_NORM","type":"NORM_CATEGORY","order":1}, {"value":"documentTypeName","type":"TYPE_NAME","order":1}, {"value":"documentTemplateName","type":"TEMPLATE_NAME","order":1}],"sections":null}],
                  "officialLongTitle":"${norm.officialLongTitle}",
                  "risAbbreviation":"${norm.risAbbreviation}",
                  "documentNumber": "${norm.documentNumber}",
                  "documentCategory": "${norm.documentCategory}",
                  "officialShortTitle": "${norm.officialShortTitle}",
                  "officialAbbreviation": "${norm.officialAbbreviation}",
                  "entryIntoForceDate": "${norm.entryIntoForceDate}",
                  "entryIntoForceDateState": null,
                  "principleEntryIntoForceDate": "${norm.principleEntryIntoForceDate}",
                  "principleEntryIntoForceDateState": null,
                  "expirationDate": "${norm.expirationDate}",
                  "expirationDateState": null,
                  "isExpirationDateTemp": ${norm.isExpirationDateTemp},
                  "principleExpirationDate": "${norm.principleExpirationDate}",
                  "principleExpirationDateState": null,
                  "announcementDate": "${norm.announcementDate}",
                  "publicationDate": "${norm.publicationDate}",
                  "completeCitation": "${norm.completeCitation}",
                  "statusNote": "${norm.statusNote}",
                  "statusDescription": "${norm.statusDescription}",
                  "statusDate": "${norm.statusDate}",
                  "statusReference": "${norm.statusReference}",
                  "repealNote": "${norm.repealNote}",
                  "repealArticle": "${norm.repealArticle}",
                  "repealDate": "${norm.repealDate}",
                  "repealReferences": "${norm.repealReferences}",
                  "reissueNote": "${norm.reissueNote}",
                  "reissueArticle": "${norm.reissueArticle}",
                  "reissueDate": "${norm.reissueDate}",
                  "reissueReference": "${norm.reissueReference}",
                  "otherStatusNote": "${norm.otherStatusNote}",
                  "documentStatusWorkNote": "${norm.documentStatusWorkNote}",
                  "documentStatusDescription": "${norm.documentStatusDescription}",
                  "documentStatusDate": "${norm.documentStatusDate}",
                  "documentStatusReference": "${norm.documentStatusReference}",
                  "documentStatusEntryIntoForceDate": "${norm.documentStatusEntryIntoForceDate}",
                  "documentStatusProof": "${norm.documentStatusProof}",
                  "documentTextProof": "${norm.documentTextProof}",
                  "otherDocumentNote": "${norm.otherDocumentNote}",
                  "applicationScopeArea": "${norm.applicationScopeArea}",
                  "applicationScopeStartDate": "${norm.applicationScopeStartDate}",
                  "applicationScopeEndDate": "${norm.applicationScopeEndDate}",
                  "categorizedReference": "${norm.categorizedReference}",
                  "otherFootnote": "${norm.otherFootnote}",
                  "footnoteChange": "${norm.footnoteChange}",
                  "footnoteComment": "${norm.footnoteComment}",
                  "footnoteDecision": "${norm.footnoteDecision}",
                  "footnoteStateLaw": "${norm.footnoteStateLaw}",
                  "footnoteEuLaw": "${norm.footnoteEuLaw}",
                  "digitalEvidenceLink": "${norm.digitalEvidenceLink}",
                  "digitalEvidenceRelatedData": "${norm.digitalEvidenceRelatedData}",
                  "digitalEvidenceExternalDataNote": "${norm.digitalEvidenceExternalDataNote}",
                  "digitalEvidenceAppendix": "${norm.digitalEvidenceAppendix}",
                  "eli":"",
                  "celexNumber": "${norm.celexNumber}",
                  "text": "${norm.text}",
                  "files":[{"name":"norm.zip","hash":"hash","createdAt":"$date"}]}
                """.trimIndent(),
            )
    }
}
