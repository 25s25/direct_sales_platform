package com.ds.oss.config;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OssWebMvcConfig implements WebMvcConfigurer {

    private final OssProperties ossProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            String basePath = ossProperties.getLocalBasePath();
            if (StrUtil.isBlank(basePath)) {
                log.warn("本地存储根目录未配置，跳过静态资源映射");
                return;
            }
            String location = "file:" + basePath;
            if (!basePath.endsWith(File.separator)) {
                location += File.separator;
            }
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations(location);
            log.info("本地文件静态资源映射：/uploads/** -> {}", location);
        } catch (Exception e) {
            log.warn("配置本地文件静态资源映射失败：{}", e.getMessage());
        }
    }
}
