package com.fptu.math_master.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterResponse {
  private UUID id;
  private UUID lessonId;
  private String title;
  private String description;
  private Integer orderIndex;
}

