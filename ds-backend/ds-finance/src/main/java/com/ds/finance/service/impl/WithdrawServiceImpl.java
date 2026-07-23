package com.ds.finance.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.ds.common.constant.SaTokenConsts;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.common.exception.BusinessException;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.finance.dto.WithdrawApplyDTO;
import com.ds.finance.entity.WalletLog;
import com.ds.finance.entity.Withdraw;
import com.ds.finance.mapper.WithdrawMapper;
import com.ds.finance.service.WalletLogService;
import com.ds.finance.service.WithdrawService;
import com.ds.member.entity.Member;
import com.ds.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawServiceImpl extends ServiceImpl<WithdrawMapper, Withdraw> implements WithdrawService {

    private final MemberMapper memberMapper;
    private final WalletLogService walletLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> apply(WithdrawApplyDTO dto) {
        long memberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            return Result.fail("会员不存在");
        }

        BigDecimal balance = member.getWalletBalance();
        if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("钱包余额不足");
        }

        BigDecimal amount = dto.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("提现金额必须大于0");
        }
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal actualAmount = amount.subtract(fee);

        if (balance.compareTo(amount) < 0) {
            return Result.fail("钱包余额不足，当前余额：" + balance);
        }

        // Deduct wallet balance
        BigDecimal balanceBefore = balance;
        BigDecimal balanceAfter = balance.subtract(amount);
        Member updateMember = new Member();
        updateMember.setId(member.getId());
        updateMember.setWalletBalance(balanceAfter);
        updateMember.setVersion(member.getVersion());
        boolean updated = memberMapper.updateById(updateMember) > 0;
        if (!updated) {
            throw new BusinessException("钱包余额更新失败或并发冲突");
        }

        // Create withdraw record
        Withdraw withdraw = new Withdraw();
        withdraw.setMemberId(memberId);
        withdraw.setWithdrawNo(generateWithdrawNo());
        withdraw.setAmount(amount);
        withdraw.setFee(fee);
        withdraw.setActualAmount(actualAmount);
        withdraw.setBankName(dto.getBankName());
        withdraw.setBankCard(dto.getBankCard());
        withdraw.setStatus(0); // PENDING
        this.save(withdraw);

        // Create wallet log
        WalletLog walletLog = new WalletLog();
        walletLog.setMemberId(memberId);
        walletLog.setLogType("WITHDRAW");
        walletLog.setAmount(amount.negate());
        walletLog.setBalanceBefore(balanceBefore);
        walletLog.setBalanceAfter(balanceAfter);
        walletLog.setReferenceId(withdraw.getId());
        walletLog.setRemark("申请提现");
        walletLog.setCreateTime(LocalDateTime.now());
        walletLogService.saveLog(walletLog);

        log.info("Withdraw applied: memberId={}, withdrawNo={}, amount={}", memberId, withdraw.getWithdrawNo(), amount);
        return Result.ok();
    }

    @Override
    public Result<PageResult<Withdraw>> page(Page<Withdraw> page, Long memberId, Integer status) {
        LambdaQueryWrapper<Withdraw> wrapper = new LambdaQueryWrapper<>();
        if (memberId != null) {
            wrapper.eq(Withdraw::getMemberId, memberId);
        }
        if (status != null) {
            wrapper.eq(Withdraw::getStatus, status);
        }
        wrapper.orderByDesc(Withdraw::getCreateTime);
        Page<Withdraw> result = this.page(page, wrapper);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> audit(Long id, Integer status, String remark) {
        Withdraw withdraw = this.getById(id);
        if (withdraw == null) {
            return Result.fail("提现记录不存在");
        }
        if (withdraw.getStatus() != 0) {
            return Result.fail("该提现申请已处理，无法重复审核");
        }

        // status=1: APPROVED, other values: REJECTED
        if (status != 1) {
            // Rejected: restore wallet balance
            Member member = memberMapper.selectById(withdraw.getMemberId());
            if (member != null) {
                BigDecimal balanceBefore = member.getWalletBalance();
                BigDecimal balanceAfter = balanceBefore.add(withdraw.getAmount());
                Member updateMember = new Member();
                updateMember.setId(member.getId());
                updateMember.setWalletBalance(balanceAfter);
                updateMember.setVersion(member.getVersion());
                boolean updated = memberMapper.updateById(updateMember) > 0;
                if (!updated) {
                    throw new BusinessException("钱包余额更新失败或并发冲突");
                }

                WalletLog walletLog = new WalletLog();
                walletLog.setMemberId(withdraw.getMemberId());
                walletLog.setLogType("WITHDRAW_REJECT");
                walletLog.setAmount(withdraw.getAmount());
                walletLog.setBalanceBefore(balanceBefore);
                walletLog.setBalanceAfter(balanceAfter);
                walletLog.setReferenceId(withdraw.getId());
                walletLog.setRemark("提现驳回：" + (remark != null ? remark : ""));
                walletLog.setCreateTime(LocalDateTime.now());
                walletLogService.saveLog(walletLog);
            }
        }

        withdraw.setStatus(status);
        withdraw.setAuditTime(LocalDateTime.now());
        withdraw.setRemark(remark);
        this.updateById(withdraw);

        log.info("Withdraw audited: id={}, status={}, remark={}", id, status, remark);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> grant(Long id) {
        Withdraw withdraw = this.getById(id);
        if (withdraw == null) {
            return Result.fail("提现记录不存在");
        }
        if (withdraw.getStatus() != 1) {
            return Result.fail("只有审核通过的提现申请才能打款");
        }

        withdraw.setStatus(2); // GRANTED
        withdraw.setGrantTime(LocalDateTime.now());
        this.updateById(withdraw);

        log.info("Withdraw granted: id={}", id);
        return Result.ok();
    }

    private String generateWithdrawNo() {
        return "W" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss") + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }
}