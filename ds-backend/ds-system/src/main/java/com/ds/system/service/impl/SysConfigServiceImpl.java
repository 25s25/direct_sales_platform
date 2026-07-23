package com.ds.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.common.result.Result;
import com.ds.system.entity.SysConfig;
import com.ds.system.mapper.SysConfigMapper;
import com.ds.system.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public Result<SysConfig> getByKey(String key) {
        if (StrUtil.isBlank(key)) {
            return Result.fail("配置键不能为空");
        }
        SysConfig config = lambdaQuery().eq(SysConfig::getConfigKey, key).one();
        return Result.ok(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateByKey(String key, String value) {
        if (StrUtil.isBlank(key)) {
            return Result.fail("配置键不能为空");
        }

        SysConfig config = lambdaQuery().eq(SysConfig::getConfigKey, key).one();
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            this.save(config);
        } else {
            config.setConfigValue(value);
            this.updateById(config);
        }

        log.info("更新配置成功: {} = {}", key, value);
        return Result.ok();
    }

    @Override
    public Result<List<SysConfig>> listAll() {
        List<SysConfig> list = this.list();
        return Result.ok(list);
    }
}