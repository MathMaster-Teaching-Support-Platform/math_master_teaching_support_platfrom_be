package com.fptu.math_master.util;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

  private SecurityUtils() {}

  public static UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      return UUID.fromString(jwtAuth.getToken().getSubject());
    }
    throw new IllegalStateException("Authentication is not JwtAuthenticationToken");
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
