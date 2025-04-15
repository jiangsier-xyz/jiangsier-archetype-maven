#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.access.auth.customizer;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Component
@SuppressWarnings("unused")
public class OAuth2AuthorizationRequestCustomizer implements Consumer<OAuth2AuthorizationRequest.Builder>, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthorizationRequestCustomizer.class);

    private static final String BIND_MODE_PARAMETER_NAME = "bind";

    private static final String CACHED_AUTHENTICATION_PREFIX =
            OAuth2AuthorizationRequestCustomizer.class.getName() + "-";

    private static final Duration CACHED_AUTHENTICATION_TIMEOUT = Duration.ofMinutes(10);

    private static final StringKeyGenerator DEFAULT_STATE_GENERATOR =
            new Base64StringKeyGenerator(Base64.getUrlEncoder());

    @Value("${auth.login.oauth2.targetUrlParameter:#{null}}")
    private String targetUrlParameter;
    @Autowired
    private RedissonClient persistentClient;
    private ApplicationContext applicationContext;

    @Override
    public void accept(OAuth2AuthorizationRequest.Builder builder) {
        StringBuilder registrationIdBuilder = new StringBuilder();
        builder.attributes(attrs ->
                registrationIdBuilder.append(attrs.get("registration_id")));
        String registrationId = registrationIdBuilder.toString();
        if (StringUtils.isBlank(registrationId)) {
            return;
        }

        BiConsumer<OAuth2AuthorizationRequest.Builder, Boolean> registrationCustomizer = null;
        try {
            String beanName = beanNameForRegistrationId(registrationId);
            registrationCustomizer = getBean(beanName);
        } catch (Exception e) {
            logger.warn("Failed to find resolver for {}", registrationId, e);
        }

        boolean isBinding = false;
        String redirectUrl = null;
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            isBinding = "1".equals(request.getParameter(BIND_MODE_PARAMETER_NAME));
            if (StringUtils.isNotBlank(targetUrlParameter)) {
                redirectUrl = request.getParameter(targetUrlParameter);
            }
        }

        if (registrationCustomizer != null) {
            registrationCustomizer.accept(builder, isBinding);
        }

        Authentication authenticated = null;

        if (isBinding) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                authenticated = authentication;
            }
        }

        OAuth2StateValue stateValue = OAuth2StateValue.builder()
                .authentication(authenticated)
                .redirectUrl(redirectUrl)
                .build();
        String state = DEFAULT_STATE_GENERATOR.generateKey();
        RBucket<OAuth2StateValue> bucket = persistentClient.getBucket(CACHED_AUTHENTICATION_PREFIX + state);
        bucket.set(stateValue, CACHED_AUTHENTICATION_TIMEOUT);
        builder.state(state);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public OAuth2StateValue getAndDeleteStateValue(String state) {
        if (StringUtils.isNotBlank(state)) {
            RBucket<OAuth2StateValue> bucket = persistentClient.getBucket(CACHED_AUTHENTICATION_PREFIX + state);
            if (bucket.isExists()) {
                return bucket.getAndDelete();
            }
        }
        return null;
    }

    private String beanNameForRegistrationId(String registrationId) {
        return registrationId + "OAuth2AuthorizationRequestCustomizer";
    }

    private BiConsumer<OAuth2AuthorizationRequest.Builder, Boolean> getBean(String beanName) {
        if(applicationContext.containsBean(beanName)){
            return (BiConsumer<OAuth2AuthorizationRequest.Builder, Boolean>) applicationContext.getBean(beanName);
        }else{
            return null;
        }
    }
}
