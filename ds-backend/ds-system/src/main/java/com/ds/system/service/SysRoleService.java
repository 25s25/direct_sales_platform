package com.ds.system.service;

import com.ds.common.result.Result;
import com.ds.system.entity.SysRole;

import java.util.List;

public interface SysRoleService {

    Result<List<SysRole>> listAll();

    Result<SysRole> getById(Long id);

    Result<Void> add(SysRole role);

    Result<Void> update(SysRole role);

    Result<Void> delete(Long id);
}