package com.fptu.math_master.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOSWebhookRequest {

  private String code;

  private String desc;

  private Boolean success;

  private WebhookData data;

  private String signature;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WebhookData {

    private Long orderCode;

    private Long amount;

    private String description;

    private String accountNumber;

    private String reference;

    private String transactionDateTime;

    private String currency;

    private String paymentLinkId;

    private String code;

    private String desc;

    private String counterAccountBankId;

    private String counterAccountBankName;

    private String counterAccountName;

    private String counterAccountNumber;

    private String virtualAccountName;

    private String virtualAccountNumber;
  }
}
