package com.pgmate.pay.bean.kovan;

import com.pgmate.lib.util.lang.CommonUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Administrator
 * ISP 취소는 요청가 응답이 동일하다.
 */
public class ISPRefund {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.bean.kovan.ISPRefund.class);

	private String type				= "0420";	// 0420 / 4 /승인취소요청
	private String bitmap			= "B20800012AE49440";	//	Primary bit map / 16 / B20800012AE49440
	private String sBitmap			= "0000004000000000"; 	// Secondary bit map / 16 / 0000004000000000
	private String trxType			= "012000"; // Processing Code / 6 / "012000" 
	public String amount			= ""; 	// 취소 요청 금액 / 12 / 
	public String currency			= "";	// 통화코드 / 3 / WON "410" , USD "840"
	public String reqDate			= ""; 	// 전문 전송 일시 / 10 / MMDDhhmmss
	public String trackId			= "";	// 전문 추적 번호 / 6 / PG에서 전문에 대한 일련번호 생성
	public String installment		= ""; 	// 할부기간 / 2 / 03~12
	public String netRefund			= "1";	// 취소 유형 “1” 정상건 취소, "2" KVP<->PG간 망상취소(승인번호없는 취소 )
	public String companyCd			= "";	// KVP에서 부여한 기관 코드 PGID와 동일 예) 이니시스 “19038”
	public String rootTrxId			= "";	// TID / 40 / 상점 거래 ID(PG에서 취급하는 거래고유번호)
	public String vpTrxId			= "";	// 거래고유번호 / 12 / 
	public String rootTrxDay 		= "";	// 원거래 승인일자 / 6 / YYMMDD
	public String resultCd 		= "";	// 응답코드 / 4 /
	public String pairingTmnId		= "";	// Terminal-ID / 16 / Left Just
	public String mchtId			= "";	// 가맹점 번호 / 15
	public String authCd 			= "";	// 승인번호 / 20 / 
	public String cardNo			= "";	//카드번호
	
	
	
	public ISPRefund() {
	}
	
	public ISPRefund(String trx) {
		this(trx.getBytes());
	}
	
	public ISPRefund(byte[] trx) {
		
		type 		= CommonUtil.toString(trx,4,4).trim();
		bitmap 		= CommonUtil.toString(trx,8,16).trim();
		cardNo 		= CommonUtil.toString(trx,0,0).trim();
		trxType 	= CommonUtil.toString(trx,40,6).trim();
		amount 		= CommonUtil.toString(trx,46,12).trim();
		currency 	= CommonUtil.toString(trx,58,3).trim();
		reqDate 	= CommonUtil.toString(trx,61,10).trim();
		trackId 	= CommonUtil.toString(trx,71,6).trim();
		installment = CommonUtil.toString(trx,77,2).trim();
		netRefund 	= CommonUtil.toString(trx,79,1).trim();
		companyCd 	= CommonUtil.toString(trx,80,5).trim();
		rootTrxId 	= CommonUtil.toString(trx,85,40).trim();
		vpTrxId 	= CommonUtil.toString(trx,125,12).trim();
		rootTrxDay 	= CommonUtil.toString(trx,137,6).trim();
		resultCd 	= CommonUtil.toString(trx,143,4).trim();
		pairingTmnId 	= CommonUtil.toString(trx,147,16).trim();
		mchtId 		= CommonUtil.toString(trx,163,15).trim();
		authCd 		= CommonUtil.toString(trx,178,20).trim();
	}
	
	
	public String get() {
		StringBuilder trx = new StringBuilder();
		trx.append(type);
		trx.append(bitmap);
		trx.append(sBitmap);
		trx.append(trxType); 
		trx.append(CommonUtil.zerofill(amount, 12));
		trx.append(CommonUtil.byteFiller(currency, 3));
		trx.append(CommonUtil.byteFiller(reqDate, 10));
		trx.append(CommonUtil.byteFiller(trackId, 6));
		trx.append(CommonUtil.byteFiller(installment, 2));
		trx.append(CommonUtil.byteFiller(netRefund, 1));
		trx.append(CommonUtil.byteFiller(companyCd, 5));
		trx.append(CommonUtil.byteFiller(rootTrxId, 40));
		trx.append(CommonUtil.byteFiller(vpTrxId, 12));
		if(rootTrxDay.length() == 8) {
			this.rootTrxDay = rootTrxDay.substring(2);
		}
		trx.append(CommonUtil.byteFiller(rootTrxDay, 6));
		trx.append(CommonUtil.byteFiller(resultCd, 4));
		trx.append(CommonUtil.byteFiller(pairingTmnId, 16)); 
		trx.append(CommonUtil.byteFiller(mchtId, 15));
		trx.append(CommonUtil.byteFiller(authCd, 20));
		String trxPlain = trx.toString();
		return CommonUtil.zerofill(trxPlain.getBytes().length, 4)+trxPlain;
	}

	public static int logForLength(String data, String tag, int start, int end) {
		end = start+end;
		logger.info("{}: [{}]", tag, data.substring(start,end));
		return end;
	}

	public static void logSend(String data) {
		int idx = 0;
		idx = logForLength(data, "MsgLen", idx, 4);
		idx = logForLength(data, "FormNo", idx, 4);
		idx = logForLength(data, "PBM", idx, 16);
		idx = logForLength(data, "SBM", idx, 16);
		idx = logForLength(data, "PC", idx, 6);
		idx = logForLength(data, "Price", idx, 12);
		idx = logForLength(data, "Currence", idx, 3);
		idx = logForLength(data, "Tdate", idx, 10);
		idx = logForLength(data, "TraceNo", idx, 6);
		idx = logForLength(data, "Quota", idx, 2);
		idx = logForLength(data, "CancelGB", idx, 1);
		idx = logForLength(data, "AcquireNo", idx, 5);
		idx = logForLength(data, "Tid", idx, 40);
		idx = logForLength(data, "TSNo", idx, 12);
		idx = logForLength(data, "OTDate", idx, 6);
		idx = logForLength(data, "Result", idx, 4);
		idx = logForLength(data, "CatId", idx, 16);
		idx = logForLength(data, "MerchantNo", idx, 15);
		idx = logForLength(data, "KVPAuthNo", idx, 20);
	}
	

}
