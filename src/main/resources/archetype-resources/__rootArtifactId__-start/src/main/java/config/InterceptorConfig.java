#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ${package}.access.trace.TraceInterceptor;

@Configuration
@SuppressWarnings("unused")
public class InterceptorConfig implements WebMvcConfigurer {
    @Value("${symbol_dollar}{perf.trace.excludeUri:${symbol_pound}{null}}")
    private String[] traceExcludeUri;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration registration =
                registry.addInterceptor(traceInterceptor())
                        .addPathPatterns("/**");
        if (traceExcludeUri != null && traceExcludeUri.length > 0) {
            registration.excludePathPatterns(traceExcludeUri);
        }

        WebMvcConfigurer.super.addInterceptors(registry);
    }

    @Bean
    public TraceInterceptor traceInterceptor() {
        return new TraceInterceptor();
    }
}