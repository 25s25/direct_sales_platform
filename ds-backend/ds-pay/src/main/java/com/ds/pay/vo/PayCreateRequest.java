package com.ds.pay.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class PayCreateRequest {

    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    @NotBlank(message = "支付渠道不能为空")
    private String channel;

    private Map<String, Object> extra;
}
