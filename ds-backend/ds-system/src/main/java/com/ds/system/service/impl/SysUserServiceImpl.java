package com.ds.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ds.common.constant.Constants;
import com.ds.common.exception.BusinessException;
import com.ds.common.result.PageResult;
import com.ds.common.result.Result;
import com.ds.system.dto.SysUserDTO;
import com.ds.system.entity.SysUser;
import com.ds.system.entity.SysUserRole;
import com.ds.system.mapper.SysUserMapper;
import com.ds.system.mapper.SysUserRoleMapper;
import com.ds.system.service.SysUserService;
import com.ds.system.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public Result<SysUserVO> getById(Long id) {
        SysUser user = super.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        SysUserVO vo = convertToVO(user);
        return Result.ok(vo);
    }

    @Override
    public Result<SysUser> getByUsername(String username) {
        SysUser user = lambdaQuery().eq(SysUser::getUsername, username).one();
        return Result.ok(user);
    }

    @Override
    public Result<PageResult<SysUserVO>> page(Page<SysUserVO> page, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> userPage = new Page<>(page.getCurrent(), page.getSize());
        Page<SysUser> result = this.page(userPage, wrapper);
        List<SysUserVO> records = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return Result.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> add(SysUserDTO dto) {
        if (StrUtil.isBlank(dto.getUsername())) {
            return Result.fail("用户名不能为空");
        }
        if (StrUtil.isBlank(dto.getPassword())) {
            return Result.fail("密码不能为空");
        }

        SysUser existUser = lambdaQuery().eq(SysUser::getUsername, dto.getUsername()).one();
        if (existUser != null) {
            return Result.fail("用户名已存在");
        }

        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user, "id");
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : Constants.STATUS_ENABLE);
        if (StrUtil.isBlank(user.getAvatar())) {
            user.setAvatar(Constants.DEFAULT_AVATAR);
        }

        boolean saved = this.save(user);
        if (!saved) {
            return Result.fail("新增用户失败");
        }
        saveUserRoles(user.getId(), dto.getRoleIds());
        log.info("新增用户成功: {}", user.getUsername());
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(SysUserDTO dto) {
        if (dto.getId() == null) {
            return Result.fail("用户ID不能为空");
        }

        SysUser dbUser = super.getById(dto.getId());
        if (dbUser == null) {
            return Result.fail("用户不存在");
        }

        if (StrUtil.isNotBlank(dto.getUsername()) && !dto.getUsername().equals(dbUser.getUsername())) {
            SysUser existUser = lambdaQuery().eq(SysUser::getUsername, dto.getUsername()).one();
            if (existUser != null) {
                return Result.fail("用户名已存在");
            }
        }

        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user);
        if (StrUtil.isNotBlank(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        } else {
            user.setPassword(null);
        }

        boolean updated = this.updateById(user);
        if (!updated) {
            return Result.fail("修改用户失败");
        }
        saveUserRoles(dto.getId(), dto.getRoleIds());
        log.info("修改用户成功: {}", dto.getId());
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        if (id == null) {
            return Result.fail("用户ID不能为空");
        }
        this.checkDefaultAdmin(id);
        boolean removed = this.removeById(id);
        if (!removed) {
            return Result.fail("删除用户失败");
        }
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, id);
        sysUserRoleMapper.delete(wrapper);
        log.info("删除用户成功: {}", id);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStatus(Long id, Integer status) {
        if (id == null) {
            return Result.fail("用户ID不能为空");
        }
        if (status == null) {
            return Result.fail("状态不能为空");
        }
        this.checkDefaultAdmin(id);

        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        boolean updated = this.updateById(user);
        if (!updated) {
            return Result.fail("修改用户状态失败");
        }
        log.info("修改用户状态成功: id={}, status={}", id, status);
        return Result.ok();
    }

    private void checkDefaultAdmin(Long id) {
        SysUser user = super.getById(id);
        if (user != null && "admin".equals(user.getUsername())) {
            throw new BusinessException("默认管理员不可操作");
        }
    }

    private SysUserVO convertToVO(SysUser user) {
        SysUserVO vo = new SysUserVO();
        BeanUtil.copyProperties(user, vo);
        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(user.getId());
        vo.setRoleIds(roleIds);
        return vo;
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            return;
        }
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId);
        sysUserRoleMapper.delete(wrapper);
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        List<SysUserRole> list = roleIds.stream()
                .filter(roleId -> roleId != null)
                .distinct()
                .map(roleId -> {
                    SysUserRole ur = new SysUserRole();
                    ur.setUserId(userId);
                    ur.setRoleId(roleId);
                    return ur;
                }).collect(Collectors.toList());
        if (!list.isEmpty()) {
            list.forEach(sysUserRoleMapper::insert);
        }
    }
}
