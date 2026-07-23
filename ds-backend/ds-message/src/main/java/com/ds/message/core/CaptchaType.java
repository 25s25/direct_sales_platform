package com.ds.message.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CaptchaType {

    EMAIL("邮箱"),
    SMS("短信");

    private final String desc;
}
