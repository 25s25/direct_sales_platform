package com.ds.bonus.strategy;

import com.ds.bonus.dto.BonusCalcDTO;
import com.ds.bonus.entity.BonusRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifferentialBonusCalculatorTest {

    private final DifferentialBonusCalculator calculator = new DifferentialBonusCalculator();

    @Test
    void getPlanType_returnsDifferential() {
        assertEquals("DIFFERENTIAL", calculator.getPlanType());
    }

    @Test
    void calculate_generatesRetailRecordForNonZeroOrderAmount() {
        BonusCalcDTO dto = BonusCalcDTO.builder()
                .orderId(1001L)
                .memberId(50L)
                .orderAmount(new BigDecimal("1000.00"))
                .orderPv(new BigDecimal("500.00"))
                .memberLevel("GOLD")
                .recommendChain(new ArrayList<>())
                .build();

        List<BonusRecord> records = calculator.calculate(dto);

        assertNotNull(records);
        assertEquals(1, records.size(), "只有一笔零利润奖金记录");
        BonusRecord retail = records.get(0);
        assertEquals("RETAIL", retail.getBonusType());
        assertEquals(50L, retail.getMemberId());
        assertEquals(0, new BigDecimal("50.00").compareTo(retail.getAmount()),
                "零售利润应为 1000 * 5% = 50.00");
        assertEquals(1001L, retail.getSourceOrderId());
        assertEquals(0, retail.getStatus());
        assertEquals(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM")), retail.getPeriod());
    }

    @Test
    void calculate_skipsRetailRecordWhenOrderAmountIsZero() {
        BonusCalcDTO dto = BonusCalcDTO.builder()
                .orderId(1002L)
                .memberId(51L)
                .orderAmount(BigDecimal.ZERO)
                .orderPv(BigDecimal.ZERO)
                .memberLevel("NORMAL")
                .recommendChain(new ArrayList<>())
                .build();

        List<BonusRecord> records = calculator.calculate(dto);

        assertTrue(records.isEmpty(), "订单金额为 0 时不应产生任何奖金记录");
    }

    @Test
    void calculate_generatesRecommendRecordsBasedOnRateDifferential() {
        // 链路：depth=1 推荐人 30% 奖金利率，depth=2 推荐人 20%
        // depth=1 差值 = 30% - 20% = 10%，PV=500 → 推荐奖 = 50
        // depth=2 是末端，下一档视为 0，差值 = 20% - 0 = 20%，PV=500 → 推荐奖 = 100
        List<BonusCalcDTO.RecommendMember> chain = new ArrayList<>();
        chain.add(BonusCalcDTO.RecommendMember.builder()
                .memberId(11L).level("DIAMOND").pv(new BigDecimal("2000"))
                .depth(1).bonusRate(new BigDecimal("0.30")).build());
        chain.add(BonusCalcDTO.RecommendMember.builder()
                .memberId(12L).level("GOLD").pv(new BigDecimal("1000"))
                .depth(2).bonusRate(new BigDecimal("0.20")).build());

        BonusCalcDTO dto = BonusCalcDTO.builder()
                .orderId(2001L)
                .memberId(99L)
                .orderAmount(new BigDecimal("800.00"))
                .orderPv(new BigDecimal("500.00"))
                .memberLevel("NORMAL")
                .recommendChain(chain)
                .build();

        List<BonusRecord> records = calculator.calculate(dto);

        // 期望：1 笔 RETAIL + 2 笔 RECOMMEND（depth=1 与 depth=2 差值均 > 0）
        assertEquals(3, records.size());
        long recommendCount = records.stream()
                .filter(r -> "RECOMMEND".equals(r.getBonusType())).count();
        assertEquals(2, recommendCount);

        BonusRecord depth1 = records.stream()
                .filter(r -> "RECOMMEND".equals(r.getBonusType()) && r.getMemberId() == 11L)
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("50.00").compareTo(depth1.getAmount()),
                "depth=1 推荐奖 = PV(500) * 差值(0.10) = 50.00");

        BonusRecord depth2 = records.stream()
                .filter(r -> "RECOMMEND".equals(r.getBonusType()) && r.getMemberId() == 12L)
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo(depth2.getAmount()),
                "depth=2 推荐奖 = PV(500) * 差值(0.20) = 100.00（末端 next 视为 0）");
    }

    @Test
    void calculate_skipsRecommendWhenRateDiffIsNotPositive() {
        // depth=1: 10% - 20% = -10% → 跳过
        // depth=2: 20% - 0 = 20% → 写入
        List<BonusCalcDTO.RecommendMember> chain = new ArrayList<>();
        chain.add(BonusCalcDTO.RecommendMember.builder()
                .memberId(21L).level("NORMAL").depth(1)
                .bonusRate(new BigDecimal("0.10")).build());
        chain.add(BonusCalcDTO.RecommendMember.builder()
                .memberId(22L).level("GOLD").depth(2)
                .bonusRate(new BigDecimal("0.20")).build());

        BonusCalcDTO dto = BonusCalcDTO.builder()
                .orderId(2002L)
                .memberId(100L)
                .orderAmount(new BigDecimal("500.00"))
                .orderPv(new BigDecimal("200.00"))
                .recommendChain(chain)
                .build();

        List<BonusRecord> records = calculator.calculate(dto);

        // 1 笔 RETAIL + 1 笔 RECOMMEND（depth=2 末端）
        assertEquals(2, records.size());
        BonusRecord recommend = records.stream()
                .filter(r -> "RECOMMEND".equals(r.getBonusType()))
                .findFirst().orElseThrow();
        assertEquals(22L, recommend.getMemberId());
    }

    @Test
    void calculate_capsRecommendChainAtThreeLevels() {
        // 5 层链，只前 3 层有机会被处理
        List<BonusCalcDTO.RecommendMember> chain = new ArrayList<>();
        BigDecimal[] rates = {new BigDecimal("0.40"), new BigDecimal("0.30"),
                new BigDecimal("0.20"), new BigDecimal("0.10"), new BigDecimal("0.05")};
        for (int i = 0; i < 5; i++) {
            chain.add(BonusCalcDTO.RecommendMember.builder()
                    .memberId(100L + i).depth(i + 1).bonusRate(rates[i]).build());
        }

        BonusCalcDTO dto = BonusCalcDTO.builder()
                .orderId(2003L)
                .memberId(200L)
                .orderAmount(BigDecimal.ZERO)
                .orderPv(new BigDecimal("1000.00"))
                .recommendChain(chain)
                .build();

        List<BonusRecord> records = calculator.calculate(dto);

        // depth=1 (差0.10)、depth=2 (差0.10)、depth=3 (差0.10) 各 1 笔；depth=4、5 不处理
        long recommendCount = records.stream()
                .filter(r -> "RECOMMEND".equals(r.getBonusType())).count();
        assertEquals(3, recommendCount, "最多处理 3 层推荐链");
    }

    @Test
    void calculate_handlesNullOrderAmountAndNullChain() {
        BonusCalcDTO dto = BonusCalcDTO.builder()
                .orderId(2004L)
                .memberId(300L)
                .orderAmount(null)
                .orderPv(null)
                .recommendChain(null)
                .build();

        List<BonusRecord> records = calculator.calculate(dto);

        // amount null 视为 0，chain null 直接跳过
        assertTrue(records.isEmpty());
    }
}
