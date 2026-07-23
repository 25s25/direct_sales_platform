package com.ds.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long memberId;

    private String memberNo;

    private String realName;

    private BigDecimal totalSales;

    private Long teamMemberCount;

    private BigDecimal selfSales;
}