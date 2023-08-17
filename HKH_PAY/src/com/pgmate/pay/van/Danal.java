package com.pgmate.pay.van;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

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
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.comm.UrlClient;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.DanalBean;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.main.Api;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;
import kr.co.danal.jsinbi.HttpClient;




/**
 * @author Administrator
 *
 */
public class Danal implements Van {

	private static Logger logger 					= LoggerFactory.getLogger( com.pgmate.pay.van.Danal.class ); 
	//static final String DN_CREDIT_URL 			= "https://tx_creditcard.danalpay.com/credit/";
	static final String DN_CREDIT_URL 			= "https://tx-creditcard.danalpay.com/credit/";
	
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
	
	public Danal(){
		
	}
	
	public Danal(SharedMap<String, Object> vanMap) {
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
		logger.debug("========== ========== ========== ========== ========== DANAL sales");
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
		req.put("SUBCPID", "subcpid");
		
		// 결제 정보
		req.put("AMOUNT", CommonUtil.toString(response.pay.amount));
		req.put("CURRENCY", "410");
		req.put("ITEMNAME", item);
		req.put("ORDERID", response.pay.trxId);

		// 고객 정보
		req.put("USERNAME", CommonUtil.nToB(response.pay.payerName, "username"));
		req.put("USERPHONE", CommonUtil.nToB(response.pay.payerTel, ""));
		req.put("USERID", response.pay.tmnId);
		req.put("USERAGENT", "ONLINE"); // 고정값
		req.put("USEREMAIL", CommonUtil.nToB(response.pay.payerEmail, "")); // 고정값

		// 카드 정보
		req.put("QUOTA", CommonUtil.zerofill(response.pay.card.installment, 2));
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
		
		// 기본 정보
		req.put("TXTYPE", "OTBILL");
		req.put("SERVICETYPE", "KEYIN");
		
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
		logger.info("DANAL pay : TRANTIME : " + CommonUtil.toString(resD.get("TRANDATE")));
		logger.info("DANAL pay : TRANTIME : " + CommonUtil.toString(resD.get("TRANTIME")));
	//	logger.info("cardName : {}", CommonUtil.toString(resD.get("CARDNAME")));
	//	sharedMap.put("cardAcquirer", CommonUtil.toString(resD.get("CARDNAME")));
	//	logger.debug("sharedMap cardAcquirer : {}", sharedMap.get("cardAcquirer"));
		
		return sharedMap;
	}


	@Override
	public synchronized SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		logger.debug("========== ========== ========== ========== ========== DANAL refund");
		HashMap<String, String> req = new HashMap<String, String>();
		// CP 정보
		req.put("TID", payMap.getString("vanTrxId"));

