package com.ds.member.social.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.member.social.core.SocialType;
import com.ds.member.social.dto.SocialBindDTO;
import com.ds.member.social.dto.SocialLoginDTO;
import com.ds.member.social.dto.SocialUnbindDTO;
import com.ds.member.social.service.MemberSocialService;
import com.ds.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth/social")
@RequiredArgsConstructor
public class SocialAuthController {

    private final MemberSocialService memberSocialService;

    @GetMapping("/url")
    public Result<String> getAuthUrl(@RequestParam String type,
                                     @RequestParam String redirectUri) {
        SocialType socialType = SocialType.of(type);
        if (socialType == null) {
            return Result.fail("不支持的社交登录类型");
        }
        return Result.ok(memberSocialService.getAuthUrl(socialType, redirectUri));
    }

    @PostMapping("/login")
    public Result<MemberVO> socialLogin(@RequestBody SocialLoginDTO dto) {
        SocialType socialType = SocialType.of(dto.getType());
        if (socialType == null) {
            return Result.fail("不支持的社交登录类型");
        }
        return memberSocialService.socialLogin(socialType, dto.getCode());
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PostMapping("/bind")
    public Result<Void> bindSocialAccount(@RequestBody SocialBindDTO dto) {
        SocialType socialType = SocialType.of(dto.getType());
        if (socialType == null) {
            return Result.fail("不支持的社交登录类型");
        }
        return memberSocialService.bindSocialAccount(socialType, dto.getCode());
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PostMapping("/unbind")
    public Result<Void> unbindSocialAccount(@RequestBody SocialUnbindDTO dto) {
        SocialType socialType = SocialType.of(dto.getType());
        if (socialType == null) {
            return Result.fail("不支持的社交登录类型");
        }
        return memberSocialService.unbindSocialAccount(socialType);
    }
}
