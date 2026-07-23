package com.ds.member.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.member.entity.MemberLevel;
import com.ds.member.service.MemberLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/member/level")
@RequiredArgsConstructor
public class MemberLevelController {

    private final MemberLevelService memberLevelService;

    @GetMapping("/all")
    public Result<List<MemberLevel>> listAll() {
        return memberLevelService.listAll();
    }

    @GetMapping("/{id}")
    public Result<MemberLevel> getById(@PathVariable Long id) {
        return memberLevelService.getById(id);
    }
}