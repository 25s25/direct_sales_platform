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
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatNativeClient implements PayClient {

    private final PayProperties payProperties;

    private RSAAutoCertificateConfig config() {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(payProperties.wechatMchId())
                .privateKey(payProperties.wechatPrivateKey())
                .merchantSerialNumber(payProperties.wechatMchSerialNo())
                .apiV3Key(payProperties.wechatApiV3Key())
                .build();
    }

    private NativePayService service() {
        return new NativePayService.Builder().config(config()).build();
    }

    @Override
    public PayChannel channel() {
        return PayChannel.WECHAT_NATIVE;
    }

    @Override
    public PaymentResult createPayment(PaymentOrder order, Map<String, Object> extra) {
        PrepayRequest request = new PrepayRequest();
        request.setAppid(payProperties.wechatAppId());
        request.setMchid(payProperties.wechatMchId());
        request.setDescription("订单支付-" + order.getPayOrderNo());
        request.setOutTradeNo(order.getPayOrderNo());
        request.setNotifyUrl(payProperties.wechatNotifyUrl());

        Amount amount = new Amount();
        amount.setTotal(order.getAmount().multiply(new BigDecimal("100")).intValue());
        request.setAmount(amount);

        try {
            PrepayResponse response = service().prepay(request);
            PaymentResult result = PaymentResult.success();
            result.setPayOrderNo(order.getPayOrderNo());
            result.setCodeUrl(response.getCodeUrl());
            return result;
        } catch (ServiceException e) {
            log.error("微信支付Native下单失败", e);
            return PaymentResult.fail(e.getMessage());
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
