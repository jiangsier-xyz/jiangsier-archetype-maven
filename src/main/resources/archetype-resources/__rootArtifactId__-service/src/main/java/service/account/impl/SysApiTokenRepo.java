#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service.account.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ${package}.service.cache.LongPeriodCache;
import ${package}.mapper.ApiTokenDynamicSqlSupport;
import ${package}.mapper.ApiTokenMapper;
import ${package}.mapper.UserDynamicSqlSupport;
import ${package}.mapper.UserMapper;
import ${package}.model.ApiToken;
import ${package}.model.User;

import java.util.Date;
import java.util.List;

import static org.mybatis.dynamic.sql.SqlBuilder.*;

@Component
@SuppressWarnings("unused")
public class SysApiTokenRepo {
    static final String TOKEN_CACHE_KEY = "'SysApiTokenRepo_token_' + ${symbol_pound}p0";
    static final String USER_CACHE_KEY = "'SysApiTokenRepo_user_' + ${symbol_pound}p0";

    @Autowired
    private ApiTokenMapper apiTokenMapper;

    @Autowired
    private UserMapper userMapper;

    @LongPeriodCache(keyBy = TOKEN_CACHE_KEY)
    public ApiToken getApiToken(String token) {
        return apiTokenMapper.selectOne(c -> c.where(ApiTokenDynamicSqlSupport.token, isEqualTo(token)))
                .orElse(null);
    }

    public List<ApiToken> listApiTokens(String userId) {
        Date now = new Date(System.currentTimeMillis());
        return apiTokenMapper.select(c -> c.where(ApiTokenDynamicSqlSupport.userId, isEqualTo(userId))
                        .and(ApiTokenDynamicSqlSupport.issuedAt, isLessThanOrEqualTo(now))
                        .and(ApiTokenDynamicSqlSupport.expiresAt, isGreaterThanOrEqualTo(now)));
    }

    @LongPeriodCache(keyBy = USER_CACHE_KEY)
    public User getUser(String token) {
        String userId = apiTokenMapper.selectOne(c -> c.where(ApiTokenDynamicSqlSupport.token, isEqualTo(token)))
                .map(ApiToken::getUserId)
                .orElse(null);
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        return userMapper.selectOne(c -> c.where(UserDynamicSqlSupport.userId, isEqualTo(userId)))
                .orElse(null);
    }
}
