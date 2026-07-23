package com.ds.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String date;

    private BigDecimal sales;

    private Long orderCount;
}