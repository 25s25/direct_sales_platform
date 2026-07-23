package com.ds.message.config;

import com.ds.common.result.Result;
import com.ds.system.entity.SysConfig;
import com.ds.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageProperties {

    private final SysConfigService sysConfigService;

    public String getValueByKey(String key) {
        Result<SysConfig> result = sysConfigService.getByKey(key);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return null;
        }
        return result.getData().getConfigValue();
    }

    public boolean getEmailEnabled() {
        return "true".equalsIgnoreCase(getValueByKey("message.email.enabled"));
    }

    public String getEmailHost() {
        return getValueByKey("message.email.host");
    }

    public int getEmailPort() {
        String value = getValueByKey("message.email.port");
        if (value == null || value.isEmpty()) {
            return 25;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 25;
        }
    }

    public String getEmailUsername() {
        return getValueByKey("message.email.username");
    }

    public String getEmailPassword() {
        return getValueByKey("message.email.password");
    }

    public String getEmailFromName() {
        String value = getValueByKey("message.email.from-name");
        return value == null || value.isEmpty() ? getEmailUsername() : value;
    }

    public boolean getSmsEnabled() {
        return "true".equalsIgnoreCase(getValueByKey("message.sms.enabled"));
    }

    public String getSmsAliyunAccessKey() {
        return getValueByKey("message.sms.aliyun.access-key");
    }

    public String getSmsAliyunSecretKey() {
        return getValueByKey("message.sms.aliyun.secret-key");
    }

    public String getSmsAliyunSignName() {
        return getValueByKey("message.sms.aliyun.sign-name");
    }

    public String getSmsAliyunTemplateCode() {
        return getValueByKey("message.sms.aliyun.template-code");
    }
}
