package com.pgmate.pay.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author juseop
 *
 */
public class CardUtil {

	/**
	 * 
	 */
	public CardUtil() {
		// TODO Auto-generated constructor stub
	}
	
	
	public static String hash(String txt){
		String SHA = ""; 
		try{
			MessageDigest sh = MessageDigest.getInstance("SHA-256");
			sh.update(txt.getBytes());
			byte byteData[] = sh.digest();
			StringBuilder sb = new StringBuilder(); 
			for(int i = 0 ; i < byteData.length ; i++){
				sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
			}
			SHA = sb.toString();				
		}catch(NoSuchAlgorithmException e){
			e.printStackTrace(); 
			SHA = ""; 
		}
		return SHA;
	}

}
