package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.Status;
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
public class WalletResponse {
    
    private Long walletId;
    
    private Integer userId;
    
    private BigDecimal balance;
    
    private Status status;
    
    private Instant createdAt;
    
    private Instant updatedAt;
}
