package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.LatexRenderRequest;

public interface LatexRenderService {
  String render(LatexRenderRequest request);
}
