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
 * 双轨制奖金需要真实的团队安置结构数据（左右区 PV、对碰比例、封顶值等）。
 * 当前 ds_member.position 字段未实际使用，也没有专门的左右区业绩汇总表。
 * 在实现真实的双轨安置模式前，切换到此制度计算结果不准确，仅用于演示策略切换能力。
 */
@Slf4j
@Component
public class BinaryBonusCalculator implements BonusCalculator {

    private static final String PLAN_TYPE = "BINARY";
    private static final BigDecimal MATCH_RATE = new BigDecimal("0.10");

    @Override
    public String getPlanType() {
        return PLAN_TYPE;
    }

    @Override
    public List<BonusRecord> calculate(BonusCalcDTO dto) {
        List<BonusRecord> records = new ArrayList<>();
        String period = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        // In a real implementation, leftPv and rightPv would be fetched from
        // the member's binary tree placement data. Here we simulate the calculation.
        BigDecimal leftPv = BigDecimal.ZERO;
        BigDecimal rightPv = BigDecimal.ZERO;

        List<BonusCalcDTO.RecommendMember> chain = dto.getRecommendChain();
        if (chain != null && !chain.isEmpty()) {
            // Simulate: the immediate parent gets match bonus based on left/right PV
            for (BonusCalcDTO.RecommendMember member : chain) {
                if (member.getDepth() != null && member.getDepth() == 1) {
                    // Use the order PV as contribution to the parent's weaker leg
                    leftPv = dto.getOrderPv() != null ? dto.getOrderPv() : BigDecimal.ZERO;
                    rightPv = BigDecimal.ZERO;
                }
            }
        }

        BigDecimal matchPv = leftPv.min(rightPv);
        if (matchPv.compareTo(BigDecimal.ZERO) > 0) {
            BonusRecord matchRecord = new BonusRecord();
            matchRecord.setMemberId(dto.getMemberId());
            matchRecord.setPeriod(period);
            matchRecord.setBonusType("MATCH");
            matchRecord.setAmount(matchPv.multiply(MATCH_RATE).setScale(2, RoundingMode.HALF_UP));
            matchRecord.setSourceOrderId(dto.getOrderId());
            matchRecord.setStatus(0);
            records.add(matchRecord);
            log.info("BINARY: match bonus={} for memberId={}, leftPv={}, rightPv={}",
                    matchRecord.getAmount(), dto.getMemberId(), leftPv, rightPv);
        }

        return records;
    }
}