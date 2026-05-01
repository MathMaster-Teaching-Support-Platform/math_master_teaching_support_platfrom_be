package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.MatrixStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixResponse {

  private UUID id;
  private UUID teacherId;
  private String teacherName;
  private String name;
  private String description;
  private Boolean isReusable;
  private Integer numberOfParts;
  private Integer gradeLevel;
  private UUID subjectId;
  private String subjectName;
  private UUID questionBankId;
  private String questionBankName;
  private Integer totalQuestionsTarget;
  private BigDecimal totalPointsTarget;
  private MatrixStatus status;
  private List<ExamMatrixPartResponse> parts; // ALWAYS populated, never null
  private Integer templateMappingCount;
  private List<TemplateMappingResponse> templateMappings;
  private Integer bankMappingCount;
  private List<BankMappingResponse> bankMappings;
  private Instant createdAt;
  private Instant updatedAt;
}
