#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.util;

import ${package}.model.User;
import ${package}.service.account.SysUserService;
import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;

@Component
@SuppressWarnings("unused")
public class AuthUtils implements ApplicationContextAware {
    private static SysUserService userService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        userService = applicationContext.getBean(SysUserService.class);
    }

    @NonNull
    public static User currentUser() {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        if (authenticated == null || authenticated instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Object principal = authenticated.getPrincipal();
        User user = null;
        if (principal instanceof User sysUser) {
            user = sysUser;
        } else if (principal instanceof String userName) {
            user = userService.loadUserByUsername(userName);
        }
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    public static String userIdToName(String userId) {
        return Optional.ofNullable(userId)
                .map(userService::loadUserByUserId)
                .map(User::getUsername)
                .orElse(null);
    }

    public static String userNameToId(String userName) {
        return Optional.ofNullable(userName)
                .map(userService::loadUserByUsername)
                .map(User::getUserId)
                .orElse(null);
    }
}
