package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    
    private Long transactionId;
    
    private Long walletId;
    
    private Long orderCode;
    
    private BigDecimal amount;
    
    private TransactionType type;
    
    private TransactionStatus status;
    
    private String description;
    
    private String paymentLinkId;
    
    private String referenceCode;
    
    private Instant transactionDate;
    
    private Instant createdAt;
    
    private Instant updatedAt;
}
