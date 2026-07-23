package com.ds.bonus.job;

import com.ds.bonus.service.BonusGrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BonusGrantJob {

    private final BonusGrantService bonusGrantService;

    @Scheduled(cron = "0 */5 * * * *")
    public void grantBonus() {
        log.info("定时任务：发放待发放奖金");
        bonusGrantService.grantPendingRecords();
    }
}
