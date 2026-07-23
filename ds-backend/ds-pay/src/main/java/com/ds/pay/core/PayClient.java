package com.ds.pay.core;

import com.ds.pay.entity.PaymentOrder;

import java.math.BigDecimal;
import java.util.Map;

public interface PayClient {

    PayChannel channel();

    PaymentResult createPayment(PaymentOrder order, Map<String, Object> extra);

    boolean verifyNotify(Map<String, Object> params, String body);

    PaymentResult queryOrder(String payOrderNo);

    RefundResult refund(String payOrderNo, BigDecimal amount);
}
