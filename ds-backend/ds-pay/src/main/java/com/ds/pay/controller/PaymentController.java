package com.ds.pay.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.pay.core.PayChannel;
import com.ds.pay.core.PaymentResult;
import com.ds.pay.core.RefundResult;
import com.ds.pay.service.PaymentService;
import com.ds.pay.vo.PayCreateRequest;
import com.ds.pay.vo.RechargeCreateRequest;
import com.ds.pay.vo.PayRefundRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PostMapping("/create")
    public Result<PaymentResult> create(@Valid @RequestBody PayCreateRequest request) {
        PayChannel channel = PayChannel.from(request.getChannel());
        return paymentService.createPayment(request.getOrderNo(), channel, request.getExtra());
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PostMapping("/recharge")
    public Result<String> recharge(@Valid @RequestBody RechargeCreateRequest request) {
        return paymentService.createRechargeOrder(request.getAmount());
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @GetMapping("/query/{payOrderNo}")
    public Result<PaymentResult> query(@PathVariable String payOrderNo) {
        return paymentService.queryPayment(payOrderNo);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PostMapping("/cancel/{payOrderNo}")
    public Result<Void> cancel(@PathVariable String payOrderNo) {
        return paymentService.cancel(payOrderNo);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PostMapping("/refund")
    public Result<RefundResult> refund(@Valid @RequestBody PayRefundRequest request) {
        return paymentService.refund(request.getPayOrderNo(), request.getAmount());
    }
}
