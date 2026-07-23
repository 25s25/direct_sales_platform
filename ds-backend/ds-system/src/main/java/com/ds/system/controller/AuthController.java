package com.ds.system.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.common.result.ResultCode;
import com.ds.system.entity.SysUser;
import com.ds.system.mapper.SysPermissionMapper;
import com.ds.system.service.SysUserService;
import com.ds.system.vo.SysUserVO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final SysPermissionMapper sysPermissionMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Data
    public static class LoginDTO {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto) {
        if (StrUtil.isBlank(dto.getUsername()) || StrUtil.isBlank(dto.getPassword())) {
            return Result.fail("账号和密码不能为空");
        }

        Result<SysUser> userResult = sysUserService.getByUsername(dto.getUsername());
        if (!userResult.isSuccess() || userResult.getData() == null) {
            return Result.fail("账号或密码错误");
        }

        SysUser user = userResult.getData();
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.fail("账号或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.fail("账号已被禁用，请联系管理员");
        }

        SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_ADMIN).login(user.getId());

        SaTokenInfo tokenInfo = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_ADMIN).getTokenInfo();

        List<String> permissions = sysPermissionMapper.selectPermissionCodesByUserId(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenInfo.getTokenValue());
        result.put("tokenName", tokenInfo.getTokenName());
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("avatar", user.getAvatar());
        result.put("permissions", permissions);

        log.info("用户登录成功: {}", dto.getUsername());
        return Result.ok(result);
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        if (SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_ADMIN).isLogin()) {
            SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_ADMIN).logout();
        }
        return Result.ok();
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        if (!SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_ADMIN).isLogin()) {
            return Result.fail(ResultCode.UNAUTHORIZED.getCode(), "未登录");
        }

        long userId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_ADMIN).getLoginIdAsLong();
        Result<SysUserVO> userResult = sysUserService.getById(userId);

        List<String> permissions = sysPermissionMapper.selectPermissionCodesByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        if (userResult.isSuccess() && userResult.getData() != null) {
            SysUserVO user = userResult.getData();
            result.put("username", user.getUsername());
            result.put("realName", user.getRealName());
            result.put("avatar", user.getAvatar());
        }
        result.put("permissions", permissions);
        return Result.ok(result);
    }
}