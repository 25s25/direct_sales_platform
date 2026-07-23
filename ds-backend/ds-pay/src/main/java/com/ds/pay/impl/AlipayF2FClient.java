package com.ds.pay.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.ds.common.exception.BusinessException;
import com.ds.pay.config.PayProperties;
import com.ds.pay.core.PayChannel;
import com.ds.pay.core.PayClient;
import com.ds.pay.core.PaymentResult;
import com.ds.pay.core.RefundResult;
import com.ds.pay.entity.PaymentOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayF2FClient implements PayClient {

    private final PayProperties payProperties;

    private AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                payProperties.alipayServerUrl(),
                payProperties.alipayAppId(),
                payProperties.alipayPrivateKey(),
                "json",
                "UTF-8",
                payProperties.alipayPublicKey(),
                payProperties.alipaySignType()
        );
    }

    @Override
    public PayChannel channel() {
        return PayChannel.ALIPAY_F2F;
    }

    @Override
    public PaymentResult createPayment(PaymentOrder order, Map<String, Object> extra) {
        AlipayClient client = alipayClient();
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        request.setNotifyUrl(payProperties.alipayNotifyUrl());
        request.setBizContent("{" +
                "\"out_trade_no\":\"" + order.getPayOrderNo() + "\"," +
                "\"total_amount\":\"" + order.getAmount().toPlainString() + "\"," +
                "\"subject\":\"订单支付-" + order.getPayOrderNo() + "\"" +
                "}");
        try {
            AlipayTradePrecreateResponse response = client.execute(request);
            if (!response.isSuccess()) {
                return PaymentResult.fail(response.getMsg());
            }
            PaymentResult result = PaymentResult.success();
            result.setPayOrderNo(order.getPayOrderNo());
            result.setQrCode(response.getQrCode());
            return result;
        } catch (AlipayApiException e) {
            log.error("支付宝当面付下单失败", e);
            return PaymentResult.fail(e.getMessage());
        }
    }

    @Override
    public boolean verifyNotify(Map<String, Object> params, String body) {
        try {
            return AlipaySignature.rsaCheckV1(
                    params.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey, e -> String.valueOf(e.getValue()))),
                    payProperties.alipayPublicKey(),
                    "UTF-8",
                    payProperties.alipaySignType()
            );
        } catch (AlipayApiException e) {
            log.error("支付宝通知验签失败", e);
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
