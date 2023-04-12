#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
public class AuthorityUtils {
    private static final String ROLE_PREFIX = "ROLE_";

    private static final String SCOPE_PREFIX = "SCOPE_";

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";

    public static final String CLIENT = "ROLE_CLIENT";

    public static String toRole(String authority) {
        if (StringUtils.isNotBlank(authority) && authority.startsWith(ROLE_PREFIX)) {
            return authority.substring(ROLE_PREFIX.length());
        }
        return authority;
    }

    public static String toScope(String authority) {
        if (StringUtils.isNotBlank(authority) && authority.startsWith(SCOPE_PREFIX)) {
            return authority.substring(SCOPE_PREFIX.length());
        }
        return authority;
    }

    public static String adminRole() {
        return toRole(ADMIN);
    }

    public static String userRole() {
        return toRole(USER);
    }

    public static String clientRole() {
        return toRole(CLIENT);
    }
}
