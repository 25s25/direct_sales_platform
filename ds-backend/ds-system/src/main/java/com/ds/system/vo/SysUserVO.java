package com.ds.system.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysUserVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String avatar;

    private Integer status;

    private LocalDateTime createTime;

    private List<Long> roleIds;

    private List<SysRoleVO> roles;
}
