#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service.common.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import ${package}.service.common.EncryptionService;
import ${package}.service.util.EncryptionUtils;

@Service
@Slf4j
@SuppressWarnings("unused")
public class EncryptionServiceImpl implements EncryptionService {
    @Value("${symbol_dollar}{auth.aes.key}")
    private String aesKey;
    private EncryptionUtils encryptionUtils;

    @PostConstruct
    public void init()
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        encryptionUtils = new EncryptionUtils(aesKey);
    }

    @Override
    public String encrypt(String plainText) {
        return encryptionUtils.encrypt(plainText);
    }

    @Override
    public String decrypt(String cipherText) {
        return encryptionUtils.decrypt(cipherText);
    }
}
