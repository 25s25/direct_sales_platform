package com.ds.pay.impl;

import com.ds.common.result.Result;
import com.ds.member.service.MemberService;
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
public class WalletPayClient implements PayClient {

    private final MemberService memberService;

    @Override
    public PayChannel channel() {
        return PayChannel.WALLET;
    }

    @Override
    public PaymentResult createPayment(PaymentOrder order, Map<String, Object> extra) {
        Result<Void> result = memberService.deductWallet(order.getMemberId(), order.getAmount());
        if (!result.isSuccess()) {
            return PaymentResult.fail(result.getMessage());
        }
        PaymentResult paymentResult = PaymentResult.success();
        paymentResult.setPayOrderNo(order.getPayOrderNo());
        return paymentResult;
    }

    @Override
    public boolean verifyNotify(Map<String, Object> params, String body) {
        return true;
    }

    @Override
    public PaymentResult queryOrder(String payOrderNo) {
        return PaymentResult.success();
    }

    @Override
    public RefundResult refund(String payOrderNo, BigDecimal amount) {
        return RefundResult.fail("钱包支付暂不支持退款");
    }
}
