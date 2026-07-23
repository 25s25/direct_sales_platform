package com.ds.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.order.dto.CreateOrderDTO;
import com.ds.order.dto.OrderReturnApplyDTO;
import com.ds.order.dto.OrderReturnAuditDTO;
import com.ds.order.entity.OrderReturn;
import com.ds.order.vo.OrderVO;

public interface OrderService {

    Result<OrderVO> create(CreateOrderDTO dto);

    Result<OrderVO> getById(Long id);

    Result<PageResult<OrderVO>> page(Page<OrderVO> page, Integer status, String keyword, Long memberId, String startDate, String endDate);

    Result<Void> pay(Long id);

    Result<Void> ship(Long id, String expressCompany, String expressNo);

    Result<Void> receive(Long id);

    Result<Void> cancel(Long id);

    Result<Void> applyReturn(OrderReturnApplyDTO dto);

    Result<PageResult<OrderReturn>> pageReturn(Page<OrderReturn> page, Integer status);

    Result<Void> auditReturn(OrderReturnAuditDTO dto);

    void paySuccess(String orderNo);
}