package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.AuthenticationRequest;
import com.fptu.math_master.dto.request.IntrospectRequest;
import com.fptu.math_master.dto.request.LogoutRequest;
import com.fptu.math_master.dto.request.RefreshRequest;
import com.fptu.math_master.dto.request.UserRegistrationRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.AuthenticationResponse;
import com.fptu.math_master.dto.response.IntrospectResponse;
import com.fptu.math_master.dto.response.UserResponse;
import com.fptu.math_master.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.text.ParseException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

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
  ApiResponse<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {

    var result = authenticationService.register(request);
    return ApiResponse.<UserResponse>builder().result(result).build();
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
}
