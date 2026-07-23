package com.ds.product.service;

import com.ds.common.result.Result;
import com.ds.product.entity.ProductCategory;

import java.util.List;

public interface ProductCategoryService {

    Result<List<ProductCategory>> listTree();

    Result<ProductCategory> getById(Long id);

    Result<Void> add(ProductCategory cat);

    Result<Void> update(ProductCategory cat);

    Result<Void> delete(Long id);
}