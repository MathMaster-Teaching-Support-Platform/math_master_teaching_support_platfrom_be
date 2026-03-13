package com.fptu.math_master.enums;

/**
 * Cognitive difficulty levels for exam questions.
 * <p>
 * The first four values follow the Vietnamese THPT (national-exam) framework
 * and are the <strong>preferred</strong> values for this platform.
 * The extended Bloom's taxonomy values are kept for backward-compatibility
 * and advanced use-cases.
 */
public enum CognitiveLevel {

  // ── Vietnamese THPT standard ─────────────────────────────────────────────
  /** Nhận Biết (NB) — recognition / recall. */
  NHAN_BIET,

  /** Thông Hiểu (TH) — comprehension / understanding. */
  THONG_HIEU,

  /** Vận Dụng (VD) — application. */
  VAN_DUNG,

  /** Vận Dụng Cao (VDC) — higher-order application / synthesis. */
  VAN_DUNG_CAO,

  // ── Extended Bloom's taxonomy (keep for compatibility) ───────────────────
  REMEMBER,
  UNDERSTAND,
  APPLY,
  ANALYZE,
  EVALUATE,
  CREATE
}

