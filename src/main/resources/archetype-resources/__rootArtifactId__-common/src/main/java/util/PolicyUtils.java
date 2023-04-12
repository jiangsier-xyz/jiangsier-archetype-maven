#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util;

import java.util.Set;

@SuppressWarnings({"unused"})
public class PolicyUtils {
    public static String all() {
        return null;
    }

    public static String fromAuthorities(Set<String> authorities) {
        return String.join(",", authorities);
    }
}
