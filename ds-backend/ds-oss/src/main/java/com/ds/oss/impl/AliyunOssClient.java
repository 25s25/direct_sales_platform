package com.ds.oss.impl;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.ds.common.exception.BusinessException;
import com.ds.oss.config.OssProperties;
import com.ds.oss.core.OssClient;
import com.ds.oss.core.OssType;
import com.ds.oss.core.UploadResult;
import com.ds.oss.util.OssPathUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunOssClient implements OssClient {

    private final OssProperties ossProperties;

    private volatile OSS ossClient;

    private OSS getClient() {
        if (ossClient == null) {
            synchronized (this) {
                if (ossClient == null) {
                    String endpoint = ossProperties.getAliyunEndpoint();
                    String accessKey = ossProperties.getAliyunAccessKey();
                    String secretKey = ossProperties.getAliyunSecretKey();
                    if (StrUtil.isBlank(endpoint) || StrUtil.isBlank(accessKey) || StrUtil.isBlank(secretKey)) {
                        throw new BusinessException("阿里云OSS配置不完整");
                    }
                    ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
                }
            }
        }
        return ossClient;
    }

    @Override
    public UploadResult upload(InputStream inputStream, String originalName, long size, String module) {
        if (inputStream == null) {
            throw new BusinessException("上传文件流不能为空");
        }
        String bucket = ossProperties.getAliyunBucket();
        if (StrUtil.isBlank(bucket)) {
            throw new BusinessException("阿里云Bucket未配置");
        }

        String ext = OssPathUtil.getExt(originalName);
        String objectName = OssPathUtil.buildObjectName(module, ext);

        try {
            getClient().putObject(bucket, objectName, inputStream);
        } catch (Exception e) {
            log.error("阿里云OSS上传失败：{}，objectName：{}", originalName, objectName, e);
            throw new BusinessException("阿里云OSS上传失败");
        }

        UploadResult result = new UploadResult();
        result.setFileName(originalName);
        result.setFileSize(size);
        result.setStorageType(OssType.ALIYUN.name().toLowerCase());
        result.setFilePath(objectName);
        result.setAccessUrl(getUrl(objectName));
        result.setModule(module);
        return result;
    }

    @Override
    public void delete(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return;
        }
        String bucket = ossProperties.getAliyunBucket();
        if (StrUtil.isBlank(bucket)) {
            return;
        }
        try {
            getClient().deleteObject(bucket, filePath);
        } catch (Exception e) {
            log.error("阿里云OSS删除失败：{}", filePath, e);
            throw new BusinessException("阿里云OSS删除失败");
        }
    }

    @Override
    public String getUrl(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        String bucket = ossProperties.getAliyunBucket();
        String endpoint = ossProperties.getAliyunEndpoint();
        if (StrUtil.isBlank(bucket) || StrUtil.isBlank(endpoint)) {
            return null;
        }
        return "https://" + bucket + "." + endpoint + "/" + filePath;
    }

    @Override
    public boolean supports(OssType type) {
        return type == OssType.ALIYUN;
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
