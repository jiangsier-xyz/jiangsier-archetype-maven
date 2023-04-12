#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class SpELUtils {
    private static final Logger logger = LoggerFactory.getLogger(SpELUtils.class);

    public static Map<String, Object> extractInfo(Object target, Method method, Object[] args) {
        Map<String, Object> info = new HashMap<>(args.length * 3 + 5);

        info.put("methodName", method.getName());
        info.put("method", method);
        info.put("target", target);
        info.put("targetClass", target.getClass());
        info.put("args", args);

        Parameter[] parameters = method.getParameters();
        if (parameters.length != args.length) {
            logger.error("Oops...The method parameters length is {} but runtime arguments length is {}",
                    parameters.length, args.length);
        }

        for (int i = 0; i < args.length; ++i) {
            info.put("p" + i, args[i]);
            info.put("a" + i, args[i]);
            if (i >= parameters.length) {
                continue;
            }
            Parameter parameter = parameters[i];
            if (parameter.isNamePresent()) {
                info.put(parameter.getName(), args[i]);
            }
        }

        return info;
    }
}
