#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import ${package}.member.SysAuthorityService;
import ${package}.member.SysUserService;
import ${package}.model.User;

import java.util.Objects;
import java.util.Set;

@Tag(name = "member")
@Service
@Validated
@SuppressWarnings("unused")
@RequestMapping("/api/member")
public class Member {
    @Autowired
    private SysUserService userService;

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

    @Operation(
            summary = "list the authorities",
            description = "List the authorities of the **current user**.")
    @GetMapping("/authorities")
    @ResponseBody
    public Set<String> listAuthorities(Authentication authenticated) {
        User user = authenticationToUser(authenticated);
        if (Objects.isNull(user)) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        }
        return authorityService.listAuthorities(user);
    }
}
