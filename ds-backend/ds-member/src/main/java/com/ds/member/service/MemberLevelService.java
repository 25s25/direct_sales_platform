package com.ds.member.service;

import com.ds.common.result.Result;
import com.ds.member.entity.MemberLevel;

import java.util.List;

public interface MemberLevelService {

    Result<List<MemberLevel>> listAll();

    Result<MemberLevel> getById(Long id);
}