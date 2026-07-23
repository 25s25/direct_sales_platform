package com.ds.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ds.member.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    List<Member> selectByAncestorPath(@Param("ancestorPath") String ancestorPath, @Param("depth") int depth);
}