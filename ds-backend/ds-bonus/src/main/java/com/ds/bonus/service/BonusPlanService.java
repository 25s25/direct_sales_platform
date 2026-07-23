package com.ds.bonus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ds.bonus.entity.BonusPlan;

import java.util.List;

public interface BonusPlanService extends IService<BonusPlan> {

    List<BonusPlan> listAll();

    BonusPlan getActive();

    void switchPlan(Long planId);

    BonusPlan getById(Long id);

    boolean add(BonusPlan plan);

    boolean update(BonusPlan plan);
}