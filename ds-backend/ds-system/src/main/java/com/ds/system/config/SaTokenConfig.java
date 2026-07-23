package com.ds.system.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpLogic;
import com.ds.common.constant.SaTokenConsts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册会员体系 StpLogic
     */
    @Primary
    @Bean
    public StpLogic stpLogicMember() {
        return new StpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER);
    }

    /**
     * 注册管理员体系 StpLogic
     */
    @Bean
    public StpLogic stpLogicAdmin() {
        return new StpLogic(SaTokenConsts.LOGIN_TYPE_ADMIN);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            boolean memberLogin = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_MEMBER).isLogin();
            boolean adminLogin = SaManager.getStpLogic(SaTokenConsts.LOGIN_TYPE_ADMIN).isLogin();
            if (!memberLogin && !adminLogin) {
                throw NotLoginException.newInstance(SaTokenConsts.LOGIN_TYPE_MEMBER, NotLoginException.NOT_TOKEN, NotLoginException.NOT_TOKEN_MESSAGE, null);
            }
        }))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/logout",
                        "/api/auth/social/**",
                        "/api/captcha/**",
                        "/api/pay/callback/**",
                        "/api/member/register",
                        "/api/member/login",
                        "/api/product/**",
                        "/api/category/**",
                        "/api/member/level/**",
                        "/uploads/**",
                        "/doc.html",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/error"
                );
    }
}