		// 결제 정보
		req.put("AMOUNT", CommonUtil.toString(response.refund.amount));
		req.put("ORDERID", response.refund.trxId);
		req.put("CANCELREQUESTER", "CP_CS_PERSON"); // 취소 요청자. 로그성 자료.
		req.put("CANCELDESC", "Customer request refund"); // 취소 사유.
		//req.put("CANCELTYPE", "P"); // 취소 사유.
		if (sharedMap.isEquals("rfdAll", "부분")) {	// 2022-03-18 다날 부분취소 추가
			req.put("CANCELTYPE", "P"); // 취소 사유. - C 전체취소 P 부분취소
		} else {
			req.put("CANCELTYPE", "C"); // 취소 사유. - C 전체취소 P 부분취소
		}
		
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
		sharedMap.put("vanDate", CommonUtil.toString(resD.get("CANCELDATETIME")));
		System.out.println();
		System.out.println("DANAL DATA : " + resD.toString());
		System.out.println();
		logger.info("vanTrxId : {},{}", sharedMap.getString("vanTrxId"), sharedMap.getString("vanDate"));
		logger.info("DANAL refund : TRANDATE : " + CommonUtil.toString(resD.get("TRANDATE")));
		logger.info("DANAL refund : TRANDATE : " + CommonUtil.toString(resD.get("TRANDATE")));
		logger.info("DANAL refund : CANCELDATETIME : " + CommonUtil.toString(resD.get("CANCELDATETIME")));
		
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
			logger.debug("========== ========== ========== ========== DANAL connect URL : " + DN_CREDIT_URL);
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
		logger.debug("========== ========== ========== ========== ========== DANAL CARD AUTH");
		DAO dao = new DAO();
		SharedMap<String, Object> sharedMap = new SharedMap<String, Object>();
		if (uri.startsWith("/api/pay/danal/auth")) {
			System.out.println();
			DanalBean danalBean = (DanalBean) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), DanalBean.class);
			dao.initRecord();
			dao.setDebug(true);
			dao.setColumns("mchtId, van, vanId, cryptoKey, semiAuth, tmnId");
			dao.setTable("VW_MCHT_TMN");
			dao.addWhere("mchtId", danalBean.mchtId);
			dao.setOrderBy("");
			SharedMap <String, Object> vanMap = dao.search().getRowFirst();
			if (vanMap.getString("mchtId").equals(danalBean.mchtId)) {
				CPID =  vanMap.getString("vanId").trim();
				CRYPTOKEY	= vanMap.getString("cryptoKey").trim();
				VAN	= vanMap.getString("van").trim();
				
				LinkedHashMap<String, String> reqA = new LinkedHashMap<String, String>();
				/**************************************************
				 * SubCP 정보
				 **************************************************/
				reqA.put("SUBCPID", "");
				
				/**************************************************
				 * 결제 정보
				 **************************************************/
				reqA.put("AMOUNT", danalBean.amount);
				reqA.put("CURRENCY", "410");
				reqA.put("ITEMNAME", danalBean.prdName);
				reqA.put("USERAGENT", danalBean.userAgent);
				String orderDate = CommonUtil.getCurrentDate("yyyyMMddhhmmss");
			 	reqA.put("ORDERID", "Danal_" + orderDate);
				reqA.put("OFFERPERIOD", "2015102920151129");
				
				/**************************************************
				 * 고객 정보
				 **************************************************/
				reqA.put("USERNAME", danalBean.payerName); // 구매자 이름
				reqA.put("USERID", danalBean.userId); // 사용자 ID
				reqA.put("USEREMAIL", danalBean.payerEmail); // 소보법 email수신처
				reqA.put("USERPHONE", danalBean.payerTel); // 
				
				/**************************************************
				 * URL 정보
				 **************************************************/
				reqA.put("CANCELURL", danalBean.cancelUrl);
				reqA.put("RETURNURL", danalBean. returnUrl);

				/**************************************************
				 * 기본 정보
				 **************************************************/
				reqA.put("TXTYPE", "AUTH");
				reqA.put("SERVICETYPE", "DANALCARD");
				reqA.put("ISNOTI", "Y");
				reqA.put("BYPASSVALUE", "this=is;a=test;bypass=value"); // BILL응답 또는 Noti에서 돌려받을 값. '&'를 사용할 경우 값이 잘리게되므로 유의.
				
				dao.initRecord();
				dao.setTable("PG_TRX_DANAL");
				dao.setRecord("mchtId", danalBean.mchtId);
				dao.setRecord("trxId", reqA.get("ORDERID"));
				dao.setRecord("van", VAN);
				dao.setRecord("vanId", CPID);
				
				dao.setRecord("amount", danalBean.amount);
				dao.setRecord("prdName", danalBean.prdName);
				dao.setRecord("prdQty", 1);
				dao.setRecord("payerName", danalBean.payerName);
				dao.setRecord("payerTel", danalBean.payerTel);
				dao.setRecord("payerEmail", danalBean.payerEmail);
				dao.setRecord("userId", danalBean.userId);
				dao.setRecord("userAgent", danalBean.userAgent);
				dao.setRecord("returnUrl", danalBean.returnUrl);
				dao.setRecord("notiUrl", danalBean.notiUrl);
				dao.setRecord("cancelUrl", danalBean.cancelUrl);
				if (danalBean.byPassValue != null || !danalBean.byPassValue.equals("")) {
					dao.setRecord("byPassValue", danalBean.byPassValue);
				}
				
				dao.setRecord("txType", danalBean.txType);
				dao.setRecord("serviceType", danalBean.serviceType);
				
				HashMap<String, String> resA = CallCredit(reqA);
				System.out.println();
				System.out.println();
				System.out.println("다날 resA 결제 통신 후 데이터 확인 : " + resA.toString());
				for (String key : resA.keySet()) {
					System.out.println("key : " + key + " \t|" + resA.get(key));
					
					return false;
				}
				System.out.println();
				System.out.println();
				
				if (resA.get("RETURNCODE").equals("0000")) {
					dao.setRecord("vanTrxId", resA.get("TID"));
					dao.setRecord("vanResultCd", resA.get("RETURNCODE"));
					dao.setRecord("vanResultMsg", resA.get("RETURNMSG"));
					
				//	dao.setRecord("regId", SessionUtil.getUserId(request));
					dao.setRecord("regId", danalBean.userId);
					dao.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
					dao.setRecord("regTime", CommonUtil.getCurrentDate("hhmmss"));
					
					if (dao.insert()) {
						logger.info("========== ========== ========== ========== DANAL CARD AUTH DB INSERT COMPLETE ");
						logger.info("========== ========== ========== ========== STARTURL : " + resA.get("STARTURL"));
						logger.info("========== ========== ========== ========== STARTPARAMS : " + resA.get("STARTPARAMS"));
						logger.info("========== ========== ========== ========== RETURNCODE : " + CommonUtil.toString(resA.get("RETURNCODE")));
						logger.info("========== ========== ========== ========== RETURNMSG : " + CommonUtil.toString(resA.get("RETURNMSG")));
						logger.info("========== ========== ========== ========== 다날 간편결제 - 인증 성공 TID : " + resA.get("TID"));
						logger.info("========== ========== ========== ========== 다날 간편결제 - 인증 성공 ORDERID : " + resA.get("ORDERID"));
						sendMsgDanalAuth(rc, CommonUtil.toString(resA.get("RETURNCODE")), "인증성공", CommonUtil.toString(resA.get("RETURNMSG")), 
								resA.get("STARTURL"), resA.get("STARTPARAMS"));
						return true;
					} else {
						logger.info("========== ========== ========== ========== DANAL CARD AUTH DB INSERT ERROR ");
					}
					return false;
				} else {
					Api.sendMsg(rc, resA.get("RETURNCODE"), "인증실패", resA.get("RETURNMSG"));
					return false;
				}
			}
		}
		return false;
	}
	public synchronized static boolean authReturn(RoutingContext rc, String uri, String trxId) {
		DAO dao = new DAO();
		SharedMap<String, Object> sharedMap = new SharedMap<String, Object>();
		if (uri.startsWith("/api/pay/danal/return")) {
			System.out.println();
			logger.debug("========== ========== ========== ========== ========== authReturn 다날 간편결제 인증리턴 - 실결제");
			DanalBean data = (DanalBean) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), DanalBean.class);
			String encData = data.RETURNPARAMS;
			logger.debug("========== ========== ========== ========== ========== rc : " + VertXUtil.getBodyAsString(rc));
			logger.debug("========== ========== ========== ========== ========== data : " + encData);
			logger.debug("========== ========== ========== ========== ========== mchtId : " + data.mchtId);
			dao.initRecord();
			dao.setDebug(true);
			dao.setColumns("mchtId, van, vanId, cryptoKey, semiAuth, tmnId");
			dao.setTable("VW_MCHT_TMN");
			dao.addWhere("mchtId", data.mchtId);
			dao.setOrderBy("");
			SharedMap <String, Object> vanMap = dao.search().getRowFirst();
			if (vanMap.getString("mchtId").equals(data.mchtId)) {
				CPID =  vanMap.getString("vanId").trim();
				CRYPTOKEY	= vanMap.getString("cryptoKey").trim();
				VAN	= vanMap.getString("van").trim();
			}
			//String encData = VertXUtil.getBodyAsString(rc).split("=")[1];
			encData = StringEscapeUtils.unescapeJava(encData);
			System.out.println();
			logger.debug("========== ========== ========== ========== ========== encData : \n" + encData);
			logger.debug("========== ========== ========== ========== ========== urlDecode : \n" + urlDecode((Object) encData));
			StringEscapeUtils.unescapeJava(encData);
			String decrypted = toDecrypt((String) urlDecode((Object) encData));
			logger.debug("========== ========== ========== ========== ========== decrypted : \n" + decrypted);
			HashMap<String, String> resData = new HashMap<String, String>();
			resData = parseQueryString(decrypted);
			logger.debug("========== ========== ========== ========== ========== resData : " + resData);
			
			logger.info("========== ========== ========== ========== notijObj : " + resData.toString());
			logger.info("========== ========== ========== ========== ");
			logger.info("========== ========== ========== ========== RETURNPARAMS : " + resData.get("RETURNPARAMS"));
			logger.info("========== ========== ========== ========== RETURNCODE : " + resData.get("RETURNCODE"));
			logger.info("========== ========== ========== ========== RETURNMSG : " + resData.get("RETURNMSG"));
			logger.info("========== ========== ========== ========== TID : " + resData.get("TID"));
			logger.info("========== ========== ========== ========== ORDERID : " + resData.get("ORDERID"));
			
			if (resData.get("RETURNCODE") != null && resData.get("RETURNMSG") != null && resData.get("TID") != null) {
				dao.initRecord();
				dao.setDebug(true);
				dao.setColumns("*");
				dao.setTable("PG_TRX_DANAL A \n" + 
						"JOIN VW_MCHT_TMN B ON A.mchtId = B.mchtId");
				dao.addWhere("vanTrxId", resData.get("TID"));
				dao.setOrderBy("");
				SharedMap<String, Object> selectMap = dao.search().getRowFirst();
				LinkedHashMap<String, String> reqA = new LinkedHashMap<String, String>();
				/**************************************************
				 * 결제 정보
				 **************************************************/
				reqA.put("TID", resData.get("TID"));
				reqA.put("AMOUNT", (String) selectMap.get("amount"));	// 최초 결제요청(AUTH)시에 보냈던 금액과 동일한 금액을 전송
				/**************************************************
				 * 연동 정보
				 **************************************************/
				reqA.put("TXTYPE", "BILL");
				reqA.put("SERVICETYPE", "DANALCARD");
				
				HashMap<String, String> resA = CallCredit(reqA);
				
				dao.initRecord();
				dao.setDebug(true);
				dao.setTable("PG_TRX_DANAL");
				logger.info("========== resA ");
				logger.info("========== resA AMOUNT : " + resA.get("AMOUNT"));
				logger.info("========== resA DISCOUNTAMOUNT : " + resA.get("DISCOUNTAMOUNT"));
				logger.info("========== resA TRXAMOUNT : " + resA.get("TRXAMOUNT"));
				logger.info("========== resA QUOTA : " + resA.get("QUOTA"));
				logger.info("========== resA CARDAUTHNO : " + resA.get("CARDAUTHNO"));
				logger.info("========== resA CARDCODE : " + resA.get("CARDCODE"));
				logger.info("========== resA CARDNAME : " + resA.get("CARDNAME"));
				logger.info("========== resA CARDNO : " + resA.get("CARDNO"));
				logger.info("========== resA BYPASSVALUE : " + resA.get("BYPASSVALUE"));
				logger.info("========== resA TRANDATE : " + resA.get("TRANDATE"));
				logger.info("========== resA TRANTIME : " + resA.get("TRANTIME"));
				
				dao.setRecord("discountAmount", resA.get("DISCOUNTAMOUNT"));
				dao.setRecord("trxAmount", resA.get("TRXAMOUNT"));
				dao.setRecord("authCd", resA.get("CARDAUTHNO"));
				dao.setRecord("installment", resA.get("QUOTA"));
				dao.setRecord("cardCd", resA.get("CARDCODE"));
				//dao.setRecord("acquirer", resA.get("CARDNAME"));
				dao.setRecord("issuer", resA.get("CARDNAME"));
				dao.setRecord("cardNo", resA.get("CARDNO"));
				dao.setRecord("bin", resA.get("CARDNO").substring(0, 6));
				logger.info("========== resA - " + resA.get("CARDNO").substring(0, 6));
				logger.info("========== resA - " + resA.get("CARDNO").substring(12, 16));
				dao.setRecord("last4", resA.get("CARDNO").substring(12, 16));
				dao.setRecord("tranDate", resA.get("TRANDATE"));
				dao.setRecord("tranTime", resA.get("TRANTIME"));
				dao.addWhere("vanTrxId", resData.get("TID"));
				dao.addWhere("amount", (String) selectMap.get("amount"));
				
				if (dao.update()) {
					TrxDAO trxDAO = new TrxDAO();
					
					SharedMap<String, Object> sMap = new SharedMap<String, Object>();
					Pay pay = new Pay();
					pay.trxId = trxId;
					pay.trxType = "ONTR";
					sMap.put("mchtId", selectMap.get("mchtId"));
					pay.tmnId = selectMap.getString("tmnId");
					long trx_long = (long) (new Date().getTime() * (Math.floor(Math.random() * 10) +1));
					dao.setRecord("trackId", "DANAL" + trx_long);
					pay.trackId = "DANAL" + trx_long;
					pay.payerName = selectMap.getString("payerName");
					pay.payerEmail = selectMap.getString("payerEmail");
					pay.payerTel = selectMap.getString("payerTel");
					pay.amount = selectMap.getLong("amount");
					
					Card card = new Card();
					String cardId =GenKey.genKeys(CPKEY.CARD, trxId);
					sMap.put("cardId", cardId);
					card.issuer = resA.get("CARDNAME").replace("카드", "");
					card.bin = resA.get("CARDNO").substring(0, 6);
					card.last4 = resA.get("CARDNO").substring(12, 16);
					//card.cardType = "";	// 다날응답데이터에 카드종류가 없음.
					card.installment = Integer.parseInt(resA.get("QUOTA"));
					//card.acquirer = resA.get("CARDNAME");
					card.issuer = resA.get("CARDNAME");
					
					pay.card = card;
					String encrypted = com.pgmate.lib.util.cipher.Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(pay.card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16)));
					trxDAO.insertCard(cardId, encrypted);
					
					String prodId = GenKey.genKeys(CPKEY.PRODUCT, trxId);
					List<Product> products = new ArrayList<>();
					Product prd = new Product();
					prd.prodId = prodId;
					prd.name = selectMap.getString("prdName");
					prd.qty = Integer.parseInt(selectMap.getString("prdQty"));
					products.add(prd);
					String regDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
					trxDAO.insertProduct(prodId, products, regDate);
					sMap.put("prodId", prodId);
					sMap.put("regDate", regDate);
					pay.compNo = "";		// 업체API 가 아니므로 제외
					pay.compMember = "";	// 업체API 가 아니므로 제외
					
					Response res = new Response();
					res.pay = pay;
					logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_REQ BEGIN");
					//trxDAO.insertTrxREQ(sMap, res);
					dao.initRecord();
						dao.setTable("PG_TRX_REQ");
					logger.debug("========== trxId " + res.pay.trxId);
					logger.debug("========== trxType " + res.pay.trxType);
					logger.debug("========== mchtId " + sMap.getString(PAYUNIT.MCHTID));
					logger.debug("========== tmnId " + res.pay.tmnId);
					logger.debug("========== trackId " + res.pay.trackId);
					logger.debug("========== amount " + res.pay.amount);
					logger.debug("========== installment " + CommonUtil.zerofill(res.pay.card.installment,2));
					logger.debug("========== cardId " + sMap.getString(PAYUNIT.KEY_CARD));
					logger.debug("========== issuer " + res.pay.card.issuer);
					logger.debug("========== prodId " + sMap.getString(PAYUNIT.KEY_PROD));
					logger.debug("========== regDate " + sMap.getString(PAYUNIT.REG_DATE));
						dao.setRecord("trxId", res.pay.trxId);
						dao.setRecord("trxType", res.pay.trxType);
						dao.setRecord("mchtId", sMap.getString(PAYUNIT.MCHTID));
						dao.setRecord("tmnId", res.pay.tmnId);
						dao.setRecord("trackId", res.pay.trackId);
						dao.setRecord("payerName", res.pay.payerName);
						dao.setRecord("payerEmail", res.pay.payerEmail);
						dao.setRecord("payerTel", res.pay.payerTel);
						dao.setRecord("amount", res.pay.amount);
						dao.setRecord("cardId", sMap.getString(PAYUNIT.KEY_CARD));
						if (res.pay.card != null) {
							dao.setRecord("issuer", res.pay.card.issuer);
							dao.setRecord("last4", res.pay.card.last4);
							dao.setRecord("cardType", res.pay.card.cardType); 
							dao.setRecord("bin", res.pay.card.bin);
							dao.setRecord("installment", CommonUtil.zerofill(res.pay.card.installment,2));
							dao.setRecord("acquirer", res.pay.card.acquirer);
						}
						dao.setRecord("prodId", sMap.getString(PAYUNIT.KEY_PROD));
						dao.setRecord("regDay", sMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
						dao.setRecord("regTime", sMap.getString(PAYUNIT.REG_DATE).substring(8));
						dao.setRecord("regDate", sMap.getString(PAYUNIT.REG_DATE));
						if (res.pay.compNo != null) {
							dao.setRecord("compNo", res.pay.compNo);
						} else {
							dao.setRecord("compNo", "");
						}
						if (res.pay.compMember != null) {
							dao.setRecord("compMember", res.pay.compMember);
						} else {
							dao.setRecord("compMember", "");
						}
						//logger.info("insertTrxREQ() insert() : {},{}", dao.insert(),res.pay.trxId);
						if (dao.insert()) {
							logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_REQ OK");
						} else {
							logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_REQ FAIL");
							Api.sendMsg(rc, resA.get("RETURNCODE"), "승인실패", resA.get("RETURNMSG"));
							return false;
						}
					logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_REQ END");
					logger.info("========== ========== ========== ========== CARDAUTHNO : " + resA.get("CARDAUTHNO"));
					pay.authCd = resA.get("CARDAUTHNO");
					logger.info("========== ========== ========== ========== CARDAUTHNO : " + pay.authCd);
					
					logger.info("========== ========== ========== ========== RETURNCODE : " + resA.get("RETURNCODE"));
					logger.info("========== ========== ========== ========== RETURNMSG : " + resA.get("RETURNMSG"));
					if (resA.get("RETURNCODE").equals("0000")) {
						res.result = ResultUtil.getResult("0000", "정상", "정상승인");
					} else {
						res.result = ResultUtil.getResult(resA.get("RETURNCODE"), "실패", resA.get("RETURNMSG"));
					}
					logger.info("========== ========== ========== ========== res.result SET : ");
					logger.info("========== ========== ========== ========== RETURNCODE : " + res.result.resultCd);
					logger.info("========== ========== ========== ========== RETURNMSG : " + res.result.resultMsg);
					
					logger.info("========== ========== ========== ========== van : " + selectMap.get("van"));
					sMap.put("van", selectMap.get("van"));
					logger.info("========== ========== ========== ========== van : " + sMap.get("van"));

					logger.info("========== ========== ========== ========== vanId : " + selectMap.get("vanId"));
					sMap.put("vanId", selectMap.get("vanId"));
					logger.info("========== ========== ========== ========== vanId : " + sMap.get("vanId"));

					logger.info("========== ========== ========== ========== TID : " + resData.get("TID"));
					sMap.put("vanTrxId", resData.get("TID"));
					logger.info("========== ========== ========== ========== TID : " + sMap.get("vanTrxId"));

					logger.info("========== ========== ========== ========== RETURNCODE: " + resData.get("RETURNCODE"));
					sMap.put("vanResultCd", resData.get("RETURNCODE"));
					logger.info("========== ========== ========== ========== RETURNCODE : " + sMap.get("vanResultCd"));

					logger.info("========== ========== ========== ========== RETURNMSG : " + resData.get("CARDAUTHNO"));
					sMap.put("vanResultMsg", resData.get("RETURNMSG"));
					logger.info("========== ========== ========== ========== RETURNMSG : " + sMap.get("vanResultMsg"));
					
					logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_RES BEGIN");
					//trxDAO.insertTrxRES(sMap, res);
					dao.initRecord();
						dao.setTable("PG_TRX_RES");
						String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
						if (sMap.getString("vanDate").length() == 14){
							curDate = sMap.getString("vanDate");
						}
						dao.setRecord("trxId", res.pay.trxId);
						dao.setRecord("authCd", CommonUtil.nToB(res.pay.authCd));
						dao.setRecord("resultCd", res.result.resultCd);
						dao.setRecord("resultMsg", "[" + res.result.resultMsg + "]" + res.result.advanceMsg);
						dao.setRecord("van", sMap.getString("van"));
						dao.setRecord("vanId", sMap.getString("vanId"));
						dao.setRecord("vanTrxId", sMap.getString("vanTrxId"));
						dao.setRecord("vanResultCd", sMap.getString("vanResultCd"));
						dao.setRecord("vanResultMsg", sMap.getString("vanResultMsg"));
						dao.setRecord("pairingVtid", sMap.getString("pairingVtid"));
						dao.setRecord("pairingRouteVan", sMap.getString("pairingRouteVan"));
						dao.setRecord("pairingCid", sMap.getString("pairingCid"));
						dao.setRecord("regDay", curDate.substring(0, 8));
						dao.setRecord("regTime", curDate.substring(8));
						dao.setRecord("regDate", curDate);
						if (res.pay.compNo != null) {
							dao.setRecord("compNo", res.pay.compNo);
						} else {
							dao.setRecord("compNo", "");
						}
						if (res.pay.compMember != null) {
							dao.setRecord("compMember", res.pay.compMember);
						} else {
							dao.setRecord("compMember", "");
						}
						if (dao.insert()) {
							logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_RES OK");
						} else {
							logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_RES FAIL");
							Api.sendMsg(rc, resA.get("RETURNCODE"), "승인실패", resA.get("RETURNMSG"));
							return false;
						}
						logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_RES END");
						dao.initRecord();
						if (res.result.resultCd.equals("0000")) {
							logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_PAY BEGIN");
						//	insertTrxPAY(res.pay.trxId);
							String q = "INSERT INTO PG_TRX_PAY  " + " SELECT A.trxId,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,amount,installment,cardId,cardType,bin,last4,'승인',prodId,issuer,acquirer,"
									+ " A.regDay,A.regTime,authCd,resultCd,resultMsg,van,vanId,vanTrxId,B.pairingVtid,B.pairingRouteVan,B.pairingCid,B.regDay,B.regTime,B.regDate,A.regDate, A.compNo, A.compMember " + " FROM PG_TRX_REQ A, PG_TRX_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
							if (dao.update(q)) {
								logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_PAY OK");
							} else {
								logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_PAY FAIL");
								Api.sendMsg(rc, resA.get("RETURNCODE"), "승인실패", resA.get("RETURNMSG"));
								return false;
							}
							logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_PAY END");
						} else {
							logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_ERR BEGIN");
						//	insertTrxERR(res.pay.trxId);
							String q = "INSERT INTO PG_TRX_ERR  " + " SELECT A.trxId,trxType,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,amount,installment,cardId,cardType,bin,last4,issuer,acquirer,prodId,"
									+ " A.regDay,A.regTime,resultCd,resultMsg,van,vanId,vanTrxId,vanResultCd,vanResultMsg,B.pairingVtid,B.pairingRouteVan,B.pairingCid,B.regDay,B.regTime,B.regDate,A.regDate, A.compNo, A.compMember " + " FROM PG_TRX_REQ A, PG_TRX_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
							if (dao.update(q)) {
								logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_ERR OK");
							} else {
								logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_ERR FAIL");
								Api.sendMsg(rc, resA.get("RETURNCODE"), "승인실패", resA.get("RETURNMSG"));
								return false;
							}
							logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - DB PG_TRX_ERR END");
						}
					
					if (resA.get("RETURNCODE").equals("0000")) {
						logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 성공 - 완료");
						Api.sendMsg(rc, resA.get("RETURNCODE"), "승인성공", resA.get("RETURNMSG"));
						return true;	
					} else {
						logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 실패 - 완료");
						Api.sendMsg(rc, resA.get("RETURNCODE"), "승인실패", resA.get("RETURNMSG"));
						return false;
					}
				} else {
					logger.info("========== ========== ========== ========== authReturn 다날 간편결제 인증결제 실패");
					Api.sendMsg(rc, resA.get("RETURNCODE"), "인증실패", resA.get("RETURNMSG"));
					return false;
				}
			} else {
				logger.info("========== ========== ========== ========== authReturn DATA ERROR");
				Api.sendMsg(rc, resData.get("RETURNCODE"), "인증실패", resData.get("RETURNMSG"));
				return false;
			}
		} else {
			logger.info("========== ========== ========== ========== authReturn URL ERROR");
			return false;
		}
	}
	
	public synchronized static HashMap<String,String> CallCredit(HashMap<String,String> data) {
		logger.debug("========== ========== ========== ========== ========== DANAL CallCredit");
		logger.info("========== ========== ========== ========== data : " + data);
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
			logger.debug("========== ========== ========== ========== DANAL connect URL : " + DN_CREDIT_URL);
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
	
	public synchronized static HashMap<String,String> CallTrans(HashMap<String,String> data) {
		logger.debug("========== ========== ========== ========== ========== DANAL CallTrans");
		logger.info("========== ========== ========== ========== data : " + data);
		HashMap<String, String> resData = new HashMap<String, String>();
		
		String REQ_STR = toQueryString(data);
		String RES_STR = "";
		
		HttpClient hc = new HttpClient();
		
		hc.setConnectionTimeout(DN_CONNECT_TIMEOUT);
		hc.setTimeout(DN_TIMEOUT);
		
		try {
			int nStatus = hc.retrieve("POST", DN_CREDIT_URL, REQ_STR, CHARSET, CHARSET);
			
			if (nStatus != 0) {
				RES_STR = "RETURNCODE=-1&RETURNMSG=NETWORK ERROR(" + nStatus + ")";
			} else {
				RES_STR = hc.getResponseBody();
				
				resData = parseQueryString(RES_STR);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			String message = "NOTCONNECTED";
			resData.put("RETURNCODE", "XXXX");
			resData.put("RETURNMSG", message);
			logger.debug("danal,error : {}", e.getMessage());
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
				data.put(st[i].substring(0, index), changeCharset(urlDecode(st[i].substring(index + 1)),"euc-kr"));
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
		logger.debug("========== ========== ========== ========== ========== toEncrypt IVKEY : " + IVKEY);
		logger.debug("========== ========== ========== ========== ========== toEncrypt CRYPTOKEY : " + CRYPTOKEY);
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
	
	public static String toDecrypt(String originalMsg) {
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
			byte[] decrypted = cipher.doFinal(Base64.decodeBase64(originalMsg.getBytes()));
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
	
	public static void sendMsgDanalAuth(RoutingContext rc, String code, String statusMessage, String message, String startUrl, String startParams, String ciUrl, String color) {
		logger.info("========== ========== ========== ========== sendMsgDanalAuth()");
		logger.info("========== ========== ========== ========== code : \t" + code);
		logger.info("========== ========== ========== ========== statusMessage : \t" + statusMessage);
		logger.info("========== ========== ========== ========== message : \t" + message);
		logger.info("========== ========== ========== ========== startUrl : \t" + startUrl);
		logger.info("========== ========== ========== ========== startParams : \t" + startParams);
		logger.info("========== ========== ========== ========== ciUrl : \t" + ciUrl);
		logger.info("========== ========== ========== ========== color : \t" + color);
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put("code", code);
		responseMap.put("statusMessage", statusMessage);
		responseMap.put("message", message);
		responseMap.put("startUrl", startUrl);
		responseMap.put("startParams", startParams);
		responseMap.put("ciUrl", ciUrl);
		responseMap.put("color", color);
		
		try {
			VertXMessage.set200(rc, "application/json", GsonUtil.toJson(responseMap), "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void sendMsgDanalAuth(RoutingContext rc, String code, String statusMessage, String message, String startUrl, String startParams) {
		logger.info("========== ========== ========== ========== sendMsgDanalAuth()");
		logger.info("========== ========== ========== ========== code : \t" + code);
		logger.info("========== ========== ========== ========== statusMessage : \t" + statusMessage);
		logger.info("========== ========== ========== ========== message : \t" + message);
		logger.info("========== ========== ========== ========== startUrl : \t" + startUrl);
		logger.info("========== ========== ========== ========== startParams : \t" + startParams);
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put("code", code);
		responseMap.put("statusMessage", statusMessage);
		responseMap.put("message", message);
		responseMap.put("startUrl", startUrl);
		responseMap.put("startParams", startParams);
		
		try {
			VertXMessage.set200(rc, "application/json", GsonUtil.toJson(responseMap), "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
