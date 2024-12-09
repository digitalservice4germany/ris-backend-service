package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import de.bund.digitalservice.ris.caselaw.domain.docx.AnchorImageElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocxImagePart;
import de.bund.digitalservice.ris.caselaw.domain.docx.ErrorRunElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.InlineImageElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.RunElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.RunTabElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.RunTextElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.UnhandledElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.UnhandledElementType;
import jakarta.xml.bind.JAXBElement;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import org.docx4j.dml.CTNonVisualDrawingProps;
import org.docx4j.dml.CTPositiveSize2D;
import org.docx4j.dml.GraphicData;
import org.docx4j.dml.wordprocessingDrawing.Anchor;
import org.docx4j.dml.wordprocessingDrawing.CTPosH;
import org.docx4j.dml.wordprocessingDrawing.CTWrapSquare;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.dml.wordprocessingDrawing.STAlignH;
import org.docx4j.dml.wordprocessingDrawing.STWrapText;
import org.docx4j.vml.CTImageData;
import org.docx4j.vml.CTShape;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.FldChar;
import org.docx4j.wml.Pict;
import org.docx4j.wml.R;
import org.docx4j.wml.R.LastRenderedPageBreak;
import org.docx4j.wml.RPr;
import org.docx4j.wml.RPrAbstract;
import org.docx4j.wml.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert the {@link org.docx4j.wml.R} to a {@link RunElement}
 *
 * @see DocxConverter
 */
public class RunElementConverter {
  private static final Logger LOGGER = LoggerFactory.getLogger(RunElementConverter.class);

  private RunElementConverter() {}

  /**
   * Convert the R element and all its children elements into {@link RunElement}.
   *
   * @param run the start R element
   * @param paragraphElement the parent paragraph element
   * @param converter the used docx converter (contains meta information of the docx file)
   * @param unhandledElements list of unhandled element in the docx file
   */
  public static void convert(
      R run,
      ParagraphElement paragraphElement,
      DocxConverter converter,
      List<UnhandledElement> unhandledElements) {
    run.getContent()
        .forEach(
            element ->
                parseRunChildrenElement(
                    element, run.getRPr(), paragraphElement, converter, unhandledElements));
  }

  private static void parseRunChildrenElement(
      Object element,
      RPr rPr,
      ParagraphElement paragraphElement,
      DocxConverter converter,
      List<UnhandledElement> unhandledElements) {

    if (element instanceof JAXBElement<?> jaxbElement) {
      var declaredType = jaxbElement.getDeclaredType();

      if (declaredType == Text.class) {
        var text = ((Text) jaxbElement.getValue()).getValue();

        if (!text.isEmpty()) {
          paragraphElement.addRunElement(
              generateRunTextElement(text, rPr, paragraphElement, converter));
        }
      } else if (declaredType == Drawing.class) {
        RunElement imageElement =
            parseDrawing(paragraphElement, (Drawing) jaxbElement.getValue(), converter);
        paragraphElement.addRunElement(imageElement);
      } else if (declaredType == R.Tab.class) {
        paragraphElement.addRunElement(new RunTabElement());
      } else if (declaredType == Pict.class) {
        parsePict(paragraphElement, (Pict) jaxbElement.getValue(), converter);
      } else if (declaredType == LastRenderedPageBreak.class) {
        // ignored because in our web presentation of the docx file there are no page breaks
      } else if (declaredType == FldChar.class) {
        // used for field calculations. don't allow calculation at the moment.
      } else if (declaredType == R.NoBreakHyphen.class) {
        RunTextElement runTextElement = new RunTextElement();
        runTextElement.setText("\u2011");
        paragraphElement.addRunElement(runTextElement);
      } else if (declaredType == R.SoftHyphen.class) {
        RunTextElement runTextElement = new RunTextElement();
        runTextElement.setText("\u00AD");
        paragraphElement.addRunElement(runTextElement);
        //         carriage return = line break
      } else if (declaredType == R.Cr.class) {
        RunTextElement runTextElement = new RunTextElement();
        runTextElement.setText("<br/>");
        paragraphElement.addRunElement(runTextElement);
      } else {
        unhandledElements.add(
            new UnhandledElement("run element", declaredType.getName(), UnhandledElementType.JAXB));
        paragraphElement.addRunElement(new ErrorRunElement(declaredType.getName()));
      }
    } else {
      unhandledElements.add(
          new UnhandledElement(
              "run element", element.getClass().getName(), UnhandledElementType.OBJECT));
    }
  }

