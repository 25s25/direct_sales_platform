package com.ds.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class MemberAdminDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String password;

    private String realName;

    private String idCard;

    private Long levelId;

    private Long recommendId;

    private Integer status;
}
