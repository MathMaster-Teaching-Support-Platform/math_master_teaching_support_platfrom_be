package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SchoolRequest {

    @NotBlank(message = "School name is required")
    @Size(max = 255, message = "School name must not exceed 255 characters")
    String name;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    String city;

    @Size(max = 100, message = "District must not exceed 100 characters")
    String district;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    String phoneNumber;

    @Size(max = 100, message = "Email must not exceed 100 characters")
    String email;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    String website;
}
