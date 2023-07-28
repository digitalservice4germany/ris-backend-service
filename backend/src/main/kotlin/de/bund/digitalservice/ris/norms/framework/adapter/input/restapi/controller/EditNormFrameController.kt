package de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.controller

import de.bund.digitalservice.ris.norms.application.port.input.EditNormFrameUseCase
import de.bund.digitalservice.ris.norms.domain.entity.MetadataSection
import de.bund.digitalservice.ris.norms.domain.entity.Metadatum
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType
import de.bund.digitalservice.ris.norms.domain.value.NormCategory
import de.bund.digitalservice.ris.norms.domain.value.UndefinedDate
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.ApiConfiguration
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.OpenApiConfiguration
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.decodeGuid
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.decodeLocalDate
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.decodeLocalTime
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping(ApiConfiguration.API_NORMS_PATH)
@Tag(name = OpenApiConfiguration.NORMS_TAG)
class EditNormFrameController(private val editNormFrameService: EditNormFrameUseCase) {

  @PutMapping(path = ["/{guid}"])
  @Operation(
      summary = "Edit the frame data of a norm",
      description = "Edits a norm given its unique guid identifier")
  @ApiResponses(
      ApiResponse(responseCode = "204", description = "Norm was updated"),
      ApiResponse(responseCode = "400"),
  )
  fun editNormFrame(
      @PathVariable guid: String,
      @RequestBody request: NormFramePropertiesRequestSchema,
  ): Mono<ResponseEntity<Unit>> {
    val properties = request.toUseCaseData()
    val command = EditNormFrameUseCase.Command(decodeGuid(guid), properties)

    return editNormFrameService.editNormFrame(command).map {
      ResponseEntity.noContent().build<Unit>()
    }
  }

  class NormFramePropertiesRequestSchema {
    lateinit var metadataSections: List<MetadataSectionRequestSchema>

    var eli: String? = null

    fun toUseCaseData(): EditNormFrameUseCase.NormFrameProperties =
        EditNormFrameUseCase.NormFrameProperties(
            this.metadataSections.map { it.toUseCaseData() },
        )
  }

  class MetadataSectionRequestSchema {
    lateinit var name: MetadataSectionName
    var metadata: List<MetadatumRequestSchema>? = null
    var order: Int = 1
    var sections: List<MetadataSectionRequestSchema>? = null

    fun toUseCaseData(): MetadataSection {
      val metadata = this.metadata?.map { it.toUseCaseData() }
      val childSections = this.sections?.map { it.toUseCaseData() }
      return MetadataSection(
          name = this.name,
          order = order,
          metadata = metadata ?: emptyList(),
          sections = childSections)
    }
  }

  class MetadatumRequestSchema {
    lateinit var value: String
    lateinit var type: MetadatumType
    var order: Int = 1

    fun toUseCaseData(): Metadatum<*> {
      val value =
          when (this.type) {
            MetadatumType.DATE -> decodeLocalDate(this.value)
            MetadatumType.TIME -> decodeLocalTime(this.value)
            MetadatumType.RESOLUTION_MAJORITY -> this.value.toBoolean()
            MetadatumType.NORM_CATEGORY -> NormCategory.valueOf(this.value)
            MetadatumType.UNDEFINED_DATE -> UndefinedDate.valueOf(this.value)
            else -> this.value
          }
      return Metadatum(value = value, type = this.type, order = this.order)
    }
  }
}
