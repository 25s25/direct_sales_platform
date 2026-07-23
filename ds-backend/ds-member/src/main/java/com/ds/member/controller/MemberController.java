package com.ds.member.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.member.dto.MemberAdminDTO;
import com.ds.member.dto.MemberLoginDTO;
import com.ds.member.dto.MemberRegisterDTO;
import com.ds.member.dto.MemberUpdateDTO;
import com.ds.member.service.MemberService;
import com.ds.member.vo.MemberVO;
import cn.hutool.core.util.StrUtil;
import com.ds.member.vo.TeamTreeVO;
import com.ds.message.core.CaptchaScene;
import com.ds.message.core.CaptchaService;
import com.ds.message.core.CaptchaType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final CaptchaService captchaService;

    @PostMapping("/register")
    public Result<MemberVO> register(@Valid @RequestBody MemberRegisterDTO dto) {
        if (StrUtil.isBlank(dto.getCode())) {
            return Result.fail("短信验证码不能为空");
        }
        boolean verified = captchaService.verifyCaptcha(CaptchaType.SMS, CaptchaScene.REGISTER, dto.getPhone(), dto.getCode(), true);
        if (!verified) {
            return Result.fail("短信验证码错误或已过期");
        }
        return memberService.register(dto);
    }

    @PostMapping("/login")
    public Result<MemberVO> login(@Valid @RequestBody MemberLoginDTO dto) {
        if (StrUtil.isNotBlank(dto.getCode())) {
            boolean verified = captchaService.verifyCaptcha(CaptchaType.SMS, CaptchaScene.LOGIN, dto.getPhone(), dto.getCode(), true);
            if (!verified) {
                return Result.fail("短信验证码错误或已过期");
            }
        }
        return memberService.login(dto);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @GetMapping("/info")
    public Result<MemberVO> info() {
        long memberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        return memberService.getById(memberId);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PutMapping("/update")
    public Result<MemberVO> updateProfile(@Valid @RequestBody MemberUpdateDTO dto) {
        long memberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        return memberService.updateProfile(memberId, dto);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @GetMapping("/{id}")
    public Result<MemberVO> getById(@PathVariable Long id) {
        long currentMemberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        if (!SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).hasPermission("system:manage") && !id.equals(currentMemberId)) {
            return Result.fail("无权访问该会员信息");
        }
        return memberService.getById(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "member:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String phone,
                          @RequestParam(required = false) String realName,
                          @RequestParam(required = false) Long level,
                          @RequestParam(required = false) Integer status) {
        Page<MemberVO> pageParam = new Page<>(page, size);
        return memberService.page(pageParam, keyword, phone, realName, level, status);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "member:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return memberService.updateStatus(id, status);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "member:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PostMapping("/admin")
    public Result<MemberVO> addByAdmin(@Valid @RequestBody MemberAdminDTO dto) {
        return memberService.addByAdmin(dto);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "member:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/admin/{id}")
    public Result<MemberVO> updateByAdmin(@PathVariable Long id, @Valid @RequestBody MemberAdminDTO dto) {
        dto.setId(id);
        return memberService.updateByAdmin(dto);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @GetMapping("/{id}/team")
    public Result<List<TeamTreeVO>> getTeamTree(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "3") int depth) {
        long currentMemberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        if (!SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).hasPermission("system:manage") && !id.equals(currentMemberId)) {
            return Result.fail("无权查看该会员团队");
        }
        return memberService.getTeamTree(id, depth);
    }
}