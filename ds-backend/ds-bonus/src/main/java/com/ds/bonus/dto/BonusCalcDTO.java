package com.ds.bonus.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BonusCalcDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;

    private Long memberId;

    private BigDecimal orderAmount;

    private BigDecimal orderPv;

    private String memberLevel;

    private List<RecommendMember> recommendChain;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendMember implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long memberId;

        private String level;

        private BigDecimal pv;

        private Integer depth;

        private BigDecimal bonusRate;
    }
}