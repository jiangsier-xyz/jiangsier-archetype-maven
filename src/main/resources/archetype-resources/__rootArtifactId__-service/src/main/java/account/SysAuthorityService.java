#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.account;

import org.springframework.lang.NonNull;
import ${package}.model.User;

import java.util.Set;

@SuppressWarnings("UnusedReturnValue")
public interface SysAuthorityService {
    Set<String> listAuthorities(@NonNull User user);

    boolean updateAuthorities(User user, Set<String> authorities);
}
