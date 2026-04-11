package com.fptu.math_master.dto.response;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserListResponse {

  List<UserResponse> users;
  Stats stats;
  Pagination pagination;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Stats {
    long total;
    long teachers;
    long students;
    long active;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Pagination {
    int page;
    int pageSize;
    long totalItems;
    int totalPages;
  }
}
