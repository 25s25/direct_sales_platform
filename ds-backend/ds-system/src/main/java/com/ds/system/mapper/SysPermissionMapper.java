package com.ds.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ds.system.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    @Select("SELECT p.permission_code FROM ds_sys_permission p " +
            "INNER JOIN ds_sys_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN ds_sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}
