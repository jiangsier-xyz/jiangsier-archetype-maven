#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.access.auth.handler;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import ${package}.access.auth.customizer.OAuth2AuthorizationRequestCustomizer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@SuppressWarnings("unused")
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

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
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        Authentication sysAuth = StringUtils.isNotBlank(state) ?
                oAuth2AuthorizationRequestCustomizer.getAndDeleteCachedAuthentication(state) : null;

        if (Objects.nonNull(sysAuth)) {
            SecurityContextHolder.getContext().setAuthentication(sysAuth);
        }

        logger.warn("Failed to authenticate by oauth2!", exception);
        super.onAuthenticationFailure(request, response, exception);
    }
}
