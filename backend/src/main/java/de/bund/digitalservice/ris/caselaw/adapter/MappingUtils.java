package de.bund.digitalservice.ris.caselaw.adapter;

import jakarta.xml.bind.ValidationException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class MappingUtils {

  private MappingUtils() {}

  public static String sanitizeHtmlFromString(String htmlData) {
    if (StringUtils.isNotEmpty(htmlData)) {
      Document document = Jsoup.parse(htmlData);
      Elements selectors = document.select("border-number > number");
      selectors.remove();
      return document.text();
    }

    return StringUtils.EMPTY;
  }

  public static <T, R> R nullSafeGet(T input, Function<? super T, R> call) {
    if (input == null) {
      return null;
    }

    return call.apply(input);
  }

  public static void validateNotNull(Object o, String message) throws ValidationException {
    validate(o != null, message);
  }

  public static void validate(boolean test, String message) throws ValidationException {
    if (!test) {
      throw new ValidationException(message);
    }
  }

  public static <T> void applyIfNotEmpty(List<T> collection, Consumer<List<T>> call) {
    if (collection != null && !collection.isEmpty()) {
      call.accept(collection);
    }
  }
}
