package com.ds.member.social.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.common.constant.Constants;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.exception.BusinessException;
import com.ds.common.result.Result;
import com.ds.member.entity.Member;
import com.ds.member.entity.MemberPath;
import com.ds.member.entity.MemberSocial;
import com.ds.member.mapper.MemberMapper;
import com.ds.member.mapper.MemberPathMapper;
import com.ds.member.mapper.MemberSocialMapper;
import com.ds.member.service.MemberService;
import com.ds.member.social.core.SocialAuthClient;
import com.ds.member.social.core.SocialClientFactory;
import com.ds.member.social.core.SocialType;
import com.ds.member.social.core.SocialUserInfo;
import com.ds.member.social.service.MemberSocialService;
import com.ds.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberSocialServiceImpl extends ServiceImpl<MemberSocialMapper, MemberSocial> implements MemberSocialService {

    private final MemberMapper memberMapper;
    private final MemberPathMapper memberPathMapper;
    private final MemberService memberService;
    private final SocialClientFactory socialClientFactory;

    @Override
    public String getAuthUrl(SocialType type, String redirectUri) {
        if (type == null || StrUtil.isBlank(redirectUri)) {
            throw new BusinessException("社交类型或回调地址不能为空");
        }
        SocialAuthClient client = socialClientFactory.getClient(type);
        String state = IdUtil.simpleUUID();
        return client.getAuthUrl(redirectUri, state);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<MemberVO> socialLogin(SocialType type, String code) {
        if (type == null || StrUtil.isBlank(code)) {
            return Result.fail("社交类型或授权码不能为空");
        }

        SocialAuthClient client = socialClientFactory.getClient(type);
        SocialUserInfo userInfo = client.getUserInfo(code);

        MemberSocial social = findSocialBinding(type, userInfo);
        Member member;
        if (social != null) {
            member = memberMapper.selectById(social.getMemberId());
            if (member == null) {
                return Result.fail("绑定的会员不存在");
            }
        } else {
            member = createMemberFromSocial(userInfo);
            createSocialBinding(member.getId(), type, userInfo);
        }

        if (member.getStatus() != null && member.getStatus() == Constants.STATUS_DISABLE) {
            return Result.fail("账号已被禁用，请联系管理员");
        }

        member.setLastLoginTime(LocalDateTime.now());
        memberMapper.updateById(member);

        SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).login(member.getId());
        SaTokenInfo tokenInfo = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getTokenInfo();

        Result<MemberVO> memberResult = memberService.getById(member.getId());
        if (!memberResult.isSuccess()) {
            return memberResult;
        }
        MemberVO vo = memberResult.getData();
        vo.setToken(tokenInfo.getTokenValue());

        log.info("社交登录成功: type={}, memberId={}", type.getCode(), member.getId());
        return Result.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> bindSocialAccount(SocialType type, String code) {
        if (type == null || StrUtil.isBlank(code)) {
            return Result.fail("社交类型或授权码不能为空");
        }
        long currentMemberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();

        SocialAuthClient client = socialClientFactory.getClient(type);
        SocialUserInfo userInfo = client.getUserInfo(code);

        MemberSocial exist = findSocialBinding(type, userInfo);
        if (exist != null && !exist.getMemberId().equals(currentMemberId)) {
            return Result.fail("该社交账号已绑定其他会员");
        }
        if (exist != null) {
            return Result.ok();
        }

        createSocialBinding(currentMemberId, type, userInfo);
        log.info("绑定社交账号成功: type={}, memberId={}", type.getCode(), currentMemberId);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unbindSocialAccount(SocialType type) {
        if (type == null) {
            return Result.fail("社交类型不能为空");
        }
        long currentMemberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();

        LambdaQueryWrapper<MemberSocial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberSocial::getMemberId, currentMemberId)
                .eq(MemberSocial::getSocialType, type.getCode());
        boolean removed = this.remove(wrapper);
        if (!removed) {
            return Result.fail("解绑失败，未找到绑定记录");
        }
        log.info("解绑社交账号成功: type={}, memberId={}", type.getCode(), currentMemberId);
        return Result.ok();
    }

    private MemberSocial findSocialBinding(SocialType type, SocialUserInfo userInfo) {
        if (StrUtil.isNotBlank(userInfo.getUnionId())) {
            MemberSocial social = lambdaQuery()
                    .eq(MemberSocial::getSocialType, type.getCode())
                    .eq(MemberSocial::getUnionId, userInfo.getUnionId())
                    .one();
            if (social != null) {
                return social;
            }
        }
        if (StrUtil.isNotBlank(userInfo.getOpenId())) {
            return lambdaQuery()
                    .eq(MemberSocial::getSocialType, type.getCode())
                    .eq(MemberSocial::getOpenId, userInfo.getOpenId())
                    .one();
        }
        return null;
    }

    private Member createMemberFromSocial(SocialUserInfo userInfo) {
        Member member = new Member();
        member.setMemberNo(IdUtil.getSnowflakeNextIdStr());
        member.setNickname(StrUtil.isNotBlank(userInfo.getNickname()) ? userInfo.getNickname() : defaultNickname(userInfo.getSocialType()));
        member.setAvatar(StrUtil.isNotBlank(userInfo.getAvatar()) ? userInfo.getAvatar() : Constants.DEFAULT_AVATAR);
        member.setWalletBalance(BigDecimal.ZERO);
        member.setFrozenAmount(BigDecimal.ZERO);
        member.setTotalPv(BigDecimal.ZERO);
        member.setStatus(Constants.STATUS_ENABLE);
        member.setRecommendId(Constants.ROOT_PARENT_ID);
        member.setParentId(Constants.ROOT_PARENT_ID);
        member.setAncestorPath("0");
        memberMapper.insert(member);

        MemberPath rootPath = new MemberPath();
        rootPath.setAncestorId(0L);
        rootPath.setDescendantId(member.getId());
        rootPath.setDepth(1);
        memberPathMapper.insertBatch(List.of(rootPath));

        return member;
    }

    private void createSocialBinding(Long memberId, SocialType type, SocialUserInfo userInfo) {
        MemberSocial social = new MemberSocial();
        social.setMemberId(memberId);
        social.setSocialType(type.getCode());
        social.setUnionId(userInfo.getUnionId());
        social.setOpenId(userInfo.getOpenId());
        social.setNickname(userInfo.getNickname());
        social.setAvatar(userInfo.getAvatar());
        social.setRawData(userInfo.getRawData());
        this.save(social);
    }

    private String defaultNickname(SocialType type) {
        return switch (type) {
            case WECHAT_WEB, WECHAT_MP, WECHAT_MINIAPP -> "微信用户";
            case WORKWECHAT -> "企业微信用户";
        };
    }
}
