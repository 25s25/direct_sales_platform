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
public class WorkWechatAuthClient implements SocialAuthClient {

    private static final String CONFIG_CORPID = "social.workwechat.corp-id";
    private static final String CONFIG_AGENTID = "social.workwechat.agent-id";
    private static final String CONFIG_SECRET = "social.workwechat.secret";

    private final SysConfigService sysConfigService;

    public WorkWechatAuthClient(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @Override
    public SocialType type() {
        return SocialType.WORKWECHAT;
    }

    @Override
    public String getAuthUrl(String redirectUri, String state) {
        String corpid = getConfigValue(CONFIG_CORPID);
        String agentid = getConfigValue(CONFIG_AGENTID);
        if (StrUtil.isBlank(corpid) || StrUtil.isBlank(agentid)) {
            throw new BusinessException("企业微信配置不完整");
        }
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String safeState = StrUtil.isBlank(state) ? "STATE" : state;
        return String.format(
                "https://open.work.weixin.qq.com/wwopen/sso/qrconnect?appid=%s&agentid=%s&redirect_uri=%s&state=%s",
                corpid, agentid, encodedRedirectUri, safeState);
    }

    @Override
    public SocialUserInfo getUserInfo(String code) {
        String corpid = getConfigValue(CONFIG_CORPID);
        String secret = getConfigValue(CONFIG_SECRET);
        if (StrUtil.isBlank(corpid) || StrUtil.isBlank(secret)) {
            throw new BusinessException("企业微信配置不完整");
        }

        String accessToken = getAccessToken(corpid, secret);
        UserTicketResult userResult = getUserTicket(accessToken, code);
        return getUserDetail(accessToken, userResult);
    }

    private String getAccessToken(String corpid, String secret) {
        String url = String.format(
                "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s",
                corpid, secret);
        String resp = HttpUtil.get(url);
        JSONObject json = JSONUtil.parseObj(resp);
        if (json.getInt("errcode", 0) != 0) {
            log.error("企业微信 access_token 获取失败: {}", resp);
            throw new BusinessException("企业微信授权失败: " + json.getStr("errmsg"));
        }
        String accessToken = json.getStr("access_token");
        if (StrUtil.isBlank(accessToken)) {
            throw new BusinessException("企业微信授权失败，未获取到 access_token");
        }
        return accessToken;
    }

    private UserTicketResult getUserTicket(String accessToken, String code) {
        String url = String.format(
                "https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo?access_token=%s&code=%s",
                accessToken, code);
        String resp = HttpUtil.get(url);
        JSONObject json = JSONUtil.parseObj(resp);
        if (json.getInt("errcode", 0) != 0) {
            log.error("企业微信 userId 获取失败: {}", resp);
            throw new BusinessException("企业微信授权失败: " + json.getStr("errmsg"));
        }
        String userId = json.getStr("UserId");
        String userTicket = json.getStr("user_ticket");
        if (StrUtil.isBlank(userId)) {
            throw new BusinessException("企业微信授权失败，未获取到用户 ID");
        }
        return new UserTicketResult(userId, userTicket);
    }

    private SocialUserInfo getUserDetail(String accessToken, UserTicketResult userResult) {
        SocialUserInfo userInfo = new SocialUserInfo();
        userInfo.setSocialType(SocialType.WORKWECHAT);
        userInfo.setOpenId(userResult.userId);

        if (StrUtil.isBlank(userResult.userTicket)) {
            userInfo.setRawData(JSONUtil.createObj().set("UserId", userResult.userId).toString());
            return userInfo;
        }

        String url = String.format(
                "https://qyapi.weixin.qq.com/cgi-bin/user/getuserdetail?access_token=%s",
                accessToken);
        JSONObject param = new JSONObject();
        param.set("user_ticket", userResult.userTicket);
        String resp = HttpUtil.post(url, param.toString());
        JSONObject json = JSONUtil.parseObj(resp);
        if (json.getInt("errcode", 0) != 0) {
            log.error("企业微信用户详情获取失败: {}", resp);
            throw new BusinessException("企业微信用户信息获取失败: " + json.getStr("errmsg"));
        }

        userInfo.setNickname(json.getStr("name"));
        userInfo.setAvatar(json.getStr("avatar"));
        userInfo.setRawData(resp);
        return userInfo;
    }

    private record UserTicketResult(String userId, String userTicket) {
    }

    private String getConfigValue(String key) {
        Result<SysConfig> result = sysConfigService.getByKey(key);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return null;
        }
        return result.getData().getConfigValue();
    }
}
