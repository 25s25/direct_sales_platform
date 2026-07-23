package com.ds.bonus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ds.bonus.entity.BonusRecord;
import com.ds.bonus.mapper.BonusRecordMapper;
import com.ds.bonus.service.BonusGrantService;
import com.ds.common.exception.BusinessException;
import com.ds.finance.entity.WalletLog;
import com.ds.finance.service.WalletLogService;
import com.ds.member.entity.Member;
import com.ds.member.mapper.MemberMapper;
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
public class BonusGrantServiceImpl implements BonusGrantService {

    private final BonusRecordMapper bonusRecordMapper;
    private final MemberMapper memberMapper;
    private final WalletLogService walletLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantPendingRecords() {
        List<BonusRecord> pendingList = bonusRecordMapper.selectList(
                new LambdaQueryWrapper<BonusRecord>()
                        .eq(BonusRecord::getStatus, 0)
                        .last("LIMIT 500"));

        if (pendingList.isEmpty()) {
            return;
        }

        log.info("开始发放奖金，待发放记录数：{}", pendingList.size());

        for (BonusRecord record : pendingList) {
            try {
                grantSingle(record);
            } catch (Exception e) {
                log.error("发放奖金失败，recordId={}，error={}", record.getId(), e.getMessage(), e);
            }
        }
    }

    private void grantSingle(BonusRecord record) {
        Member member = memberMapper.selectById(record.getMemberId());
        if (member == null) {
            log.warn("会员不存在，跳过发放，memberId={}", record.getMemberId());
            return;
        }

        BigDecimal amount = record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("奖金金额无效，跳过发放，recordId={}", record.getId());
            return;
        }

        BigDecimal balanceBefore = member.getWalletBalance() != null ? member.getWalletBalance() : BigDecimal.ZERO;
        BigDecimal balanceAfter = balanceBefore.add(amount);

        Member updateMember = new Member();
        updateMember.setId(member.getId());
        updateMember.setWalletBalance(balanceAfter);
        updateMember.setVersion(member.getVersion());
        int updated = memberMapper.updateById(updateMember);
        if (updated <= 0) {
            throw new BusinessException("会员钱包更新失败，可能存在并发冲突，memberId=" + member.getId());
        }

        WalletLog walletLog = new WalletLog();
        walletLog.setMemberId(member.getId());
        walletLog.setLogType("BONUS");
        walletLog.setAmount(amount);
        walletLog.setBalanceBefore(balanceBefore);
        walletLog.setBalanceAfter(balanceAfter);
        walletLog.setReferenceId(record.getId());
        walletLog.setRemark("奖金发放：" + record.getBonusType() + "，来源订单：" + record.getSourceOrderNo());
        walletLog.setCreateTime(LocalDateTime.now());
        walletLogService.saveLog(walletLog);

        BonusRecord updateRecord = new BonusRecord();
        updateRecord.setId(record.getId());
        updateRecord.setStatus(1);
        updateRecord.setGrantTime(LocalDateTime.now());
        bonusRecordMapper.updateById(updateRecord);

        log.info("奖金发放成功，recordId={}，memberId={}，amount={}", record.getId(), member.getId(), amount);
    }
}