  private static RunElement generateRunTextElement(
      String text, RPrAbstract rPr, ParagraphElement paragraph, DocxConverter converter) {
    RunTextElement runTextElement = new RunTextElement();

    runTextElement.setText(text);
    addStyle(runTextElement, rPr, paragraph, converter);

    return runTextElement;
  }

  private static RunElement parseDrawing(
      ParagraphElement parent, Drawing drawing, DocxConverter converter) {
    if (drawing.getAnchorOrInline().size() != 1) {
      throw new DocxConverterException("more than one graphic data in a drawing");
    }

    var drawingObject = drawing.getAnchorOrInline().get(0);
    if (drawingObject instanceof Inline inline) {
      return parseInlineImageElement(inline, converter);
    } else if (drawingObject instanceof Anchor anchor) {
      return parseAnchorImageElement(parent, anchor, converter);
    } else {
      LOGGER.error("unsupported drawing object");
      return new ErrorRunElement(
          "anchor drawing object? " + drawingObject.getClass().getSimpleName());
    }
  }

  private static void parsePict(ParagraphElement parent, Pict pict, DocxConverter converter) {
    pict.getAnyAndAny()
        .forEach(
            child -> {
              if (child instanceof JAXBElement<?> jaxbElement) {
                if (jaxbElement.getDeclaredType() == CTShape.class) {
                  parseCTShape(parent, (CTShape) jaxbElement.getValue(), converter);
                } else {
                  LOGGER.error(
                      "unknown jaxb child '{}' in pict element: {}",
                      jaxbElement.getName(),
                      jaxbElement.getValue());
                }
              } else if (child instanceof CTShape shape) {
                parseCTShape(parent, shape, converter);
              } else {
                LOGGER.info("unknown child in pict element: {}", child);
              }
            });
  }

  private static void parseCTShape(
      ParagraphElement parent, CTShape shape, DocxConverter converter) {
    shape
        .getEGShapeElements()
        .forEach(
            jaxbElement -> {
              if (jaxbElement.getValue() instanceof CTImageData imageData) {
                parseCTImageData(parent, imageData, shape.getStyle(), converter);
              } else {
                LOGGER.info(
                    "unknown shape child '{}': {}", jaxbElement.getName(), jaxbElement.getValue());
              }
            });
  }

  private static void parseCTImageData(
      ParagraphElement parent, CTImageData imageData, String style, DocxConverter converter) {
    DocxImagePart image = converter.getImages().get(imageData.getId());
    AnchorImageElement imageElement = new AnchorImageElement();
    imageElement.setContentType(image.contentType());
    var base64 = Base64.getEncoder().encodeToString(image.bytes());
    imageElement.setBase64Representation(base64);
    StyleConverter.getListFromString(style).forEach(imageElement::addStyle);
    parent.addRunElement(imageElement);
  }

  private static RunElement parseAnchorImageElement(
      ParagraphElement parent, Anchor anchor, DocxConverter converter) {
    if (anchor == null
        || anchor.getGraphic() == null
        || anchor.getGraphic().getGraphicData() == null) {
      throw new DocxConverterException("no graphic data");
    }

    Dimension size = parseImageSize(anchor.getExtent());

    RunElement runElement =
        parseGraphicData(
            anchor.getGraphic().getGraphicData(), size, AnchorImageElement.class, converter);

    if (runElement instanceof AnchorImageElement imageElement) {
      imageElement.setAlternateText(parseImageAlternateText(anchor.getDocPr()));
      String floating = parseFloating(anchor);
      if (floating != null) {
        if (floating.equals("error")) {
          if (anchor.getPositionH() == null || anchor.getPositionH().getAlign() == null)
            return new ErrorRunElement("anchor image with unknown alignment: null");
          return new ErrorRunElement(
              "anchor image with unknown alignment: " + anchor.getPositionH().getAlign().value());
        }
        parent.setClearfix(true);
        imageElement.setFloating(floating);
      }

      return imageElement;
    }

    return runElement;
  }

  private static RunElement parseInlineImageElement(Inline inline, DocxConverter converter) {
    if (inline == null
        || inline.getGraphic() == null
        || inline.getGraphic().getGraphicData() == null) {
      throw new DocxConverterException("no graphic data");
    }

    Dimension size = parseImageSize(inline.getExtent());

    RunElement runElement =
        parseGraphicData(
            inline.getGraphic().getGraphicData(), size, InlineImageElement.class, converter);

    if (runElement instanceof InlineImageElement imageElement) {
      imageElement.setAlternateText(parseImageAlternateText(inline.getDocPr()));

      return imageElement;
    }

    return runElement;
  }

