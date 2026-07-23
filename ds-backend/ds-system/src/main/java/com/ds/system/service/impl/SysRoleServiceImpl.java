package com.ds.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.common.constant.Constants;
import com.ds.common.result.Result;
import com.ds.system.entity.SysRole;
import com.ds.system.mapper.SysRoleMapper;
import com.ds.system.service.SysRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Override
    public Result<List<SysRole>> listAll() {
        List<SysRole> list = this.list();
        return Result.ok(list);
    }

    @Override
    public Result<SysRole> getById(Long id) {
        if (id == null) {
            return Result.fail("角色ID不能为空");
        }
        SysRole role = super.getById(id);
        if (role == null) {
            return Result.fail("角色不存在");
        }
        return Result.ok(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> add(SysRole role) {
        if (StrUtil.isBlank(role.getName())) {
            return Result.fail("角色名称不能为空");
        }
        if (StrUtil.isBlank(role.getRoleCode())) {
            return Result.fail("角色编码不能为空");
        }

        long count = lambdaQuery().eq(SysRole::getRoleCode, role.getRoleCode()).count();
        if (count > 0) {
            return Result.fail("角色编码已存在");
        }

        role.setStatus(role.getStatus() != null ? role.getStatus() : Constants.STATUS_ENABLE);
        boolean saved = this.save(role);
        if (!saved) {
            return Result.fail("新增角色失败");
        }
        log.info("新增角色成功: {}", role.getName());
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(SysRole role) {
        if (role.getId() == null) {
            return Result.fail("角色ID不能为空");
        }

        SysRole dbRole = super.getById(role.getId());
        if (dbRole == null) {
            return Result.fail("角色不存在");
        }

        if (StrUtil.isNotBlank(role.getRoleCode()) && !role.getRoleCode().equals(dbRole.getRoleCode())) {
            long count = lambdaQuery().eq(SysRole::getRoleCode, role.getRoleCode()).count();
            if (count > 0) {
                return Result.fail("角色编码已存在");
            }
        }

        boolean updated = this.updateById(role);
        if (!updated) {
            return Result.fail("修改角色失败");
        }
        log.info("修改角色成功: {}", role.getId());
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        if (id == null) {
            return Result.fail("角色ID不能为空");
        }
        boolean removed = this.removeById(id);
        if (!removed) {
            return Result.fail("删除角色失败");
        }
        log.info("删除角色成功: {}", id);
        return Result.ok();
    }
}