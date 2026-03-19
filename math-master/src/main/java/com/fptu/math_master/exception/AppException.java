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
  }

  private ErrorCode errorCode;
}
