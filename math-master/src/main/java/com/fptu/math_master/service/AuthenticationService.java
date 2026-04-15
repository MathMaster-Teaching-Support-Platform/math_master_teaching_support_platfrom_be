package com.fptu.math_master.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;

import com.fptu.math_master.dto.request.AuthenticationRequest;
import com.fptu.math_master.dto.request.ForgotPasswordRequest;
import com.fptu.math_master.dto.request.GoogleAuthRequest;
import com.fptu.math_master.dto.request.IntrospectRequest;
import com.fptu.math_master.dto.request.LogoutRequest;
import com.fptu.math_master.dto.request.RefreshRequest;
import com.fptu.math_master.dto.request.ResetPasswordRequest;
import com.fptu.math_master.dto.request.RoleSelectionRequest;
import com.fptu.math_master.dto.request.UserRegistrationRequest;
import com.fptu.math_master.dto.response.AuthenticationResponse;
import com.fptu.math_master.dto.response.IntrospectResponse;
import com.fptu.math_master.dto.response.UserRegisterResponse;
import com.nimbusds.jose.JOSEException;

public interface AuthenticationService {
  IntrospectResponse introspect(IntrospectRequest request);

  AuthenticationResponse login(AuthenticationRequest request);

  AuthenticationResponse googleLogin(GoogleAuthRequest request)
      throws GeneralSecurityException, IOException;

  void logout(LogoutRequest request) throws ParseException, JOSEException;

  AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException;

  UserRegisterResponse register(UserRegistrationRequest request);

  void confirmEmail(String token);

  AuthenticationResponse selectRole(RoleSelectionRequest request);

  void forgotPassword(ForgotPasswordRequest request);

  void resetPassword(ResetPasswordRequest request);
}
