package com.ds.oss.impl;

import cn.hutool.core.util.StrUtil;
import com.ds.common.exception.BusinessException;
import com.ds.oss.config.OssProperties;
import com.ds.oss.core.OssClient;
import com.ds.oss.core.OssType;
import com.ds.oss.core.UploadResult;
import com.ds.oss.util.OssPathUtil;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class QiniuOssClient implements OssClient {

    private final OssProperties ossProperties;

    @Override
    public UploadResult upload(InputStream inputStream, String originalName, long size, String module) {
        if (inputStream == null) {
            throw new BusinessException("上传文件流不能为空");
        }
        String bucket = ossProperties.getQiniuBucket();
        String accessKey = ossProperties.getQiniuAccessKey();
        String secretKey = ossProperties.getQiniuSecretKey();
        if (StrUtil.isBlank(bucket) || StrUtil.isBlank(accessKey) || StrUtil.isBlank(secretKey)) {
            throw new BusinessException("七牛云配置不完整");
        }

        String ext = OssPathUtil.getExt(originalName);
        String objectName = OssPathUtil.buildObjectName(module, ext);

        Auth auth = Auth.create(accessKey, secretKey);
        String token = auth.uploadToken(bucket);
        Configuration configuration = new Configuration();
        UploadManager uploadManager = new UploadManager(configuration);

        try (inputStream) {
            Response response = uploadManager.put(inputStream, objectName, token, null, null);
            if (!response.isOK()) {
                log.error("七牛云上传失败：{}，objectName：{}，response：{}", originalName, objectName, response.bodyString());
                throw new BusinessException("七牛云上传失败");
            }
        } catch (IOException e) {
            log.error("七牛云上传失败：{}，objectName：{}", originalName, objectName, e);
            throw new BusinessException("七牛云上传失败");
        }

        UploadResult result = new UploadResult();
        result.setFileName(originalName);
        result.setFileSize(size);
        result.setStorageType(OssType.QINIU.name().toLowerCase());
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
        String bucket = ossProperties.getQiniuBucket();
        String accessKey = ossProperties.getQiniuAccessKey();
        String secretKey = ossProperties.getQiniuSecretKey();
        if (StrUtil.isBlank(bucket) || StrUtil.isBlank(accessKey) || StrUtil.isBlank(secretKey)) {
            return;
        }
        Auth auth = Auth.create(accessKey, secretKey);
        Configuration configuration = new Configuration();
        com.qiniu.storage.BucketManager bucketManager = new com.qiniu.storage.BucketManager(auth, configuration);
        try {
            bucketManager.delete(bucket, filePath);
        } catch (Exception e) {
            log.error("七牛云删除失败：{}", filePath, e);
            throw new BusinessException("七牛云删除失败");
        }
    }

    @Override
    public String getUrl(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        String domain = ossProperties.getQiniuDomain();
        if (StrUtil.isBlank(domain)) {
            return null;
        }
        return domain + "/" + filePath;
    }

    @Override
    public boolean supports(OssType type) {
        return type == OssType.QINIU;
    }
}
