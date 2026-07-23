package com.ds.member.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.member.dto.MemberAdminDTO;
import com.ds.member.dto.MemberLoginDTO;
import com.ds.member.dto.MemberRegisterDTO;
import com.ds.member.dto.MemberUpdateDTO;
import com.ds.member.vo.MemberVO;
import com.ds.member.vo.TeamTreeVO;

import java.math.BigDecimal;
import java.util.List;

public interface MemberService {

    Result<MemberVO> register(MemberRegisterDTO dto);

    Result<MemberVO> login(MemberLoginDTO dto);

    Result<MemberVO> getById(Long id);

    Result<PageResult<MemberVO>> page(Page<MemberVO> page, String keyword, String phone, String realName, Long level, Integer status);

    Result<Void> updateStatus(Long id, Integer status);

    Result<List<TeamTreeVO>> getTeamTree(Long memberId, int depth);

    Result<Void> deductWallet(Long memberId, BigDecimal amount);

    Result<Void> addWallet(Long memberId, BigDecimal amount);

    Result<MemberVO> addByAdmin(MemberAdminDTO dto);

    Result<MemberVO> updateByAdmin(MemberAdminDTO dto);

    Result<MemberVO> updateProfile(Long memberId, MemberUpdateDTO dto);
}