#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service.cache;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import ${package}.util.SpELUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

@Component("fullNameKeyGenerator")
@SuppressWarnings("unused")
public class FullNameKeyGenerator implements KeyGenerator {
    private static final Logger logger = LoggerFactory.getLogger(FullNameKeyGenerator.class);

    private static final String[] SHORT_PERIOD_CACHE_NAMES = new String[]{"shortPeriod"};
    private static final String[] MIDDLE_PERIOD_CACHE_NAMES = new String[]{"middlePeriod"};
    private static final String[] LONG_PERIOD_CACHE_NAMES = new String[]{"longPeriod"};

    @Override
    public @NonNull Object generate(@NonNull Object target, @NonNull Method method, @Nullable Object... params) {
        Pair<String[], String> cachesKeyExpr = getCachesKeyExpr(method);
        String key = null;
        if (cachesKeyExpr != null && !ObjectUtils.isEmpty(cachesKeyExpr.getRight())) {
            key = generateSpELKey(target, method, params, cachesKeyExpr);
        }
        if (!StringUtils.hasText(key)) {
            key = generateDefaultKey(target, method, params);
        }
        return key;
    }

    private Pair<String[], String> getCachesKeyExpr(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof ShortPeriodCache shortCache) {
                return Pair.of(SHORT_PERIOD_CACHE_NAMES, shortCache.keyBy());
            } else if (annotation instanceof MiddlePeriodCache middleCache) {
                return Pair.of(MIDDLE_PERIOD_CACHE_NAMES, middleCache.keyBy());
            } else if (annotation instanceof LongPeriodCache longCache) {
                return Pair.of(LONG_PERIOD_CACHE_NAMES, longCache.keyBy());
            } else if (annotation instanceof Cacheable cache) {
                return Pair.of(cache.cacheNames(), cache.key());
            }
        }

        return null;
    }

    private Pair<String[], String> getCachesKeyExpr(Method method) {
        Pair<String[], String> cachesKeyExpr = getCachesKeyExpr(method.getAnnotations());
        return cachesKeyExpr != null ? cachesKeyExpr
                : getCachesKeyExpr(method.getDeclaringClass().getAnnotations());
    }

    private String generateDefaultKey(Object target, Method method, Object[] params) {
        String key = target.getClass().getName() + "::" + method.getName() + "_";
        if (params != null && params.length > 0) {
            key += StringUtils.arrayToDelimitedString(params, "_");
        }
        return key;
    }

    private String generateSpELKey(Object target, Method method, Object[] params,
                                   Pair<String[], String> cachesKeyExpr) {
        SpelExpressionParser parser = new SpelExpressionParser();
        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
        Map<String, Object> contextInfo = SpELUtils.extractInfo(target, method, params);
        contextInfo.forEach(context::setVariable);
        context.setVariable("caches", cachesKeyExpr.getLeft());
        try {
            Expression expr = parser.parseExpression(cachesKeyExpr.getRight());
            return expr.getValue(context, String.class);
        } catch (ParseException | EvaluationException e) {
            logger.error("Failed to generate SpEL key!", e);
        }
        return null;
    }

    static class Ada {
        public String hi = "Hello ";

        public String sayHello(String somebody) {
            return hi + somebody;
        }
    }

    private static Pair<String[], String> testPair(String expr) {
        return Pair.of(LONG_PERIOD_CACHE_NAMES, expr);
    }

    public static void main(String[] args) throws NoSuchMethodException {
        Ada ada = new Ada();
        Method hello = Ada.class.getMethod("sayHello", String.class);
        String bob = "bob";

        FullNameKeyGenerator gen = new FullNameKeyGenerator();
        Object[] params = new Object[]{bob};
        System.out.println(gen.generateSpELKey(ada, hello, params, testPair("${symbol_pound}methodName")));
        System.out.println(gen.generateSpELKey(ada, hello, params, testPair("${symbol_pound}method.name")));
        System.out.println(gen.generateSpELKey(ada, hello, params, testPair("${symbol_pound}args")));
        System.out.println(gen.generateSpELKey(ada, hello, params, testPair("${symbol_pound}p0")));
        System.out.println(gen.generateSpELKey(ada, hello, params, testPair("${symbol_pound}somebody")));
        System.out.println(gen.generateSpELKey(ada, hello, params, testPair("${symbol_pound}target.hi")));
        System.out.println(gen.generateSpELKey(ada, hello, params, testPair("'Bye ' + ${symbol_pound}p0")));
    }
}
