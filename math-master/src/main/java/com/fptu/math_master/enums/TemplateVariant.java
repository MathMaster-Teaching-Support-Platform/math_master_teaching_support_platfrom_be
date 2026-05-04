package com.fptu.math_master.enums;

/**
 * Records how a {@link com.fptu.math_master.entity.QuestionTemplate} was created.
 * Audit only — does not branch generation logic. Both variants produce the same
 * Blueprint shape (templateText with {{}}, parameters with constraintText, answerFormula).
 *
 * <ul>
 *   <li>{@link #AI_REVERSE_TEMPLATED} — Method 1: teacher wrote a complete real-valued
 *       question; AI extracted placeholders + constraints in one call.</li>
 *   <li>{@link #MANUAL} — Method 2: power user wrote {{a}}, {{b}} placeholders and
 *       defined constraints by hand.</li>
 *   <li>{@link #PARAMETRIC}, {@link #PROPOSITIONAL} — legacy values, kept only so old
 *       database rows still deserialise. New code never writes these.</li>
 * </ul>
 */
public enum TemplateVariant {
  AI_REVERSE_TEMPLATED,
  MANUAL,
  /** @deprecated migrated to {@link #MANUAL} by V112; keep only for legacy deserialisation. */
  @Deprecated
  PARAMETRIC,
  /** @deprecated migrated to {@link #MANUAL} by V112; keep only for legacy deserialisation. */
  @Deprecated
  PROPOSITIONAL
}
