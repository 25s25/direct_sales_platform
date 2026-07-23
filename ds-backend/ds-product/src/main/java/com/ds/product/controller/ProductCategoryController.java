package com.ds.product.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ds.common.constant.SaTokenConsts;
import com.ds.common.result.Result;
import com.ds.product.entity.ProductCategory;
import com.ds.product.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/product/category")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @GetMapping("/tree")
    public Result<List<ProductCategory>> listTree() {
        return productCategoryService.listTree();
    }

    @GetMapping("/{id}")
    public Result<ProductCategory> getById(@PathVariable Long id) {
        return productCategoryService.getById(id);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "product:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PostMapping
    public Result<?> add(@RequestBody ProductCategory cat) {
        return productCategoryService.add(cat);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "product:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @PutMapping
    public Result<?> update(@RequestBody ProductCategory cat) {
        return productCategoryService.update(cat);
    }

    @SaCheckLogin(type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @SaCheckPermission(value = "product:manage", type = SaTokenConsts.LOGIN_TYPE_ADMIN)
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        return productCategoryService.delete(id);
    }
}