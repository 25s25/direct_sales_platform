package com.ds.oss.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ds.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_file_record")
public class FileRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String storageType;

    private String filePath;

    private String accessUrl;

    private String module;

    private Long bizId;

    private Long createBy;

    @TableLogic
    private Integer deleted;
}
