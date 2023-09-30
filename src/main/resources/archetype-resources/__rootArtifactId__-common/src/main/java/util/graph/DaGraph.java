#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util.graph;

/**
 * Directed Acyclic Graph
 */
@SuppressWarnings("unused")
public class DaGraph<T> extends DiGraph<T>{
    protected boolean allowCycle() {
        return false;
    }
}
