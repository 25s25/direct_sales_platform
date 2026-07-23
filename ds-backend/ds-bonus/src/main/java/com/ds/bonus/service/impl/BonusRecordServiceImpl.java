package com.ds.bonus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.bonus.entity.BonusRecord;
import com.ds.bonus.mapper.BonusRecordMapper;
import com.ds.bonus.service.BonusRecordService;
import com.ds.common.result.Result;
import com.ds.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BonusRecordServiceImpl extends ServiceImpl<BonusRecordMapper, BonusRecord> implements BonusRecordService {

    private final MemberService memberService;

    @Override
    public Page<BonusRecord> page(Page<BonusRecord> page, Long memberId, String period, String bonusType) {
        LambdaQueryWrapper<BonusRecord> wrapper = new LambdaQueryWrapper<>();
        if (memberId != null) {
            wrapper.eq(BonusRecord::getMemberId, memberId);
        }
        if (period != null && !period.isEmpty()) {
            wrapper.eq(BonusRecord::getPeriod, period);
        }
        if (bonusType != null && !bonusType.isEmpty()) {
            wrapper.eq(BonusRecord::getBonusType, bonusType);
        }
        wrapper.orderByDesc(BonusRecord::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public List<BonusRecord> getByMemberId(Long memberId) {
        return lambdaQuery()
                .eq(BonusRecord::getMemberId, memberId)
                .orderByDesc(BonusRecord::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(List<Long> recordIds) {
        for (Long recordId : recordIds) {
            BonusRecord record = this.getById(recordId);
            if (record == null) {
                log.warn("Bonus record not found: recordId={}", recordId);
                continue;
            }
            if (record.getStatus() != null && record.getStatus() == 1) {
                log.warn("Bonus record already granted: recordId={}", recordId);
                continue;
            }
            if (record.getAmount() != null && record.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                Result<Void> addResult = memberService.addWallet(record.getMemberId(), record.getAmount());
                if (!addResult.isSuccess()) {
                    log.error("Grant bonus failed: recordId={}, msg={}", recordId, addResult.getMessage());
                    throw new RuntimeException("奖金入账失败: " + addResult.getMessage());
                }
            }
            BonusRecord update = new BonusRecord();
            update.setId(recordId);
            update.setStatus(1);
            update.setGrantTime(LocalDateTime.now());
            this.updateById(update);
        }
        log.info("Granted {} bonus records: recordIds={}", recordIds.size(), recordIds);
    }
}