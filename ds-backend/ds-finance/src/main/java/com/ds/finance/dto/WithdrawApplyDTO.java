package com.ds.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class WithdrawApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "提现金额不能为空")
    private BigDecimal amount;

    @NotBlank(message = "银行名称不能为空")
    private String bankName;

    @NotBlank(message = "银行卡号不能为空")
    private String bankCard;
}