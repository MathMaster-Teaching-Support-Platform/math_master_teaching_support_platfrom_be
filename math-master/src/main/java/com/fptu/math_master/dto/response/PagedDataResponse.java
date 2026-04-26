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
public class PagedDataResponse<T> {

  private List<T> data;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
}
