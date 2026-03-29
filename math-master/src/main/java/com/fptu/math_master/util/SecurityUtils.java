package com.fptu.math_master.util;

import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

  private SecurityUtils() {}

  public static UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (!(auth instanceof JwtAuthenticationToken jwtAuth) || !auth.isAuthenticated()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    String subject = jwtAuth.getToken().getSubject();
    if (subject == null || subject.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    try {
      return UUID.fromString(subject);
    } catch (IllegalArgumentException ex) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }

  public static boolean hasRole(String roleName) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return false;
    String prefixed = "ROLE_" + roleName;
    return auth.getAuthorities().stream()
        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
        .anyMatch(a -> a.equals(roleName) || a.equals(prefixed));
  }
}
