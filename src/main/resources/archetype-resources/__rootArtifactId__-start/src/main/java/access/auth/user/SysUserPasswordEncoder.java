#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.access.auth.user;

import org.springframework.security.crypto.password.PasswordEncoder;

import ${package}.service.common.EncryptionService;

public record SysUserPasswordEncoder(EncryptionService encryptionService) implements PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        return encryptionService.encrypt(rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}
