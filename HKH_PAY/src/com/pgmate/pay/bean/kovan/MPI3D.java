package com.pgmate.pay.bean.kovan;

import com.pgmate.lib.util.lang.CommonUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Administrator
 *
 */
public class MPI3D extends KovanHead {
	private static Logger logger = LoggerFactory.getLogger(MPI3D.class);

	//공통부문
	public String mchtFiller1 	= ""; 			// PG입점업체정보 / 95 / 1-10 SPACE
	public String mchtBRN 		= "";			// PG입점업체정보 / 95 / 11-20 서브몰사업자번호
	public String mchtFiller2 	= "";			// PG입점업체정보 / 95 / 21-35 SPACE 
	public String mchtUrl 		= "";			// PG입점업체정보 / 95 / 36-75 상점URL
	public String mchtIp 		= "";			// PG입점업체정보 / 95 / 76-95 상점IP
	public String installment 	= ""; 			// 할부개월수 / 5 / 002+할부개월(2)
	public String cavv 			= "";			//CAVV 값 / 28 / VISA 3D CAVV값 (Base64-Encoded)
	public String xid 			= "";			//xid 값 / 28 / VISA 3D XID값 (Base64-Encoded)
	public String retry 		= "";			//00: 일반승인 , 01 : 재승인 
	
	
		
	public MPI3D() {
	}

	/* 승인요청 (안심클릭) */
	public String getPay(boolean debug) {
		super.bitmap			= "3238040128608A02";	//승인,안심클릭
		super.trxType 			= "000030";				//거래구분코드
		
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
		trx.append(CommonUtil.byteFiller(mchtFiller1, 10));
		trx.append(CommonUtil.byteFiller(mchtBRN, 10));
		trx.append(CommonUtil.byteFiller(mchtFiller2, 15));
		trx.append(CommonUtil.byteFiller(mchtUrl, 40));
		trx.append(CommonUtil.byteFiller(mchtIp, 20));
		trx.append(CommonUtil.byteFiller("002"+installment, 5));
		trx.append(CommonUtil.byteFiller(cavv, 28));
		trx.append(CommonUtil.byteFiller(xid, 28));
		trx.append(CommonUtil.byteFiller(retry, 2));
		
		String p = trx.toString();
		if(debug) {
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
			logger.info("거래입력유형: ["       +   p.substring(idx, (idx += 3  ))   +          "] cur: " + idx);
			logger.info("취급기관코드: ["       +   p.substring(idx, (idx += 8  ))   +          "] cur: " + idx);
			logger.info("TRACK II DATA: ["      +   p.substring(idx, (idx += 39 ))	 +          "] cur: " + idx);
			logger.info("거래고유번호: ["       +   p.substring(idx, (idx += 12 ))   +          "] cur: " + idx);
			logger.info("가맹점번호: ["         +   p.substring(idx, (idx += 15 ))   +          "] cur: " + idx);
			logger.info("CATID, 사업자번호: ["  +   p.substring(idx, (idx += 40 ))   +          "] cur: " + idx);
			logger.info("통화코드: ["           +   p.substring(idx, (idx += 3  ))   +          "] cur: " + idx);
			logger.info("PG입점업제정보: ["     +   p.substring(idx, (idx += 95  ))   +          "] cur: " + idx);
			logger.info("할부개월수: ["         +   p.substring(idx, (idx += 5  ))   +          "] cur: " + idx);
			logger.info("CAVV: ["           	+   p.substring(idx, (idx += 28 ))   +          "] cur: " + idx);
			logger.info("XID: ["         		+   p.substring(idx, (idx += 28  ))   +          "] cur: " + idx);
			logger.info("재승인 Flag: ["    	+   p.substring(idx, (idx += 2  ))   +          "] cur: " + idx);
		}
		return p;
	}
	
	
	/* 승인요청 (인증+승인, 비안심클릭)
	 * //MPI 인증 승인
		authDob = ""; 			// 주민사업자번호 / 13 / 법인 : 사업자번호(10) + SPACE(3) || 개인 : "000000" + 주민번호 뒷자리(7)
		authPw 	= ""; 			// 비밀번호 / 4 / 비밀번호(2) + SPACE(2)
		check 	= "";			// Check / 23 / SPACE(23)
		unique 	= ""; 			// 특수거래 / 54 / 서비스구분(2) + FILLER(52)
	 */
	public String getPayWithAuth(String authDob,String authPw,String unique) {
		super.bitmap			= "3238040128609A02";	//승인,안심클릭
		super.trxType 			= "910030";				//거래구분코드
		
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
		trx.append(CommonUtil.setFiller(23));
		
		trx.append(CommonUtil.byteFiller(mchtFiller1, 10));
		trx.append(CommonUtil.byteFiller(mchtBRN, 10));
		trx.append(CommonUtil.byteFiller(mchtFiller2, 15));
		trx.append(CommonUtil.byteFiller(mchtUrl, 40));
		trx.append(CommonUtil.byteFiller(mchtIp, 20));
		
		trx.append(CommonUtil.byteFiller("002"+installment, 5));
		trx.append(CommonUtil.byteFiller(cavv, 28));
		trx.append(CommonUtil.byteFiller(xid, 28));
		trx.append(CommonUtil.byteFiller(unique, 54)); 
		
		
		return trx.toString();
	}

}
