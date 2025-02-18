#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.access.auth.user;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.provisioning.UserDetailsManager;
import ${package}.service.account.SysAuthorityService;
import ${package}.service.account.SysBindService;
import ${package}.service.account.SysUserService;
import ${package}.model.User;

import java.net.URL;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class SysUserDetailsManager implements UserDetailsManager {
    private final SysUserService userService;
    private final SysBindService bindService;
    private final SysAuthorityService authorityService;
    private final PasswordEncoder passwordEncoder;

    public SysUserDetailsManager(SysUserService userService,
                                 SysBindService bindService,
                                 SysAuthorityService authorityService,
                                 PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.bindService = bindService;
        this.authorityService = authorityService;
        this.passwordEncoder = passwordEncoder;
    }

    private User mapUser(UserDetails userDetails, User user) {
        Date now = new Date();

        if (user == null) {
            user = new User();
        }

        return user.withUsername(userDetails.getUsername())
                .withPassword(userDetails.getPassword())
                .withEnabled(userDetails.isEnabled() ? (byte) 1 : (byte) 0)
                .withLocked(userDetails.isAccountNonLocked() ? (byte) 0 : (byte) 1)
                .withExpiresAt(userDetails.isAccountNonExpired() ? null : now)
                .withPasswordExpiresAt(userDetails.isCredentialsNonExpired() ? null : now);
    }

    @Override
    public void createUser(UserDetails userDetails) {
        if (userDetails == null) {
            return;
        }

        User user;

        if (userDetails instanceof SysUserDetails sysUserDetails) {
            user = sysUserDetails;
        } else {
            user = mapUser(userDetails, null);
        }

        if (userService.userExists(user.getUsername())) {
            return;
        }

        userService.createUser(user);
    }

    @Override
    public void updateUser(UserDetails userDetails) {
        if (userDetails == null) {
            return;
        }

        User user;
        if (userDetails instanceof SysUserDetails sysUserDetails) {
            user = sysUserDetails;
            if (!userExists(user.getUsername())) {
                return;
            }
        } else {
            user = ((SysUserDetails) loadUserByUsername(userDetails.getUsername()));
            if (user == null) {
                return;
            }
            user = mapUser(userDetails, user);
        }

        userService.updateUser(user);
    }

    @Override
    public void deleteUser(String username) {
        userService.deleteUser(username);
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        SysUserDetails sysUser = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(SysUserDetails.class::isInstance)
                .map(SysUserDetails.class::cast)
                .orElse(null);

        if (sysUser == null) {
            return;
        }

        User user = userService.loadUserByUsernameAndPassword(sysUser.getUsername(), oldPassword);
        if (user == null) {
            return;
        }

        user.setPassword(newPassword);
        userService.updateUser(user);
    }

    @Override
    public boolean userExists(String username) {
        return userService.userExists(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.loadUserByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("No user for " + username);
        }
        return SysUserDetails.builder()
                .fromUser(user)
                .withAuthorityService(authorityService)
                .withPasswordEncoder(passwordEncoder)
                .build();
    }

    public SysUserDetails loadUserByUsernameAndPlatform(String username, String platform) {
        User user = userService.loadUserByUsernameAndPlatform(username, platform);
        if (user == null) {
            return null;
        }
        return SysUserDetails.builder()
                .fromUser(user)
                .withAuthorityService(authorityService)
                .withPasswordEncoder(passwordEncoder)
                .build();
    }

    private String getUsername(OAuth2User oAuth2User, String platform) {
        String username = oAuth2User.getName();
        if ("aliyun".equalsIgnoreCase(platform)) {
            String uid = oAuth2User.getAttribute("uid");
            if (StringUtils.isNotBlank(uid)) {
                username = uid;
            }
        } else if ("github".equalsIgnoreCase(platform)) {
            String login = oAuth2User.getAttribute("login");
            if (StringUtils.isNotBlank(login)) {
                username = login;
            }
        }

        return username + "@" + platform;
    }

    private String generatePassword() {
        return UUID.randomUUID().toString();
    }

    private String getNickname(OAuth2User oAuth2User, String platform) {
        String nickname = oAuth2User.getName();
        if ("aliyun".equalsIgnoreCase(platform)) {
            String uid = oAuth2User.getAttribute("uid");
            String aid = oAuth2User.getAttribute("aid");
            if (uid == null || aid == null) {
                return nickname;
            }
            if (StringUtils.equals(uid, aid)) {
                // primary account. the 'name' attribute is always 'root'.
                String loginName = oAuth2User.getAttribute("login_name");
                nickname = StringUtils.isBlank(loginName) ? nickname : loginName;
            } else {
                // ram account. the 'name' attribute makes sense.
                String name = oAuth2User.getAttribute("name");
                nickname = StringUtils.isBlank(name) ? nickname : name;
            }
        } else if ("github".equalsIgnoreCase(platform)) {
            String login = oAuth2User.getAttribute("login");
            if (StringUtils.isNotBlank(login)) {
                nickname = login;
            }
        } else {
            String name = oAuth2User.getAttribute(StandardClaimNames.NICKNAME);
            if (StringUtils.isBlank(name)) {
                name = oAuth2User.getAttribute(StandardClaimNames.NAME);
            }
            if (StringUtils.isNotBlank(name)) {
                nickname = name;
            }
        }
        return nickname;
    }

    private String getSub(OAuth2User oAuth2User, String platform) {
        String sub = oAuth2User.getAttribute(IdTokenClaimNames.SUB);
        if (StringUtils.isBlank(sub)) {
            sub = getUsername(oAuth2User, platform);
        }
        return sub;
    }

    private Collection<String> getAud(OAuth2User oAuth2User, OAuth2AuthorizedClient oAuth2Client, String platform) {
        Collection<String> audSet = oAuth2User.getAttribute(IdTokenClaimNames.AUD);
        if (CollectionUtils.isEmpty(audSet)) {
            audSet = Collections.singleton(oAuth2Client.getClientRegistration().getClientId());
        }
        return audSet;
    }

    private String getProfile(OAuth2User oAuth2User, String platform) {
        String profile = oAuth2User.getAttribute(StandardClaimNames.PROFILE);
        if (StringUtils.isBlank(profile)) {
            if ("github".equalsIgnoreCase(platform)) {
                profile = oAuth2User.getAttribute("html_url");
            } else {
                profile = oAuth2User.getAttribute("url");
            }
        }
        return profile;
    }

    private Date toDate(String dateStr, String zoneInfo) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }

        if (StringUtils.isBlank(zoneInfo)) {
            zoneInfo = "UTC";
        }

        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
            return Date.from(date.atStartOfDay(ZoneId.of(zoneInfo)).toInstant());
        } catch (DateTimeException e) {
            log.warn("Wrong date format: {}", dateStr);
        }
        return null;
    }

    private Date timestampToDate(Object timestamp) {
        if (timestamp == null) {
            return null;
        }

        try {
            return switch (timestamp) {
                case String timestampStr -> Date.from(Instant.parse(timestampStr));
                case Long timestampLong -> new Date(timestampLong);
                default -> throw new IllegalArgumentException("Unknown timestamp class: " + timestamp.getClass());
            };
        } catch (IllegalArgumentException | DateTimeParseException e) {
            log.warn("Unknown timestamp {}", timestamp, e);
            return null;
        }
    }

    private Byte toByte(Boolean bool) {
        return bool != null && bool ? (byte)1 : (byte)0;
    }

    public SysUserDetails createSysUserFromOAuth2UserIfNecessary(OAuth2User oAuth2User,
                                                                 OAuth2AuthorizedClient oAuth2Client) {
        String platform = oAuth2Client.getClientRegistration().getRegistrationId();
        String sub = getSub(oAuth2User, platform);
        User user = Optional.ofNullable(bindService.getUserIdByPlatformAndSub(platform, sub))
                .filter(StringUtils::isNotBlank)
                .map(userService::loadUserByUserId)
                .orElse(null);
        Set<String> authorities = oAuth2User.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        if (user == null) {
            String zoneInfo = oAuth2User.getAttribute(StandardClaimNames.ZONEINFO);

            user = new User()
                    .withUsername(getUsername(oAuth2User, platform))
                    .withPassword(generatePassword())
                    .withNickname(getNickname(oAuth2User, platform))
                    .withGivenName(oAuth2User.getAttribute(StandardClaimNames.GIVEN_NAME))
                    .withMiddleName(oAuth2User.getAttribute(StandardClaimNames.MIDDLE_NAME))
                    .withFamilyName(oAuth2User.getAttribute(StandardClaimNames.FAMILY_NAME))
                    .withPreferredUsername(oAuth2User.getAttribute(StandardClaimNames.PREFERRED_USERNAME))
                    .withProfile(getProfile(oAuth2User, platform))
                    .withPicture(oAuth2User.getAttribute(StandardClaimNames.PICTURE))
                    .withWebsite(oAuth2User.getAttribute(StandardClaimNames.WEBSITE))
                    .withEmail(oAuth2User.getAttribute(StandardClaimNames.EMAIL))
                    .withEmailVerified(toByte(oAuth2User.getAttribute(StandardClaimNames.EMAIL_VERIFIED)))
                    .withGender(oAuth2User.getAttribute(StandardClaimNames.GENDER))
                    .withBirthdate(toDate(oAuth2User.getAttribute(StandardClaimNames.BIRTHDATE), zoneInfo))
                    .withZoneinfo(zoneInfo)
                    .withLocale(oAuth2User.getAttribute(StandardClaimNames.LOCALE))
                    .withPhoneNumber(oAuth2User.getAttribute(StandardClaimNames.PHONE_NUMBER))
                    .withPhoneNumberVerified(toByte(oAuth2User.getAttribute(StandardClaimNames.PHONE_NUMBER_VERIFIED)))
                    .withAddress(oAuth2User.getAttribute(StandardClaimNames.ADDRESS))
                    .withUpdatedAt(timestampToDate(oAuth2User.getAttribute(StandardClaimNames.UPDATED_AT)))
                    .withPlatform(platform)
                    .withEnabled((byte) 1)
                    .withLocked((byte) 0)
                    .withExpiresAt(null)
                    .withPasswordExpiresAt(null);

            userService.createUser(user);
        } else {
            authorities.addAll(authorityService.listAuthorities(user));
        }

        if (CollectionUtils.isNotEmpty(authorities)) {
            authorityService.updateAuthorities(user, authorities);
        }

        return SysUserDetails.builder()
                .fromUser(user)
                .withAuthorityService(authorityService)
                .withPasswordEncoder(passwordEncoder)
                .build();
    }

    public boolean bindSysUserAndOAuth2UserIfNecessary(SysUserDetails sysUser,
                                                       OAuth2User oAuth2User,
                                                       OAuth2AuthorizedClient oAuth2Client) {
        String platform = oAuth2Client.getClientRegistration().getRegistrationId();
        OAuth2RefreshToken refreshToken = oAuth2Client.getRefreshToken();
        if (refreshToken == null && bindService.isBound(sysUser, platform)) {
            return false;
        }
        String refreshTokenValue = Optional.ofNullable(refreshToken)
                .map(AbstractOAuth2Token::getTokenValue)
                .orElse(null);
        Date issuedAt = Optional.ofNullable(refreshToken)
                .map(AbstractOAuth2Token::getIssuedAt)
                .map(Date::from)
                .orElseGet(Date::new);
        Date expiresAt = Optional.ofNullable(refreshToken)
                .map(AbstractOAuth2Token::getExpiresAt)
                .map(Date::from)
                .orElse(null);
        String sub = getSub(oAuth2User, platform);
        URL iss = oAuth2User.getAttribute(IdTokenClaimNames.ISS);
        Collection<String> aud = getAud(oAuth2User, oAuth2Client, platform);
        return bindService.bind(sysUser, platform, sub, iss, aud, refreshTokenValue, issuedAt, expiresAt);
    }
}