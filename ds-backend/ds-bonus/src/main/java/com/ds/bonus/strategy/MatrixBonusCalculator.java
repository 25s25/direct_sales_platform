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
 * 矩阵制奖金需要真实的矩阵结构数据（每层的宽度和深度限制、填满/溢出规则等）。
 * 当前没有专门的矩阵安置表来记录每个会员在矩阵中的位置。
 * 在实现真实的矩阵安置模式前，切换到此制度计算结果不准确，仅用于演示策略切换能力。
 */
@Slf4j
@Component
public class MatrixBonusCalculator implements BonusCalculator {

    private static final String PLAN_TYPE = "MATRIX";
    private static final int MATRIX_WIDTH = 3;
    private static final int MATRIX_DEPTH = 5;
    private static final BigDecimal LAYER_BONUS_RATE = new BigDecimal("0.05");

    @Override
    public String getPlanType() {
        return PLAN_TYPE;
    }

    @Override
    public List<BonusRecord> calculate(BonusCalcDTO dto) {
        List<BonusRecord> records = new ArrayList<>();
        String period = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        // Calculate layer bonus for each filled position in the matrix
        // In a real implementation, the matrix structure would be queried from DB
        BigDecimal orderPv = dto.getOrderPv() != null ? dto.getOrderPv() : BigDecimal.ZERO;
        List<BonusCalcDTO.RecommendMember> chain = dto.getRecommendChain();
        if (chain != null && !chain.isEmpty()) {
            int levelsToProcess = Math.min(chain.size(), MATRIX_DEPTH);
            for (int i = 0; i < levelsToProcess; i++) {
                BonusCalcDTO.RecommendMember ancestor = chain.get(i);
                int depth = ancestor.getDepth() != null ? ancestor.getDepth() : i + 1;

                // Simulate filled positions: width^(depth) positions possible
                int positionsAtDepth = (int) Math.pow(MATRIX_WIDTH, depth);
                int filledPositions = Math.min(positionsAtDepth, MATRIX_WIDTH);

                BigDecimal layerBonus = orderPv
                        .multiply(LAYER_BONUS_RATE)
                        .multiply(new BigDecimal(filledPositions))
                        .divide(new BigDecimal(positionsAtDepth), 2, RoundingMode.HALF_UP);

                if (layerBonus.compareTo(BigDecimal.ZERO) > 0) {
                    BonusRecord layerRecord = new BonusRecord();
                    layerRecord.setMemberId(ancestor.getMemberId());
                    layerRecord.setPeriod(period);
                    layerRecord.setBonusType("LAYER");
                    layerRecord.setAmount(layerBonus);
                    layerRecord.setSourceOrderId(dto.getOrderId());
                    layerRecord.setStatus(0);
                    records.add(layerRecord);
                    log.info("MATRIX: layer bonus={} for ancestorId={}, depth={}",
                            layerBonus, ancestor.getMemberId(), depth);
                }
            }
        }

        return records;
    }
}