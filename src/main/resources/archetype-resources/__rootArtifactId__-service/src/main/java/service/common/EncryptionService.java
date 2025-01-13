#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service.common;

@SuppressWarnings("unused")
public interface EncryptionService {
    String encrypt(String plainText);
    String decrypt(String cipherText);
}
