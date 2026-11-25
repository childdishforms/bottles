package ru.practice.water;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class Passwords {
    private static final int ITERATIONS = 600_000;
    private static byte[] derive(char[] password, byte[] salt) {
        var spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        } finally {
            spec.clearPassword();
        }
    }
    static String encode(char[] password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        var encoder = Base64.getEncoder();
        return encoder.encodeToString(salt) + "." + encoder.encodeToString(derive(password, salt));
    }
    static boolean matches(char[] password, String stored) {
        String[] parts = stored.split("\\.", 2);
        var decoder = Base64.getDecoder();
        return parts.length == 2 && MessageDigest.isEqual(derive(password, decoder.decode(parts[0])), decoder.decode(parts[1]));
    }
}
