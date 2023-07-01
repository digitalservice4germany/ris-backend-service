package de.bund.digitalservice.ris.norms.domain.specification.norm

import de.bund.digitalservice.ris.norms.domain.entity.MetadataSection
import de.bund.digitalservice.ris.norms.domain.entity.Metadatum
import de.bund.digitalservice.ris.norms.domain.entity.Norm
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class HasValidSectionsTest {
    @Test
    fun `it is not satisfied if the section can not be added directly to the Norm`() {
        val instance = mockk<Norm>()
        every { instance.guid } returns UUID.randomUUID()

        every { instance.metadataSections } returns listOf(
            MetadataSection(
                MetadataSectionName.PRINT_ANNOUNCEMENT,
                listOf(Metadatum("announcement gazette", MetadatumType.ANNOUNCEMENT_GAZETTE)),
            ),
            MetadataSection(
                MetadataSectionName.DIGITAL_ANNOUNCEMENT,
                listOf(Metadatum("announcement medium", MetadatumType.ANNOUNCEMENT_MEDIUM)),
            ),
            MetadataSection(
                MetadataSectionName.EU_ANNOUNCEMENT,
                listOf(Metadatum("eu government gazette", MetadatumType.EU_GOVERNMENT_GAZETTE)),
            ),
            MetadataSection(
                MetadataSectionName.OTHER_OFFICIAL_ANNOUNCEMENT,
                listOf(Metadatum("other official reference", MetadatumType.OTHER_OFFICIAL_REFERENCE)),
            ),
        )

        assertThat(hasValidSections.isSatisfiedBy(instance)).isFalse()
    }

    @Test
    fun `it is satisfied if the section can be added directly to the Norm`() {
        val instance = mockk<Norm>()
        every { instance.guid } returns UUID.randomUUID()

        every { instance.metadataSections } returns listOf(
            MetadataSection(
                MetadataSectionName.NORM,
                listOf(Metadatum("unofficial short title", MetadatumType.UNOFFICIAL_SHORT_TITLE)),
            ),
            MetadataSection(
                MetadataSectionName.OFFICIAL_REFERENCE,
                listOf(),
                1,
                listOf(
                    MetadataSection(
                        MetadataSectionName.DIGITAL_ANNOUNCEMENT,
                        listOf(Metadatum("announcement medium", MetadatumType.ANNOUNCEMENT_MEDIUM)),
                    ),
                ),
            ),
            MetadataSection(
                MetadataSectionName.CATEGORIZED_REFERENCE,
                listOf(Metadatum("test reference", MetadatumType.TEXT)),
                1,
                emptyList(),
            ),
        )

        assertThat(hasValidSections.isSatisfiedBy(instance)).isTrue()
    }

    @Test
    fun `it is satisfied if the section footnote can be added directly to the Norm`() {
        val instance = mockk<Norm>()
        every { instance.guid } returns UUID.randomUUID()
        every { instance.metadataSections } returns listOf(
            MetadataSection(
                MetadataSectionName.FOOTNOTES,
                listOf(Metadatum("footnote reference", MetadatumType.FOOTNOTE_REFERENCE)),
            ),
        )
        assertThat(hasValidSections.isSatisfiedBy(instance)).isTrue()
    }

    @Test
    fun `it is satisfied if the document status section can be added directly to the Norm`() {
        val instance = mockk<Norm>()
        every { instance.guid } returns UUID.randomUUID()

        every { instance.metadataSections } returns listOf(
            MetadataSection(
                MetadataSectionName.NORM,
                listOf(Metadatum("unofficial short title", MetadatumType.UNOFFICIAL_SHORT_TITLE)),
            ),
            MetadataSection(
                MetadataSectionName.DOCUMENT_STATUS_SECTION,
                listOf(),
                1,
                listOf(
                    MetadataSection(
                        MetadataSectionName.DOCUMENT_STATUS,
                        listOf(Metadatum("work note", MetadatumType.WORK_NOTE)),
                    ),
                ),
            ),
        )

        assertThat(hasValidSections.isSatisfiedBy(instance)).isTrue()
    }

    @Test
    fun `it is satisfied if the status indication can be added directly to the Norm`() {
        val instance = mockk<Norm>()
        every { instance.guid } returns UUID.randomUUID()

        every { instance.metadataSections } returns listOf(
            MetadataSection(
                MetadataSectionName.NORM,
                listOf(Metadatum("unofficial short title", MetadatumType.UNOFFICIAL_SHORT_TITLE)),
            ),
            MetadataSection(
                MetadataSectionName.STATUS_INDICATION,
                listOf(),
                1,
                listOf(
                    MetadataSection(
                        MetadataSectionName.STATUS,
                        listOf(Metadatum("note", MetadatumType.NOTE)),
                    ),
                ),
            ),
        )

        assertThat(hasValidSections.isSatisfiedBy(instance)).isTrue()
    }

    @Test
    fun `it is satisfied if the publication date can be added directly to the Norm`() {
        val instance = mockk<Norm>()
        every { instance.guid } returns UUID.randomUUID()

        every { instance.metadataSections } returns listOf(
            MetadataSection(
                MetadataSectionName.PUBLICATION_DATE,
                listOf(Metadatum(LocalDate.now(), MetadatumType.DATE)),
            ),
        )

        assertThat(hasValidSections.isSatisfiedBy(instance)).isTrue()
    }
}
