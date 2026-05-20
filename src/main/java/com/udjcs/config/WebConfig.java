package com.udjcs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login", "/logout", "/switch-user",
                        "/member-login", "/member-logout", "/register",
                        "/portal/**",
                        "/e/**",
                        "/organization/*/banner", "/organization/*/logo", "/organization/display-pictures/*",
                        "/invitations/*/banner",
                        "/members/*/photo",
                        "/gallery/*/image",
                        "/images/**", "/uploads/**", "/css/**", "/js/**", "/error");

        registry.addInterceptor(new MemberAuthInterceptor())
                .addPathPatterns("/portal/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
