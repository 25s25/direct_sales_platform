package com.ds.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysRoleVO {

    private Long id;

    private String name;

    private String roleCode;

    private String remark;

    private Integer status;

    private LocalDateTime createTime;
}
