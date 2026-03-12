package com.fptu.math_master.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloneAssessmentRequest {

  private String newTitle;

  private Boolean cloneQuestions;
}
