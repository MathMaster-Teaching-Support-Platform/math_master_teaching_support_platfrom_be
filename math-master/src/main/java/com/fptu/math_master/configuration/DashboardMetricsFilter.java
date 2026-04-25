package com.fptu.math_master.configuration;

import java.io.IOException;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DashboardMetricsFilter extends OncePerRequestFilter {

  private static final Set<String> TARGET_PATHS =
      Set.of(
          "/admin/dashboard/stats",
          "/admin/system/status",
          "/admin/dashboard/revenue-by-month",
          "/student/dashboard",
          "/student/dashboard/summary",
          "/student/dashboard/upcoming-tasks",
          "/student/dashboard/recent-grades",
          "/student/dashboard/learning-progress",
          "/student/dashboard/weekly-activity",
          "/student/dashboard/streak",
          "/api/student/dashboard",
          "/api/student/dashboard/summary",
          "/api/student/dashboard/upcoming-tasks",
          "/api/student/dashboard/recent-grades",
          "/api/student/dashboard/learning-progress",
          "/api/student/dashboard/weekly-activity",
          "/api/student/dashboard/streak");

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !TARGET_PATHS.contains(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startedAtNanos = System.nanoTime();
    ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
    try {
      filterChain.doFilter(request, responseWrapper);
    } finally {
      long elapsedMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
      int payloadBytes = responseWrapper.getContentSize();
      log.info(
          "dashboard-metric method={} path={} status={} latencyMs={} payloadBytes={}",
          request.getMethod(),
          request.getRequestURI(),
          responseWrapper.getStatus(),
          elapsedMs,
          payloadBytes);
      responseWrapper.copyBodyToResponse();
    }
  }
}
