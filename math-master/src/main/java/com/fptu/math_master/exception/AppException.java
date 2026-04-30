package com.fptu.math_master.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * The exception to 'AppException'.
 */
public class AppException extends RuntimeException {

  public AppException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.message = errorCode.getMessage();
  }

  public AppException(ErrorCode errorCode, String customMessage) {
    super(customMessage);
    this.errorCode = errorCode;
    this.message = customMessage;
  }

  private final ErrorCode errorCode;
  private final String message;
}
