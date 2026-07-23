package com.ds.oss.impl;

import cn.hutool.core.util.StrUtil;
import com.ds.common.exception.BusinessException;
import com.ds.oss.config.OssProperties;
import com.ds.oss.core.OssClient;
import com.ds.oss.core.OssType;
import com.ds.oss.core.UploadResult;
import com.ds.oss.util.OssPathUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalOssClient implements OssClient {

    private final OssProperties ossProperties;

    @Override
    public UploadResult upload(InputStream inputStream, String originalName, long size, String module) {
        if (inputStream == null) {
            throw new BusinessException("上传文件流不能为空");
        }

        String basePath = ossProperties.getLocalBasePath();
        String baseUrl = ossProperties.getLocalBaseUrl();
        String ext = OssPathUtil.getExt(originalName);
        String objectName = OssPathUtil.buildObjectName(module, ext);

        Path targetPath = Paths.get(basePath, objectName);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("本地文件上传失败：{}，路径：{}", originalName, targetPath, e);
            throw new BusinessException("本地文件上传失败");
        }

        String accessUrl = baseUrl + "/" + objectName;

        UploadResult result = new UploadResult();
        result.setFileName(originalName);
        result.setFileSize(size);
        result.setStorageType(OssType.LOCAL.name().toLowerCase());
        result.setFilePath(objectName);
        result.setAccessUrl(accessUrl);
        result.setModule(module);
        return result;
    }

    @Override
    public void delete(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return;
        }
        String basePath = ossProperties.getLocalBasePath();
        Path targetPath = Paths.get(basePath, filePath);
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            log.error("本地文件删除失败：{}", targetPath, e);
            throw new BusinessException("本地文件删除失败");
        }
    }

    @Override
    public String getUrl(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        return ossProperties.getLocalBaseUrl() + "/" + filePath;
    }

    @Override
    public boolean supports(OssType type) {
        return type == OssType.LOCAL;
    }
}
