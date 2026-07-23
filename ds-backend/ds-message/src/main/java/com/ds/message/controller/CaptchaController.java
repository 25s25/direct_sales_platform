package com.ds.message.controller;

import com.ds.common.result.Result;
import com.ds.message.core.CaptchaScene;
import com.ds.message.core.CaptchaService;
import com.ds.message.core.CaptchaType;
import com.ds.message.vo.CaptchaSendRequest;
import com.ds.message.vo.CaptchaVerifyRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @PostMapping("/send")
    public Result<Void> send(@Valid @RequestBody CaptchaSendRequest request, HttpServletRequest httpRequest) {
        captchaService.sendCaptcha(
                parseCaptchaType(request.getType()),
                parseCaptchaScene(request.getScene()),
                request.getTarget(),  //发送目标
                getClientIp(httpRequest)
        );
        return Result.ok();
    }

    @PostMapping("/verify")
    public Result<Boolean> verify(@Valid @RequestBody CaptchaVerifyRequest request) {
        boolean result = captchaService.verifyCaptcha(
                parseCaptchaType(request.getType()),
                parseCaptchaScene(request.getScene()),
                request.getTarget(),
                request.getCode(),
                true
        );
        return Result.ok(result);
    }

    private CaptchaType parseCaptchaType(String type) {
        try {
            return CaptchaType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new com.ds.common.exception.BusinessException("不支持的验证码类型: " + type);
        }
    }

    private CaptchaScene parseCaptchaScene(String scene) {
        try {
            return CaptchaScene.valueOf(scene.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new com.ds.common.exception.BusinessException("不支持的验证码场景: " + scene);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            try {
                ip = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                ip = "127.0.0.1";
            }
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
