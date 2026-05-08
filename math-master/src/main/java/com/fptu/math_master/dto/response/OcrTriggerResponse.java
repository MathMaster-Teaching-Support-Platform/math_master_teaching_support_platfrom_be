package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.BookStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrTriggerResponse {
  private UUID bookId;
  private BookStatus status;
  private int mappingCount;
  private int totalPagesQueued;
  private String message;
}
