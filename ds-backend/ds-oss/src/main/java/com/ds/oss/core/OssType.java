package com.ds.oss.core;

import cn.hutool.core.util.StrUtil;

public enum OssType {

    LOCAL,
    ALIYUN,
    TENCENT,
    QINIU;

    public static OssType from(String type) {
        if (StrUtil.isBlank(type)) {
            return LOCAL;
        }
        for (OssType ossType : values()) {
            if (ossType.name().equalsIgnoreCase(type)) {
                return ossType;
            }
        }
        return LOCAL;
    }
}
