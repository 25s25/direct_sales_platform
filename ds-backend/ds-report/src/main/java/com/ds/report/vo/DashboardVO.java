package com.ds.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalMembers;

    private Long todayNewMembers;

    private Long totalOrders;

    private Long todayOrders;

    private Long pendingOrders;

    private BigDecimal totalSales;

    private BigDecimal todaySales;

    private BigDecimal monthSales;

    private BigDecimal totalBonus;

    private BigDecimal monthBonus;

    private List<SalesTrendVO> salesTrend;

    private List<MemberTrendVO> memberTrend;

    @Data
    public static class SalesTrendVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private BigDecimal amount;
    }

    @Data
    public static class MemberTrendVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private Long count;
    }
}