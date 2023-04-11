package de.bund.digitalservice.ris.norms.domain.entity

import de.bund.digitalservice.ris.norms.domain.specification.metadatum.hasValidValueType
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType

data class Metadatum<T>(val value: T, val type: MetadatumType, val order: Int = 1) {
    init {
        require(hasValidValueType.isSatisfiedBy(this)) {
            "Incorrect value type '${value!!::class.java.simpleName}' for datum type '$type'"
        }
    }
}
