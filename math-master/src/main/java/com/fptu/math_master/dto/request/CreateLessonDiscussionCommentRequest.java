package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLessonDiscussionCommentRequest {

  @NotBlank(message = "COMMENT_REQUIRED")
  @Size(max = 3000)
  private String content;

  private UUID parentId;
}
