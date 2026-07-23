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

@Slf4j
@Component
public class DifferentialBonusCalculator implements BonusCalculator {

    private static final String PLAN_TYPE = "DIFFERENTIAL";
    private static final int MAX_RECOMMEND_LEVELS = 3;
    private static final BigDecimal RETAIL_BONUS_RATE = new BigDecimal("0.05");

    @Override
    public String getPlanType() {
        return PLAN_TYPE;
    }

    @Override
    public List<BonusRecord> calculate(BonusCalcDTO dto) {
        List<BonusRecord> records = new ArrayList<>();
        String period = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        // 1. Retail profit: orderAmount * member_bonus_rate%
        BigDecimal orderAmount = dto.getOrderAmount() != null ? dto.getOrderAmount() : BigDecimal.ZERO;
        BigDecimal orderPv = dto.getOrderPv() != null ? dto.getOrderPv() : BigDecimal.ZERO;
        BigDecimal retailProfit = orderAmount.multiply(RETAIL_BONUS_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        if (retailProfit.compareTo(BigDecimal.ZERO) > 0) {
            BonusRecord retailRecord = new BonusRecord();
            retailRecord.setMemberId(dto.getMemberId());
            retailRecord.setPeriod(period);
            retailRecord.setBonusType("RETAIL");
            retailRecord.setAmount(retailProfit);
            retailRecord.setSourceOrderId(dto.getOrderId());
            retailRecord.setStatus(0);
            records.add(retailRecord);
            log.info("DIFFERENTIAL: retail profit={} for memberId={}", retailProfit, dto.getMemberId());
        }

        // 2. Recommend bonus: rate difference between levels for recommendChain (max 3 levels)
        List<BonusCalcDTO.RecommendMember> chain = dto.getRecommendChain();
        if (chain != null && !chain.isEmpty()) {
            int levelsToProcess = Math.min(chain.size(), MAX_RECOMMEND_LEVELS);
            for (int i = 0; i < levelsToProcess; i++) {
                BonusCalcDTO.RecommendMember current = chain.get(i);
                BigDecimal currentRate = current.getBonusRate() != null ? current.getBonusRate() : BigDecimal.ZERO;

                BigDecimal nextRate = BigDecimal.ZERO;
                if (i + 1 < chain.size()) {
                    BonusCalcDTO.RecommendMember next = chain.get(i + 1);
                    nextRate = next.getBonusRate() != null ? next.getBonusRate() : BigDecimal.ZERO;
                }

                BigDecimal rateDiff = currentRate.subtract(nextRate);
                if (rateDiff.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal recommendBonus = orderPv.multiply(rateDiff)
                            .setScale(2, RoundingMode.HALF_UP);
                    if (recommendBonus.compareTo(BigDecimal.ZERO) > 0) {
                        BonusRecord recommendRecord = new BonusRecord();
                        recommendRecord.setMemberId(current.getMemberId());
                        recommendRecord.setPeriod(period);
                        recommendRecord.setBonusType("RECOMMEND");
                        recommendRecord.setAmount(recommendBonus);
                        recommendRecord.setSourceOrderId(dto.getOrderId());
                        recommendRecord.setStatus(0);
                        records.add(recommendRecord);
                        log.info("DIFFERENTIAL: recommend bonus={} for ancestorId={}, depth={}, rateDiff={}",
                                recommendBonus, current.getMemberId(), current.getDepth(), rateDiff);
                    }
                }
            }
        }

        return records;
    }
}