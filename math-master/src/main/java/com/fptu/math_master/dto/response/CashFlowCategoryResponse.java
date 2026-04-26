package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CashFlowType;
import lombok.*;

/** A logical cash-flow category derived from TransactionType. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowCategoryResponse {
    /** Matches TransactionType.name() */
    private String id;
    private String name;
    private CashFlowType type;
    private String color;
    private String icon;
}
