#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.config;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import ${package}.access.auth.customizer.OAuth2AuthorizationRequestCustomizer;
import ${package}.access.auth.handler.OAuth2AuthenticationFailureHandler;
import ${package}.access.auth.handler.OAuth2AuthenticationSuccessHandler;
import ${package}.access.auth.ApiTokenAuthenticationProvider;
import ${package}.access.auth.user.SysUserDetailsManager;
import ${package}.service.account.SysAuthorityService;
import ${package}.service.account.SysBindService;
import ${package}.service.account.SysUserService;
import ${package}.util.AuthorityUtils;

import java.util.Arrays;

@Configuration
@SuppressWarnings("unused")
public class SecurityConfig {
    @Value("${symbol_dollar}{auth.role.adminUri:${symbol_pound}{null}}")
    private String[] adminUri;

    @Value("${symbol_dollar}{auth.role.apiUri:${symbol_pound}{null}}")
    private String[] apiUri;

    @Value("${symbol_dollar}{auth.role.privateUri:${symbol_pound}{null}}")
    private String[] privateUri;

    @Value("${symbol_dollar}{auth.role.publicUri:${symbol_pound}{null}}")
    private String[] publicUri;

    @Value("${symbol_dollar}{auth.login.uri}")
    private String loginUri;

    @Value("${symbol_dollar}{auth.login.oauth2.successUri}")
    private String oauth2LoginSuccessUri;

    @Value("${symbol_dollar}{auth.login.oauth2.failureUri}")
    private String oauth2LoginFailureUri;

    @Value("${symbol_dollar}{auth.login.portal.successUri}")
    private String portalLoginSuccessUri;

    @Value("${symbol_dollar}{auth.login.portal.failureUri}")
    private String portalLoginFailureUri;

    @Value("${symbol_dollar}{auth.login.oauth2.baseUri:${symbol_pound}{T(org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter).DEFAULT_AUTHORIZATION_REQUEST_BASE_URI}}")
    private String authorizationRequestBaseUri;

    @Value("${symbol_dollar}{auth.logout.uri}")
    private String logoutUri;
    @Value("${symbol_dollar}{auth.logout.successUri}")
    private String logoutSuccessUri;

    private void configPrivatePath(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        if (ArrayUtils.isNotEmpty(privateUri)) {
            registry.requestMatchers(privateUri).denyAll();
        }
    }

    private void configPublicPath(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        if (ArrayUtils.isNotEmpty(publicUri)) {
            registry.requestMatchers(publicUri).permitAll().requestMatchers(oauth2LoginFailureUri).permitAll();
        }
    }

    private void configAdminPath(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        if (ArrayUtils.isNotEmpty(adminUri)) {
            registry.requestMatchers(adminUri).hasRole(AuthorityUtils.adminRole());
        }
    }

    private void configApiPath(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        if (ArrayUtils.isNotEmpty(apiUri)) {
            // registry.antMatchers(apiUri).hasRole(AuthorityUtils.clientRole());
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ClientRegistrationRepository clientRegistrationRepository,
                                           OAuth2AuthorizationRequestCustomizer oAuth2AuthorizationRequestCustomizer,
                                           OAuth2AuthorizedClientService oAuth2ClientService,
                                           UserDetailsManager userDetailsManager,
                                           ApiTokenAuthenticationProvider apiTokenAuthenticationProvider
                                           ) throws Exception {
        http.authorizeHttpRequests(registry -> {
            configPrivatePath(registry);
            configPublicPath(registry);
            configAdminPath(registry);
            configApiPath(registry);
            registry.anyRequest().authenticated();
        });

        http.logout(logout ->
                logout.logoutUrl(logoutUri)
                        .logoutSuccessUrl(logoutSuccessUri)
                        .permitAll()
        );

        http.formLogin(portal ->
                portal.loginPage(loginUri)
                        .permitAll()
                        .defaultSuccessUrl(portalLoginSuccessUri)
                        .failureUrl(portalLoginFailureUri)
        );

        SysUserDetailsManager sysUserDetailsManager = (SysUserDetailsManager) userDetailsManager;

        DefaultOAuth2AuthorizationRequestResolver oAuth2RequestResolver =  new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, authorizationRequestBaseUri);
        oAuth2RequestResolver.setAuthorizationRequestCustomizer(oAuth2AuthorizationRequestCustomizer);

        OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler = new OAuth2AuthenticationSuccessHandler(
                oAuth2ClientService, sysUserDetailsManager, oAuth2AuthorizationRequestCustomizer);
        oAuth2SuccessHandler.setDefaultTargetUrl(oauth2LoginSuccessUri);

        OAuth2AuthenticationFailureHandler oAuth2FailureHandler = new OAuth2AuthenticationFailureHandler(
                oAuth2AuthorizationRequestCustomizer);
        oAuth2FailureHandler.setDefaultFailureUrl(oauth2LoginFailureUri);

        http.oauth2Login(oauth2 ->
                oauth2.loginPage(loginUri)
                        .permitAll()
                        .authorizationEndpoint(endpoint ->
                                endpoint.authorizationRequestResolver(oAuth2RequestResolver))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
        );

        if (ArrayUtils.isNotEmpty(apiUri)) {
            http.addFilterAfter(
                    apiAuthenticationFilter(apiTokenAuthenticationProvider), UsernamePasswordAuthenticationFilter.class);
            http.csrf(csrfConf -> csrfConf.ignoringRequestMatchers(apiUri));
        }

        http.userDetailsService(userDetailsManager);

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder(EncryptionService encryptionService) {
        return new SysUserPasswordEncoder(encryptionService);
    }

    @Bean
    public UserDetailsManager users(SysUserService userService,
                                    SysBindService bindService,
                                    SysAuthorityService authorityService,
                                    PasswordEncoder passwordEncoder) {
        return new SysUserDetailsManager(userService, bindService, authorityService, passwordEncoder);
    }

    private RequestMatcher toRequestMatcher(String[] paths) {
        return new OrRequestMatcher(Arrays.stream(paths)
                .map(AntPathRequestMatcher::new)
                .map(RequestMatcher.class::cast)
                .toList());
    }

    private AuthenticationFilter apiAuthenticationFilter(ApiTokenAuthenticationProvider provider) {
        AuthenticationFilter filter = new AuthenticationFilter(provider::authenticate, provider);
        filter.setRequestMatcher(toRequestMatcher(apiUri));
        filter.setSuccessHandler((request, response, authentication) -> {});
        return filter;
    }
}
