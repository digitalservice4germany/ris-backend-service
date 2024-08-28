package de.bund.digitalservice.ris.caselaw.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bund.digitalservice.ris.caselaw.adapter.converter.docx.DocumentConverterException;
import de.bund.digitalservice.ris.caselaw.adapter.converter.docx.DocumentationUnitDocxListUtils;
import de.bund.digitalservice.ris.caselaw.adapter.converter.docx.DocxConverter;
import de.bund.digitalservice.ris.caselaw.adapter.converter.docx.FooterConverter;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.ConvertedDocumentElementDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseConvertedDocumentElementRepository;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.ConvertedDocumentElementTransformer;
import de.bund.digitalservice.ris.caselaw.domain.ConvertedDocumentElement;
import de.bund.digitalservice.ris.caselaw.domain.ConverterService;
import de.bund.digitalservice.ris.caselaw.domain.docx.BorderNumber;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocumentationUnitDocx;
import de.bund.digitalservice.ris.caselaw.domain.docx.Docx2Html;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocxImagePart;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocxMetadataProperty;
import de.bund.digitalservice.ris.caselaw.domain.docx.ECLIElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.FooterElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.MetadataProperty;
import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.model.listnumbering.ListNumberingDefinition;
import org.docx4j.model.structure.HeaderFooterPolicy;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.DocPropsCustomPart;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.ImageJpegPart;
import org.docx4j.openpackaging.parts.WordprocessingML.ImagePngPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MetafileEmfPart;
import org.docx4j.wml.Style;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
@Slf4j
public class DocxConverterService implements ConverterService {
  private final S3Client client;
  private final DocumentBuilderFactory documentBuilderFactory;
  private final DocxConverter converter;
  private final DatabaseConvertedDocumentElementRepository convertedDocumentElementRepository;
  private final ObjectMapper objectMapper;

  @Value("${otc.obs.bucket-name}")
  private String bucketName;

  public DocxConverterService(
      S3Client client,
      DocumentBuilderFactory documentBuilderFactory,
      DocxConverter converter,
      DatabaseConvertedDocumentElementRepository convertedDocumentElementRepository,
      ObjectMapper objectMapper) {
    this.client = client;
    this.documentBuilderFactory = documentBuilderFactory;
    this.converter = converter;
    this.convertedDocumentElementRepository = convertedDocumentElementRepository;
    this.objectMapper = objectMapper;
  }

