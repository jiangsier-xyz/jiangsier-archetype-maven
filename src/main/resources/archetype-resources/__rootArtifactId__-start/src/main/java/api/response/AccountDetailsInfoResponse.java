#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Schema(description = "Account details.")
@Data
public class AccountDetailsInfoResponse extends TraceableResponse {
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
}
