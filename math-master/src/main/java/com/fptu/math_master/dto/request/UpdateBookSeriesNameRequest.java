package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBookSeriesNameRequest {

  @NotBlank
  @Size(max = 255)
  private String name;
}

