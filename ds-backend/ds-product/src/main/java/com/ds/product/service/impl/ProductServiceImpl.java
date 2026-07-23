package com.ds.product.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.common.constant.Constants;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.product.dto.ProductDTO;
import com.ds.product.entity.Product;
import com.ds.product.entity.ProductCategory;
import com.ds.product.mapper.ProductCategoryMapper;
import com.ds.product.mapper.ProductMapper;
import com.ds.product.service.ProductService;
import com.ds.product.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductCategoryMapper productCategoryMapper;

    @Override
    public Result<?> page(Page<ProductVO> pageParam, Long categoryId, String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(Product::getName, keyword)
                    .or()
                    .like(Product::getProductNo, keyword)
                    .or()
                    .like(Product::getSubtitle, keyword));
        }
        wrapper.orderByDesc(Product::getCreateTime);

        Page<Product> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        Page<Product> result = this.page(page, wrapper);

        List<ProductVO> voList = result.getRecords().stream()
                .map(this::toProductVO)
                .collect(Collectors.toList());

        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), voList));
    }

    @Override
    public Result<ProductVO> getById(Long id) {
        if (id == null) {
            return Result.fail("商品ID不能为空");
        }
        Product product = super.getById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        return Result.ok(toProductVO(product));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> add(ProductDTO dto) {
        if (dto.getCategoryId() != null) {
            ProductCategory category = productCategoryMapper.selectById(dto.getCategoryId());
            if (category == null) {
                return Result.fail("商品分类不存在");
            }
        }

        Product product = new Product();
        product.setProductNo(IdUtil.getSnowflakeNextIdStr());
        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName());
        product.setSubtitle(dto.getSubtitle());
        product.setMainImage(dto.getMainImage());
        product.setImages(dto.getImages());
        product.setDetail(dto.getDetail());
        product.setRetailPrice(dto.getRetailPrice());
        product.setMemberPrice(dto.getMemberPrice());
        product.setPv(dto.getPv());
        product.setStock(dto.getStock());
        product.setSalesCount(0);
        product.setIsRecommend(dto.getIsRecommend() != null ? dto.getIsRecommend() : 0);
        product.setIsNew(dto.getIsNew() != null ? dto.getIsNew() : 0);
        product.setStatus(dto.getStatus() != null ? dto.getStatus() : Constants.STATUS_ENABLE);

        boolean saved = this.save(product);
        if (!saved) {
            return Result.fail("添加商品失败");
        }

        log.info("商品添加成功: productNo={}, name={}", product.getProductNo(), product.getName());
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(ProductDTO dto) {
        if (dto.getId() == null) {
            return Result.fail("商品ID不能为空");
        }
        Product product = new Product();
        product.setId(dto.getId());
        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName());
        product.setSubtitle(dto.getSubtitle());
        product.setMainImage(dto.getMainImage());
        product.setImages(dto.getImages());
        product.setDetail(dto.getDetail());
        product.setRetailPrice(dto.getRetailPrice());
        product.setMemberPrice(dto.getMemberPrice());
        product.setPv(dto.getPv());
        product.setStock(dto.getStock());
        product.setIsRecommend(dto.getIsRecommend());
        product.setIsNew(dto.getIsNew());
        product.setStatus(dto.getStatus());

        boolean updated = this.updateById(product);
        if (!updated) {
            return Result.fail("更新商品失败");
        }

        log.info("商品更新成功: id={}", product.getId());
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStatus(Long id, Integer status) {
        if (id == null) {
            return Result.fail("商品ID不能为空");
        }
        if (status == null) {
            return Result.fail("状态不能为空");
        }

        Product product = super.getById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }

        Product updateProduct = new Product();
        updateProduct.setId(id);
        updateProduct.setStatus(status);
        boolean updated = this.updateById(updateProduct);
        if (!updated) {
            return Result.fail("修改商品状态失败");
        }

        log.info("修改商品状态成功: id={}, status={}", id, status);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStock(Long id, Integer stock) {
        if (id == null) {
            return Result.fail("商品ID不能为空");
        }
        if (stock == null || stock < 0) {
            return Result.fail("库存数量不合法");
        }

        Product product = super.getById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }

        Product updateProduct = new Product();
        updateProduct.setId(id);
        updateProduct.setStock(stock);
        boolean updated = this.updateById(updateProduct);
        if (!updated) {
            return Result.fail("修改商品库存失败");
        }

        log.info("修改商品库存成功: id={}, stock={}", id, stock);
        return Result.ok();
    }

    private ProductVO toProductVO(Product product) {
        if (product == null) {
            return null;
        }
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setProductNo(product.getProductNo());
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(getCategoryName(product.getCategoryId()));
        vo.setName(product.getName());
        vo.setSubtitle(product.getSubtitle());
        vo.setMainImage(product.getMainImage());
        vo.setImages(product.getImages());
        vo.setDetail(product.getDetail());
        vo.setRetailPrice(product.getRetailPrice());
        vo.setMemberPrice(product.getMemberPrice());
        vo.setPv(product.getPv());
        vo.setStock(product.getStock());
        vo.setSalesCount(product.getSalesCount());
        vo.setIsRecommend(product.getIsRecommend());
        vo.setIsNew(product.getIsNew());
        vo.setStatus(product.getStatus());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        return vo;
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        ProductCategory category = productCategoryMapper.selectById(categoryId);
        return category != null ? category.getName() : null;
    }
}