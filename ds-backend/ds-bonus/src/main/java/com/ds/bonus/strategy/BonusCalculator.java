package com.ds.bonus.strategy;

import com.ds.bonus.dto.BonusCalcDTO;
import com.ds.bonus.entity.BonusRecord;

import java.util.List;

public interface BonusCalculator {

    String getPlanType();

    List<BonusRecord> calculate(BonusCalcDTO dto);
}