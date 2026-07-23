package com.ds.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String date;

    private Long newCount;

    private Long totalCount;
}