package com.ds.member.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MemberUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String phone;

    private String email;

    private String realName;

    private String idCard;

    private String avatar;
}
