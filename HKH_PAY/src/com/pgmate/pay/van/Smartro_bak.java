package com.pgmate.pay.van;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Refund;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.PAYUNIT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.co.smartro.adapter.server.client.SmilePayClient;
import kr.co.smartro.adapter.server.client.dto.WebMessageDTO;

public class Smartro_bak implements Van {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.van.Smartro_bak.class);

	private static String SMARTRO_DOMAIN = "211.193.35.215"; // 211.193.35.7(test) 211.193.35.215(real)
	//private static String SMARTRO_DOMAIN = "211.193.35.7"; // 211.193.35.7(test) 211.193.35.215(real)
	private static String SMARTRO_PORT = "9001";
	private static String SOCKET_TIMEOUT = "120000";

	private String VAN_ID 				= "";
	private String CRYPTOKEY			= "";
	private String VAN					= "";

	private SmilePayClient payClient = null;

	public Smartro_bak(SharedMap<String, Object> tmnVanMap) {
		VAN_ID =  tmnVanMap.getString("vanId").trim();
		CRYPTOKEY	= tmnVanMap.getString("cryptoKey").trim();
		VAN = tmnVanMap.getString("van");

		payClient = new SmilePayClient();
		payClient.setParam("SMILEPAY_DOMAIN_NAME", SMARTRO_DOMAIN);
		payClient.setParam("SMILEPAY_ADAPTOR_LISTEN_PORT", SMARTRO_PORT);
		payClient.setParam("SOCKET_SO_TIMEOUT", SOCKET_TIMEOUT);
		/** 2-1. 로그 디렉토리 설정 */
		//payClient.setParam("SMILEPAY_LOG_HOME", "/payprod/log/payprod_log/adplog");
		/** 2-2. 어플리케이션로그 모드 설정(0: DISABLE, 1: ENABLE) */
		payClient.setParam("APP_LOG", "0");
		/** 2-3. 이벤트로그 모드 설정(0: DISABLE, 1: ENABLE) */
		payClient.setParam("EVENT_LOG", "0");
		/** 2-4. 암호화플래그 설정(N: 평문, A2:암호화) */
		payClient.setParam("EncFlag", "A2");
		/** 2-6. 통화구분 설정(현재 KRW(원화) 가능) */
		payClient.setParam("Currency", "KRW");
		
		payClient.setParam("MID", VAN_ID); // 가맹점 아이디
		payClient.setParam("PayMethod", "CARD"); // 지불수단(고정값)

		payClient.setParam("TransType", "0"); // 결제타입(고정값)
		payClient.setParam("FormBankCd", "01"); // (고정값)
		payClient.setParam("KeyInCl", "01"); // (고정값)
		payClient.setParam("VbankExpDate", ""); // 입금만료일
		payClient.setParam("MallReserved", ""); // 상점예비정보
		
		payClient.setParam("BuyerAddr", ""); // 배송지주소
		payClient.setParam("BuyerPostNo", ""); // 우편번호
		payClient.setParam("MallUserID", ""); // 회원사고객ID

		payClient.setParam("CardInterest", "0"); // 카드 (고정)
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		/** 2-5. 서비스모드 설정(결제 서비스 : PY0 , 취소 서비스 : CL0) */
		payClient.setParam("SERVICE_MODE", "PY0");
		
		/* 고정값 */
		payClient.setParam("ChannelType", "WEB"); // Chanel 타입 (고정값)
		payClient.setParam("Language ", ""); // 리턴 메시지 언어 종류 한글 KR 영어 EN
		payClient.setParam("TID", ""); // 거래TID (요청시에는 빈값임)(고정값)
		payClient.setParam("UserIP", ""); // 회원사고객 IP

		// 공통 파라미터
		if(response.pay.products != null) {
			Product prd = response.pay.products.get(0);
			payClient.setParam("GoodsCnt", Integer.toString(prd.qty)); // 결제 상품 품목 개수
			payClient.setParam("GoodsName", prd.name); // 거래 상품명
		} else {
			payClient.setParam("GoodsCnt", "1"); // 결제 상품 품목 개수
			payClient.setParam("GoodsName", "상품"); // 거래 상품명
		}
		payClient.setParam("Amt", CommonUtil.toString(response.pay.amount)); // 거래 금액
		payClient.setParam("Moid", response.pay.trxId); // LENG: 100 - 상품주문번호 특수문자 쉽표 드 일체 불가 영문 한글만가능

		payClient.setParam("BuyerName", getDefaultStr(response.pay.payerName, "구매자")); // 구매자명
		payClient.setParam("BuyerAuthNum", getDefaultStr(response.pay.payerTel, "15222742")); // 구매자인증번호(휴대폰번호,사업자번호)
		payClient.setParam("BuyerTel", getDefaultStr(response.pay.payerTel, "15222742")); // 구매자연락처
		payClient.setParam("BuyerEmail", getDefaultStr(response.pay.payerEmail, "")); // 구매자 이메일
		
		if(CommonUtil.isNullOrSpace(sharedMap.getString(PAYUNIT.REMOTEIP))) {
			payClient.setParam("MallIP", sharedMap.getString(PAYUNIT.HOST)); // 상점서버 IP (필수값)
		} else {
			payClient.setParam("MallIP", sharedMap.getString(PAYUNIT.REMOTEIP)); // 상점서버 IP (필수값)
		}

		// 카드 파라미터
		payClient.setParam("CardType", "01"); // 개인카드01 법인카드02 TODO: 미리 구분해서 보내던가 문의해서 필수인지 확인한다.

		payClient.setParam("CardQuota", CommonUtil.zerofill(response.pay.card.installment, 2)); // 할부 개월

		payClient.setParam("CardNo", response.pay.card.number); // 카드코드
		payClient.setParam("CardExpire", response.pay.card.expiry); // 카드유효기간(YYMM)
		payClient.setParam("CardPwd", "");
		
		payClient.setParam("AuthFlg", "2"); // 키인 결제:2, 구 인증 결제:3
		if(response.pay.metadata != null){		//KSNET 에서 가급적 사용하지 않으려함.
			if(response.pay.metadata.isTrue("cardAuth")){
				// 구인증 방식
				payClient.setParam("BuyerAuthNum", response.pay.metadata.getString("authDob")); // 구 인증 방식만 사용 생년월일  YYMMDD
				payClient.setParam("CardPwd", response.pay.metadata.getString("authPw")); // 구 인증 방식만 사용 카드비밀번호
				payClient.setParam("AuthFlg", "3"); // 키인 결제:2, 구 인증 결제:3
			}
			response.pay.metadata = null;
		}

		/** 3. 결제 요청 */
		WebMessageDTO res = null;
		try{
			res = payClient.doService();
			
		}catch(Exception e){
			logger.info("KSNET CONNECTION ERROR [{}]",CommonUtil.getExceptionMessage(e));
			logger.info("KSNET IP :{},PORT:{}",SMARTRO_DOMAIN, SMARTRO_PORT);
			res = new WebMessageDTO();
			res.setParameter("ResultCode", "XXXX");
			res.setParameter("AuthDate", CommonUtil.getCurrentDate("yyMMddhhmmss"));
			res.setParameter("ResultMsg", "통신장애");
			res.setParameter("ErrorMsg", "통신장애");
		}finally{
			logger.debug("SMARTRO RESPONSE [{},{}]", GsonUtil.toJson(res));
		}

		sharedMap.put("van",VAN);
		sharedMap.put("vanId",VAN_ID);

		if(res.getParameter("ResultCode").equals("3001")){
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
			response.pay.authCd = res.getParameter("AuthCode");
			sharedMap.put("vanTrxId",res.getParameter("TID"));
			sharedMap.put("vanResultCd","0000");
			sharedMap.put("vanResultMsg","정상승인");
			sharedMap.put("authCd",res.getParameter("AuthCode"));
			sharedMap.put("vanDate","20"+res.getParameter("AuthDate"));
			sharedMap.put("cardAcquirer", res.getParameter("AcquCardName"));
			
		}else if(!res.getParameter("ResultCode").equals("XXXX")){	
			String vanMessage = res.getParameter("ErrorCD") + " " + res.getParameter("ResultMsg")+" "+res.getParameter("ErrorMsg");
			response.result 	= ResultUtil.getResult(res.getParameter("ResultCode"),"승인실패",vanMessage);
			sharedMap.put("vanTrxId",CommonUtil.nToB(res.getParameter("TID")));
			sharedMap.put("vanResultCd",CommonUtil.nToB(res.getParameter("AuthCode")));
			sharedMap.put("vanResultMsg",vanMessage);
		}else{
			logger.info("시스템 장애 응답 구분값 없음. :{}", res.getParameter("ResultCode"));
			response.result 	= ResultUtil.getResult("XXXX","실패","통신장애");
			sharedMap.put("vanTrxId",CommonUtil.nToB(res.getParameter("TID")));
			sharedMap.put("vanResultCd",CommonUtil.nToB(res.getParameter("AuthCode")));
			sharedMap.put("vanResultMsg",CommonUtil.nToB(res.getParameter("ResultMsg")));	
		}
		
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));

		return sharedMap;
	}

	@Override  
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) { 

		/** 2-5. 서비스모드 설정(결제 서비스 : PY0 , 취소 서비스 : CL0) */
		payClient.setParam("SERVICE_MODE", "CL0");
		
		payClient.setParam("CancelAmt", CommonUtil.toString(response.refund.amount));
		payClient.setParam("CancelPwd", CRYPTOKEY); //취소 패스워드
		
		
		payClient.setParam("CancelMsg", "100"); // 결제취소 100 / 망취소 60
		
		//payClient.setParam("MID", SMARTRO_MID);
		payClient.setParam("TID", payMap.getString("vanTrxId"));
		
		if(sharedMap.isEquals("rfdAll", "부분")) {
			payClient.setParam("PartialCancelCode", "1");	//부분 취소
		} else {
			payClient.setParam("PartialCancelCode", "0");	//전체 취소
		}
		
		//payClient.setParam("MallIP", sharedMap.getString(PAYUNIT.REMOTEIP)); // 상점서버 IP (필수값)
		/** 3. 결제 요청 */
		WebMessageDTO res = null;
		try{
			res = payClient.doService();
			
		} catch (Exception e) {
			logger.info("KSNET CONNECTION ERROR [{}]",CommonUtil.getExceptionMessage(e));
			logger.info("KSNET IP :{},PORT:{}",SMARTRO_DOMAIN, SMARTRO_PORT);
			res = new WebMessageDTO();
			res.setParameter("ResultCode", "XXXX");
			res.setParameter("CancelDate", CommonUtil.getCurrentDate("yyyyMMdd"));
			res.setParameter("CancelTime", CommonUtil.getCurrentDate("hhmmss"));
			res.setParameter("ResultMsg", "통신장애");
			res.setParameter("ErrorMsg", "통신장애");
		}finally{
			logger.debug("SMARTRO RESPONSE [{},{}]", GsonUtil.toJson(res));
		}

		sharedMap.put("van",payMap.getString("van"));
		sharedMap.put("vanId",VAN_ID);

		if(res.getParameter("ResultCode").equals("2001")){
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
			response.refund.authCd = payMap.getString("authCd");
			// 부분취소는 PartialTID 를 사용한다고 함.
			sharedMap.put("vanTrxId", res.getParameter("TID"));
			sharedMap.put("vanResultCd","0000");
			sharedMap.put("vanResultMsg","정상승인");
			sharedMap.put("authCd", payMap.getString("authCd"));
			sharedMap.put("vanRegDate",res.getParameter("CancelDate") + res.getParameter("CancelTime"));
		}else if(!res.getParameter("ResultCode").equals("XXXX")){	
			String vanMessage = res.getParameter("ErrorCD") + " " + res.getParameter("ResultMsg")+" "+res.getParameter("ErrorMsg");
			response.result 	= ResultUtil.getResult(res.getParameter("ResultCode"),"승인실패",vanMessage);
			sharedMap.put("vanTrxId",res.getParameter("TID"));
			sharedMap.put("vanResultCd",res.getParameter("ResultCode"));
			sharedMap.put("vanResultMsg",vanMessage);
		}else{
			logger.info("시스템 장애 응답 구분값 없음. :{}", res.getParameter("ResultCode"));
			response.result 	= ResultUtil.getResult("XXXX","실패","통신장애");
			sharedMap.put("vanTrxId",res.getParameter("TID"));
			sharedMap.put("vanResultCd",res.getParameter("ResultCode"));
			sharedMap.put("vanResultMsg",res.getParameter("ResultMsg"));
		}
		
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));
		return sharedMap;
	}


	public String getDefaultStr(String src, String defaultStr) {
		if (src == null || src.length() < 1) {
			if (defaultStr == null) { return ""; }
			return defaultStr;
		}
		return src;
	}


	public static void main(String[] args) {
		SharedMap<String, Object> tmnVanMap = new SharedMap<String, Object>();
		tmnVanMap.put("van", "SMARTRO");
		tmnVanMap.put("secondKey", "");
		tmnVanMap.put("vanId", "0000000001");
		Smartro_bak sm = new Smartro_bak(tmnVanMap);
		TrxDAO trxDAO = new TrxDAO(); 
		SharedMap<String, Object> sharedMap = new SharedMap<String, Object>(); 
		Response response = new Response();

		sharedMap.put(PAYUNIT.REMOTEIP, "127.0.0.1");

		response.pay = new Pay();
		response.pay.trxId = "T2020062900001";
		response.pay.amount = 1004;

		response.pay.card = new Card();
		response.pay.card.number = "6243680046512003";
		response.pay.card.expiry = "2401";
		response.pay.card.installment = 0;

		//sm.sales(trxDAO, sharedMap, response);

		SharedMap<String, Object> payMap = new SharedMap<String, Object>();
		payMap.put("vanTrxId", "SMTPAY002m01012006301121535086");

		response.refund = new Refund();
		response.refund.amount = 1004;

		sm.refund(trxDAO, sharedMap, payMap, response);
	}
}