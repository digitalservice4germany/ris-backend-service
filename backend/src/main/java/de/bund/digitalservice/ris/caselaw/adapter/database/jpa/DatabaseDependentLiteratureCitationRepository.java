package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseDependentLiteratureCitationRepository
    extends JpaRepository<DependentLiteratureCitationDTO, UUID> {}
