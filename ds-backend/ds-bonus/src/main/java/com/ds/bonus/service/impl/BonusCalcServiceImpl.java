package com.ds.bonus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ds.bonus.config.BonusStrategyConfig;
import com.ds.bonus.dto.BonusCalcDTO;
import com.ds.bonus.entity.BonusPlan;
import com.ds.bonus.entity.BonusRecord;
import com.ds.bonus.mapper.BonusPlanMapper;
import com.ds.bonus.mapper.BonusRecordMapper;
import com.ds.bonus.service.BonusCalcService;
import com.ds.bonus.strategy.BonusCalculator;
import com.ds.member.entity.Member;
import com.ds.member.entity.MemberLevel;
import com.ds.member.entity.MemberPath;
import com.ds.member.mapper.MemberLevelMapper;
import com.ds.member.mapper.MemberMapper;
import com.ds.member.mapper.MemberPathMapper;
import com.ds.order.entity.Order;
import com.ds.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BonusCalcServiceImpl implements BonusCalcService {

    private static final String BONUS_EXCHANGE = "ds.bonus.exchange";
    private static final String BONUS_ROUTING_KEY = "ds.bonus.calculate";
    private static final int MAX_RECOMMEND_LEVELS = 3;

    private final BonusPlanMapper bonusPlanMapper;
    private final BonusRecordMapper bonusRecordMapper;
    private final BonusStrategyConfig bonusStrategyConfig;
    private final OrderMapper orderMapper;
    private final MemberMapper memberMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final MemberPathMapper memberPathMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateForOrder(Long orderId) {
        log.info("Starting bonus calculation for orderId={}", orderId);

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("Order not found: orderId={}", orderId);
            return;
        }

        // 1. Get the active bonus plan
        BonusPlan activePlan = bonusPlanMapper.selectOne(
                new LambdaQueryWrapper<BonusPlan>().eq(BonusPlan::getIsActive, 1));
        if (activePlan == null) {
            log.warn("No active bonus plan found, skipping calculation for orderId={}", orderId);
            return;
        }

        // 2. Get the calculator for the active plan's planType
        BonusCalculator calculator = bonusStrategyConfig.getCalculator(activePlan.getPlanType());
        log.info("Using calculator: {} for planType={}", calculator.getClass().getSimpleName(), activePlan.getPlanType());

        // 3. Build BonusCalcDTO from the order
        BonusCalcDTO dto = buildCalcDTO(order);
        if (dto == null) {
            log.warn("Failed to build BonusCalcDTO for orderId={}", orderId);
            return;
        }

        // 4. Call calculator.calculate(dto)
        List<BonusRecord> bonusRecords = calculator.calculate(dto);
        log.info("Calculator returned {} bonus records for orderId={}", bonusRecords.size(), orderId);

        // 5. Save all BonusRecord results
        if (bonusRecords != null && !bonusRecords.isEmpty()) {
            for (BonusRecord record : bonusRecords) {
                record.setSourceOrderNo(order.getOrderNo());
                bonusRecordMapper.insert(record);
            }
            log.info("Saved {} bonus records for orderId={}", bonusRecords.size(), orderId);
        }

        // 6. Send RabbitMQ message for async processing
        try {
            rabbitTemplate.convertAndSend(BONUS_EXCHANGE, BONUS_ROUTING_KEY, orderId);
            log.info("Sent bonus calculation message to RabbitMQ for orderId={}", orderId);
        } catch (Exception e) {
            log.error("Failed to send RabbitMQ message for orderId={}: {}", orderId, e.getMessage(), e);
        }
    }

    private BonusCalcDTO buildCalcDTO(Order order) {
        Member member = memberMapper.selectById(order.getMemberId());
        if (member == null) {
            log.warn("Member not found: memberId={}", order.getMemberId());
            return null;
        }

        String memberLevel = getLevelName(member.getLevelId());

        // Build recommend chain from MemberPath
        List<BonusCalcDTO.RecommendMember> recommendChain = buildRecommendChain(member.getId());

        return BonusCalcDTO.builder()
                .orderId(order.getId())
                .memberId(order.getMemberId())
                .orderAmount(order.getTotalAmount())
                .orderPv(order.getTotalPv())
                .memberLevel(memberLevel)
                .recommendChain(recommendChain)
                .build();
    }

    private List<BonusCalcDTO.RecommendMember> buildRecommendChain(Long memberId) {
        List<BonusCalcDTO.RecommendMember> chain = new ArrayList<>();

        List<MemberPath> ancestorPaths = memberPathMapper.selectList(
                new LambdaQueryWrapper<MemberPath>()
                        .eq(MemberPath::getDescendantId, memberId)
                        .gt(MemberPath::getDepth, 0)
                        .le(MemberPath::getDepth, MAX_RECOMMEND_LEVELS)
                        .orderByAsc(MemberPath::getDepth));

        if (ancestorPaths == null || ancestorPaths.isEmpty()) {
            return chain;
        }

        for (MemberPath path : ancestorPaths) {
            Member ancestor = memberMapper.selectById(path.getAncestorId());
            if (ancestor == null) {
                continue;
            }

            String levelName = getLevelName(ancestor.getLevelId());
            BigDecimal bonusRate = getBonusRate(ancestor.getLevelId());

            BonusCalcDTO.RecommendMember rm = BonusCalcDTO.RecommendMember.builder()
                    .memberId(ancestor.getId())
                    .level(levelName)
                    .pv(ancestor.getTotalPv())
                    .depth(path.getDepth())
                    .bonusRate(bonusRate)
                    .build();

            chain.add(rm);
        }

        return chain;
    }

    private String getLevelName(Long levelId) {
        if (levelId == null) {
            return null;
        }
        MemberLevel level = memberLevelMapper.selectById(levelId);
        return level != null ? level.getName() : null;
    }

    private BigDecimal getBonusRate(Long levelId) {
        if (levelId == null) {
            return BigDecimal.ZERO;
        }
        MemberLevel level = memberLevelMapper.selectById(levelId);
        return level != null && level.getBonusRate() != null ? level.getBonusRate() : BigDecimal.ZERO;
    }
}