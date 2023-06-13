#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api;

import ${package}.account.SysUserService;
import ${package}.api.response.AccountBasicInfoResponse;
import ${package}.api.response.AccountDetailsInfoResponse;
import ${package}.api.util.AuthUtils;
import ${package}.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.constraints.NotBlank;

@Tag(name = "account")
@Service
@Validated
@SuppressWarnings("unused")
@RequestMapping("/api/account")
@ResponseBody
public class Account {
    @Autowired
    private SysUserService userService;

    @Operation(
            summary = "Get account basic information.",
            description = "Return the basic information of the account, including: username, nickname, picture/avatar link."
    )
    @GetMapping("/basic/{username}")
    private AccountBasicInfoResponse basic(
            @Schema(description = "Username") @PathVariable("username") @NotBlank String username) {
        User user = userService.loadUserByUsername(username);
        return AuthUtils.toResponse(user, AccountBasicInfoResponse.class);
    }

    @Operation(
            summary = "Get current account details.",
            description = "Return the current account details, fields refer to OpenID Connect (OIDC) [standard claims](https://openid.net/specs/openid-connect-core-1_0.html${symbol_pound}Claims)。")
    @GetMapping("/details")
    private AccountDetailsInfoResponse details() {
        User user = AuthUtils.currentUser();
        return AuthUtils.toResponse(user, AccountDetailsInfoResponse.class);
    }
}
