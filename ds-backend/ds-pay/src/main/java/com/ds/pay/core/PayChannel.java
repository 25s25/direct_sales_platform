package com.ds.pay.core;

import com.ds.common.exception.BusinessException;

public enum PayChannel {

    ALIPAY_F2F,
    ALIPAY_WEB,
    WECHAT_NATIVE,
    WECHAT_JSAPI,
    WECHAT_MINIAPP,
    PAYPAL,
    WALLET;

    public static PayChannel from(String channel) {
        if (channel == null || channel.isBlank()) {
            throw new BusinessException("支付渠道不能为空");
        }
        try {
            return valueOf(channel.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的支付渠道: " + channel);
        }
    }
}
