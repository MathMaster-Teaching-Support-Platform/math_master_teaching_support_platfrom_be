package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Teacher preview submission. Stateless — does not persist a submission /
 * answers / quiz attempt. Shape of {@code answers} value depends on question
 * type:
 *
 * <ul>
 *   <li>MULTIPLE_CHOICE: option key string ("A" / "B" / "C" / "D")</li>
 *   <li>TRUE_FALSE: comma-separated true keys ("A,C") or list of keys</li>
 *   <li>SHORT_ANSWER: free-text answer</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewSubmitRequest {

  @NotNull(message = "Answers map is required")
  private Map<UUID, Object> answers;
}
