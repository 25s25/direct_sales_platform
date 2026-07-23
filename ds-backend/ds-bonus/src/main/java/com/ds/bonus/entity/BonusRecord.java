package com.ds.bonus.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ds.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_bonus_record")
public class BonusRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long memberId;

    private String period;

    private String bonusType;

    private BigDecimal amount;

    private Long sourceOrderId;

    private String sourceOrderNo;

    private Integer status;

    private LocalDateTime grantTime;
}