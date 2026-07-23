package com.ds.message.core;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.ds.common.exception.BusinessException;
import com.ds.common.result.ResultCode;
import com.ds.message.config.MessageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final StringRedisTemplate stringRedisTemplate;
    private final MessageProperties messageProperties;
    private final List<CaptchaSender> captchaSenders;

    private static final String CAPTCHA_KEY_PREFIX = "ds:captcha:";
    private static final String LIMIT_KEY_PREFIX = "ds:captcha:limit:";
    private static final String LOCK_KEY_PREFIX = "ds:captcha:lock:";

    private static final long CAPTCHA_TTL_SECONDS = 300;
    private static final long LOCK_TTL_SECONDS = 900;
    private static final int MAX_ERROR_COUNT = 5;
    private static final int MAX_DAILY_PER_SCENE = 10;
    private static final int MAX_HOURLY_PER_IP = 30;
    private static final long LIMIT_TTL_HOURS = 3600;
    private static final long LIMIT_TTL_DAYS = 86400;

    public void sendCaptcha(CaptchaType type, CaptchaScene scene, String target, String ip) {
        if (type == null || scene == null || StrUtil.isBlank(target)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        checkEnabled(type);

        String lockKey = buildLockKey(type, target);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        String codeKey = buildCodeKey(type, scene, target);
        Long codeTtl = stringRedisTemplate.getExpire(codeKey, TimeUnit.SECONDS);
        if (codeTtl != null && codeTtl >= CAPTCHA_TTL_SECONDS - 60) {
            throw new BusinessException("验证码已发送，请60秒后再试");
        }

        String sceneLimitKey = buildSceneLimitKey(type, scene, target);
        long sceneCount = getCount(sceneLimitKey);
        if (sceneCount >= MAX_DAILY_PER_SCENE) {
            throw new BusinessException("今日该场景验证码发送次数已达上限");
        }

        if (StrUtil.isNotBlank(ip)) {
            String ipLimitKey = buildIpLimitKey(ip);
            long ipCount = getCount(ipLimitKey);
            if (ipCount >= MAX_HOURLY_PER_IP) {
                throw new BusinessException("该IP验证码发送次数已达上限");
            }
        }

        String code = RandomUtil.randomNumbers(6);
        sendByType(type, target, code, scene);

        stringRedisTemplate.opsForValue().set(codeKey, code, CAPTCHA_TTL_SECONDS, TimeUnit.SECONDS);

        incrementLimit(sceneLimitKey, LIMIT_TTL_DAYS);
        if (StrUtil.isNotBlank(ip)) {
            incrementLimit(buildIpLimitKey(ip), LIMIT_TTL_HOURS);
        }
        incrementLimit(buildTargetLimitKey(type, target), LIMIT_TTL_HOURS);
    }

    //判断
    public boolean verifyCaptcha(CaptchaType type, CaptchaScene scene, String target, String code, boolean deleteAfterSuccess) {
        //短信 注册 手机号 用户输入的验证码 判断
        if (type == null || scene == null || StrUtil.isBlank(target) || StrUtil.isBlank(code)) {
            return false;
        }

        String lockKey = buildLockKey(type, target);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
            throw new BusinessException("验证失败次数过多，请15分钟后再试");
        }

        String codeKey = buildCodeKey(type, scene, target);
        String storedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            return false;
        }

        if (!storedCode.equals(code)) {
            handleError(type, target);
            return false;
        }

        if (deleteAfterSuccess) {
            stringRedisTemplate.delete(codeKey);
        }
        stringRedisTemplate.delete(buildErrorKey(type, target));
        return true;
    }

    private void checkEnabled(CaptchaType type) {
        if (type == CaptchaType.EMAIL && !messageProperties.getEmailEnabled()) {
            throw new BusinessException("邮件验证码功能未启用");
        }
        if (type == CaptchaType.SMS && !messageProperties.getSmsEnabled()) {
            throw new BusinessException("短信验证码功能未启用");
        }
    }

    private void sendByType(CaptchaType type, String target, String code, CaptchaScene scene) {
        for (CaptchaSender sender : captchaSenders) {
            if (sender.supports(type)) {
                sender.send(target, code, scene);
                return;
            }
        }
        throw new BusinessException("不支持的验证码类型");
    }

    private void handleError(CaptchaType type, String target) {
        String errorKey = buildErrorKey(type, target);  // key: ds:captcha:lock:sms:138xxxx1234:error
        long errorCount = stringRedisTemplate.opsForValue().increment(errorKey, 1);  // 错误次数+1
        stringRedisTemplate.expire(errorKey, LOCK_TTL_SECONDS, TimeUnit.SECONDS);    // 重置过期时间15分钟
        if (errorCount >= MAX_ERROR_COUNT) {             // MAX_ERROR_COUNT = 5
            String lockKey = buildLockKey(type, target); // 错误达到5次，上锁！
            stringRedisTemplate.opsForValue().set(lockKey, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            stringRedisTemplate.delete(errorKey);         // 删除错误计数，释放空间
        }
    }


    private long getCount(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void incrementLimit(String key, long ttlSeconds) {
        stringRedisTemplate.opsForValue().increment(key, 1);
        stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
    }

    private String buildCodeKey(CaptchaType type, CaptchaScene scene, String target) {
        return CAPTCHA_KEY_PREFIX + type.name().toLowerCase() + ":" + scene.name().toLowerCase() + ":" + target;
    }

    private String buildTargetLimitKey(CaptchaType type, String target) {
        return LIMIT_KEY_PREFIX + type.name().toLowerCase() + ":" + target;
    }

    private String buildSceneLimitKey(CaptchaType type, CaptchaScene scene, String target) {
        return LIMIT_KEY_PREFIX + type.name().toLowerCase() + ":" + scene.name().toLowerCase() + ":" + target;
    }

    private String buildIpLimitKey(String ip) {
        return LIMIT_KEY_PREFIX + "ip:" + ip;
    }

    private String buildLockKey(CaptchaType type, String target) {
        return LOCK_KEY_PREFIX + type.name().toLowerCase() + ":" + target;
    }

    private String buildErrorKey(CaptchaType type, String target) {
        return LOCK_KEY_PREFIX + type.name().toLowerCase() + ":" + target + ":error";
    }
}
