package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.ReviewCommissionProposalRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.CommissionProposalResponse;
import com.fptu.math_master.enums.CommissionProposalStatus;
import com.fptu.math_master.service.CommissionProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin endpoints for reviewing teacher commission proposals.
 * All endpoints require the ADMIN role.
 */
@RestController
@RequestMapping("/admin/commission")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin — Commission", description = "Admin APIs for reviewing teacher revenue split proposals")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommissionController {

    CommissionProposalService commissionProposalService;

    @Operation(
        summary = "List all commission proposals",
        description = "Returns a paginated list of proposals. Pass `status` to filter by "
                    + "PENDING, APPROVED, or REJECTED. Omit to retrieve all proposals.")
    @GetMapping("/proposals")
    public ApiResponse<Page<CommissionProposalResponse>> getAllProposals(
            @RequestParam(required = false) CommissionProposalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String order) {

        Sort sort = "ASC".equalsIgnoreCase(order)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ApiResponse.<Page<CommissionProposalResponse>>builder()
                .result(commissionProposalService.getAllProposals(status, pageable))
                .build();
    }

    @Operation(
        summary = "Approve or reject a commission proposal",
        description = "Sets the proposal status to APPROVED or REJECTED, records the reviewing admin, "
                    + "and sends an in-app notification to the teacher.")
    @PutMapping("/proposals/{proposalId}/review")
    public ApiResponse<CommissionProposalResponse> reviewProposal(
            @PathVariable UUID proposalId,
            @Valid @RequestBody ReviewCommissionProposalRequest request) {

        return ApiResponse.<CommissionProposalResponse>builder()
                .result(commissionProposalService.reviewProposal(proposalId, request))
                .build();
    }
}
