package com.ds.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.system.entity.SysLog;
import com.ds.system.mapper.SysLogMapper;
import com.ds.system.service.SysLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> saveLog(SysLog log) {
        boolean saved = super.save(log);
        if (!saved) {
            return Result.fail("保存日志失败");
        }
        return Result.ok();
    }

    @Override
    public Result<PageResult<SysLog>> page(Page<SysLog> page, String keyword, String startDate, String endDate) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysLog::getUsername, keyword)
                    .or()
                    .like(SysLog::getModule, keyword)
                    .or()
                    .like(SysLog::getAction, keyword)
                    .or()
                    .like(SysLog::getDescription, keyword));
        }
        if (StrUtil.isNotBlank(startDate)) {
            wrapper.ge(SysLog::getCreateTime, startDate + " 00:00:00");
        }
        if (StrUtil.isNotBlank(endDate)) {
            wrapper.le(SysLog::getCreateTime, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(SysLog::getCreateTime);
        Page<SysLog> result = this.page(page, wrapper);
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }
}