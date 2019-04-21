package com.whohim.library.com.whohim.library.fillter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;


/**
 * @author WhomHim
 * @description
 * @date Create in 2019/4/19 16:22
 */
@Configuration
public class WebAppConfig extends WebMvcConfigurationSupport {

    @Bean
    public SessionInterceptor sessionInterceptor() {
        return new SessionInterceptor();
    }

    /**
     * 注册 拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 设置拦截的路径、不拦截的路径、优先级等等
        registry.addInterceptor(sessionInterceptor())
                .excludePathPatterns("/user/bindLibrary/", "/user/cancelBindLibrary/", "/user/getSessionkey/")
                .addPathPatterns("/**");
        super.addInterceptors(registry);
    }


}
