package com.ds.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SysUserDTO {

    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    private String realName;

    private String phone;

    private String avatar;

    private Integer status;

    private List<Long> roleIds;
}
