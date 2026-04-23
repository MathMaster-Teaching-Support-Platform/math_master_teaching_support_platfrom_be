package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.RefundRequestRequest;
import com.fptu.math_master.dto.response.RefundRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for managing refund requests.
 */
public interface RefundService {

  /**
   * Create a refund request for an order.
   *
   * @param orderId the order ID
   * @param request the refund request details
   * @return the created refund request
   */
  RefundRequestResponse createRefundRequest(UUID orderId, RefundRequestRequest request);

  /**
   * Get a refund request by ID.
   *
   * @param refundRequestId the refund request ID
   * @return the refund request
   */
  RefundRequestResponse getRefundRequest(UUID refundRequestId);

  /**
   * Get all refund requests for the current user.
   *
   * @param pageable pagination parameters
   * @return page of refund requests
   */
  Page<RefundRequestResponse> getMyRefundRequests(Pageable pageable);

  /**
   * Get all pending refund requests (admin only).
   *
   * @param pageable pagination parameters
   * @return page of pending refund requests
   */
  Page<RefundRequestResponse> getPendingRefundRequests(Pageable pageable);

  /**
   * Approve a refund request (admin only).
   *
   * @param refundRequestId the refund request ID
   * @param adminNotes optional admin notes
   * @return the updated refund request
   */
  RefundRequestResponse approveRefundRequest(UUID refundRequestId, String adminNotes);

  /**
   * Reject a refund request (admin only).
   *
   * @param refundRequestId the refund request ID
   * @param adminNotes rejection reason
   * @return the updated refund request
   */
  RefundRequestResponse rejectRefundRequest(UUID refundRequestId, String adminNotes);

  /**
   * Cancel a refund request (student only, must be pending).
   *
   * @param refundRequestId the refund request ID
   * @return the updated refund request
   */
  RefundRequestResponse cancelRefundRequest(UUID refundRequestId);
}
