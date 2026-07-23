package com.ds.bonus.listener;

import com.ds.bonus.config.RabbitMQConfig;
import com.ds.bonus.service.BonusCalcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BonusOrderPaidListener {

    private final BonusCalcService bonusCalcService;

    @RabbitListener(queues = RabbitMQConfig.BONUS_ORDER_PAID_QUEUE)
    public void onOrderPaid(Long orderId) {
        log.info("收到订单支付成功消息，开始计算奖金: orderId={}", orderId);
        try {
            bonusCalcService.calculateForOrder(orderId);
        } catch (Exception e) {
            log.error("奖金计算失败: orderId={}", orderId, e);
            throw e;
        }
    }
}
