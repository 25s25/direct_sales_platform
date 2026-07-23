package com.ds.report.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.report.service.ReportService;
import com.ds.report.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/report/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ReportService reportService;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "system:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/overview")
    public Result<DashboardVO> overview() {
        log.info("Dashboard overview requested");
        DashboardVO vo = reportService.getDashboardOverview();
        return Result.ok(vo);
    }
}