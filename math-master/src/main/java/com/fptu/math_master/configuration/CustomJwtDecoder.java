package com.fptu.math_master.configuration;

import com.fptu.math_master.repository.InvalidatedTokenRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {

  private final InvalidatedTokenRepository invalidatedTokenRepository;

  @NonFinal
  @Value("${jwt.signerKey}")
  private String signerKey;

  @Override
  public Jwt decode(String token) throws JwtException {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);

      // Verify signature
      JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
      if (!signedJWT.verify(verifier)) {
        throw new JwtException("Invalid token signature");
      }

      // Check expiration
      Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
      if (expirationTime == null || expirationTime.before(new Date())) {
        throw new JwtException("Token expired");
      }

      // Check if token is invalidated (logged out)
      String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
      if (jwtId != null && invalidatedTokenRepository.existsById(jwtId)) {
        throw new JwtException("Token has been invalidated");
      }

      return new Jwt(
          token,
          signedJWT.getJWTClaimsSet().getIssueTime().toInstant(),
          signedJWT.getJWTClaimsSet().getExpirationTime().toInstant(),
          signedJWT.getHeader().toJSONObject(),
          signedJWT.getJWTClaimsSet().getClaims());

    } catch (ParseException e) {
      throw new JwtException("Invalid token format", e);
    } catch (JOSEException e) {
      throw new JwtException("Token verification failed", e);
    }
  }
}
