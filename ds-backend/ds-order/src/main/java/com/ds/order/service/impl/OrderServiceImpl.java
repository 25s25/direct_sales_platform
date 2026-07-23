package com.ds.order.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.common.config.DsBonusProperties;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.exception.BusinessException;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.member.entity.Member;
import com.ds.member.entity.MemberPath;
import com.ds.member.mapper.MemberMapper;
import com.ds.member.mapper.MemberPathMapper;
import com.ds.member.service.MemberService;
import com.ds.order.dto.CreateOrderDTO;
import com.ds.order.dto.OrderReturnApplyDTO;
import com.ds.order.dto.OrderReturnAuditDTO;
import com.ds.order.entity.Order;
import com.ds.order.entity.OrderBonus;
import com.ds.order.entity.OrderItem;
import com.ds.order.entity.OrderReturn;
import com.ds.order.mapper.OrderBonusMapper;
import com.ds.order.mapper.OrderItemMapper;
import com.ds.order.mapper.OrderMapper;
import com.ds.order.mapper.OrderReturnMapper;
import com.ds.order.service.OrderService;
import com.ds.order.vo.OrderVO;
import com.ds.product.entity.Product;
import com.ds.product.entity.ProductSku;
import com.ds.product.mapper.ProductMapper;
import com.ds.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PAID = 1;
    private static final int STATUS_SHIPPED = 2;
    private static final int STATUS_RECEIVED = 3;
    private static final int STATUS_COMPLETED = 4;
    private static final int STATUS_CANCELLED = 5;

    private static final int BONUS_MAX_LEVEL = 3;

    private static final int RETURN_STATUS_PENDING = 0;
    private static final int RETURN_STATUS_APPROVED = 1;
    private static final int RETURN_STATUS_REJECTED = 2;

    private final OrderItemMapper orderItemMapper;
    private final OrderBonusMapper orderBonusMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final MemberMapper memberMapper;
    private final MemberPathMapper memberPathMapper;
    private final MemberService memberService;
    private final DsBonusProperties bonusProperties;
    private final RabbitTemplate rabbitTemplate;
    private final OrderReturnMapper orderReturnMapper;

    private static final String BONUS_EXCHANGE = "ds.bonus.exchange";
    private static final String BONUS_ORDER_PAID_ROUTING_KEY = "ds.bonus.order.paid";
    private static final String BONUS_ORDER_RETURNED_ROUTING_KEY = "ds.bonus.order.returned";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<OrderVO> create(CreateOrderDTO dto) {
        long memberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return Result.fail("订单商品不能为空");
        }

        List<OrderItem> orderItems = validateAndBuildItems(dto);
        BigDecimal totalAmount = calculateTotalAmount(orderItems);
        BigDecimal totalPv = calculateTotalPv(orderItems);

        Order order = new Order();
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setMemberId(memberId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalPv(totalPv);
        order.setReceiverName(dto.getReceiverName());
        order.setReceiverPhone(dto.getReceiverPhone());
        order.setReceiverAddr(dto.getReceiverAddr());
        order.setRemark(dto.getRemark());
        order.setStatus(STATUS_PENDING);

        boolean saved = this.save(order);
        if (!saved) {
            return Result.fail("创建订单失败");
        }

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItemMapper.insert(orderItem);
        }

        log.info("订单创建成功: orderNo={}, memberId={}, totalAmount={}", order.getOrderNo(), memberId, totalAmount);
        return Result.ok(toOrderVO(order, orderItems));
    }

    private List<OrderItem> validateAndBuildItems(CreateOrderDTO dto) {
        List<OrderItem> orderItems = new ArrayList<>();
        List<CreateOrderDTO.OrderItemDTO> itemDTOs = dto.getItems();

        for (CreateOrderDTO.OrderItemDTO itemDTO : itemDTOs) {
            if (itemDTO.getProductId() == null) {
                throw new BusinessException("商品ID不能为空");
            }
            if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
                throw new BusinessException("商品数量必须大于0");
            }

            Product product = productMapper.selectById(itemDTO.getProductId());
            if (product == null) {
                throw new BusinessException("商品不存在: productId=" + itemDTO.getProductId());
            }
            if (product.getStock() == null || product.getStock() < itemDTO.getQuantity()) {
                throw new BusinessException("商品库存不足: " + product.getName());
            }

            BigDecimal price = product.getMemberPrice() != null ? product.getMemberPrice() : product.getRetailPrice();
            String specName = null;
            BigDecimal pv = product.getPv() != null ? product.getPv() : BigDecimal.ZERO;

            if (StrUtil.isNotBlank(itemDTO.getSkuCode())) {
                ProductSku sku = productSkuMapper.selectOne(
                        new LambdaQueryWrapper<ProductSku>()
                                .eq(ProductSku::getProductId, itemDTO.getProductId())
                                .eq(ProductSku::getSkuCode, itemDTO.getSkuCode()));
                if (sku == null) {
                    throw new BusinessException("SKU不存在: " + itemDTO.getSkuCode());
                }
                if (sku.getStock() == null || sku.getStock() < itemDTO.getQuantity()) {
                    throw new BusinessException("SKU库存不足: " + sku.getSkuCode());
                }
                if (sku.getPrice() != null) {
                    price = sku.getPrice();
                }
                specName = sku.getSpecName();
            }

            BigDecimal itemPv = pv.multiply(new BigDecimal(itemDTO.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setSkuCode(itemDTO.getSkuCode());
            orderItem.setSpecName(specName);
            orderItem.setPrice(price);
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPv(itemPv);
            orderItem.setCreateTime(LocalDateTime.now());
            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private BigDecimal calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalPv(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getPv)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void deductStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            Product updateProduct = new Product();
            updateProduct.setId(product.getId());
            updateProduct.setStock(product.getStock() - item.getQuantity());
            updateProduct.setSalesCount((product.getSalesCount() != null ? product.getSalesCount() : 0) + item.getQuantity());
            updateProduct.setVersion(product.getVersion());
            boolean productUpdated = productMapper.updateById(updateProduct) > 0;
            if (!productUpdated) {
                throw new BusinessException("库存不足或并发冲突: " + product.getName());
            }

            if (StrUtil.isNotBlank(item.getSkuCode())) {
                ProductSku sku = productSkuMapper.selectOne(
                        new LambdaQueryWrapper<ProductSku>()
                                .eq(ProductSku::getProductId, item.getProductId())
                                .eq(ProductSku::getSkuCode, item.getSkuCode()));
                ProductSku updateSku = new ProductSku();
                updateSku.setId(sku.getId());
                updateSku.setStock(sku.getStock() - item.getQuantity());
                updateSku.setVersion(sku.getVersion());
                boolean skuUpdated = productSkuMapper.updateById(updateSku) > 0;
                if (!skuUpdated) {
                    throw new BusinessException("SKU库存不足或并发冲突: " + sku.getSkuCode());
                }
            }
        }
    }

    private void restoreStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                Product updateProduct = new Product();
                updateProduct.setId(product.getId());
                updateProduct.setStock(product.getStock() + item.getQuantity());
                updateProduct.setSalesCount((product.getSalesCount() != null ? product.getSalesCount() : 0) - item.getQuantity());
                productMapper.updateById(updateProduct);
            }
            if (StrUtil.isNotBlank(item.getSkuCode())) {
                ProductSku sku = productSkuMapper.selectOne(
                        new LambdaQueryWrapper<ProductSku>()
                                .eq(ProductSku::getProductId, item.getProductId())
                                .eq(ProductSku::getSkuCode, item.getSkuCode()));
                if (sku != null) {
                    ProductSku updateSku = new ProductSku();
                    updateSku.setId(sku.getId());
                    updateSku.setStock(sku.getStock() + item.getQuantity());
                    productSkuMapper.updateById(updateSku);
                }
            }
        }
    }

    private void createBonusRecords(Long orderId, Long memberId, BigDecimal totalPv) {
        List<MemberPath> ancestorPaths = memberPathMapper.selectList(
                new LambdaQueryWrapper<MemberPath>()
                        .eq(MemberPath::getDescendantId, memberId)
                        .gt(MemberPath::getDepth, 0)
                        .le(MemberPath::getDepth, BONUS_MAX_LEVEL)
                        .orderByAsc(MemberPath::getDepth));

        if (ancestorPaths != null && !ancestorPaths.isEmpty()) {
            for (MemberPath path : ancestorPaths) {
                Member ancestor = memberMapper.selectById(path.getAncestorId());
                if (ancestor == null) {
                    continue;
                }
                OrderBonus bonus = new OrderBonus();
                bonus.setOrderId(orderId);
                bonus.setMemberId(ancestor.getId());
                bonus.setRelationType("recommend");
                bonus.setBonusLevel(path.getDepth());
                BigDecimal bonusPv = totalPv.multiply(bonusProperties.getRecommendRate())
                        .setScale(2, RoundingMode.HALF_UP);
                bonus.setPv(bonusPv);
                bonus.setCreateTime(LocalDateTime.now());
                orderBonusMapper.insert(bonus);
            }
        }
    }

    private void updateMemberPv(Long memberId, BigDecimal totalPv) {
        Member member = memberMapper.selectById(memberId);
        if (member != null) {
            Member updateMember = new Member();
            updateMember.setId(memberId);
            updateMember.setTotalPv((member.getTotalPv() != null ? member.getTotalPv() : BigDecimal.ZERO).add(totalPv));
            memberMapper.updateById(updateMember);
        }
    }

    @Override
    public Result<OrderVO> getById(Long id) {
        if (id == null) {
            return Result.fail("订单ID不能为空");
        }
        Order order = super.getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        return Result.ok(toOrderVO(order, items));
    }

    @Override
    public Result<PageResult<OrderVO>> page(Page<OrderVO> pageParam, Integer status, String keyword, Long memberId, String startDate, String endDate) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(Order::getOrderNo, keyword)
                    .or()
                    .like(Order::getReceiverName, keyword)
                    .or()
                    .like(Order::getReceiverPhone, keyword));
        }
        if (memberId != null) {
            wrapper.eq(Order::getMemberId, memberId);
        }
        if (StrUtil.isNotBlank(startDate)) {
            wrapper.ge(Order::getCreateTime, startDate);
        }
        if (StrUtil.isNotBlank(endDate)) {
            wrapper.le(Order::getCreateTime, endDate);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        Page<Order> result = this.page(page, wrapper);

        List<OrderVO> voList = result.getRecords().stream()
                .map(order -> {
                    List<OrderItem> items = orderItemMapper.selectList(
                            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
                    return toOrderVO(order, items);
                })
                .collect(Collectors.toList());

        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), voList));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> pay(Long id) {
        if (id == null) {
            return Result.fail("订单ID不能为空");
        }

        Order order = super.getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (order.getStatus() != STATUS_PENDING) {
            return Result.fail("当前订单状态无法支付");
        }

        Result<Void> deductResult = memberService.deductWallet(order.getMemberId(), order.getPayAmount());
        if (!deductResult.isSuccess()) {
            return Result.fail(deductResult.getMessage());
        }

        Order updateOrder = new Order();
        updateOrder.setId(id);
        updateOrder.setStatus(STATUS_PAID);
        updateOrder.setPayTime(LocalDateTime.now());
        boolean updated = this.updateById(updateOrder);
        if (!updated) {
            return Result.fail("支付失败");
        }

        log.info("订单支付成功: orderNo={}, id={}", order.getOrderNo(), id);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> ship(Long id, String expressCompany, String expressNo) {
        if (id == null) {
            return Result.fail("订单ID不能为空");
        }

        Order order = super.getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (order.getStatus() != STATUS_PAID) {
            return Result.fail("当前订单状态无法发货");
        }

        Order updateOrder = new Order();
        updateOrder.setId(id);
        updateOrder.setStatus(STATUS_SHIPPED);
        updateOrder.setShipTime(LocalDateTime.now());
        updateOrder.setExpressCompany(expressCompany);
        updateOrder.setExpressNo(expressNo);
        boolean updated = this.updateById(updateOrder);
        if (!updated) {
            return Result.fail("发货失败");
        }

        log.info("订单发货成功: orderNo={}, id={}, expressCompany={}, expressNo={}", order.getOrderNo(), id, expressCompany, expressNo);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> receive(Long id) {
        if (id == null) {
            return Result.fail("订单ID不能为空");
        }

        Order order = super.getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (order.getStatus() != STATUS_SHIPPED) {
            return Result.fail("当前订单状态无法确认收货");
        }

        Order updateOrder = new Order();
        updateOrder.setId(id);
        updateOrder.setStatus(STATUS_RECEIVED);
        updateOrder.setReceiveTime(LocalDateTime.now());
        boolean updated = this.updateById(updateOrder);
        if (!updated) {
            return Result.fail("确认收货失败");
        }

        log.info("订单确认收货成功: orderNo={}, id={}", order.getOrderNo(), id);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancel(Long id) {
        if (id == null) {
            return Result.fail("订单ID不能为空");
        }

        Order order = super.getById(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (order.getStatus() != STATUS_PENDING && order.getStatus() != STATUS_PAID) {
            return Result.fail("当前订单状态无法取消");
        }

        if (order.getStatus() == STATUS_PAID) {
            Result<Void> refundResult = memberService.addWallet(order.getMemberId(), order.getPayAmount());
            if (!refundResult.isSuccess()) {
                return Result.fail(refundResult.getMessage());
            }
        }

        // restore stock
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                Product updateProduct = new Product();
                updateProduct.setId(product.getId());
                updateProduct.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(updateProduct);
            }
            if (StrUtil.isNotBlank(item.getSkuCode())) {
                ProductSku sku = productSkuMapper.selectOne(
                        new LambdaQueryWrapper<ProductSku>()
                                .eq(ProductSku::getProductId, item.getProductId())
                                .eq(ProductSku::getSkuCode, item.getSkuCode()));
                if (sku != null) {
                    ProductSku updateSku = new ProductSku();
                    updateSku.setId(sku.getId());
                    updateSku.setStock(sku.getStock() + item.getQuantity());
                    productSkuMapper.updateById(updateSku);
                }
            }
        }

        Order updateOrder = new Order();
        updateOrder.setId(id);
        updateOrder.setStatus(STATUS_CANCELLED);
        boolean updated = this.updateById(updateOrder);
        if (!updated) {
            return Result.fail("取消订单失败");
        }

        log.info("订单取消成功: orderNo={}, id={}", order.getOrderNo(), id);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> applyReturn(OrderReturnApplyDTO dto) {
        long memberId = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).getLoginIdAsLong();
        Order order = super.getById(dto.getOrderId());
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!order.getMemberId().equals(memberId)) {
            return Result.fail("无权操作该订单");
        }
        if (order.getStatus() != STATUS_RECEIVED && order.getStatus() != STATUS_COMPLETED) {
            return Result.fail("只有已签收或已完成的订单才能申请退货");
        }

        BigDecimal refundAmount = dto.getRefundAmount() != null ? dto.getRefundAmount() : order.getPayAmount();
        if (refundAmount.compareTo(order.getPayAmount()) > 0) {
            return Result.fail("退款金额不能超过订单实付金额");
        }

        OrderReturn returnRecord = new OrderReturn();
        returnRecord.setReturnNo(IdUtil.getSnowflakeNextIdStr());
        returnRecord.setOrderId(order.getId());
        returnRecord.setMemberId(memberId);
        returnRecord.setReason(dto.getReason());
        returnRecord.setRefundAmount(refundAmount);
        returnRecord.setStatus(RETURN_STATUS_PENDING);
        orderReturnMapper.insert(returnRecord);

        log.info("退货申请提交：returnNo={}，orderId={}", returnRecord.getReturnNo(), order.getId());
        return Result.ok();
    }

    @Override
    public Result<PageResult<OrderReturn>> pageReturn(Page<OrderReturn> page, Integer status) {
        LambdaQueryWrapper<OrderReturn> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(OrderReturn::getStatus, status);
        }
        wrapper.orderByDesc(OrderReturn::getCreateTime);
        Page<OrderReturn> result = orderReturnMapper.selectPage(page, wrapper);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> auditReturn(OrderReturnAuditDTO dto) {
        OrderReturn returnRecord = orderReturnMapper.selectById(dto.getReturnId());
        if (returnRecord == null) {
            return Result.fail("退货单不存在");
        }
        if (returnRecord.getStatus() != RETURN_STATUS_PENDING) {
            return Result.fail("该退货单已处理");
        }

        if (dto.getStatus() == RETURN_STATUS_REJECTED) {
            returnRecord.setStatus(RETURN_STATUS_REJECTED);
            returnRecord.setAuditRemark(dto.getRemark());
            returnRecord.setAuditTime(LocalDateTime.now());
            orderReturnMapper.updateById(returnRecord);
            return Result.ok();
        }

        Order order = super.getById(returnRecord.getOrderId());
        if (order == null) {
            return Result.fail("订单不存在");
        }

        Result<Void> refundResult = memberService.addWallet(order.getMemberId(), returnRecord.getRefundAmount());
        if (!refundResult.isSuccess()) {
            return Result.fail(refundResult.getMessage());
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        restoreStock(items);

        Member member = memberMapper.selectById(order.getMemberId());
        if (member != null) {
            Member updateMember = new Member();
            updateMember.setId(member.getId());
            BigDecimal currentPv = member.getTotalPv() != null ? member.getTotalPv() : BigDecimal.ZERO;
            BigDecimal orderPv = order.getTotalPv() != null ? order.getTotalPv() : BigDecimal.ZERO;
            updateMember.setTotalPv(currentPv.subtract(orderPv));
            memberMapper.updateById(updateMember);
        }

        try {
            rabbitTemplate.convertAndSend(BONUS_EXCHANGE, BONUS_ORDER_RETURNED_ROUTING_KEY, order.getId());
            log.info("已发送退货奖金回退消息: orderNo={}, orderId={}", order.getOrderNo(), order.getId());
        } catch (Exception e) {
            log.error("发送退货奖金回退消息失败: orderNo={}", order.getOrderNo(), e);
        }

        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(STATUS_CANCELLED);
        this.updateById(updateOrder);

        returnRecord.setStatus(RETURN_STATUS_APPROVED);
        returnRecord.setAuditRemark(dto.getRemark());
        returnRecord.setAuditTime(LocalDateTime.now());
        orderReturnMapper.updateById(returnRecord);

        log.info("退货审核通过：returnNo={}，orderId={}", returnRecord.getReturnNo(), order.getId());
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo) {
        Order order = this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在: " + orderNo);
        }
        if (order.getStatus() != STATUS_PENDING) {
            log.warn("订单状态不是待支付，跳过处理: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }

        List<OrderItem> orderItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

        deductStock(orderItems);
        createBonusRecords(order.getId(), order.getMemberId(), order.getTotalPv());
        updateMemberPv(order.getMemberId(), order.getTotalPv());

        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(STATUS_PAID);
        updateOrder.setPayTime(LocalDateTime.now());
        boolean updated = this.updateById(updateOrder);
        if (!updated) {
            throw new BusinessException("更新订单支付状态失败: " + orderNo);
        }

        try {
            rabbitTemplate.convertAndSend(BONUS_EXCHANGE, BONUS_ORDER_PAID_ROUTING_KEY, order.getId());
            log.info("已发送奖金计算消息: orderNo={}, orderId={}", orderNo, order.getId());
        } catch (Exception e) {
            log.error("发送奖金计算消息失败: orderNo={}", orderNo, e);
        }
        log.info("订单支付成功: orderNo={}", orderNo);
    }

    private OrderVO toOrderVO(Order order, List<OrderItem> items) {
        if (order == null) {
            return null;
        }
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setMemberId(order.getMemberId());
        Member member = memberMapper.selectById(order.getMemberId());
        if (member != null) {
            vo.setMemberName(member.getRealName());
            vo.setMemberPhone(member.getPhone());
        }
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setTotalPv(order.getTotalPv());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddr(order.getReceiverAddr());
        vo.setPayType(order.getPayType());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setReceiveTime(order.getReceiveTime());
        vo.setExpressCompany(order.getExpressCompany());
        vo.setExpressNo(order.getExpressNo());
        vo.setStatus(order.getStatus());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());

        if (items != null) {
            List<OrderVO.OrderItemVO> itemVOs = items.stream().map(item -> {
                OrderVO.OrderItemVO itemVO = new OrderVO.OrderItemVO();
                itemVO.setId(item.getId());
                itemVO.setProductId(item.getProductId());
                itemVO.setProductName(item.getProductName());
                itemVO.setProductImage(item.getProductImage());
                itemVO.setSkuCode(item.getSkuCode());
                itemVO.setSpecName(item.getSpecName());
                itemVO.setPrice(item.getPrice());
                itemVO.setQuantity(item.getQuantity());
                itemVO.setPv(item.getPv());
                return itemVO;
            }).collect(Collectors.toList());
            vo.setItems(itemVOs);
        }

        return vo;
    }
}