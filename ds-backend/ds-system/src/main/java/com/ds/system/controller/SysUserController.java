package com.ds.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ds.common.constant.SaTokenConsts;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.result.Result;
import com.ds.system.dto.SysUserDTO;
import com.ds.system.service.SysUserService;
import com.ds.system.vo.SysUserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/{id}")
    @SaCheckPermission(value = "system:user:view", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> getById(@PathVariable Long id) {
        return sysUserService.getById(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/page")
    @SaCheckPermission(value = "system:user:view", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword) {
        Page<SysUserVO> pageParam = new Page<>(page, size);
        return sysUserService.page(pageParam, keyword);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PostMapping
    @SaCheckPermission(value = "system:user:add", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> add(@Valid @RequestBody SysUserDTO user) {
        return sysUserService.add(user);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping
    @SaCheckPermission(value = "system:user:edit", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> update(@Valid @RequestBody SysUserDTO user) {
        return sysUserService.update(user);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @DeleteMapping("/{id}")
    @SaCheckPermission(value = "system:user:delete", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> delete(@PathVariable Long id) {
        return sysUserService.delete(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/{id}/status")
    @SaCheckPermission(value = "system:user:edit", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return sysUserService.updateStatus(id, status);
    }
}