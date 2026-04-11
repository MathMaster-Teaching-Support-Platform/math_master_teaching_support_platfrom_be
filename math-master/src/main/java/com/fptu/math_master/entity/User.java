package com.fptu.math_master.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.hibernate.annotations.Nationalized;

import com.fptu.math_master.enums.Gender;
import com.fptu.math_master.enums.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
/**
 * The entity of 'User'.
 */
public class User extends BaseEntity {

  /**
   * username
   */
  @Size(max = 50)
  @Column(name = "username", length = 50)
  private String userName;

  /**
   * password
   */
  @Size(max = 255)
  @Column(name = "password")
  private String password;

  /**
   * full_name
   */
  @Size(max = 255)
  @Column(name = "full_name")
  private String fullName;

  /**
   * email
   */
  @Size(max = 255)
  @Nationalized
  @Column(name = "email", length = 255)
  private String email;

  /**
   * phone_number
   */
  @Size(max = 20)
  @Column(name = "phone_number", length = 20)
  private String phoneNumber;

  /**
   * gender
   */
  @Column(name = "gender")
  @Enumerated(EnumType.STRING)
  private Gender gender;

  /**
   * avatar
   */
  @Column(name = "avatar")
  private String avatar;

  /**
   * dob
   */
  @Column(name = "dob")
  private LocalDate dob;

  /**
   * code
   */
  @Size(max = 100)
  @Column(name = "code", length = 100)
  private String code;

  /**
   * status
   */
  @Builder.Default
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private Status status = Status.INACTIVE;

  /**
   * last_login
   */
  @Column(name = "last_login")
  private Instant lastLogin;

  /**
   * ban_reason
   */
  @Column(name = "ban_reason", length = 500)
  private String banReason;

  /**
   * ban_date
   */
  @Column(name = "ban_date")
  private Instant banDate;

  /**
   * created_by_name
   */
  @Size(max = 50)
  @Column(name = "created_by_name", length = 50)
  private String createdByName;

  /**
   * updated_by_name
   */
  @Size(max = 50)
  @Column(name = "updated_by_name", length = 50)
  private String updatedByName;

  /**
   * Relationships
   * - Many-to-Many with Role
   */
  @ManyToMany
  @JoinTable(
      name = "users_roles",
      joinColumns = @JoinColumn(name = "user_account_id"),
      inverseJoinColumns = @JoinColumn(name = "roles_id"))
  Set<Role> roles;
}
