package com.ds.finance.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.ds.common.constant.SaTokenConsts;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.finance.entity.WalletLog;
import com.ds.finance.service.WalletLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/finance/wallet")
@RequiredArgsConstructor
public class WalletLogController {

    private final WalletLogService walletLogService;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "finance:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/logs")
    public Result<PageResult<WalletLog>> logs(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Long memberId,
                                               @RequestParam(required = false) String type) {
        Page<WalletLog> pageParam = new Page<>(page, size);
        Page<WalletLog> result = walletLogService.page(pageParam, memberId, type);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @GetMapping("/logs/my")
    public Result<PageResult<WalletLog>> myLogs(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        long memberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        Page<WalletLog> pageParam = new Page<>(page, size);
        Page<WalletLog> result = walletLogService.page(pageParam, memberId, null);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }
}