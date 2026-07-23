package com.ds.bonus.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.bonus.entity.BonusRecord;

import java.util.List;

public interface BonusRecordService {

    Page<BonusRecord> page(Page<BonusRecord> page, Long memberId, String period, String bonusType);

    List<BonusRecord> getByMemberId(Long memberId);

    void grant(List<Long> recordIds);
}