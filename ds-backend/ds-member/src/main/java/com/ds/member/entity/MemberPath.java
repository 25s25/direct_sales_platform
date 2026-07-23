package com.ds.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("ds_member_path")
public class MemberPath implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long ancestorId;

    private Long descendantId;

    private Integer depth;
}