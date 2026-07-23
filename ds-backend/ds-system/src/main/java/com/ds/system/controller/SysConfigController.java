package com.ds.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/list")
    @SaCheckPermission(value = "system:config:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> listAll() {
        return sysConfigService.listAll();
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/{key}")
    @SaCheckPermission(value = "system:config:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> getByKey(@PathVariable String key) {
        return sysConfigService.getByKey(key);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/{key}")
    @SaCheckPermission(value = "system:config:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    public Result<?> updateByKey(@PathVariable String key, @RequestParam String value) {
        return sysConfigService.updateByKey(key, value);
    }
}