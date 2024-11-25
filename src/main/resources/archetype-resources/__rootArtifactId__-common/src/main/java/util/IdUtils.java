#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util;

import java.util.UUID;

public class IdUtils {
    public static String newId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}