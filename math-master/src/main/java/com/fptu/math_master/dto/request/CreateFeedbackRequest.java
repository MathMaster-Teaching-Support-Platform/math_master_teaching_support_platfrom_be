package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFeedbackRequest {

  @NotBlank(message = "Title is required")
  @Size(max = 200, message = "Title cannot exceed 200 characters")
  private String title;

  @NotBlank(message = "Description is required")
  @Size(max = 5000, message = "Description cannot exceed 5000 characters")
  private String description;

  @Size(max = 100, message = "Category cannot exceed 100 characters")
  private String category;

  @Size(max = 500, message = "Related URL cannot exceed 500 characters")
  private String relatedUrl;
}
