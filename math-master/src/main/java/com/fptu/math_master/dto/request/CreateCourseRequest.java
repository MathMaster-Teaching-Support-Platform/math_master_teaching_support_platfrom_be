package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CourseProvider;
import com.fptu.math_master.enums.CourseLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseRequest {

  /**
   * Determines the course flow.
   * MINISTRY → existing flow (subjectId + schoolGradeId required).
   * CUSTOM   → Udemy-style (subjectId + schoolGradeId optional).
   */
  @NotNull(message = "provider is required")
  private CourseProvider provider;

  /** Required when provider = MINISTRY. */
  private UUID subjectId;

  /** Required when provider = MINISTRY. */
  private UUID schoolGradeId;

  @NotBlank(message = "title is required")
  @Size(max = 255, message = "title must not exceed 255 characters")
  private String title;

  private String description;

  private String whatYouWillLearn;
  private String requirements;
  private String targetAudience;
  private String subtitle;
  private String language;
  
  /** Course difficulty level. Defaults to ALL_LEVELS if not specified. */
  private CourseLevel level;
  
  @DecimalMin(value = "0.0", inclusive = true, message = "originalPrice must not be negative")
  private java.math.BigDecimal originalPrice;
  
  @DecimalMin(value = "0.0", inclusive = true, message = "discountedPrice must not be negative")
  private java.math.BigDecimal discountedPrice;
  
  @Future(message = "discountExpiryDate must be in the future")
  private java.time.Instant discountExpiryDate;
}
