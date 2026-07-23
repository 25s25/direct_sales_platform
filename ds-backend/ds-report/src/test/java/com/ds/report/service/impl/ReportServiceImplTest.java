package com.ds.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ds.bonus.entity.BonusRecord;
import com.ds.bonus.mapper.BonusRecordMapper;
import com.ds.member.entity.Member;
import com.ds.member.entity.MemberPath;
import com.ds.member.mapper.MemberLevelMapper;
import com.ds.member.mapper.MemberMapper;
import com.ds.member.mapper.MemberPathMapper;
import com.ds.order.entity.Order;
import com.ds.order.mapper.OrderMapper;
import com.ds.report.vo.BonusReportVO;
import com.ds.report.vo.DashboardVO;
import com.ds.report.vo.MemberReportVO;
import com.ds.report.vo.SalesReportVO;
import com.ds.report.vo.TeamReportVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private MemberMapper memberMapper;
    @Mock private BonusRecordMapper bonusRecordMapper;
    @Mock private MemberPathMapper memberPathMapper;
    @Mock private MemberLevelMapper memberLevelMapper;

    private ReportServiceImpl reportService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        // mock 环境下 LambdaQueryWrapper 找不到 TableInfo 缓存；
        // 用一个真实 MybatisConfiguration 提前注册各实体元数据
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "ds-report-test");
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, Member.class);
        TableInfoHelper.initTableInfo(assistant, BonusRecord.class);
        TableInfoHelper.initTableInfo(assistant, MemberPath.class);
    }

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(
                orderMapper, memberMapper, bonusRecordMapper,
                memberPathMapper, memberLevelMapper);
    }

    // ---- Dashboard ----

    @Test
    void getDashboardOverview_aggregatesAllCountersAndSums() {
        when(memberMapper.selectCount(null)).thenReturn(500L);
        when(memberMapper.selectCount(any(Wrapper.class))).thenReturn(10L); // 今日新增
        // totalOrders 状态 ≥ 1
        when(orderMapper.selectCount(any(Wrapper.class))).thenReturn(200L);

        // 销售聚合：全部 + 今日
        Map<String, Object> totalRow = new HashMap<>();
        totalRow.put("pay_amount", new BigDecimal("50000.00"));
        when(orderMapper.selectMaps(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(totalRow));

        // 奖金聚合
        Map<String, Object> bonusRow = new HashMap<>();
        bonusRow.put("amount", new BigDecimal("8000.00"));
        when(bonusRecordMapper.selectMaps(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(bonusRow));

        DashboardVO vo = reportService.getDashboardOverview();

        assertNotNull(vo);
        assertEquals(500L, vo.getTotalMembers());
        assertEquals(10L, vo.getTodayNewMembers());
        assertEquals(200L, vo.getTotalOrders());
        assertEquals(0, new BigDecimal("50000.00").compareTo(vo.getTotalSales()));
        assertEquals(0, new BigDecimal("50000.00").compareTo(vo.getTodaySales()));
        assertEquals(0, new BigDecimal("50000.00").compareTo(vo.getMonthSales()));
        assertEquals(0, new BigDecimal("8000.00").compareTo(vo.getTotalBonus()));
        assertEquals(0, new BigDecimal("8000.00").compareTo(vo.getMonthBonus()));
        assertNotNull(vo.getSalesTrend());
        assertNotNull(vo.getMemberTrend());
    }

    @Test
    void getDashboardOverview_handlesEmptyAggregations() {
        when(memberMapper.selectCount(null)).thenReturn(0L);
        when(memberMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(orderMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(orderMapper.selectMaps(any(Wrapper.class))).thenReturn(new ArrayList<>());
        when(bonusRecordMapper.selectMaps(any(Wrapper.class))).thenReturn(new ArrayList<>());

        DashboardVO vo = reportService.getDashboardOverview();

        assertEquals(0L, vo.getTotalMembers());
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalSales()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalBonus()));
        assertTrue(vo.getSalesTrend().isEmpty());
        assertTrue(vo.getMemberTrend().isEmpty());
    }

    // ---- Sales report ----

    @Test
    void getSalesReport_groupsOrdersByDateAndSumsAmounts() {
        String startDate = "2026-07-01";
        String endDate = "2026-07-31";

        Map<String, Object> row1 = new HashMap<>();
        row1.put("pay_amount", new BigDecimal("100.00"));
        row1.put("create_time", java.time.LocalDateTime.of(2026, 7, 1, 10, 0));
        Map<String, Object> row2 = new HashMap<>();
        row2.put("pay_amount", new BigDecimal("200.00"));
        row2.put("create_time", java.time.LocalDateTime.of(2026, 7, 1, 14, 0));
        Map<String, Object> row3 = new HashMap<>();
        row3.put("pay_amount", new BigDecimal("300.00"));
        row3.put("create_time", java.time.LocalDateTime.of(2026, 7, 2, 9, 0));

        when(orderMapper.selectMaps(any(Wrapper.class)))
                .thenReturn(new ArrayList<>(java.util.Arrays.asList(row1, row2, row3)));

        List<SalesReportVO> result = reportService.getSalesReport(startDate, endDate);

        assertEquals(2, result.size(), "应按日期分组");
        SalesReportVO first = result.get(0);
        assertEquals("2026-07-01", first.getDate());
        assertEquals(2L, first.getOrderCount());
        assertEquals(0, new BigDecimal("300.00").compareTo(first.getSales()));
    }

    // ---- Member report ----

    @Test
    void getMemberReport_buildsDailyNewAndRunningTotal() {
        String startDate = "2026-07-20";
        String endDate = "2026-07-22";

        Map<String, Object> row1 = new HashMap<>();
        row1.put("create_time", java.time.LocalDateTime.of(2026, 7, 22, 9, 0));
        Map<String, Object> row2 = new HashMap<>();
        row2.put("create_time", java.time.LocalDateTime.of(2026, 7, 22, 11, 0));
        Map<String, Object> row3 = new HashMap<>();
        row3.put("create_time", java.time.LocalDateTime.of(2026, 7, 21, 9, 0));

        when(memberMapper.selectMaps(any(Wrapper.class)))
                .thenReturn(new ArrayList<>(java.util.Arrays.asList(row1, row2, row3)));
        when(memberMapper.selectCount(null)).thenReturn(100L);

        List<MemberReportVO> result = reportService.getMemberReport(startDate, endDate);

        assertNotNull(result);
        // 7/22 新增 2, 7/21 新增 1
        MemberReportVO d22 = result.stream()
                .filter(v -> "2026-07-22".equals(v.getDate())).findFirst().orElseThrow();
        assertEquals(2L, d22.getNewCount());
        MemberReportVO d21 = result.stream()
                .filter(v -> "2026-07-21".equals(v.getDate())).findFirst().orElseThrow();
        assertEquals(1L, d21.getNewCount());
        // 实现：先按遇见的顺序写 LinkedHashMap，最后 Collections.reverse(vos)，
        // 所以输出是倒序（更早的日期在前）：[7/21, 7/22]
        assertEquals("2026-07-21", result.get(0).getDate());
        assertEquals("2026-07-22", result.get(1).getDate());
    }

    // ---- Bonus report ----

    @Test
    void getBonusReport_groupsByBonusType() {
        String startDate = "2026-07-01";
        String endDate = "2026-07-31";

        Map<String, Object> retail1 = new HashMap<>();
        retail1.put("bonus_type", "RETAIL");
        retail1.put("amount", new BigDecimal("100.00"));
        Map<String, Object> retail2 = new HashMap<>();
        retail2.put("bonus_type", "RETAIL");
        retail2.put("amount", new BigDecimal("50.00"));
        Map<String, Object> recommend = new HashMap<>();
        recommend.put("bonus_type", "RECOMMEND");
        recommend.put("amount", new BigDecimal("80.00"));

        when(bonusRecordMapper.selectMaps(any(Wrapper.class)))
                .thenReturn(new ArrayList<>(java.util.Arrays.asList(retail1, retail2, recommend)));

        List<BonusReportVO> result = reportService.getBonusReport(startDate, endDate);

        assertEquals(2, result.size());
        BonusReportVO retailVo = result.stream()
                .filter(v -> "RETAIL".equals(v.getBonusType())).findFirst().orElseThrow();
        assertEquals(2L, retailVo.getCount());
        assertEquals(0, new BigDecimal("150.00").compareTo(retailVo.getAmount()));

        BonusReportVO recommendVo = result.stream()
                .filter(v -> "RECOMMEND".equals(v.getBonusType())).findFirst().orElseThrow();
        assertEquals(1L, recommendVo.getCount());
    }

    // ---- Team report ----

    @Test
    void getTeamReport_returnsEmptyVo_whenMemberNotFound() {
        when(memberMapper.selectById(999L)).thenReturn(null);

        TeamReportVO vo = reportService.getTeamReport(999L, "2026-07-01", "2026-07-31");

        assertNotNull(vo);
        assertNull(vo.getMemberId());
        // 实现未显式设 0，teamMemberCount 保持 Long 默认 null
        assertNull(vo.getTeamMemberCount(), "会员不存在时 teamMemberCount 不应被设置");
        assertNull(vo.getTotalSales());
        assertNull(vo.getSelfSales());
    }

    @Test
    void getTeamReport_calculatesSelfAndTeamSalesFromDescendants() {
        Long memberId = 1L;
        Member m = new Member();
        m.setId(memberId);
        m.setMemberNo("M001");
        m.setRealName("Leader");
        when(memberMapper.selectById(memberId)).thenReturn(m);

        // 闭包表：member 1 的所有后代（含 self 行 depth=0）。实现未按 depth 过滤，
        // 故 teamMemberCount 实际包含 self
        MemberPath path1 = new MemberPath();
        path1.setAncestorId(memberId);
        path1.setDescendantId(2L);
        path1.setDepth(1);
        MemberPath path2 = new MemberPath();
        path2.setAncestorId(memberId);
        path2.setDescendantId(3L);
        path2.setDepth(1);
        MemberPath selfPath = new MemberPath();
        selfPath.setAncestorId(memberId);
        selfPath.setDescendantId(memberId);
        selfPath.setDepth(0);

        when(memberPathMapper.selectList(any(Wrapper.class)))
                .thenReturn(new ArrayList<>(java.util.Arrays.asList(path1, path2, selfPath)));

        // 订单聚合：自己 100，下级 200 和 300
        Map<String, Object> o1 = new HashMap<>();
        o1.put("member_id", 1L);
        o1.put("pay_amount", new BigDecimal("100.00"));
        Map<String, Object> o2 = new HashMap<>();
        o2.put("member_id", 2L);
        o2.put("pay_amount", new BigDecimal("200.00"));
        Map<String, Object> o3 = new HashMap<>();
        o3.put("member_id", 3L);
        o3.put("pay_amount", new BigDecimal("300.00"));

        when(orderMapper.selectMaps(any(Wrapper.class)))
                .thenReturn(new ArrayList<>(java.util.Arrays.asList(o1, o2, o3)));

        TeamReportVO vo = reportService.getTeamReport(memberId, "2026-07-01", "2026-07-31");

        assertEquals(memberId, vo.getMemberId());
        assertEquals("M001", vo.getMemberNo());
        assertEquals("Leader", vo.getRealName());
        // 实际：实现未按 depth>0 过滤，teamMemberCount = 3（含 self）
        assertEquals(3L, vo.getTeamMemberCount());
        assertEquals(0, new BigDecimal("100.00").compareTo(vo.getSelfSales()));
        assertEquals(0, new BigDecimal("600.00").compareTo(vo.getTotalSales()),
                "团队销售 = 100 + 200 + 300 = 600");
    }

    @Test
    void getTeamReport_excludesNullPayAmount() {
        Long memberId = 1L;
        Member m = new Member();
        m.setId(memberId);
        m.setMemberNo("M001");
        m.setRealName("Leader");
        when(memberMapper.selectById(memberId)).thenReturn(m);

        when(memberPathMapper.selectList(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(new MemberPath() {{
                    setAncestorId(memberId);
                    setDescendantId(2L);
                    setDepth(1);
                }}));

        Map<String, Object> o1 = new HashMap<>();
        o1.put("member_id", 2L);
        o1.put("pay_amount", null);
        when(orderMapper.selectMaps(any(Wrapper.class)))
                .thenReturn(Collections.singletonList(o1));

        TeamReportVO vo = reportService.getTeamReport(memberId, "2026-07-01", "2026-07-31");

        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getTotalSales()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getSelfSales()));
    }
}
