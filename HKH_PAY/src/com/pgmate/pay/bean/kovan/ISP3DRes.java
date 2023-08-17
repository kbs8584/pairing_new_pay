package com.pgmate.pay.bean.kovan;

import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 *
 */
public class ISP3DRes {


	

	public String type 		= "";	//	0100
	public String bitmap 		= "";	//	전문 번호 / 4 / 322000012AC10108
	public String cardNo 		= "";	//	카드번호 / 16 / 카드번호(8)+0000+카드번호(4)
	public String trxType 		= "";	//	Processing Code / 6 / 011000
	public String amount 		= "";		//	승인금액 / 12 / 
	public String reqDate 		= "";	// 전문 전송 일시 / 10 / MMDDhhmmss
	public String trackId 		= "";		// 전문 추적 번호 / 6 / 승인 요청시의 번호
	public String installment  = "";	// 할부기간 / 2 / 03~12
	public String companyCd 	= "";	// 취급 기관코드 / 5 / 승인 요청시의 코드
	public String trxId 		= "";		// TID / 40 / 승인 요청시의 ID
	public String currency 	= "";	// 통화코드 / 3 / WON "410", USD "840"
	public String vpTrxId 		= "";		// 거래고유 번호 / 12 / 승인요청시의 코드
	public String resultCd 	= "";	// 응답코드 / 4 / 00** 카드사 응답, 01** KVP 거절
	public String pairingTmnId 	= "";	// Terminal-ID / 16 / 
	public String mchtId 		= "";	// 가맹점 번호 / 15 / 
	public String cardCd 		= "";	// 카드코드 / 22 / KVP카드 코드 앞2자리 길이
	public String authCd 		= "";	// 승인번호 / 20 / KVP승인번호(*********** + 카드사승인번호(2~8))
	public String cardResultCd = "";	// 카드사 응답코드 / 4 / 카드사의 응답코드 
	public String point 		= "";	//	가용포인트 / 2 / 포인트 정보 있는경우
	
	
	public ISP3DRes() {
		
	}
	
	public ISP3DRes(String trx) {
		this(trx.getBytes());
		
	}
	public ISP3DRes(byte[] trx) {
		
		type 		= CommonUtil.toString(trx,4,4).trim();
		bitmap 		= CommonUtil.toString(trx,8,16).trim();
		cardNo 		= CommonUtil.toString(trx,24,16).trim();
		trxType 	= CommonUtil.toString(trx,40,6).trim();
		amount 		= CommonUtil.toString(trx,46,12).trim();
		reqDate 	= CommonUtil.toString(trx,58,10).trim();
		trackId 	= CommonUtil.toString(trx,68,6).trim();
		installment = CommonUtil.toString(trx,74,2).trim();
		companyCd 	= CommonUtil.toString(trx,76,5).trim();
		trxId 		= CommonUtil.toString(trx,81,40).trim();
		currency 	= CommonUtil.toString(trx,121,3).trim();
		trxId 		= CommonUtil.toString(trx,124,12).trim();
		resultCd 	= CommonUtil.toString(trx,136,4).trim();
		pairingTmnId 	= CommonUtil.toString(trx,140,16).trim();
		mchtId 		= CommonUtil.toString(trx,156,15).trim();
		cardCd 		= CommonUtil.toString(trx,171,22).trim();
		authCd 		= CommonUtil.toString(trx,193,20).trim();
		cardResultCd = CommonUtil.toString(trx,213,4).trim();
		point 		= CommonUtil.toString(trx,217,2).trim(); 
	}
	
	
	

}
