package com.fptu.math_master.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookSeriesResponse {
  UUID id;
  String name;
}

