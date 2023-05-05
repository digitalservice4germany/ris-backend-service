package de.bund.digitalservice.ris.norms.domain.specification.norm

import de.bund.digitalservice.ris.norms.domain.entity.Norm
import de.bund.digitalservice.ris.norms.domain.specification.Specification
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.AGE_INDICATION
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.CITATION_DATE
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.DOCUMENT_TYPE
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.LEAD
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.NORM
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.NORM_PROVIDER
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.OFFICIAL_REFERENCE
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.PARTICIPATION
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName.SUBJECT_AREA

val hasValidSections =
    object : Specification<Norm> {
        override fun isSatisfiedBy(instance: Norm): Boolean = instance.metadataSections.all {
            it.name in listOf(
                NORM,
                SUBJECT_AREA,
                LEAD,
                PARTICIPATION,
                CITATION_DATE,
                AGE_INDICATION,
                OFFICIAL_REFERENCE,
                NORM_PROVIDER,
                DOCUMENT_TYPE,
            )
        }
    }
