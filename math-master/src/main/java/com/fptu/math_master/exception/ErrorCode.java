package com.fptu.math_master.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {
  UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
  USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
  USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
  INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
  USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
  UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
  UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
  INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
  ROLE_NOT_EXISTED(1009, "Role not existed", HttpStatus.NOT_FOUND),
  ROLE_ALREADY_EXISTS(1010, "Role already exists", HttpStatus.BAD_REQUEST),
  PERMISSION_NOT_EXISTED(1011, "Permission not existed", HttpStatus.NOT_FOUND),
  PERMISSION_ALREADY_EXISTS(1012, "Permission already exists", HttpStatus.BAD_REQUEST),
  EMAIL_ALREADY_EXISTS(1013, "Email already exists", HttpStatus.BAD_REQUEST),
  INCORRECT_PASSWORD(1014, "Current password is incorrect", HttpStatus.BAD_REQUEST),
  PASSWORD_MISMATCH(1015, "New password and confirm password do not match", HttpStatus.BAD_REQUEST),
  USER_ALREADY_BANNED(1016, "User is already banned", HttpStatus.BAD_REQUEST),
  USER_NOT_BANNED(1017, "User is not banned", HttpStatus.BAD_REQUEST),
  USER_ALREADY_DISABLED(1018, "User is already disabled", HttpStatus.BAD_REQUEST),
  USER_ALREADY_ENABLED(1019, "User is already enabled", HttpStatus.BAD_REQUEST),
  PROFILE_ALREADY_EXISTS(1020, "Teacher profile already exists", HttpStatus.BAD_REQUEST),
  PROFILE_NOT_FOUND(1021, "Teacher profile not found", HttpStatus.NOT_FOUND),
  PROFILE_ALREADY_APPROVED(1022, "Profile is already approved", HttpStatus.BAD_REQUEST),
  PROFILE_CANNOT_BE_MODIFIED(1023, "Approved profile cannot be modified", HttpStatus.BAD_REQUEST),
  INVALID_PROFILE_STATUS(1024, "Invalid profile status for this operation", HttpStatus.BAD_REQUEST),
  SCHOOL_NOT_FOUND(1025, "School not found", HttpStatus.NOT_FOUND),
  SCHOOL_ALREADY_EXISTS(1026, "School already exists", HttpStatus.BAD_REQUEST),
  WALLET_NOT_FOUND(1027, "Wallet not found", HttpStatus.NOT_FOUND),
  WALLET_ALREADY_EXISTS(1028, "Wallet already exists", HttpStatus.BAD_REQUEST),
  INSUFFICIENT_BALANCE(1029, "Insufficient balance", HttpStatus.BAD_REQUEST),
  TRANSACTION_NOT_FOUND(1030, "Transaction not found", HttpStatus.NOT_FOUND),
  PAYMENT_CREATION_FAILED(1031, "Payment creation failed", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_WEBHOOK_SIGNATURE(1032, "Invalid webhook signature", HttpStatus.BAD_REQUEST),
  PAYMENT_ALREADY_PROCESSED(1033, "Payment already processed", HttpStatus.BAD_REQUEST),
  INVALID_AMOUNT(1034, "Invalid amount", HttpStatus.BAD_REQUEST),
  QUESTION_BANK_NOT_FOUND(1035, "Question bank not found", HttpStatus.NOT_FOUND),
  QUESTION_BANK_ACCESS_DENIED(1036, "You do not have permission to access this question bank", HttpStatus.FORBIDDEN),
  QUESTION_BANK_HAS_QUESTIONS_IN_USE(1037, "Question bank has questions being used in assessments", HttpStatus.BAD_REQUEST),
  INVALID_SUBJECT(1038, "Invalid subject", HttpStatus.BAD_REQUEST),
  NOT_A_TEACHER(1039, "Only teachers can create question banks", HttpStatus.FORBIDDEN);

  ErrorCode(int code, String message, HttpStatusCode statusCode) {
    this.code = code;
    this.message = message;
    this.statusCode = statusCode;
  }

  private final int code;
  private final String message;
  private final HttpStatusCode statusCode;
}
