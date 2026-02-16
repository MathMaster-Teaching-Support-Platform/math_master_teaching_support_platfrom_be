package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateImportRequest {

    @NotNull(message = "File is required")
    MultipartFile file;

    /**
     * Optional hint for the subject/topic
     */
    String subjectHint;

    /**
     * Optional context to help AI understand the question type
     */
    String contextHint;
}

