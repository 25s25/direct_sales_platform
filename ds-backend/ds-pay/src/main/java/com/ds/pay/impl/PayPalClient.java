package com.ds.pay.impl;

import com.ds.pay.config.PayProperties;
import com.ds.pay.core.PayChannel;
import com.ds.pay.core.PayClient;
import com.ds.pay.core.PaymentResult;
import com.ds.pay.core.RefundResult;
import com.ds.pay.entity.PaymentOrder;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayPalClient implements PayClient {

    private final PayProperties payProperties;

    private APIContext apiContext() {
        String mode = payProperties.paypalMode();
        if (mode == null || mode.isBlank()) {
            mode = "sandbox";
        }
        return new APIContext(payProperties.paypalClientId(), payProperties.paypalClientSecret(), mode);
    }

    @Override
    public PayChannel channel() {
        return PayChannel.PAYPAL;
    }

    @Override
    public PaymentResult createPayment(PaymentOrder order, Map<String, Object> extra) {
        Amount amount = new Amount();
        amount.setCurrency("USD");
        amount.setTotal(order.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());

        Transaction transaction = new Transaction();
        transaction.setDescription("Order " + order.getPayOrderNo());
        transaction.setAmount(amount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(payProperties.paypalCancelUrl());
        redirectUrls.setReturnUrl(payProperties.paypalReturnUrl());
        payment.setRedirectUrls(redirectUrls);

        try {
            Payment createdPayment = payment.create(apiContext());
            String approvalUrl = createdPayment.getLinks().stream()
                    .filter(link -> "approval_url".equals(link.getRel()))
                    .findFirst()
                    .map(Links::getHref)
                    .orElse(null);

            PaymentResult result = PaymentResult.success();
            result.setPayOrderNo(order.getPayOrderNo());
            result.setApprovalUrl(approvalUrl);
            result.setThirdOrderNo(createdPayment.getId());
            return result;
        } catch (PayPalRESTException e) {
            log.error("PayPal下单失败", e);
            return PaymentResult.fail(e.getMessage());
        }
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
        return RefundResult.fail("基础版未实现退款");
    }
}
