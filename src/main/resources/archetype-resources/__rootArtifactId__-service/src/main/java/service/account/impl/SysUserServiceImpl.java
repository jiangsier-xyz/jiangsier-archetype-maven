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
import ${package}.util.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

@Service
@SuppressWarnings("unused")
public class SysUserServiceImpl implements SysUserService {
    private static final String CACHE_KEY = "'SysUserServiceImpl_' + ${symbol_pound}p0";
    private static final String CACHE_KEY_FROM_USER = CACHE_KEY + ".username";

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean createUser(User user) {
        Date now = new Date(System.currentTimeMillis());
        int rows = userMapper.insertSelective(user
                .withGmtCreate(now)
                .withGmtModified(now)
                .withUserId(IdUtils.newId()));

        if (rows == 0) {
            user.withUserId(null)
                    .withGmtCreate(null)
                    .withGmtModified(null);
            return false;
        }

        if (user.getPlatform() == null) {
            user.setPlatform("system");
        }
        if (user.getEmailVerified() == null) {
            user.setEmailVerified((byte)0);
        }
        if (user.getPhoneNumberVerified() == null) {
            user.setPhoneNumberVerified((byte)0);
        }
        if (user.getEnabled() == null) {
            user.setEnabled((byte)1);
        }
        if (user.getLocked() == null) {
            user.setLocked((byte)0);
        }
        return true;
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
        return loadUserByUsername(username) != null;
    }

    @Override
    @MiddlePeriodCache(keyBy = CACHE_KEY)
    public User loadUserByUserId(String userId) {
        return userMapper.selectOne(c ->
                        c.where(UserDynamicSqlSupport.userId, isEqualTo(userId))
                                .and(UserDynamicSqlSupport.enabled, isEqualTo((byte)1)))
                .orElse(null);
    }

    @Override
    @MiddlePeriodCache(keyBy = CACHE_KEY)
    public User loadUserByUsername(String username) {
        return userMapper.selectOne(c ->
                        c.where(UserDynamicSqlSupport.username, isEqualTo(username))
                                .and(UserDynamicSqlSupport.enabled, isEqualTo((byte)1)))
                .orElse(null);
    }

    @Override
    public User loadUserByUsernameAndPassword(String username, String password) {
        return userMapper.selectOne(c -> c.where(UserDynamicSqlSupport.username, isEqualTo(username))
                        .and(UserDynamicSqlSupport.password, isEqualTo(password))
                        .and(UserDynamicSqlSupport.enabled, isEqualTo((byte)1)))
                .orElse(null);
    }

    @Override
    public User loadUserByUsernameAndPlatform(String username, String platform) {
        return userMapper.selectOne(c -> c.where(UserDynamicSqlSupport.username, isEqualTo(username))
                        .and(UserDynamicSqlSupport.platform, isEqualTo(platform))
                        .and(UserDynamicSqlSupport.enabled, isEqualTo((byte)1)))
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
