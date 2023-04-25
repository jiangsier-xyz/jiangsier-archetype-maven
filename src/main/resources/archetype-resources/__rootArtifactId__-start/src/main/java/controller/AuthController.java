#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import ${package}.account.SysApiTokenService;
import ${package}.account.SysAuthorityService;
import ${package}.account.SysUserService;
import ${package}.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.Duration;
import java.util.*;

@Controller
@Validated
@SuppressWarnings("unused")
public class AuthController {
    @Autowired
    private OAuth2AuthorizedClientService oAuth2ClientService;

    @Autowired
    private SysUserService userService;

    @Autowired
    private SysApiTokenService apiTokenService;

    @Autowired
    private SysAuthorityService authorityService;

    private User authenticationToUser(Authentication authenticated) {
        Object principal = authenticated.getPrincipal();
        if (principal instanceof User user) {
            return user;
        } else if (principal instanceof String userName) {
            return userService.loadUserByUsername(userName);
        }
        return null;
    }

    @RequestMapping("/login/oauth2/success")
    public String oAuth2Success(HttpServletRequest request) {
        Map<String, Object> userInfo = new HashMap<>(1);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getDetails() instanceof OAuth2AuthenticationToken oAuth2Auth) {
            String clientRegistrationId = oAuth2Auth.getAuthorizedClientRegistrationId();
            String principalName = oAuth2Auth.getName();
            OAuth2AuthorizedClient oAuth2Client =
                    oAuth2ClientService.loadAuthorizedClient(clientRegistrationId, principalName);
            OAuth2User oAuth2User = oAuth2Auth.getPrincipal();
            userInfo.put("oAuth2Client", oAuth2Client);
            request.setAttribute("userExtraInfo", userInfo);
        }

        return "forward:/public/test/info/user";
    }

    @RequestMapping("/login/oauth2/failure")
    public String oAuth2Failure() {
        return "forward:/public/test/info/user";
    }

    @RequestMapping("/login/portal/success")
    public String portalSuccess() {
        return "forward:/public/test/info/user";
    }

    @RequestMapping("/login/portal/failure")
    public String portalFailure() {
        return "forward:/public/test/info/request";
    }

    @GetMapping("/token/create")
    @ResponseBody
    public String createToken(Authentication authenticated) {
        User user = authenticationToUser(authenticated);
        if (Objects.isNull(user)) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }
        return apiTokenService.createToken(user);
    }

    @GetMapping("/token/create/{duration}")
    @ResponseBody
    public String createToken(Authentication authenticated,
                              @PathVariable("duration") @NotNull @Positive Long duration) {
        User user = authenticationToUser(authenticated);
        if (Objects.isNull(user)) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }
        return apiTokenService.createToken(user, Duration.ofSeconds(duration));
    }

    @GetMapping("/token/delete/{token}")
    @ResponseBody
    public String deleteToken(@PathVariable("token") @NotBlank String token) {
        if (!apiTokenService.deleteToken(token)) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        }
        return token;
    }

    @GetMapping("/token/disable/{token}")
    @ResponseBody
    public String disableToken(@PathVariable("token") @NotNull String token) {
        if (!apiTokenService.disableToken(token)) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        }
        return token;
    }

    @GetMapping("/token/list")
    @ResponseBody
    public List<String> listTokens(Authentication authenticated) {
        User user = authenticationToUser(authenticated);
        if (Objects.isNull(user)) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }
        return apiTokenService.listTokens(user);
    }
}
