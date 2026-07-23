package com.ds.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ds.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_sys_config")
public class SysConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String configKey;

    private String configValue;

    private String remark;
}