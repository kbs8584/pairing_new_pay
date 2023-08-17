package com.pgmate.pay.van;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.util.comm.UrlClient;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.bean.DanalBean;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.main.Api;
import com.pgmate.pay.proc.ResultUtil;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class DanalAuth implements Van {

	private static Logger logger 					= LoggerFactory.getLogger( com.pgmate.pay.van.DanalAuth.class ); 
	static final String DN_CREDIT_URL 			= "https://tx-creditcard.danalpay.com/credit/";
	//static final String DN_CREDIT_URL 			= "211.170.89.1";	
	// 211.170.89.1 ~ 211.170.89.15 
	// 150.242.133.113 ~ 150.242.133.116
	
	static final String DN_SERVICE_URL			= "https://trans.teledit.com";
	
	static final int DN_CONNECT_TIMEOUT 		= 5000;
	static final int DN_TIMEOUT 					= 30000;
	
	static final String ERC_NETWORK_ERROR 	= "-1";
	static final String ERM_NETWORK 				= "Network Error";
	static final String CHARSET 						= "EUC-KR";
	
	private static String IVKEY 						= "d7d02c92cb930b661f107cb92690fc83"; 
	private static String CPID 						= "";
	private static String CRYPTOKEY				= "";
	private static String VAN							= "";
	
	static {
	    disableSslVerification();
	}
	
	public DanalAuth(){
		
	}
	
	public DanalAuth(SharedMap<String, Object> vanMap) {
		CPID =  vanMap.getString("vanId").trim();
		CRYPTOKEY	= vanMap.getString("cryptoKey").trim();
		VAN	= vanMap.getString("van").trim();
		logger.info("==========");
		logger.info("========== cryptoKey : " + vanMap.getString("cryptoKey").trim());
		logger.info("========== CRYPTOKEY : " + CRYPTOKEY);
		logger.info("==========");
	}

	@Override
	public synchronized SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,Response response) {
		String item = "";
		try {
			if (response.pay.products != null && response.pay.products.size() > 0) {
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
			}
		} catch (Exception e) {
			
		}
		
		HashMap<String, String> req = new HashMap<String, String>();
		// CP 정보
		req.put("CPID", CPID);
		req.put("SUBCPID", "");
		
		// 결제 정보
		req.put("ORDERID", response.pay.trxId);
		req.put("ITEMNAME", item);
		req.put("AMOUNT", CommonUtil.toString(response.pay.amount));
		req.put("CURRENCY", "410");
		req.put("OFFERPERIOD", "2015102920151129");

		// 고객 정보
		req.put("USERNAME", CommonUtil.nToB(response.pay.payerName, "username"));
		req.put("USERID", response.pay.tmnId);
		req.put("USEREMAIL", CommonUtil.nToB(response.pay.payerEmail, "tp1@thepayone.com")); // 고정값
		req.put("USERPHONE", CommonUtil.nToB(response.pay.payerTel, "0216701915"));
		req.put("USERAGENT", "WP"); // 고정값

		// 기본 정보
		req.put("TXTYPE", "AUTH");
		req.put("SERVICETYPE", "DANALCARD");
		req.put("CANCELURL", "http://admin-dev.pairingpayments.net/");
		req.put("RETURNURL", "http://admin-dev.pairingpayments.net/");
		req.put("ISNOTI", "DANALCARD");
		req.put("NOTIURL", "http://admin-dev.pairingpayments.net/");
		req.put("APPURL", "http://admin-dev.pairingpayments.net/");
		req.put("BYPASSVALUE", "");
		
		// 카드 정보
		req.put("CARDCODE", "");
		req.put("QUOTA", CommonUtil.zerofill(response.pay.card.installment, 2));
		req.put("CARDCODEBASE", "");
		req.put("QUOTABASE", "");
		
		// 제휴사 정보
		req.put("ALLIANCECODEBASE", "NONE");
		
		// 표중결제창 정보
		req.put("COLOR", "");
		req.put("CIURL", "");
/*
		req.put("ISREBILL", "N"); // 고정값
		req.put("BILLINFO", response.pay.card.number); // 카드번호
		req.put("EXPIREPERIOD", response.pay.card.expiry); // 유효기간 YYMM
		req.put("CARDPWD", ""); // 비밀번호 앞 2자리
		req.put("CARDAUTH", ""); // 생년월일 YYMMDD
		if (response.pay.metadata != null) {
			if (response.pay.metadata.isTrue("cardAuth")) {
				req.put("CARDPWD", response.pay.metadata.getString("authPw")); // 비밀번호 앞 2자리
				req.put("CARDAUTH", response.pay.metadata.getString("authDob")); // 생년월일 YYMMDD
			}
			response.pay.metadata = null;
		}
*/		
		HashMap<String, String> resD = connect(req);
		
		response.pay.authCd = CommonUtil.toString(resD.get("CARDAUTHNO"));
		if (CommonUtil.toString(resD.get("RETURNCODE")).equals("0000")) {
			response.result = ResultUtil.getResult("0000", "정상", "정상승인");
		} else {
			response.result = ResultUtil.getResult(CommonUtil.toString(resD.get("RETURNCODE")), "승인실패", CommonUtil.toString(resD.get("RETURNMSG")));
		}
		sharedMap.put("van", VAN);
		sharedMap.put("vanId", CPID);
		sharedMap.put("vanTrxId", CommonUtil.toString(resD.get("TID")));
		sharedMap.put("vanResultCd", CommonUtil.toString(resD.get("RETURNCODE")));
		sharedMap.put("vanResultMsg", CommonUtil.toString(resD.get("RETURNMSG")));
		sharedMap.put("vanDate", CommonUtil.toString(resD.get("TRANDATE")) + CommonUtil.toString(resD.get("TRANTIME")));
		logger.info("vanTrxId : {},{}", sharedMap.getString("vanTrxId"), sharedMap.getString("vanDate"));
		logger.debug("cardName : {}", CommonUtil.toString(resD.get("CARDNAME")));
		
		return sharedMap;
	}


	@Override
	public synchronized SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		
		HashMap<String, String> req = new HashMap<String, String>();
		// CP 정보
		req.put("TID", payMap.getString("vanTrxId"));

		// 결제 정보
		req.put("AMOUNT", CommonUtil.toString(response.refund.amount));
		req.put("ORDERID", response.refund.trxId);
		req.put("CANCELREQUESTER", "CP_CS_PERSON"); // 취소 요청자. 로그성 자료.
		req.put("CANCELDESC", "Customer request refund"); // 취소 사유.
		req.put("CANCELTYPE", "C"); // 취소 사유.
		
		// 기본 정보
		req.put("TXTYPE", "CANCEL");
		req.put("SERVICETYPE", "DANALCARD");
		
		HashMap<String,String> resD = connect(req);
		
		response.refund.authCd = CommonUtil.toString(resD.get("CARDAUTHNO"));
		if (CommonUtil.toString(resD.get("RETURNCODE")).equals("0000")) {
			response.result = ResultUtil.getResult("0000", "정상", "정상취소");
		} else {
			response.result = ResultUtil.getResult(CommonUtil.toString(resD.get("RETURNCODE")), "취소실패", CommonUtil.toString(resD.get("RETURNMSG")));
		}
		sharedMap.put("van", payMap.getString("van"));
		sharedMap.put("vanId", CPID);
		sharedMap.put("vanTrxId", CommonUtil.toString(resD.get("TID")));
		sharedMap.put("vanResultCd", CommonUtil.toString(resD.get("RETURNCODE")));
		sharedMap.put("vanResultMsg", CommonUtil.toString(resD.get("RETURNMSG")));
		sharedMap.put("vanDate", CommonUtil.toString(resD.get("TRANDATE")) + CommonUtil.toString(resD.get("TRANTIME")));
		logger.info("vanTrxId : {},{}", sharedMap.getString("vanTrxId"), sharedMap.getString("vanDate"));

		return sharedMap;
	}
	
	public synchronized HashMap<String,String> connect(HashMap<String,String> data) {
		logger.debug("========== ========== ========== ========== ========== DANAL connect");
		String req = "CPID=" + CPID + "&DATA=" + urlEncode(toEncrypt(toQueryString(data)));
		logger.info("========== ========== ========== ========== CPID : " + CPID);
		logger.info("========== ========== ========== ========== data : " + data);
		logger.info("========== ========== ========== ========== data : " + toQueryString(data));
		logger.info("========== ========== ========== ========== data : " + toEncrypt(toQueryString(data)));
		logger.info("========== ========== ========== ========== data : " + urlEncode(toEncrypt(toQueryString(data))));
		logger.info("send : [{}]", req);
		HashMap<String, String> resData = new HashMap<String, String>();
		long time = System.currentTimeMillis();
		String message = "";
		UrlClient client = null;
		try {
			client = new UrlClient(DN_CREDIT_URL, "POST", "application/x-www-form-urlencoded; charset=euc-kr");
			client.setTimeout(DN_CONNECT_TIMEOUT, DN_TIMEOUT);
			client.setDoInputOutput(true, true);
			message = client.connect(req);
			
			logger.debug("recv : [{}]", message);
			if (message.indexOf("RETURNCODE") > -1) {
				resData = parseQueryString(message);
			} else if (message.indexOf("DATA=") > -1) {
				String decrypted = toDecrypt(urlDecode(message.split("=")[1]));
				resData = parseQueryString(decrypted);
			} else {
				logger.debug("danal,data error : {}", message);
			}

		} catch (Exception e) {
			message = "NOTCONNECTED";
			resData.put("RETURNCODE", "XXXX");
			resData.put("RETURNMSG", message);
			logger.debug("danal,error : {}", e.getMessage());
		} finally {
			logger.debug("danal Elasped Time =[{} sec]",
					CommonUtil.parseDouble((System.currentTimeMillis() - time) / 1000));
			logger.debug("danal, res = [{}]", resData);

		}
		return resData;
	}
	
	public synchronized static boolean auth(RoutingContext rc, String uri, String trxId) {
		logger.debug("========== ========== ========== ========== ========== DANAL AUTH");
		DAO dao = new DAO();
		SharedMap<String, Object> sharedMap = new SharedMap<String, Object>();
		if (uri.startsWith("/api/pay/danal/auth")) {
			System.out.println();
			DanalBean danalBean = (DanalBean) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), DanalBean.class);
			dao.initRecord();
			dao.setDebug(true);
			dao.setColumns("mchtId, van, vanId, cryptoKey, semiAuth");
			dao.setTable("VW_MCHT_TMN");
			dao.addWhere("mchtId", danalBean.mchtId);
			dao.setOrderBy("");
			SharedMap <String, Object> vanMap = dao.search().getRowFirst();
			if (vanMap.getString("mchtId").equals(danalBean.mchtId)) {
				CPID =  vanMap.getString("vanId").trim();
				CRYPTOKEY	= vanMap.getString("cryptoKey").trim();
				VAN	= vanMap.getString("van").trim();
				
				LinkedHashMap<String, String> reqA = new LinkedHashMap<String, String>();
				/******************************************************
				 ** 아래의 데이터는 고정값입니다.( 변경하지 마세요 )
				 * TXTYPE       : ITEMSEND
				 * SERVICE		: UAS
				 * AUTHTYPE		: 36
				 ******************************************************/
				reqA.put("TXTYPE", "ITEMSEND");
				reqA.put("SERVICE", "UAS");
				reqA.put("AUTHTYPE", "36");
				
				/******************************************************
				 * CPID 	 : 다날에서 제공해 드린 ID( function 파일 참조 )
				 * CPPWD	 : 다날에서 제공해 드린 PWD( function 파일 참조 )
				 * TARGETURL : 인증 완료 시 이동 할 페이지의 FULL URL
				 * CPTITLE   : 가맹점의 대표 URL 혹은 APP 이름 
				 ******************************************************/
				reqA.put("CPID", CPID);
				reqA.put("CPPWD", CRYPTOKEY);
				//reqA.put( "TARGETURL", "http://localhost/Danal/UAS/Mobile/CPCGI.jsp" );
				//reqA.put( "CPTITLE", "www.danal.co.kr" );
				reqA.put("TARGETURL", "http://admin-dev.pairingpayments.net/");
				reqA.put("CPTITLE", "페어링");
				
				/***[ 선택 사항 ]**************************************/
				/******************************************************
				 * USERID       : 사용자 ID
				 * ORDERID      : CP 주문번호	 
				 * AGELIMIT		: 서비스 사용 제한 나이 설정( 가맹점 필요 시 사용 )
				 ******************************************************/
				reqA.put("USERID", vanMap.getString("mchtId"));
				reqA.put("ORDERID", trxId);	
				reqA.put( "AGELIMIT", "019" );

				HashMap<String, String> resA = CallTrans(reqA);
				if (resA.get("RETURNCODE").equals("0000")) {
					Api.sendMsgPay(rc, resA.get("RETURNCODE"), "인증", resA.get("RETURNMSG"), resA.get("TID"));
					return true;
				} else {
					Api.sendMsg(rc, resA.get("RETURNCODE"), "인증실패", resA.get("RETURNMSG"));
					return true;
				}
			}
		}
		return false;
	}	
	
	public synchronized static HashMap<String,String> CallTrans(HashMap<String,String> data) {
		logger.debug("========== ========== ========== ========== ========== DANAL CallTrans");
		logger.info("========== ========== ========== ========== data : " + data);
		//String req = toQueryString(data);
		String req = urlEncode(toEncrypt(toQueryString(data)));
		logger.info("========== ========== ========== ========== req : " + req);
		HashMap<String, String> resData = new HashMap<String, String>();
		long time = System.currentTimeMillis();
		String message = "";
		UrlClient client = null;
		try {
			client = new UrlClient(DN_CREDIT_URL, "POST", "application/x-www-form-urlencoded; charset=euc-kr");
			client.setTimeout(DN_CONNECT_TIMEOUT, DN_TIMEOUT);
			client.setDoInputOutput(true, true);
			message = client.connect(req);
			
			logger.debug("recv : [{}]", message);
			if (message.indexOf("RETURNCODE") > -1) {
				resData = parseQueryString(message);
			} else if (message.indexOf("DATA=") > -1) {
				//String decrypted = toDecrypt(urlDecode(message.split("=")[1]));
				String decrypted = urlDecode(message.split("=")[1]);
				resData = parseQueryString(decrypted);
			} else {
				logger.debug("danal,data error : {}", message);
			}

		} catch (Exception e) {
			message = "NOTCONNECTED";
			resData.put("RETURNCODE", "XXXX");
			resData.put("RETURNMSG", message);
			logger.debug("danal,error : {}", e.getMessage());
		} finally {
			logger.debug("danal Elasped Time =[{} sec]",
					CommonUtil.parseDouble((System.currentTimeMillis() - time) / 1000));
			logger.debug("danal, res = [{}]", resData);

		}
		return resData;
	}
	
	
	public static String toQueryString(HashMap<String,String> data){
		StringBuilder sb = new StringBuilder();
		for( String key : data.keySet() ){
			sb.append(key+"="+urlEncode(changeCharset(CommonUtil.nToB(data.get(key)),"euc-kr")));
			sb.append("&");
	    }
		StringBuilder sbb = new StringBuilder();
		for( String key : data.keySet() ){
			sbb.append(key+"="+urlEncode(changeCharset(CommonUtil.nToB(data.get(key)),"euc-kr")));
			sbb.append("\n");
		}
		logger.info("\n\n" + sbb + "\n");
		if (sb.length() > 0){
			return sb.substring(0, sb.length() - 1);
		} else {
			return "";
		}
	}
	
	
	public static HashMap<String,String> parseQueryString(String str){
		HashMap<String,String> data = new HashMap<String,String>();
		String[] st = str.split("&");

		for (int i = 0; i < st.length; i++) {
			int index = st[i].indexOf('=');
			if (index > 0)
				data.put(st[i].substring(0, index), changeCharset(urlDecode(st[i].substring(index + 1)),"utf-8"));
		}
		StringBuilder sbb = new StringBuilder();
		for( String key : data.keySet() ){
			sbb.append(key+"="+urlEncode(changeCharset(CommonUtil.nToB(data.get(key)),"euc-kr")));
			sbb.append("\n");
		}
		logger.info("\n\n" + sbb + "\n");
		
		return data;

	}
	
	
	
	
	public static String urlEncode(Object obj) {
		if (obj == null)
			return null;

		try {
			return URLEncoder.encode(obj.toString(), CHARSET);
		} catch (Exception e) {
			return obj.toString();
		}
	}

	/*
	 *  urlDecode
	 */
	public static String urlDecode(Object obj) {
		if (obj == null)
			return null;

		try {
			return URLDecoder.decode(obj.toString(),CHARSET);
		} catch (Exception e) {
			return obj.toString();
		}
	}

	
	public static String toEncrypt(String originalMsg)  {
		logger.debug("========== ========== ========== ========== ========== toEncrypt originalMsg : " + originalMsg);
		logger.debug("========== ========== ========== ========== ========== toDecrypt IVKEY : " + IVKEY);
		logger.debug("========== ========== ========== ========== ========== toDecrypt CRYPTOKEY : " + CRYPTOKEY);
		String AESMode = "AES/CBC/PKCS5Padding";
		String SecetKeyAlgorithmString = "AES";
		//logger.debug("request : [{}]",originalMsg);
		IvParameterSpec ivspec = new IvParameterSpec(hexToByteArray(IVKEY));
		SecretKey keySpec = new SecretKeySpec(hexToByteArray(CRYPTOKEY), SecetKeyAlgorithmString);
		try{
			Cipher cipher = Cipher.getInstance(AESMode);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivspec);
			byte[] encrypted = cipher.doFinal(originalMsg.getBytes());
			return new String(Base64.encodeBase64(encrypted));
		}catch(Exception e){
			logger.info("Encrypt error : {}",e.getMessage());
			return "";
		}
	}
	
	public String toDecrypt(String originalMsg) {
		logger.debug("========== ========== ========== ========== ========== toDecrypt originalMsg : " + originalMsg);
		logger.debug("========== ========== ========== ========== ========== toDecrypt IVKEY : " + IVKEY);
		logger.debug("========== ========== ========== ========== ========== toDecrypt CRYPTOKEY : " + CRYPTOKEY);
		//logger.debug("response : [{}]",originalMsg);
		String AESMode = "AES/CBC/PKCS5Padding";
		String SecetKeyAlgorithmString = "AES";

		IvParameterSpec ivspec = new IvParameterSpec(hexToByteArray(IVKEY));
		SecretKey keySpec = new SecretKeySpec(hexToByteArray(CRYPTOKEY), SecetKeyAlgorithmString);
		try {
			Cipher cipher = Cipher.getInstance(AESMode);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivspec);
			byte[] decrypted = cipher.doFinal(Base64.decodeBase64(originalMsg));
			String retValue = new String(decrypted);

			return retValue;
		} catch (Exception e) {
			logger.info("Decrypt error : {}", e.getMessage());
			return "";
		}
	}

	private static byte[] hexToByteArray(String hex) {
		if (hex == null || hex.length() == 0) {
			return null;
		}

		byte[] ba = new byte[hex.length() / 2];
		for (int i = 0; i < ba.length; i++) {
			ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return ba;
	}
	
	 public static String changeCharset(String str, String charset) {
		try {
			byte[] bytes = str.getBytes(charset);
			return new String(bytes, charset);
		} catch (UnsupportedEncodingException e) {
		} // Exception
		return "";
    }
	 
	private static void disableSslVerification() {
		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}
	
}
