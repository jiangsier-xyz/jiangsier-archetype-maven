#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SuppressWarnings("unused")
public @interface Trace {
    String value() default "";
    boolean ignoreArgs() default false;
    boolean ignoreReturn() default false;

    /**
     * Format: SpEL(Spring Expression Language)
     */
    String extInfo() default "";
}
