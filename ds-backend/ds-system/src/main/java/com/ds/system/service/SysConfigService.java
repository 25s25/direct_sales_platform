package com.ds.system.service;

import com.ds.common.result.Result;
import com.ds.system.entity.SysConfig;

import java.util.List;

public interface SysConfigService {

    Result<SysConfig> getByKey(String key);

    Result<Void> updateByKey(String key, String value);

    Result<List<SysConfig>> listAll();
}