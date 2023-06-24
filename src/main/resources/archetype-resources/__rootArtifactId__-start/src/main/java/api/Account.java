#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api;

import ${package}.api.dto.UserBasicInfoDTO;
import ${package}.api.dto.UserDetailsDTO;
import ${package}.api.util.AuthUtils;
import ${package}.model.User;
import ${package}.service.account.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Service
@Tag(name = "account")
@RequestMapping("/api/account")
@ResponseBody
@Validated
@SuppressWarnings("unused")
public class Account {
    @Autowired
    private SysUserService userService;

    @Operation(
            summary = "Get basic user information.",
            description = "Return the basic user information of the account, including: username, nickname, picture/avatar link."
    )
    @GetMapping("/basic/{username}")
    public UserBasicInfoDTO basic(
            @Parameter(description = "Username") @PathVariable("username") @NotBlank
            String username) {
        User user = userService.loadUserByUsername(username);
        return UserBasicInfoDTO.fromUser(user);
    }

    @Operation(
            summary = "Get current user details.",
            description = "Return the current user details of the account, fields refer to OpenID Connect (OIDC) [standard claims](https://openid.net/specs/openid-connect-core-1_0.html${symbol_pound}Claims).")
    @GetMapping("/details")
    public UserDetailsDTO details() {
        User user = AuthUtils.currentUser();
        return UserDetailsDTO.fromUser(user);
    }

    @Operation(
            summary = "Update current user details.",
            description = "Update current user details of the account."
    )
    @PostMapping("/update")
    public Boolean update(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User details")
            @RequestBody
            @NotNull
            UserDetailsDTO userDetails) {
        User user =userDetails.toUser();
        user.setUserId(AuthUtils.currentUser().getUserId());
        return userService.updateUser(user);
    }
}
