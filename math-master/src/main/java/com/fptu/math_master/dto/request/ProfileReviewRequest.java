package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.ProfileStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileReviewRequest {

    @NotNull(message = "Status is required")
    ProfileStatus status; // APPROVED or REJECTED

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    String adminComment;
}
