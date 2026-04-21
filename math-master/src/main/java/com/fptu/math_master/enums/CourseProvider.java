package com.fptu.math_master.enums;

/**
 * Discriminates between the two course creation flows:
 * <ul>
 *   <li>{@link #MINISTRY} – backed by the Ministry of Education curriculum
 *       (Grade → Subject → Chapter → Lesson).</li>
 *   <li>{@link #CUSTOM}   – Udemy-style; teacher defines sections and lessons freely.</li>
 * </ul>
 */
public enum CourseProvider {
  /** Default – existing Ministry of Education curriculum flow. */
  MINISTRY,
  /** Udemy-style – teacher defines sections and lessons freely. */
  CUSTOM
}
