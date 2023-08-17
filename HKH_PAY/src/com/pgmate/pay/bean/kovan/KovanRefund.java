package com.pgmate.pay.bean.kovan;

import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 * 카드 인증만 할 경우 
 */
public class KovanRefund extends KovanHead {

	//취소거래
	public String mchtFiller1 	= ""; 			// PG입점업체정보 / 95 / 1-10 SPACE
	public String mchtBRN 		= "";			// PG입점업체정보 / 95 / 11-20 서브몰사업자번호
	public String mchtFiller2 	= "";			// PG입점업체정보 / 95 / 21-35 SPACE 
	public String mchtUrl 		= "";			// PG입점업체정보 / 95 / 36-75 상점URL
	public String mchtIp 		= "";			// PG입점업체정보 / 95 / 76-95 상점IP
	public String installment 	= ""; 			// 할부개월수 / 5 / 002+할부개월(2)
	public String authCd 		= ""; 			// 승인번호
	public String route			= "";			// “00” :  일반거래 , “01” : VISA 3D 안심클릭 거래
	

		
	public KovanRefund() {
		super.trxType			= "000031";				
		super.bitmap			= "3238040128608B40";
		super.reqDate			= CommonUtil.getCurrentDate("MMddHHmmss");
		super.trxDay			= CommonUtil.getCurrentDate("yyMMdd");
		super.trxTime			= CommonUtil.getCurrentDate("HHmmss");
	}
	
	public String getRefund() {
		super.type 				= "0420";
		return get();
	}
	
	/**
	 * 통신장애에 따른 취소를 요청 할 경우 응답이 없어서 취소할때.Thread로 전송 후 종료한다.
	 * @return
	 */
	public String getNetRefund() {
		super.type 				= "0520";
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
		trx.append("707");	//ECI고정값
		trx.append(CommonUtil.byteFiller("069"+companyCd, 8));
		trx.append(CommonUtil.byteFiller("37"+trackII, 39));
		trx.append(CommonUtil.byteFiller(trxId, 12));
		trx.append(CommonUtil.byteFiller(mchtId, 15));
		trx.append(CommonUtil.byteFiller(pairingTmnId, 10));
		trx.append(CommonUtil.byteFiller(pairingBRN, 10));
		trx.append(CommonUtil.setFiller(20));  
		trx.append(CommonUtil.byteFiller(currency, 3));
		
		//trx.append(CommonUtil.byteFiller("", 95)); // TEST
		trx.append(CommonUtil.byteFiller(mchtFiller1, 10));
		trx.append(CommonUtil.byteFiller(mchtBRN, 10));
		trx.append(CommonUtil.byteFiller(mchtFiller2, 15));
		trx.append(CommonUtil.byteFiller(mchtUrl, 40));
		trx.append(CommonUtil.byteFiller(mchtIp, 20));

		trx.append(CommonUtil.byteFiller("002"+installment, 5));
		trx.append(CommonUtil.byteFiller("008"+authCd, 11));
		trx.append(CommonUtil.byteFiller(route, 2));
		return trx.toString();
	}

}
