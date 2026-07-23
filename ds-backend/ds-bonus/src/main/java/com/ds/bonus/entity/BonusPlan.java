package com.ds.bonus.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ds.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_bonus_plan")
public class BonusPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String name;

    private String planType;

    private String calcRuleJson;

    private Integer isActive;

    private String remark;
}