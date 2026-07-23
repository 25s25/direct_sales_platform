package com.ds.common.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

public abstract class BaseController<S extends com.baomidou.mybatisplus.extension.service.IService<T>, T> {

    @Autowired
    protected S service;

    @GetMapping("/{id}")
    public Result<T> getById(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @GetMapping("/list")
    public Result<List<T>> list() {
        return Result.ok(service.list());
    }

    @GetMapping("/page")
    public Result<PageResult<T>> page(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size) {
        Page<T> pageParam = new Page<>(page, size);
        IPage<T> result = service.page(pageParam);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @PostMapping
    public Result<?> save(@Valid @RequestBody T entity) {
        service.save(entity);
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@Valid @RequestBody T entity) {
        service.updateById(entity);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.ok();
    }
}