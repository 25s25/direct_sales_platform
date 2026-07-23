package com.ds.finance.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ds.common.constant.SaTokenConsts;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.finance.dto.WithdrawApplyDTO;
import com.ds.finance.entity.Withdraw;
import com.ds.finance.service.WithdrawService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/finance/withdraw")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody WithdrawApplyDTO dto) {
        return withdrawService.apply(dto);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "finance:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/page")
    public Result<PageResult<Withdraw>> page(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) Integer status) {
        Page<Withdraw> pageParam = new Page<>(page, size);
        return withdrawService.page(pageParam, null, status);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @GetMapping("/my")
    public Result<PageResult<Withdraw>> my(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        long memberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        Page<Withdraw> pageParam = new Page<>(page, size);
        return withdrawService.page(pageParam, memberId, null);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "finance:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id,
                               @RequestParam Integer status,
                               @RequestParam(required = false) String remark) {
        return withdrawService.audit(id, status, remark);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "finance:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/{id}/grant")
    public Result<Void> grant(@PathVariable Long id) {
        return withdrawService.grant(id);
    }
}