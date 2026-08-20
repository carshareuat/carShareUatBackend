package com.carpool.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    @Autowired
    public StaticResourceConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String localRoot = appProperties.getFileStorage().getLocalRoot();
        Path base = Paths.get(localRoot).toAbsolutePath().normalize();
        String location = base.toUri().toString();
        if (!location.endsWith("/")) location = location + "/";
        // Expose files under /files/** so frontend can access stored files
        registry.addResourceHandler("/files/**")
            .addResourceLocations(location)
            .setCachePeriod(3600);
    }
}