  public String getOriginalText(WordprocessingMLPackage mlPackage) {
    if (mlPackage == null) {
      return "<no word file selected>";
    }

    String originalText;
    originalText = mlPackage.getMainDocumentPart().getXML();

    try {
      DocumentBuilder dBuilder = documentBuilderFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(new InputSource(new StringReader(originalText)));
      XPath xPath = XPathFactory.newInstance().newXPath();
      String expression = "/document//t";
      NodeList nodeList =
          (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

      StringBuilder sb = new StringBuilder();
      for (var i = 0; i < nodeList.getLength(); i++) {
        sb.append(nodeList.item(i).getTextContent());
      }
      originalText = sb.toString();
    } catch (IOException
        | SAXException
        | XPathExpressionException
        | ParserConfigurationException e) {
      throw new DocumentConverterException("Couldn't read all text elements of docx xml!", e);
    }

    return originalText;
  }

  /**
   * Convert docx file to a object with the html content of the word file and some metadata
   * extracted of the docx file.
   *
   * @param fileName name of the file in the bucket
   * @return the generated object with html content and metadata, if the file name is null a empty
   *     mono is returned
   */
  public Docx2Html getConvertedObject(String fileName) {
    if (fileName == null) {
      return null;
    }

    List<DocumentationUnitDocx> packedList = convertDocx(fileName);

    String content = null;
    if (!packedList.isEmpty()) {
      content =
          packedList.stream()
              .map(DocumentationUnitDocx::toHtmlString)
              .collect(Collectors.joining());
    }

    List<String> ecliList =
        packedList.stream()
            .filter(ECLIElement.class::isInstance)
            .map(ECLIElement.class::cast)
            .map(ECLIElement::getText)
            .toList();

    Map<DocxMetadataProperty, String> properties =
        packedList.stream()
            .filter(MetadataProperty.class::isInstance)
            .map(MetadataProperty.class::cast)
            .collect(Collectors.toMap(MetadataProperty::getKey, MetadataProperty::getValue));

    return new Docx2Html(content, ecliList, properties);
  }

  @Override
  public List<ConvertedDocumentElement> getConvertedObjectList(
      UUID documentationUnitId, String fileName) throws DocumentConverterException {

    if (fileName == null) {
      return Collections.emptyList();
    }

    List<ConvertedDocumentElementDTO> convertedDocumentElements =
        convertedDocumentElementRepository.findAllByDocumentationUnitIdOrderByRank(
            documentationUnitId);

    if (convertedDocumentElements.isEmpty()) {
      List<DocumentationUnitDocx> elements = convertDocx(fileName);
      convertedDocumentElements = convertDocument(documentationUnitId, elements);
    }

    return convertedDocumentElements.stream()
        .map(dto -> ConvertedDocumentElementTransformer.transformDTO(objectMapper, dto))
        .toList();
  }

  @Override
  @Transactional
  public List<ConvertedDocumentElement> getReconvertObjectList(
      UUID documentationUnitId, String fileName) throws DocumentConverterException {

    if (fileName == null) {
      return Collections.emptyList();
    }

    convertedDocumentElementRepository.deleteAllByDocumentationUnitId(documentationUnitId);

    List<DocumentationUnitDocx> elements = convertDocx(fileName);

    return convertDocument(documentationUnitId, elements).stream()
        .map(dto -> ConvertedDocumentElementTransformer.transformDTO(objectMapper, dto))
        .toList();
  }

  @Override
  @Transactional
  public List<ConvertedDocumentElement> removeBorderNumbers(
      UUID documentationUnitId, String fileName) {
    List<ConvertedDocumentElementDTO> convertedDocumentElements =
        convertedDocumentElementRepository.findAllByDocumentationUnitIdOrderByRank(
            documentationUnitId);

    List<DocumentationUnitDocx> elementsWithoutBorderNumbers = new ArrayList<>();
    convertedDocumentElements.forEach(
        convertedDocumentElementDTO -> {
          DocumentationUnitDocx contentObject =
              ConvertedDocumentElementTransformer.getContentObject(
                  objectMapper, convertedDocumentElementDTO);
          if (contentObject instanceof BorderNumber borderNumber) {
            elementsWithoutBorderNumbers.addAll(borderNumber.getChildren());
          } else {
            elementsWithoutBorderNumbers.add(contentObject);
          }
        });

    convertedDocumentElementRepository.deleteAllByDocumentationUnitId(documentationUnitId);
    return convertDocument(documentationUnitId, elementsWithoutBorderNumbers).stream()
        .map(dto -> ConvertedDocumentElementTransformer.transformDTO(objectMapper, dto))
        .toList();
  }

  @Override
  @Transactional
  public List<ConvertedDocumentElement> addBorderNumbers(
      UUID documentationUnitId, String fileName, UUID startId) {
    List<ConvertedDocumentElementDTO> convertedDocumentElements =
        convertedDocumentElementRepository.findAllByDocumentationUnitIdOrderByRank(
            documentationUnitId);

    AtomicLong borderNumberValue = new AtomicLong(1);
    AtomicBoolean startBorderNumbers = new AtomicBoolean(true);
    if (startId != null) {
      startBorderNumbers.set(false);
    }
    AtomicReference<BorderNumber> lastBorderNumber = new AtomicReference<>(null);

    List<DocumentationUnitDocx> newConvertedElementList = new ArrayList<>();
    convertedDocumentElements.forEach(
        convertedDocumentElementDTO -> {
          DocumentationUnitDocx contentObject =
              ConvertedDocumentElementTransformer.getContentObject(
                  objectMapper, convertedDocumentElementDTO);
          if (convertedDocumentElementDTO.getId().equals(startId)) {
            startBorderNumbers.set(true);
          }
          if (startBorderNumbers.get()
              && contentObject instanceof ParagraphElement paragraphElement) {
            if (paragraphElement.getText().isBlank() && lastBorderNumber.get() != null) {
              lastBorderNumber.get().addChild(paragraphElement);
            } else {
              BorderNumber borderNumber = new BorderNumber();
              borderNumber.addNumberText(String.valueOf(borderNumberValue.getAndIncrement()));
              borderNumber.addChild(paragraphElement);
              lastBorderNumber.set(borderNumber);
              newConvertedElementList.add(borderNumber);
            }
          } else {
            newConvertedElementList.add(contentObject);
          }
        });

    convertedDocumentElementRepository.deleteAllByDocumentationUnitId(documentationUnitId);
    return convertDocument(documentationUnitId, newConvertedElementList).stream()
        .map(dto -> ConvertedDocumentElementTransformer.transformDTO(objectMapper, dto))
        .toList();
  }

  @Override
  @Transactional
  public List<ConvertedDocumentElement> removeBorderNumber(
      UUID documentationUnitId, String fileName, UUID elementId) {
    List<ConvertedDocumentElementDTO> convertedDocumentElements =
        convertedDocumentElementRepository.findAllByDocumentationUnitIdOrderByRank(
            documentationUnitId);

    AtomicLong borderNumberValue = new AtomicLong(1);

    List<DocumentationUnitDocx> newConvertedElementList = new ArrayList<>();
    convertedDocumentElements.forEach(
        convertedDocumentElementDTO -> {
          DocumentationUnitDocx contentObject =
              ConvertedDocumentElementTransformer.getContentObject(
                  objectMapper, convertedDocumentElementDTO);
          if (contentObject instanceof BorderNumber borderNumber) {
            if (convertedDocumentElementDTO.getId().equals(elementId)) {
              newConvertedElementList.addAll(borderNumber.getChildren());
            } else {
              borderNumber.setNumberText(String.valueOf(borderNumberValue.getAndIncrement()));
              newConvertedElementList.add(borderNumber);
            }
          } else {
            newConvertedElementList.add(contentObject);
          }
        });

    convertedDocumentElementRepository.deleteAllByDocumentationUnitId(documentationUnitId);
    return convertDocument(documentationUnitId, newConvertedElementList).stream()
        .map(dto -> ConvertedDocumentElementTransformer.transformDTO(objectMapper, dto))
        .toList();
  }

  @Override
  @Transactional
  public List<ConvertedDocumentElement> joinBorderNumbers(
      UUID documentationUnitId, String fileName, UUID elementId) {
    List<ConvertedDocumentElementDTO> convertedDocumentElements =
        convertedDocumentElementRepository.findAllByDocumentationUnitIdOrderByRank(
            documentationUnitId);

    AtomicLong borderNumberValue = new AtomicLong(1);
    AtomicReference<BorderNumber> lastBorderNumber = new AtomicReference<>(null);

    List<DocumentationUnitDocx> newConvertedElementList = new ArrayList<>();
    convertedDocumentElements.forEach(
        convertedDocumentElementDTO -> {
          DocumentationUnitDocx contentObject =
              ConvertedDocumentElementTransformer.getContentObject(
                  objectMapper, convertedDocumentElementDTO);
          if (contentObject instanceof BorderNumber borderNumber) {
            if (convertedDocumentElementDTO.getId().equals(elementId)) {
              if (lastBorderNumber.get() != null) {
                lastBorderNumber.get().getChildren().addAll(borderNumber.getChildren());
              } else {
                newConvertedElementList.addAll(borderNumber.getChildren());
              }
            } else {
              borderNumber.setNumberText(String.valueOf(borderNumberValue.getAndIncrement()));
              newConvertedElementList.add(borderNumber);
              lastBorderNumber.set(borderNumber);
            }
          } else {
            newConvertedElementList.add(contentObject);
          }
        });

    convertedDocumentElementRepository.deleteAllByDocumentationUnitId(documentationUnitId);
    return convertDocument(documentationUnitId, newConvertedElementList).stream()
        .map(dto -> ConvertedDocumentElementTransformer.transformDTO(objectMapper, dto))
        .toList();
  }

  private List<ConvertedDocumentElementDTO> convertDocument(
      UUID documentationUnitId, List<DocumentationUnitDocx> elements)
      throws DocumentConverterException {

    AtomicLong rank = new AtomicLong(1L);
    List<ConvertedDocumentElementDTO> dtoList = new ArrayList<>();

    elements.forEach(
        documentationUnitDocx -> {
          ConvertedDocumentElementDTO convertedDocumentElementDTO;
          try {
            convertedDocumentElementDTO =
                ConvertedDocumentElementDTO.builder()
                    .documentationUnitId(documentationUnitId)
                    .content(objectMapper.writeValueAsString(documentationUnitDocx))
                    .rank(rank.getAndIncrement())
                    .build();
          } catch (JsonProcessingException e) {
            throw new DocumentConverterException(
                "Couldn't persist "
                    + rank.get()
                    + ". element "
                    + "for documentation unit '"
                    + documentationUnitId
                    + "'",
                e);
          }
          dtoList.add(convertedDocumentElementDTO);
        });

    return convertedDocumentElementRepository.saveAll(dtoList);
  }

  private List<DocumentationUnitDocx> convertDocx(String fileName) {
    if (fileName == null) {
      return Collections.emptyList();
    }

    GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(fileName).build();

    ResponseBytes<GetObjectResponse> response =
        client.getObject(request, ResponseTransformer.toBytes());

    List<DocumentationUnitDocx> documentationUnitDocxList;
    documentationUnitDocxList = parseAsDocumentationUnitDocxList(response.asInputStream());
    return DocumentationUnitDocxListUtils.packList(documentationUnitDocxList);
  }

  /**
   * Convert the content file (docx) into a list of DocumentationUnitDocx elements. Read the styles,
   * images, footers and numbering definitions from the docx file.
   *
   * @param inputStream input stream of the content file
   * @return list of DocumentationUnitDocx elements
   */
  public List<DocumentationUnitDocx> parseAsDocumentationUnitDocxList(InputStream inputStream) {
    if (inputStream == null) {
      return Collections.emptyList();
    }

    WordprocessingMLPackage mlPackage;
    try {
      mlPackage = WordprocessingMLPackage.load(inputStream);
    } catch (Docx4JException e) {
      throw new DocumentConverterException("Couldn't load docx file!", e);
    }

    converter.setStyles(readStyles(mlPackage));
    converter.setImages(readImages(mlPackage));
    converter.setFooters(readFooters(mlPackage, converter));
    converter.setListNumberingDefinitions(readListNumberingDefinitions(mlPackage));

    List<DocumentationUnitDocx> documentationUnitDocxList =
        mlPackage.getMainDocumentPart().getContent().stream()
            .map(converter::convert)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    Set<FooterElement> footerElements = parseFooterAndIdentifyECLI();
    documentationUnitDocxList.addAll(
        0, footerElements.stream().filter(ECLIElement.class::isInstance).toList());
    documentationUnitDocxList.addAll(
        footerElements.stream()
            .filter(footerElement -> !(footerElement instanceof ECLIElement))
            .toList());

    documentationUnitDocxList.addAll(readDocumentProperties(mlPackage));

    DocumentationUnitDocxListUtils.postProcessBorderNumbers(documentationUnitDocxList);

    return documentationUnitDocxList;
  }

  private List<MetadataProperty> readDocumentProperties(WordprocessingMLPackage mlPackage) {
    DocPropsCustomPart customProps = mlPackage.getDocPropsCustomPart();
    List<MetadataProperty> props = new ArrayList<>();

    if (customProps == null || customProps.getJaxbElement() == null) {
      return props;
    }
    for (var prop : customProps.getJaxbElement().getProperty()) {
      DocxMetadataProperty field = DocxMetadataProperty.fromKey(prop.getName());
      if (prop.getLpwstr() != null && field != null) {
        props.add(new MetadataProperty(field, prop.getLpwstr()));
      }
    }

    return props;
  }

  private Set<FooterElement> parseFooterAndIdentifyECLI() {
    Set<FooterElement> footerElements = new HashSet<>();

    // Check if footers are null
    if (converter.getFooters() == null) {
      return footerElements;
    }
    converter
        .getFooters()
        .forEach(
            footer -> {
              if (footer == null || footer.getText() == null) {
                return;
              }

              if (isECLI(footer.getText())) {
                footerElements.add(new ECLIElement(footer));
              } else {
                footerElements.add(new FooterElement(footer));
              }
            });

    return footerElements;
  }

  /**
   * Check if the ECLI has the right format
   *
   * @see <a
   *     href="https://e-justice.europa.eu/175/EN/european_case_law_identifier_ecli?init=true">European
   *     Case Law Identifier</a>
   * @param ecli ECLI to check
   * @return true if the input value is equals to the format specification, else false
   */
  private boolean isECLI(String ecli) {
    if (!ecli.startsWith("ECLI")) {
      return false;
    }

    String[] parts = ecli.split(":");

    if (parts.length != 5) {
      return false;
    }

    if (!parts[0].equals("ECLI")) {
      return false;
    }

    if (parts[2].length() > 7) {
      return false;
    }

    if (parts[3].length() != 4) {
      return false;
    }

    try {
      Integer.parseInt(parts[3]);
    } catch (NumberFormatException ex) {
      return false;
    }

    Pattern pattern = Pattern.compile("[\\w.]{1,25}");
    Matcher matcher = pattern.matcher(parts[4]);

    return matcher.matches();
  }

  private List<ParagraphElement> readFooters(
      WordprocessingMLPackage mlPackage, DocxConverter converter) {
    if (mlPackage == null
        || mlPackage.getDocumentModel() == null
        || mlPackage.getDocumentModel().getSections() == null) {
      return Collections.emptyList();
    }

    List<ParagraphElement> footers = new ArrayList<>();

    mlPackage
        .getDocumentModel()
        .getSections()
        .forEach(
            section -> {
              HeaderFooterPolicy headerFooterPolicy = section.getHeaderFooterPolicy();

              if (headerFooterPolicy.getDefaultFooter() != null) {
                footers.add(
                    FooterConverter.convert(
                        headerFooterPolicy.getDefaultFooter().getContent(), converter));
              }

              if (headerFooterPolicy.getFirstFooter() != null) {
                footers.add(
                    FooterConverter.convert(
                        headerFooterPolicy.getFirstFooter().getContent(), converter));
              }

              if (headerFooterPolicy.getEvenFooter() != null) {
                footers.add(
                    FooterConverter.convert(
                        headerFooterPolicy.getEvenFooter().getContent(), converter));
              }
            });

    return footers;
  }

  private Map<String, ListNumberingDefinition> readListNumberingDefinitions(
      WordprocessingMLPackage mlPackage) {
    if (mlPackage == null
        || mlPackage.getMainDocumentPart() == null
        || mlPackage.getMainDocumentPart().getNumberingDefinitionsPart() == null) {
      return Collections.emptyMap();
    }

    return mlPackage
        .getMainDocumentPart()
        .getNumberingDefinitionsPart()
        .getInstanceListDefinitions();
  }

  private Map<String, Style> readStyles(WordprocessingMLPackage mlPackage) {
    if (mlPackage == null
        || mlPackage.getMainDocumentPart() == null
        || mlPackage.getMainDocumentPart().getStyleDefinitionsPart() == null) {
      return Collections.emptyMap();
    }

    return mlPackage
        .getMainDocumentPart()
        .getStyleDefinitionsPart()
        .getJaxbElement()
        .getStyle()
        .stream()
        .collect(Collectors.toMap(Style::getStyleId, Function.identity()));
  }

  private Map<String, DocxImagePart> readImages(WordprocessingMLPackage mlPackage) {
    if (mlPackage == null
        || mlPackage.getParts() == null
        || mlPackage.getParts().getParts() == null) {
      return Collections.emptyMap();
    }

    Map<String, DocxImagePart> images = new HashMap<>();

    mlPackage
        .getParts()
        .getParts()
        .values()
        .forEach(
            part -> {
              if (part instanceof ImageJpegPart jpegPart) {
                part.getSourceRelationships()
                    .forEach(
                        relationship ->
                            images.put(
                                relationship.getId(),
                                new DocxImagePart(jpegPart.getContentType(), jpegPart.getBytes())));
              } else if (part instanceof MetafileEmfPart emfPart) {
                part.getSourceRelationships()
                    .forEach(
                        relationship ->
                            images.put(
                                relationship.getId(),
                                new DocxImagePart(emfPart.getContentType(), emfPart.getBytes())));
              } else if (part instanceof ImagePngPart pngPart) {
                part.getSourceRelationships()
                    .forEach(
                        relationship ->
                            images.put(
                                relationship.getId(),
                                new DocxImagePart(pngPart.getContentType(), pngPart.getBytes())));
              } else if (part instanceof BinaryPartAbstractImage imagePart) {
                throw new DocumentConverterException(
                    "unknown image file format: " + imagePart.getClass().getName());
              }
            });

    return images;
  }
}
