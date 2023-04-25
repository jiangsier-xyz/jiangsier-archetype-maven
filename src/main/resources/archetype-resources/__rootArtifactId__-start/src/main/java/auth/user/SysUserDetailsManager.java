#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.auth.user;

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
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.provisioning.UserDetailsManager;
import ${package}.account.SysAuthorityService;
import ${package}.account.SysBindService;
import ${package}.account.SysUserService;
import ${package}.model.User;

import java.net.URL;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SysUserDetailsManager implements UserDetailsManager {
    private static final Logger logger = LoggerFactory.getLogger(SysUserDetailsManager.class);

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
        Date now = new Date(System.currentTimeMillis());

        if (Objects.isNull(user)) {
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
        if (Objects.isNull(userDetails)) {
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
        if (Objects.isNull(userDetails)) {
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
            if (Objects.isNull(user)) {
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

        if (Objects.isNull(sysUser)) {
            return;
        }

        User user = userService.loadUserByUsernameAndPassword(sysUser.getUsername(), oldPassword);
        if (Objects.isNull(user)) {
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
        if (Objects.isNull(user)) {
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
        if (Objects.isNull(user)) {
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
            username = StringUtils.isBlank(uid) ? oAuth2User.getName() : uid;
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
            if (Objects.isNull(uid) || Objects.isNull(aid)) {
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
        } else {
            String name = oAuth2User.getAttribute(StandardClaimNames.NICKNAME);
            if (StringUtils.isBlank(name)) {
                name = oAuth2User.getAttribute(StandardClaimNames.NAME);
            }
            nickname = StringUtils.isBlank(name) ? nickname : name;
        }
        return nickname;
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
            logger.warn("Wrong date format: {}", dateStr);
        }
        return null;
    }

    private Date timestampToDate(Long timestamp) {
        if (Objects.isNull(timestamp)) {
            return null;
        }
        return new Date(timestamp);
    }

    private Byte toByte(Boolean bool) {
        return Objects.nonNull(bool) && bool ? (byte)1 : (byte)0;
    }

    public SysUserDetails createSysUserFromOAuth2UserIfNecessary(OAuth2User oAuth2User,
                                                                 OAuth2AuthorizedClient oAuth2Client) {
        String platform = oAuth2Client.getClientRegistration().getRegistrationId();
        String username = getUsername(oAuth2User, platform);
        User user = userService.loadUserByUsernameAndPlatform(username, platform);
        if (Objects.isNull(user)) {
            String zoneInfo = oAuth2User.getAttribute(StandardClaimNames.ZONEINFO);

            user = new User()
                    .withUsername(username)
                    .withPassword(generatePassword())
                    .withNickname(getNickname(oAuth2User, platform))
                    .withGivenName(oAuth2User.getAttribute(StandardClaimNames.GIVEN_NAME))
                    .withMiddleName(oAuth2User.getAttribute(StandardClaimNames.MIDDLE_NAME))
                    .withFamilyName(oAuth2User.getAttribute(StandardClaimNames.FAMILY_NAME))
                    .withPreferredUsername(oAuth2User.getAttribute(StandardClaimNames.PREFERRED_USERNAME))
                    .withProfile(oAuth2User.getAttribute(StandardClaimNames.PROFILE))
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
        }

        Set<String> authorities = oAuth2User.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

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
        OAuth2RefreshToken refreshToken = oAuth2Client.getRefreshToken();
        if (Objects.isNull(refreshToken)) {
            return false;
        }
        String platform = oAuth2Client.getClientRegistration().getRegistrationId();
        String sub = oAuth2User.getAttribute(IdTokenClaimNames.SUB);
        URL iss = oAuth2User.getAttribute(IdTokenClaimNames.ISS);
        Collection<String> aud = oAuth2User.getAttribute(IdTokenClaimNames.AUD);
        String refreshTokenValue = refreshToken.getTokenValue();
        Date issuedAt = Optional.ofNullable(refreshToken.getIssuedAt()).map(Date::from).orElse(null);
        Date expiresAt = Optional.ofNullable(refreshToken.getExpiresAt()).map(Date::from).orElse(null);

        return bindService.bind(sysUser, platform, sub, iss, aud, refreshTokenValue, issuedAt, expiresAt);
    }
}
