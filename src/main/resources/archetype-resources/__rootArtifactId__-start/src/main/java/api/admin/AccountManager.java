#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.api.admin;

import ${package}.api.dto.UserBasicInfoDTO;
import ${package}.api.dto.UserDetailsDTO;
import ${package}.model.User;
import ${package}.service.account.SysApiTokenService;
import ${package}.service.account.SysAuthorityService;
import ${package}.service.account.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Tag(name = "account manager (for admin)")
@RequestMapping("/api/admin")
@ResponseBody
@Validated
@SuppressWarnings("unused")
public class AccountManager {
    @Autowired
    private SysUserService userService;

    @Autowired
    private SysAuthorityService authorityService;

    @Autowired
    private SysApiTokenService apiTokenService;

    @Operation(
            summary = "List users.",
            description = "Get user list."
    )
    @GetMapping("/accounts")
    public List<UserBasicInfoDTO> list() {
        return list(0, 0);
    }

    @Operation(
            summary = "List users.",
            description = "Get user list. Returns a maximum of pageSize user informations."
    )
    @GetMapping("/accounts/{pageSize}")
    public List<UserBasicInfoDTO> list(
            @Parameter(description = "Maximum size") @PathVariable("pageSize") Integer pageSize) {
        return list(pageSize, 0);
    }

    @Operation(
            summary = "List users.",
            description = "Get user list page by page. Returns the pageNum page, and a maximum of pageSize user informations."
    )
    @GetMapping("/accounts/{pageSize}/{pageNum}")
    public List<UserBasicInfoDTO> list(
            @Parameter(description = "Maximum size") @PathVariable("pageSize") Integer pageSize,
            @Parameter(description = "Current page") @PathVariable("pageNum") Integer pageNum) {
        int limit = Objects.nonNull(pageSize) && pageSize > 0 ? pageSize : 0;
        int offset = Objects.nonNull(pageSize) && Objects.nonNull(pageNum) && pageSize * pageNum > 0
                ? pageSize * pageNum
                : 0;
        return userService.listUsers(limit, offset)
                .stream()
                .map(UserBasicInfoDTO::fromUser)
                .toList();
    }

    @Operation(
            summary = "Get user details.",
            description = "Return the user details, fields refer to OpenID Connect (OIDC) [standard claims](https://openid.net/specs/openid-connect-core-1_0.html${symbol_pound}Claims)."
    )
    @GetMapping("/account/{username}")
    public UserDetailsDTO details(
            @Parameter(description = "Username") @PathVariable("username") @NotBlank
            String username) {
        User user = userService.loadUserByUsername(username);
        return UserDetailsDTO.fromUser(user);
    }

    @Operation(
            summary = "Create a user.",
            description = "Create a user by passing the user details."
    )
    @PostMapping("/account")
    public Boolean create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User details")
            @RequestBody
            @NotNull
            UserDetailsDTO userDetails) {
        return userService.createUser(userDetails.toUser());
    }

    @Operation(
            summary = "Update a user.",
            description = "Update a user details."
    )
    @PutMapping("/account")
    public Boolean update(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User details")
            @RequestBody
            @NotNull
            UserDetailsDTO userDetails) {
        return userService.updateUser(userDetails.toUser());
    }

    @Operation(
            summary = "Delete a user.",
            description = "Delete a user by username."
    )
    @DeleteMapping("/account/{username}")
    public Boolean delete(
            @Parameter(description = "Username") @PathVariable("username") @NotBlank
            String username) {
        return userService.deleteUser(username);
    }

    @Operation(
            summary = "List authorities.",
            description = "List a user's authorities."
    )
    @GetMapping("/authority/{username}")
    public Set<String> listAuthorities(
            @Parameter(description = "Username") @PathVariable("username") @NotBlank
            String username) {
        User user = userService.loadUserByUsername(username);
        if (Objects.isNull(user)) {
            return null;
        }
        return authorityService.listAuthorities(user);
    }

    @Operation(
            summary = "Update authorities.",
            description = "Update a user's authorities."
    )
    @PutMapping("/authority/{username}")
    public Boolean updateAuthorities(
            @Parameter(description = "Username") @PathVariable("username") @NotBlank
            String username,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Authorities") @RequestBody @NotEmpty
            Set<String> authorities) {
        User user = userService.loadUserByUsername(username);
        if (Objects.isNull(user)) {
            return null;
        }
        return authorityService.updateAuthorities(user, authorities);
    }

    @Operation(
            summary = "Get api tokens.",
            description = "Get a user's api tokens."
    )
    @GetMapping("/token/{username}")
    public List<String> listTokens(
            @Parameter(description = "Username") @PathVariable("username") @NotBlank
            String username) {
        User user = userService.loadUserByUsername(username);
        if (Objects.isNull(user)) {
            return null;
        }
        return apiTokenService.listTokens(user);
    }

    @Operation(
            summary = "Create an api token.",
            description = "Create an api token for the user，Valid for the duration of seconds."
    )
    @PostMapping("/token/{username}/{duration}")
    public String createToken(
            @Parameter(description = "Username") @PathVariable("username") @NotBlank
            String username,
            @Parameter(description = "Validity of seconds.") @PathVariable("duration") @NotNull @PositiveOrZero
            Long duration) {
        User user = userService.loadUserByUsername(username);
        if (Objects.isNull(user)) {
            return null;
        }
        return apiTokenService.createToken(user, Duration.ofSeconds(duration));
    }

    @Operation(
            summary = "Delete an api token.",
            description = "Delete an api token."
    )
    @DeleteMapping("/token/{token}")
    public Boolean deleteToken(
            @Parameter(description = "Api token") @PathVariable("token") @NotBlank
            String token) {
        return apiTokenService.deleteToken(token);
    }

    @Operation(
            summary = "Disable an api token",
            description = "Disable an api token."
    )
    @PutMapping("/token/{token}")
    public Boolean disableToken(
            @Parameter(description = "Api token") @PathVariable("token") @NotBlank
            String token) {
        return apiTokenService.disableToken(token);
    }

    @Operation(
            summary = "Get the user corresponding to the api token.",
            description = "Get the user details corresponding to the api token."
    )
    @GetMapping("/tokenBy/{token}")
    public UserDetailsDTO tokenBy(
            @Parameter(description = "Api token") @PathVariable("token") @NotBlank
            String token) {
        return UserDetailsDTO.fromUser(apiTokenService.getUser(token));
    }
}
