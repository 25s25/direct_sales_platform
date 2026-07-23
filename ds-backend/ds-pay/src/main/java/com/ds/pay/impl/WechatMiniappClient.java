package com.ds.pay.impl;

import com.ds.pay.config.PayProperties;
import com.ds.pay.core.PayChannel;
import com.ds.pay.core.PaymentResult;
import com.ds.pay.entity.PaymentOrder;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
public class WechatMiniappClient extends WechatJsapiClient {

    private final PayProperties payProperties;

    public WechatMiniappClient(PayProperties payProperties) {
        super(payProperties);
        this.payProperties = payProperties;
    }

    private RSAAutoCertificateConfig config() {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(payProperties.wechatMchId())
                .privateKey(payProperties.wechatPrivateKey())
                .merchantSerialNumber(payProperties.wechatMchSerialNo())
                .apiV3Key(payProperties.wechatApiV3Key())
                .build();
    }

    private JsapiService service() {
        return new JsapiService.Builder().config(config()).build();
    }

    @Override
    public PayChannel channel() {
        return PayChannel.WECHAT_MINIAPP;
    }

    @Override
    public PaymentResult createPayment(PaymentOrder order, Map<String, Object> extra) {
        String openid = extra != null ? (String) extra.get("openid") : null;
        if (openid == null || openid.isBlank()) {
            return PaymentResult.fail("小程序支付需要openid");
        }

        String appId = payProperties.wechatMiniAppId();
        if (appId == null || appId.isBlank()) {
            appId = payProperties.wechatAppId();
        }

        PrepayRequest request = new PrepayRequest();
        request.setAppid(appId);
        request.setMchid(payProperties.wechatMchId());
        request.setDescription("订单支付-" + order.getPayOrderNo());
        request.setOutTradeNo(order.getPayOrderNo());
        request.setNotifyUrl(payProperties.wechatNotifyUrl());

        Amount amount = new Amount();
        amount.setTotal(order.getAmount().multiply(new BigDecimal("100")).intValue());
        request.setAmount(amount);

        Payer payer = new Payer();
        payer.setOpenid(openid);
        request.setPayer(payer);

        try {
            PrepayResponse response = service().prepay(request);
            String prepayId = response.getPrepayId();
            PaymentResult result = PaymentResult.success();
            result.setPayOrderNo(order.getPayOrderNo());
            result.setPrepayId(prepayId);
            result.setJsapiParams(buildJsapiParams(appId, prepayId));
            return result;
        } catch (ServiceException e) {
            log.error("微信小程序支付下单失败", e);
            return PaymentResult.fail(e.getMessage());
        }
    }
}
