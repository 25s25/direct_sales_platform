package com.ds.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ds.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}