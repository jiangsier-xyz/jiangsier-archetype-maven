#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service.cache;

import org.springframework.cache.annotation.Cacheable;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Cacheable(cacheNames = "middlePeriod", keyGenerator = "fullNameKeyGenerator", unless = "#result == null")
@SuppressWarnings("unused")
public @interface MiddlePeriodCache {
    String keyBy() default "";
}
