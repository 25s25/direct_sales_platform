package com.ds.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderReturnApplyDTO {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    private String reason;

    private BigDecimal refundAmount;
}
