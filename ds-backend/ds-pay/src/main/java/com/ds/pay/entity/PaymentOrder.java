package com.ds.pay.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ds.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_payment_order")
public class PaymentOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String payOrderNo;

    private String orderNo;

    private Long memberId;

    private String channel;

    private BigDecimal amount;

    private Integer status;

    private String thirdOrderNo;

    private LocalDateTime payTime;

    private LocalDateTime expireTime;

    private Integer callbackCount;

    private String callbackResult;

    @TableField("extra_data")
    private String extraData;

    @TableLogic
    private Integer deleted;

    private String remark;
}
