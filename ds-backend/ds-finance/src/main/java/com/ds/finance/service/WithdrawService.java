package com.ds.finance.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.finance.dto.WithdrawApplyDTO;
import com.ds.finance.entity.Withdraw;

public interface WithdrawService {

    Result<Void> apply(WithdrawApplyDTO dto);

    Result<PageResult<Withdraw>> page(Page<Withdraw> page, Long memberId, Integer status);

    Result<Void> audit(Long id, Integer status, String remark);

    Result<Void> grant(Long id);
}