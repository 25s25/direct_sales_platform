package com.ds.product.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.product.dto.ProductDTO;
import com.ds.product.service.ProductService;
import com.ds.product.vo.ProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) Long categoryId,
                          @RequestParam(required = false) String keyword) {
        Page<ProductVO> pageParam = new Page<>(page, size);
        return productService.page(pageParam, categoryId, keyword);
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "product:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PostMapping
    public Result<?> add(@Valid @RequestBody ProductDTO dto) {
        return productService.add(dto);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "product:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping
    public Result<?> update(@Valid @RequestBody ProductDTO dto) {
        return productService.update(dto);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "product:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return productService.updateStatus(id, status);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "product:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping("/{id}/stock")
    public Result<?> updateStock(@PathVariable Long id, @RequestParam Integer stock) {
        return productService.updateStock(id, stock);
    }
}