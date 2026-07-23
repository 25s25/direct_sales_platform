package com.ds.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CreateOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<OrderItemDTO> items;

    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    @NotBlank(message = "收货人手机号不能为空")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    private String receiverAddr;

    private String remark;

    @Data
    public static class OrderItemDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long productId;

        private String skuCode;

        private Integer quantity;
    }
}