package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionCreationRequest {

    @NotBlank(message = "Permission code is required")
    @Size(max = 100, message = "Permission code must not exceed 100 characters")
    String code;

    @NotBlank(message = "Permission name is required")
    @Size(max = 255, message = "Permission name must not exceed 255 characters")
    String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description;
}

