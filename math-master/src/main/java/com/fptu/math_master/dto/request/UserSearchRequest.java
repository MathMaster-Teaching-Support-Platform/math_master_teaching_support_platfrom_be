package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.Gender;
import com.fptu.math_master.enums.Status;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
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
public class UserSearchRequest {

  String keyword;

  String userName;

  String email;

  String fullName;

  Gender gender;

  Status status;

  String code;

  @PastOrPresent(message = "dobFrom must not be in the future")
  LocalDate dobFrom;

  @PastOrPresent(message = "dobTo must not be in the future")
  LocalDate dobTo;

  String roleName;

  @AssertTrue(message = "dobFrom must not be after dobTo")
  public boolean isDobRangeValid() {
    if (dobFrom == null || dobTo == null) return true;
    return !dobFrom.isAfter(dobTo);
  }
}
