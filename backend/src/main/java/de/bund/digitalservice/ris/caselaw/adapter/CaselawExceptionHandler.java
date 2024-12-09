package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.adapter.exception.BucketException;
import de.bund.digitalservice.ris.caselaw.adapter.exception.LdmlTransformationException;
import de.bund.digitalservice.ris.caselaw.adapter.exception.PublishException;
import de.bund.digitalservice.ris.caselaw.domain.exception.ImportApiKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class CaselawExceptionHandler extends ResponseEntityExceptionHandler {
  @ExceptionHandler({ImportApiKeyException.class})
  public ResponseEntity<Object> handleImportApiKeyException(ImportApiKeyException ex) {

    ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getMessage());

    return new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({LdmlTransformationException.class})
  public ResponseEntity<Object> handleLdmlTransformationException(LdmlTransformationException ex) {

    ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, ex.getMessage());

    return new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({PublishException.class})
  public ResponseEntity<Object> handlePublishException(PublishException ex) {

    ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());

    return new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler({BucketException.class})
  public ResponseEntity<Object> handleBucketException(BucketException ex) {

    ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());

    return new ResponseEntity<>(apiError, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(AsyncRequestNotUsableException.class)
  void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException ex) {
    /* ignore exception because most of the time this exception occurred when the user
    close the browser without waiting to the result of an api call. */
  }

  public record ApiError(HttpStatus status, String message) {}
}
