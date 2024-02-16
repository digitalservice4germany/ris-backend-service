package de.bund.digitalservice.ris.caselaw.domain;

import static de.bund.digitalservice.ris.caselaw.domain.DateUtil.getYearAsYY;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.validation.ConstraintViolationException;
import java.time.Year;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class DocumentNumberFormatterTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "KORE7****YYYY",
        "KVRE*****YY41",
        "WBRE61*****YY",
        "BSGRE1*****YY",
        "DSRE*****YYYY",
        "XXRE*******YY"
      })
  void shouldCreateCorrectFormat(String pattern) {
    Year currentYear = DateUtil.getCurrentYear();
    var prefix = pattern.substring(0, pattern.indexOf('*'));
    DocumentNumberFormatter documentNumberFormatter =
        DocumentNumberFormatter.builder().pattern(pattern).year(currentYear).docNumber(2).build();

    var result = documentNumberFormatter.toString();

    Assertions.assertEquals(13, result.length(), "Format must contain 13");
    Assertions.assertTrue(result.startsWith(prefix), " Prefix is not contained in format");
    Assertions.assertFalse(result.contains("*"), "Doc number was not parsed correctly");
    Assertions.assertTrue(result.contains(getYearAsYY()), "Year is not included in format");
  }

  @Test
  void shouldCreateShortYearFormat() {
    var currentYear = DateUtil.getCurrentYear();
    String format = "BSGRE1****YY";
    DocumentNumberFormatter documentNumberFormatter =
        DocumentNumberFormatter.builder().pattern(format).year(currentYear).docNumber(1).build();

    var result = documentNumberFormatter.toString();

    Assertions.assertTrue(result.endsWith(getYearAsYY(currentYear)));
  }

  @Test
  void shouldThrownExceptionWhenDocNumberOverflow() {
    String pattern = "BSGRE1****YY";

    DocumentNumberFormatter documentNumberFormatter =
        DocumentNumberFormatter.builder()
            .pattern(pattern)
            .year(DateUtil.getCurrentYear())
            .docNumber(10000)
            .build();

    Exception exception =
        assertThrows(DocumentNumberPatternException.class, documentNumberFormatter::toString);

    String expectedMessage = "Doc number is bigger than the * amount";
    String actualMessage = exception.getMessage();

    Assertions.assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test // TODO: Fix lombok annotation validator.
  @Disabled("Lombok annotation validation doesnot work.")
  void shouldThrownExceptionWhenDocNumberWithNegativeDocNumber() {
    String pattern = "BSGRE1****YY";
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          DocumentNumberFormatter.builder()
              .pattern(pattern)
              .year(DateUtil.getCurrentYear())
              .docNumber(-4) // Passing a negative docNumber
              .build();
        });
  }
}
