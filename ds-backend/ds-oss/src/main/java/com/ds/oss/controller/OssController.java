package com.ds.oss.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.oss.core.UploadResult;
import com.ds.oss.service.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/oss")
@RequiredArgsConstructor
public class OssController {

    private final OssService ossService;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PostMapping("/upload")
    public Result<UploadResult> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam String module,
                                       @RequestParam(required = false) Long bizId) {
        return ossService.upload(file, module, bizId);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PostMapping("/admin/upload")
    public Result<UploadResult> adminUpload(@RequestParam("file") MultipartFile file,
                                            @RequestParam String module,
                                            @RequestParam(required = false) Long bizId) {
        return ossService.adminUpload(file, module, bizId);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return ossService.delete(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @GetMapping("/{id}/url")
    public Result<String> getUrl(@PathVariable Long id) {
        return ossService.getUrl(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/list")
    public Result<List<UploadResult>> listByBiz(@RequestParam String module,
                                                @RequestParam Long bizId) {
        return ossService.listByBiz(module, bizId);
    }
}
