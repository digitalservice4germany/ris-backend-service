package de.bund.digitalservice.ris.caselaw.adapter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenSearchAppender extends AppenderBase<ILoggingEvent> {
  private final OkHttpClient httpClient = new OkHttpClient();
  private final ObjectMapper objectMapper;

  public OpenSearchAppender() {
    objectMapper = new ObjectMapper();
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addDeserializer(
        LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE_TIME));
    objectMapper.registerModule(javaTimeModule);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    LogJson json = new LogJson();
    json.dateTime =
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli(eventObject.getTimeStamp()), TimeZone.getDefault().toZoneId());
    json.level = eventObject.getLevel().toString();
    json.message = eventObject.getMessage();
    if (eventObject.getThrowableProxy() != null) {
      json.exceptionStackTrace =
          eventObject.getThrowableProxy().getMessage() + System.lineSeparator();
      json.exceptionStackTrace +=
          Arrays.stream(eventObject.getThrowableProxy().getStackTraceElementProxyArray())
              .map(StackTraceElementProxy::getSTEAsString)
              .collect(Collectors.joining(System.lineSeparator()));
    }

    String jsonBody;
    try {
      jsonBody = objectMapper.writeValueAsString(json);
    } catch (JsonProcessingException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException(e);
    }

    RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
    //    String credential = Credentials.basic("admin", "2zo_Wi2LMuKTEwjbgQV.Xbpk.pjfgBGT");
    Request request =
        new Request.Builder()
            .url("http://localhost:9200/ris_backend_service_logging/_doc")
            .post(body)
            //        .addHeader("Authorization", credential)
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
    } catch (IOException e) {
      System.err.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  class LogJson {
    private LocalDateTime dateTime;
    private String level;
    private String message;
    private String exceptionStackTrace;
  }
}
