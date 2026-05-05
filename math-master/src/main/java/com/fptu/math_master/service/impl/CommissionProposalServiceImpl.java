package com.fptu.math_master.service.impl;

import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.dto.request.CommissionProposalRequest;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.dto.request.ReviewCommissionProposalRequest;
import com.fptu.math_master.dto.response.CommissionProposalResponse;
import com.fptu.math_master.entity.CommissionProposal;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.CommissionProposalStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CommissionProposalRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.CommissionProposalService;
import com.fptu.math_master.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class CommissionProposalServiceImpl implements CommissionProposalService {

    /** Platform default teacher share when no approved proposal exists (90 %). */
    private static final BigDecimal DEFAULT_TEACHER_SHARE = new BigDecimal("0.90");

    CommissionProposalRepository commissionProposalRepository;
    UserRepository userRepository;
    StreamPublisher streamPublisher;

    // ── Teacher-facing ────────────────────────────────────────────────────────

    @Override
    public CommissionProposalResponse submitProposal(CommissionProposalRequest request) {
        UUID teacherId = SecurityUtils.getCurrentUserId();

        // Block submission if a PENDING proposal already exists
        commissionProposalRepository
                .findFirstByTeacherIdAndStatusOrderByCreatedAtDesc(teacherId, CommissionProposalStatus.PENDING)
                .ifPresent(p -> {
                    throw new AppException(ErrorCode.COMMISSION_PROPOSAL_PENDING_EXISTS);
                });

        BigDecimal teacherShare = request.getTeacherShare().setScale(4, RoundingMode.HALF_UP);
        BigDecimal platformShare = BigDecimal.ONE.subtract(teacherShare).setScale(4, RoundingMode.HALF_UP);

        CommissionProposal proposal = CommissionProposal.builder()
                .teacherId(teacherId)
                .teacherShare(teacherShare)
                .platformShare(platformShare)
                .status(CommissionProposalStatus.PENDING)
                .build();

        proposal = commissionProposalRepository.save(proposal);

        log.info("Teacher {} submitted commission proposal: teacherShare={}", teacherId, teacherShare);

        return mapToResponse(proposal);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionProposalResponse> getMyProposals(Pageable pageable) {
        UUID teacherId = SecurityUtils.getCurrentUserId();
        return commissionProposalRepository
                .findByTeacherIdOrderByCreatedAtDesc(teacherId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionProposalResponse getMyActiveRate() {
        UUID teacherId = SecurityUtils.getCurrentUserId();
        return commissionProposalRepository
                .findTopByTeacherIdAndStatusOrderByReviewedAtDesc(teacherId, CommissionProposalStatus.APPROVED)
                .map(this::mapToResponse)
                .orElse(null);
    }

    // ── Admin-facing ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<CommissionProposalResponse> getAllProposals(CommissionProposalStatus status, Pageable pageable) {
        if (status != null) {
            return commissionProposalRepository
                    .findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(this::mapToResponse);
        }
        return commissionProposalRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public CommissionProposalResponse reviewProposal(UUID proposalId, ReviewCommissionProposalRequest request) {
        UUID adminId = SecurityUtils.getCurrentUserId();

        // Validate action
        if (request.getAction() != CommissionProposalStatus.APPROVED
                && request.getAction() != CommissionProposalStatus.REJECTED) {
            throw new AppException(ErrorCode.COMMISSION_PROPOSAL_INVALID_ACTION);
        }

        CommissionProposal proposal = commissionProposalRepository.findById(proposalId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMISSION_PROPOSAL_NOT_FOUND));

        if (proposal.getStatus() != CommissionProposalStatus.PENDING) {
            throw new AppException(ErrorCode.COMMISSION_PROPOSAL_ALREADY_REVIEWED);
        }

        proposal.setStatus(request.getAction());
        proposal.setAdminNote(request.getAdminNote());
        proposal.setReviewedBy(adminId);
        proposal.setReviewedAt(Instant.now());
        proposal = commissionProposalRepository.save(proposal);

        log.info("Admin {} {} commission proposal {} for teacher {}",
                adminId, request.getAction(), proposalId, proposal.getTeacherId());

        // Notify teacher
        notifyTeacher(proposal, request.getAction());

        return mapToResponse(proposal);
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getActiveTeacherShare(UUID teacherId) {
        return commissionProposalRepository
                .findTopByTeacherIdAndStatusOrderByReviewedAtDesc(teacherId, CommissionProposalStatus.APPROVED)
                .map(CommissionProposal::getTeacherShare)
                .orElse(DEFAULT_TEACHER_SHARE);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void notifyTeacher(CommissionProposal proposal, CommissionProposalStatus action) {
        try {
            boolean approved = action == CommissionProposalStatus.APPROVED;
            String title = approved
                    ? "Đề xuất hoa hồng đã được phê duyệt"
                    : "Đề xuất hoa hồng bị từ chối";
            String teacherSharePct = proposal.getTeacherShare()
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP) + "%";
            String content = approved
                    ? "Đề xuất nhận " + teacherSharePct + " doanh thu của bạn đã được phê duyệt và có hiệu lực từ đơn hàng tiếp theo."
                    : "Đề xuất nhận " + teacherSharePct + " doanh thu của bạn đã bị từ chối."
                            + (proposal.getAdminNote() != null ? " Lý do: " + proposal.getAdminNote() : "");

            NotificationRequest notification = NotificationRequest.builder()
                    .id(UUID.randomUUID().toString())
                    .type("COMMISSION")
                    .title(title)
                    .content(content)
                    .recipientId(proposal.getTeacherId().toString())
                    .senderId("SYSTEM")
                    .timestamp(LocalDateTime.now())
                    .actionUrl("/teacher/commission")
                    .build();
            streamPublisher.publish(notification);
        } catch (Exception e) {
            log.error("Failed to send commission proposal notification to teacher {}", proposal.getTeacherId(), e);
        }
    }

    private CommissionProposalResponse mapToResponse(CommissionProposal proposal) {
        User teacher = userRepository.findById(proposal.getTeacherId()).orElse(null);
        return CommissionProposalResponse.builder()
                .id(proposal.getId())
                .teacherId(proposal.getTeacherId())
                .teacherName(teacher != null ? teacher.getFullName() : null)
                .teacherEmail(teacher != null ? teacher.getEmail() : null)
                .teacherShare(proposal.getTeacherShare())
                .platformShare(proposal.getPlatformShare())
                .status(proposal.getStatus())
                .adminNote(proposal.getAdminNote())
                .reviewedBy(proposal.getReviewedBy())
                .reviewedAt(proposal.getReviewedAt())
                .createdAt(proposal.getCreatedAt())
                .updatedAt(proposal.getUpdatedAt())
                .build();
    }
}
