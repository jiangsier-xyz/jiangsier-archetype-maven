#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.access.auth.handler;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import ${package}.access.auth.customizer.OAuth2AuthorizationRequestCustomizer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("unused")
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private final OAuth2AuthorizationRequestCustomizer oAuth2AuthorizationRequestCustomizer;

    public OAuth2AuthenticationFailureHandler(OAuth2AuthorizationRequestCustomizer oAuth2AuthorizationRequestCustomizer) {
        this.oAuth2AuthorizationRequestCustomizer = oAuth2AuthorizationRequestCustomizer;
    }

    public OAuth2AuthenticationFailureHandler(String defaultFailureUrl, OAuth2AuthorizationRequestCustomizer oAuth2AuthorizationRequestCustomizer) {
        super(defaultFailureUrl);
        this.oAuth2AuthorizationRequestCustomizer = oAuth2AuthorizationRequestCustomizer;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        Optional.ofNullable(request.getParameter(OAuth2ParameterNames.STATE))
                .filter(StringUtils::isNotBlank)
                .map(oAuth2AuthorizationRequestCustomizer::getAndDeleteStateValue)
                .map(OAuth2StateValue::getAuthentication)
                .ifPresent(authentication ->
                        SecurityContextHolder.getContext().setAuthentication(authentication));

        log.warn("Failed to authenticate by oauth2!", exception);
        super.onAuthenticationFailure(request, response, exception);
    }
}
