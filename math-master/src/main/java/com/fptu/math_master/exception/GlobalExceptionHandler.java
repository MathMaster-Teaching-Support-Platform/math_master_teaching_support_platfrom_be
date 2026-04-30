package com.fptu.math_master.exception;

import com.fptu.math_master.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

@ControllerAdvice
@Slf4j
/**
 * The exception to 'GlobalExceptionHandler'.
 */
public class GlobalExceptionHandler {

  private static final String MIN_ATTRIBUTE = "min";

  /**
   * Handles AsyncRequestNotUsableException which occurs when a client closes the connection
   * before the response is completed (e.g., timeout, navigation, or cancel).
   *
   * @param exception the AsyncRequestNotUsableException thrown
   */
  @ExceptionHandler(value = AsyncRequestNotUsableException.class)
  void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException exception) {
    // Client closed the connection (timeout/navigation/cancel). Not a server-side business error.
    log.warn("Client disconnected before response completed: {}", exception.getMessage());
  }

  /**
   * Handles generic uncategorized exceptions that are not caught by other exception handlers.
   *
   * @param exception the generic Exception to handle
   * @return ResponseEntity containing an ApiResponse with UNCATEGORIZED_EXCEPTION error code
   */
  @ExceptionHandler(value = Exception.class)
  ResponseEntity<ApiResponse<Void>> handlingRuntimeException(Exception exception) {
    log.error("Exception: ", exception);
    ApiResponse<Void> apiResponse = new ApiResponse<>();

    apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
    apiResponse.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
  }

  /**
   * Handles AppException which contains application-specific error information.
   *
   * @param exception the AppException containing error code and message
   * @return ResponseEntity with appropriate HTTP status and error details
   */
  @ExceptionHandler(value = AppException.class)
  ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception) {
    ErrorCode errorCode = exception.getErrorCode();
    
    // Log business exceptions so developers can trace them in the console
    log.warn("AppException occurred - Code: {}, Message: {}", errorCode.getCode(), errorCode.getMessage());
    
    ApiResponse<Void> apiResponse = new ApiResponse<>();

    apiResponse.setCode(errorCode.getCode());
    apiResponse.setMessage(exception.getMessage());

    return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
  }

  /**
   * Handles AccessDeniedException which occurs when a user lacks sufficient permissions.
   *
   * @param exception the AccessDeniedException thrown
   * @return ResponseEntity with HTTP 403 Forbidden status and UNAUTHORIZED error code
   */
  @ExceptionHandler(value = AccessDeniedException.class)
  ResponseEntity<ApiResponse<Void>> handlingAccessDeniedException(AccessDeniedException exception) {
    ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

    return ResponseEntity.status(errorCode.getStatusCode())
        .body(
            ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build());
  }

  /**
   * Handles MethodArgumentNotValidException for request validation failures.
   * Attempts to extract the error code from the validation message and retrieve
   * constraint violation attributes to provide detailed error information.
   *
   * @param exception the MethodArgumentNotValidException containing validation errors
   * @return ResponseEntity with HTTP 400 Bad Request and detailed error information
   */
  @ExceptionHandler(value = MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Void>> handlingValidation(MethodArgumentNotValidException exception) {
    String validationMessage =
        exception.getFieldError() != null
            ? exception.getFieldError().getDefaultMessage()
            : ErrorCode.INVALID_KEY.getMessage();

    ErrorCode errorCode = ErrorCode.INVALID_KEY;
    Map<String, Object> attributes = null;
    boolean mappedFromEnum = false;
    try {
      errorCode = ErrorCode.valueOf(validationMessage);
      mappedFromEnum = true;

      var constraintViolation =
          exception.getBindingResult().getAllErrors().getFirst().unwrap(ConstraintViolation.class);

      attributes = constraintViolation.getConstraintDescriptor().getAttributes();

      log.info(attributes.toString());

    } catch (IllegalArgumentException e) {
      log.warn("Invalid error code: {}", validationMessage);
    }

    ApiResponse<Void> apiResponse = new ApiResponse<>();

    apiResponse.setCode(errorCode.getCode());
    if (!mappedFromEnum) {
      apiResponse.setMessage(validationMessage);
    } else {
      apiResponse.setMessage(
          Objects.nonNull(attributes)
              ? mapAttribute(errorCode.getMessage(), attributes)
              : errorCode.getMessage());
    }

    return ResponseEntity.badRequest().body(apiResponse);
  }

  /**
   * Maps constraint violation attributes to error message by replacing placeholders.
   * Currently supports replacing the min value constraint in the message.
   *
   * @param message the error message with placeholder(s) to be replaced
   * @param attributes the constraint attributes containing values for replacement
   * @return the formatted message with replaced attribute values
   */
  private String mapAttribute(String message, Map<String, Object> attributes) {
    String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));

    return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
  }
}
