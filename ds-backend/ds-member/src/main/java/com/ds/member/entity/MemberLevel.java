package com.ds.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ds_member_level")
public class MemberLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String levelCode;

    private Integer sortOrder;

    private BigDecimal conditionPv;

    private BigDecimal discountRate;

    private BigDecimal bonusRate;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}