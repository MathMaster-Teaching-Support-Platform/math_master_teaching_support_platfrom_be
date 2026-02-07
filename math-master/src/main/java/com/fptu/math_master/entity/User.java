package com.fptu.math_master.entity;

import com.fptu.math_master.enums.Gender;
import com.fptu.math_master.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id", nullable = false)
    private Integer id;

    @Size(max = 50)
    @Column(name = "username", length = 50)
    private String userName;

    @Size(max = 255)
    @Column(name = "password", length = 255)
    private String password;

    @Size(max = 255)
    @Column(name = "full_name", length = 255)
    private String fullName;

    @Size(max = 255)
    @Nationalized
    @Column(name = "email", length = 255)
    private String email;

    @Size(max = 20)
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Lob
    @Column(name = "avatar")
    private String avatar;

    @Column(name = "dob")
    private LocalDate dob;

    @Size(max = 100)
    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "ban_reason", length = 500)
    private String banReason;

    @Column(name = "ban_date")
    private Instant banDate;

    @Column(name = "created_date")
    private Instant createdDate;

    @Size(max = 50)
    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_date")
    private Instant updatedDate;

    @Size(max = 50)
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @ManyToMany
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_account_id"),
            inverseJoinColumns = @JoinColumn(name = "roles_id")
    )
    Set<Role> roles;

    @PrePersist
    public void prePersist() {
        if (status == null) status = Status.INACTIVE;
        if (createdDate == null) createdDate = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedDate = Instant.now();
    }
}
