package de.bund.digitalservice.ris.caselaw.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeUserGroupRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeUserGroupDTO;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentationOfficeUserGroupTransformer;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import({
  DatabaseDocumentationOfficeUserGroupService.class,
  DatabaseDocumentationOfficeRepository.class,
  DatabaseDocumentationOfficeUserGroupRepository.class,
})
class DatabaseDocumentationOfficeUserGroupServiceTest {

  private final DocumentationOfficeConfigUserGroup dsInternalGroupConfig =
      DocumentationOfficeConfigUserGroup.builder()
          .docOfficeAbbreviation("DS")
          .userGroupPathName("/DS")
          .isInternal(true)
          .build();
  private final DocumentationOfficeConfigUserGroup dsExternalGroupConfig =
      DocumentationOfficeConfigUserGroup.builder()
          .docOfficeAbbreviation("DS")
          .userGroupPathName("/DS")
          .isInternal(false)
          .build();

  private final DocumentationOfficeDTO dsDocOffice =
      DocumentationOfficeDTO.builder().abbreviation("DS").id(UUID.randomUUID()).build();
  private final DocumentationOfficeDTO bghDocOffice =
      DocumentationOfficeDTO.builder().abbreviation("BGH").id(UUID.randomUUID()).build();

  private final DocumentationOfficeUserGroupDTO dsInternalGroupDTO =
      DocumentationOfficeUserGroupDTO.builder()
          .documentationOffice(dsDocOffice)
          .isInternal(true)
          .userGroupPathName("/DS")
          .build();
  private final DocumentationOfficeUserGroupDTO dsExternalGroupDTO =
      DocumentationOfficeUserGroupDTO.builder()
          .documentationOffice(dsDocOffice)
          .isInternal(false)
          .userGroupPathName("/DS")
          .build();
  private final DocumentationOfficeUserGroupDTO bghInternalGroupDTO =
      DocumentationOfficeUserGroupDTO.builder()
          .documentationOffice(bghDocOffice)
          .isInternal(true)
          .userGroupPathName("/BGH")
          .build();

  private DatabaseDocumentationOfficeUserGroupService service;

  @MockBean private DatabaseDocumentationOfficeRepository officeRepository;
  @MockBean private DatabaseDocumentationOfficeUserGroupRepository groupRepository;

  @Test
  void shouldHandleEmptyConfig() {
    doReturn(List.of()).when(this.groupRepository).findAll();
    doReturn(List.of()).when(this.officeRepository).findAll();
    List<DocumentationOfficeConfigUserGroup> configuredGroups = List.of();
    this.service =
        new DatabaseDocumentationOfficeUserGroupService(
            groupRepository, officeRepository, configuredGroups);

    this.service.onApplicationEvent(null);

    verify(this.groupRepository, never()).saveAll(any());
    verify(this.groupRepository, never()).deleteAll(any());
  }

  @Test
  void shouldThrowOnNonExistentDocOffice() {
    doReturn(List.of()).when(this.groupRepository).findAll();
    doReturn(List.of()).when(this.officeRepository).findAll();
    List<DocumentationOfficeConfigUserGroup> configuredGroups = List.of(dsInternalGroupConfig);
    this.service =
        new DatabaseDocumentationOfficeUserGroupService(
            groupRepository, officeRepository, configuredGroups);

    assertThatThrownBy(() -> this.service.onApplicationEvent(null));

    verify(this.groupRepository, never()).saveAll(any());
    verify(this.groupRepository, never()).deleteAll(any());
  }

  @Test
  void shouldCreateNewGroup() {
    doReturn(List.of()).when(this.groupRepository).findAll();
    doReturn(List.of(dsDocOffice)).when(this.officeRepository).findAll();
    List<DocumentationOfficeConfigUserGroup> configuredGroups = List.of(dsInternalGroupConfig);
    this.service =
        new DatabaseDocumentationOfficeUserGroupService(
            groupRepository, officeRepository, configuredGroups);

    this.service.onApplicationEvent(null);

    verify(this.groupRepository, times(1)).saveAll(List.of(dsInternalGroupDTO));
    verify(this.groupRepository, never()).deleteAll(any());
  }

  @Test
  void shouldDeleteObsoleteGroup() {
    doReturn(List.of(dsInternalGroupDTO)).when(this.groupRepository).findAll();
    doReturn(List.of(dsDocOffice)).when(this.officeRepository).findAll();
    List<DocumentationOfficeConfigUserGroup> configuredGroups = List.of();
    this.service =
        new DatabaseDocumentationOfficeUserGroupService(
            groupRepository, officeRepository, configuredGroups);

    this.service.onApplicationEvent(null);

    verify(this.groupRepository, times(1)).deleteAll(List.of(dsInternalGroupDTO));
    verify(this.groupRepository, never()).saveAll(any());
  }

  @Test
  void shouldRecreateGroupWithNewConfig_InternalFlagChanged() {
    doReturn(List.of(dsInternalGroupDTO)).when(this.groupRepository).findAll();
    doReturn(List.of(dsDocOffice)).when(this.officeRepository).findAll();
    List<DocumentationOfficeConfigUserGroup> configuredGroups = List.of(dsExternalGroupConfig);
    this.service =
        new DatabaseDocumentationOfficeUserGroupService(
            groupRepository, officeRepository, configuredGroups);

    this.service.onApplicationEvent(null);

    verify(this.groupRepository, times(1)).deleteAll(List.of(dsInternalGroupDTO));
    verify(this.groupRepository, times(1)).saveAll(List.of(dsExternalGroupDTO));
  }

  @Test
  void shouldRecreateGroupWithNewConfig_DocOfficeChanged() {
    doReturn(List.of(bghInternalGroupDTO)).when(this.groupRepository).findAll();
    doReturn(List.of(dsDocOffice, bghDocOffice)).when(this.officeRepository).findAll();
    List<DocumentationOfficeConfigUserGroup> configuredGroups = List.of(dsInternalGroupConfig);
    this.service =
        new DatabaseDocumentationOfficeUserGroupService(
            groupRepository, officeRepository, configuredGroups);

    this.service.onApplicationEvent(null);

    verify(this.groupRepository, times(1)).deleteAll(List.of(bghInternalGroupDTO));
    verify(this.groupRepository, times(1)).saveAll(List.of(dsInternalGroupDTO));
  }

  @Test
  void shouldIdleWhenDatabaseGroupsMatchConfiguration() {
    doReturn(List.of(dsInternalGroupDTO, dsExternalGroupDTO)).when(this.groupRepository).findAll();
    doReturn(List.of(bghDocOffice, dsDocOffice)).when(this.officeRepository).findAll();
    List<DocumentationOfficeConfigUserGroup> configuredGroups =
        List.of(dsExternalGroupConfig, dsInternalGroupConfig);
    this.service =
        new DatabaseDocumentationOfficeUserGroupService(
            groupRepository, officeRepository, configuredGroups);

    this.service.onApplicationEvent(null);

    verify(this.groupRepository, never()).saveAll(any());
    verify(this.groupRepository, never()).deleteAll(any());
    var expectedGroups =
        Stream.of(dsInternalGroupDTO, dsExternalGroupDTO)
            .map(DocumentationOfficeUserGroupTransformer::transformToDomain)
            .toList();
    assertThat(this.service.getAllUserGroups()).isEqualTo(expectedGroups);
  }
}
