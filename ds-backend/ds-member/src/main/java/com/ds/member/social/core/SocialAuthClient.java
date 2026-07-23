package com.ds.member.social.core;

public interface SocialAuthClient {

    SocialType type();

    String getAuthUrl(String redirectUri, String state);

    SocialUserInfo getUserInfo(String code);
}
