package com.ds.oss.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Date;

public class OssPathUtil {

    private OssPathUtil() {
    }

    public static String buildObjectName(String module, String ext) {
        String date = DateUtil.format(new Date(), "yyyyMMdd");
        String uuid = IdUtil.simpleUUID();
        if (StrUtil.isBlank(ext)) {
            return module + "/" + date + "/" + uuid;
        }
        return module + "/" + date + "/" + uuid + "." + ext.toLowerCase();
    }

    public static String getExt(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "";
        }
        int index = fileName.lastIndexOf(".");
        return index == -1 ? "" : fileName.substring(index + 1);
    }
}
