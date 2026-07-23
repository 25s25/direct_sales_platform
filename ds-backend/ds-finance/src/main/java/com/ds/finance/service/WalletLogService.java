package com.ds.finance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.finance.entity.WalletLog;

import java.util.List;

public interface WalletLogService {

    Page<WalletLog> page(Page<WalletLog> page, Long memberId, String type);

    List<WalletLog> getByMemberId(Long memberId);

    boolean saveLog(WalletLog walletLog);
}