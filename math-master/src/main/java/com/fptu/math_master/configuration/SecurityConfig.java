package com.fptu.math_master.configuration;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

  CustomJwtDecoder customJwtDecoder;

  private static final String[] PUBLIC_POST_ENDPOINTS = {
      "/api/auth/register",
      "/api/auth/login",
      "/api/auth/google",
      "/api/auth/introspect",
      "/api/auth/logout",
      "/api/auth/refresh",
      "/api/auth/forgot-password",
      "/api/auth/reset-password",
      "/api/payment/webhook",
      "/api/question-templates/import-from-file",
  };

  private static final String[] PUBLIC_GET_ENDPOINTS = {
      "/api/schools/**",
      // OCR images exported by Python are exposed under /api/static/** for FE preview.
      "/api/static/**",
      "/actuator/health",
      "/actuator/info",
      "/actuator/mappings",
      "/api/ai/test",
      "/api/lessons/**",
      "/api/lesson-slides/public/**",
      "/api/auth/confirm-email",
      "/api/courses",
      "/api/courses/*",
      "/api/courses/*/preview",
      "/api/courses/*/reviews",
      "/api/courses/*/reviews/summary",
      "/api/courses/*/lessons",
      "/api/courses/*/sections",
      "/api/courses/*/lessons/*/video-url",
      // Public system config — e.g. privacy policy for registration flow
      "/api/config/**",
  };

  private static final String[] SWAGGER_WHITELIST = {
      "/v3/api-docs/**",
      "/api/v3/api-docs/**",
      "/swagger-ui/**",
      "/swagger-ui.html",
      "/swagger-resources/**",
      "/webjars/**"
  };

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
        .authorizeHttpRequests(
            request -> request
                .requestMatchers("/api/payment/webhook/**")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/mindmaps/public/**")
                .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers(SWAGGER_WHITELIST)
                .permitAll()
                .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS)
                .permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS)
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/crawl-data/static/**")
                .permitAll()
                .requestMatchers("/api/v1/crawl-data/**")
                .hasRole("ADMIN")
                .anyRequest()
                .authenticated())
        .oauth2ResourceServer(
            oauth2 -> oauth2
                .jwt(
                    jwtConfigurer -> jwtConfigurer
                        .decoder(customJwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()))
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()));

    return httpSecurity.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:3001",
            "https://sep.nhducminhqt.name.vn",
            "https://nhducminhqt.name.vn",
            "http://nhducminhqt.name.vn",
            "https://math-master-teaching-support-platfr.vercel.app"));

    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("Content-Disposition"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

    return jwtAuthenticationConverter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }
}
