package com.ds.oss.core;

import lombok.Data;

import java.io.Serializable;

@Data
public class UploadResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String storageType;

    private String filePath;

    private String accessUrl;

    private String module;

    private Long bizId;
}
