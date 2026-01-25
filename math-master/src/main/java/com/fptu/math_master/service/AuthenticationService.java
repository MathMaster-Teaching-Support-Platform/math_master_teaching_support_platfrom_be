package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.AuthenticationRequest;
import com.fptu.math_master.dto.request.IntrospectRequest;
import com.fptu.math_master.dto.request.LogoutRequest;
import com.fptu.math_master.dto.request.RefreshRequest;
import com.fptu.math_master.dto.response.AuthenticationResponse;
import com.fptu.math_master.dto.response.IntrospectResponse;
import com.nimbusds.jose.JOSEException;
import java.text.ParseException;

public interface AuthenticationService {
        IntrospectResponse introspect(IntrospectRequest request);

        AuthenticationResponse login(AuthenticationRequest request);

        void logout(LogoutRequest request) throws ParseException, JOSEException;

        AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException;
}
