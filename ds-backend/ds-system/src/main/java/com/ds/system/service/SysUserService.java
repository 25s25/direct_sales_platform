package com.ds.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.system.dto.SysUserDTO;
import com.ds.system.entity.SysUser;
import com.ds.system.vo.SysUserVO;

public interface SysUserService {

    Result<SysUserVO> getById(Long id);

    Result<SysUser> getByUsername(String username);

    Result<PageResult<SysUserVO>> page(Page<SysUserVO> page, String keyword);

    Result<Void> add(SysUserDTO user);

    Result<Void> update(SysUserDTO user);

    Result<Void> delete(Long id);

    Result<Void> updateStatus(Long id, Integer status);
}