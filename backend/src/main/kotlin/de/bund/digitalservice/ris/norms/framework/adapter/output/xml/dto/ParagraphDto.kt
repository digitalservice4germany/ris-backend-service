package de.bund.digitalservice.ris.norms.framework.adapter.output.xml.dto

data class ParagraphDto(
    val guid: String,
    val marker: String,
    val markerText: IdentifiedElement?,
    val articleMarker: String,
    val content: ContentDto,
)
