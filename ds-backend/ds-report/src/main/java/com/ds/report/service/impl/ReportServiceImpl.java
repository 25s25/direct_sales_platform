package com.ds.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ds.bonus.entity.BonusRecord;
import com.ds.bonus.mapper.BonusRecordMapper;
import com.ds.member.entity.Member;
import com.ds.member.entity.MemberLevel;
import com.ds.member.entity.MemberPath;
import com.ds.member.mapper.MemberLevelMapper;
import com.ds.member.mapper.MemberMapper;
import com.ds.member.mapper.MemberPathMapper;
import com.ds.order.entity.Order;
import com.ds.order.mapper.OrderMapper;
import com.ds.report.service.ReportService;
import com.ds.report.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderMapper orderMapper;
    private final MemberMapper memberMapper;
    private final BonusRecordMapper bonusRecordMapper;
    private final MemberPathMapper memberPathMapper;
    private final MemberLevelMapper memberLevelMapper;

    @Override
    public DashboardVO getDashboardOverview() {
        DashboardVO vo = new DashboardVO();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        vo.setTotalMembers(memberMapper.selectCount(null));
        vo.setTodayNewMembers(memberMapper.selectCount(
                new LambdaQueryWrapper<Member>()
                        .between(Member::getCreateTime, todayStart, todayEnd)));

        LambdaQueryWrapper<Order> paidOrderWrapper = new LambdaQueryWrapper<Order>()
                .ge(Order::getStatus, 1);

        vo.setTotalOrders(orderMapper.selectCount(paidOrderWrapper));

        vo.setTodayOrders(orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getStatus, 1)
                        .between(Order::getCreateTime, todayStart, todayEnd)));

        List<Map<String, Object>> orderAgg = orderMapper.selectMaps(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getStatus, 1)
                        .select(Order::getPayAmount)
                        .apply("1=1"));

        BigDecimal totalSales = orderAgg.stream()
                .map(m -> (BigDecimal) m.get("pay_amount"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalSales(totalSales);

        List<Map<String, Object>> todayOrderAgg = orderMapper.selectMaps(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getStatus, 1)
                        .between(Order::getCreateTime, todayStart, todayEnd)
                        .select(Order::getPayAmount)
                        .apply("1=1"));

        BigDecimal todaySales = todayOrderAgg.stream()
                .map(m -> (BigDecimal) m.get("pay_amount"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTodaySales(todaySales);

        vo.setPendingOrders(orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, 0)));

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().atTime(LocalTime.MAX);
        List<Map<String, Object>> monthOrderAgg = orderMapper.selectMaps(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getStatus, 1)
                        .between(Order::getCreateTime, monthStart, monthEnd)
                        .select(Order::getPayAmount)
                        .apply("1=1"));
        BigDecimal monthSales = monthOrderAgg.stream()
                .map(m -> (BigDecimal) m.get("pay_amount"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setMonthSales(monthSales);

        List<Map<String, Object>> bonusAgg = bonusRecordMapper.selectMaps(
                new LambdaQueryWrapper<BonusRecord>()
                        .select(BonusRecord::getAmount)
                        .apply("1=1"));

        BigDecimal totalBonus = bonusAgg.stream()
                .map(m -> (BigDecimal) m.get("amount"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalBonus(totalBonus);

        List<Map<String, Object>> monthBonusAgg = bonusRecordMapper.selectMaps(
                new LambdaQueryWrapper<BonusRecord>()
                        .between(BonusRecord::getCreateTime, monthStart, monthEnd)
                        .select(BonusRecord::getAmount)
                        .apply("1=1"));
        BigDecimal monthBonus = monthBonusAgg.stream()
                .map(m -> (BigDecimal) m.get("amount"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setMonthBonus(monthBonus);

        vo.setSalesTrend(buildSalesTrend(monthStart, monthEnd));
        vo.setMemberTrend(buildMemberTrend(monthStart, monthEnd));

        log.info("Dashboard overview: totalMembers={}, todayNewMembers={}, totalOrders={}, todayOrders={}, pendingOrders={}, totalSales={}, todaySales={}, monthSales={}, totalBonus={}, monthBonus={}",
                vo.getTotalMembers(), vo.getTodayNewMembers(), vo.getTotalOrders(), vo.getTodayOrders(), vo.getPendingOrders(),
                vo.getTotalSales(), vo.getTodaySales(), vo.getMonthSales(), vo.getTotalBonus(), vo.getMonthBonus());

        return vo;
    }

    private List<DashboardVO.SalesTrendVO> buildSalesTrend(LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> results = orderMapper.selectMaps(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getStatus, 1)
                        .between(Order::getCreateTime, start, end)
                        .select(Order::getPayAmount, Order::getCreateTime)
                        .apply("1=1"));

        Map<String, BigDecimal> salesByDate = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            LocalDateTime createTime = (LocalDateTime) row.get("create_time");
            if (createTime == null) {
                continue;
            }
            BigDecimal payAmount = (BigDecimal) row.get("pay_amount");
            String date = createTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            salesByDate.merge(date, payAmount != null ? payAmount : BigDecimal.ZERO, BigDecimal::add);
        }

        List<DashboardVO.SalesTrendVO> list = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : salesByDate.entrySet()) {
            DashboardVO.SalesTrendVO vo = new DashboardVO.SalesTrendVO();
            vo.setDate(entry.getKey());
            vo.setAmount(entry.getValue());
            list.add(vo);
        }
        return list;
    }

    private List<DashboardVO.MemberTrendVO> buildMemberTrend(LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> results = memberMapper.selectMaps(
                new LambdaQueryWrapper<Member>()
                        .between(Member::getCreateTime, start, end)
                        .select(Member::getCreateTime)
                        .apply("1=1"));

        Map<String, Long> countByDate = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            LocalDateTime createTime = (LocalDateTime) row.get("create_time");
            if (createTime == null) {
                continue;
            }
            String date = createTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            countByDate.merge(date, 1L, Long::sum);
        }

        List<DashboardVO.MemberTrendVO> list = new ArrayList<>();
        for (Map.Entry<String, Long> entry : countByDate.entrySet()) {
            DashboardVO.MemberTrendVO vo = new DashboardVO.MemberTrendVO();
            vo.setDate(entry.getKey());
            vo.setCount(entry.getValue());
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<SalesReportVO> getSalesReport(String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE).atTime(LocalTime.MAX);

        List<Map<String, Object>> results = orderMapper.selectMaps(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getStatus, 1)
                        .between(Order::getCreateTime, start, end)
                        .select(Order::getPayAmount, Order::getCreateTime)
                        .apply("1=1"));

        Map<String, List<Order>> groupedByDate = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            LocalDateTime createTime = (LocalDateTime) row.get("create_time");
            if (createTime == null) {
                continue;
            }
            Order order = new Order();
            order.setPayAmount((BigDecimal) row.get("pay_amount"));
            order.setCreateTime(createTime);
            String date = createTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            groupedByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(order);
        }

        List<SalesReportVO> vos = new ArrayList<>();
        for (Map.Entry<String, List<Order>> entry : groupedByDate.entrySet()) {
            SalesReportVO vo = new SalesReportVO();
            vo.setDate(entry.getKey());
            vo.setOrderCount((long) entry.getValue().size());
            BigDecimal sales = entry.getValue().stream()
                    .map(Order::getPayAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setSales(sales);
            vos.add(vo);
        }

        log.info("Sales report: startDate={}, endDate={}, resultSize={}", startDate, endDate, vos.size());
        return vos;
    }

    @Override
    public List<MemberReportVO> getMemberReport(String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE).atTime(LocalTime.MAX);

        List<Map<String, Object>> results = memberMapper.selectMaps(
                new LambdaQueryWrapper<Member>()
                        .between(Member::getCreateTime, start, end)
                        .select(Member::getCreateTime)
                        .apply("1=1"));

        Map<String, Long> newCountByDate = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            LocalDateTime createTime = (LocalDateTime) row.get("create_time");
            if (createTime == null) {
                continue;
            }
            String date = createTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            newCountByDate.merge(date, 1L, Long::sum);
        }

        Long totalCount = memberMapper.selectCount(null);

        List<MemberReportVO> vos = new ArrayList<>();
        long runningTotal = totalCount;
        for (Map.Entry<String, Long> entry : newCountByDate.entrySet()) {
            MemberReportVO vo = new MemberReportVO();
            vo.setDate(entry.getKey());
            vo.setNewCount(entry.getValue());
            vo.setTotalCount(runningTotal);
            vos.add(vo);
            runningTotal -= entry.getValue();
        }
        Collections.reverse(vos);

        log.info("Member report: startDate={}, endDate={}, resultSize={}", startDate, endDate, vos.size());
        return vos;
    }

    @Override
    public List<BonusReportVO> getBonusReport(String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE).atTime(LocalTime.MAX);

        List<Map<String, Object>> results = bonusRecordMapper.selectMaps(
                new LambdaQueryWrapper<BonusRecord>()
                        .between(BonusRecord::getCreateTime, start, end)
                        .select(BonusRecord::getBonusType, BonusRecord::getAmount)
                        .apply("1=1"));

        Map<String, BonusReportVO> groupMap = new LinkedHashMap<>();
        for (Map<String, Object> row : results) {
            String bonusType = (String) row.get("bonus_type");
            BigDecimal amount = (BigDecimal) row.get("amount");
            BonusReportVO vo = groupMap.computeIfAbsent(bonusType, k -> {
                BonusReportVO v = new BonusReportVO();
                v.setBonusType(k);
                v.setAmount(BigDecimal.ZERO);
                v.setCount(0L);
                return v;
            });
            vo.setAmount(vo.getAmount().add(amount != null ? amount : BigDecimal.ZERO));
            vo.setCount(vo.getCount() + 1);
        }

        List<BonusReportVO> vos = new ArrayList<>(groupMap.values());

        log.info("Bonus report: startDate={}, endDate={}, resultSize={}", startDate, endDate, vos.size());
        return vos;
    }

    @Override
    public TeamReportVO getTeamReport(Long memberId, String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE).atTime(LocalTime.MAX);

        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            log.warn("Team report: member not found, memberId={}", memberId);
            return new TeamReportVO();
        }

        List<MemberPath> paths = memberPathMapper.selectList(
                new LambdaQueryWrapper<MemberPath>()
                        .eq(MemberPath::getAncestorId, memberId));

        List<Long> teamMemberIds = paths.stream()
                .map(MemberPath::getDescendantId)
                .distinct()
                .collect(Collectors.toList());

        TeamReportVO vo = new TeamReportVO();
        vo.setMemberId(member.getId());
        vo.setMemberNo(member.getMemberNo());
        vo.setRealName(member.getRealName());
        vo.setTeamMemberCount((long) teamMemberIds.size());

        List<Long> allMemberIds = new ArrayList<>();
        allMemberIds.add(memberId);
        allMemberIds.addAll(teamMemberIds);

        List<Map<String, Object>> orderResults = orderMapper.selectMaps(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getStatus, 1)
                        .in(Order::getMemberId, allMemberIds)
                        .between(Order::getCreateTime, start, end)
                        .select(Order::getMemberId, Order::getPayAmount)
                        .apply("1=1"));

        BigDecimal selfSales = BigDecimal.ZERO;
        BigDecimal teamSales = BigDecimal.ZERO;

        for (Map<String, Object> row : orderResults) {
            Long orderMemberId = (Long) row.get("member_id");
            BigDecimal payAmount = (BigDecimal) row.get("pay_amount");
            if (payAmount == null) {
                payAmount = BigDecimal.ZERO;
            }
            if (orderMemberId != null && orderMemberId.equals(memberId)) {
                selfSales = selfSales.add(payAmount);
            }
            teamSales = teamSales.add(payAmount);
        }

        vo.setSelfSales(selfSales);
        vo.setTotalSales(teamSales);

        log.info("Team report: memberId={}, startDate={}, endDate={}, teamMemberCount={}, totalSales={}, selfSales={}",
                memberId, startDate, endDate, vo.getTeamMemberCount(), vo.getTotalSales(), vo.getSelfSales());

        return vo;
    }
}