package com.ds.pay.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.exception.BusinessException;
import com.ds.common.result.Result;
import com.ds.finance.entity.WalletLog;
import com.ds.finance.service.WalletLogService;
import com.ds.member.entity.Member;
import com.ds.member.mapper.MemberMapper;
import com.ds.member.service.MemberService;
import com.ds.order.entity.Order;
import com.ds.order.mapper.OrderMapper;
import com.ds.order.service.OrderService;
import com.ds.pay.config.PayProperties;
import com.ds.pay.core.PayChannel;
import com.ds.pay.core.PayClient;
import com.ds.pay.core.PayStatus;
import com.ds.pay.core.PaymentResult;
import com.ds.pay.core.RefundResult;
import com.ds.pay.entity.PaymentOrder;
import com.ds.pay.mapper.PaymentOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final DateTimeFormatter PAY_ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentOrderMapper paymentOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final MemberService memberService;
    private final MemberMapper memberMapper;
    private final WalletLogService walletLogService;
    private final PayProperties payProperties;
    private final List<PayClient> payClients;

    private Map<PayChannel, PayClient> clientMap() {
        return payClients.stream().collect(Collectors.toMap(PayClient::channel, c -> c));
    }

    private PayClient getClient(PayChannel channel) {
        PayClient client = clientMap().get(channel);
        if (client == null) {
            throw new BusinessException("不支持的支付渠道: " + channel);
        }
        return client;
    }

    private boolean isChannelEnabled(PayChannel channel) {
        if (channel == PayChannel.WALLET) {
            return true;
        }
        return payProperties.isChannelEnabled(channel);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<PaymentResult> createPayment(String orderNo, PayChannel channel, Map<String, Object> extra) {
        if (!isChannelEnabled(channel)) {
            return Result.fail("支付渠道未启用");
        }

        long memberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        boolean isRecharge = orderNo != null && orderNo.startsWith("RECHARGE_");

        PaymentOrder paymentOrder;
        if (isRecharge) {
            paymentOrder = paymentOrderMapper.selectOne(
                    new LambdaQueryWrapper<PaymentOrder>()
                            .eq(PaymentOrder::getOrderNo, orderNo)
                            .eq(PaymentOrder::getMemberId, memberId));
            if (paymentOrder == null) {
                return Result.fail("充值订单不存在");
            }
            if (paymentOrder.getStatus() != null && paymentOrder.getStatus() != PayStatus.PENDING.getCode()) {
                return Result.fail("充值订单状态异常，无法支付");
            }
        } else {
            Order order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
            if (order == null) {
                return Result.fail("订单不存在");
            }
            if (!order.getMemberId().equals(memberId)) {
                return Result.fail("无权操作该订单");
            }
            if (order.getStatus() != 0) {
                return Result.fail("订单状态异常，无法支付");
            }

            paymentOrder = new PaymentOrder();
            paymentOrder.setPayOrderNo("PAY" + LocalDateTime.now().format(PAY_ORDER_NO_FORMAT) + RandomUtil.randomNumbers(6));
            paymentOrder.setOrderNo(orderNo);
            paymentOrder.setMemberId(memberId);
            paymentOrder.setAmount(order.getPayAmount());
            paymentOrder.setStatus(PayStatus.PENDING.getCode());
            paymentOrder.setChannel(channel.name());
            paymentOrderMapper.insert(paymentOrder);
        }

        if (StrUtil.isBlank(paymentOrder.getPayOrderNo())) {
            paymentOrder.setPayOrderNo("PAY" + LocalDateTime.now().format(PAY_ORDER_NO_FORMAT) + RandomUtil.randomNumbers(6));
        }
        paymentOrder.setChannel(channel.name());
        paymentOrderMapper.updateById(paymentOrder);

        PaymentResult result;
        if (isRecharge && channel == PayChannel.WALLET) {
            Result<Void> addResult = memberService.addWallet(memberId, paymentOrder.getAmount());
            if (!addResult.isSuccess()) {
                PaymentOrder update = new PaymentOrder();
                update.setId(paymentOrder.getId());
                update.setStatus(PayStatus.FAILED.getCode());
                paymentOrderMapper.updateById(update);
                return Result.fail(addResult.getMessage());
            }
            saveRechargeWalletLog(memberId, paymentOrder.getAmount(), paymentOrder.getId());
            result = PaymentResult.success();
            paymentOrder.setStatus(PayStatus.SUCCESS.getCode());
            paymentOrder.setPayTime(LocalDateTime.now());
        } else {
            PayClient client = getClient(channel);
            result = client.createPayment(paymentOrder, extra);
            if (!result.isSuccess()) {
                PaymentOrder update = new PaymentOrder();
                update.setId(paymentOrder.getId());
                update.setStatus(PayStatus.FAILED.getCode());
                paymentOrderMapper.updateById(update);
                return Result.fail(result.getMsg());
            }

            if (channel == PayChannel.WALLET) {
                paymentOrder.setStatus(PayStatus.SUCCESS.getCode());
                paymentOrder.setPayTime(LocalDateTime.now());
                orderService.paySuccess(orderNo);
            } else {
                paymentOrder.setStatus(PayStatus.PAYING.getCode());
            }
        }

        paymentOrderMapper.updateById(paymentOrder);
        result.setStatus(paymentOrder.getStatus());
        result.setPayOrderNo(paymentOrder.getPayOrderNo());
        return Result.ok(result);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<String> createRechargeOrder(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("充值金额必须大于0");
        }
        long memberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        String orderNo = "RECHARGE_" + System.currentTimeMillis();

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setPayOrderNo("PAY" + LocalDateTime.now().format(PAY_ORDER_NO_FORMAT) + RandomUtil.randomNumbers(6));
        paymentOrder.setOrderNo(orderNo);
        paymentOrder.setMemberId(memberId);
        paymentOrder.setAmount(amount);
        paymentOrder.setStatus(PayStatus.PENDING.getCode());
        paymentOrder.setChannel(PayChannel.WALLET.name());
        paymentOrderMapper.insert(paymentOrder);

        return Result.ok(orderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(PayChannel channel, Map<String, Object> params, String body) {
        PayClient client = getClient(channel);
        if (!client.verifyNotify(params, body)) {
            log.warn("支付回调验签失败: channel={}", channel);
            return "fail";
        }

        String payOrderNo = extractPayOrderNo(channel, params, body);
        if (payOrderNo == null || payOrderNo.isBlank()) {
            return "fail";
        }

        PaymentOrder paymentOrder = paymentOrderMapper.selectByPayOrderNo(payOrderNo);
        if (paymentOrder == null) {
            return "fail";
        }
        if (paymentOrder.getStatus() != null && paymentOrder.getStatus() == PayStatus.SUCCESS.getCode()) {
            return "success";
        }

        PaymentOrder update = new PaymentOrder();
        update.setId(paymentOrder.getId());
        update.setStatus(PayStatus.SUCCESS.getCode());
        update.setPayTime(LocalDateTime.now());
        update.setCallbackCount((paymentOrder.getCallbackCount() != null ? paymentOrder.getCallbackCount() : 0) + 1);
        if (StrUtil.isNotBlank(body)) {
            update.setCallbackResult(body);
        } else if (params != null && !params.isEmpty()) {
            update.setCallbackResult(params.toString());
        }
        paymentOrderMapper.updateById(update);

        if (paymentOrder.getOrderNo().startsWith("RECHARGE_")) {
            Result<Void> addResult = memberService.addWallet(paymentOrder.getMemberId(), paymentOrder.getAmount());
            if (!addResult.isSuccess()) {
                log.error("充值入账失败: payOrderNo={}, msg={}", payOrderNo, addResult.getMessage());
                throw new BusinessException("充值入账失败: " + addResult.getMessage());
            }
            saveRechargeWalletLog(paymentOrder.getMemberId(), paymentOrder.getAmount(), paymentOrder.getId());
        } else {
            orderService.paySuccess(paymentOrder.getOrderNo());
        }
        return "success";
    }

    private String extractPayOrderNo(PayChannel channel, Map<String, Object> params, String body) {
        if (channel.name().startsWith("WECHAT")) {
            return (String) params.get("out_trade_no");
        }
        return (String) params.get("out_trade_no");
    }

    private void saveRechargeWalletLog(Long memberId, BigDecimal amount, Long referenceId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            return;
        }
        BigDecimal balanceAfter = member.getWalletBalance() != null ? member.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal balanceBefore = balanceAfter.subtract(amount);
        WalletLog log = new WalletLog();
        log.setMemberId(memberId);
        log.setLogType("RECHARGE");
        log.setAmount(amount);
        log.setBalanceBefore(balanceBefore);
        log.setBalanceAfter(balanceAfter);
        log.setReferenceId(referenceId);
        log.setRemark("钱包充值");
        walletLogService.saveLog(log);
    }

    public Result<PaymentResult> queryPayment(String payOrderNo) {
        PaymentOrder paymentOrder = paymentOrderMapper.selectByPayOrderNo(payOrderNo);
        if (paymentOrder == null) {
            return Result.fail("支付订单不存在");
        }
        if (StrUtil.isBlank(paymentOrder.getChannel())) {
            return Result.fail("支付订单渠道信息缺失");
        }
        PayClient client = getClient(PayChannel.valueOf(paymentOrder.getChannel()));
        PaymentResult result = client.queryOrder(payOrderNo);
        result.setPayOrderNo(payOrderNo);
        result.setStatus(paymentOrder.getStatus());
        return Result.ok(result);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<RefundResult> refund(String payOrderNo, BigDecimal amount) {
        PaymentOrder paymentOrder = paymentOrderMapper.selectByPayOrderNo(payOrderNo);
        if (paymentOrder == null) {
            return Result.fail("支付订单不存在");
        }
        if (paymentOrder.getStatus() == null || paymentOrder.getStatus() != PayStatus.SUCCESS.getCode()) {
            return Result.fail("支付订单未成功，无法退款");
        }
        PayClient client = getClient(PayChannel.valueOf(paymentOrder.getChannel()));
        RefundResult result = client.refund(payOrderNo, amount);
        if (result.isSuccess()) {
            PaymentOrder update = new PaymentOrder();
            update.setId(paymentOrder.getId());
            update.setStatus(PayStatus.REFUNDED.getCode());
            paymentOrderMapper.updateById(update);
        }
        return Result.ok(result);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancel(String payOrderNo) {
        PaymentOrder paymentOrder = paymentOrderMapper.selectByPayOrderNo(payOrderNo);
        if (paymentOrder == null) {
            return Result.fail("支付订单不存在");
        }
        if (paymentOrder.getStatus() != null && paymentOrder.getStatus() == PayStatus.SUCCESS.getCode()) {
            return Result.fail("已支付订单无法取消");
        }
        PaymentOrder update = new PaymentOrder();
        update.setId(paymentOrder.getId());
        update.setStatus(PayStatus.FAILED.getCode());
        paymentOrderMapper.updateById(update);
        return Result.ok();
    }
}
