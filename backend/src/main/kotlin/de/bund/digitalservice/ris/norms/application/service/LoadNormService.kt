package de.bund.digitalservice.ris.norms.application.service

import de.bund.digitalservice.ris.norms.application.port.input.LoadNormUseCase
import de.bund.digitalservice.ris.norms.application.port.output.GetNormByGuidOutputPort
import de.bund.digitalservice.ris.norms.domain.entity.Norm
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class LoadNormService(private val getNormByGuidPort: GetNormByGuidOutputPort) : LoadNormUseCase {

    override fun loadNorm(query: LoadNormUseCase.Query): Mono<Norm> {
        return getNormByGuidPort.getNormByGuid(query.guid)
    }
}
