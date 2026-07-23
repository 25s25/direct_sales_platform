package com.ds.order.controller;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.order.dto.CreateOrderDTO;
import com.ds.order.dto.OrderReturnApplyDTO;
import com.ds.order.dto.OrderReturnAuditDTO;
import com.ds.order.entity.Order;
import com.ds.order.mapper.OrderMapper;
import com.ds.order.service.OrderService;
import com.ds.order.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PostMapping("/create")
    public Result<OrderVO> create(@Valid @RequestBody CreateOrderDTO dto) {
        return orderService.create(dto);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @GetMapping("/{id}")
    public Result<OrderVO> getById(@PathVariable Long id) {
        long currentMemberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).hasPermission("system:manage") && !order.getMemberId().equals(currentMemberId)) {
            return Result.fail("无权访问该订单");
        }
        return orderService.getById(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long memberId,
                          @RequestParam(required = false) String startDate,
                          @RequestParam(required = false) String endDate) {
        long currentMemberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        if (!SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).hasPermission("system:manage")) {
            memberId = currentMemberId;
        }
        Page<OrderVO> pageParam = new Page<>(page, size);
        return orderService.page(pageParam, status, keyword, memberId, startDate, endDate);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PutMapping("/{id}/pay")
    public Result<?> pay(@PathVariable Long id) {
        return orderService.pay(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "order:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/admin/page")
    public Result<?> adminPage(@RequestParam(defaultValue = "1") long page,
                                @RequestParam(defaultValue = "10") long size,
                                @RequestParam(required = false) Integer status,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) Long memberId,
                                @RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate) {
        Page<OrderVO> pageParam = new Page<>(page, size);
        return orderService.page(pageParam, status, keyword, memberId, startDate, endDate);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "order:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/admin/{id}")
    public Result<OrderVO> adminGetById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "order:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/admin/{id}/cancel")
    public Result<?> adminCancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "order:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/admin/{id}/ship")
    public Result<?> ship(@PathVariable Long id,
                          @RequestParam String expressCompany,
                          @RequestParam String expressNo) {
        return orderService.ship(id, expressCompany, expressNo);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PutMapping("/{id}/receive")
    public Result<?> receive(@PathVariable Long id) {
        return orderService.receive(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_MEMBER)
    @PostMapping("/return/apply")
    public Result<Void> applyReturn(@Valid @RequestBody OrderReturnApplyDTO dto) {
        return orderService.applyReturn(dto);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "order:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @GetMapping("/return/page")
    public Result<?> pageReturn(@RequestParam(defaultValue = "1") long page,
                                @RequestParam(defaultValue = "10") long size,
                                @RequestParam(required = false) Integer status) {
        Page<com.ds.order.entity.OrderReturn> pageParam = new Page<>(page, size);
        return orderService.pageReturn(pageParam, status);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "order:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PostMapping("/return/audit")
    public Result<Void> auditReturn(@Valid @RequestBody OrderReturnAuditDTO dto) {
        return orderService.auditReturn(dto);
    }
}