package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleUpdateRequest {

  @Size(max = 255, message = "Role name must not exceed 255 characters")
  String name;

  Set<String> permissionCodes;
}

