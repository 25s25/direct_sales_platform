package com.ds.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.common.constant.Constants;
import com.ds.common.result.Result;
import com.ds.product.entity.ProductCategory;
import com.ds.product.mapper.ProductCategoryMapper;
import com.ds.product.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements ProductCategoryService {

    @Override
    public Result<List<ProductCategory>> listTree() {
        List<ProductCategory> allCategories = lambdaQuery()
                .orderByAsc(ProductCategory::getSortOrder)
                .orderByAsc(ProductCategory::getId)
                .list();

        Map<Long, List<ProductCategory>> parentChildMap = allCategories.stream()
                .filter(cat -> cat.getParentId() != null && cat.getParentId() != 0)
                .collect(Collectors.groupingBy(ProductCategory::getParentId));

        List<ProductCategory> tree = allCategories.stream()
                .filter(cat -> cat.getParentId() == null || cat.getParentId() == 0)
                .peek(cat -> cat.setChildren(buildChildren(cat.getId(), parentChildMap)))
                .collect(Collectors.toList());

        return Result.ok(tree);
    }

    @Override
    public Result<ProductCategory> getById(Long id) {
        if (id == null) {
            return Result.fail("分类ID不能为空");
        }
        ProductCategory category = super.getById(id);
        if (category == null) {
            return Result.fail("分类不存在");
        }
        return Result.ok(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> add(ProductCategory cat) {
        if (cat.getParentId() != null && cat.getParentId() != 0) {
            ProductCategory parent = super.getById(cat.getParentId());
            if (parent == null) {
                return Result.fail("父级分类不存在");
            }
        }
        if (cat.getStatus() == null) {
            cat.setStatus(Constants.STATUS_ENABLE);
        }
        boolean saved = this.save(cat);
        if (!saved) {
            return Result.fail("添加分类失败");
        }
        log.info("商品分类添加成功: id={}, name={}", cat.getId(), cat.getName());
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(ProductCategory cat) {
        if (cat.getId() == null) {
            return Result.fail("分类ID不能为空");
        }
        ProductCategory existing = super.getById(cat.getId());
        if (existing == null) {
            return Result.fail("分类不存在");
        }
        boolean updated = this.updateById(cat);
        if (!updated) {
            return Result.fail("更新分类失败");
        }
        log.info("商品分类更新成功: id={}, name={}", cat.getId(), cat.getName());
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        if (id == null) {
            return Result.fail("分类ID不能为空");
        }
        ProductCategory category = super.getById(id);
        if (category == null) {
            return Result.fail("分类不存在");
        }
        long childCount = lambdaQuery().eq(ProductCategory::getParentId, id).count();
        if (childCount > 0) {
            return Result.fail("该分类下存在子分类，无法删除");
        }
        boolean removed = this.removeById(id);
        if (!removed) {
            return Result.fail("删除分类失败");
        }
        log.info("商品分类删除成功: id={}, name={}", id, category.getName());
        return Result.ok();
    }

    private List<ProductCategory> buildChildren(Long parentId, Map<Long, List<ProductCategory>> parentChildMap) {
        List<ProductCategory> children = parentChildMap.getOrDefault(parentId, new ArrayList<>());
        for (ProductCategory child : children) {
            child.setChildren(buildChildren(child.getId(), parentChildMap));
        }
        return children;
    }
}