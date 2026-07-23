package com.ds.member.social.core;

import lombok.Data;

import java.io.Serializable;

@Data
public class SocialUserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private SocialType socialType;

    private String unionId;

    private String openId;

    private String nickname;

    private String avatar;

    private String rawData;
}
