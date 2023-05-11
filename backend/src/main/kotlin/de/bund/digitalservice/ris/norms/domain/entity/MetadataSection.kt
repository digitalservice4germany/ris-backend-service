package de.bund.digitalservice.ris.norms.domain.entity

import de.bund.digitalservice.ris.norms.domain.specification.section.hasValidChildren
import de.bund.digitalservice.ris.norms.domain.specification.section.hasValidMetadata
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName
import java.util.*

data class MetadataSection(
    val name: MetadataSectionName,
    val metadata: List<Metadatum<*>>,
    val order: Int = 1,
    val sections: List<MetadataSection>? = null,
    val guid: UUID = UUID.randomUUID(),
) {
    init {
        require(hasValidChildren.isSatisfiedBy(this)) {
            "Incorrect children for section '$name'"
        }
        require(hasValidMetadata.isSatisfiedBy(this)) {
            "Incorrect metadata for section '$name'"
        }
    }
}
