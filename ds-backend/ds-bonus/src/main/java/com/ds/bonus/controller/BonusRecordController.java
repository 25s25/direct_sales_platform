package com.ds.bonus.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.constant.SaTokenConsts;
import com.ds.bonus.entity.BonusRecord;
import com.ds.bonus.service.BonusRecordService;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bonus/record")
@RequiredArgsConstructor
public class BonusRecordController {

    private final BonusRecordService bonusRecordService;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "bonus:record:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/page")
    public Result<PageResult<BonusRecord>> page(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) Long memberId,
                                                 @RequestParam(required = false) String period,
                                                 @RequestParam(required = false) String bonusType) {
        Page<BonusRecord> pageParam = new Page<>(page, size);
        Page<BonusRecord> result = bonusRecordService.page(pageParam, memberId, period, bonusType);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @GetMapping("/my")
    public Result<List<BonusRecord>> my() {
        long memberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        return Result.ok(bonusRecordService.getByMemberId(memberId));
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "bonus:record:grant", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/grant")
    public Result<Void> grant(@RequestBody List<Long> recordIds) {
        bonusRecordService.grant(recordIds);
        return Result.ok();
    }
}