#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service.config;

import java.util.Map;
import java.util.Properties;

public interface RuntimeConfig {
    String get();
    Properties getProperties();
    Map<String, Object> getMap();
    default <T> T getObject() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
