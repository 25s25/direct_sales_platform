package com.ds.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ds.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_member")
public class Member extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String memberNo;

    private String phone;

    private String password;

    private String realName;

    private String idCard;

    private String nickname;

    private String avatar;

    private String email;

    private Integer emailVerified;

    private Integer phoneVerified;

    private Long levelId;

    private Long recommendId;

    private Long parentId;

    private Integer position;

    private String ancestorPath;

    private BigDecimal walletBalance;

    private BigDecimal frozenAmount;

    private BigDecimal totalPv;

    private Integer status;

    @Version
    private Integer version;

    private String registerIp;

    private LocalDateTime lastLoginTime;
}