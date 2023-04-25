#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.auth.provider;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;
import ${package}.auth.authentication.ApiTokenAuthenticationToken;
import ${package}.auth.user.SysUserDetails;
import ${package}.account.SysApiTokenService;
import ${package}.account.SysAuthorityService;
import ${package}.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@SuppressWarnings("unused")
public class ApiTokenAuthenticationProvider implements AuthenticationProvider, AuthenticationConverter {
    @Value("${symbol_dollar}{auth.token.parameterName:_token}")
    private String parameterName;

    @Value("${symbol_dollar}{auth.token.headerName:X-API-TOKEN}")
    private String headerName;

    @Value("${symbol_dollar}{auth.token.prefix:${symbol_pound}{null}}")
    private String prefix;

    @Autowired
    private SysApiTokenService apiTokenService;

    @Autowired
    private SysAuthorityService authorityService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiTokenAuthenticationToken apiTokenAuthentication = (ApiTokenAuthenticationToken) authentication;
        Set<String> tokens = (Set<String>)apiTokenAuthentication.getCredentials();
        SysUserDetails sysUser = null;
        for (String token : tokens) {
            User user = apiTokenService.getUser(token);
            if (Objects.nonNull(user)) {
                sysUser = SysUserDetails.builder()
                        .fromUser(user)
                        .withAuthorityService(authorityService)
                        .withPasswordEncoder(passwordEncoder)
                        .build();
                break;
            }
        }

        if (Objects.isNull(sysUser)) {
            throw new BadCredentialsException("Invalid tokens: " + String.join(",", tokens));
        }

        ApiTokenAuthenticationToken authenticated = new ApiTokenAuthenticationToken(sysUser, tokens);
        authenticated.setDetails(authentication.getDetails());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return aClass.equals(ApiTokenAuthenticationToken.class);
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        Set<String> tokens = resolveTokensFromParameters(request);
        if (CollectionUtils.isEmpty(tokens)) {
            tokens = resolveTokensFromHeader(request);
        }

        return CollectionUtils.isNotEmpty(tokens) ? new ApiTokenAuthenticationToken(tokens) : null;
    }

    private boolean validateToken(String token) {
        return Objects.isNull(prefix) ? StringUtils.isNotBlank(token) : token.startsWith(prefix);
    }

    private Set<String> resolveTokensFromParameters(HttpServletRequest request) {
        String[] tokens = request.getParameterValues(parameterName);
        return Objects.isNull(tokens) || tokens.length == 0 ? Collections.emptySet()
                : Arrays.stream(tokens).filter(this::validateToken).collect(Collectors.toSet());
    }

    private Set<String> resolveTokensFromHeader(HttpServletRequest request) {
        String tokenValue = request.getHeader(headerName);
        return StringUtils.isBlank(tokenValue) ? Collections.emptySet()
                : Arrays.stream(tokenValue.split(",")).filter(this::validateToken).collect(Collectors.toSet());
    }
}
