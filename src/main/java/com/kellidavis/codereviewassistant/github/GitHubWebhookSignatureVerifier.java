package com.kellidavis.codereviewassistant.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class GitHubWebhookSignatureVerifier {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private final byte[] secret;

    public GitHubWebhookSignatureVerifier(@Value("${github.webhook.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isValid(byte[] payload, String providedSignature) {
        if(secret.length == 0){
            return false;
        }

        if(providedSignature == null || !providedSignature.startsWith(SIGNATURE_PREFIX)){
            return false;
        }

        try{
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            SecretKeySpec secretKey = new SecretKeySpec(secret, HMAC_ALGORITHM);

            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload);

            String expectedSignature = SIGNATURE_PREFIX + HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),
                    providedSignature.getBytes(StandardCharsets.UTF_8));
        }catch(GeneralSecurityException e){
            throw new IllegalStateException("Unable to verify GitHub webhook signature", e);
        }
    }
}