  private static Dimension parseImageSize(CTPositiveSize2D extent) {
    if (extent == null) {
      return null;
    }

    Dimension size = new Dimension();
    if (extent.getCx() != 0) {
      size.width = DocxUnitConverter.convertEMUToPixel(extent.getCx());
    }

    if (extent.getCy() != 0) {
      size.height = DocxUnitConverter.convertEMUToPixel(extent.getCy());
    }

    return size;
  }

  private static String parseImageAlternateText(CTNonVisualDrawingProps docPr) {
    if (docPr == null) {
      return null;
    }

    String alternateText = "";
    alternateText += docPr.getName() != null ? docPr.getName() : "";
    alternateText += docPr.getDescr() != null ? docPr.getDescr() : "";

    return !alternateText.isBlank() ? alternateText : null;
  }

  private static String parseFloating(Anchor anchor) {
    CTPosH positionH = anchor.getPositionH();
    CTWrapSquare wrapSquare = anchor.getWrapSquare();

    if ((positionH == null || positionH.getAlign() == null) && (wrapSquare == null)) {
      return null;
    }

    if ((positionH != null && positionH.getAlign() == STAlignH.LEFT)
        || (wrapSquare != null && wrapSquare.getWrapText() == STWrapText.LEFT)) {
      return "left";
    } else if ((positionH != null && positionH.getAlign() == STAlignH.RIGHT)
        || (wrapSquare != null && wrapSquare.getWrapText() == STWrapText.RIGHT)) {
      return "right";
    } else {
      return "error";
    }
  }

  private static RunElement parseGraphicData(
      GraphicData graphicData,
      Dimension size,
      Class<? extends InlineImageElement> clazz,
      DocxConverter converter) {

    InlineImageElement imageElement = new InlineImageElement();
    try {
      imageElement = clazz.getDeclaredConstructor().newInstance();
    } catch (InstantiationException
        | NoSuchMethodException
        | IllegalAccessException
        | InvocationTargetException e) {
      LOGGER.error("Couldn't instantiate image class: {}", clazz.getSimpleName(), e);
    }

    var pic = graphicData.getPic();

    if (pic != null) {
      var embed = pic.getBlipFill().getBlip().getEmbed();
      var image = converter.getImages().get(embed);

      if (image != null) {
        addImageContent(imageElement, image, size);
      }
    } else {
      LOGGER.error("no picture");
      List<Object> anyGraphicElement = graphicData.getAny();
      StringBuilder stringBuilder = new StringBuilder();
      anyGraphicElement.forEach(
          el -> stringBuilder.append(el.getClass().getSimpleName()).append(", "));
      return new ErrorRunElement("unknown graphic element: " + stringBuilder);
    }

    return imageElement;
  }

  private static void addImageContent(
      InlineImageElement element, DocxImagePart image, Dimension size) {
    element.setContentType(image.contentType());

    byte[] bytes = image.bytes();
    if (image.contentType().equals("image/x-emf")) {
      bytes = EMF2PNGConverter.convertEMF2PNG(bytes, size);
      element.setContentType("image/png");
    }

    if (image.contentType().equals("image/unknown")) {
      element.setContentType("image/png");
      // base64 representation of the material design icon for unknown files
      element.setBase64Representation(
          "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");
    } else {
      element.setBase64Representation(Base64.getEncoder().encodeToString(bytes));
    }

    element.setSize(size);
  }

  private static void addStyle(
      RunTextElement textElement,
      RPrAbstract rPr,
      ParagraphElement paragraph,
      DocxConverter converter) {
    if (rPr != null) {
      RunElementStyleAdapter.addStyles(textElement, rPr);
    } else {
      if (paragraph != null && paragraph.getStyleReference() != null) {
        var style = converter.getStyles().get(paragraph.getStyleReference());

        if (style != null && style.getRPr() != null) {
          RunElementStyleAdapter.addStyles(textElement, style.getRPr());
        }
      }
    }
  }

  public static String parseTextFromRun(R r) {
    return r.getContent().stream()
        .filter(part -> part instanceof JAXBElement<?>)
        .map(part -> (JAXBElement<?>) part)
        .filter(el -> el.getDeclaredType() == Text.class)
        .map(el -> (Text) el.getValue())
        .map(Text::getValue)
        .collect(Collectors.joining());
  }
}
