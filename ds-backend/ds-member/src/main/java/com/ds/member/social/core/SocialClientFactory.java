package com.ds.member.social.core;

import com.ds.common.exception.BusinessException;
import com.ds.member.social.impl.WechatAuthClient;
import com.ds.member.social.impl.WorkWechatAuthClient;
import com.ds.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialClientFactory {

    private final SysConfigService sysConfigService;

    public SocialAuthClient getClient(SocialType type) {
        if (type == null) {
            throw new BusinessException("社交类型不能为空");
        }
        return switch (type) {
            case WECHAT_WEB, WECHAT_MP, WECHAT_MINIAPP -> new WechatAuthClient(type, sysConfigService);
            case WORKWECHAT -> new WorkWechatAuthClient(sysConfigService);
        };
    }
}
