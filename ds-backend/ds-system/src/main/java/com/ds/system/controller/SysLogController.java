package com.ds.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.system.entity.SysLog;
import com.ds.system.service.SysLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/system/log")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService sysLogService;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "system:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/page")
    public Result<PageResult<SysLog>> page(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate) {
        return sysLogService.page(new Page<>(page, size), keyword, startDate, endDate);
    }
}