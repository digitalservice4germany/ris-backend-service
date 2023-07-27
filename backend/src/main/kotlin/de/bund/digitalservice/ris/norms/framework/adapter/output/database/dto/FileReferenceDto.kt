package de.bund.digitalservice.ris.norms.framework.adapter.output.database.dto

import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(name = "files")
data class FileReferenceDto(
    val guid: UUID,
    val name: String,
    var hash: String,
    @Column("norm_guid") val normGuid: UUID,
    @Column("created_at") val createdAt: LocalDateTime,
)
