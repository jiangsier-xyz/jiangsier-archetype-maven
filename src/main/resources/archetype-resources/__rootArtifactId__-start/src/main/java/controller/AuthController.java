#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import ${package}.service.account.SysApiTokenService;
import ${package}.service.account.SysAuthorityService;
import ${package}.service.account.SysUserService;
import ${package}.model.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Controller
@Validated
@SuppressWarnings("unused")
public class AuthController {
    @Autowired
    private InMemoryClientRegistrationRepository clientRegistrationRepository;

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

    @GetMapping("/login")
    public String login(Model model) {
        List<String> registrations = new LinkedList<>();
        clientRegistrationRepository.iterator().forEachRemaining(
                r -> registrations.add(r.getRegistrationId()));
        model.addAttribute("registrations", registrations);
        return "login";
    }

    @RequestMapping("/login/oauth2/success")
    public String oAuth2Success(HttpServletRequest request) {
        return "redirect:/swagger-ui/index.html";
    }

    @RequestMapping("/login/oauth2/failure")
    public String oAuth2Failure() {
        return "redirect:/login?error";
    }

    @RequestMapping("/login/portal/success")
    public String portalSuccess() {
        return "redirect:/swagger-ui/index.html";
    }

    @RequestMapping("/login/portal/failure")
    public String portalFailure() {
        return "redirect:/login?error";
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
