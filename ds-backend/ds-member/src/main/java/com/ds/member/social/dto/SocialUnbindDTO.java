package com.ds.member.social.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SocialUnbindDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
}
