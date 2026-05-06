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
public class QuestionBankRequest {

  @NotBlank(message = "Name is required")
  @Size(max = 255, message = "Name must not exceed 255 characters")
  private String name;

  private String description;

  private Boolean isPublic;

  private UUID chapterId;

  /**
   * Required for the new "create bank by grade" happy-case flow.
   * Once set, the bank's chapter tree is fully derived from this grade.
   */
  private UUID schoolGradeId;

  /**
   * Optional: narrow the bank to a single subject (môn) within the grade.
   */
  private UUID subjectId;
}
