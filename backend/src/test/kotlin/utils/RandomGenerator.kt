package utils

import de.bund.digitalservice.ris.norms.application.port.input.EditNormFrameUseCase
import de.bund.digitalservice.ris.norms.domain.entity.Article
import de.bund.digitalservice.ris.norms.domain.entity.FileReference
import de.bund.digitalservice.ris.norms.domain.entity.MetadataSection
import de.bund.digitalservice.ris.norms.domain.entity.Metadatum
import de.bund.digitalservice.ris.norms.domain.entity.Norm
import de.bund.digitalservice.ris.norms.domain.entity.Paragraph
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.controller.EditNormFrameControllerTest
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.decodeLocalDate
import org.apache.commons.lang3.RandomStringUtils
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates.inClass
import org.jeasy.random.FieldPredicates.named
import java.time.LocalDate
import java.util.Random

fun createRandomNormFameProperties(): EditNormFrameUseCase.NormFrameProperties {
    val parameters: EasyRandomParameters =
        EasyRandomParameters()
            .randomize(named("metadataSections")) {
                createSimpleSections()
            }
    return EasyRandom(parameters).nextObject(EditNormFrameUseCase.NormFrameProperties::class.java)
}

fun createRandomEditNormRequestTestSchema(): EditNormFrameControllerTest.NormFramePropertiesTestRequestSchema {
    val parameters: EasyRandomParameters =
        EasyRandomParameters().randomize(named(".+Date\$")) {
            // needed for string date fields
            createRandomLocalDateInString()
        }
            .randomize(named("metadataSections")) {
                createSimpleSections()
            }
    return EasyRandom(parameters)
        .nextObject(EditNormFrameControllerTest.NormFramePropertiesTestRequestSchema::class.java)
}

fun createRandomNorm(): Norm {
    val parameters: EasyRandomParameters =
        EasyRandomParameters().randomize(named("marker").and(inClass(Article::class.java))) {
            "§ " + Random().nextInt(1, 50).toString()
        }.randomize(named("marker").and(inClass(Paragraph::class.java))) {
            "(" + Random().nextInt(1, 50).toString() + ")"
        }.randomize(named("citationYear")) {
            EasyRandom(EasyRandomParameters().stringLengthRange(4, 4)).nextObject(String::class.java)
        }.randomize(named("metadataSections")) {
            emptyList<MetadataSection>()
        }.randomize(named("files")) {
            emptyList<FileReference>()
        }
    return EasyRandom(parameters).nextObject(Norm::class.java)
}

fun createRandomNormWithCitationDate(): Norm {
    return createRandomNorm().copy(
        metadataSections = listOf(
            MetadataSection(MetadataSectionName.CITATION_DATE, listOf(Metadatum(decodeLocalDate("2002-02-02"), MetadatumType.DATE))),
        ),
    )
}

fun createRandomFileReference(): FileReference {
    return EasyRandom().nextObject(FileReference::class.java)
}

private fun createRandomLocalDateInString(): String {
    return EasyRandom().nextObject(LocalDate::class.java).toString()
}

fun createSimpleSections(): List<MetadataSection> = listOf(
    MetadataSection(
        MetadataSectionName.NORM,
        listOf(
            Metadatum("foo", MetadatumType.KEYWORD, 0),
            Metadatum("bar", MetadatumType.KEYWORD, 1),
        ),
    ),
)

fun randomString(length: Int = 10): String = RandomStringUtils.randomAlphabetic(length)
