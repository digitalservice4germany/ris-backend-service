package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.domain.HandoverReport;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class PostgresHandoverResultReportRepositoryImplMailTest {

  PostgresHandoverReportRepositoryImpl reportRepository;
  @MockBean private DatabaseHandoverReportRepository handoverReportRepository;

  @BeforeEach
  public void setup() {
    this.reportRepository = new PostgresHandoverReportRepositoryImpl(handoverReportRepository);
  }

  @Test
  void saveAll() {
    UUID entityId = UUID.randomUUID();
    Instant received = Instant.now();

    when(handoverReportRepository.saveAll(any(Iterable.class)))
        .thenReturn(
            List.of(
                HandoverReportDTO.builder()
                    .content("report content")
                    .receivedDate(received)
                    .entityId(entityId)
                    .build()));

    HandoverReport report =
        HandoverReport.builder()
            .entityId(entityId)
            .content("report content")
            .receivedDate(received)
            .build();

    List<HandoverReport> savedReportList =
        reportRepository.saveAll(Collections.singletonList(report));

    assertThat(savedReportList).hasSize(1);
    assertThat(savedReportList.get(0).content()).isEqualTo("report content");
    assertThat(savedReportList.get(0).receivedDate()).isEqualTo(received);

    verify(handoverReportRepository)
        .saveAll(
            Collections.singletonList(
                HandoverReportDTO.builder()
                    .content("report content")
                    .receivedDate(received)
                    .entityId(entityId)
                    .id(any())
                    .build()));
  }
}
