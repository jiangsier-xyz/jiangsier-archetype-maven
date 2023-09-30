#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.util.graph;

import org.apache.commons.collections4.Transformer;

public class VertexTransformer<T> implements Transformer<DiGraph.Vertex<T>, T> {
    @Override
    public T transform(DiGraph.Vertex<T> v) {
        return v.getData();
    }
}
