package com.ewb.gateway.config;

import com.ewb.common.security.JwtUtil;
import com.ewb.gateway.filter.ReverseProxyFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewaySecurityConfig {

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public FilterRegistrationBean<ReverseProxyFilter> reverseProxyFilterRegistration(ReverseProxyFilter filter) {
        FilterRegistrationBean<ReverseProxyFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setName("reverseProxyFilter");
        registration.setOrder(1);
        return registration;
    }
}
