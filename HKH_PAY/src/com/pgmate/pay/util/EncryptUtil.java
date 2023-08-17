package com.pgmate.pay.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptUtil {

    public static final String CHARSET_UTF8 = "UTF-8";
    public static final String DEFAULT_CHARSET = CHARSET_UTF8;

    private static final String AES = "AES";
    public static final String AES_CBC_ALGORITHM = AES + "/CBC/PKCS5Padding";

    public static String sha256(String valueStr) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        digest.update(valueStr.getBytes(DEFAULT_CHARSET));

        return bytesToHex(digest.digest());
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public static String generateKey(final int keyLen) throws NoSuchAlgorithmException {

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keyLen);
        SecretKey secretKey = keyGen.generateKey();
        byte[] encoded = secretKey.getEncoded();
        return DatatypeConverter.printHexBinary(encoded).toLowerCase();
    }

    public static String aes256Encrypt(String keyStr, String ivStr, CharSequence plainText) {
        if (plainText == null || plainText.length() == 0) {
            return null;
        }
        try {
            Cipher c = Cipher.getInstance(AES_CBC_ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, getSecretKey(keyStr), new IvParameterSpec(Hex.decode(ivStr)));

            byte[] encrypted = c.doFinal(Utf8.encode(plainText));
            return new String(Hex.encode(encrypted));
        } catch (Exception e) {
            e.printStackTrace();
            // 필요에 따라 예외처리 수행
            return null;
        }
    }

    public static String aes256Decrypt(String keyStr, String ivStr, CharSequence encryptedText) {
        if (encryptedText == null || encryptedText.length() == 0) {
            return null;
        }
        try {
            Cipher c = Cipher.getInstance(AES_CBC_ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, getSecretKey(keyStr), new IvParameterSpec(Hex.decode(ivStr)));

            byte[] decrypted = c.doFinal(Hex.decode(encryptedText));
            return Utf8.decode(decrypted);
        } catch (Exception e) {
            e.printStackTrace();
            // 필요에 따라 예외처리 수행
            return null;
        }
    }

    private static SecretKey getSecretKey(String keyStr) {
        byte[] keyData = Hex.decode(keyStr);
        return new SecretKeySpec(keyData, AES);
    }

    public static final class Hex {

        private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f'};

        public static char[] encode(byte[] bytes) {
            final int nBytes = bytes.length;
            char[] result = new char[2 * nBytes];

            int j = 0;
            for (int i = 0; i < nBytes; i++) {
                // Char for top 4 bits
                result[j++] = HEX[(0xF0 & bytes[i]) >>> 4];
                // Bottom 4
                result[j++] = HEX[(0x0F & bytes[i])];
            }

            return result;
        }

        public static byte[] decode(CharSequence s) {
            int nChars = s.length();

            if (nChars % 2 != 0) {
                throw new IllegalArgumentException(
                        "Hex-encoded string must have an even number of characters");
            }

            byte[] result = new byte[nChars / 2];

            for (int i = 0; i < nChars; i += 2) {
                int msb = Character.digit(s.charAt(i), 16);
                int lsb = Character.digit(s.charAt(i + 1), 16);

                if (msb < 0 || lsb < 0) {
                    throw new IllegalArgumentException(
                            "Detected a Non-hex character at " + (i + 1) + " or " + (i + 2) + " position");
                }
                result[i / 2] = (byte) ((msb << 4) | lsb);
            }
            return result;
        }
    }

    public static final class Utf8 {
        private static final Charset CHARSET = Charset.forName("UTF-8");

        /**
         * Get the bytes of the String in UTF-8 encoded form.
         */
        public static byte[] encode(CharSequence string) {
            try {
                ByteBuffer bytes = CHARSET.newEncoder().encode(CharBuffer.wrap(string));
                byte[] bytesCopy = new byte[bytes.limit()];
                System.arraycopy(bytes.array(), 0, bytesCopy, 0, bytes.limit());

                return bytesCopy;
            } catch (CharacterCodingException e) {
                throw new IllegalArgumentException("Encoding failed", e);
            }
        }

        /**
         * Decode the bytes in UTF-8 form into a String.
         */
        public static String decode(byte[] bytes) {
            try {
                return CHARSET.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException e) {
                throw new IllegalArgumentException("Decoding failed", e);
            }
        }
    }
}
