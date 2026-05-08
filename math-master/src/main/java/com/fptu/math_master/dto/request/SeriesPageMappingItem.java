package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesPageMappingItem {
  @NotNull private UUID lessonId;
  @NotNull private UUID bookId;
  @NotNull @Min(1) private Integer pageStart;
  @NotNull @Min(1) private Integer pageEnd;
}

