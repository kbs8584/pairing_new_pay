package com.pgmate.pay.van;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.EncryptUtil;
import com.pgmate.pay.util.PAYUNIT;

/**
 *  웰컴 페이페이먼츠 결제
 * @author Kim JongHyun
 *	2022-03-21
 */

public class Welcome implements Van {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.van.Welcome.class);

	private static String PAY_NOAUTH_DOMIN = "https://payapi.welcomepayments.co.kr/api/payment/approval"; // 211.193.35.7(test) 211.193.35.215(real)
	private static String CANCEL_DOMIN = "https://payapi.welcomepayments.co.kr/api/payment/cancel"; // 211.193.35.7(test) 211.193.35.215(real)
	
// 네트워크 에러 테스트	
	//private static String PAY_NOAUTH_DOMIN = "https://payapi.welcomepayments.co.kr/api/payment4444/approval"; // statusCode, statusBody X -> html return
//	private static String PAY_NOAUTH_DOMIN = "https://payapi.welcomepayments.co.krr/api/payment/approval"; // 211.193.35.7(test) 211.193.35.215(real)
//	private static String CANCEL_DOMIN = "https://payapi.welcomepayments.co.krrrrr/api/payment/cancel"; // 211.193.35.7(test) 211.193.35.215(real)
	
	private static String SMARTRO_PORT = "9001";
	private static String SOCKET_TIMEOUT = "120000";

	private String VAN						= "";
	private String VAN_ID 				= "";
	private String APIKEY					= "";
	private String IV_VALUE				= "04b42d4a018e0432adc578e912245554";

	public Welcome(SharedMap<String, Object> tmnVanMap) {
		VAN = tmnVanMap.getString("van");
		VAN_ID =  tmnVanMap.getString("vanId").trim();
		APIKEY	= tmnVanMap.getString("cryptoKey").trim();
		IV_VALUE = tmnVanMap.getString("secondKey").trim();

		logger.info("========== ========== ========== ========== ========== VAN SET ");
		logger.info("========== ========== ========== Welcome VAN : " + VAN);
		logger.info("========== ========== ========== Welcome MID vanId : " + VAN_ID);
		logger.info("========== ========== ========== Welcome APIKEY : " + APIKEY);
		logger.info("========== ========== ========== Welcome IV_VALUE : " + IV_VALUE);
		logger.info("========== ========== ========== ========== ========== VAN SET ");
	}

	@Override
	public synchronized SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		logger.info("========== ========== ========== ========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME BEGIN");
		String MAP_VAN = sharedMap.getString("van").trim();
		String MAP_VAN_ID = sharedMap.getString("vanId").trim();
		String MAP_APIKEY = sharedMap.getString("cryptoKey").trim();
		String MAP_IV_VALUE = sharedMap.getString("secondKey").trim();
		logger.info("========== ========== ========== ========== WELCOME VAN RESET");
		logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME MAP_VAN : " + MAP_VAN);
		logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME MAP_VAN_ID : " + MAP_VAN_ID);
		logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME MAP_APIKEY : " + MAP_APIKEY);
		logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME MAP_IV_VALUE : " + MAP_IV_VALUE);
		
		String payType = "CREDIT_CARD";										//고정
        String payMedhod = "";							//비인증 : CREDIT_UNAUTH_API / 구인증 : CREDIT_OLDAUTH_API
        String cardNo = response.pay.card.number;							//카드번호
		logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME cardNo : " + cardNo);
        cardNo = EncryptUtil.aes256Encrypt(MAP_APIKEY, MAP_IV_VALUE, cardNo);
        String card_expiry_ym = response.pay.card.expiry;					//카드 유효기간 yymm
        String millis = String.valueOf(System.currentTimeMillis());			//매 요청시마다 생성
        String order_no = response.pay.trxId;									//주문번호(매 요청시마다 생성, 매번 중복되지 않는 값으로 설정)
        String user_name = response.pay.payerName;						//구매자 이름
        String amount = String.valueOf(response.pay.amount);				//결제금액
		String prdName = "";
		try {
			if (response.pay.products != null && response.pay.products.size() > 0) {
				Product pdt = response.pay.products.get(0);
				prdName = pdt.name;
			}
		} catch (Exception e) {
			e.printStackTrace();
			response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
			sharedMap.put("vanResultCd", "XXXX");
			sharedMap.put("vanResultMsg", "WELCOME JSONParser Exception");
			return sharedMap;
		}
        String product_name = prdName;                    	            	//상품 이름
        String card_sell_mm = "";
        if (response.pay.card.installment < 10) {
        	card_sell_mm = "0" + String.valueOf(response.pay.card.installment);	//할부개월수 (일시불 : 00, 2개월 : 02, 3개월 : 03 ... 두자리숫자)
        } else {
        	card_sell_mm = String.valueOf(response.pay.card.installment);	//할부개월수 (일시불 : 00, 2개월 : 02, 3개월 : 03 ... 두자리숫자)
        }
        String echo = "";															//요청데이터 그대로 반환(필요없을시 공백으로)
     // REQ MSG
        //JSONObject obj = new JSONObject();
        Map<String, Object> obj = new HashMap<>();
        obj.put("mid", MAP_VAN_ID);
        obj.put("pay_type", payType);
        
        obj.put("card_no", cardNo);
        obj.put("card_expiry_ym", card_expiry_ym);
        obj.put("order_no", order_no);
        obj.put("user_name", user_name);
        obj.put("amount", amount);
        obj.put("product_name", product_name);
        obj.put("card_sell_mm", card_sell_mm);
        obj.put("echo", echo);
        obj.put("millis", millis);
        
        String hash_value = "";
        if (response.pay.metadata != null) {						// 구인증 비인증 체크
			if (response.pay.metadata.isTrue("cardAuth")) {
				payMedhod = "CREDIT_OLDAUTH_API";		// 구인증CREDIT_UNAUTH_API
				logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME 웰컴 구인증 결제 payMedhod : " + payMedhod);
		        try {
					//hash_value = EncryptUtil.sha256(VAN_ID + payType + payMedhod + order_no + amount + millis + APIKEY);
		        /*
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - hash_value = MID + 거래구분 + 거래번호 + 금액 + 시스템밀리초 + MAP_APIKEY");
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - MID : " + MAP_VAN_ID);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - 결제구분 : " + payType);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - 결제방법 : " + payMedhod);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - 주문번호 : " + order_no);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - 승인금액 : " + amount);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - 시스템밀리초 : " + millis);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - MAP_APIKEY : " + MAP_APIKEY);
		        */	
		        	String value = MAP_VAN_ID + payType + payMedhod + order_no + amount + millis + MAP_APIKEY;
		        //	logger.info("========== order_no trxId : " + order_no + " ========== sales() - value : " + value);
					logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME value : " + value);
					hash_value = EncryptUtil.sha256(value);
					logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME hash_value : " + hash_value);
				} catch (NoSuchAlgorithmException | UnsupportedEncodingException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
					logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME hash_value sha256 Exception : " + e2.getMessage());
					response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
					sharedMap.put("vanResultCd", "XXXX");
					sharedMap.put("vanResultMsg", "WELCOME 구인증 sha256 Exception");
					return sharedMap;
				}
				obj.put("pay_method", payMedhod);
		        obj.put("hash_value", hash_value);
				
				String authPw = "";
				try {
					//hash_value = EncryptUtil.sha256(VAN_ID + payType + payMedhod + order_no + amount + millis + APIKEY);
					logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME authPw 구인증 비밀번호 : " + response.pay.metadata.getString("authPw"));
		        	authPw = EncryptUtil.aes256Encrypt(MAP_APIKEY, MAP_IV_VALUE, response.pay.metadata.getString("authPw"));
		        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME authPw Enc : " + authPw);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
		        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME authPw aes256Encrypt Exception : " + e.getMessage());
					response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
					sharedMap.put("vanResultCd", "XXXX");
					sharedMap.put("vanResultMsg", "WELCOME 구인증 authPw aes256Encrypt Exception");
					return sharedMap;
				}
				String authDob = "";
				try {
					//hash_value = EncryptUtil.sha256(VAN_ID + payType + payMedhod + order_no + amount + millis + MAP_APIKEY);
		        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME authDob 구인증 생년월일 : " + response.pay.metadata.getString("authDob"));
					authDob = EncryptUtil.aes256Encrypt(MAP_APIKEY, MAP_IV_VALUE, response.pay.metadata.getString("authDob"));
		        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME authDob Enc : " + authDob);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
		        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME authDob aes256Encrypt Exception : " + e.getMessage());
					response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
					sharedMap.put("vanResultCd", "XXXX");
					sharedMap.put("vanResultMsg", "WELCOME 구인증 authDob aes256Encrypt Exception");
					return sharedMap;
				}
				obj.put("card_pw", authPw);
				obj.put("card_holder_ymd", authDob);
			} else {
				payMedhod = "CREDIT_UNAUTH_API";		// 비인증
	        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME 웰컴 비인증 결제 - metadata NULL | payMedhod : " + payMedhod);
				try {
				/*
					//hash_value = EncryptUtil.sha256(VAN_ID + payType + payMedhod + order_no + amount + millis + MAP_APIKEY);
					logger.info("========== order_no trxId : " + order_no + " ========== sales() - metadata : " + response.pay.metadata);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - hash_value = MID + 거래구분 + 거래번호 + 금액 + 시스템밀리초 + MAP_APIKEY");
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - MID : " + MAP_VAN_ID);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - 결제구분 : " + payType);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - 결제방법 : " + payMedhod);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - 주문번호 : " + order_no);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - 승인금액 : " + amount);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - 시스템밀리초 : " + millis);
		        	logger.info("========== order_no trxId : " + order_no + " ========== sales() - APIKEY : " + MAP_APIKEY);
		        */	
		        	String value = MAP_VAN_ID + payType + payMedhod + order_no + amount + millis + MAP_APIKEY;
		        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME value : " + value);
					hash_value = EncryptUtil.sha256(value);
		        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME hash_value : " + hash_value);
				} catch (NoSuchAlgorithmException | UnsupportedEncodingException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
					logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME hash_value aes256Encrypt Exception : " + e2.getMessage());
					response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
					sharedMap.put("vanResultCd", "XXXX");
					sharedMap.put("vanResultMsg", "WELCOME 비인증 sha256 Exception");
					return sharedMap;
				}
				obj.put("pay_method", payMedhod);
		        obj.put("hash_value", hash_value);
			}
		} else {
			payMedhod = "CREDIT_UNAUTH_API";		// 비인증
        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME 웰컴 비인증 결제 - metadata NULL | payMedhod : " + payMedhod);
			 try {
					//hash_value = EncryptUtil.sha256(MAP_VAN_ID + payType + payMedhod + order_no + amount + millis + APIKEY);
			/*	 
				logger.info("========== order_no trxId : " + order_no + " ========== sales() - metadata : " + response.pay.metadata);
				logger.info("========== order_no trxId : " + order_no + " ========== sales() - hash_value = MID + 거래구분 + 거래번호 + 금액 + 시스템밀리초 + MAP_APIKEY");
				logger.info("========== order_no trxId : " + order_no + " ========== sales() - MID : " + MAP_VAN_ID);
				logger.info("========== order_no trxId : " + order_no + " ========== sales() - 결제구분 : " + payType);
				logger.info("========== order_no trxId : " + order_no + " ========== sales() - 결제방법 : " + payMedhod);
				logger.info("========== order_no trxId : " + order_no + " ========== sales() - 주문번호 : " + order_no);
				logger.info("========== order_no trxId : " + order_no + " ========== sales() - 승인금액 : " + amount);
				logger.info("========== order_no trxId : " + order_no + " ========== sales() - 시스템밀리초 : " + millis);
				logger.info("========== order_no trxId : " + order_no + " ========== sales() - APIKEY : " + MAP_APIKEY);
			*/	
				String value = MAP_VAN_ID + payType + payMedhod + order_no + amount + millis + MAP_APIKEY;
	        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME value : " + value);
				hash_value = EncryptUtil.sha256(value);
	        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME hash_value : " + hash_value);
				} catch (NoSuchAlgorithmException | UnsupportedEncodingException e2) {
					// TODO Auto-generated catch block
				e2.printStackTrace();
				logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME hash_value aes256Encrypt Exception : " + e2.getMessage());
				response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
				sharedMap.put("vanResultCd", "XXXX");
				sharedMap.put("vanResultMsg", "WELCOME 비인증 sha256 Exception");
				return sharedMap;
			}
			obj.put("pay_method", payMedhod);
	        obj.put("hash_value", hash_value);
		}
        String results = "";
        if (obj != null) logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME Req_Msg : " + obj.toString());

        sharedMap.put("van", MAP_VAN);
		sharedMap.put("vanId", MAP_VAN_ID);
		logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME van : " + MAP_VAN + " - vanId : " + MAP_VAN_ID);
		
