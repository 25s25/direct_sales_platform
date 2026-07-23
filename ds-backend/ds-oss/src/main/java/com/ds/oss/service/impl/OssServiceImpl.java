package com.ds.oss.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.exception.BusinessException;
import com.ds.common.result.Result;
import com.ds.common.result.ResultCode;
import com.ds.oss.config.OssProperties;
import com.ds.oss.core.OssClient;
import com.ds.oss.core.OssType;
import com.ds.oss.core.UploadResult;
import com.ds.oss.entity.FileRecord;
import com.ds.oss.mapper.FileRecordMapper;
import com.ds.oss.service.OssService;
import com.ds.oss.util.OssPathUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssServiceImpl extends ServiceImpl<FileRecordMapper, FileRecord> implements OssService {

    private final OssProperties ossProperties;
    private final List<OssClient> ossClients;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<UploadResult> upload(MultipartFile file, String module, Long bizId) {
        return doUpload(file, module, bizId, SaTokenConsts.LOGIN_TYPE_MEMBER);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<UploadResult> adminUpload(MultipartFile file, String module, Long bizId) {
        return doUpload(file, module, bizId, SaTokenConsts.LOGIN_TYPE_ADMIN);
    }

    private Result<UploadResult> doUpload(MultipartFile file, String module, Long bizId, String loginType) {
        if (file == null || file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }
        if (StrUtil.isBlank(module)) {
            return Result.fail("业务模块不能为空");
        }

        long maxFileSize = ossProperties.getMaxFileSize();
        if (file.getSize() > maxFileSize) {
            return Result.fail("文件大小超过限制");
        }

        String originalName = file.getOriginalFilename();
        String ext = OssPathUtil.getExt(originalName).toLowerCase();
        if (StrUtil.isBlank(ext)) {
            return Result.fail("无法识别文件扩展名");
        }

        Set<String> allowedTypes = parseAllowedTypes(ossProperties.getAllowedTypes());
        if (!allowedTypes.contains(ext)) {
            return Result.fail("不支持的文件类型");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            return Result.fail("读取上传文件失败");
        }

        String magicType;
        try (ByteArrayInputStream magicStream = new ByteArrayInputStream(bytes)) {
            magicType = FileTypeUtil.getType(magicStream);
        } catch (IOException e) {
            log.error("Magic Number校验失败", e);
            return Result.fail("Magic Number校验失败");
        }
        if (StrUtil.isNotBlank(magicType) && !allowedTypes.contains(magicType.toLowerCase())) {
            return Result.fail("文件Magic Number校验失败");
        }

        OssType ossType = OssType.from(ossProperties.getType());
        OssClient client = ossClients.stream()
                .filter(c -> c.supports(ossType))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到对应的OSS客户端：" + ossType));

        UploadResult uploadResult;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            uploadResult = client.upload(inputStream, originalName, file.getSize(), module);
        } catch (IOException e) {
            log.error("关闭上传流失败", e);
            throw new BusinessException("上传失败");
        }

        uploadResult.setBizId(bizId);
        uploadResult.setFileType(file.getContentType());

        FileRecord record = new FileRecord();
        record.setFileName(uploadResult.getFileName());
        record.setFileType(uploadResult.getFileType());
        record.setFileSize(uploadResult.getFileSize());
        record.setStorageType(uploadResult.getStorageType());
        record.setFilePath(uploadResult.getFilePath());
        record.setAccessUrl(uploadResult.getAccessUrl());
        record.setModule(module);
        record.setBizId(bizId);
        record.setCreateBy(getCurrentUserId(loginType));

        boolean saved = this.save(record);
        if (!saved) {
            throw new BusinessException("保存文件记录失败");
        }

        uploadResult.setId(record.getId());
        return Result.ok(uploadResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        if (id == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        FileRecord record = this.getById(id);
        if (record == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }

        OssType ossType = OssType.from(record.getStorageType());
        OssClient client = ossClients.stream()
                .filter(c -> c.supports(ossType))
                .findFirst()
                .orElse(null);
        if (client != null) {
            client.delete(record.getFilePath());
        }

        boolean removed = this.removeById(id);
        if (!removed) {
            return Result.fail("删除文件记录失败");
        }
        return Result.ok();
    }

    @Override
    public Result<String> getUrl(Long id) {
        if (id == null) {
            return Result.fail(ResultCode.BAD_REQUEST);
        }
        FileRecord record = this.getById(id);
        if (record == null) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        return Result.ok(record.getAccessUrl());
    }

    @Override
    public Result<List<UploadResult>> listByBiz(String module, Long bizId) {
        if (StrUtil.isBlank(module) || bizId == null) {
            return Result.ok(Collections.emptyList());
        }
        List<FileRecord> records = lambdaQuery()
                .eq(FileRecord::getModule, module)
                .eq(FileRecord::getBizId, bizId)
                .orderByDesc(FileRecord::getCreateTime)
                .list();
        if (CollUtil.isEmpty(records)) {
            return Result.ok(Collections.emptyList());
        }
        List<UploadResult> results = records.stream().map(this::toUploadResult).collect(Collectors.toList());
        return Result.ok(results);
    }

    private Set<String> parseAllowedTypes(String allowedTypes) {
        if (StrUtil.isBlank(allowedTypes)) {
            return Collections.emptySet();
        }
        return Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    private Long getCurrentUserId(String loginType) {
        try {
            return SaManager.getStpLogic(loginType).getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private UploadResult toUploadResult(FileRecord record) {
        UploadResult result = new UploadResult();
        result.setId(record.getId());
        result.setFileName(record.getFileName());
        result.setFileType(record.getFileType());
        result.setFileSize(record.getFileSize());
        result.setStorageType(record.getStorageType());
        result.setFilePath(record.getFilePath());
        result.setAccessUrl(record.getAccessUrl());
        result.setModule(record.getModule());
        result.setBizId(record.getBizId());
        return result;
    }
}
