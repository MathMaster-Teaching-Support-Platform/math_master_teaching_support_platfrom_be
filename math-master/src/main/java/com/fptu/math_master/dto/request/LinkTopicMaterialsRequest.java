package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request to link materials (slides, questions, mindmaps, docs) to a topic
 * Based on question ID, the system fetches all related materials
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LinkTopicMaterialsRequest {

  @NotNull(message = "Question ID is required - system will fetch related materials")
  UUID questionId;

  @Builder.Default
  Boolean includeSlides = true;

  @Builder.Default
  Boolean includeQuestions = true;

  @Builder.Default
  Boolean includeMindmaps = true;

  @Builder.Default
  Boolean includeDocuments = true;

  @Builder.Default
  Integer startSequenceOrder = 1;
}
