package com.ds.member.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String memberNo;

    private String phone;

    private String realName;

    private String idCard;

    private String nickname;

    private String avatar;

    private String email;

    private Integer emailVerified;

    private Integer phoneVerified;

    private Long levelId;

    private String levelName;

    private Long recommendId;

    private String recommendName;

    private Long parentId;

    private String parentName;

    private Integer position;

    private String ancestorPath;

    private BigDecimal walletBalance;

    private BigDecimal frozenAmount;

    private BigDecimal totalPv;

    private Integer status;

    private String registerIp;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private String token;

    private Integer wechatBound;

    private Integer workWechatBound;

    private List<String> permissions;
}