package com.ds.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ds.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_order_return")
public class OrderReturn extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String returnNo;

    private Long orderId;

    private Long memberId;

    private String reason;

    private BigDecimal refundAmount;

    private Integer status;

    private String auditRemark;

    private LocalDateTime auditTime;
}