/*
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(PAY_NOAUTH_DOMIN);
        httpPost.addHeader("Content-Type", "application/json");
        StringEntity requestEntity = new StringEntity(obj.toString(), ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntity);
        HttpResponse res = null;
		try {
			res = client.execute(httpPost);
			logger.info("Response Code : " + res.getStatusLine().getStatusCode());
			//logger.info("Response res toString : " + res.toString());
			
			if (res.getStatusLine().getStatusCode() != 200) {
				logger.info("========== ========== ========== ========== WELCOME CONNECT getStatusCode " + res.getStatusLine().getStatusCode());
				logger.info("시스템 장애 응답 구분값 없음. :{}", res.getStatusLine().getStatusCode());
				response.result = ResultUtil.getResult("XXXX", "실패", "통신장애");
				sharedMap.put("vanResultCd", "XXXX");
				sharedMap.put("vanResultMsg", "통신장애 : " + res.getStatusLine().getStatusCode());
				return sharedMap;
			}
		} catch (Exception e) {
			logger.info("========== ========== ========== ========== WELCOME CONNECT EXCEPTION ERROR");
			e.printStackTrace();
			logger.info("========== ========== ========== ========== WELCOME CONNECT EXCEPTION ERROR : " + e);
			logger.info("시스템 장애 응답 구분값 없음. :{}", res.getStatusLine().getStatusCode());
			response.result = ResultUtil.getResult("XXXX", "실패", "통신장애");
			sharedMap.put("vanResultCd", "XXXX");
			sharedMap.put("vanResultMsg", "통신장애 : " + res.getStatusLine().getStatusCode());
			return sharedMap;
		}
		
        BufferedReader rd = null;
		try {
			rd = new BufferedReader(new InputStreamReader(res.getEntity().getContent(), StandardCharsets.UTF_8));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.info("========== ========== ========== ========== WELCOME BufferedReader InputStreamReader Exception : " + e.getMessage());
			logger.info("========== ========== ========== ========== WELCOME BufferedReader InputStreamReader Exception : " + e);
			response.result = ResultUtil.getResult("XXXX", "실패", e.toString());
			sharedMap.put("van", VAN);
			sharedMap.put("vanId", VAN_ID);
			sharedMap.put("vanResultCd", "XXXX");
			sharedMap.put("vanResultMsg", "실패 : WELCOME BufferedReader InputStreamReader Exception");
			return sharedMap;
		}
        StringBuilder result = new StringBuilder();
        String line = "";
        try {
			while ((line = rd.readLine()) != null)
			    result.append(line);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.info("========== ========== ========== ========== WELCOME StringBuilder append Exception Exception : " + e.getMessage());
			logger.info("========== ========== ========== ========== WELCOME StringBuilder append Exception : " + e);
			response.result = ResultUtil.getResult("XXXX", "실패", "실패");
			sharedMap.put("van", VAN);
			sharedMap.put("vanId", VAN_ID);
			sharedMap.put("vanResultCd", "XXXX");
			sharedMap.put("vanResultMsg", "실패 : WELCOME StringBuilder append Exception");
			return sharedMap;
		}
        results = result.toString();
        logger.info("results : " + results);
        
        JSONParser connParser = new JSONParser();
		Object connObj = new Object();
		try {
			connObj = connParser.parse(results);
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("========== ========== ========== ========== WELCOME JSONParser Exception : " + e.getMessage());
			logger.info("========== ========== ========== ========== WELCOME JSONParser Exception : " + e);
			response.result = ResultUtil.getResult("XXXX", "실패", "실패");
			sharedMap.put("van", VAN);
			sharedMap.put("vanId", VAN_ID);
			sharedMap.put("vanResultCd", "XXXX");
			sharedMap.put("vanResultMsg", "WELCOME JSONParser Exception");
		}
		JSONObject connjObj = (JSONObject) connObj;
		if (connjObj.get("result_code").equals("0000")) {
			response.result = ResultUtil.getResult("0000", "정상", "정상승인");
			response.pay.authCd = (String) connjObj.get("approval_no");
			sharedMap.put("vanTrxId", connjObj.get("transaction_no"));
			sharedMap.put("vanResultCd", "0000");
			sharedMap.put("vanResultMsg", "정상승인");
			sharedMap.put("authCd", connjObj.get("approval_no"));
			sharedMap.put("vanDate", connjObj.get("approval_ymdhms"));
			sharedMap.put("cardAcquirer", connjObj.get("card_name"));
			
		} else if (!connjObj.get("result_code").equals("XXXX")) {
			//String vanMessage = connjObj.get("ErrorCD") + " " + connjObj.get("result_message");
			// ErrorCD = null
			String vanMessage = (String) connjObj.get("result_message");
			response.result = ResultUtil.getResult((String) connjObj.get("result_code"), "승인실패", vanMessage);
			sharedMap.put("vanTrxId", CommonUtil.nToB((String) connjObj.get("TID")));
			sharedMap.put("vanResultCd", CommonUtil.nToB((String) connjObj.get("result_code")));
			sharedMap.put("vanResultMsg", vanMessage);
			String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
			sharedMap.put("vanDate", curDate);
		} else {
			logger.info("시스템 장애 응답 구분값 없음. :{}", connjObj.get("result_code"));
			response.result = ResultUtil.getResult("XXXX", "실패", "통신장애");
			sharedMap.put("vanTrxId", CommonUtil.nToB((String) connjObj.get("TID")));
			sharedMap.put("vanResultCd", CommonUtil.nToB((String) connjObj.get("result_code")));
			sharedMap.put("vanResultMsg", CommonUtil.nToB((String) connjObj.get("result_message")));
		}
		logger.info("========== ========== ========== ========== van sharedMap : " + sharedMap.getString("van") + " / vanId : " + sharedMap.getString("vanId"));
		logger.info("========== ========== ========== ========== vanTrxId : : " + sharedMap.getString("vanTrxId") + " / vanDate : " + sharedMap.getString("vanDate"));
		logger.info("========== ========== ========== ========== Welcome - hash_value : " + hash_value);
		logger.info("========== ========== ========== ========== Welcome - hash_value sharedMap : " + sharedMap.get("hashValue"));
		logger.info("");
*/

	
		ResponseEntity<String> res = null; 
		String statusArr[] = null;
		String statusCode = "";
		String statusMessage = "";
		String res_body = "";
        try {
        	res = apiExchange(PAY_NOAUTH_DOMIN, getJsonString(obj));
	        //Response
        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() res : " + res);
        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() Status : " + res.getStatusCode());
        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() body : " + res.getBody());
        	statusArr = res.getStatusCode().toString().split(" ");
        	statusCode = statusArr[0];
        	statusMessage = statusArr[1];
        	statusMessage = statusArr[1];
        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() statusCode : " + statusCode);
        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() statusMessage : " + statusMessage);
        	//logger.info("body :" + res.getBody());
	    } catch (HttpClientErrorException e) {
	        // HttpStatusCode 400 오류
	    	e.printStackTrace();
        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() HttpClientErrorException : " + e.getMessage());
        	res_body = e.getResponseBodyAsString();
        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() HttpClientErrorException : getResponseBodyAsString : " + res_body);
        	JSONParser connParser = new JSONParser();
    		Object connObj = new Object();
    		try {
    			if (statusCode.equals("200")) {
    				connObj = connParser.parse(res.getBody());
    			} else {
    				connObj = connParser.parse(res_body);
    			}
    		} catch (Exception e1) {
    			e.printStackTrace();
            	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() HttpClientErrorException - JSONParser Exception : " + e1);
    			response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
    			sharedMap.put("vanResultCd", "XXXX");
    			sharedMap.put("vanResultMsg", "WELCOME JSONParser Exception");
    			return sharedMap;
    		}
    		JSONObject connjObj = (JSONObject) connObj;
    		if (connjObj.get("result_code") != null) {
    			statusCode = (String) connjObj.get("result_code");
    		}
    		if (connjObj.get("result_message") != null) {
    			statusMessage = (String) connjObj.get("result_message");
    		}
        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() HttpClientErrorException - statusCode : " + statusCode);
        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() HttpClientErrorException - statusMessage : " + statusMessage);
			response.result = ResultUtil.getResult(statusCode, "승인실패", statusMessage);
			sharedMap.put("vanResultCd", statusCode);
			sharedMap.put("vanResultMsg", statusMessage);
			return sharedMap;
	    } catch (HttpServerErrorException e) {
	        // HttpStatusCode 500 오류logger.warn("HttpClientErrorException :" + e);
	    	e.printStackTrace();
	    	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() HttpServerErrorException : " + e.getMessage());
        	res_body = e.getResponseBodyAsString();
        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() HttpServerErrorException : getResponseBodyAsString : " + res_body);
        	JSONParser connParser = new JSONParser();
    		Object connObj = new Object();
    		try {
    			if (statusCode.equals("200")) {
    				connObj = connParser.parse(res.getBody());
    			} else {
    				connObj = connParser.parse(res_body);
    			}
    		} catch (Exception e1) {
    			e.printStackTrace();
    			logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() HttpServerErrorException : JSONParser Exception : " + e1.getMessage());
    			response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
    			sharedMap.put("vanResultCd", "XXXX");
    			sharedMap.put("vanResultMsg", "WELCOME JSONParser Exception");
    			return sharedMap;
    		}
    		JSONObject connjObj = (JSONObject) connObj;
    		if (connjObj.get("result_code") != null) {
    			statusCode = (String) connjObj.get("result_code");
    		}
    		if (connjObj.get("result_code") != null) {
    			statusMessage = (String) connjObj.get("result_message");
    		}
    		logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() HttpServerErrorException : statusCode : " + statusCode);
    		logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() HttpServerErrorException : statusMessage : " + statusMessage);
    		response.result = ResultUtil.getResult(statusCode, "승인실패", statusMessage);
			sharedMap.put("vanResultCd", statusCode);
			sharedMap.put("vanResultMsg", statusMessage);
			return sharedMap;
	    } catch (Exception e) {
	    	e.getStackTrace();
	    	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() Exception : " + e.getMessage());
        	
        	// 2022-07-26 - 가맹점, PG 무응답 로그 기록
        	trxDAO.updateTransactionError(order_no, hash_value, 0, 1);
        	
        	res_body = ((RestClientResponseException) e).getResponseBodyAsString();
        	JSONParser connParser = new JSONParser();
    		Object connObj = new Object();
    		try {
    			if (statusCode.equals("200")) {
    				connObj = connParser.parse(res.getBody());
    			} else {
    				connObj = connParser.parse(res_body);
    			}
    		} catch (Exception e1) {
    			e.printStackTrace();
    	    	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() Exception : JSONParser Exception : " + e1.getMessage());
    			response.result = ResultUtil.getResult("XXXX", "실패", "통신장애");
    			sharedMap.put("vanResultCd", "XXXX");
    			sharedMap.put("vanResultMsg", "WELCOME 통신장애");
    			return sharedMap;
    		}
    		JSONObject connjObj = (JSONObject) connObj;
    		if (connjObj.get("result_code") != null) {
    			statusCode = (String) connjObj.get("result_code");
    		}
    		if (connjObj.get("result_code") != null) {
    			statusMessage = (String) connjObj.get("result_message");
    		}
    		logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() Exception : statusCode : " + statusCode);
    		logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME apiExchange() Exception : statusMessage : " + statusMessage);
			response.result = ResultUtil.getResult("XXXX", "실패", "통신장애");
			sharedMap.put("vanResultCd", statusCode);
			sharedMap.put("vanResultMsg", statusMessage);
			return sharedMap;
	    }
		
		if (res != null) logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME res : " + res.toString());
		else if (res != null) logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME res IS NULL");
        
        JSONParser connParser = new JSONParser();
		Object connObj = new Object();
		try {
			if (statusCode.equals("200")) {
				if (res.getBody() != null) {
					connObj = connParser.parse(res.getBody());
					logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME connObj = connParser.parse(res.getBody()); - res.getBody() : " + res.getBody());
				}
			} else {
				connObj = connParser.parse(res_body);
	    		logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME connObj = connParser.parse(res.getBody()); - res_body : " + res_body);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME connObj = connParser.parse(res.getBody()); -  JSONParser Exception : " + e.getMessage());
			response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
			sharedMap.put("vanResultCd", "XXXX");
			sharedMap.put("vanResultMsg", "WELCOME JSONParser Exception");
			return sharedMap;
		}
		JSONObject connjObj = (JSONObject) connObj;
    	if (connjObj.get("result_code").equals("0000")) {
			response.result = ResultUtil.getResult("0000", "정상", "정상승인");
			response.pay.authCd = (String) connjObj.get("approval_no");
			sharedMap.put("vanTrxId", connjObj.get("transaction_no"));
			sharedMap.put("vanResultCd", "0000");
			sharedMap.put("vanResultMsg", "정상승인");
			sharedMap.put("authCd", connjObj.get("approval_no"));
			sharedMap.put("vanDate", connjObj.get("approval_ymdhms"));
			sharedMap.put("cardAcquirer", connjObj.get("card_name"));
			
		} else if (!connjObj.get("result_code").equals("XXXX")) {
			//String vanMessage = connjObj.get("ErrorCD") + " " + connjObj.get("result_message");
			// ErrorCD = null
			String vanMessage = (String) connjObj.get("result_message");
			response.result = ResultUtil.getResult((String) connjObj.get("result_code"), "승인실패", vanMessage);
			sharedMap.put("vanTrxId", CommonUtil.nToB((String) connjObj.get("TID")));
			sharedMap.put("vanResultCd", CommonUtil.nToB((String) connjObj.get("result_code")));
			sharedMap.put("vanResultMsg", vanMessage);
			String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
			sharedMap.put("vanDate", curDate);
		} else {
			logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME 시스템 장애 응답 구분값 없음. :{} " + connjObj.get("result_code"));
			statusArr = res.getStatusCode().toString().split(" ");
        	statusCode = statusArr[0];
        	statusMessage = statusArr[1];
        	logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME 시스템 장애 응답 구분값 없음. : statusCode. : " + statusCode);
			logger.info("========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME 시스템 장애 응답 구분값 없음. : statusMessage. : " + statusMessage);
			response.result = ResultUtil.getResult("XXXX", "실패", "통신장애");
			sharedMap.put("vanTrxId", CommonUtil.nToB((String) connjObj.get("TID")));
			sharedMap.put("vanResultCd", statusCode);
			sharedMap.put("vanResultMsg", statusMessage);
		}
    	logger.info("========== ========== ========== ========== ========== sales() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME END");
		
		return sharedMap;
	}

	@Override  
	public synchronized SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) { 
		logger.info("========== ========== ========== ========== ========== refund() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME BEGIN");
		String payType = "CREDIT_CARD";										//고정
        String transactionType = "";												
        if (sharedMap.isEquals("rfdAll", "부분")) {								//거래구분
        	transactionType = "PART_CANCEL";
        } else {
        	transactionType = "CANCEL";
        }
        String userId = response.refund.mchtId;								//회원아이디
        String transactionNo = payMap.getString("vanTrxId");						//거래번호
        String amount = String.valueOf(response.refund.amount);				//금액
        String cancelReason = "결제취소 처리";													//취소사유
        String ipAddress = sharedMap.getString("remoteIp");					//IP주소
        String millis = String.valueOf(System.currentTimeMillis());			//매 요청시마다 생성
        String hash_value = "";
        try {
        /*
        	System.out.println();
        	logger.info("========== vanTrxId : " + payMap.getString("vanTrxId") + " ========== refund() - hash_value = MID + 거래구분 + 거래번호 + 금액 + 시스템밀리초 + APIKEY");
        	logger.info("========== vanTrxId : " + payMap.getString("vanTrxId") + " ========== refund() - MID : " + VAN_ID);
        	logger.info("========== vanTrxId : " + payMap.getString("vanTrxId") + " ========== refund() - 거래구분" + transactionType);
        	logger.info("========== vanTrxId : " + payMap.getString("vanTrxId") + " ========== refund() - 거래번호" + transactionNo);
        	logger.info("========== vanTrxId : " + payMap.getString("vanTrxId") + " ========== refund() - 금액 : " + amount);
        	logger.info("========== vanTrxId : " + payMap.getString("vanTrxId") + " ========== refund() - 시스템밀리초 : " + millis);
        	logger.info("========== vanTrxId : " + payMap.getString("vanTrxId") + " ========== refund() - APIKEY : " + APIKEY);
        */
        	String value = VAN_ID + transactionType + transactionNo + amount + millis + APIKEY;
        	logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME 암호화할 문자열 value : " + value);
			//hash_value = EncryptUtil.sha256(VAN_ID + transactionType + transactionNo + amount + millis + APIKEY);
			hash_value = EncryptUtil.sha256(value);
			logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME 암호화할 문자열 hash_value : " + hash_value);
			System.out.println();
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME Exception : " + e2.getMessage());
			response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
			sharedMap.put("vanResultCd", "XXXX");
			sharedMap.put("vanResultMsg", "WELCOME Exception : " + e2.getMessage());
			return sharedMap;
		}
        
        // REQ MSG
        JSONObject obj = new JSONObject();
        obj.put("mid", VAN_ID);
        obj.put("pay_type", payType);
        obj.put("transaction_type", transactionType);
        obj.put("user_id", userId);
        obj.put("transaction_no", transactionNo);
        obj.put("amount", amount);
        obj.put("cancel_reason", cancelReason);
        obj.put("ip_address", ipAddress);
        obj.put("millis", millis);
        obj.put("hash_value", hash_value);

        String results = "";
        if (obj != null) logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME Req_Msg : " + obj.toString());
        
/*
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(CANCEL_DOMIN);
        httpPost.addHeader("Content-Type", "application/json");
        StringEntity requestEntity = new StringEntity(obj.toString(), ContentType.APPLICATION_JSON);
        httpPost.setEntity(requestEntity);
        HttpResponse res = null;
		try {
			res = client.execute(httpPost);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        logger.info("Response Code : " + res.getStatusLine().getStatusCode());
        BufferedReader rd = null;
		try {
			rd = new BufferedReader(new InputStreamReader(res.getEntity().getContent(), StandardCharsets.UTF_8));
		} catch (UnsupportedOperationException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        StringBuilder result = new StringBuilder();
        String line = "";
        try {
			while ((line = rd.readLine()) != null)
			    result.append(line);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        results = result.toString();
        logger.info("results : " + results);
        
        JSONParser connParser = new JSONParser();
		Object connObj = new Object();
		try {
			connObj = connParser.parse(results);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		JSONObject connjObj = (JSONObject) connObj;
		
		// 응답
		// {"result_code":"0000","result_message":"정상","pay_type":"CREDIT_CARD","transaction_no":"202203225654432","amount":"1004","order_no":"T220322011655","cancel_amount":"1004","remain_amount":"0","cancel_ymdhms":"20220323184234"}
		
		if (connjObj.get("result_code").equals("0000")) {
			response.result = ResultUtil.getResult("0000", "정상", "정상승인");
			sharedMap.put("vanTrxId", connjObj.get("transaction_no"));
			sharedMap.put("vanResultCd", "0000");
			sharedMap.put("vanResultMsg", "정상승인");
			sharedMap.put("vanDate", connjObj.get("cancel_ymdhms"));
			
		} else if (!connjObj.get("result_code").equals("XXXX")) {
			String vanMessage = connjObj.get("result_code") + " : " + connjObj.get("result_message");
			response.result = ResultUtil.getResult((String) connjObj.get("result_code"), "승인실패", vanMessage);
			sharedMap.put("vanTrxId", CommonUtil.nToB((String) connjObj.get("transaction_no")));
			sharedMap.put("vanResultCd", CommonUtil.nToB((String) connjObj.get("result_code")));
			sharedMap.put("vanResultMsg", vanMessage);
			String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
			sharedMap.put("vanDate", curDate);
		} else {
			logger.info("시스템 장애 응답 구분값 없음. :{}", connjObj.get("result_code"));
			response.result = ResultUtil.getResult("XXXX", "실패", "통신장애");
			sharedMap.put("vanTrxId", CommonUtil.nToB((String) connjObj.get("transaction_no")));
			sharedMap.put("vanResultCd", CommonUtil.nToB((String) connjObj.get("result_code")));
			sharedMap.put("vanResultMsg", "시스템 통신장애 응답");
		}
		
		sharedMap.put("van", payMap.getString("van"));
		sharedMap.put("vanId", VAN_ID);
		logger.info("van : " + payMap.getString("van") + " / vanId : " + VAN_ID);
		logger.info("van sharedMap : " + sharedMap.getString("van") + " / vanId : " + sharedMap.getString("vanId"));
		logger.info("vanTrxId : : " + sharedMap.getString("vanTrxId") + " / vanDate : " + sharedMap.getString("vanDate"));
		logger.info("");

*/
        ResponseEntity<String> res = null; 
		String statusArr[] = null;
		String statusCode = "";
		String statusMessage = "";
		String res_body = "";
        try {
        	res = apiExchange(CANCEL_DOMIN, getJsonString(obj));
	        //Response
        	logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME res : " + res.toString());
        	logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME Status : " + res.getStatusCode());
        	logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME body : " + res.getBody());
        	statusArr = res.getStatusCode().toString().split(" ");
        	statusCode = statusArr[0];
        	statusMessage = statusArr[1];
        	statusMessage = statusArr[1];
        	logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME statusCode : " + statusCode);
        	logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME statusMessage : " + statusMessage);
        	//logger.info("body :" + res.getBody());
	    } catch (HttpClientErrorException e) {
	        // HttpStatusCode 400 오류
	    	logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - HttpClientErrorException : " + e.getMessage());
        	res_body = e.getResponseBodyAsString();
        	logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - HttpClientErrorException : getResponseBodyAsString : " + res_body);
        	JSONParser connParser = new JSONParser();
    		Object connObj = new Object();
    		try {
    			if (statusCode.equals("200")) {
    				connObj = connParser.parse(res.getBody());
    			} else {
    				connObj = connParser.parse(res_body);
    			}
    		} catch (Exception e1) {
    			e.printStackTrace();
    			logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - HttpClientErrorException : Exception : " + e1.getMessage());
    			response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
    			sharedMap.put("vanResultCd", "XXXX");
    			sharedMap.put("vanResultMsg", "WELCOME JSONParser Exception");
    			return sharedMap;
    		}
    		JSONObject connjObj = (JSONObject) connObj;
    		if (connjObj.get("result_code") != null) {
    			statusCode = (String) connjObj.get("result_code");
    		}
    		if (connjObj.get("result_message") != null) {
    			statusMessage = (String) connjObj.get("result_message");
    		}
    		logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - HttpClientErrorException : statusCode : " + statusCode);
    		logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - HttpClientErrorException : statusMessage : " + statusMessage);
			response.result = ResultUtil.getResult(statusCode, "승인실패", statusMessage);
			sharedMap.put("vanResultCd", statusCode);
			sharedMap.put("vanResultMsg", statusMessage);
			return sharedMap;
	    } catch (HttpServerErrorException e) {
	        // HttpStatusCode 500 오류logger.warn("HttpClientErrorException :" + e);
	    	logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - HttpServerErrorException : " + e.getMessage());
        	res_body = e.getResponseBodyAsString();
        	logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - HttpServerErrorException : getResponseBodyAsString : " + res_body);
        	JSONParser connParser = new JSONParser();
    		Object connObj = new Object();
    		try {
    			if (statusCode.equals("200")) {
    				connObj = connParser.parse(res.getBody());
    			} else {
    				connObj = connParser.parse(res_body);
    			}
    		} catch (Exception e1) {
    			e.printStackTrace();
    			logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - HttpServerErrorException : Exception : " + e1.getMessage());
    			response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
    			sharedMap.put("vanResultCd", "XXXX");
    			sharedMap.put("vanResultMsg", "WELCOME JSONParser Exception");
    			return sharedMap;
    		}
    		JSONObject connjObj = (JSONObject) connObj;
    		if (connjObj.get("result_code") != null) {
    			statusCode = (String) connjObj.get("result_code");
    		}
    		if (connjObj.get("result_code") != null) {
    			statusMessage = (String) connjObj.get("result_message");
    		}
    		logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - HttpServerErrorException : statusCode : " + statusCode);
    		logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - HttpServerErrorException : statusMessage : " + statusMessage);
    		response.result = ResultUtil.getResult(statusCode, "승인실패", statusMessage);
			sharedMap.put("vanResultCd", statusCode);
			sharedMap.put("vanResultMsg", statusMessage);
			return sharedMap;
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - Exception : " + e.getMessage());
        	
        	// 2022-07-26 - 가맹점, PG 무응답 로그 기록
        	trxDAO.updateTransactionError(payMap.getString("trxId"), hash_value, 0, 1);
        	
        	res_body = ((RestClientResponseException) e).getResponseBodyAsString();
        	JSONParser connParser = new JSONParser();
    		Object connObj = new Object();
    		try {
    			if (statusCode.equals("200")) {
    				connObj = connParser.parse(res.getBody());
    			} else {
    				connObj = connParser.parse(res_body);
    			}
    		} catch (Exception e1) {
    			e1.printStackTrace();
    			logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - Exception - Exception : " + e1.getMessage());
    			response.result = ResultUtil.getResult("XXXX", "실패", "통신장애");
    			sharedMap.put("vanResultCd", "XXXX");
    			sharedMap.put("vanResultMsg", "WELCOME 통신장애");
    			return sharedMap;
    		}
    		JSONObject connjObj = (JSONObject) connObj;
    		if (connjObj.get("result_code") != null) {
    			statusCode = (String) connjObj.get("result_code");
    		}
    		if (connjObj.get("result_code") != null) {
    			statusMessage = (String) connjObj.get("result_message");
    		}
    		logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - Exception : statusCode : " + statusCode);
    		logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME apiExchange() - Exception : statusMessage : " + statusMessage);
			response.result = ResultUtil.getResult("XXXX", "실패", "통신장애");
			sharedMap.put("vanResultCd", statusCode);
			sharedMap.put("vanResultMsg", statusMessage);
			return sharedMap;
	    }
        
        if (res != null) logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME res : " + res.toString());
        else logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME res IS NULL");
        
        JSONParser connParser = new JSONParser();
		Object connObj = new Object();
		try {
			if (statusCode.equals("200")) {
				connObj = connParser.parse(res.getBody());
			} else {
				connObj = connParser.parse(res_body);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME JSONParser Exception : " + e.getMessage());
			response.result = ResultUtil.getResult("XXXX", "실패", "승인실패");
			sharedMap.put("vanResultCd", "XXXX");
			sharedMap.put("vanResultMsg", "WELCOME JSONParser Exception");
			return sharedMap;
		}
		JSONObject connjObj = (JSONObject) connObj;
		if (connjObj != null) logger.info("========== ========== refund() - vanTrxId : " + payMap.getString("vanTrxId") + " - WELCOME connjObj : " + connjObj.toString());
		
		if (connjObj.get("result_code").equals("0000")) {
			response.result = ResultUtil.getResult("0000", "정상", "정상승인");
			sharedMap.put("vanTrxId", connjObj.get("transaction_no"));
			sharedMap.put("vanResultCd", "0000");
			sharedMap.put("vanResultMsg", "정상승인");
			sharedMap.put("vanDate", connjObj.get("cancel_ymdhms"));
		} else if (!connjObj.get("result_code").equals("XXXX")) {
			String vanMessage = connjObj.get("result_code") + " : " + connjObj.get("result_message");
			response.result = ResultUtil.getResult((String) connjObj.get("result_code"), "승인실패", vanMessage);
			sharedMap.put("vanTrxId", CommonUtil.nToB((String) connjObj.get("transaction_no")));
			sharedMap.put("vanResultCd", CommonUtil.nToB((String) connjObj.get("result_code")));
			sharedMap.put("vanResultMsg", vanMessage);
			String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
			sharedMap.put("vanDate", curDate);
		} else {
			logger.info("시스템 장애 응답 구분값 없음. :{}", connjObj.get("result_code"));
			response.result = ResultUtil.getResult("XXXX", "실패", "통신장애");
			sharedMap.put("vanTrxId", CommonUtil.nToB((String) connjObj.get("transaction_no")));
			sharedMap.put("vanResultCd", CommonUtil.nToB((String) connjObj.get("result_code")));
			sharedMap.put("vanResultMsg", "시스템 통신장애 응답");
		}
		logger.info("========== ========== ========== ========== ========== refund() - trxId : " + sharedMap.get(PAYUNIT.TRX_ID) + " - WELCOME END");
		sharedMap.put("van", payMap.getString("van"));
		sharedMap.put("vanId", VAN_ID);
		logger.info("van : " + payMap.getString("van") + " / vanId : " + VAN_ID);
		logger.info("van sharedMap : " + sharedMap.getString("van") + " / vanId : " + sharedMap.getString("vanId"));
		logger.info("vanTrxId : : " + sharedMap.getString("vanTrxId") + " / vanDate : " + sharedMap.getString("vanDate"));
		logger.info("");
		
		return sharedMap;
	}
	
	// 2022-06-20
	//API 호출부분
    public ResponseEntity<String> apiExchange(String url, String body){
    	logger.info("========== ========== ========== ========== apiExchange() - BEGIN");
    	logger.info("========== ========== ========== ========== apiExchange() - url : " + url);
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

        logger.debug("========== ========== ========== ========== apiExchange() headers : " + headers);
        
       ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
       logger.debug("========== ========== ========== ========== apiExchange Exception - entity : \n" + entity.toString());
       if (response != null) logger.debug("========== ========== ========== ========== apiExchange Exception - response NOT NULL : " + response);
       else if (response == null) logger.debug("========== ========== ========== ========== apiExchange Exception - response NULL");
       System.out.println();       
/*        
        ResponseEntity<String> response = new ResponseEntity<String>(headers, HttpStatus.OK);
        try {
        	//ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        	response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
			// TODO: handle exception
        	logger.debug("========== ========== ========== ========== apiExchange Exception - entity : " + entity.toString());
        	if (response != null) logger.debug("========== ========== ========== ========== apiExchange Exception - response : " + response.toString());
        	else if (response == null) logger.debug("========== ========== ========== ========== apiExchange Exception - response NULL");
        	e.printStackTrace();
		}
*/
       logger.info("========== ========== ========== ========== apiExchange() - END");
        return response;
    }

	public static String getJsonString(Map<String, Object> map) {
        JSONObject jsonObject = new JSONObject();
        map.forEach((key, value) -> jsonObject.put(key, value));
        logger.info("========== ========== ========== ========== getJsonString() - jsonObject.toJSONString : " + jsonObject.toJSONString());
        return jsonObject.toJSONString();
    }
	
	public ResponseEntity<String> select(String mid, String apiKey, String orderNo) {
		logger.info("========== ========== ========== ========== select() - mid : " + mid + " | apiKey : " + apiKey + " | orderNo : " + orderNo);
		ResponseEntity<String> response = null;
        try {
            String selectApiUrl = "https://payapi.welcomepayments.co.kr/api/search/order";
            logger.info("========== ========== ========== ========== select() - selectApiUrl : " + selectApiUrl);
            response = apiExchange(selectApiUrl, getSelectParameter(mid, apiKey, orderNo));
            //Response
			logger.info("========== ========== ========== ========== select() - Status :" + response.getStatusCode());
			logger.info("========== ========== ========== ========== select() - body :" + response.getBody());
        } catch (HttpClientErrorException e) {
            // HttpStatusCode 400 오류
        	logger.info("========== ========== ========== ========== select() - HttpClientErrorException :" + e.getMessage());
        	e.printStackTrace();
            logger.info("========== ========== ========== ========== select() - Status :" + e.getStatusCode());
            logger.info("========== ========== ========== ========== select() - body :" + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            // HttpStatusCode 500 오류
        	logger.info("========== ========== ========== ========== select() - HttpServerErrorException :" + e.getMessage());
        	e.printStackTrace();
            logger.info("========== ========== ========== ========== select() - Status :" + e.getStatusCode());
            logger.info("========== ========== ========== ========== select() - body :" + e.getResponseBodyAsString());
        } catch (Exception e) {
            //exception
        	logger.info("========== ========== ========== ========== select() - HttpServerErrorException :" + e.getMessage());
        	e.printStackTrace();
        }
        return response;
    }
	
	 public String getSelectParameter(String mid, String apiKey, String orderNo) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        //String mid = "";                                            //MID(제공)
        //String apiKey = "";                                         //APIKEY(제공)
        //String orderNo = "";                                      //주문번호
        String hash_value = EncryptUtil.sha256(mid + orderNo + apiKey);

        Map<String, Object> param = new HashMap<>();
        param.put("mid", mid);
        param.put("order_no", orderNo);
        param.put("hash_value", hash_value);

        return getJsonString(param);
    }

}