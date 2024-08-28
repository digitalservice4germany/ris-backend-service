package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.adapter.converter.docx.DocumentConverterException;
import de.bund.digitalservice.ris.caselaw.domain.docx.Docx2Html;
import java.util.List;
import java.util.UUID;

public interface ConverterService {
  Docx2Html getConvertedObject(String fileName);

  /**
   * Get a list of converted document elements {@link ConvertedDocumentElement} for a original
   * document.<br>
   * The converted document elements {@link ConvertedDocumentElement} are generate and persist with
   * the first request.
   *
   * @param documentationUnitId id for the related documentation unit
   * @param fileName path to the original document
   * @return a list of converted document elements {@link ConvertedDocumentElement}
   * @throws DocumentConverterException thrown when no xml string representation of a converted
   *     document element can't be generated
   */
  List<ConvertedDocumentElement> getConvertedObjectList(UUID documentationUnitId, String fileName)
      throws DocumentConverterException;

  List<ConvertedDocumentElement> removeBorderNumbers(UUID documentationUnitId, String fileName);

  List<ConvertedDocumentElement> addBorderNumbers(
      UUID documentationUnitId, String fileName, UUID startId);

  List<ConvertedDocumentElement> removeBorderNumber(
      UUID documentationUnitId, String fileName, UUID startId);

  List<ConvertedDocumentElement> joinBorderNumbers(
      UUID documentationUnitId, String fileName, UUID startId);

  List<ConvertedDocumentElement> getReconvertObjectList(UUID id, String s3Path);
}
