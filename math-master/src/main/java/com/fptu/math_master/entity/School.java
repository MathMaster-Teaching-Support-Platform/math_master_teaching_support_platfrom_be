package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "schools")
/**
 * The entity of 'School'.
 */
public class School extends BaseEntity {

  /**
   * name
   */
  @Column(name = "name", nullable = false, unique = true)
  String name;

  /**
   * address
   */
  @Column(name = "address")
  String address;

  /**
   * city
   */
  @Column(name = "city")
  String city;

  /**
   * district
   */
  @Column(name = "district")
  String district;

  /**
   * phone_number
   */
  @Column(name = "phone_number")
  String phoneNumber;

  /**
   * email
   */
  @Column(name = "email")
  String email;

  /**
   * website
   */
  @Column(name = "website")
  String website;
}
