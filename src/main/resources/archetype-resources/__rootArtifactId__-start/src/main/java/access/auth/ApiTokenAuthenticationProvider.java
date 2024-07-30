#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.access.auth;

import jakarta.servlet.http.HttpServletRequest;
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
import org.thymeleaf.util.ArrayUtils;
import ${package}.access.auth.user.SysUserDetails;
import ${package}.model.User;
import ${package}.service.account.SysApiTokenService;
import ${package}.service.account.SysAuthorityService;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
@SuppressWarnings("unused")
public class ApiTokenAuthenticationProvider implements AuthenticationProvider, AuthenticationConverter {
    @Value("${symbol_dollar}{auth.token.parameterName:${symbol_pound}{null}}")
    private String parameterName;

    @Value("${symbol_dollar}{auth.token.headerName:${symbol_pound}{null}}")
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

        if (sysUser == null) {
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
        Set<String> tokens = resolveTokensFromQuery(request);
        if (CollectionUtils.isEmpty(tokens)) {
            tokens = resolveTokensFromHeader(request);
        }

        return CollectionUtils.isNotEmpty(tokens) ? new ApiTokenAuthenticationToken(tokens) : null;
    }

    private boolean validateToken(String token) {
        return prefix == null ? StringUtils.isNotBlank(token) : token.startsWith(prefix);
    }

    private Set<String> resolveTokensFromQuery(HttpServletRequest request) {
        if (StringUtils.isBlank(parameterName)) {
            return null;
        }
        String[] tokens = request.getParameterValues(parameterName);
        return ArrayUtils.isEmpty(tokens) ? null
                : Arrays.stream(tokens).filter(this::validateToken).collect(Collectors.toSet());
    }

    private Set<String> resolveTokensFromHeader(HttpServletRequest request) {
        String tokenValue;
        if (StringUtils.isBlank(headerName)) {
            tokenValue = StringUtils.removeStart(request.getHeader(AUTHORIZATION), "Bearer ");
        } else {
            tokenValue = request.getHeader(headerName);
        }
        return StringUtils.isBlank(tokenValue) ? null
                : Arrays.stream(tokenValue.trim().split(",")).filter(this::validateToken).collect(Collectors.toSet());
    }
}
