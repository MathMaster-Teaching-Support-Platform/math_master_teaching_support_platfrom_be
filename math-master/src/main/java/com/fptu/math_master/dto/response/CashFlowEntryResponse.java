package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CashFlowType;
import com.fptu.math_master.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A single transaction row in the cash-flow list view. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowEntryResponse {
    private UUID id;
    /** INFLOW or OUTFLOW — derived from TransactionType */
    private CashFlowType direction;
    /** Original TransactionType for display */
    private TransactionType type;
    private CashFlowCategoryResponse category;
    private BigDecimal amount;
    private String currency;
    private String description;
    /** User full name (from wallet owner) */
    private String userName;
    private String userEmail;
    private Long orderCode;
    private Instant transactionDate;
    private Instant createdAt;
}
