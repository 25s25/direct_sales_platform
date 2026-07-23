package com.ds.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ds.member.entity.MemberPath;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberPathMapper extends BaseMapper<MemberPath> {

    void insertBatch(@Param("list") List<MemberPath> list);
}