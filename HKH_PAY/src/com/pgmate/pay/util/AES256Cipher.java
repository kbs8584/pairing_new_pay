package com.pgmate.pay.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AES256Cipher {
	private static Logger logger 						= LoggerFactory.getLogger(com.pgmate.pay.util.AES256Cipher.class); 
	/* AES-256 양방향 */
	private static volatile AES256Cipher INSTANCE;

    final static String secretKey 							= "d7d02c92cb930b661f107cb92690fc83"; // 32bit
    
    public static String IVKEY 							= "d7d02c92cb930b661f107cb92690fc83"; 
    
  //static String IV = ""; // 16bit
    static String IV = secretKey.substring(0, 16);

    public static AES256Cipher getInstance() {
        if (INSTANCE == null) {
            synchronized (AES256Cipher.class) {
                if (INSTANCE == null)
                    INSTANCE = new AES256Cipher();
            }
        }
        return INSTANCE;
    }

    private AES256Cipher() {
        IV = secretKey.substring(0, 16);
    }

    // 암호화
    public static String AES_Encode(String str)
            throws java.io.UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] keyData = secretKey.getBytes();

        SecretKey secureKey = new SecretKeySpec(keyData, "AES");

        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, secureKey, new IvParameterSpec(IV.getBytes()));

        byte[] encrypted = c.doFinal(str.getBytes("UTF-8"));
        String enStr = new String(Base64.encodeBase64(encrypted));

        return enStr;
    }

    // 복호화
    public static String AES_Decode(String str)
            throws java.io.UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] keyData = secretKey.getBytes();
        SecretKey secureKey = new SecretKeySpec(keyData, "AES");
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, secureKey, new IvParameterSpec(IV.getBytes("UTF-8")));

        byte[] byteStr = Base64.decodeBase64(str.getBytes());

        return new String(c.doFinal(byteStr), "UTF-8");
    }
    
	public static String toDecrypt(String CRYPTOKEY, String originalMsg) {
		// logger.debug("response : [{}]",originalMsg);
		logger.info("AES256 toDecrypt | CRYPTOKEY : " + CRYPTOKEY);
		logger.info("AES256 toDecrypt | originalMsg : " + originalMsg);
		String AESMode = "AES/CBC/PKCS5Padding";
		String SecetKeyAlgorithmString = "AES";

		IvParameterSpec ivspec = new IvParameterSpec(hexToByteArray(IVKEY));
		SecretKey keySpec = new SecretKeySpec(hexToByteArray(CRYPTOKEY), SecetKeyAlgorithmString);
		try {
			Cipher cipher = Cipher.getInstance(AESMode);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivspec);
			byte[] decrypted = cipher.doFinal(Base64.decodeBase64(originalMsg));
			String retValue = new String(decrypted);

			return retValue;
		} catch (Exception e) {
			logger.info("Decrypt error : {}", e.getMessage());
			return "";
		}
	}
	
	private static byte[] hexToByteArray(String hex) {
		if (hex == null || hex.length() == 0) {
			return null;
		}

		byte[] ba = new byte[hex.length() / 2];
		for (int i = 0; i < ba.length; i++) {
			ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return ba;
	}
	
	// 가상계좌
	public static byte[] ivBytes = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

    public static String AES_Encode(String str, String key, byte[] ivBytes)	throws java.io.UnsupportedEncodingException, NoSuchAlgorithmException,
                        NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,	IllegalBlockSizeException, BadPaddingException {
        byte[] textBytes = str.getBytes("UTF-8");
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        SecretKeySpec newKey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        Cipher cipher = null;
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
        return Base64.encodeBase64String(cipher.doFinal(textBytes));
    }
    
    public static String AES_Encode(String str, String key)	throws java.io.UnsupportedEncodingException, NoSuchAlgorithmException,
    					NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,	IllegalBlockSizeException, BadPaddingException {
    	return AES_Encode(str, key, ivBytes);
    }

    public static String AES_Decode(String str, String key, byte[] ivBytes)	throws java.io.UnsupportedEncodingException, NoSuchAlgorithmException,
                        NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] textBytes =  Base64.decodeBase64(str.getBytes());
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        SecretKeySpec newKey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);
        return new String(cipher.doFinal(textBytes), "UTF-8");
    }

    public static String AES_Decode(String str, String key)	throws java.io.UnsupportedEncodingException, NoSuchAlgorithmException,
                        NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,	IllegalBlockSizeException, BadPaddingException {
    return AES_Decode(str, key, ivBytes);
    }
}
