package com.pgmate.pay.van;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.bean.SmartroBean;
import com.pgmate.pay.bean.SmartroVacctBean;
import com.pgmate.pay.main.Api;

import io.vertx.ext.web.RoutingContext;
import kr.co.smartro.adapter.server.client.SmilePayClient;

public class SmartroVacct {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.van.SmartroVacct.class);

	private static String SMARTRO_DOMAIN = "211.193.35.215"; // 211.193.35.7(test) 211.193.35.215(real)
	//private static String SMARTRO_DOMAIN = "211.193.35.7"; // 211.193.35.7(test) 211.193.35.215(real)
	private static String SMARTRO_PORT = "9001";
	private static String SOCKET_TIMEOUT = "120000";

	private String VAN_ID 				= "";
	private String CRYPTOKEY			= "";
	private String VAN						= "";

	private SmilePayClient payClient = null;

	public SmartroVacct(SharedMap<String, Object> tmnVanMap) {
		VAN_ID =  tmnVanMap.getString("vanId").trim();
		CRYPTOKEY	= StringEscapeUtils.escapeJava(tmnVanMap.getString("secondKey").trim());
		CRYPTOKEY	= StringEscapeUtils.escapeHtml(CRYPTOKEY);
		VAN = tmnVanMap.getString("van");

		payClient = new SmilePayClient();
		payClient.setParam("SMILEPAY_DOMAIN_NAME", SMARTRO_DOMAIN);
		payClient.setParam("SMILEPAY_ADAPTOR_LISTEN_PORT", SMARTRO_PORT);
		payClient.setParam("SOCKET_SO_TIMEOUT", SOCKET_TIMEOUT);
		/** 2-1. 로그 디렉토리 설정 */
		// payClient.setParam("SMILEPAY_LOG_HOME", "/payprod/log/payprod_log/adplog");
		/** 2-2. 어플리케이션로그 모드 설정(0: DISABLE, 1: ENABLE) */
		payClient.setParam("APP_LOG", "0");
		/** 2-3. 이벤트로그 모드 설정(0: DISABLE, 1: ENABLE) */
		payClient.setParam("EVENT_LOG", "0");
		/** 2-4. 암호화플래그 설정(N: 평문, A2:암호화) */
		payClient.setParam("EncFlag", "A2");
		/** 2-6. 통화구분 설정(현재 KRW(원화) 가능) */
		payClient.setParam("Currency", "KRW");

		payClient.setParam("MID", VAN_ID); 			// 가맹점 아이디
		payClient.setParam("PayMethod", "VBANK"); 	// 지불수단(고정값)

		payClient.setParam("TransType", "0"); // 결제타입(고정값)
		payClient.setParam("FormBankCd", "01"); // (고정값)
		payClient.setParam("KeyInCl", "01"); // (고정값)
		payClient.setParam("VbankExpDate", ""); // 입금만료일 - 가상계좌 이용 시 필수
		payClient.setParam("MallReserved", ""); // 상점예비정보

		payClient.setParam("BuyerAddr", ""); // 배송지주소
		payClient.setParam("BuyerPostNo", ""); // 우편번호
		payClient.setParam("MallUserID", ""); // 회원사고객ID

		payClient.setParam("CardInterest", "0"); // 카드 (고정)
		
		// 2022-02-16 추가
		payClient.setParam("ReturnUrl", ""); // 카드 (고정)
		
		logger.info("========== ========== ========== ========== ========== ");
		logger.info("========== ========== ========== ========== payClient MID vanId : " + payClient.getParam("MID"));
		logger.info("========== ========== ========== ========== payClient merchantKey : " + payClient.getParam("merchantKey"));
		logger.info("========== ========== ========== ========== payClient merchantKey = CRYPTOKEY : " + CRYPTOKEY);
		logger.info("========== ========== ========== ========== ========== ");
	}
	
	public synchronized static boolean vacct(RoutingContext rc, String uri) {
		logger.info("========== ========== ========== ========== ========== vacct() - 스마트로 가상계좌 발급");
		if (uri.startsWith("/api/vacct/smartro")) {
			SharedMap<String, Object> sharedMap = new SharedMap<String, Object>();
			System.out.println();
			SmartroBean smartroBean = (SmartroBean) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), SmartroBean.class);
			DAO dao = new DAO();
			dao.setColumns("A.mchtId, A.van, B.vanId, A.semiAuth, B.vanId AS mid, B.cryptoKey as merchantKey, B.vactUrl \n");
			dao.setTable("PG_MCHT_TMN A \n" + 
					"JOIN PG_VAN B ON A.vanIdx = B.idx \n");
			dao.addWhere("mchtId", smartroBean.mchtId);
			dao.setOrderBy("");
			SharedMap<String, Object> vanMap = dao.search().getRowFirst();
			
			// REAL 전환 시 
			String TargetURL = "https://approval.smartropay.co.kr";
			//String TargetURL = "https://tapproval.smartropay.co.kr";		// 테스트
			TargetURL = vanMap.getString("vactUrl");
			
			String Vacct = "/payment/approval/vacct.do";

			JSONObject body = new JSONObject();
			JSONObject snd = new JSONObject();

			String merchantKey = "";  // 발급받은 테스트 상점키 설정(Real 전환 시 운영 상점키 설정)
			merchantKey = vanMap.getString("merchantKey");
			
			String Mid = vanMap.getString("mid");
			String EdiDate = getyyyyMMddHHmmss();
			String Moid = "SMARTRO_" + EdiDate;
			
			// 요청 파라미터 (각 값들은 가맹점 환경에 맞추어 설정해 주세요.)
			body.put("PayMethod", "VBANK");
			body.put("GoodsCnt", 1);
			body.put("GoodsName", smartroBean.prdName);
			body.put("Amt", smartroBean.amount);
			body.put("Moid", Moid);
			logger.info("========== ========== ========== ========== Moid : " + Moid);
			body.put("EdiDate" ,EdiDate);
			body.put("Mid" , Mid);
			body.put("TransType", "00");  										// 00: default , 04: 일괄 채번
			if (body.get("TransType") == null || body.get("TransType").equals("") || body.get("TransType").equals("00")) {
				body.put("SubId", "");  											// TransType 04일 경우 가상계좌번호 입력
			} else if (body.get("TransType").equals("04")) { 
				body.put("SubId", "");	// TransType 04일 경우 가상계좌번호 입력
			}
			body.put("BuyerName", smartroBean.payerName);
			body.put("BuyerTel", smartroBean.payerTel);
			body.put("BuyerEmail", smartroBean.payerEmail);

			// 가상계좌 관련 정보
			Timestamp toDay = new Timestamp((new Date()).getTime());
			Timestamp nxDay = null;
			try {
			    nxDay = getTimestampWithSpan(toDay, 7);
			} catch(Exception e){}
			String VbankExpDate = nxDay.toString();
			VbankExpDate = VbankExpDate.substring(0, 10);
			VbankExpDate = VbankExpDate.replaceAll("-", "");
			
			body.put("VbankExpDate"	,VbankExpDate);
			body.put("VbankBankCode", smartroBean.bankCd);		// 매뉴얼 사이트의 '통합코드 조회-은행사 코드' 참조 후 설정해 주세요.
			body.put("VBankAccountName", smartroBean.accntHolder);

			// 현금 영수증 관련 정보
			// 부가세 직접 계산 가맹점의 경우 아래 값들을 각각 계산하여 설정해야 합니다.
			body.put("CashReceiptType", "0");                   // 현금영수증 용도구분 (0: 미발행, 1 : 발행(개인 소득공제), 2 : 발행(사업자 지출증빙) 3: 자진발급)
			if (body.get("CashReceiptType").equals("") || body.get("CashReceiptType").equals("0")) {
				logger.info("========== ========== ========== ========== 현금영수증 미발행");
				body.put("ReceiptAmt", smartroBean.amount);	// 현금영수증 총 금액
				body.put("ReceiptTaxAmt", 0);					    // 현금영수증 과세
				body.put("ReceiptTaxFreeAmt", 0);				// 현금영수증 비과세
				body.put("ReceiptSupplyAmt", 0);                 // 현금영수증 공급가액
				body.put("ReceiptVatAmt", 0);                     // 현금영수증 부가세
				body.put("ReceiptIdentity", "");                    	// 현금영수증 발급번호
			} else {
				if (body.get("CashReceiptType").equals("1")) logger.info("========== ========== ========== ========== 현금영수증 발행(개인 소득공제");
				if (body.get("CashReceiptType").equals("2")) logger.info("========== ========== ========== ========== 현금영수증 발행(사업자 지출증빙");
				if (body.get("CashReceiptType").equals("3")) logger.info("========== ========== ========== ========== 현금영수증 자진발급");
				body.put("ReceiptAmt", "1004");                     // 현금영수증 총 금액
				body.put("ReceiptTaxAmt", "0");					    // 현금영수증 과세
				body.put("ReceiptTaxFreeAmt", "0");				// 현금영수증 비과세
				body.put("ReceiptSupplyAmt", "0");                 // 현금영수증 공급가액
				body.put("ReceiptVatAmt", "0");                     // 현금영수증 부가세
				body.put("ReceiptIdentity", "");                    // 현금영수증 발급번호
			}
			logger.info("========== ========== ========== ========== body : " + body);

			// json 데이터 AES256 암호화
			try {
			    snd.put("EncData", AES_Encode(body.toString(), merchantKey.substring(0, 32)));
			    snd.put("Mid", Mid);
			    logger.info("========== ========== ========== ========== snd EncData : " + snd.get("EncData"));
			    logger.info("========== ========== ========== ========== snd Mid : " + snd.get("Mid"));
			} catch(Exception e){
			    e.printStackTrace();
			    return false;
			}
			logger.info("========== ========== ========== ========== snd : " + snd);
			
			JSONObject resObj = connect(TargetURL + Vacct, snd);
			logger.info("========== ========== ========== resObj : " + resObj);
			logger.info("========== ========== ========== resObj ResultCode : " + resObj.get("ResultCode"));
			logger.info("========== ========== ========== resObj ResultMsg : " + resObj.get("ResultMsg"));
			//result = PayUtil.callApi(TrAuthKey, Tid);
			if (resObj.get("ResultCode").equals("4100")) {
				dao.initRecord();
				dao.setTable("PG_VACCT_SMARTRO");
				dao.setRecord("mchtId", smartroBean.mchtId);
				dao.setRecord("van", vanMap.get("van"));
				dao.setRecord("vanId", Mid);
				dao.setRecord("trxId", resObj.get("Moid"));
				dao.setRecord("accntHolder", smartroBean.accntHolder);
				dao.setRecord("bankCd", smartroBean.bankCd);
				dao.setRecord("expireDate", VbankExpDate);
				dao.setRecord("amount", smartroBean.amount);
				dao.setRecord("prdName", smartroBean.prdName);
				dao.setRecord("prdQty", 1);
				dao.setRecord("payerName", smartroBean.payerName);
				dao.setRecord("payerTel", smartroBean.payerTel);
				dao.setRecord("payerEmail", smartroBean.payerEmail);
				
				dao.setRecord("userId", smartroBean.userId);
				
				dao.setRecord("bankName", resObj.get("VbankName"));
				dao.setRecord("account", resObj.get("VbankNum"));
				
				//dao.setRecord("isCashReceipt", danalBean.isCashReceiptUi);
				// SubId
				// VerifyValue
				
				dao.setRecord("vanTrxId", Moid);
				dao.setRecord("vanResultCd", resObj.get("ResultCode"));
				dao.setRecord("vanResultMsg", resObj.get("ResultMsg"));
				
				dao.setRecord("regId", smartroBean.userId);
				dao.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
				dao.setRecord("regTime", CommonUtil.getCurrentDate("hhmmss"));

				if (dao.insert()) {
					logger.info("========== ========== ========== ========== ========== " + resObj.get("ResultCode") + " " + resObj.get("ResultMsg") + " | DB 입력 완료");
					logger.info("========== ========== ========== ========== SMARTRO VACCT OK");
					//VertXMessage.set200(rc, "application/json", "NOK", "");
					Api.sendMsg(rc, CommonUtil.toString(resObj.get("ResultCode")), "승인성공", CommonUtil.toString(resObj.get("ResultMsg")));
					return true;
				} else {
					logger.info("========== ========== ========== ========== ========== " + resObj.get("ResultCode") + " " + resObj.get("ResultMsg") + " | DB 입력 실패");
					logger.info("========== ========== ========== ========== SMARTRO VACCT FAIL");
					//VertXMessage.set200(rc, "application/json", "NOK", "");
					Api.sendMsg(rc, CommonUtil.toString(resObj.get("ResultCode")), "승인실패", CommonUtil.toString(resObj.get("ResultMsg")));
					return false;
				}
			} else {
				logger.info("========== ========== ========== ========== ========== " + resObj.get("ResultCode") + " " + resObj.get("ResultMsg"));
				logger.info("========== ========== ========== ========== SMARTRO VACCT FAIL");
				//VertXMessage.set200(rc, "application/json", "NOK", "");
				Api.sendMsg(rc, CommonUtil.toString(resObj.get("ResultCode")), "승인실패", CommonUtil.toString(resObj.get("ResultMsg")));
				return false;
			}
		}
		return false;
	}
	
	/* 현재일자 */
	public static final String getyyyyMMddHHmmss() {
	    SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss");
	    return yyyyMMddHHmmss.format(new Date());
	}
	/* 현재일자  */
	public static final String getyyyyMMddHHmm() {
	    SimpleDateFormat yyyyMMddHHmm = new SimpleDateFormat("yyyyMMddHHmm");
	    return yyyyMMddHHmm.format(new Date());
	}
	public static final Timestamp getTimestampWithSpan(Timestamp sourceTS, long day) throws Exception {
		Timestamp targetTS = null;
	    if (sourceTS != null) {
	        targetTS = new Timestamp(sourceTS.getTime() + (day * 1000 * 60 * 60 * 24));
	    }
	    return targetTS;
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

    public synchronized static JSONObject connect(String startUrl, HashMap<String,String> data) {
		logger.info("========== ========== ========== ========== ========== connect");
		logger.info("========== ========== ========== ========== connect startUrl : " + startUrl);
		HashMap<String, String> resData = new HashMap<String, String>();
		
		JSONParser resParser = new JSONParser();
		Object obj = new Object();
		JSONObject resObj = new JSONObject();
		
		long time = System.currentTimeMillis();
		// URL 호출로 승인 요청 후 결과 받기
		URL url = null;
		HttpsURLConnection connection = null;
		int connectTimeout = 1000;
		int readTimeout = 5000; // 가맹점에 맞게 TimeOut 조절
		StringBuilder responseBody = null;
		try {
		    SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
		    sslCtx.init(null, null, new SecureRandom());

		    url = new URL(startUrl);
		    logger.info("========== ========== ========== ========== connection url : " + url);
		    connection = (HttpsURLConnection) url.openConnection();
		    connection.addRequestProperty("Content-Type", "application/json");
		    connection.addRequestProperty("Accept", "application/json");
		    connection.setDoOutput(true);
		    connection.setDoInput(true);
		    connection.setConnectTimeout(connectTimeout);
		    connection.setReadTimeout(readTimeout);

		    OutputStreamWriter osw = new OutputStreamWriter(new BufferedOutputStream(connection.getOutputStream()) , "utf-8" );
		    char[] bytes = data.toString().toCharArray();
		    osw.write(bytes,0,bytes.length);
		    osw.flush();
		    osw.close();

		    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
		    String line = null;
		    responseBody =  new StringBuilder();
		    while ((line = br.readLine()) != null) {
		        logger.info("========== ========== ========== ========== response : " +  line);
		        // response {"ResultMsg":"시스템에러 잠시후 재 거래","ResultCode":"9999"}
		        responseBody.append(line);
		    }
		    br.close();
		} catch (Exception e) {
			String message = "SMARTRO VACCT NOT CONNECTED";
			resData.put("RETURNCODE", "XXXX");
			resData.put("RETURNMSG", message);
			logger.warn("========== ========== ========== ========== SMARTRO ERROR : {}", e.getMessage());
		} finally {
			
			try {
				obj = resParser.parse(responseBody.toString());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			resObj = (JSONObject) obj;
			logger.debug("========== ========== ========== ========== SMARTRO Elasped Time =[{} sec]", CommonUtil.parseDouble((System.currentTimeMillis() - time) / 1000));
			logger.debug("========== ========== ========== ========== SMARTRO res = [{}]", resObj);
		}
		return resObj;
	}
    
    public synchronized static boolean vacctNoti(RoutingContext rc, String uri) {
		if (uri.startsWith("/api/vacct/smartro/noti")) {
			logger.info("========== ========== ========== ========== ========== 가상계좌 스마트로 입금통보 ");
			SharedMap<String, Object> sharedMap = new SharedMap<String, Object>();
			System.out.println();
			SmartroVacctBean smartroVacctBean = (SmartroVacctBean) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), SmartroVacctBean.class);
			DAO dao = new DAO();
			dao.setColumns("A.mchtId, A.van, B.vanId, A.semiAuth, B.vanId AS mid, B.cryptoKey as merchantKey \n");
			dao.setTable("PG_MCHT_TMN A \n" + 
					"JOIN PG_VAN B ON A.vanIdx = B.idx \n");
			dao.addWhere("vanId", smartroVacctBean.MID);
			dao.setOrderBy("");
			SharedMap<String, Object> vanMap = dao.search().getRowFirst();
			
			String merchantKey = ""; // 발급받은 테스트 상점키 설정(Real 전환 시 운영 상점키 설정)
			String VerifySignValue = encodeSHA256Base64(
					smartroVacctBean.TID.substring(0, 10) + 
					smartroVacctBean.ResultCode + 
					smartroVacctBean.TID.substring(10, 15) + 
					merchantKey + 
					smartroVacctBean.TID.substring(15, smartroVacctBean.TID.length()));
			
			String SignValue = smartroVacctBean.SignValue;
			String ResultCode = smartroVacctBean.ResultCode;
			//검증에 성공했을 경우
			if (SignValue.equals(VerifySignValue)) {
				if ("3001".equals(ResultCode)) { // CARD
					// 결제 성공시 DB처리
					// TID 결제 취소한 데이터 존재시 UPDATE, 존재하지 않을 경우 INSERT
					// if(DB 처리 및 내부처리 로직 성공시) {
					return true;
					// }
				}
				if ("4000".equals(ResultCode)) { // BANK
					// 결제 성공시 DB처리
					// TID 결제 취소한 데이터 존재시 UPDATE, 존재하지 않을 경우 INSERT
					// if(DB 처리 및 내부처리 로직 성공시) {
					return true;
					// }
				}
				if ("4100".equals(ResultCode)) { // VBANK 채번완료
					// 결제 성공시 DB처리
					// TID 결제 취소한 데이터 존재시 UPDATE, 존재하지 않을 경우 INSERT
					// if(DB 처리 및 내부처리 로직 성공시) {
					return true;
					// }
				}
				if ("4110".equals(ResultCode)) { // VBANK 입금완료
					// 결제 성공시 DB처리
					// TID 결제 취소한 데이터 존재시 UPDATE, 존재하지 않을 경우 INSERT
					// if(DB 처리 및 내부처리 로직 성공시) {
					
					// 발급 계좌 조회
					dao.initRecord();
					dao.setTable("PG_VACCT_SMARTRO");
					dao.setColumns("*");
					dao.addWhere("vanTrxId", smartroVacctBean.TID);
					SharedMap<String, Object> smartroMap = dao.search().getRowFirst();
					if (!smartroMap.get("vanTrxId").equals("")) {
						// 입금 정보 저장
						dao.initRecord();
						dao.setTable("PG_VACCT_SMARTRO_DEPOSIT");
						dao.setRecord("van", vanMap.get("van"));
						dao.setRecord("vanId", smartroVacctBean.MID);
						dao.setRecord("amount", smartroVacctBean.Amt);
						dao.setRecord("payerName", smartroVacctBean.Name);
						dao.setRecord("prdName", smartroVacctBean.GoodsName);
						dao.setRecord("vanTrxId", smartroVacctBean.TID);
						dao.setRecord("rootVanTrxId", smartroVacctBean.OTID);
						dao.setRecord("prdTrxId", smartroVacctBean.OID);
						dao.setRecord("AuthDate", smartroVacctBean.AuthDate);
						dao.setRecord("authCode", smartroVacctBean.AuthCode);
						dao.setRecord("vanResultCd", smartroVacctBean.ResultCode);
						dao.setRecord("vanResultMsg", smartroVacctBean.ResultMsg);
						
						dao.setRecord("StateCd", smartroVacctBean.StateCd);
						dao.setRecord("FnCd", smartroVacctBean.FnCd);
						dao.setRecord("FnName", smartroVacctBean.FnName);
						dao.setRecord("account", smartroVacctBean.VbankNum);
						dao.setRecord("bankName", smartroVacctBean.VbankName);
						dao.setRecord("SignValue", smartroVacctBean.SignValue);
						// 선택
						dao.setRecord("MallUserID", smartroVacctBean.MallUserID);
						dao.setRecord("ReceiptType", smartroVacctBean.ReceiptType);
						dao.setRecord("RcptAppNo", smartroVacctBean.RcptAppNo);
						dao.setRecord("RcptCcNo", smartroVacctBean.RcptCcNo);
						dao.setRecord("CardUsePoint", smartroVacctBean.CardUsePoint);
						
						dao.setRecord("regId", "SYSTEM");
						dao.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
						dao.setRecord("regTime", CommonUtil.getCurrentDate("hhmmss"));
					
						if (dao.insert()) {
							Api.sendMsg(rc, smartroVacctBean.ResultCode, "승인성공", smartroVacctBean.ResultMsg);
							return true;
						} else {

							return false;
						}
					}
				}
				if ("A000".equals(ResultCode)) { // CELLPHONE
					// 결제 성공시 DB처리
					// TID 결제 취소한 데이터 존재시 UPDATE, 존재하지 않을 경우 INSERT
					// if(DB 처리 및 내부처리 로직 성공시) {
					return true;
					// }
				}
				if ("7001".equals(ResultCode)) { // 현금영수증
					// 결제 성공시 DB처리
					// TID 결제 취소한 데이터 존재시 UPDATE, 존재하지 않을 경우 INSERT
					// if(DB 처리 및 내부처리 로직 성공시) {
					return true;
					// }
				}
				if ("2001".equals(ResultCode)) { // 결제 취소
					// 취소 성공시 DB처리
					// TID 결제 취소한 데이터 존재시 UPDATE, 존재하지 않을 경우 INSERT
					// if(DB 처리 및 내부처리 로직 성공시) {
					return true;
					// }
				}
				if ("2211".equals(ResultCode)) { // 환불

				}
				if ("2013".equals(ResultCode)) { // 기취소 거래

				}
			} else {
				return false;
			}
		} else {
			return false;
		}
		return false;
	}
    /* SHA256 암호화 */
	public static final String encodeSHA256Base64(String strPW) {
	    String passACL = null;
	    MessageDigest md = null;

	    try {
	        md = MessageDigest.getInstance("SHA-256");
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    md.update(strPW.getBytes());
	    byte[] raw = md.digest();
	    byte[] encodedBytes = Base64.encodeBase64(raw);
	    passACL = new String(encodedBytes);

	    return passACL;
	}
    
    public synchronized static boolean vacctRefund(RoutingContext rc, String uri) {
    	if (uri.startsWith("/api/vacct/smartro/refund")) {
    		logger.info("========== ========== ========== ========== ========== 가상계좌 스마트로 환불이체 ");
    		SharedMap<String, Object> sharedMap = new SharedMap<String, Object>();
    		System.out.println();
    		SmartroBean smartroBean = (SmartroBean) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), SmartroBean.class);
    		DAO dao = new DAO();
    		dao.setColumns("A.mchtId, A.van, B.vanId, A.semiAuth, B.vanId AS mid, B.cryptoKey as merchantKey, B.vactUrl \n");
    		dao.setTable("PG_MCHT_TMN A \n" + 
    				"JOIN PG_VAN B ON A.vanIdx = B.idx \n");
    		dao.addWhere("mchtId", smartroBean.mchtId);
    		dao.setOrderBy("");
    		SharedMap<String, Object> vanMap = dao.search().getRowFirst();
    		
    		// REAL 전환 시 
    		String TargetURL = "https://approval.smartropay.co.kr";
    		//String TargetURL = "https://tapproval.smartropay.co.kr";		// 테스트
    		TargetURL = vanMap.getString("vactUrl");
    		String Vacct = "/payment/approval/vacct.do";
    		if (vanMap.getString("vactUrl") == null || "".equals(vanMap.getString("vactUrl"))) {
    			logger.warn("========== ========== ========== ========== 가상계좌 연동 URL이 없습니다 DB PG_VAN 테이블 vactUrl 을 확인하세요.");
    		}
    		String vactUrl = TargetURL + Vacct;
    		
    		JSONObject body = new JSONObject();
    		JSONObject snd = new JSONObject();
    		
    		String merchantKey = "";  // 발급받은 테스트 상점키 설정(Real 전환 시 운영 상점키 설정)
    		merchantKey = vanMap.getString("merchantKey");
    		
    		String Mid = vanMap.getString("mid");
    		String EdiDate = getyyyyMMddHHmmss();
    		String Moid = "SMARTRO_" + EdiDate;
    		
    		// 요청 파라미터 (각 값들은 가맹점 환경에 맞추어 설정해 주세요.)
    		body.put("PayMethod", "VBANK");
    		body.put("GoodsCnt", 1);
    		body.put("GoodsName", smartroBean.prdName);
    		body.put("Amt", smartroBean.amount);
    		body.put("Moid", Moid);
    		logger.info("========== ========== ========== ========== Moid : " + Moid);
    		body.put("EdiDate" ,EdiDate);
    		body.put("Mid" , Mid);
    		body.put("TransType", "00");  										// 00: default , 04: 일괄 채번
    		if (body.get("TransType") == null || body.get("TransType").equals("") || body.get("TransType").equals("00")) {
    			body.put("SubId", "");  											// TransType 04일 경우 가상계좌번호 입력
    		} else if (body.get("TransType").equals("04")) { 
    			body.put("SubId", "");	// TransType 04일 경우 가상계좌번호 입력
    		}
    		body.put("BuyerName", smartroBean.payerName);
    		body.put("BuyerTel", smartroBean.payerTel);
    		body.put("BuyerEmail", smartroBean.payerEmail);
    		
    		// 가상계좌 관련 정보
    		Timestamp toDay = new Timestamp((new Date()).getTime());
    		Timestamp nxDay = null;
    		try {
    			nxDay = getTimestampWithSpan(toDay, 7);
    		} catch(Exception e){}
    		String VbankExpDate = nxDay.toString();
    		VbankExpDate = VbankExpDate.substring(0, 10);
    		VbankExpDate = VbankExpDate.replaceAll("-", "");
    		
    		body.put("VbankExpDate"	,VbankExpDate);
    		body.put("VbankBankCode", smartroBean.bankCd);		// 매뉴얼 사이트의 '통합코드 조회-은행사 코드' 참조 후 설정해 주세요.
    		body.put("VBankAccountName", smartroBean.accntHolder);
    		
    		// 현금 영수증 관련 정보
    		// 부가세 직접 계산 가맹점의 경우 아래 값들을 각각 계산하여 설정해야 합니다.
    		body.put("CashReceiptType", "0");                   // 현금영수증 용도구분 (0: 미발행, 1 : 발행(개인 소득공제), 2 : 발행(사업자 지출증빙) 3: 자진발급)
    		if (body.get("CashReceiptType").equals("") || body.get("CashReceiptType").equals("0")) {
    			logger.info("========== ========== ========== ========== 현금영수증 미발행");
    			body.put("ReceiptAmt", smartroBean.amount);	// 현금영수증 총 금액
    			body.put("ReceiptTaxAmt", 0);					    // 현금영수증 과세
    			body.put("ReceiptTaxFreeAmt", 0);				// 현금영수증 비과세
    			body.put("ReceiptSupplyAmt", 0);                 // 현금영수증 공급가액
    			body.put("ReceiptVatAmt", 0);                     // 현금영수증 부가세
    			body.put("ReceiptIdentity", "");                    	// 현금영수증 발급번호
    		} else {
    			if (body.get("CashReceiptType").equals("1")) logger.info("========== ========== ========== ========== 현금영수증 발행(개인 소득공제");
    			if (body.get("CashReceiptType").equals("2")) logger.info("========== ========== ========== ========== 현금영수증 발행(사업자 지출증빙");
    			if (body.get("CashReceiptType").equals("3")) logger.info("========== ========== ========== ========== 현금영수증 자진발급");
    			body.put("ReceiptAmt", "1004");                     // 현금영수증 총 금액
    			body.put("ReceiptTaxAmt", "0");					    // 현금영수증 과세
    			body.put("ReceiptTaxFreeAmt", "0");				// 현금영수증 비과세
    			body.put("ReceiptSupplyAmt", "0");                 // 현금영수증 공급가액
    			body.put("ReceiptVatAmt", "0");                     // 현금영수증 부가세
    			body.put("ReceiptIdentity", "");                    // 현금영수증 발급번호
    		}
    		
    		logger.info("========== ========== ========== ========== body : " + body);
    		
    		// json 데이터 AES256 암호화
    		try {
    			snd.put("EncData", AES_Encode(body.toString(), merchantKey.substring(0, 32)));
    			snd.put("Mid", Mid);
    			logger.info("========== ========== ========== ========== snd EncData : " + snd.get("EncData"));
    			logger.info("========== ========== ========== ========== snd Mid : " + snd.get("Mid"));
    		} catch(Exception e){
    			e.printStackTrace();
    			return false;
    		}
    		logger.info("========== ========== ========== ========== snd : " + snd);
    		
    		//JSONObject resObj = connect(TargetURL + Vacct, snd);
    		JSONObject resObj = connect(vactUrl, snd);
    		logger.info("========== ========== ========== resObj : " + resObj);
    		logger.info("========== ========== ========== resObj ResultCode : " + resObj.get("ResultCode"));
    		logger.info("========== ========== ========== resObj ResultMsg : " + resObj.get("ResultMsg"));
    		//result = PayUtil.callApi(TrAuthKey, Tid);
    		if (resObj.get("ResultCode").equals("4100")) {
    			dao.initRecord();
    			dao.setTable("PG_VACCT_SMARTRO");
    			dao.setRecord("mchtId", smartroBean.mchtId);
    			dao.setRecord("van", vanMap.get("van"));
    			dao.setRecord("vanId", Mid);
    			dao.setRecord("trxId", resObj.get("Moid"));
    			dao.setRecord("accntHolder", smartroBean.accntHolder);
    			dao.setRecord("bankCd", smartroBean.bankCd);
    			dao.setRecord("expireDate", VbankExpDate);
    			dao.setRecord("amount", smartroBean.amount);
    			dao.setRecord("prdName", smartroBean.prdName);
    			dao.setRecord("prdQty", 1);
    			dao.setRecord("payerName", smartroBean.payerName);
    			dao.setRecord("payerTel", smartroBean.payerTel);
    			dao.setRecord("payerEmail", smartroBean.payerEmail);
    			
    			dao.setRecord("userId", smartroBean.userId);
    			
    			dao.setRecord("bankName", resObj.get("VbankName"));
    			dao.setRecord("account", resObj.get("VbankNum"));
    			
    			//dao.setRecord("isCashReceipt", danalBean.isCashReceiptUi);
    			// SubId
    			// VerifyValue
    			
    			dao.setRecord("vanTrxId", resObj.get("Tid"));
    			dao.setRecord("vanResultCd", resObj.get("ResultCode"));
    			dao.setRecord("vanResultMsg", resObj.get("ResultMsg"));
    			
    			dao.setRecord("regId", smartroBean.userId);
    			dao.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
    			dao.setRecord("regTime", CommonUtil.getCurrentDate("hhmmss"));
    			
    			if (dao.insert()) {
    				logger.info("========== ========== ========== ========== ========== " + resObj.get("ResultCode") + " " + resObj.get("ResultMsg") + " | DB 입력 완료");
    				logger.info("========== ========== ========== ========== SMARTRO VACCT OK");
    				//VertXMessage.set200(rc, "application/json", "NOK", "");
    				Api.sendMsg(rc, CommonUtil.toString(resObj.get("ResultCode")), "승인성공", CommonUtil.toString(resObj.get("ResultMsg")));
    				return true;
    			} else {
    				logger.info("========== ========== ========== ========== ========== " + resObj.get("ResultCode") + " " + resObj.get("ResultMsg") + " | DB 입력 실패");
    				logger.info("========== ========== ========== ========== SMARTRO VACCT FAIL");
    				//VertXMessage.set200(rc, "application/json", "NOK", "");
    				Api.sendMsg(rc, CommonUtil.toString(resObj.get("ResultCode")), "승인실패", CommonUtil.toString(resObj.get("ResultMsg")));
    				return false;
    			}
    		} else {
    			logger.info("========== ========== ========== ========== ========== " + resObj.get("ResultCode") + " " + resObj.get("ResultMsg"));
    			logger.info("========== ========== ========== ========== SMARTRO VACCT FAIL");
    			//VertXMessage.set200(rc, "application/json", "NOK", "");
    			Api.sendMsg(rc, CommonUtil.toString(resObj.get("ResultCode")), "승인실패", CommonUtil.toString(resObj.get("ResultMsg")));
    			return false;
    		}
    	}
    	return false;
    }

}