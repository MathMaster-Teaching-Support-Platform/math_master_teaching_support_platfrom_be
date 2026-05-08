package com.fptu.math_master.enums;

/**
 * Lifecycle of a textbook (Book) in the OCR pipeline.
 *
 * <pre>
 *   DRAFT       -> book record created, PDF maybe uploaded, no page mapping yet
 *   MAPPING     -> page mapping in progress (not all lessons mapped)
 *   READY       -> mapping complete and validated, ready to trigger OCR
 *   OCR_RUNNING -> Python crawler is processing pages
 *   OCR_DONE    -> OCR finished; user can verify per page
 *   OCR_FAILED  -> Python returned error; ocr_error column populated
 * </pre>
 */
public enum BookStatus {
  DRAFT,
  MAPPING,
  READY,
  OCR_RUNNING,
  OCR_DONE,
  OCR_FAILED
}
