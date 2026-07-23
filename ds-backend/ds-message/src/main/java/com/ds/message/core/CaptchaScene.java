package com.ds.message.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CaptchaScene {

    REGISTER("注册"),
    LOGIN("登录"),
    BIND("绑定"),
    RESET("重置");

    private final String desc;
}
