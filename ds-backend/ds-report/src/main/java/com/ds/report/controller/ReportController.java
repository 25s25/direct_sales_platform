package com.ds.report.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.report.service.ReportService;
import com.ds.report.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "report:view", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/sales")
    public Result<List<SalesReportVO>> sales(@RequestParam String startDate,
                                              @RequestParam String endDate) {
        log.info("Sales report requested: startDate={}, endDate={}", startDate, endDate);
        List<SalesReportVO> list = reportService.getSalesReport(startDate, endDate);
        return Result.ok(list);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "report:view", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/member")
    public Result<List<MemberReportVO>> member(@RequestParam String startDate,
                                                @RequestParam String endDate) {
        log.info("Member report requested: startDate={}, endDate={}", startDate, endDate);
        List<MemberReportVO> list = reportService.getMemberReport(startDate, endDate);
        return Result.ok(list);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "report:view", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/bonus")
    public Result<List<BonusReportVO>> bonus(@RequestParam String startDate,
                                              @RequestParam String endDate) {
        log.info("Bonus report requested: startDate={}, endDate={}", startDate, endDate);
        List<BonusReportVO> list = reportService.getBonusReport(startDate, endDate);
        return Result.ok(list);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @GetMapping("/team")
    public Result<TeamReportVO> team(@RequestParam Long memberId,
                                      @RequestParam String startDate,
                                      @RequestParam String endDate) {
        if (memberId == null) {
            return Result.fail("会员ID不能为空");
        }
        long currentMemberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        if (!SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_ADMIN).hasPermission("system:manage") && !memberId.equals(currentMemberId)) {
            return Result.fail("无权查看该团队报表");
        }
        log.info("Team report requested: memberId={}, startDate={}, endDate={}", memberId, startDate, endDate);
        TeamReportVO vo = reportService.getTeamReport(memberId, startDate, endDate);
        return Result.ok(vo);
    }
}