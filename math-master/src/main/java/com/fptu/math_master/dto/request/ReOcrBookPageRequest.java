package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Step-4 verify: queue re-OCR for one (lesson, PDF page) via the Python crawler. */
public record ReOcrBookPageRequest(
    @NotNull UUID lessonId, @NotNull @Min(1) Integer pageNumber) {}
