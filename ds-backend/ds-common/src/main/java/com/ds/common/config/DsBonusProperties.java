package com.ds.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "ds.bonus")
public class DsBonusProperties {

    private BigDecimal recommendRate = new BigDecimal("0.1");
}
