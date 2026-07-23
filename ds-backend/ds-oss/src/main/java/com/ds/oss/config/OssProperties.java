package com.ds.oss.config;

import cn.hutool.core.util.StrUtil;
import com.ds.common.exception.BusinessException;
import com.ds.common.result.Result;
import com.ds.system.entity.SysConfig;
import com.ds.system.service.SysConfigService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
//@ConfigurationProperties(prefix = "oss")
@RequiredArgsConstructor
public class OssProperties {

    private final SysConfigService sysConfigService;

    public String getConfig(String key) {
        Result<SysConfig> result = sysConfigService.getByKey("oss." + key);
        if (!result.isSuccess() || result.getData() == null) {
            return null;
        }
        return result.getData().getConfigValue();
    }

    public String getConfig(String key, String defaultValue) {
        String value = getConfig(key);
        return StrUtil.isBlank(value) ? defaultValue : value;
    }

    public long getConfigLong(String key, long defaultValue) {
        String value = getConfig(key);
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getType() {
        return getConfig("type", "local");
    }

    public long getMaxFileSize() {
        return getConfigLong("max-file-size", 10 * 1024 * 1024);
    }

    public String getAllowedTypes() {
        return getConfig("allowed-types", "jpg,jpeg,png,gif,bmp,webp,doc,docx,xls,xlsx,ppt,pptx,pdf,txt,zip,rar,mp4,mp3");
    }

    public String getLocalBasePath() {
        String basePath = getConfig("local.base-path");
        if (StrUtil.isBlank(basePath)) {
            throw new BusinessException("本地存储根目录未配置：oss.local.base-path");
        }
        return basePath;
    }

    public String getLocalBaseUrl() {
        String baseUrl = getConfig("local.base-url");
        if (StrUtil.isBlank(baseUrl)) {
            throw new BusinessException("本地访问基础URL未配置：oss.local.base-url");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String getAliyunEndpoint() {
        return getConfig("aliyun.endpoint");
    }

    public String getAliyunBucket() {
        return getConfig("aliyun.bucket");
    }

    public String getAliyunAccessKey() {
        return getConfig("aliyun.access-key");
    }

    public String getAliyunSecretKey() {
        return getConfig("aliyun.secret-key");
    }

    public String getTencentRegion() {
        return getConfig("tencent.region");
    }

    public String getTencentBucket() {
        return getConfig("tencent.bucket");
    }

    public String getTencentSecretId() {
        return getConfig("tencent.secret-id");
    }

    public String getTencentSecretKey() {
        return getConfig("tencent.secret-key");
    }

    public String getQiniuDomain() {
        String domain = getConfig("qiniu.domain");
        if (StrUtil.isBlank(domain)) {
            return null;
        }
        return domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
    }

    public String getQiniuBucket() {
        return getConfig("qiniu.bucket");
    }

    public String getQiniuAccessKey() {
        return getConfig("qiniu.access-key");
    }

    public String getQiniuSecretKey() {
        return getConfig("qiniu.secret-key");
    }
}
