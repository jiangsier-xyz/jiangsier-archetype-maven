#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.member.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ${package}.cache.MiddlePeriodCache;
import ${package}.cache.MiddlePeriodCacheEvict;
import ${package}.mapper.UserDynamicSqlSupport;
import ${package}.mapper.UserMapper;
import ${package}.member.SysUserService;
import ${package}.model.User;

import java.util.Date;
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
}
