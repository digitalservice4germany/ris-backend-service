package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.domain.DependentLiteratureCitationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DependentLiteratureCitationTypeConverter
    implements AttributeConverter<DependentLiteratureCitationType, String> {

  @Override
  public String convertToDatabaseColumn(DependentLiteratureCitationType attribute) {
    return (attribute == null) ? null : attribute.getValue(); // Store "passive" or "active"
  }

  @Override
  public DependentLiteratureCitationType convertToEntityAttribute(String dbData) {
    return (dbData == null) ? null : DependentLiteratureCitationType.of(dbData);
  }
}
