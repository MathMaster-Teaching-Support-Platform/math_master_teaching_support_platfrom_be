package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.MindmapStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapResponse {

  private UUID id;
  private UUID teacherId;
  private String teacherName;
  private UUID lessonId;
  private String lessonTitle;
  private String title;
  private String description;
  private Boolean aiGenerated;
  private String generationPrompt;
  private MindmapStatus status;
  private Integer nodeCount;
  private Instant createdAt;
  private Instant updatedAt;
}
