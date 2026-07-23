package com.ds.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.system.entity.SysRole;
import com.ds.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/list")
    @SaCheckPermission(value = "system:role:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> listAll() {
        return sysRoleService.listAll();
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/{id}")
    @SaCheckPermission(value = "system:role:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> getById(@PathVariable Long id) {
        return sysRoleService.getById(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PostMapping
    @SaCheckPermission(value = "system:role:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> add(@RequestBody SysRole role) {
        return sysRoleService.add(role);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping
    @SaCheckPermission(value = "system:role:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> update(@RequestBody SysRole role) {
        return sysRoleService.update(role);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @DeleteMapping("/{id}")
    @SaCheckPermission(value = "system:role:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> delete(@PathVariable Long id) {
        return sysRoleService.delete(id);
    }
}