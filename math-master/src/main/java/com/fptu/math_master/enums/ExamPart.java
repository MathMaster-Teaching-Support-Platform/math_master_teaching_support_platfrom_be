package com.fptu.math_master.enums;

import lombok.Getter;

/**
 * Represents the parts of a Vietnamese THPT exam.
 * Each part has a fixed question type:
 * - Part I: Multiple Choice (MCQ)
 * - Part II: True/False (TF)
 * - Part III: Short Answer (SA)
 *
 * This is a fixed mapping for the Vietnamese THPT format.
 * If flexibility is needed in the future, add a part_type_overrides JSONB column to ExamMatrix.
 */
@Getter
public enum ExamPart {
  PART_I(1, QuestionType.MULTIPLE_CHOICE, "Phần I - Trắc nghiệm"),
  PART_II(2, QuestionType.TRUE_FALSE, "Phần II - Đúng/Sai"),
  PART_III(3, QuestionType.SHORT_ANSWER, "Phần III - Tự luận");

  private final int partNumber;
  private final QuestionType questionType;
  private final String displayName;

  ExamPart(int partNumber, QuestionType questionType, String displayName) {
    this.partNumber = partNumber;
    this.questionType = questionType;
    this.displayName = displayName;
  }

  /**
   * Get the question type for a given part number.
   *
   * @param partNumber the part number (1, 2, or 3)
   * @return the corresponding QuestionType
   * @throws IllegalArgumentException if partNumber is not 1, 2, or 3
   * @deprecated Use ExamMatrixPart entity instead. Part types are now configurable per matrix.
   */
  @Deprecated
  public static QuestionType typeForPart(int partNumber) {
    for (ExamPart part : ExamPart.values()) {
      if (part.partNumber == partNumber) {
        return part.questionType;
      }
    }
    throw new IllegalArgumentException("Invalid part number: " + partNumber + ". Must be 1, 2, or 3.");
  }

  /**
   * Get the part number for a given question type.
   *
   * @param questionType the question type
   * @return the corresponding part number (1, 2, or 3)
   * @throws IllegalArgumentException if questionType is not MCQ, TF, or SA
   */
  public static int partForType(QuestionType questionType) {
    for (ExamPart part : ExamPart.values()) {
      if (part.questionType == questionType) {
        return part.partNumber;
      }
    }
    throw new IllegalArgumentException("Question type " + questionType + " is not mapped to any exam part.");
  }

  /**
   * Get the ExamPart for a given part number.
   *
   * @param partNumber the part number (1, 2, or 3)
   * @return the corresponding ExamPart
   * @throws IllegalArgumentException if partNumber is not 1, 2, or 3
   */
  public static ExamPart fromPartNumber(int partNumber) {
    for (ExamPart part : ExamPart.values()) {
      if (part.partNumber == partNumber) {
        return part;
      }
    }
    throw new IllegalArgumentException("Invalid part number: " + partNumber + ". Must be 1, 2, or 3.");
  }

  /**
   * Get the ExamPart for a given question type.
   *
   * @param questionType the question type
   * @return the corresponding ExamPart
   * @throws IllegalArgumentException if questionType is not MCQ, TF, or SA
   */
  public static ExamPart fromQuestionType(QuestionType questionType) {
    for (ExamPart part : ExamPart.values()) {
      if (part.questionType == questionType) {
        return part;
      }
    }
    throw new IllegalArgumentException("Question type " + questionType + " is not mapped to any exam part.");
  }
}
