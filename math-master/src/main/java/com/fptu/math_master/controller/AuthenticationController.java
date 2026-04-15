package com.fptu.math_master.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fptu.math_master.dto.request.AuthenticationRequest;
import com.fptu.math_master.dto.request.ForgotPasswordRequest;
import com.fptu.math_master.dto.request.GoogleAuthRequest;
import com.fptu.math_master.dto.request.IntrospectRequest;
import com.fptu.math_master.dto.request.LogoutRequest;
import com.fptu.math_master.dto.request.RefreshRequest;
import com.fptu.math_master.dto.request.ResetPasswordRequest;
import com.fptu.math_master.dto.request.RoleSelectionRequest;
import com.fptu.math_master.dto.request.UserRegistrationRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.AuthenticationResponse;
import com.fptu.math_master.dto.response.IntrospectResponse;
import com.fptu.math_master.dto.response.UserRegisterResponse;
import com.fptu.math_master.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

  AuthenticationService authenticationService;

  @PostMapping("/register")
  @Operation(
      summary = "User registration",
      description =
          "Register a new user account with basic information. The user will be assigned the default USER role.")
  ApiResponse<UserRegisterResponse> register(@Valid @RequestBody UserRegistrationRequest request) {

    var result = authenticationService.register(request);
    return ApiResponse.<UserRegisterResponse>builder().result(result).build();
  }

  @PostMapping("/login")
  @Operation(
      summary = "User login",
      description =
          "Authenticate user credentials and return access token (JWT) along with related authentication information.")
  ApiResponse<AuthenticationResponse> authenticate(
      @Valid @RequestBody AuthenticationRequest request) {

    var result = authenticationService.login(request);
    return ApiResponse.<AuthenticationResponse>builder().result(result).build();
  }

  @PostMapping("/google")
  @Operation(
      summary = "Google Login",
      description = "Authenticate user with Google ID token and return access token (JWT).")
  ApiResponse<AuthenticationResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest request)
      throws GeneralSecurityException, IOException {

    var result = authenticationService.googleLogin(request);
    return ApiResponse.<AuthenticationResponse>builder().result(result).build();
  }

  @PostMapping("/introspect")
  @Operation(
      summary = "Token introspection",
      description =
          "Validate an access token and return its status, expiration time, and associated user information.")
  ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) {

    var result = authenticationService.introspect(request);
    return ApiResponse.<IntrospectResponse>builder().result(result).build();
  }

  @PostMapping("/refresh")
  @Operation(
      summary = "Refresh access token",
      description =
          "Generate a new access token using a valid refresh token when the current access token is expired.")
  ApiResponse<AuthenticationResponse> refresh(@RequestBody RefreshRequest request)
      throws ParseException, JOSEException {

    var result = authenticationService.refreshToken(request);
    return ApiResponse.<AuthenticationResponse>builder().result(result).build();
  }

  @PostMapping("/logout")
  @Operation(
      summary = "User logout",
      description =
          "Invalidate the refresh token or access token to terminate the current user session.")
  ApiResponse<Void> logout(@RequestBody LogoutRequest request)
      throws ParseException, JOSEException {

    authenticationService.logout(request);
    return ApiResponse.<Void>builder().build();
  }

  @PostMapping("/select-role")
  @Operation(
      summary = "Select user role",
      description = "After Google login, new user selects their role (Teacher or Student).")
  ApiResponse<AuthenticationResponse> selectRole(@Valid @RequestBody RoleSelectionRequest request) {
    var result = authenticationService.selectRole(request);
    return ApiResponse.<AuthenticationResponse>builder().result(result).build();
  }

  @GetMapping("/confirm-email")
  @Operation(
      summary = "Confirm email address",
      description = "Activate a registered account by verifying the confirmation token sent via email.")
  ApiResponse<Void> confirmEmail(@RequestParam String token) {
    authenticationService.confirmEmail(token);
    return ApiResponse.<Void>builder().build();
  }

  @PostMapping("/forgot-password")
  @Operation(
      summary = "Forgot password",
      description =
          "Send a password reset email to the given address. Always returns 200 OK regardless of whether the email exists to prevent user enumeration.")
  ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    authenticationService.forgotPassword(request);
    return ApiResponse.<Void>builder().build();
  }

  @PostMapping("/reset-password")
  @Operation(
      summary = "Reset password",
      description = "Reset the account password using the token received via email.")
  ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authenticationService.resetPassword(request);
    return ApiResponse.<Void>builder().build();
  }
}
