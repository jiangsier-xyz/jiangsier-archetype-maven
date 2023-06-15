#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.util;

import ${package}.account.SysUserService;
import ${package}.model.User;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AuthUtils implements ApplicationContextAware {
    private static SysUserService userService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        userService = applicationContext.getBean(SysUserService.class);
    }

    public static User currentUser() {
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.isNull(authenticated)) {
            return null;
        }
        Object principal = authenticated.getPrincipal();
        if (principal instanceof User user) {
            return user;
        } else if (principal instanceof String userName) {
            return userService.loadUserByUsername(userName);
        } else {
            return null;
        }
    }
}
