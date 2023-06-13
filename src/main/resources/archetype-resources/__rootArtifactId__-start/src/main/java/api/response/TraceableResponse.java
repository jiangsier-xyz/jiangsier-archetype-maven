#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.response;

import ${package}.util.TraceUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Traceable response.")
@Data
public class TraceableResponse {
    @Schema(description = "Trace identifier.")
    private String traceId;

    public TraceableResponse() {
        this.traceId = TraceUtils.getTraceId();
    }
}
