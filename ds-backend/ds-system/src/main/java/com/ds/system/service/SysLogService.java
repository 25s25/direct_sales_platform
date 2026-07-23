package com.ds.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.system.entity.SysLog;

public interface SysLogService {

    Result<Void> saveLog(SysLog log);

    Result<PageResult<SysLog>> page(Page<SysLog> page, String keyword, String startDate, String endDate);
}