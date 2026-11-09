package com.vasav.springtalentengine.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
class ApiKeyCipher {

    private final TextEncryptor encryptor;

    ApiKeyCipher(@Value("${app.byok.master-key}") String masterKey, @Value("${app.byok.salt}") String salt) {
        this.encryptor = Encryptors.delux(masterKey, salt);
    }

    String encrypt(String apiKey) {
        return encryptor.encrypt(apiKey);
    }

    String decrypt(String encrypted) {
        return encryptor.decrypt(encrypted);
    }
}
