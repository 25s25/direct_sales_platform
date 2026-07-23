package com.ds.member.social.service;

import com.ds.common.result.Result;
import com.ds.member.social.core.SocialType;
import com.ds.member.vo.MemberVO;

public interface MemberSocialService {

    String getAuthUrl(SocialType type, String redirectUri);

    Result<MemberVO> socialLogin(SocialType type, String code);

    Result<Void> bindSocialAccount(SocialType type, String code);

    Result<Void> unbindSocialAccount(SocialType type);
}
