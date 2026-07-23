package com.ds.member.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TeamTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String memberNo;

    private String phone;

    private String realName;

    private String levelName;

    private LocalDateTime joinTime;

    private List<TeamTreeVO> children = new ArrayList<>();
}