package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class DocxUnitConverterTest {
  @Test
  void testConvertTwipToPixel() {
    var result = DocxUnitConverter.convertTwipToPixel(1440);

    assertThat(result).isEqualTo(96);
  }

  @Test
  void testConvertEMUToPixel() {
    var result = DocxUnitConverter.convertEMUToPixel(914400);

    assertThat(result).isEqualTo(96);
  }

  @Test
  void testConvertPointToPixel() {
    assertThat(DocxUnitConverter.convertPointToPixel(BigInteger.valueOf(48))).isEqualTo(6);

    assertThat(DocxUnitConverter.convertPointToPixel(BigInteger.valueOf(2))).isEqualTo(0.25f);
  }
}
