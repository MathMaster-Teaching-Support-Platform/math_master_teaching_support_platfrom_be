package com.fptu.math_master.dto.response;

import java.util.Set;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse {

  UUID id;

  String name;

  Set<PermissionResponse> permissions;
}
