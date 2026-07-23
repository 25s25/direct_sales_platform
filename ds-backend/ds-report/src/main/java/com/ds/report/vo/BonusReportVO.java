package com.ds.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BonusReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String bonusType;

    private BigDecimal amount;

    private Long count;
}