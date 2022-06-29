package de.bund.digitalservice.ris.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.config.ConverterConfiguration;
import de.bund.digitalservice.ris.domain.docx.DocUnitBorderNumber;
import de.bund.digitalservice.ris.domain.docx.DocUnitParagraphElement;
import de.bund.digitalservice.ris.domain.docx.DocUnitRunTextElement;
import de.bund.digitalservice.ris.domain.docx.DocUnitTable;
import de.bund.digitalservice.ris.domain.docx.Docx2Html;
import de.bund.digitalservice.ris.utils.DocxConverter;
import de.bund.digitalservice.ris.utils.DocxConverterException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.xml.parsers.DocumentBuilderFactory;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(SpringExtension.class)
@Import({DocxConverterService.class, ConverterConfiguration.class})
class DocxConverterServiceTest {
  @Autowired DocxConverterService service;

  @MockBean S3AsyncClient client;

  @Mock WordprocessingMLPackage mlPackage;

  @Mock ResponseBytes<GetObjectResponse> responseBytes;

  @Autowired DocumentBuilderFactory documentBuilderFactory;

  @MockBean DocxConverter converter;

  @Test
  void testGetOriginalText() {
    MainDocumentPart mockedMainDocumentPart = mock(MainDocumentPart.class);
    when(mlPackage.getMainDocumentPart()).thenReturn(mockedMainDocumentPart);
    when(mlPackage.getMainDocumentPart().getXML())
        .thenReturn("<document><p><t>text</t></p></document>");

    var result = service.getOriginalText(mlPackage);

    assertEquals("text", result);
  }

  @Test
  void testGetDocxFiles() {
    ListObjectsV2Response response =
        ListObjectsV2Response.builder()
            .contents(S3Object.builder().key("test.docx").build())
            .build();
    when(client.listObjectsV2(any(ListObjectsV2Request.class)))
        .thenReturn(CompletableFuture.completedFuture(response));

    StepVerifier.create(service.getDocxFiles())
        .consumeNextWith(
            responseEntity -> {
              assertNotNull(responseEntity);
              assertEquals(1, Objects.requireNonNull(responseEntity.getBody()).size());
              assertEquals("test.docx", responseEntity.getBody().get(0));
              assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
            })
        .verifyComplete();
  }

  @Test
  void testGetHtml() {
    when(client.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
        .thenReturn(CompletableFuture.completedFuture(responseBytes));
    when(responseBytes.asInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));
    MainDocumentPart mainDocumentPart = mock(MainDocumentPart.class);
    when(mlPackage.getMainDocumentPart()).thenReturn(mainDocumentPart);
    when(mainDocumentPart.getContent()).thenReturn(List.of("1", "2", "3", "4", "5", "6"));
    when(converter.convert("1")).thenReturn(generateText("test"));
    when(converter.convert("2")).thenReturn(generateBorderNumber("1"));
    when(converter.convert("3")).thenReturn(generateText("border number 1"));
    when(converter.convert("4")).thenReturn(generateBorderNumber("2"));
    when(converter.convert("5")).thenReturn(generateText("border number 2"));
    when(converter.convert("6")).thenReturn(generateTable("table content"));

    try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
        mockStatic(WordprocessingMLPackage.class)) {
      mockedMLPackageStatic
          .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
          .thenReturn(mlPackage);

      StepVerifier.create(service.getHtml("test.docx"))
          .consumeNextWith(
              docx2Html -> {
                assertNotNull(docx2Html);
                assertEquals(
                    "<p>test</p><border-number number=\"1\"><p>border number 1</p></border-number><border-number number=\"2\"><p>border number 2</p></border-number>table content",
                    docx2Html.getContent());
              })
          .verifyComplete();
    }
  }

  @Test
  void testGetHtml_withEmptyBorderNumber() {
    when(client.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
        .thenReturn(CompletableFuture.completedFuture(responseBytes));
    when(responseBytes.asInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));
    MainDocumentPart mainDocumentPart = mock(MainDocumentPart.class);
    when(mlPackage.getMainDocumentPart()).thenReturn(mainDocumentPart);
    when(mainDocumentPart.getContent()).thenReturn(List.of("1", "2", "3", "4"));

    try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
            mockStatic(WordprocessingMLPackage.class);
        MockedStatic<DocxConverter> mockedConverter = mockStatic(DocxConverter.class)) {
      mockedMLPackageStatic
          .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
          .thenReturn(mlPackage);
      mockedConverter.when(() -> converter.convert("1")).thenReturn(generateText("test"));
      mockedConverter.when(() -> converter.convert("2")).thenReturn(generateBorderNumber("1"));
      mockedConverter.when(() -> converter.convert("3")).thenReturn(generateBorderNumber("2"));
      mockedConverter
          .when(() -> converter.convert("4"))
          .thenReturn(generateText("border number 2"));

      StepVerifier.create(service.getHtml("test.docx"))
          .consumeNextWith(
              docx2Html -> {
                assertNotNull(docx2Html);
                assertEquals(
                    "<p>test</p><border-number number=\"1\"></border-number><border-number number=\"2\"><p>border number 2</p></border-number>",
                    docx2Html.getContent());
              })
          .verifyComplete();
    }
  }

  @Test
  void testGetHtml_withInputStreamIsNull() {
    when(client.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
        .thenReturn(CompletableFuture.completedFuture(responseBytes));
    when(responseBytes.asInputStream()).thenReturn(null);

    StepVerifier.create(service.getHtml("test.docx"))
        .consumeNextWith(docx2Html -> assertEquals(Docx2Html.EMPTY, docx2Html))
        .verifyComplete();
  }

  @Test
  void testGetHtml_withLoadDocxThrowsException() {
    when(client.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
        .thenReturn(CompletableFuture.completedFuture(responseBytes));
    when(responseBytes.asInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));

    try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
        mockStatic(WordprocessingMLPackage.class)) {
      mockedMLPackageStatic
          .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
          .thenThrow(Docx4JException.class);

      StepVerifier.create(service.getHtml("test.docx"))
          .expectErrorMatches(
              throwable ->
                  throwable instanceof DocxConverterException
                      && throwable.getMessage().equals("Couldn't load docx file!"))
          .verify();
    }
  }

  private DocUnitParagraphElement generateText(String text) {
    var textElement = new DocUnitParagraphElement();
    var runTextElement = new DocUnitRunTextElement();
    runTextElement.setText(text);
    textElement.addRunElement(runTextElement);
    return textElement;
  }

  private DocUnitBorderNumber generateBorderNumber(String text) {
    DocUnitBorderNumber borderNumber = new DocUnitBorderNumber();
    borderNumber.addNumberText(text);
    return borderNumber;
  }

  private DocUnitTable generateTable(String text) {
    DocUnitTable table = new DocUnitTable();
    table.setTextContent(text);
    return table;
  }
}
