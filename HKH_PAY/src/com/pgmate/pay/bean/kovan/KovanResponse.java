package com.pgmate.pay.bean.kovan;

import com.pgmate.lib.util.lang.CommonUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Administrator
 *
 */
public class KovanResponse extends KovanHead{
	private static Logger logger = LoggerFactory.getLogger(KovanResponse.class);
	
	public String iniCd		= "";		// 이니 참조코드 
	public String installment	= "";	//할부개월
	public String authCd 		= "";	//	승인번호 / 11 / "008"+승인번호
	public String issuerCd 	 	= "";	// 발급사코드 / 4 / 발급사코드(4)
	public String cardResultCd	= "";	// 카드사 응답코드 / 4 / 00일경우 : LEFT 정렬 빈 부분은 SPACE 처리
	public String resultCd	= "";		//VAN 사 응답코드 / 4 / 00일경우 : LEFT 정렬 빈 부분은 SPACE 처리
	
	
	public KovanResponse() {
		
	}
	public KovanResponse(String trx) {
		this(trx.getBytes());
	}
	
	public KovanResponse(byte[] trx) {
		iso 		= CommonUtil.toString(trx,0,3).trim();
		host 		= CommonUtil.toString(trx,3,9).trim();
		type 		= CommonUtil.toString(trx,12,4).trim();
		bitmap 		= CommonUtil.toString(trx,16,16).trim();
		trxType 	= CommonUtil.toString(trx,32,6).trim();
		amount 		= CommonUtil.toString(trx,38,12).trim();
		reqDate 	= CommonUtil.toString(trx,50,10).trim();
		trackId 	= CommonUtil.toString(trx,60,6).trim();
		trxTime 	= CommonUtil.toString(trx,66,6).trim();
		trxDay 		= CommonUtil.toString(trx,72,6).trim();	
		companyCd 	= CommonUtil.toString(trx,78,8).trim();
		trackII 	= CommonUtil.toString(trx,86,39).trim();
		trxId	 	= CommonUtil.toString(trx,125,12).trim();
		iniCd 		= CommonUtil.toString(trx,137,2).trim();
		mchtId 		= CommonUtil.toString(trx,139,15).trim();
		pairingTmnId	= CommonUtil.toString(trx,154,10).trim();
		pairingBRN 	= CommonUtil.toString(trx,164,10).trim();
		currency 	= CommonUtil.toString(trx,194,3).trim();
		installment = CommonUtil.toString(trx,200,2).trim();	//3자리 SKIP "002"
		authCd 		= CommonUtil.toString(trx,205,11).trim();	//3자리 SKIP "008"
		issuerCd 	= CommonUtil.toString(trx,213,4).trim();
		cardResultCd= CommonUtil.toString(trx,217,4).trim();
		resultCd = CommonUtil.toString(trx,221,4).trim();
	}
	
	public static void printFormat(String p) {
		int idx = 0;
		logger.info("MPI 응답 메시지 정보");
		logger.info("TEXT 개시문자: ["      +   p.substring(idx, (idx += 3  ))   +          "] cur: " + idx);
		logger.info("HOST가맹점: ["         +   p.substring(idx, (idx += 9  ))   +          "] cur: " + idx);
		logger.info("전문TYPE: ["           +   p.substring(idx, (idx += 4  ))   +          "] cur: " + idx);
		logger.info("Primary Bitmap: ["     +   p.substring(idx, (idx += 16 ))   +          "] cur: " + idx);
		logger.info("거래구분코드: ["       +   p.substring(idx, (idx += 6  ))   +          "] cur: " + idx);
		logger.info("거래금액: ["           +   p.substring(idx, (idx += 12 ))   +          "] cur: " + idx);
		logger.info("전문전송일시: ["       +   p.substring(idx, (idx += 10 ))   +          "] cur: " + idx);
		logger.info("전문추적번호: ["       +   p.substring(idx, (idx += 6  ))   +          "] cur: " + idx);
		logger.info("거래개시시간: ["       +   p.substring(idx, (idx += 6  ))   +          "] cur: " + idx);
		logger.info("거래개시일: ["         +   p.substring(idx, (idx += 6  ))   +          "] cur: " + idx);
		logger.info("취급기관코드: ["       +   p.substring(idx, (idx += 8  ))   +          "] cur: " + idx);
		String trackII = p.substring(idx, (idx += 39 ));
		logger.info("TRACK II DATA: ["      +   trackII.substring(0, 6) + "******" + trackII.substring(12, trackII.length())   +          "] cur: " + idx);
		logger.info("거래고유번호: ["       +   p.substring(idx, (idx += 12 ))   +          "] cur: " + idx);
		logger.info("PG 응답코드: ["        +   p.substring(idx, (idx += 2  ))   +          "] cur: " + idx);
		logger.info("가맹점번호: ["         +   p.substring(idx, (idx += 15 ))   +          "] cur: " + idx);
		logger.info("CATID, 사업자번호: ["  +   p.substring(idx, (idx += 40 ))   +          "] cur: " + idx);
		logger.info("통화코드: ["           +   p.substring(idx, (idx += 3  ))   +          "] cur: " + idx);
		logger.info("할부개월수: ["         +   p.substring(idx, (idx += 5  ))   +          "] cur: " + idx);
		logger.info("승인번호: ["           +   p.substring(idx, (idx += 11 ))   +          "] cur: " + idx);
		logger.info("발급사코드: ["         +   p.substring(idx, (idx += 4  ))   +          "] cur: " + idx);
		logger.info("카드사 응답코드: ["    +   p.substring(idx, (idx += 4  ))   +          "] cur: " + idx);
		logger.info("VAN사 응답코드: ["     +   p.substring(idx, (idx += 4  ))   +          "] cur: " + idx);
	}

}
