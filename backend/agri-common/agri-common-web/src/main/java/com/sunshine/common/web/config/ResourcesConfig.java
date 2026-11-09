package com.sunshine.common.web.config;

import com.sunshine.common.web.handler.GlobalExceptionHandler;
import com.sunshine.common.web.interceptor.PlusWebInvokeTimeInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 通用配置
 *
 * @author JHK
 */
@AutoConfiguration
public class ResourcesConfig implements WebMvcConfigurer {
    @Value("${yunji.cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
    private String allowedOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 全局访问性能拦截
        registry.addInterceptor(new PlusWebInvokeTimeInterceptor());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    }

    /**
     * 跨域配置
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(false);
        // 设置访问源地址
        for (String origin : allowedOrigins.split(",")) {
            String value = origin.trim();
            if (value.contains("*") || !value.startsWith("http")) {
                throw new IllegalStateException("CORS origins must be explicit HTTP origins");
            }
            config.addAllowedOrigin(value);
        }
        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 有效期 1800秒
        config.setMaxAge(1800L);
        // 添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // 返回新的CorsFilter
        return new CorsFilter(source);
    }

    /**
     * 全局异常处理器
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
