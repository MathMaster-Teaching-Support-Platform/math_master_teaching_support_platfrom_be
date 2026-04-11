package com.fptu.math_master.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSystemStatusResponse {

  private List<ServiceStatus> services;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ServiceStatus {
    private String name;
    /** "active", "warning", or "error" */
    private String status;
    private String description;
    /** nullable — only present for storage-type services */
    private Integer usagePercent;
  }
}
