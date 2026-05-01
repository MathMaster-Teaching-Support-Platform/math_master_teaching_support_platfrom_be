package com.fptu.math_master.entity;

import com.fptu.math_master.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Represents a configurable part in an exam matrix.
 * Replaces the hardcoded Part I=MCQ, Part II=TF, Part III=SA mapping.
 * Each matrix can define 1-3 parts with custom question types.
 */
@Entity
@Table(
    name = "exam_matrix_parts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_matrix_part_number",
            columnNames = {"exam_matrix_id", "part_number"}
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamMatrixPart extends BaseEntity {

    @Column(name = "exam_matrix_id", nullable = false)
    UUID examMatrixId;

    @Column(name = "part_number", nullable = false)
    Integer partNumber;

    @Column(name = "question_type", nullable = false)
    @Enumerated(EnumType.STRING)
    QuestionType questionType;

    @Column(name = "name")
    String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_matrix_id", insertable = false, updatable = false)
    ExamMatrix examMatrix;
}
