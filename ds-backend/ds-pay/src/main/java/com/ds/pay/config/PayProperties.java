package com.ds.pay.config;

import com.ds.common.result.Result;
import com.ds.pay.core.PayChannel;
import com.ds.system.entity.SysConfig;
import com.ds.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayProperties {

    private final SysConfigService sysConfigService;

    private String getValue(String key) {
        Result<SysConfig> result = sysConfigService.getByKey(key);
        if (!result.isSuccess() || result.getData() == null) {
            return null;
        }
        return result.getData().getConfigValue();
    }

    private boolean isEnabled(String key) {
        return "true".equalsIgnoreCase(getValue(key));
    }

    public boolean isChannelEnabled(PayChannel channel) {
        return isEnabled("pay." + channel.name().toLowerCase().replace("_", ".") + ".enabled");
    }

    // ==================== 支付宝通用 ====================
    public boolean alipayEnabled() {
        return isEnabled("pay.alipay.enabled");
    }

    public String alipayAppId() {
        return getValue("pay.alipay.app-id");
    }

    public String alipayPrivateKey() {
        return getValue("pay.alipay.merchant-private-key");
    }

    public String alipayPublicKey() {
        return getValue("pay.alipay.alipay-public-key");
    }

    public String alipayServerUrl() {
        return getValue("pay.alipay.gateway");
    }

    public String alipayNotifyUrl() {
        return getValue("pay.alipay.notify-url");
    }

    public String alipayReturnUrl() {
        return getValue("pay.alipay.return-url");
    }

    public String alipayProductCode() {
        return getValue("pay.alipay.web.product-code");
    }

    public String alipaySignType() {
        return getValue("pay.alipay.sign-type");
    }

    public boolean alipayF2fEnabled() {
        return isEnabled("pay.alipay.f2f.enabled");
    }

    public String alipayF2fQrTimeout() {
        return getValue("pay.alipay.f2f.qr-timeout");
    }

    public boolean alipayWebEnabled() {
        return isEnabled("pay.alipay.web.enabled");
    }

    // ==================== 微信支付通用 ====================
    public boolean wechatEnabled() {
        return isEnabled("pay.wechat.enabled");
    }

    public String wechatAppId() {
        return getValue("pay.wechat.jsapi.app-id");
    }

    public String wechatMiniAppId() {
        return getValue("pay.wechat.miniapp.app-id");
    }

    public String wechatJsapiSecret() {
        return getValue("pay.wechat.jsapi.secret");
    }

    public String wechatMiniAppSecret() {
        return getValue("pay.wechat.miniapp.secret");
    }

    public String wechatNativeAppId() {
        return getValue("pay.wechat.native.app-id");
    }

    public String wechatMchId() {
        return getValue("pay.wechat.mchid");
    }

    public String wechatApiV3Key() {
        return getValue("pay.wechat.api-v3-key");
    }

    public String wechatMchSerialNo() {
        return getValue("pay.wechat.cert-serial-no");
    }

    public String wechatPrivateKey() {
        return getValue("pay.wechat.private-key");
    }

    public String wechatNotifyUrl() {
        return getValue("pay.wechat.notify-url");
    }

    // ==================== PayPal ====================
    public boolean paypalEnabled() {
        return isEnabled("pay.paypal.enabled");
    }

    public String paypalClientId() {
        return getValue("pay.paypal.client-id");
    }

    public String paypalClientSecret() {
        return getValue("pay.paypal.client-secret");
    }

    public String paypalMode() {
        return getValue("pay.paypal.mode");
    }

    public String paypalReturnUrl() {
        return getValue("pay.paypal.return-url");
    }

    public String paypalCancelUrl() {
        return getValue("pay.paypal.cancel-url");
    }
}
