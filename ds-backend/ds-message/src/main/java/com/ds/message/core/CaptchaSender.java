package com.ds.message.core;

public interface CaptchaSender {

    boolean supports(CaptchaType type);

    void send(String target, String code, CaptchaScene scene);
}
