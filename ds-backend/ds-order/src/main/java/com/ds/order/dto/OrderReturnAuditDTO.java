package com.ds.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderReturnAuditDTO {

    @NotNull(message = "退货单ID不能为空")
    private Long returnId;

    @NotNull(message = "审核状态不能为空")
    private Integer status;

    private String remark;
}
