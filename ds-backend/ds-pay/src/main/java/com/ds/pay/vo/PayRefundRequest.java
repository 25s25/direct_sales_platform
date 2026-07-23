package com.ds.pay.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayRefundRequest {

    @NotBlank(message = "支付订单号不能为空")
    private String payOrderNo;

    @NotNull(message = "退款金额不能为空")
    private BigDecimal amount;
}
