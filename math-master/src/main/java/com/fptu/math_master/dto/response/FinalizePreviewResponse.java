package com.fptu.math_master.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /exam-matrices/{matrixId}/cells/{cellId}/finalize. Summarises the result of
 * atomically persisting generated questions to DB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalizePreviewResponse {

  /** The template mapping that was finalised. */
  private UUID templateMappingId;

  /** The exam matrix owning the mapping. */
  private UUID matrixId;

  /** The template used to generate the questions. */
  private UUID templateId;

  /** How many questions were submitted for saving. */
  private int requestedCount;

  /** How many questions were actually saved (may be less if duplicates skipped). */
  private int savedCount;

  /** IDs of the newly created Question records. */
  private List<UUID> questionIds;

  /** Updated count of questions currently mapped to this template mapping (after replace/append logic). */
  private int currentMappingQuestionCount;

  /** Target number of questions for this mapping (from mapping.questionCount). */
  private int mappingTargetCount;

  /**
   * Non-fatal warnings, e.g.: - question skipped due to duplicate text - difficulty mismatch with
   * cell - cognitiveLevel mismatch with cell - total now exceeds cell target
   */
  private List<String> warnings;
}
