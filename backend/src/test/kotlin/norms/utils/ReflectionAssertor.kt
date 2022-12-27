package norms.utils

import de.bund.digitalservice.ris.norms.application.port.input.EditNormFrameUseCase
import de.bund.digitalservice.ris.norms.application.port.input.ImportNormUseCase
import de.bund.digitalservice.ris.norms.domain.entity.Norm
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.controller.EditNormFrameController
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.controller.ImportNormController
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.decodeLocalDate
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.reflect.full.memberProperties

fun assertEditNormFrameProperties(
    commandProperties: EditNormFrameUseCase.NormFrameProperties,
    normFrameProperties: EditNormFrameUseCase.NormFrameProperties
) {
    EditNormFrameUseCase.NormFrameProperties::class.memberProperties.forEach {
        assertThat(it.get(commandProperties)).isEqualTo(it.get(normFrameProperties))
    }
}

fun assertEditNormFramePropertiesAndEditNormRequestSchema(
    normFrameProperties: EditNormFrameUseCase.NormFrameProperties,
    normFrameRequestSchema: EditNormFrameController.NormFramePropertiesRequestSchema
) {
    val normFrameRequestSchemaMembers =
        EditNormFrameController.NormFramePropertiesRequestSchema::class.memberProperties
    val normFramePropertiesMembers =
        EditNormFrameUseCase.NormFrameProperties::class.memberProperties
    normFramePropertiesMembers.forEach { normFramePropertiesMember ->
        val found =
            normFrameRequestSchemaMembers.find { normFrameRequestSchemaMember ->
                normFramePropertiesMember.name == normFrameRequestSchemaMember.name
            }
        when (
            val normFramePropertiesMemberValue =
                normFramePropertiesMember.get(normFrameProperties)
        ) {
            is LocalDate ->
                assertThat(normFramePropertiesMemberValue)
                    .isEqualTo(
                        decodeLocalDate(found?.get(normFrameRequestSchema).toString())
                    )
            else -> {
                assertThat(normFramePropertiesMemberValue)
                    .isEqualTo(found?.get(normFrameRequestSchema))
            }
        }
    }
}

fun assertNormAndEditNormFrameProperties(
    norm: Norm,
    normFrameProperties: EditNormFrameUseCase.NormFrameProperties
) {
    val normMembers = Norm::class.memberProperties
    val normFramePropertiesMembers =
        EditNormFrameUseCase.NormFrameProperties::class.memberProperties
    normFramePropertiesMembers.forEach { normFramePropertiesMember ->
        val found =
            normMembers.find { normMember -> normFramePropertiesMember.name == normMember.name }
        assertThat(normFramePropertiesMember.get(normFrameProperties)).isEqualTo(found?.get(norm))
    }
}

fun assertNormDataAndImportNormRequestSchemaWithoutArticles(
    normData: ImportNormUseCase.NormData,
    importNormRequestSchema: ImportNormController.NormRequestSchema
) {
    val normDataMembers = ImportNormUseCase.NormData::class.memberProperties
    val importNormRequestSchemaMembers =
        ImportNormController.NormRequestSchema::class.memberProperties
    normDataMembers.filter { it.name != "articles" }.forEach { normDataMember ->
        val found =
            importNormRequestSchemaMembers.find { importNormRequestSchemaMember ->
                normDataMember.name == importNormRequestSchemaMember.name
            }
        when (val normDataMemberValue = normDataMember.get(normData)) {
            is LocalDate ->
                assertThat(normDataMemberValue)
                    .isEqualTo(
                        decodeLocalDate(found?.get(importNormRequestSchema).toString())
                    )
            else -> {
                assertThat(normDataMemberValue).isEqualTo(found?.get(importNormRequestSchema))
            }
        }
    }
}

fun assertNormAndNormDataWithoutArticles(norm: Norm, normData: ImportNormUseCase.NormData) {
    val normMembers = Norm::class.memberProperties
    val normDataMembers = ImportNormUseCase.NormData::class.memberProperties
    normMembers.filter { it.name !in listOf("articles", "guid") }.forEach { normMember ->
        val found =
            normDataMembers.find { normDataMember -> normMember.name == normDataMember.name }
        when (val normMemberValue = normMember.get(norm)) {
            is LocalDate ->
                assertThat(normMemberValue)
                    .isEqualTo(decodeLocalDate(found?.get(normData).toString()))
            else -> {
                assertThat(normMemberValue).isEqualTo(found?.get(normData))
            }
        }
    }
}
