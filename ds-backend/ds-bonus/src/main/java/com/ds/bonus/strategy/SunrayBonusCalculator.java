package com.ds.bonus.strategy;

import com.ds.bonus.dto.BonusCalcDTO;
import com.ds.bonus.entity.BonusRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO: 当前为占位实现。
 * 太阳线奖金需要真实的太阳线层级关系数据（每个直接推荐作为一条独立线，按线业绩发奖金）。
 * 当前仅使用推荐链中的 depth=1 节点模拟计算，未按每条线的累计业绩汇总。
 * 在实现真实的太阳线团队数据前，切换到此制度计算结果不准确，仅用于演示策略切换能力。
 */
@Slf4j
@Component
public class SunrayBonusCalculator implements BonusCalculator {

    private static final String PLAN_TYPE = "SUNRAY";
    private static final BigDecimal LINE_BONUS_RATE = new BigDecimal("0.08");

    @Override
    public String getPlanType() {
        return PLAN_TYPE;
    }

    @Override
    public List<BonusRecord> calculate(BonusCalcDTO dto) {
        List<BonusRecord> records = new ArrayList<>();
        String period = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        // Sunray: each direct referral is a separate line
        // Calculate bonus per line based on line performance
        BigDecimal orderPv = dto.getOrderPv() != null ? dto.getOrderPv() : BigDecimal.ZERO;
        List<BonusCalcDTO.RecommendMember> chain = dto.getRecommendChain();
        if (chain != null && !chain.isEmpty()) {
            for (BonusCalcDTO.RecommendMember member : chain) {
                if (member.getDepth() == null) {
                    continue;
                }
                // Direct referrals (depth=1) get line bonus based on line performance
                if (member.getDepth() == 1) {
                    BigDecimal lineBonus = orderPv
                            .multiply(LINE_BONUS_RATE)
                            .setScale(2, RoundingMode.HALF_UP);

                    if (lineBonus.compareTo(BigDecimal.ZERO) > 0) {
                        BonusRecord leaderRecord = new BonusRecord();
                        leaderRecord.setMemberId(member.getMemberId());
                        leaderRecord.setPeriod(period);
                        leaderRecord.setBonusType("LEADER");
                        leaderRecord.setAmount(lineBonus);
                        leaderRecord.setSourceOrderId(dto.getOrderId());
                        leaderRecord.setStatus(0);
                        records.add(leaderRecord);
                        log.info("SUNRAY: line leader bonus={} for memberId={}, linePv={}",
                                lineBonus, member.getMemberId(), dto.getOrderPv());
                    }
                }
            }
        }

        return records;
    }
}