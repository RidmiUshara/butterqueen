package lk.jiat.butterqueen.data;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Salted, deliberately slow verifier for a six-digit app-lock code. */
public final class PasscodeHash {
    private static final int ITERATIONS = 210_000;
    private static final int HASH_BITS = 256;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private PasscodeHash() { }

    public static boolean valid(char[] code) {
        if (code == null || code.length != 6) return false;
        for (char digit : code) if (digit < '0' || digit > '9') return false;
        return true;
    }

    public static String[] create(char[] code) {
        if (!valid(code)) throw new IllegalArgumentException("Six digits required");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derive(code, salt);
        try {
            return new String[]{hex(salt), hex(hash)};
        } finally {
            Arrays.fill(hash, (byte) 0);
            Arrays.fill(salt, (byte) 0);
        }
    }

    public static boolean matches(char[] code, String saltHex, String hashHex) {
        if (!valid(code)) return false;
        try {
            byte[] salt = fromHex(saltHex);
            byte[] expected = fromHex(hashHex);
            if (salt.length != 16 || expected.length != 32) return false;
            byte[] actual = derive(code, salt);
            boolean match = MessageDigest.isEqual(actual, expected);
            Arrays.fill(actual, (byte) 0);
            Arrays.fill(expected, (byte) 0);
            return match;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private static byte[] derive(char[] code, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(code, salt, ITERATIONS, HASH_BITS);
        try {
            // Android supports this PBKDF2 PRF across the app's API 24+ range.
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
        } catch (Exception error) {
            throw new IllegalStateException("Passcode verifier unavailable", error);
        } finally {
            spec.clearPassword();
        }
    }

    private static String hex(byte[] bytes) {
        char[] output = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            output[i * 2] = HEX[(bytes[i] >>> 4) & 15];
            output[i * 2 + 1] = HEX[bytes[i] & 15];
        }
        return new String(output);
    }

    private static byte[] fromHex(String text) {
        if (text == null || (text.length() & 1) != 0) throw new IllegalArgumentException();
        byte[] result = new byte[text.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int high = Character.digit(text.charAt(i * 2), 16);
            int low = Character.digit(text.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) throw new IllegalArgumentException();
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }
}
