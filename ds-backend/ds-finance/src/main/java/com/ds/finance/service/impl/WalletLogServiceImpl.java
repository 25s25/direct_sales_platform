package com.ds.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.finance.entity.WalletLog;
import com.ds.finance.mapper.WalletLogMapper;
import com.ds.finance.service.WalletLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class WalletLogServiceImpl extends ServiceImpl<WalletLogMapper, WalletLog> implements WalletLogService {

    @Override
    public Page<WalletLog> page(Page<WalletLog> page, Long memberId, String type) {
        LambdaQueryWrapper<WalletLog> wrapper = new LambdaQueryWrapper<>();
        if (memberId != null) {
            wrapper.eq(WalletLog::getMemberId, memberId);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(WalletLog::getLogType, type);
        }
        wrapper.orderByDesc(WalletLog::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public List<WalletLog> getByMemberId(Long memberId) {
        return lambdaQuery()
                .eq(WalletLog::getMemberId, memberId)
                .orderByDesc(WalletLog::getCreateTime)
                .list();
    }

    @Override
    public boolean saveLog(WalletLog walletLog) {
        return super.save(walletLog);
    }
}