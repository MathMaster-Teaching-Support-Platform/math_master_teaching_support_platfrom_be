package com.fptu.math_master.dto.request;

import com.fptu.math_master.dto.response.ContentBlockDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Patch one OCR'd page. Either updates content blocks, toggles verified, or both.
 * Null fields = leave unchanged (so FE can flip just the {@code verified} flag without resending
 * the whole content array).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLessonPageRequest {

  @Valid
  private List<ContentBlockDto> contentBlocks;

  private Boolean verified;
}
