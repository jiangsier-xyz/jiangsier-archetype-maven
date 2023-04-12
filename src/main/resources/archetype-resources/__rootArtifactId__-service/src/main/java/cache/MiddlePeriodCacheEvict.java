#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@CacheEvict(cacheNames = "middlePeriod")
@SuppressWarnings("unused")
public @interface MiddlePeriodCacheEvict {
    @AliasFor("keyBy")
    String key() default "";

    @AliasFor("key")
    String keyBy() default "";
}
