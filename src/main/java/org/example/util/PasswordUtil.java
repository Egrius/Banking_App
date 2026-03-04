package org.example.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordUtil {
    private static final BCrypt.Hasher HASHER = BCrypt.withDefaults();
    private static final BCrypt.Verifyer VERIFYER = BCrypt.verifyer();

    public static String hash(String rawPassword) {
        return HASHER.hashToString(12, rawPassword.toCharArray());
    }

    public static boolean verify(String rawPassword, String hashedPassword) {
        BCrypt.Result result = VERIFYER.verify(rawPassword.toCharArray(), hashedPassword);
        return result.verified;
    }
}
