package com.fptu.math_master.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Date;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
/**
 * The entity of 'InvalidatedToken'.
 */
public class InvalidatedToken {

  /**
   * id
   */
  @Id String id;

  /**
   * expiry_time
   */
  Date expiryTime;
}
