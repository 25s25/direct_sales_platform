package com.ds.bonus.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ds.bonus.entity.BonusPlan;
import com.ds.bonus.service.BonusPlanService;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bonus/plan")
@RequiredArgsConstructor
public class BonusPlanController {

    private final BonusPlanService bonusPlanService;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "system:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/all")
    public Result<List<BonusPlan>> all() {
        return Result.ok(bonusPlanService.listAll());
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "system:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/active")
    public Result<BonusPlan> active() {
        return Result.ok(bonusPlanService.getActive());
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "bonus:plan:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/switch/{planId}")
    public Result<Void> switchPlan(@PathVariable Long planId) {
        bonusPlanService.switchPlan(planId);
        return Result.ok();
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "bonus:plan:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PostMapping("/")
    public Result<Void> add(@RequestBody BonusPlan plan) {
        boolean saved = bonusPlanService.add(plan);
        if (!saved) {
            return Result.fail("添加奖金方案失败");
        }
        return Result.ok();
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "bonus:plan:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BonusPlan plan) {
        plan.setId(id);
        boolean updated = bonusPlanService.update(plan);
        if (!updated) {
            return Result.fail("更新奖金方案失败");
        }
        return Result.ok();
    }
}