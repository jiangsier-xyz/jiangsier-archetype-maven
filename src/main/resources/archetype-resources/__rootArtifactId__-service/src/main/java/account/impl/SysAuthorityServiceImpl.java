#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.account.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ${package}.mapper.AuthorityDynamicSqlSupport;
import ${package}.mapper.AuthorityMapper;
import ${package}.account.SysAuthorityService;
import ${package}.model.Authority;
import ${package}.model.User;
import ${package}.util.AuthorityUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

@Service
@SuppressWarnings("unused")
public class SysAuthorityServiceImpl implements SysAuthorityService {
    @Autowired
    private AuthorityMapper authorityMapper;

    @Override
    public Set<String> listAuthorities(@NonNull User user) {
        Set<String> authorities = new HashSet<>();
        authorities.add(AuthorityUtils.USER);
        authorityMapper.select(c -> c.where(AuthorityDynamicSqlSupport.userId, isEqualTo(user.getUserId())))
                .stream()
                .map(Authority::getScope)
                .forEach(authorities::add);

        return authorities;
    }

    @Override
    @Transactional
    public boolean updateAuthorities(User user, Set<String> authorities) {
        Date now = new Date(System.currentTimeMillis());
        authorityMapper.delete(c -> c.where(AuthorityDynamicSqlSupport.userId, isEqualTo(user.getUserId())));
        for (String authority : authorities) {
            if (AuthorityUtils.USER.equalsIgnoreCase(authority)) {
                continue;
            }
            Authority row = new Authority()
                    .withUserId(user.getUserId())
                    .withGmtCreate(now)
                    .withGmtModified(now)
                    .withScope(authority);
             authorityMapper.insertSelective(row);
        }
        return true;
    }
}
