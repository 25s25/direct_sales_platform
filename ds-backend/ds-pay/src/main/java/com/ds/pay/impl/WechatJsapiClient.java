package com.ds.pay.impl;

import com.ds.pay.config.PayProperties;
import com.ds.pay.core.PayChannel;
import com.ds.pay.core.PayClient;
import com.ds.pay.core.PaymentResult;
import com.ds.pay.core.RefundResult;
import com.ds.pay.entity.PaymentOrder;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.core.util.PemUtil;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatJsapiClient implements PayClient {

    private final PayProperties payProperties;

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
        return PayChannel.WECHAT_JSAPI;
    }

    @Override
    public PaymentResult createPayment(PaymentOrder order, Map<String, Object> extra) {
        String openid = extra != null ? (String) extra.get("openid") : null;
        if (openid == null || openid.isBlank()) {
            return PaymentResult.fail("JSAPI支付需要openid");
        }

        PrepayRequest request = new PrepayRequest();
        request.setAppid(payProperties.wechatAppId());
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
            result.setJsapiParams(buildJsapiParams(payProperties.wechatAppId(), prepayId));
            return result;
        } catch (ServiceException e) {
            log.error("微信支付JSAPI下单失败", e);
            return PaymentResult.fail(e.getMessage());
        }
    }

    protected Map<String, String> buildJsapiParams(String appId, String prepayId) {
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String packageStr = "prepay_id=" + prepayId;
        String signType = "RSA";

        String signStr = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageStr + "\n";
        String paySign = sign(signStr);

        return Map.of(
                "appId", appId,
                "timeStamp", timeStamp,
                "nonceStr", nonceStr,
                "package", packageStr,
                "signType", signType,
                "paySign", paySign
        );
    }

    private String sign(String data) {
        try {
            PrivateKey privateKey = PemUtil.loadPrivateKeyFromString(payProperties.wechatPrivateKey());
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new RuntimeException("微信支付签名失败", e);
        }
    }

    @Override
    public boolean verifyNotify(Map<String, Object> params, String body) {
        try {
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber((String) params.get("Wechatpay-Serial"))
                    .nonce((String) params.get("Wechatpay-Nonce"))
                    .signature((String) params.get("Wechatpay-Signature"))
                    .timestamp((String) params.get("Wechatpay-Timestamp"))
                    .signType((String) params.get("Wechatpay-Signature-Type"))
                    .body(body)
                    .build();
            NotificationParser parser = new NotificationParser(config());
            parser.parse(requestParam, com.wechat.pay.java.service.payments.model.Transaction.class);
            return true;
        } catch (Exception e) {
            log.error("微信支付通知验签失败", e);
            return false;
        }
    }

    @Override
    public PaymentResult queryOrder(String payOrderNo) {
        return PaymentResult.success();
    }

    @Override
    public RefundResult refund(String payOrderNo, BigDecimal amount) {
        return RefundResult.fail("基础版未实现退款");
    }
}
