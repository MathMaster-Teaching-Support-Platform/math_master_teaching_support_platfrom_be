package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CommissionProposalRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.CommissionProposalResponse;
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

/**
 * Teacher-facing endpoints for managing commission-split proposals.
 * All endpoints require the TEACHER role.
 */
@RestController
@RequestMapping("/teacher/commission")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Teacher — Commission", description = "APIs for teachers to propose revenue split rates")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherCommissionController {

    CommissionProposalService commissionProposalService;

    @Operation(
        summary = "Submit a new revenue split proposal",
        description = "Submits a request for the teacher's desired revenue share (50%–97%). "
                    + "Only one PENDING proposal is allowed at a time.")
    @PostMapping("/proposals")
    public ApiResponse<CommissionProposalResponse> submitProposal(
            @Valid @RequestBody CommissionProposalRequest request) {
        return ApiResponse.<CommissionProposalResponse>builder()
                .result(commissionProposalService.submitProposal(request))
                .build();
    }

    @Operation(
        summary = "List my proposals",
        description = "Returns a paginated history of all proposals submitted by the authenticated teacher.")
    @GetMapping("/proposals")
    public ApiResponse<Page<CommissionProposalResponse>> getMyProposals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.<Page<CommissionProposalResponse>>builder()
                .result(commissionProposalService.getMyProposals(pageable))
                .build();
    }

    @Operation(
        summary = "Get currently active rate",
        description = "Returns the most-recently-approved proposal for the authenticated teacher, "
                    + "or null if the platform default (90/10) applies.")
    @GetMapping("/proposals/active")
    public ApiResponse<CommissionProposalResponse> getMyActiveRate() {
        return ApiResponse.<CommissionProposalResponse>builder()
                .result(commissionProposalService.getMyActiveRate())
                .build();
    }
}
