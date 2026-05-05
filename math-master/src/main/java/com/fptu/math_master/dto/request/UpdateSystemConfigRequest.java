package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for updating a system config value.
 *
 * @param configValue new raw value (plain text or JSON string); must not be blank
 */
public record UpdateSystemConfigRequest(
        @NotBlank(message = "Config value must not be blank")
        String configValue
) {}
