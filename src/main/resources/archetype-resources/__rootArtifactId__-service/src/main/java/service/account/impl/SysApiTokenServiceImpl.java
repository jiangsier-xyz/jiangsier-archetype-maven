#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service.account.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import ${package}.service.account.SysApiTokenService;
import ${package}.service.cache.LongPeriodCacheEvict;
import ${package}.mapper.ApiTokenDynamicSqlSupport;
import ${package}.mapper.ApiTokenMapper;
import ${package}.service.account.ApiTokenType;
import ${package}.model.ApiToken;
import ${package}.model.User;

import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

@Service
@SuppressWarnings("unused")
public class SysApiTokenServiceImpl implements SysApiTokenService {
    @Value("${symbol_dollar}{auth.token.prefix:${symbol_pound}{null}}")
    private String prefix;

    @Value("${symbol_dollar}{auth.token.maxCountPerUser:${symbol_pound}{null}}")
    private Integer maxCount;

    @Autowired
    private SysApiTokenRepo apiTokenRepo;

    @Autowired
    private ApiTokenMapper apiTokenMapper;

    @Override
    public String createToken(User user, ApiTokenType type, String policy, TemporalAmount duration) {
        if (Objects.nonNull(maxCount) && listTokens(user).size() >= maxCount) {
            return null;
        }
        Date now = new Date(System.currentTimeMillis());
        String token = Objects.isNull(prefix) ? "" : prefix;
        token += UUID.randomUUID().toString().replaceAll("-", "");
        int rows = apiTokenMapper.insertSelective(new ApiToken()
                .withGmtCreate(now)
                .withGmtModified(now)
                .withIssuedAt(now)
                .withExpiresAt(Objects.nonNull(duration) ? Date.from(now.toInstant().plus(duration)) : null)
                .withPolicy(policy)
                .withType(type.getType())
                .withUserId(user.getUserId())
                .withToken(token));
        if (rows > 0) {
            return token;
        } else {
            return null;
        }
    }

    @Override
    public List<String> listTokens(User user) {
        return apiTokenRepo.listApiTokens(user.getUserId())
                .stream()
                .filter(this::isEnabled)
                .map(ApiToken::getToken)
                .toList();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "longPeriod", key = SysApiTokenRepo.TOKEN_CACHE_KEY),
            @CacheEvict(cacheNames = "longPeriod", key = SysApiTokenRepo.USER_CACHE_KEY)
    })
    public boolean deleteToken(String token) {
        int rows = apiTokenMapper.delete(c -> c.where(ApiTokenDynamicSqlSupport.token, isEqualTo(token)));
        return rows > 0;
    }

    @Override
    @LongPeriodCacheEvict(keyBy = SysApiTokenRepo.TOKEN_CACHE_KEY)
    public boolean disableToken(String token) {
        int rows = 0;
        ApiToken apiToken = apiTokenRepo.getApiToken(token);
        if (Objects.nonNull(apiToken)) {
            apiToken.setExpiresAt(apiToken.getIssuedAt());
            rows = apiTokenMapper.updateByPrimaryKey(apiToken);
        }
        return rows > 0;
    }

    private boolean isEnabled(ApiToken apiToken) {
        Date now = new Date(System.currentTimeMillis());
        return Objects.nonNull(apiToken) &&
                apiToken.getIssuedAt().before(now) &&
                (apiToken.getExpiresAt() == null || apiToken.getExpiresAt().after(now));
    }

    @Override
    public boolean isEnabled(String token) {
        return isEnabled(apiTokenRepo.getApiToken(token));
    }

    @Override
    public User getUser(String token) {
        return isEnabled(token) ? apiTokenRepo.getUser(token) : null;
    }
}
