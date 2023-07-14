package de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.controller

import de.bund.digitalservice.ris.norms.application.port.input.GenerateNormFileUseCase
import de.bund.digitalservice.ris.norms.domain.entity.FileReference
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.ApiConfiguration
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.OpenApiConfiguration
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.encodeGuid
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.encodeLocalDateTime
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping(ApiConfiguration.API_NORMS_PATH)
@Tag(name = OpenApiConfiguration.NORMS_TAG)
class GenerateNormFileController(private val generateNormFileService: GenerateNormFileUseCase) {

    @PostMapping(path = ["/{guid}/files"])
    @Operation(summary = "Generate a new juris zip export for the norm", description = "Generate a new compressed ZIP file containing all juris xml files for the latest edits of a norm")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Zip File Created"),
        ApiResponse(responseCode = "400"),
    )
    fun generateNormFile(@PathVariable guid: String): Mono<ResponseEntity<FileReferenceResponseSchema>> {
        val command = GenerateNormFileUseCase.Command(UUID.fromString(guid))
        return generateNormFileService.generateNormFile(command)
            .map { fileReference -> FileReferenceResponseSchema.fromUseCaseData(fileReference) }
            .map { fileReferenceResponseSchema -> ResponseEntity.ok().body(fileReferenceResponseSchema) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .onErrorReturn(ResponseEntity.internalServerError().build())
    }

    data class FileReferenceResponseSchema
    constructor(
        val name: String,
        val hash: String,
        val createdAt: String,
        val guid: String,
    ) {
        companion object {
            fun fromUseCaseData(data: FileReference): FileReferenceResponseSchema = FileReferenceResponseSchema(
                data.name,
                data.hash,
                encodeLocalDateTime(data.createdAt),
                encodeGuid(data.guid),
            )
        }
    }
}
