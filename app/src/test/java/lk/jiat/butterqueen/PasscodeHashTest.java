package lk.jiat.butterqueen;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import lk.jiat.butterqueen.data.PasscodeHash;

public class PasscodeHashTest {
    @Test
    public void saltedVerifierAcceptsOnlyTheChosenSixDigitCode() {
        char[] code = "284619".toCharArray();
        String[] verifier = PasscodeHash.create(code);

        assertTrue(PasscodeHash.matches(code, verifier[0], verifier[1]));
        assertFalse(PasscodeHash.matches("284610".toCharArray(), verifier[0], verifier[1]));
        assertFalse(PasscodeHash.valid("12345".toCharArray()));
        assertFalse(PasscodeHash.valid("abc123".toCharArray()));
    }
}
