package com.ds.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ds.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_withdraw")
public class Withdraw extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long memberId;

    private String withdrawNo;

    private BigDecimal amount;

    private BigDecimal fee;

    private BigDecimal actualAmount;

    private String bankName;

    private String bankCard;

    private Integer status;

    private LocalDateTime auditTime;

    private LocalDateTime grantTime;

    private String remark;
}