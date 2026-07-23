package com.ds.member.social.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SocialBindDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;

    private String code;
}
