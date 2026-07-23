package com.ds.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ds.common.config.DsBonusProperties;
import com.ds.common.result.Result;
import com.ds.member.mapper.MemberMapper;
import com.ds.member.mapper.MemberPathMapper;
import com.ds.member.service.MemberService;
import com.ds.order.entity.Order;
import com.ds.order.entity.OrderItem;
import com.ds.order.mapper.OrderBonusMapper;
import com.ds.order.mapper.OrderItemMapper;
import com.ds.order.mapper.OrderMapper;
import com.ds.order.mapper.OrderReturnMapper;
import com.ds.product.mapper.ProductMapper;
import com.ds.product.mapper.ProductSkuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceStatusTest {

    @Mock private OrderItemMapper orderItemMapper;
    @Mock private OrderBonusMapper orderBonusMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ProductSkuMapper productSkuMapper;
    @Mock private MemberMapper memberMapper;
    @Mock private MemberPathMapper memberPathMapper;
    @Mock private MemberService memberService;
    @Mock private OrderMapper orderMapper;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private OrderReturnMapper orderReturnMapper;

    private OrderServiceImpl orderService;
    private DsBonusProperties bonusProperties;

    @BeforeEach
    void setUp() {
        bonusProperties = new DsBonusProperties();
        bonusProperties.setRecommendRate(new BigDecimal("0.10"));
        orderService = new OrderServiceImpl(
                orderItemMapper, orderBonusMapper, productMapper, productSkuMapper,
                memberMapper, memberPathMapper, memberService, bonusProperties, rabbitTemplate,
                orderReturnMapper);
        // ServiceImpl<OrderMapper, Order> 需要 baseMapper 不为 null
        ReflectionTestUtils.setField(orderService, "baseMapper", orderMapper);
        lenient().when(orderMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        // 让 updateById 默认成功（返回 1），各 case 可覆盖
        lenient().when(orderMapper.updateById(any(Order.class))).thenReturn(1);
        // 默认扣款/退款都成功
        lenient().when(memberService.deductWallet(any(), any())).thenReturn(Result.ok());
        lenient().when(memberService.addWallet(any(), any())).thenReturn(Result.ok());
    }

    private Order givenOrderWithStatus(int status) {
        Order order = new Order();
        order.setId(100L);
        order.setOrderNo("ORDER-100");
        order.setMemberId(1L);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setPayAmount(new BigDecimal("100.00"));
        order.setStatus(status);
        when(orderMapper.selectById(100L)).thenReturn(order);
        return order;
    }

    // ---- pay() ----

    @Test
    void pay_succeeds_whenStatusIsPending() {
        givenOrderWithStatus(0); // PENDING

        Result<Void> result = orderService.pay(100L);

        assertTrue(result.isSuccess(), "待支付状态应允许支付: " + result.getMessage());
        verify(memberService, times(1)).deductWallet(1L, new BigDecimal("100.00"));
    }

    @Test
    void pay_fails_whenStatusIsAlreadyPaid() {
        givenOrderWithStatus(1); // PAID

        Result<Void> result = orderService.pay(100L);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("无法支付"));
        verify(memberService, never()).deductWallet(any(), any());
    }

    @Test
    void pay_fails_whenStatusIsShipped() {
        givenOrderWithStatus(2); // SHIPPED

        Result<Void> result = orderService.pay(100L);

        assertFalse(result.isSuccess());
    }

    @Test
    void pay_fails_whenStatusIsCancelled() {
        givenOrderWithStatus(5); // CANCELLED

        Result<Void> result = orderService.pay(100L);

        assertFalse(result.isSuccess());
    }

    @Test
    void pay_fails_whenOrderDoesNotExist() {
        when(orderMapper.selectById(999L)).thenReturn(null);

        Result<Void> result = orderService.pay(999L);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("订单不存在"));
    }

    // ---- ship() ----

    @Test
    void ship_succeeds_whenStatusIsPaid() {
        givenOrderWithStatus(1); // PAID

        Result<Void> result = orderService.ship(100L, "顺丰", "SF123456");

        assertTrue(result.isSuccess());
    }

    @Test
    void ship_fails_whenStatusIsPending() {
        givenOrderWithStatus(0); // PENDING

        Result<Void> result = orderService.ship(100L, "顺丰", "SF123456");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("无法发货"));
    }

    @Test
    void ship_fails_whenStatusIsReceived() {
        givenOrderWithStatus(3); // RECEIVED

        Result<Void> result = orderService.ship(100L, "顺丰", "SF123456");

        assertFalse(result.isSuccess());
    }

    // ---- receive() ----

    @Test
    void receive_succeeds_whenStatusIsShipped() {
        givenOrderWithStatus(2); // SHIPPED

        Result<Void> result = orderService.receive(100L);

        assertTrue(result.isSuccess());
    }

    @Test
    void receive_fails_whenStatusIsPending() {
        givenOrderWithStatus(0);

        Result<Void> result = orderService.receive(100L);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("无法确认收货"));
    }

    @Test
    void receive_fails_whenStatusIsCancelled() {
        givenOrderWithStatus(5);

        Result<Void> result = orderService.receive(100L);

        assertFalse(result.isSuccess());
    }

    // ---- cancel() ----

    @Test
    void cancel_succeeds_whenStatusIsPending_withoutRefund() {
        givenOrderWithStatus(0);
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        Result<Void> result = orderService.cancel(100L);

        assertTrue(result.isSuccess());
        verify(memberService, never()).addWallet(any(), any());
    }

    @Test
    void cancel_succeeds_whenStatusIsPaid_withRefund() {
        givenOrderWithStatus(1);
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(memberService.addWallet(any(), any())).thenReturn(Result.ok());

        Result<Void> result = orderService.cancel(100L);

        assertTrue(result.isSuccess());
        verify(memberService, times(1)).addWallet(1L, new BigDecimal("100.00"));
    }

    @Test
    void cancel_fails_whenStatusIsShipped() {
        givenOrderWithStatus(2);

        Result<Void> result = orderService.cancel(100L);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("无法取消"));
    }

    @Test
    void cancel_fails_whenStatusIsReceived() {
        givenOrderWithStatus(3);

        Result<Void> result = orderService.cancel(100L);

        assertFalse(result.isSuccess());
    }

    @Test
    void cancel_fails_whenStatusIsAlreadyCancelled() {
        givenOrderWithStatus(5);

        Result<Void> result = orderService.cancel(100L);

        assertFalse(result.isSuccess());
    }
}
