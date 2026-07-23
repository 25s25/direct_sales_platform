package com.ds.oss.impl;

import cn.hutool.core.util.StrUtil;
import com.ds.common.exception.BusinessException;
import com.ds.oss.config.OssProperties;
import com.ds.oss.core.OssClient;
import com.ds.oss.core.OssType;
import com.ds.oss.core.UploadResult;
import com.ds.oss.util.OssPathUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class TencentCosClient implements OssClient {

    private final OssProperties ossProperties;

    private volatile COSClient cosClient;

    private COSClient getClient() {
        if (cosClient == null) {
            synchronized (this) {
                if (cosClient == null) {
                    String region = ossProperties.getTencentRegion();
                    String secretId = ossProperties.getTencentSecretId();
                    String secretKey = ossProperties.getTencentSecretKey();
                    if (StrUtil.isBlank(region) || StrUtil.isBlank(secretId) || StrUtil.isBlank(secretKey)) {
                        throw new BusinessException("腾讯云COS配置不完整");
                    }
                    BasicCOSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
                    ClientConfig clientConfig = new ClientConfig(new Region(region));
                    cosClient = new COSClient(credentials, clientConfig);
                }
            }
        }
        return cosClient;
    }

    @Override
    public UploadResult upload(InputStream inputStream, String originalName, long size, String module) {
        if (inputStream == null) {
            throw new BusinessException("上传文件流不能为空");
        }
        String bucket = ossProperties.getTencentBucket();
        if (StrUtil.isBlank(bucket)) {
            throw new BusinessException("腾讯云Bucket未配置");
        }

        String ext = OssPathUtil.getExt(originalName);
        String objectName = OssPathUtil.buildObjectName(module, ext);

        try {
            PutObjectRequest request = new PutObjectRequest(bucket, objectName, inputStream, null);
            getClient().putObject(request);
        } catch (Exception e) {
            log.error("腾讯云COS上传失败：{}，objectName：{}", originalName, objectName, e);
            throw new BusinessException("腾讯云COS上传失败");
        }

        UploadResult result = new UploadResult();
        result.setFileName(originalName);
        result.setFileSize(size);
        result.setStorageType(OssType.TENCENT.name().toLowerCase());
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
        String bucket = ossProperties.getTencentBucket();
        if (StrUtil.isBlank(bucket)) {
            return;
        }
        try {
            getClient().deleteObject(bucket, filePath);
        } catch (Exception e) {
            log.error("腾讯云COS删除失败：{}", filePath, e);
            throw new BusinessException("腾讯云COS删除失败");
        }
    }

    @Override
    public String getUrl(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        String bucket = ossProperties.getTencentBucket();
        String region = ossProperties.getTencentRegion();
        if (StrUtil.isBlank(bucket) || StrUtil.isBlank(region)) {
            return null;
        }
        return "https://" + bucket + ".cos." + region + ".myqcloud.com/" + filePath;
    }

    @Override
    public boolean supports(OssType type) {
        return type == OssType.TENCENT;
    }

    @PreDestroy
    public void destroy() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }
}
