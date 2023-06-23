#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service.account.impl;

import ${package}.mapper.UserDynamicSqlSupport;
import ${package}.mapper.UserMapper;
import ${package}.model.User;
import ${package}.service.account.SysUserService;
import ${package}.service.cache.MiddlePeriodCache;
import ${package}.service.cache.MiddlePeriodCacheEvict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

@Service
@SuppressWarnings("unused")
public class SysUserServiceImpl implements SysUserService {
    private final static String CACHE_KEY = "'SysUserServiceImpl_' + ${symbol_pound}p0";
    private final static String CACHE_KEY_FROM_USER = CACHE_KEY + ".username";

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean createUser(User user) {
        Date now = new Date(System.currentTimeMillis());
        int rows = userMapper.insertSelective(user
                .withGmtCreate(now)
                .withGmtModified(now)
                .withUserId(UUID.randomUUID().toString().replaceAll("-", "")));
        return rows > 0;
    }

    @Override
    @MiddlePeriodCacheEvict(keyBy = CACHE_KEY_FROM_USER)
    public boolean updateUser(User user) {
        Date now = new Date(System.currentTimeMillis());
        int rows = userMapper.updateByPrimaryKeySelective(user.withGmtModified(now));
        return rows > 0;
    }

    @Override
    @MiddlePeriodCacheEvict(keyBy = CACHE_KEY)
    public boolean deleteUser(String username) {
        int rows = userMapper.delete(c -> c.where(UserDynamicSqlSupport.username, isEqualTo(username)));
        return rows > 0;
    }

    @Override
    public boolean userExists(String username) {
        return Objects.nonNull(loadUserByUsername(username));
    }

    @Override
    @MiddlePeriodCache(keyBy = CACHE_KEY)
    public User loadUserByUserId(String userId) {
        return userMapper.selectOne(c -> c.where(UserDynamicSqlSupport.userId, isEqualTo(userId)))
                .orElse(null);
    }

    @Override
    @MiddlePeriodCache(keyBy = CACHE_KEY)
    public User loadUserByUsername(String username) {
        return userMapper.selectOne(c -> c.where(UserDynamicSqlSupport.username, isEqualTo(username)))
                .orElse(null);
    }

    @Override
    public User loadUserByUsernameAndPassword(String username, String password) {
        return userMapper.selectOne(c -> c.where(UserDynamicSqlSupport.username, isEqualTo(username))
                        .and(UserDynamicSqlSupport.password, isEqualTo(password)))
                .orElse(null);
    }

    @Override
    public User loadUserByUsernameAndPlatform(String username, String platform) {
        return userMapper.selectOne(c -> c.where(UserDynamicSqlSupport.username, isEqualTo(username))
                        .and(UserDynamicSqlSupport.platform, isEqualTo(platform)))
                .orElse(null);
    }

    @Override
    public List<User> listUsers(int limit, int offset) {
        return userMapper.select(c -> {
            if (limit > 0) {
                c.limit(limit);
            }
            if (offset > 0) {
                c.offset(offset);
            }
            return c;
        });
    }
}
