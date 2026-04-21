package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.TopicStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Individual topic item for batch operations.
 * - If id is null → CREATE new topic
 * - If id is not null and status is ACTIVE → UPDATE existing topic
 * - If id is not null and status is INACTIVE → SOFT DELETE topic
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicBatchItem {

  /**
   * Topic ID - null for new topics, UUID for existing topics
   */
  private UUID id;

  /**
   * Topic title (required)
   */
  @NotBlank(message = "Vui lòng nhập tên chủ đề")
  private String title;

  /**
   * Topic description (optional)
   */
  private String description;

  /**
   * Sequence order for visual display (required)
   */
  @NotNull(message = "Thứ tự là bắt buộc")
  @Min(value = 1, message = "Thứ tự phải lớn hơn 0")
  private Integer sequenceOrder;

  /**
   * Difficulty level (required)
   */
  @NotNull(message = "Độ khó là bắt buộc")
  private QuestionDifficulty difficulty;

  /**
   * Course IDs - topics can link to multiple courses (optional)
   */
  private List<UUID> courseIds;

  /**
   * Entry test mark checkpoint (optional, 0-10)
   */
  @Min(value = 0, message = "Điểm mốc phải từ 0-10")
  @Max(value = 10, message = "Điểm mốc phải từ 0-10")
  private Double mark;

  /**
   * Topic status (required)
   * - ACTIVE: Normal topic
   * - INACTIVE: Soft deleted topic
   */
  @NotNull(message = "Trạng thái là bắt buộc")
  private TopicStatus status;
}
