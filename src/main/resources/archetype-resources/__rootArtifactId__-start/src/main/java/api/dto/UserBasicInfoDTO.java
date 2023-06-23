#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.dto;

import ${package}.api.util.CommonUtils;
import ${package}.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Objects;

@Schema(description = "User basic information.")
@Data
public class UserBasicInfoDTO extends TraceableDTO{
    @Schema(description = "Username")
    private String username;
    @Schema(description = "Nickname")
    private String nickname;
    @Schema(description = "Picture/Atavar")
    private String picture;

    public static UserBasicInfoDTO fromUser(User user) {
        if (Objects.isNull(user)) {
            return null;
        }
        return CommonUtils.convert(user, UserBasicInfoDTO.class);
    }
}
