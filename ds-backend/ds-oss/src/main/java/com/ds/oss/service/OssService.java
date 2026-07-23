package com.ds.oss.service;

import com.ds.common.result.Result;
import com.ds.oss.core.UploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OssService {

    Result<UploadResult> upload(MultipartFile file, String module, Long bizId);

    Result<UploadResult> adminUpload(MultipartFile file, String module, Long bizId);

    Result<Void> delete(Long id);

    Result<String> getUrl(Long id);

    Result<List<UploadResult>> listByBiz(String module, Long bizId);
}
