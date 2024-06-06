#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.access.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Transient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ${package}.access.auth.user.SysUserDetails;
import ${package}.util.AuthorityUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Transient
public class ApiTokenAuthenticationToken extends AbstractAuthenticationToken {
    private final Set<String> tokens;

    private SysUserDetails sysUser;

    private static List<GrantedAuthority> getAuthorities(SysUserDetails sysUser) {
        List<GrantedAuthority> authorities = new LinkedList<>(sysUser.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(AuthorityUtils.CLIENT));
        return authorities;
    }

    public ApiTokenAuthenticationToken(SysUserDetails sysUser, Set<String> tokens) {
        super(getAuthorities(sysUser));
        this.sysUser = sysUser;
        this.tokens = tokens;
        setAuthenticated(true);
    }

    public ApiTokenAuthenticationToken(Set<String> tokens) {
        super(null);
        this.tokens = tokens;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return tokens;
    }

    @Override
    public Object getPrincipal() {
        return sysUser;
    }
}
