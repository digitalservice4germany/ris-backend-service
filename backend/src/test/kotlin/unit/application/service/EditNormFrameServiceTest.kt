package de.bund.digitalservice.ris.norms.application.service

import de.bund.digitalservice.ris.norms.application.port.input.EditNormFrameUseCase
import de.bund.digitalservice.ris.norms.application.port.output.EditNormOutputPort
import de.bund.digitalservice.ris.norms.domain.entity.MetadataSection
import de.bund.digitalservice.ris.norms.domain.entity.Metadatum
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.DEFINITION
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType.KEYWORD
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import utils.assertNormAndEditNormFrameProperties
import utils.createRandomNormFameProperties
import java.util.UUID

class EditNormFrameServiceTest {

    @Test
    fun `it calls the output port to save the norm minimal required properties`() {
        val editNormOutputPort = mockk<EditNormOutputPort>()
        val service = EditNormFrameService(editNormOutputPort)
        val guid = UUID.randomUUID()
        val properties = EditNormFrameUseCase.NormFrameProperties("new title", emptyList())
        val command = EditNormFrameUseCase.Command(guid, properties)

        every { editNormOutputPort.editNorm(any()) } returns Mono.just(true)

        StepVerifier.create(service.editNormFrame(command)).expectNextCount(1).verifyComplete()

        verify(exactly = 1) { editNormOutputPort.editNorm(any()) }
        verify {
            editNormOutputPort.editNorm(
                withArg {
                    assertNormAndEditNormFrameProperties(it.norm, properties)
                },
            )
        }
    }

    @Test
    fun `it calls the output port to save the norm with optional data and fields`() {
        val guid = UUID.randomUUID()
        val metadata = listOf(Metadatum("foo", KEYWORD, 0), Metadatum("bar", KEYWORD, 1))
        val metadataSections = listOf(
            MetadataSection(MetadataSectionName.GENERAL_INFORMATION, metadata, null),
            MetadataSection(MetadataSectionName.DEFINITION, listOf(Metadatum("definition", DEFINITION, 0)), null),
        )
        val properties = createRandomNormFameProperties().copy(metadataSections = metadataSections)
        val editNormOutputPort = mockk<EditNormOutputPort>()
        val service = EditNormFrameService(editNormOutputPort)
        val command = EditNormFrameUseCase.Command(guid, properties)

        every { editNormOutputPort.editNorm(any()) } returns Mono.just(true)

        StepVerifier.create(service.editNormFrame(command)).expectNextCount(1).verifyComplete()

        verify(exactly = 1) { editNormOutputPort.editNorm(any()) }

        verify {
            editNormOutputPort.editNorm(
                withArg {
                    assertNormAndEditNormFrameProperties(it.norm, properties)
                },
            )
        }
    }
}
