package de.bund.digitalservice.ris.norms.domain.specification.section

import de.bund.digitalservice.ris.norms.domain.entity.MetadataSection
import de.bund.digitalservice.ris.norms.domain.specification.Specification
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.AGE_INDICATION
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.CATEGORIZED_REFERENCE
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.CITATION_DATE
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.DIGITAL_ANNOUNCEMENT
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.DIVERGENT_ENTRY_INTO_FORCE
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.DIVERGENT_ENTRY_INTO_FORCE_DEFINED
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.DIVERGENT_ENTRY_INTO_FORCE_UNDEFINED
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.DIVERGENT_EXPIRATION
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.DIVERGENT_EXPIRATION_DEFINED
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.DIVERGENT_EXPIRATION_UNDEFINED
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.DOCUMENT_TYPE
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.ENTRY_INTO_FORCE
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.EU_ANNOUNCEMENT
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.EXPIRATION
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.LEAD
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.NORM
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.NORM_PROVIDER
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.OFFICIAL_REFERENCE
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.OTHER_OFFICIAL_ANNOUNCEMENT
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.PARTICIPATION
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.PRINCIPLE_ENTRY_INTO_FORCE
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.PRINCIPLE_EXPIRATION
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.PRINT_ANNOUNCEMENT
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.SUBJECT_AREA

val hasValidChildren =
    object : Specification<MetadataSection> {
        override fun isSatisfiedBy(instance: MetadataSection): Boolean = when (instance.name) {
            NORM, NORM_PROVIDER, SUBJECT_AREA, LEAD, PARTICIPATION,
            CITATION_DATE, AGE_INDICATION, PRINT_ANNOUNCEMENT, DIGITAL_ANNOUNCEMENT,
            EU_ANNOUNCEMENT, OTHER_OFFICIAL_ANNOUNCEMENT, DOCUMENT_TYPE, DIVERGENT_ENTRY_INTO_FORCE_DEFINED,
            DIVERGENT_ENTRY_INTO_FORCE_UNDEFINED, DIVERGENT_EXPIRATION_DEFINED, DIVERGENT_EXPIRATION_UNDEFINED, CATEGORIZED_REFERENCE, ENTRY_INTO_FORCE, PRINCIPLE_ENTRY_INTO_FORCE, EXPIRATION, PRINCIPLE_EXPIRATION,
            -> hasNone(instance)

            OFFICIAL_REFERENCE -> hasOneOfType(listOf(PRINT_ANNOUNCEMENT, DIGITAL_ANNOUNCEMENT, EU_ANNOUNCEMENT, OTHER_OFFICIAL_ANNOUNCEMENT), instance)
            DIVERGENT_ENTRY_INTO_FORCE -> hasOneOfType(listOf(DIVERGENT_ENTRY_INTO_FORCE_DEFINED, DIVERGENT_ENTRY_INTO_FORCE_UNDEFINED), instance)
            DIVERGENT_EXPIRATION -> hasOneOfType(listOf(DIVERGENT_EXPIRATION_DEFINED, DIVERGENT_EXPIRATION_UNDEFINED), instance)
        }

        private fun hasNone(instance: MetadataSection): Boolean =
            instance.sections.isNullOrEmpty()
        private fun hasOneOfType(sectionNames: List<MetadataSectionName>, instance: MetadataSection): Boolean =
            instance.sections?.count() == 1 && instance.sections.all { it.name in sectionNames }
    }
