package com.digniche.muntum.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 정적 리소스 서빙 (로컬)
 * - /uploads/**로 들어오는 여청을 ./uploads/ 폴더의 실제 파일로 연결
 */
@Configuration
@Profile("local")
public class WebConfig implements WebMvcConfigurer {
    @Value("${storage.local.base-path}")
    private String basePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + basePath + "/");
    }
}
