package com.ds.member.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.common.result.Result;
import com.ds.member.entity.MemberLevel;
import com.ds.member.mapper.MemberLevelMapper;
import com.ds.member.service.MemberLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberLevelServiceImpl extends ServiceImpl<MemberLevelMapper, MemberLevel> implements MemberLevelService {

    @Override
    public Result<List<MemberLevel>> listAll() {
        List<MemberLevel> list = lambdaQuery()
                .eq(MemberLevel::getStatus, 1)
                .orderByAsc(MemberLevel::getSortOrder)
                .list();
        return Result.ok(list);
    }

    @Override
    public Result<MemberLevel> getById(Long id) {
        MemberLevel level = super.getById(id);
        if (level == null) {
            return Result.fail("会员等级不存在");
        }
        return Result.ok(level);
    }
}