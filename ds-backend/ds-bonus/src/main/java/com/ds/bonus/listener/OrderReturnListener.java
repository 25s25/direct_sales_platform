package com.ds.bonus.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ds.bonus.entity.BonusRecord;
import com.ds.bonus.mapper.BonusRecordMapper;
import com.ds.common.exception.BusinessException;
import com.ds.finance.entity.WalletLog;
import com.ds.finance.service.WalletLogService;
import com.ds.member.entity.Member;
import com.ds.member.mapper.MemberMapper;
import com.ds.order.entity.Order;
import com.ds.order.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReturnListener {

    private final BonusRecordMapper bonusRecordMapper;
    private final MemberMapper memberMapper;
    private final WalletLogService walletLogService;
    private final OrderMapper orderMapper;

    @RabbitListener(queues = "ds.bonus.order.returned.queue")
    public void onOrderReturned(Long orderId, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.info("收到退货奖金回退消息，orderId={}", orderId);
            processReturn(orderId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理退货奖金回退消息失败，orderId={}", orderId, e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private void processReturn(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("订单不存在，orderId={}", orderId);
            return;
        }

        List<BonusRecord> bonusRecords = bonusRecordMapper.selectList(
                new LambdaQueryWrapper<BonusRecord>()
                        .eq(BonusRecord::getSourceOrderId, orderId)
                        .eq(BonusRecord::getStatus, 1));

        for (BonusRecord bonus : bonusRecords) {
            Member bonusMember = memberMapper.selectById(bonus.getMemberId());
            if (bonusMember != null) {
                BigDecimal balanceBefore = bonusMember.getWalletBalance() != null ? bonusMember.getWalletBalance() : BigDecimal.ZERO;
                BigDecimal balanceAfter = balanceBefore.subtract(bonus.getAmount());

                Member updateBonusMember = new Member();
                updateBonusMember.setId(bonusMember.getId());
                updateBonusMember.setWalletBalance(balanceAfter);
                updateBonusMember.setVersion(bonusMember.getVersion());
                int updated = memberMapper.updateById(updateBonusMember);
                if (updated <= 0) {
                    throw new BusinessException("退货回退奖金时会员钱包更新失败，memberId=" + bonusMember.getId());
                }

                WalletLog walletLog = new WalletLog();
                walletLog.setMemberId(bonusMember.getId());
                walletLog.setLogType("BONUS_DEDUCT");
                walletLog.setAmount(bonus.getAmount().negate());
                walletLog.setBalanceBefore(balanceBefore);
                walletLog.setBalanceAfter(balanceAfter);
                walletLog.setReferenceId(bonus.getId());
                walletLog.setRemark("退货回退奖金，来源订单：" + order.getOrderNo());
                walletLog.setCreateTime(LocalDateTime.now());
                walletLogService.saveLog(walletLog);
            }

            BonusRecord updateBonus = new BonusRecord();
            updateBonus.setId(bonus.getId());
            updateBonus.setStatus(2);
            bonusRecordMapper.updateById(updateBonus);
        }

        log.info("退货奖金回退完成，orderId={}，回退记录数={}", orderId, bonusRecords.size());
    }
}
