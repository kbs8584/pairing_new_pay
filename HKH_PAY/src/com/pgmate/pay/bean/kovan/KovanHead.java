package com.pgmate.pay.bean.kovan;

/**
 * @author Administrator
 *
 */
public class KovanHead {

	public String iso 			= "ISO";		// TEXT 개시문자 / 3 / ISO
	public String host 			= "023400052";	// HOST 가맹점 / 9 / 023400052 
	public String type 			= "0200";		// 전문TYPE / 4 / 0200
	public String bitmap 		= "";			// Primary Bitmap / 16 /3238040128608A02
	public String trxType 		= "";			// 거래구분코드 / 6 / 000030
	public String amount 		= "";	 		// 거래금액 / 12 / 1000원일 경우 : 000000001000 
	public String reqDate 		= ""; 			// 전문전송일시 / 10 / MMDDhhmmss
	public String trackId 		= "";			// 전문추적번호 / 6 / 일련번호
	public String trxTime 		= "";			// 거래개시시간 / 6 / Hhmmss
	public String trxDay 		= "";			// 거래개시일 / 6 / YYMMDD
	public String eci 			= "";			// 거래입력유형 / 3 / "7"+ECI value(2)
	public String companyCd 	= "";			// 취급기관코드 / 8 / "069"+단말기번호 4번째자리부터 5자리 
	public String trackII 		= "";			// TRACK II DATA  / 39 / "37"+카드번호+"="+유효기간(4)
	public String trxId 		= "";			// 거래고유번호 / 12 /  
	public String mchtId 		= "";			// 가맹점번호 / 15 / SPACE
	public String pairingTmnId	= "";			// 1-10 CATID, 11-20 사업자번호
	public String pairingBRN		= "1208815955";	// 1-10 CATID, 11-20 사업자번호     이후 공백 20 
	public String currency 		= ""; 			// 통화코드 / 3 / 원화 : 410, 달러 : 840
	
	
	
	
	public KovanHead() {
		// TODO Auto-generated constructor stub
	}

}
