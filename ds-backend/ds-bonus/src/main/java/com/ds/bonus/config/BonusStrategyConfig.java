package com.ds.bonus.config;

import com.ds.bonus.strategy.BonusCalculator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class BonusStrategyConfig {

    @Autowired
    private List<BonusCalculator> calculators;

    private final Map<String, BonusCalculator> strategyMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (BonusCalculator calculator : calculators) {
            String planType = calculator.getPlanType();
            strategyMap.put(planType, calculator);
            log.info("Registered bonus calculator: planType={}, class={}",
                    planType, calculator.getClass().getSimpleName());
        }
        log.info("Bonus strategy config initialized with {} calculators", strategyMap.size());
    }

    public BonusCalculator getCalculator(String planType) {
        BonusCalculator calculator = strategyMap.get(planType);
        if (calculator == null) {
            throw new IllegalArgumentException("Unsupported bonus plan type: " + planType);
        }
        return calculator;
    }
}