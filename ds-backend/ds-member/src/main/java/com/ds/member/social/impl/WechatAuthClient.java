package com.ds.member.social.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ds.common.exception.BusinessException;
import com.ds.common.result.Result;
import com.ds.member.social.core.SocialAuthClient;
import com.ds.member.social.core.SocialType;
import com.ds.member.social.core.SocialUserInfo;
import com.ds.system.entity.SysConfig;
import com.ds.system.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
public class WechatAuthClient implements SocialAuthClient {

    private static final String CONFIG_APPID_PREFIX = "social.wechat.%s.app-id";
    private static final String CONFIG_SECRET_PREFIX = "social.wechat.%s.secret";

    private final SocialType type;
    private final SysConfigService sysConfigService;

    public WechatAuthClient(SocialType type, SysConfigService sysConfigService) {
        if (type != SocialType.WECHAT_WEB && type != SocialType.WECHAT_MP && type != SocialType.WECHAT_MINIAPP) {
            throw new IllegalArgumentException("不支持的微信授权类型: " + type);
        }
        this.type = type;
        this.sysConfigService = sysConfigService;
    }

    private String typeKey() {
        return switch (type) {
            case WECHAT_WEB -> "web";
            case WECHAT_MP -> "mp";
            case WECHAT_MINIAPP -> "miniapp";
            default -> type.getCode();
        };
    }

    @Override
    public SocialType type() {
        return type;
    }

    @Override
    public String getAuthUrl(String redirectUri, String state) {
        String appid = getConfigValue(String.format(CONFIG_APPID_PREFIX, typeKey()));
        if (StrUtil.isBlank(appid)) {
            throw new BusinessException("微信 AppID 未配置");
        }
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String safeState = StrUtil.isBlank(state) ? "STATE" : state;
        if (type == SocialType.WECHAT_WEB) {
            return String.format(
                    "https://open.weixin.qq.com/connect/qrconnect?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_login&state=%s#wechat_redirect",
                    appid, encodedRedirectUri, safeState);
        }
        if (type == SocialType.WECHAT_MP) {
            return String.format(
                    "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_userinfo&state=%s#wechat_redirect",
                    appid, encodedRedirectUri, safeState);
        }
        return "";
    }

    @Override
    public SocialUserInfo getUserInfo(String code) {
        String appid = getConfigValue(String.format(CONFIG_APPID_PREFIX, typeKey()));
        String secret = getConfigValue(String.format(CONFIG_SECRET_PREFIX, typeKey()));
        if (StrUtil.isBlank(appid) || StrUtil.isBlank(secret)) {
            throw new BusinessException("微信应用配置不完整");
        }

        if (type == SocialType.WECHAT_MINIAPP) {
            return getMiniappUserInfo(appid, secret, code);
        }
        return getOpenPlatformUserInfo(appid, secret, code);
    }

    private SocialUserInfo getOpenPlatformUserInfo(String appid, String secret, String code) {
        String tokenUrl = String.format(
                "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                appid, secret, code);
        String tokenResp = HttpUtil.get(tokenUrl);
        JSONObject tokenJson = JSONUtil.parseObj(tokenResp);
        if (tokenJson.containsKey("errcode")) {
            log.error("微信 access_token 获取失败: {}", tokenResp);
            throw new BusinessException("微信授权失败: " + tokenJson.getStr("errmsg"));
        }
        String accessToken = tokenJson.getStr("access_token");
        String openid = tokenJson.getStr("openid");
        if (StrUtil.isBlank(accessToken) || StrUtil.isBlank(openid)) {
            throw new BusinessException("微信授权失败，未获取到 access_token 或 openid");
        }

        String userUrl = String.format(
                "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN",
                accessToken, openid);
        String userResp = HttpUtil.get(userUrl);
        JSONObject userJson = JSONUtil.parseObj(userResp);
        if (userJson.containsKey("errcode")) {
            log.error("微信用户信息获取失败: {}", userResp);
            throw new BusinessException("微信用户信息获取失败: " + userJson.getStr("errmsg"));
        }

        SocialUserInfo userInfo = new SocialUserInfo();
        userInfo.setSocialType(type);
        userInfo.setUnionId(userJson.getStr("unionid"));
        userInfo.setOpenId(userJson.getStr("openid"));
        userInfo.setNickname(userJson.getStr("nickname"));
        userInfo.setAvatar(userJson.getStr("headimgurl"));
        userInfo.setRawData(userResp);
        return userInfo;
    }

    private SocialUserInfo getMiniappUserInfo(String appid, String secret, String code) {
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appid, secret, code);
        String resp = HttpUtil.get(url);
        JSONObject json = JSONUtil.parseObj(resp);
        if (json.containsKey("errcode")) {
            log.error("微信小程序登录失败: {}", resp);
            throw new BusinessException("微信小程序登录失败: " + json.getStr("errmsg"));
        }
        String openid = json.getStr("openid");
        if (StrUtil.isBlank(openid)) {
            throw new BusinessException("微信小程序登录失败，未获取到 openid");
        }
        SocialUserInfo userInfo = new SocialUserInfo();
        userInfo.setSocialType(type);
        userInfo.setUnionId(json.getStr("unionid"));
        userInfo.setOpenId(openid);
        userInfo.setRawData(resp);
        return userInfo;
    }

    private String getConfigValue(String key) {
        Result<SysConfig> result = sysConfigService.getByKey(key);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return null;
        }
        return result.getData().getConfigValue();
    }
}
