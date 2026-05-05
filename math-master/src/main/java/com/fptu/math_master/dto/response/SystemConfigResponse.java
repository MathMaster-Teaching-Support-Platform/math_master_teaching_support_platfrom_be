package com.fptu.math_master.dto.response;

import java.time.Instant;

/**
 * Public/admin read view of a single system config entry.
 *
 * @param configKey   unique key (e.g. {@code privacy_policy})
 * @param configValue raw value — plain text or JSON string
 * @param description human-readable description of this config
 * @param updatedAt   last modification timestamp
 */
public record SystemConfigResponse(
        String configKey,
        String configValue,
        String description,
        Instant updatedAt
) {}
