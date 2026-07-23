package com.ds.report.service;

import com.ds.report.vo.*;

import java.util.List;

public interface ReportService {

    DashboardVO getDashboardOverview();

    List<SalesReportVO> getSalesReport(String startDate, String endDate);

    List<MemberReportVO> getMemberReport(String startDate, String endDate);

    List<BonusReportVO> getBonusReport(String startDate, String endDate);

    TeamReportVO getTeamReport(Long memberId, String startDate, String endDate);
}