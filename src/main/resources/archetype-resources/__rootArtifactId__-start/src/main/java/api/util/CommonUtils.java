#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

@Slf4j
public class CommonUtils {
    public static <S, T> T convert(S source, Class<T> clazz) {
        try {
            Objects.requireNonNull(source);
            T response = clazz.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, response);
            return response;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("Failed to construct instance of {}", clazz.getSimpleName(), e);
            throw new IllegalStateException(e);
        }
    }
}
