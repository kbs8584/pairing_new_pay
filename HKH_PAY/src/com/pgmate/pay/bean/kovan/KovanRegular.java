package com.pgmate.pay.bean.kovan;

import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 * 카드 인증만 할 경우 
 */
public class KovanRegular extends KovanHead {

	//인증 거래 또는 인증+승인거래 #### 비인증관련승인거래는 애매함.
	public String authDob 		= ""; 			// 주민사업자번호 / 13 / 법인 : 사업자번호(10) + SPACE(3) || 개인 : "000000" + 주민번호 뒷자리(7)
	public String authPw 		= ""; 			// 비밀번호 / 4 / 비밀번호(2) + SPACE(2)
	public String check 		= "";			// Check / 23 / SPACE(23)
	public String mchtFiller1 	= ""; 			// PG입점업체정보 / 95 / 1-10 SPACE
	public String mchtBRN 		= "";			// PG입점업체정보 / 95 / 11-20 서브몰사업자번호
	public String mchtFiller2 	= "";			// PG입점업체정보 / 95 / 21-35 SPACE 
	public String mchtUrl 		= "";			// PG입점업체정보 / 95 / 36-75 상점URL
	public String mchtIp 		= "";			// PG입점업체정보 / 95 / 76-95 상점IP
	public String installment 	= ""; 			// 할부개월수 / 5 / 002+할부개월(2)

		
	public KovanRegular() {
		super.bitmap			= "3238040128609A00";	//비인증 인증,승인
		super.reqDate			= CommonUtil.getCurrentDate("MMddHHmmss");
		super.trxDay			= CommonUtil.getCurrentDate("yyMMdd");
		super.trxTime			= CommonUtil.getCurrentDate("HHmmss");
	}
	
	/**
	 * 수기 인증만 할 경우 (카드 등록 , 카드 유효성확인)
	 * @return
	 */
	public String getOnlyAuth() {
		
		super.trxType			= "710030";				//인증만 할 경우 
		
		return get();
	}
	
	/**
	 * 인증 및 승인 처리 ( 결제 동시 카드 등록 )
	 * @return
	 */
	public String getPayWithAuth() {
		super.trxType			= "810030";				//인증을 동반한 승인 처리 
		return get();
		
	}
	
	
	private String get() {
		StringBuilder trx = new StringBuilder();
		trx.append(iso);	//3
		trx.append(host);	//9
		trx.append(type);	//4
		trx.append(bitmap);	//16	
		trx.append(trxType);	//6
		trx.append(CommonUtil.zerofill(amount, 12));
		trx.append(CommonUtil.byteFiller(reqDate, 10));
		trx.append(CommonUtil.byteFiller(trackId, 6));
		trx.append(CommonUtil.byteFiller(trxTime, 6));
		trx.append(CommonUtil.byteFiller(trxDay, 6));
		trx.append(CommonUtil.byteFiller("7"+eci, 3));
		trx.append(CommonUtil.byteFiller("069"+companyCd, 8));
		trx.append(CommonUtil.byteFiller("37"+trackII, 39));
		trx.append(CommonUtil.byteFiller(trxId, 12));
		trx.append(CommonUtil.byteFiller(mchtId, 15));
		trx.append(CommonUtil.byteFiller(pairingTmnId, 10));
		trx.append(CommonUtil.byteFiller(pairingBRN, 10));
		trx.append(CommonUtil.setFiller(20));  
		trx.append(CommonUtil.byteFiller(currency, 3));
		trx.append(CommonUtil.byteFiller(authDob, 13));
		trx.append(CommonUtil.byteFiller(authPw, 4));
		trx.append(CommonUtil.byteFiller(check, 23));
		trx.append(CommonUtil.byteFiller(mchtFiller1, 10));
		trx.append(CommonUtil.byteFiller(mchtBRN, 10));
		trx.append(CommonUtil.byteFiller(mchtFiller2, 15));
		trx.append(CommonUtil.byteFiller(mchtUrl, 40));
		trx.append(CommonUtil.byteFiller(mchtIp, 20));
		trx.append(CommonUtil.byteFiller("002"+installment, 5));
		
		
		return trx.toString();
	}

}
