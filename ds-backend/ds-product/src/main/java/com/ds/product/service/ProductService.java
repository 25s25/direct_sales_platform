package com.ds.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.result.Result;
import com.ds.product.dto.ProductDTO;
import com.ds.product.vo.ProductVO;

public interface ProductService {

    Result<?> page(Page<ProductVO> page, Long categoryId, String keyword);

    Result<ProductVO> getById(Long id);

    Result<Void> add(ProductDTO dto);

    Result<Void> update(ProductDTO dto);

    Result<Void> updateStatus(Long id, Integer status);

    Result<Void> updateStock(Long id, Integer stock);
}