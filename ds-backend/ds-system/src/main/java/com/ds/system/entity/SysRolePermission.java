package com.ds.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ds_sys_role_permission")
public class SysRolePermission {

    private Long roleId;

    private Long permissionId;
}
