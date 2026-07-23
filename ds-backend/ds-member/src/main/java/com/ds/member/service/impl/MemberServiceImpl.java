package com.ds.member.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.common.constant.Constants;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.exception.BusinessException;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.member.dto.MemberAdminDTO;
import com.ds.member.dto.MemberLoginDTO;
import com.ds.member.dto.MemberRegisterDTO;
import com.ds.member.dto.MemberUpdateDTO;
import com.ds.member.entity.Member;
import com.ds.member.entity.MemberLevel;
import com.ds.member.entity.MemberPath;
import com.ds.member.entity.MemberSocial;
import com.ds.member.mapper.MemberLevelMapper;
import com.ds.member.mapper.MemberMapper;
import com.ds.member.mapper.MemberPathMapper;
import com.ds.member.mapper.MemberSocialMapper;
import com.ds.member.service.MemberService;
import com.ds.member.social.core.SocialType;
import com.ds.member.vo.MemberVO;
import com.ds.member.vo.TeamTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    private final MemberPathMapper memberPathMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final MemberSocialMapper memberSocialMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<MemberVO> register(MemberRegisterDTO dto) {
        if (StrUtil.isBlank(dto.getPhone())) {
            return Result.fail("手机号不能为空");
        }
        if (StrUtil.isBlank(dto.getPassword())) {
            return Result.fail("密码不能为空");
        }

        Member existMember = lambdaQuery().eq(Member::getPhone, dto.getPhone()).one();
        if (existMember != null) {
            return Result.fail("该手机号已注册");
        }

        Member member = new Member();
        member.setMemberNo(IdUtil.getSnowflakeNextIdStr());
        member.setPhone(dto.getPhone());
        member.setPassword(passwordEncoder.encode(dto.getPassword()));
        member.setRealName(dto.getRealName());
        member.setIdCard(dto.getIdCard());
        member.setNickname(dto.getPhone());
        member.setAvatar(Constants.DEFAULT_AVATAR);
        member.setEmail(dto.getEmail());
        member.setEmailVerified(0);
        member.setPhoneVerified(1);
        member.setWalletBalance(BigDecimal.ZERO);
        member.setFrozenAmount(BigDecimal.ZERO);
        member.setTotalPv(BigDecimal.ZERO);
        member.setStatus(Constants.STATUS_ENABLE);

        String inviteCode = StrUtil.trim(dto.getInviteCode());
        Long recommendId = Constants.ROOT_PARENT_ID;
        String ancestorPath = String.valueOf(Constants.ROOT_PARENT_ID);

        if (StrUtil.isNotBlank(inviteCode)) {
            Member recommender = lambdaQuery().eq(Member::getMemberNo, inviteCode).one();
            if (recommender == null) {
                return Result.fail("邀请码无效");
            }
            recommendId = recommender.getId();
            member.setRecommendId(recommendId);
            member.setParentId(recommendId);

            if (StrUtil.isNotBlank(recommender.getAncestorPath()) && !String.valueOf(Constants.ROOT_PARENT_ID).equals(recommender.getAncestorPath())) {
                ancestorPath = recommender.getAncestorPath() + "," + recommender.getId();
            } else {
                ancestorPath = recommender.getId().toString();
            }
        } else {
            member.setRecommendId(Constants.ROOT_PARENT_ID);
            member.setParentId(Constants.ROOT_PARENT_ID);
        }

        member.setAncestorPath(ancestorPath);

        boolean saved = this.save(member);
        if (!saved) {
            return Result.fail("注册失败");
        }

        buildMemberPath(member.getId(), member.getAncestorPath());

        log.info("会员注册成功: memberNo={}, phone={}", member.getMemberNo(), member.getPhone());
        return Result.ok(toMemberVO(member));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<MemberVO> login(MemberLoginDTO dto) {
        if (StrUtil.isBlank(dto.getPhone())) {
            return Result.fail("手机号不能为空");
        }

        Member member = lambdaQuery().eq(Member::getPhone, dto.getPhone()).one();
        if (member == null) {
            return Result.fail("手机号或密码错误");
        }

        if (member.getStatus() != null && member.getStatus() == Constants.STATUS_DISABLE) {
            return Result.fail("账号已被禁用，请联系管理员");
        }

        if (StrUtil.isNotBlank(dto.getCode())) {
            // 短信验证码登录，验证码由调用方校验
        } else if (StrUtil.isNotBlank(dto.getPassword())) {
            if (!passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
                return Result.fail("手机号或密码错误");
            }
        } else {
            return Result.fail("密码或验证码不能为空");
        }

        SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).login(member.getId());

        member.setLastLoginTime(LocalDateTime.now());
        this.updateById(member);

        SaTokenInfo tokenInfo = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getTokenInfo();

        MemberVO vo = toMemberVO(member);
        vo.setToken(tokenInfo.getTokenValue());

        log.info("会员登录成功: phone={}, memberId={}", member.getPhone(), member.getId());
        return Result.ok(vo);
    }

    @Override
    public Result<MemberVO> getById(Long id) {
        Member member = super.getById(id);
        if (member == null) {
            return Result.fail("会员不存在");
        }
        return Result.ok(toMemberVO(member));
    }

    @Override
    public Result<PageResult<MemberVO>> page(Page<MemberVO> pageParam, String keyword, String phone, String realName, Long level, Integer status) {
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(Member::getPhone, keyword)
                    .or()
                    .like(Member::getMemberNo, keyword)
                    .or()
                    .like(Member::getRealName, keyword)
                    .or()
                    .like(Member::getNickname, keyword));
        }
        if (StrUtil.isNotBlank(phone)) {
            wrapper.like(Member::getPhone, phone);
        }
        if (StrUtil.isNotBlank(realName)) {
            wrapper.like(Member::getRealName, realName);
        }
        if (level != null) {
            wrapper.eq(Member::getLevelId, level);
        }
        if (status != null) {
            wrapper.eq(Member::getStatus, status);
        }
        wrapper.orderByDesc(Member::getCreateTime);

        Page<Member> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        Page<Member> result = this.page(page, wrapper);

        List<MemberVO> voList = result.getRecords().stream()
                .map(this::toMemberVO)
                .collect(Collectors.toList());

        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), voList));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStatus(Long id, Integer status) {
        if (id == null) {
            return Result.fail("会员ID不能为空");
        }
        if (status == null) {
            return Result.fail("状态不能为空");
        }

        Member member = super.getById(id);
        if (member == null) {
            return Result.fail("会员不存在");
        }

        Member updateMember = new Member();
        updateMember.setId(id);
        updateMember.setStatus(status);
        boolean updated = this.updateById(updateMember);
        if (!updated) {
            return Result.fail("修改会员状态失败");
        }
        log.info("修改会员状态成功: id={}, status={}", id, status);
        return Result.ok();
    }

    @Override
    public Result<List<TeamTreeVO>> getTeamTree(Long memberId, int depth) {
        if (memberId == null) {
            return Result.fail("会员ID不能为空");
        }
        Member member = super.getById(memberId);
        if (member == null) {
            return Result.fail("会员不存在");
        }

        List<TeamTreeVO> tree = buildTeamTree(memberId, depth, 0);
        return Result.ok(tree);
    }

    private List<TeamTreeVO> buildTeamTree(Long parentId, int maxDepth, int currentDepth) {
        if (currentDepth >= maxDepth) {
            return new ArrayList<>();
        }

        List<Member> children = lambdaQuery()
                .eq(Member::getParentId, parentId)
                .eq(Member::getStatus, Constants.STATUS_ENABLE)
                .list();

        List<TeamTreeVO> vos = new ArrayList<>();
        for (Member child : children) {
            TeamTreeVO vo = new TeamTreeVO();
            vo.setId(child.getId());
            vo.setMemberNo(child.getMemberNo());
            vo.setPhone(child.getPhone());
            vo.setRealName(child.getRealName());
            vo.setLevelName(getLevelName(child.getLevelId()));
            vo.setJoinTime(child.getCreateTime());
            vo.setChildren(buildTeamTree(child.getId(), maxDepth, currentDepth + 1));
            vos.add(vo);
        }
        return vos;
    }

    private void buildMemberPath(Long descendantId, String ancestorPath) {
        Set<Long> ancestorIdSet = new LinkedHashSet<>();
        ancestorIdSet.add(Constants.ROOT_PARENT_ID);

        if (StrUtil.isNotBlank(ancestorPath) && !String.valueOf(Constants.ROOT_PARENT_ID).equals(ancestorPath)) {
            for (String ancestorIdStr : ancestorPath.split(",")) {
                if (StrUtil.isBlank(ancestorIdStr)) {
                    continue;
                }
                ancestorIdSet.add(Long.parseLong(ancestorIdStr.trim()));
            }
        }

        List<MemberPath> pathList = new ArrayList<>();
        int depth = ancestorIdSet.size();
        for (Long ancestorId : ancestorIdSet) {
            MemberPath path = new MemberPath();
            path.setAncestorId(ancestorId);
            path.setDescendantId(descendantId);
            path.setDepth(depth);
            pathList.add(path);
            depth--;
        }

        MemberPath selfPath = new MemberPath();
        selfPath.setAncestorId(descendantId);
        selfPath.setDescendantId(descendantId);
        selfPath.setDepth(0);
        pathList.add(selfPath);

        if (!pathList.isEmpty()) {
            memberPathMapper.insertBatch(pathList);
        }
    }

    private MemberVO toMemberVO(Member member) {
        if (member == null) {
            return null;
        }
        MemberVO vo = new MemberVO();
        vo.setId(member.getId());
        vo.setMemberNo(member.getMemberNo());
        vo.setPhone(member.getPhone());
        vo.setRealName(member.getRealName());
        vo.setIdCard(member.getIdCard());
        vo.setNickname(member.getNickname());
        vo.setAvatar(member.getAvatar());
        vo.setEmail(member.getEmail());
        vo.setEmailVerified(member.getEmailVerified());
        vo.setPhoneVerified(member.getPhoneVerified());
        vo.setLevelId(member.getLevelId());
        vo.setLevelName(getLevelName(member.getLevelId()));
        vo.setRecommendId(member.getRecommendId());
        vo.setRecommendName(getMemberName(member.getRecommendId()));
        vo.setParentId(member.getParentId());
        vo.setParentName(getMemberName(member.getParentId()));
        vo.setPosition(member.getPosition());
        vo.setAncestorPath(member.getAncestorPath());
        vo.setWalletBalance(member.getWalletBalance());
        vo.setFrozenAmount(member.getFrozenAmount());
        vo.setTotalPv(member.getTotalPv());
        vo.setStatus(member.getStatus());
        vo.setRegisterIp(member.getRegisterIp());
        vo.setLastLoginTime(member.getLastLoginTime());
        vo.setCreateTime(member.getCreateTime());
        vo.setWechatBound(hasSocialBound(member.getId(), SocialType.WECHAT_WEB) ? 1 : 0);
        vo.setWorkWechatBound(hasSocialBound(member.getId(), SocialType.WORKWECHAT) ? 1 : 0);
        if (SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).isLogin()) {
            vo.setPermissions(SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getPermissionList());
        }
        return vo;
    }

    private boolean hasSocialBound(Long memberId, SocialType type) {
        if (memberId == null || type == null) {
            return false;
        }
        return memberSocialMapper.selectCount(
                new LambdaQueryWrapper<MemberSocial>()
                        .eq(MemberSocial::getMemberId, memberId)
                        .eq(MemberSocial::getSocialType, type.getCode())) > 0;
    }

    private String getLevelName(Long levelId) {
        if (levelId == null) {
            return null;
        }
        MemberLevel level = memberLevelMapper.selectById(levelId);
        return level != null ? level.getName() : null;
    }

    private String getMemberName(Long memberId) {
        if (memberId == null || memberId.equals(Constants.ROOT_PARENT_ID)) {
            return null;
        }
        Member member = super.getById(memberId);
        return member != null ? (StrUtil.isNotBlank(member.getRealName()) ? member.getRealName() : member.getPhone()) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deductWallet(Long memberId, BigDecimal amount) {
        if (memberId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("扣减金额无效");
        }
        Member member = super.getById(memberId);
        if (member == null) {
            return Result.fail("会员不存在");
        }
        BigDecimal balance = member.getWalletBalance() != null ? member.getWalletBalance() : BigDecimal.ZERO;
        if (balance.compareTo(amount) < 0) {
            return Result.fail("余额不足");
        }
        Member update = new Member();
        update.setId(memberId);
        update.setWalletBalance(balance.subtract(amount));
        update.setVersion(member.getVersion());
        boolean updated = this.updateById(update);
        if (!updated) {
            throw new BusinessException("余额扣减失败，请重试");
        }
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addWallet(Long memberId, BigDecimal amount) {
        if (memberId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("入账金额无效");
        }
        Member member = super.getById(memberId);
        if (member == null) {
            return Result.fail("会员不存在");
        }
        BigDecimal balance = member.getWalletBalance() != null ? member.getWalletBalance() : BigDecimal.ZERO;
        Member update = new Member();
        update.setId(memberId);
        update.setWalletBalance(balance.add(amount));
        update.setVersion(member.getVersion());
        boolean updated = this.updateById(update);
        if (!updated) {
            throw new BusinessException("余额入账失败，请重试");
        }
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<MemberVO> addByAdmin(MemberAdminDTO dto) {
        if (StrUtil.isBlank(dto.getPhone())) {
            return Result.fail("手机号不能为空");
        }
        Member exist = lambdaQuery().eq(Member::getPhone, dto.getPhone()).one();
        if (exist != null) {
            return Result.fail("手机号已存在");
        }

        Member member = new Member();
        member.setMemberNo(IdUtil.getSnowflakeNextIdStr());
        member.setPhone(dto.getPhone());
        member.setPassword(passwordEncoder.encode(StrUtil.isNotBlank(dto.getPassword()) ? dto.getPassword() : "123456"));
        member.setRealName(dto.getRealName());
        member.setIdCard(dto.getIdCard());
        member.setNickname(dto.getPhone());
        member.setAvatar(Constants.DEFAULT_AVATAR);
        member.setLevelId(dto.getLevelId());
        member.setWalletBalance(BigDecimal.ZERO);
        member.setFrozenAmount(BigDecimal.ZERO);
        member.setTotalPv(BigDecimal.ZERO);
        member.setStatus(dto.getStatus() != null ? dto.getStatus() : Constants.STATUS_ENABLE);

        Long recommendId = dto.getRecommendId();
        if (recommendId != null && !recommendId.equals(Constants.ROOT_PARENT_ID)) {
            Member recommender = super.getById(recommendId);
            if (recommender == null) {
                return Result.fail("推荐人不存在");
            }
            member.setRecommendId(recommendId);
            member.setParentId(recommendId);
            String ancestorPath = recommender.getAncestorPath() != null
                    ? recommender.getAncestorPath() + "," + recommendId
                    : recommendId.toString();
            member.setAncestorPath(ancestorPath);
        } else {
            member.setRecommendId(Constants.ROOT_PARENT_ID);
            member.setParentId(Constants.ROOT_PARENT_ID);
            member.setAncestorPath("0");
        }

        this.save(member);
        return Result.ok(toMemberVO(member));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<MemberVO> updateByAdmin(MemberAdminDTO dto) {
        if (dto.getId() == null) {
            return Result.fail("会员ID不能为空");
        }
        Member member = super.getById(dto.getId());
        if (member == null) {
            return Result.fail("会员不存在");
        }
        if (StrUtil.isNotBlank(dto.getPhone()) && !dto.getPhone().equals(member.getPhone())) {
            Member exist = lambdaQuery().eq(Member::getPhone, dto.getPhone()).ne(Member::getId, dto.getId()).one();
            if (exist != null) {
                return Result.fail("手机号已存在");
            }
            member.setPhone(dto.getPhone());
        }
        if (StrUtil.isNotBlank(dto.getRealName())) {
            member.setRealName(dto.getRealName());
        }
        if (StrUtil.isNotBlank(dto.getIdCard())) {
            member.setIdCard(dto.getIdCard());
        }
        if (dto.getLevelId() != null) {
            member.setLevelId(dto.getLevelId());
        }
        if (dto.getStatus() != null) {
            member.setStatus(dto.getStatus());
        }
        if (StrUtil.isNotBlank(dto.getPassword())) {
            member.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        Long recommendId = dto.getRecommendId();
        if (recommendId != null && !recommendId.equals(member.getRecommendId())) {
            if (!recommendId.equals(Constants.ROOT_PARENT_ID)) {
                Member recommender = super.getById(recommendId);
                if (recommender == null) {
                    return Result.fail("推荐人不存在");
                }
                member.setRecommendId(recommendId);
                member.setParentId(recommendId);
                String ancestorPath = recommender.getAncestorPath() != null
                        ? recommender.getAncestorPath() + "," + recommendId
                        : recommendId.toString();
                member.setAncestorPath(ancestorPath);
            } else {
                member.setRecommendId(Constants.ROOT_PARENT_ID);
                member.setParentId(Constants.ROOT_PARENT_ID);
                member.setAncestorPath("0");
            }
        }

        this.updateById(member);
        return Result.ok(toMemberVO(member));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<MemberVO> updateProfile(Long memberId, MemberUpdateDTO dto) {
        Member member = super.getById(memberId);
        if (member == null) {
            return Result.fail("会员不存在");
        }

        if (StrUtil.isNotBlank(dto.getPhone()) && !dto.getPhone().equals(member.getPhone())) {
            Member exist = lambdaQuery().eq(Member::getPhone, dto.getPhone()).ne(Member::getId, memberId).one();
            if (exist != null) {
                return Result.fail("手机号已存在");
            }
            member.setPhone(dto.getPhone());
            member.setPhoneVerified(1);
        }
        if (StrUtil.isNotBlank(dto.getEmail()) && !dto.getEmail().equals(member.getEmail())) {
            Member exist = lambdaQuery().eq(Member::getEmail, dto.getEmail()).ne(Member::getId, memberId).one();
            if (exist != null) {
                return Result.fail("邮箱已存在");
            }
            member.setEmail(dto.getEmail());
            member.setEmailVerified(1);
        }
        if (StrUtil.isNotBlank(dto.getRealName())) {
            member.setRealName(dto.getRealName());
        }
        if (StrUtil.isNotBlank(dto.getIdCard())) {
            member.setIdCard(dto.getIdCard());
        }
        if (StrUtil.isNotBlank(dto.getAvatar())) {
            member.setAvatar(dto.getAvatar());
        }

        this.updateById(member);
        return Result.ok(toMemberVO(member));
    }
}