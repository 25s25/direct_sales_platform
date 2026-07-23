package com.ds.message.impl;

import cn.hutool.core.util.StrUtil;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.ds.message.config.MessageProperties;
import com.ds.message.core.CaptchaScene;
import com.ds.message.core.CaptchaSender;
import com.ds.message.core.CaptchaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsCaptchaSender implements CaptchaSender {

    private final MessageProperties messageProperties;

    @Override
    public boolean supports(CaptchaType type) {
        return type == CaptchaType.SMS;
    }

    @Override
    public void send(String target, String code, CaptchaScene scene) {
        if (!messageProperties.getSmsEnabled()) {
            log.warn("短信验证码功能未启用");
            return;
        }

        String accessKeyId = messageProperties.getSmsAliyunAccessKey();
        String accessKeySecret = messageProperties.getSmsAliyunSecretKey();
        if (StrUtil.isBlank(accessKeyId) || StrUtil.isBlank(accessKeySecret)) {
            log.warn("【开发模式】未配置阿里云短信密钥，验证码为：{}，目标手机号：{}，场景：{}", code, target, scene);
            return;
        }

        try {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret);
            config.endpoint = "dysmsapi.aliyuncs.com";
            Client client = new Client(config);

            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(target)
                    .setSignName(messageProperties.getSmsAliyunSignName())
                    .setTemplateCode(messageProperties.getSmsAliyunTemplateCode())
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            SendSmsResponse response = client.sendSms(request);
            if (response.getBody() != null && "OK".equals(response.getBody().getCode())) {
                log.info("短信验证码已发送至：{}", target);
            } else {
                log.error("短信发送失败：{}", response.getBody() != null ? response.getBody().getMessage() : "未知错误");
            }
        } catch (Exception e) {
            log.error("短信发送异常", e);
        }
    }
}
