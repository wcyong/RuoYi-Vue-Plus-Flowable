package com.ruoyi.report.config;

import com.ruoyi.report.interceptor.JmReportHandlerInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvcConfigurer 配置
 *
 * @author gssong
 */
@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${checkToken.url}")
    private String url;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JmReportHandlerInterceptor(url));
    }

}
