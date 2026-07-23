package com.ds.order.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private Long memberId;

    private String memberName;

    private String memberPhone;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private BigDecimal discountAmount;

    private BigDecimal totalPv;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddr;

    private Integer payType;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime receiveTime;

    private String expressCompany;

    private String expressNo;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<OrderItemVO> items;

    @Data
    public static class OrderItemVO implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long id;

        private Long productId;

        private String productName;

        private String productImage;

        private String skuCode;

        private String specName;

        private BigDecimal price;

        private Integer quantity;

        private BigDecimal pv;
    }
}