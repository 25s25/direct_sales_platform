package com.ds.member.social.core;

import lombok.Getter;

@Getter
public enum SocialType {

    WECHAT_WEB("wechat_web", "微信开放平台"),
    WECHAT_MP("wechat_mp", "微信公众号"),
    WECHAT_MINIAPP("wechat_miniapp", "微信小程序"),
    WORKWECHAT("workwechat", "企业微信");

    private final String code;
    private final String desc;

    SocialType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SocialType of(String code) {
        for (SocialType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
