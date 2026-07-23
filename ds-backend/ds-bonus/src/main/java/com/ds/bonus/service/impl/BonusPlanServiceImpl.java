package com.ds.bonus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.bonus.entity.BonusPlan;
import com.ds.bonus.mapper.BonusPlanMapper;
import com.ds.bonus.service.BonusPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BonusPlanServiceImpl extends ServiceImpl<BonusPlanMapper, BonusPlan> implements BonusPlanService {

    @Override
    public List<BonusPlan> listAll() {
        return lambdaQuery().orderByDesc(BonusPlan::getCreateTime).list();
    }

    @Override
    public BonusPlan getActive() {
        return lambdaQuery().eq(BonusPlan::getIsActive, 1).one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void switchPlan(Long planId) {
        // Set all plans to inactive
        List<BonusPlan> allPlans = lambdaQuery().list();
        for (BonusPlan plan : allPlans) {
            if (plan.getIsActive() != null && plan.getIsActive() == 1) {
                BonusPlan update = new BonusPlan();
                update.setId(plan.getId());
                update.setIsActive(0);
                this.updateById(update);
            }
        }

        // Set the selected plan to active
        BonusPlan selectedPlan = new BonusPlan();
        selectedPlan.setId(planId);
        selectedPlan.setIsActive(1);
        boolean updated = this.updateById(selectedPlan);
        if (!updated) {
            throw new RuntimeException("切换奖金方案失败: planId=" + planId);
        }

        log.info("Bonus plan switched: planId={}", planId);
    }

    @Override
    public BonusPlan getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(BonusPlan plan) {
        return this.save(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(BonusPlan plan) {
        return this.updateById(plan);
    }
}