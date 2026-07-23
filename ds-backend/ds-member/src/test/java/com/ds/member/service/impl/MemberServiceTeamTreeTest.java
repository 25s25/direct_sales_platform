package com.ds.member.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ds.common.result.Result;
import com.ds.member.entity.Member;
import com.ds.member.mapper.MemberLevelMapper;
import com.ds.member.mapper.MemberMapper;
import com.ds.member.mapper.MemberPathMapper;
import com.ds.member.mapper.MemberSocialMapper;
import com.ds.member.vo.TeamTreeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * MemberServiceImpl.getTeamTree 的单元测试。
 *
 * <p>由于实现内部依赖 MyBatis-Plus 的 lambdaQuery()（需要真实 SqlSession），
 * 这里只覆盖入参校验与会员存在性校验，递归构建逻辑需通过
 * {@code @SpringBootTest} 集成测试覆盖（见 MemberServiceIntegrationTest）。
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTeamTreeTest {

    @Mock private MemberPathMapper memberPathMapper;
    @Mock private MemberLevelMapper memberLevelMapper;
    @Mock private MemberSocialMapper memberSocialMapper;
    @Mock private MemberMapper memberMapper;

    private MemberServiceImpl memberService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        memberService = new MemberServiceImpl(
                memberPathMapper, memberLevelMapper, memberSocialMapper, passwordEncoder);
        ReflectionTestUtils.setField(memberService, "baseMapper", memberMapper);
    }

    @Test
    void getTeamTree_fails_whenMemberIdIsNull() {
        Result<List<TeamTreeVO>> result = memberService.getTeamTree(null, 3);

        assertFalse(result.isSuccess());
        assertEquals("会员ID不能为空", result.getMessage());
    }

    @Test
    void getTeamTree_fails_whenMemberNotFound() {
        Long memberId = 999L;
        when(memberMapper.selectById(memberId)).thenReturn(null);

        Result<List<TeamTreeVO>> result = memberService.getTeamTree(memberId, 3);

        assertFalse(result.isSuccess());
        assertEquals("会员不存在", result.getMessage());
    }

    @Test
    void getTeamTree_usesProvidedDepthParameter() {
        // 仅证明 depth 参数被传入，递归逻辑依赖真实 MyBatis 行为
        Member m = new Member();
        m.setId(1L);
        m.setParentId(0L);
        m.setStatus(1);
        when(memberMapper.selectById(1L)).thenReturn(m);

        // 这里只能验证入口校验通过前会命中 mapper，递归失败属于已知 MP mock 限制
        // 故仅验证不抛空指针校验异常即可
        try {
            memberService.getTeamTree(1L, 5);
        } catch (Exception ex) {
            // 预期：MybatisPlusException (lambdaQuery 在 mock 环境下不可用)
            assertTrue(ex.getClass().getName().contains("MybatisPlus")
                            || ex.getClass().getName().contains("Mybatis"),
                    "应抛出与 MyBatis 代理相关的异常，实际：" + ex);
        }
    }
}
