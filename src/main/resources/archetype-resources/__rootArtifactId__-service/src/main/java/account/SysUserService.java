#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.account;

import ${package}.model.User;

@SuppressWarnings("UnusedReturnValue")
public interface SysUserService {
    boolean createUser(User user);
    boolean updateUser(User user);
    boolean deleteUser(String username);
    boolean userExists(String username);
    User loadUserByUsername(String username);
    User loadUserByUsernameAndPassword(String username, String password);
    User loadUserByUsernameAndPlatform(String username, String platform);
}
