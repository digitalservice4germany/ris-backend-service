package de.bund.digitalservice.ris.norms.application.service

import de.bund.digitalservice.ris.norms.application.port.input.ListNormsUseCase
import de.bund.digitalservice.ris.norms.application.port.output.SearchNormsOutputPort
import de.bund.digitalservice.ris.norms.domain.entity.Norm
import de.bund.digitalservice.ris.norms.domain.value.MetadataSectionName
import de.bund.digitalservice.ris.norms.domain.value.MetadatumType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class ListNormsService(private val searchNormsOutputAdapter: SearchNormsOutputPort) : ListNormsUseCase {

    companion object {
        private val logger = LoggerFactory.getLogger(ListNormsService::class.java)
    }

    override fun listNorms(query: ListNormsUseCase.Query): Flux<ListNormsUseCase.NormData> {
        return searchNormsOutputAdapter.searchNorms(SearchNormsOutputPort.Query(query.searchTerm ?: ""))
            .map { mapToNormData(it) }
            .doOnError { exception ->
                logger.error("Error occurred while listing all norms:", exception)
            }
    }
}

private fun mapToNormData(norm: Norm) = ListNormsUseCase.NormData(
    norm.guid,
    norm.getFirstMetadatum(MetadataSectionName.NORM, MetadatumType.OFFICIAL_LONG_TITLE)?.value.toString(),
    norm.eli,
)
