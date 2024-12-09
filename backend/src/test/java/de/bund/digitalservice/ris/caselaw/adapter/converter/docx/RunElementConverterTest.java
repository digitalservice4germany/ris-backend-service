package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.domain.docx.DocxImagePart;
import de.bund.digitalservice.ris.caselaw.domain.docx.InlineImageElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.RunElement;
import jakarta.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.docx4j.dml.CTBlip;
import org.docx4j.dml.CTBlipFillProperties;
import org.docx4j.dml.CTPositiveSize2D;
import org.docx4j.dml.Graphic;
import org.docx4j.dml.GraphicData;
import org.docx4j.dml.picture.Pic;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.R;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunElementConverterTest {
  @Test
  void testConvert_withUnknownImageContentType_addPlaceholderImage() {
    R rElement = new R();
    Drawing drawing = new Drawing();
    Inline inline = getInline();
    drawing.getAnchorOrInline().add(inline);
    rElement.getContent().add(new JAXBElement<>(QName.valueOf(""), Drawing.class, drawing));

    ParagraphElement paragraphElement = new ParagraphElement();

    DocxConverter converter = mock(DocxConverter.class);
    Map<String, DocxImagePart> imageMap = new HashMap<>();
    DocxImagePart imagePart = new DocxImagePart("image/unknown", new byte[] {});
    imageMap.put("image1", imagePart);
    when(converter.getImages()).thenReturn(imageMap);

    RunElementConverter.convert(rElement, paragraphElement, converter, new ArrayList<>());

    RunElement runElement = paragraphElement.getRunElements().get(0);
    assertThat(runElement).isInstanceOf(InlineImageElement.class);
    InlineImageElement imageElement = (InlineImageElement) runElement;
    assertThat(imageElement.getContentType()).isEqualTo("image/png");
    assertThat(imageElement.getBase64Representation())
        .isEqualTo(
            "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");
  }

  @Test
  void testConvert_withEMFImage_convertToPNG() {
    R rElement = new R();
    Drawing drawing = new Drawing();
    Inline inline = getInline();
    drawing.getAnchorOrInline().add(inline);
    rElement.getContent().add(new JAXBElement<>(QName.valueOf(""), Drawing.class, drawing));

    ParagraphElement paragraphElement = new ParagraphElement();

    DocxConverter converter = mock(DocxConverter.class);
    Map<String, DocxImagePart> imageMap = new HashMap<>();
    DocxImagePart imagePart = new DocxImagePart("image/x-emf", new byte[] {});
    imageMap.put("image1", imagePart);
    when(converter.getImages()).thenReturn(imageMap);

    RunElementConverter.convert(rElement, paragraphElement, converter, new ArrayList<>());

    RunElement runElement = paragraphElement.getRunElements().get(0);
    assertThat(runElement).isInstanceOf(InlineImageElement.class);
    InlineImageElement imageElement = (InlineImageElement) runElement;
    assertThat(imageElement.getContentType()).isEqualTo("image/png");
  }

  @NotNull
  private static Inline getInline() {
    Inline inline = new Inline();
    CTPositiveSize2D size = new CTPositiveSize2D();
    // 9525 pixel are one EMU (see DocxUnitConverter)
    size.setCx(9525 * 100);
    size.setCy(9525 * 100);
    inline.setExtent(size);
    Graphic graphic = new Graphic();
    GraphicData graphicData = mock(GraphicData.class);
    Pic pic = new Pic();
    CTBlipFillProperties blibFill = new CTBlipFillProperties();
    CTBlip blib = new CTBlip();
    blib.setEmbed("image1");
    blibFill.setBlip(blib);
    pic.setBlipFill(blibFill);
    when(graphicData.getPic()).thenReturn(pic);
    graphic.setGraphicData(graphicData);
    inline.setGraphic(graphic);
    return inline;
  }
}
