package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminSendEmailRequest {

  @NotBlank(message = "Subject is required")
  @Size(max = 255, message = "Subject must not exceed 255 characters")
  String subject;

  @NotBlank(message = "Body is required")
  @Size(max = 10000, message = "Body must not exceed 10000 characters")
  String body;
}
