package com.ds.message.config;

import cn.hutool.core.util.StrUtil;
import com.ds.common.result.Result;
import com.ds.message.MessageTestApplication;
import com.ds.system.entity.SysConfig;
import com.ds.system.service.SysConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = MessageTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class SysConfigCheckTest {

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 邮件与短信配置键集合，值是否与 init.sql 中一致可在此维护
     */
    private static final String[] REQUIRED_EMAIL_KEYS = {
            "message.email.enabled",
            "message.email.host",
            "message.email.port",
            "message.email.username",
            "message.email.password",
            "message.email.from-name"
    };

    private static final String[] REQUIRED_SMS_KEYS = {
            "message.sms.enabled",
            "message.sms.aliyun.access-key",
            "message.sms.aliyun.secret-key",
            "message.sms.aliyun.sign-name",
            "message.sms.aliyun.template-code"
    };

    @Test
    void allMessageConfigKeysExist() {
        Map<String, String> configMap = loadConfigMap();

        for (String key : REQUIRED_EMAIL_KEYS) {
            assertTrue(configMap.containsKey(key), "缺少邮件配置项: " + key);
        }

        for (String key : REQUIRED_SMS_KEYS) {
            assertTrue(configMap.containsKey(key), "缺少短信配置项: " + key);
        }
    }

    @Test
    void emailConfigValuesAreValid() {
        Map<String, String> configMap = loadConfigMap();

        String enabled = configMap.get("message.email.enabled");
        assertTrue("true".equalsIgnoreCase(enabled) || "false".equalsIgnoreCase(enabled),
                "message.email.enabled 必须是 true 或 false");

        String port = configMap.get("message.email.port");
        assertDoesNotThrow(() -> Integer.parseInt(port), "message.email.port 必须是有效数字");

        if ("true".equalsIgnoreCase(enabled)) {
            assertFalse(StrUtil.isBlank(configMap.get("message.email.host")), "邮件启用时 host 不能为空");
            assertFalse(StrUtil.isBlank(configMap.get("message.email.username")), "邮件启用时 username 不能为空");
            assertFalse(StrUtil.isBlank(configMap.get("message.email.password")), "邮件启用时 password 不能为空");
        }
    }

    @Test
    void smsConfigValuesAreValid() {
        Map<String, String> configMap = loadConfigMap();

        String enabled = configMap.get("message.sms.enabled");
        assertTrue("true".equalsIgnoreCase(enabled) || "false".equalsIgnoreCase(enabled),
                "message.sms.enabled 必须是 true 或 false");

        if ("true".equalsIgnoreCase(enabled)) {
            assertFalse(StrUtil.isBlank(configMap.get("message.sms.aliyun.access-key")), "短信启用时 access-key 不能为空");
            assertFalse(StrUtil.isBlank(configMap.get("message.sms.aliyun.secret-key")), "短信启用时 secret-key 不能为空");
            assertFalse(StrUtil.isBlank(configMap.get("message.sms.aliyun.sign-name")), "短信启用时 sign-name 不能为空");
            assertFalse(StrUtil.isBlank(configMap.get("message.sms.aliyun.template-code")), "短信启用时 template-code 不能为空");
        }
    }

    private Map<String, String> loadConfigMap() {
        Result<List<SysConfig>> result = sysConfigService.listAll();
        assertTrue(result.isSuccess(), "查询配置失败: " + result.getMessage());
        assertNotNull(result.getData(), "配置列表为空");

        Map<String, String> map = new HashMap<>();
        for (SysConfig config : result.getData()) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        return map;
    }
}
