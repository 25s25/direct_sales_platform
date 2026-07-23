package com.ds.member.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ds.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_member_social")
public class MemberSocial extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long memberId;

    private String socialType;

    private String unionId;

    private String openId;

    private String nickname;

    private String avatar;

    private String rawData;

    @TableLogic
    private Integer deleted;
}
