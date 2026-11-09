package com.sunshine.yunji.content.identity.application;

import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;

/** Uniform identity boundary, including newly added administrator read endpoints. */
@Configuration
public class AdminBoundary implements WebMvcConfigurer {
    private final Actors actors;
    public AdminBoundary(Actors actors){this.actors=actors;}
    @Override public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(new HandlerInterceptor(){
            @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){
                if(!"OPTIONS".equals(request.getMethod()))actors.staff();return true;
            }
        }).addPathPatterns("/api/v1/admin/**");
    }
}
