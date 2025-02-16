#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.dto;

import ${package}.api.util.AuthUtils;
import ${package}.api.util.CommonUtils;
import ${package}.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.Objects;

@Schema(description = "User details.")
@Data
public class UserDetailsDTO extends TraceableDTO {
    private String username;
    private String nickname;
    private String givenName;
    private String middleName;
    private String familyName;
    private String preferredUsername;
    private String profile;
    private String picture;
    private String website;
    private String email;
    private String gender;
    private Date birthdate;
    private String zoneinfo;
    private String locale;
    private String phoneNumber;
    private Date updatedAt;
    private String platform;
    private Byte enabled;
    private Byte locked;
    private Date expiresAt;
    private Date passwordExpiresAt;
    private String address;

    public static UserDetailsDTO from(User user) {
        if (user == null) {
            return null;
        }
        return CommonUtils.convert(user, UserDetailsDTO.class);
    }

    public User toUser() {
        User user = CommonUtils.convert(this, User.class);
        user.setUserId(AuthUtils.userNameToId(getUsername()));
        return user;
    }
}
