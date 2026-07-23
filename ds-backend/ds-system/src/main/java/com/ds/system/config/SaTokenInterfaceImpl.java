package com.ds.system.config;

import cn.dev33.satoken.stp.StpInterface;
import com.ds.system.mapper.SysPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SaTokenInterfaceImpl implements StpInterface {

    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = null;
        try {
            userId = Long.valueOf(String.valueOf(loginId));
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }
        List<String> permissions = sysPermissionMapper.selectPermissionCodesByUserId(userId);
        return permissions != null ? permissions : new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }
}
