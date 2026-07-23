package com.ds.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ds.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_order")
public class Order extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private Long memberId;

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

    private String expressCompany;

    private String expressNo;

    private LocalDateTime receiveTime;

    private Integer status;

    private String remark;
}