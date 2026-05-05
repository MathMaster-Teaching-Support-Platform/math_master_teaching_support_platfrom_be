package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A configurable platform-wide setting stored as a key-value pair.
 * The {@code config_value} field holds arbitrary TEXT (plain string, JSON, etc.).
 * <p>
 * Only one active row per {@code config_key} is allowed (unique constraint).
 * Soft-delete is inherited from {@link BaseEntity}.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "system_config",
    indexes = {
        @Index(name = "idx_system_config_key", columnList = "config_key")
    }
)
public class SystemConfig extends BaseEntity {

    /** Unique identifier for the setting (e.g. {@code privacy_policy}). */
    @Column(name = "config_key", nullable = false, unique = true, length = 255)
    private String configKey;

    /** The setting value — plain string or JSON blob. */
    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    /** Human-readable description of what this config controls. */
    @Column(name = "description", length = 500)
    private String description;
}
