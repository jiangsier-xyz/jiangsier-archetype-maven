#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.dto;

import ${package}.util.TraceUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TraceableDTO {
    @Schema(description = "Trace identifier.")
    private String requestId;

    public TraceableDTO() {
        this.requestId = TraceUtils.getTraceId();
    }
}
