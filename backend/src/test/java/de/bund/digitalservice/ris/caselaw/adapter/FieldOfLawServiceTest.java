package de.bund.digitalservice.ris.caselaw.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import({FieldOfLawService.class})
class FieldOfLawServiceTest {
  @Autowired FieldOfLawService service;

  @MockBean FieldOfLawRepository repository;

  @Test
  void testGetFieldsOfLaw_withoutQuery_shouldFindAllByOrderByIdentifierAsc() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAllByOrderByIdentifierAsc(pageable)).thenReturn(Page.empty());

    var page =
        service.getFieldsOfLawBySearchQuery(
            Optional.empty(), Optional.empty(), Optional.empty(), pageable);
    assertThat(page.isEmpty()).isTrue();
    assertThat(page.getContent()).isEmpty();

    verify(repository).findAllByOrderByIdentifierAsc(pageable);
    verify(repository, never()).findByCombinedCriteria(any(), any(), any());
  }

  @Test
  void testGetFieldsOfLaw_withEmptyQuery_shouldFindAllByOrderByIdentifierAsc() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAllByOrderByIdentifierAsc(pageable)).thenReturn(Page.empty());

    var page =
        service.getFieldsOfLawBySearchQuery(
            Optional.of(""), Optional.of(""), Optional.of(""), pageable);
    assertThat(page.isEmpty()).isTrue();
    assertThat(page.getContent()).isEmpty();

    verify(repository).findAllByOrderByIdentifierAsc(pageable);
    verify(repository, never()).findByCombinedCriteria(any(), any(), any());
  }

  @Test
  void testGetFieldsOfLaw_withIdentifier_shouldFindByIdentifier() {
    Pageable pageable = PageRequest.of(0, 10);
    var identifierString = "foo";
    var expectedFieldsOfLaw = List.of(generateFieldOfLaw());
    when(repository.findByCombinedCriteria(identifierString, null, null))
        .thenReturn(expectedFieldsOfLaw);

    var page =
        service.getFieldsOfLawBySearchQuery(
            Optional.of(identifierString), Optional.empty(), Optional.empty(), pageable);
    assertThat(page.getContent()).isEqualTo(expectedFieldsOfLaw);
    assertThat(page.isEmpty()).isFalse();

    verify(repository).findByCombinedCriteria(identifierString, null, null);
  }

  @Test
  void testGetFieldsOfLaw_withSearchTerm_shouldFindBySearchTerms() {
    Pageable pageable = PageRequest.of(0, 10);
    var searchTerms = new String[] {"foo", "bar"};
    var expectedFieldsOfLaw = List.of(generateFieldOfLaw());
    when(repository.findByCombinedCriteria(null, searchTerms, null))
        .thenReturn(expectedFieldsOfLaw);

    var page =
        service.getFieldsOfLawBySearchQuery(
            Optional.empty(), Optional.of("foo bar"), Optional.empty(), pageable);
    assertThat(page.isEmpty()).isFalse();
    assertThat(page.getContent()).isEqualTo(expectedFieldsOfLaw);

    verify(repository).findByCombinedCriteria(null, searchTerms, null);
  }

  @Test
  void testGetFieldsOfLaw_withNorm_shouldFindByNorm() {
    Pageable pageable = PageRequest.of(0, 10);
    var normSearchTerms = new String[] {"foo", "bar"};
    var expectedFieldsOfLaw = List.of(generateFieldOfLaw());
    when(repository.findByCombinedCriteria(null, null, normSearchTerms))
        .thenReturn(expectedFieldsOfLaw);

    var page =
        service.getFieldsOfLawBySearchQuery(
            Optional.empty(), Optional.empty(), Optional.of("foo § bar"), pageable);
    assertThat(page.isEmpty()).isFalse();
    assertThat(page.getContent()).isEqualTo(expectedFieldsOfLaw);

    verify(repository).findByCombinedCriteria(null, null, normSearchTerms);
  }

  @Test
  void
      testGetFieldsOfLaw_withIdentifierAndSearchTermAndNorm_shouldFindByIdentifierAndSearchTermsAndNorm() {
    Pageable pageable = PageRequest.of(0, 10);
    var identifierString = "foo";
    var searchTerms = new String[] {"foo", "bar"};
    var normString = new String[] {"baz", "qux"};
    var expectedFieldsOfLaw = List.of(generateFieldOfLaw());
    when(repository.findByCombinedCriteria(identifierString, searchTerms, normString))
        .thenReturn(expectedFieldsOfLaw);

    var page =
        service.getFieldsOfLawBySearchQuery(
            Optional.of(identifierString),
            Optional.of("foo bar"),
            Optional.of("§baz qux"),
            pageable);
    assertThat(page.isEmpty()).isFalse();
    assertThat(page.getContent()).isEqualTo(expectedFieldsOfLaw);

    verify(repository).findByCombinedCriteria(identifierString, searchTerms, normString);
  }

  @Test
  void testGetFieldsOfLaw_withIdentifierAndSearchTerm_shouldCallRepository() {
    Pageable pageable = PageRequest.of(0, 10);
    var identifierString = "foo";
    var searchTerms = new String[] {"foo", "bar"};
    var expectedFieldsOfLaw = List.of(generateFieldOfLaw());
    when(repository.findByCombinedCriteria(identifierString, searchTerms, null))
        .thenReturn(expectedFieldsOfLaw);

    var page =
        service.getFieldsOfLawBySearchQuery(
            Optional.of(identifierString), Optional.of("foo bar"), Optional.empty(), pageable);
    assertThat(page.isEmpty()).isFalse();
    assertThat(page.getContent()).isEqualTo(expectedFieldsOfLaw);

    verify(repository).findByCombinedCriteria(identifierString, searchTerms, null);
  }

  @Test
  void testGetFieldsOfLaw_withIdentifierAndNorm_shouldFindByIdentifierAndNorm() {
    Pageable pageable = PageRequest.of(0, 10);
    var identifierString = "foo";
    var normString = new String[] {"foo", "bar"};
    var expectedFieldsOfLaw = List.of(generateFieldOfLaw());
    when(repository.findByCombinedCriteria(identifierString, null, normString))
        .thenReturn(expectedFieldsOfLaw);

    var page =
        service.getFieldsOfLawBySearchQuery(
            Optional.of(identifierString), Optional.empty(), Optional.of("foo bar"), pageable);
    assertThat(page.isEmpty()).isFalse();
    assertThat(page.getContent()).isEqualTo(expectedFieldsOfLaw);

    verify(repository).findByCombinedCriteria(identifierString, null, normString);
  }

  @Test
  void testGetFieldsOfLaw_withSearchTermAndNorm_shouldFindByNormAndSearchTerms() {
    Pageable pageable = PageRequest.of(0, 10);
    var searchTerms = new String[] {"foo", "bar"};
    var normString = new String[] {"baz", "qux"};
    var expectedFieldsOfLaw = List.of(generateFieldOfLaw());
    when(repository.findByCombinedCriteria(null, searchTerms, normString))
        .thenReturn(expectedFieldsOfLaw);

    var page =
        service.getFieldsOfLawBySearchQuery(
            Optional.empty(), Optional.of("foo bar"), Optional.of("baz §qux "), pageable);
    assertThat(page.isEmpty()).isFalse();
    assertThat(page.getContent()).isEqualTo(expectedFieldsOfLaw);

    verify(repository).findByCombinedCriteria(null, searchTerms, normString);
  }

  @Test
  void testGetFieldsOfLawByIdentifierSearch_withoutQuery_shouldFindAllByOrderByIdentifierAsc() {
    when(repository.findAllByOrderByIdentifierAsc(any())).thenReturn(Page.empty());

    var list = service.getFieldsOfLawByIdentifierSearch(Optional.empty());
    assertThat(list).isEmpty();

    verify(repository).findAllByOrderByIdentifierAsc(any());
  }

  @Test
  void testGetFieldsOfLawByIdentifierSearch_withQuery_shouldFindAllByOrderByIdentifierAsc() {
    String searchString = "foo";
    when(repository.findByIdentifier(eq(searchString), any())).thenReturn(List.of());

    var list = service.getFieldsOfLawByIdentifierSearch(Optional.of(searchString));
    assertThat(list).isEmpty();

    verify(repository).findByIdentifier(eq(searchString), any());
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

  FieldOfLaw generateFieldOfLaw() {
    return FieldOfLaw.builder()
        .id(UUID.randomUUID())
        .hasChildren(false)
        .identifier("TS-01-01")
        .text("stext 2")
        .linkedFields(Collections.emptyList())
        .norms(List.of(new Norm("abbr1", "description")))
        .children(Collections.emptyList())
        .build();
  }
}
