package de.bund.digitalservice.ris.caselaw.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.domain.FieldOfLawRepository;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.FieldOfLaw;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.Norm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import({FieldOfLawService.class})
class FieldOfLawServiceTest {
  @Autowired FieldOfLawService service;

  @MockBean FieldOfLawRepository repository;

  @Test
  void testGetFieldsOfLaw_withoutQuery_shouldntCallRepository() {
    Pageable pageable = Pageable.unpaged();
    when(repository.findAllByOrderByIdentifierAsc(pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    var page = service.getFieldsOfLawBySearchQuery(Optional.empty(), pageable);
    assertThat(page.getContent()).isEmpty();
    assertThat(page.isEmpty()).isTrue();

    verify(repository, times(1)).findAllByOrderByIdentifierAsc(pageable);
  }

  @Test
  void testGetFieldsOfLaw_withEmptyQuery_shouldntCallRepository() {
    Pageable pageable = Pageable.unpaged();
    when(repository.findAllByOrderByIdentifierAsc(pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    var page = service.getFieldsOfLawBySearchQuery(Optional.of(""), pageable);
    assertThat(page.getContent()).isEmpty();
    assertThat(page.isEmpty()).isTrue();

    verify(repository, times(1)).findAllByOrderByIdentifierAsc(pageable);
  }

  @Test
  void testGetFieldsOfLaw_withEmptyTerms_shouldReturnEmptyList() {
    List<FieldOfLaw> resultWithNullSearchTerms = repository.findBySearchTerms(null);
    Assertions.assertEquals(0, resultWithNullSearchTerms.size());
    List<FieldOfLaw> resultWithEmptySearchTerms = repository.findBySearchTerms(new String[0]);
    Assertions.assertEquals(0, resultWithEmptySearchTerms.size());
  }

  @Test
  void testGetFieldsOfLaw_withQuery_shouldCallRepository() {
    Pageable pageable = PageRequest.of(0, 10);
    String[] searchTerms = new String[] {"test"};
    when(repository.findBySearchTerms(searchTerms)).thenReturn(Collections.emptyList());

    var page = service.getFieldsOfLawBySearchQuery(Optional.of("test"), pageable);
    assertThat(page.getContent()).isEmpty();
    assertThat(page.isEmpty()).isTrue();

    verify(repository, times(1)).findBySearchTerms(searchTerms);
    verify(repository, never()).findAllByOrderByIdentifierAsc(pageable);
  }

  @Test
  void testGetFieldsOfLaw_withMultipleSearchTerms_shouldCallRepository() {
    Pageable pageable = PageRequest.of(0, 10);
    String[] searchTerms = new String[] {"test", "multiple"};
    when(repository.findBySearchTerms(searchTerms)).thenReturn(Collections.emptyList());

    var page = service.getFieldsOfLawBySearchQuery(Optional.of("test multiple"), pageable);
    assertThat(page.getContent()).isEmpty();
    assertThat(page.isEmpty()).isTrue();

    verify(repository, times(1)).findBySearchTerms(searchTerms);
    verify(repository, never()).findAllByOrderByIdentifierAsc(pageable);
  }

  @Test
  void
      testGetFieldsOfLaw_withQueryWithWhitespaceAtTheStartAndTheEnd_shouldCallRepositoryWithTrimmedSearchString() {
    Pageable pageable = PageRequest.of(0, 10);
    String[] searchTerms = new String[] {"test"};
    when(repository.findBySearchTerms(searchTerms)).thenReturn(Collections.emptyList());

    var page = service.getFieldsOfLawBySearchQuery(Optional.of(" test  \t"), pageable);
    assertThat(page.getContent()).isEmpty();
    assertThat(page.isEmpty()).isTrue();

    verify(repository, times(1)).findBySearchTerms(searchTerms);
    verify(repository, never()).findAllByOrderByIdentifierAsc(pageable);
  }

  @Test
  void testGetChildrenOfFieldOfLaw_withNumberIsEmpty_shouldCallRepository() {
    when(repository.findAllByParentIdentifierOrderByIdentifierAsc(""))
        .thenReturn(Collections.emptyList());

    Assertions.assertTrue(service.getChildrenOfFieldOfLaw("").isEmpty());

    verify(repository, times(1)).findAllByParentIdentifierOrderByIdentifierAsc("");
    verify(repository, never()).getTopLevelNodes();
  }

  @Test
  void testGetChildrenOfFieldOfLaw_withNumberIsRoot_shouldCallRepository() {
    when(repository.getTopLevelNodes()).thenReturn(Collections.emptyList());

    Assertions.assertTrue(service.getChildrenOfFieldOfLaw("root").isEmpty());

    verify(repository, times(1)).getTopLevelNodes();
    verify(repository, never()).findAllByParentIdentifierOrderByIdentifierAsc(anyString());
  }

  @Test
  void testGetChildrenOfFieldOfLaw_withNumber_shouldCallRepository() {
    when(repository.findAllByParentIdentifierOrderByIdentifierAsc("test"))
        .thenReturn(Collections.emptyList());

    Assertions.assertTrue(service.getChildrenOfFieldOfLaw("test").isEmpty());

    verify(repository, times(1)).findAllByParentIdentifierOrderByIdentifierAsc("test");
    verify(repository, never()).getTopLevelNodes();
  }

  @Test
  void testGetTreeForFieldOfLaw_withFieldNumberDoesntExist() {
    when(repository.findTreeByIdentifier("test")).thenReturn(null);
    service.getTreeForFieldOfLaw("test");
    verify(repository, times(1)).findTreeByIdentifier("test");
  }

  @Test
  void testGetTreeForFieldOfLaw_withFieldNumberAtTopLevel() {
    FieldOfLaw child = FieldOfLaw.builder().identifier("test").build();
    when(repository.findTreeByIdentifier("test")).thenReturn(child);

    var folTree = service.getTreeForFieldOfLaw("test");
    Assertions.assertNotNull(folTree);

    verify(repository, times(1)).findTreeByIdentifier("test");
  }

  @Test
  void testGetFieldsOfLaw_withSearchString() {
    String searchString = "stext";
    String[] searchTerms = new String[] {searchString};
    FieldOfLaw expectedFieldOfLaw =
        FieldOfLaw.builder()
            .id(UUID.randomUUID())
            .hasChildren(false)
            .identifier("TS-01-01")
            .text("stext 2")
            .linkedFields(Collections.emptyList())
            .norms(List.of(new Norm("abbr1", "description")))
            .children(Collections.emptyList())
            .build();

    Pageable pageable = PageRequest.of(0, 10);
    PageImpl<FieldOfLaw> page = new PageImpl<>(List.of(expectedFieldOfLaw), pageable, 1);

    when(repository.findBySearchTerms(searchTerms)).thenReturn(List.of(expectedFieldOfLaw));

    Slice<FieldOfLaw> fieldOfLawPage =
        service.getFieldsOfLawBySearchQuery(Optional.of(searchString), pageable);
    assertThat(fieldOfLawPage).isEqualTo(page);

    verify(repository).findBySearchTerms(searchTerms);
  }

  @Test
  void testGetFieldOfLawChildren() {
    FieldOfLaw expectedFieldOfLaw =
        FieldOfLaw.builder()
            .id(UUID.randomUUID())
            .hasChildren(true)
            .identifier("TS-01-01")
            .text("stext 2")
            .linkedFields(Collections.emptyList())
            .norms(List.of(new Norm("abbr1", "description")))
            .children(new ArrayList<>())
            .build();

    when(repository.findAllByParentIdentifierOrderByIdentifierAsc("TS-01-01"))
        .thenReturn(List.of(expectedFieldOfLaw));

    var response = service.getChildrenOfFieldOfLaw("TS-01-01");

    assertThat(response).hasSize(1);
    assertThat(response).extracting("identifier").containsExactly("TS-01" + "-01");

    verify(repository).findAllByParentIdentifierOrderByIdentifierAsc("TS-01-01");
  }

  @Test
  void testSearchAndOrderByScore_pageableOffsetGreaterThanResultListSize() {
    FieldOfLaw databaseFieldOfLaw = FieldOfLaw.builder().build();

    when(repository.findBySearchTerms(any(String[].class))).thenReturn(List.of(databaseFieldOfLaw));

    Slice<FieldOfLaw> result = service.searchAndOrderByScore("foo", PageRequest.of(1, 5));
    assertThat(result).isEmpty();
  }
}
