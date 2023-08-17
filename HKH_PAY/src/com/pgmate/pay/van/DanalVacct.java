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
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.bean.DanaNotiBean;
import com.pgmate.pay.bean.DanalBean;
import com.pgmate.pay.main.Api;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class DanalVacct {

	private static Logger logger 							= LoggerFactory.getLogger(com.pgmate.pay.van.DanalVacct.class);
	/*****************************************************
	 * 다날 가상계좌 결제
	 *****************************************************/

	/*****************************************************
	 * 연동에 필요한 Function 및 변수값 설정
	 *
	 * 연동에 대한 문의사항 있으시면 아래 메일주소로 연락 주십시오.
	 * DANAL Commerce Division Technique supporting Team 
	 * EMail : vac_tech@danal.co.kr	 
	 ******************************************************/

	/******************************************************
	 *  DN_TX_URL	: 결제 서버 정의
	 ******************************************************/
	static final String DN_TX_URL 							= "https://tx-vaccount.danalpay.com/vaccount/";
	
	/******************************************************
	 *  Set Timeout
	 ******************************************************/
	private static final int DN_CONNECT_TIMEOUT 	= 5000;
	private static final int DN_TIMEOUT 					= 30000; //SO_Timeout setting

	private static final String ERC_NETWORK_ERROR 	= "-1";
	private static final String ERM_NETWORK 			= "Network Error";

	/******************************************************
	 * CPID		: 다날에서 제공해 드린 CPID
	 * CRYPTOKEY	: 다날에서 제공해 드린 암복호화 PW
	******************************************************/
	private static String CPID 										= ""; //영업담당자에게 문의
	private static String CRYPTOKEY 								= ""; //영업담당자에게 문의
	private static String IVKEY 										= "45b913a44d61353d20402a2518de592a"; //수정하지 마세요.

	public static String CHARSET = "EUC-KR";
	public String TEST_AMOUNT = "1004";
	
	private static String VAN 										= ""; //영업담당자에게 문의
	
	static {
	    disableSslVerification();
	}
	
	public DanalVacct(){
		
	}
	
	public DanalVacct(SharedMap<String, Object> vanMap) {
		CPID =  vanMap.getString("vanId").trim();
		CRYPTOKEY	= vanMap.getString("vacctKey").trim();
		logger.info("========== DanalVacct()");
		logger.info("========== CPID : " + CPID);
		logger.info("========== CRYPTOKEY : " + CRYPTOKEY);
		logger.info("==========");
	}

	public synchronized static boolean vacctPay(RoutingContext rc, String uri) {
		DAO dao = new DAO();
		SharedMap<String, Object> sharedMap = new SharedMap<String, Object>();
		if (uri.startsWith("/api/vacct/danal")) {
			System.out.println();
			DanalBean danalBean = (DanalBean) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), DanalBean.class);
			dao.initRecord();
			dao.setDebug(true);
			dao.setColumns("mchtId, van, vanId, vacctKey");
			dao.setTable("VW_MCHT_TMN");
			dao.addWhere("mchtId", danalBean.mchtId);
			dao.setOrderBy("");
			SharedMap <String, Object> vanMap = dao.search().getRowFirst();
			if (vanMap.getString("mchtId").equals(danalBean.mchtId)) {
				CPID =  vanMap.getString("vanId").trim();
				CRYPTOKEY	= vanMap.getString("vacctKey").trim();
				VAN	= vanMap.getString("van").trim();
				
				
				LinkedHashMap<String, String> reqA = new LinkedHashMap<String, String>();
				reqA.put("CPID", CPID);
				//reqA.put("SUBCPID", "S_" + CPID);
				/**************************************************
				 * 결제 정보
				 **************************************************/
				reqA.put("ACCOUNTHOLDER", danalBean.accntHolder);
				
				reqA.put("AMOUNT", danalBean.amount);
				reqA.put("ITEMNAME", danalBean.prdName);
				//String expireDate = CommonUtil.getCurrentDate("yyyyMMddhhmmss");
				//reqA.put("EXPIREDATE", expireDate.substring(0, 8));
				reqA.put("EXPIREDATE", danalBean.expireDate);
				String orderDate = CommonUtil.getCurrentDate("yyyyMMddhhmmss");
			 	reqA.put("ORDERID", "DNVAC" + orderDate);
				reqA.put("BANKCODEBASE", danalBean.bankCd);
		
				reqA.put("BYPASSVALUE", "BYPASSVALUE_TEST");
				reqA.put("ISCASHRECEIPTUI", "Y");
				
				/**************************************************
				 * 구매자 정보
				 **************************************************/
				reqA.put("USERID", "user_tester");
				reqA.put("USERNAME", danalBean.payerName);
				reqA.put("USEREMAIL", danalBean.payerEmail);
				reqA.put("USERAGENT", danalBean.userAgent);
		
				/**************************************************
				 * 연동 정보
				 **************************************************/
				reqA.put("RETURNURL", danalBean.returnUrl);
				reqA.put("NOTIURL", danalBean.notiUrl);
				reqA.put("CANCELURL", danalBean.cancelUrl);
				
				reqA.put("TXTYPE", danalBean.txType);
				reqA.put("SERVICETYPE", danalBean.serviceType);
				
				/**
				 * 추가정보 입력
				 */
				//reqA.put("ISCASHRECEIPTUI", "Y");
		
				/**************************************************
				 * 가상계좌 발급요청전문 발송 및 응답처리
				 **************************************************/
				//req = CallVAccount(req, false);
				HashMap<String, String> resA = CallVaccount(reqA);
				
				dao.initRecord();
				dao.setTable("PG_VACCT_DANAL");
				dao.setRecord("mchtId", danalBean.mchtId);
				dao.setRecord("van", VAN);
				dao.setRecord("vanId", CPID);
				dao.setRecord("trxId", reqA.get("ORDERID"));
				dao.setRecord("accntHolder", danalBean.accntHolder);
				dao.setRecord("bankCd", danalBean.bankCd);
				dao.setRecord("expireDate", danalBean.expireDate);
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
				dao.setRecord("txType", danalBean.txType);
				dao.setRecord("txTypePath", "CP");
				dao.setRecord("serviceType", danalBean.serviceType);
				if (danalBean.byPassValue != null || !danalBean.byPassValue.equals("")) {
					dao.setRecord("byPassValue", danalBean.byPassValue);
				}
				
				if (CommonUtil.toString(resA.get("RETURNCODE")).equals("0000")) {
					dao.setRecord("vanTrxId", resA.get("TID"));
					dao.setRecord("vanResultCd", resA.get("RETURNCODE"));
					dao.setRecord("vanResultMsg", resA.get("RETURNMSG"));
					
					dao.setRecord("regId", danalBean.userId);
					dao.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
					dao.setRecord("regTime", CommonUtil.getCurrentDate("hhmmss"));
					
					if (dao.insert()) {
						logger.info("========== ========== ========== ========== 다날 가상계좌 발급 - 인증 성공 ");
						logger.info("========== ========== ========== ========== RETURNCODE : " + CommonUtil.toString(resA.get("RETURNCODE")));
						logger.info("========== ========== ========== ========== RETURNMSG : " + CommonUtil.toString(resA.get("RETURNMSG")));
						logger.info("========== ========== ========== ========== 다날 가상계좌 발급 - 인증 성공 TID : " + resA.get("TID"));
						logger.info("========== ========== ========== ========== 다날 가상계좌 발급 - 인증 성공 AMOUNT : " + resA.get("AMOUNT"));
						logger.info("========== ========== ========== ========== 다날 가상계좌 발급 - 인증 성공 ORDERID : " + resA.get("ORDERID"));
						
						sendMsgDanalAuth(rc, CommonUtil.toString(resA.get("RETURNCODE")), "인증성공", CommonUtil.toString(resA.get("RETURNMSG")), 
								resA.get("STARTURL"), resA.get("STARTPARAMS"), resA.get("CIURL"), resA.get("COLOR"));
						return true;
					} else {
						logger.info("========== ========== ========== ========== 다날 가상계좌 발급 - 인증 DB INSERT ERROR ");
					}
				} else {
					if (dao.insert()) {
						logger.info("========== ========== ========== ========== 다날 가상계좌 발급 - 인증 실패 ");
						logger.info("========== ========== ========== ========== RETURNCODE : " + CommonUtil.toString(resA.get("RETURNCODE")));
						logger.info("========== ========== ========== ========== RETURNMSG : " + CommonUtil.toString(resA.get("RETURNMSG")));
						if (resA.get("RETURNCODE").equals("1158")) {
							Api.sendMsg(rc, CommonUtil.toString(resA.get("RETURNCODE")), "승인실패", CommonUtil.toString(resA.get("RETURNMSG") + " - 다른 은행을 선택하세요."));
						} else {
							Api.sendMsg(rc, CommonUtil.toString(resA.get("RETURNCODE")), "승인실패", CommonUtil.toString(resA.get("RETURNMSG")));						
						}
					} else {
						logger.info("========== ========== ========== ========== 다날 가상계좌 발급 - 인증 실패 DB INSERT ERROR ");
					}
					return false;
				}
				/*
				sharedMap.put("van", VAN);
				sharedMap.put("vanId", CPID);
				sharedMap.put("vanTrxId", CommonUtil.toString(resD.get("TID")));
				sharedMap.put("vanResultCd", CommonUtil.toString(resD.get("RETURNCODE")));
				sharedMap.put("vanResultMsg", CommonUtil.toString(resD.get("RETURNMSG")));
				sharedMap.put("vanDate", CommonUtil.toString(resD.get("TRANDATE")) + CommonUtil.toString(resD.get("TRANTIME")));
				logger.info("vanTrxId : {},{}", sharedMap.getString("vanTrxId"), sharedMap.getString("vanDate"));
				*/
			}
			return false;
		}
		return false;
	}
	
	public synchronized static boolean vacctAuthReturn(RoutingContext rc, String uri) {
		DAO dao = new DAO();
		SharedMap<String, Object> sharedMap = new SharedMap<String, Object>();
		if (uri.startsWith("/api/vacct/danal/return")) {
			System.out.println();
			logger.debug("========== ========== ========== ========== ========== vacctAuthReturn 계좌발급 인증리턴");
			//DanaReturnBean data = (DanaReturnBean) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), DanaReturnBean.class);
			//String encData = data.RETURNPARAMS;
			String encData = VertXUtil.getBodyAsString(rc).split("=")[1];
			logger.debug("========== ========== ========== ========== ========== encData : " + encData);
			
			String decrypted = toDecrypt(urlDecode((Object) encData));
			logger.debug("========== ========== ========== ========== ========== decrypted : " + decrypted);
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
				dao.setDebug(true);
				dao.initRecord();
				dao.setColumns("*");
				dao.setTable("PG_VACCT_DANAL");
				dao.addWhere("vanTrxId", resData.get("TID"));
				SharedMap<String, Object> selectMap = dao.search().getRowFirst();
				
				LinkedHashMap<String, String> reqA = new LinkedHashMap<String, String>();
				/**************************************************
				 * 결제 정보
				 **************************************************/
				reqA.put("TID", resData.get("TID"));
				reqA.put("AMOUNT", (String) selectMap.get("amount"));
				/**************************************************
				 * 연동 정보
				 **************************************************/
				reqA.put("TXTYPE", "ISSUEVACCOUNT");
				reqA.put("SERVICETYPE", "DANALVACCOUNT");
				
				HashMap<String, String> resA = CallVaccount(reqA);
				
				if (resA.get("RETURNCODE").equals("0000")) {
					dao.setDebug(true);
					dao.initRecord();
					dao.setTable("PG_VACCT_DANAL");
					dao.setRecord("txType", "ISSUEVACCOUNT");
					dao.setRecord("serviceType", "DANALVACCOUNT");
					logger.info("========== resA ");
					logger.info("========== resA BANKCODE : " + resA.get("BANKCODE"));
					logger.info("========== resA BANKNAME : " + resA.get("BANKNAME"));
					logger.info("========== resA EXPIREDATE : " + resA.get("EXPIREDATE"));
					logger.info("========== resA EXPIRETIME : " + resA.get("EXPIRETIME"));
					logger.info("========== resA VIRTUALACCOUNT : " + resA.get("VIRTUALACCOUNT"));
					logger.info("========== resA ISCASHRECEIPT : " + resA.get("ISCASHRECEIPT"));
					logger.info("========== resA AMOUNT : " + resA.get("AMOUNT"));
					logger.info("========== resA ");
					dao.setRecord("bankCd", resA.get("BANKCODE"));
					dao.setRecord("bankName", resA.get("BANKNAME"));
					dao.setRecord("expireDate", resA.get("EXPIREDATE"));
					dao.setRecord("expireTime", resA.get("EXPIRETIME"));
					dao.setRecord("account", resA.get("VIRTUALACCOUNT"));
					dao.setRecord("isCashReceipt", resA.get("ISCASHRECEIPT"));
					dao.setRecord("amount", resA.get("AMOUNT"));
					dao.setRecord("updDate", CommonUtil.getCurrentDate("yyyy-MM-dd hh:mm:ss"));
					dao.addWhere("vanTrxId", resData.get("TID"));
					
					if (dao.update()) {
						VertXMessage.set200(rc, "application/json", "OK", "");
						return true;
					} else {
						logger.info("========== ========== ========== ========== vacctAuthReturn 가상계좌 발급 인증 성공 - DB 업데이트 실패");
						VertXMessage.set200(rc, "application/json", "NOK", "");
					}
				} else {
					logger.info("========== ========== ========== ========== vacctAuthReturn 가상계좌 발급 인증 실패");
					VertXMessage.set200(rc, "application/json", "NOK", "");
					return false;
				}
			} else {
				logger.info("========== ========== ========== ========== vacctAuthReturn DATA ERROR");
				VertXMessage.set200(rc, "application/json", "NOK", "");
				return false;
			}
		} else {
			logger.info("========== ========== ========== ========== vacctAuthReturn URL ERROR");
			VertXMessage.set200(rc, "application/json", "NOK", "");
			return false;	
		}
		return false;
	}
	public synchronized static boolean vacctPayNoti(RoutingContext rc, String uri) {
		DAO dao = new DAO();
		SharedMap<String, Object> sharedMap = new SharedMap<String, Object>();
		if (uri.startsWith("/api/vacct/danal/noti")) {
			System.out.println();
			logger.debug("========== ========== ========== ========== ========== DANAL VACCT 입금통보");
			DanaNotiBean notiData = (DanaNotiBean) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc), DanaNotiBean.class);
			/*
			logger.debug("========== ========== ========== ========== ========== notiData : " + notiData);
			JSONParser notiParser = new JSONParser();
			Object notiObj = new Object();
			try {
				notiObj = notiParser.parse(notiData);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			JSONObject notijObj = (JSONObject) notiObj;
			
			logger.info("========== ========== ========== ========== notijObj toJSONString : " + notijObj.toJSONString());
			logger.info("========== ========== ========== ========== notijObj : " + notijObj.toString());
			logger.info("========== ========== ========== ========== ");
			logger.info("========== ========== ========== ========== TID : " + notijObj.get("TID"));
			logger.info("========== ========== ========== ========== RETURNCODE : " + notijObj.get("RETURNCODE"));
			logger.info("========== ========== ========== ========== RETURNMSG : " + notijObj.get("RETURNMSG"));
			logger.info("========== ========== ========== ========== ORDERID : " + notijObj.get("ORDERID"));
			 */
			logger.info("========== ========== ========== ========== RETURNCODE : " + notiData.RETURNCODE);
			logger.info("========== ========== ========== ========== RETURNMSG : " + notiData.RETURNMSG);
			logger.info("========== ========== ========== ========== TID : " + notiData.TID);
			logger.info("========== ========== ========== ========== ORDERID : " + notiData.ORDERID);
			if (notiData.RETURNCODE != null && notiData.RETURNMSG != null && notiData.TID != null) {
				dao.initRecord();
				dao.setTable("PG_VACCT_DANAL_DEPOSIT");
				dao.setRecord("vanResultCd", notiData.RETURNCODE);
				dao.setRecord("vanResultMsg", notiData.RETURNMSG);
				dao.setRecord("vanTrxId", notiData.TID);
				dao.setRecord("trxId", notiData.ORDERID);
				if (notiData.AMOUNT.equals("")) {
					dao.setRecord("amount", 0);
				} else {
					dao.setRecord("amount", Integer.parseInt(notiData.AMOUNT));
				}
				dao.setRecord("tranDate", notiData.TRANDATE);
				dao.setRecord("tranTime", notiData.TRANTIME);
				dao.setRecord("account", notiData.VIRTUALACCOUNT);
				dao.setRecord("depositUserName", notiData.DEPOSITUSERNAME);
				dao.setRecord("accntHolder", notiData.ACCOUNTHOLDER);
				dao.setRecord("depositUserName", notiData.DEPOSITUSERNAME);
				dao.setRecord("userId", notiData.USERID);
				dao.setRecord("payerEmail", notiData.USERMAIL);
				dao.setRecord("prdName", notiData.ITEMNAME);
				dao.setRecord("byPassValue", notiData.BYPASSVALUE);
				dao.setRecord("expireDate", notiData.EXPIREDATE);
				dao.setRecord("expireTime", notiData.EXPIRETIME);
				dao.setRecord("bankCd", notiData.BANKCODE);
				dao.setRecord("bankName", notiData.BANKNAME);
				dao.setRecord("isCashReceipt", notiData.ISCASHRECEIPT);
				if (notiData.RETURNCODE.equals("0000")) {
					if (dao.insert()) {
						logger.info("========== ========== ========== ========== DANAL VACCT noti DB OK");
						//Api.sendMsg(rc, "OK", "정산", "정산승인");
						VertXMessage.set200(rc, "application/json", "OK", "");
						return true;
					} else {
						logger.info("========== ========== ========== ========== DANAL VACCT noti DB ERROR");
						VertXMessage.set200(rc, "application/json", "NOK", "");
					}
				} else {
					logger.info("========== ========== ========== ========== DANAL VACCT noti ERROR");
					VertXMessage.set200(rc, "application/json", "NOK", "");
					return false;
				}
			} else {
				logger.info("========== ========== ========== ========== DANAL VACCT noti data ERROR");
				VertXMessage.set200(rc, "application/json", "NOK", "");
				return false;
			}
		} else {
			VertXMessage.set200(rc, "application/json", "NOK", "");
			return false;	
		}
		VertXMessage.set200(rc, "application/json", "NOK", "");
		return false;
	}
	
	public synchronized static HashMap<String,String> connect(String startUrl, HashMap<String,String> data) {
		logger.info("");
		logger.debug("========== ========== ========== ========== ========== DANAL VACCT connect");
		//String req = toQueryString(data);
		String req = urlEncode(toEncrypt(toQueryString(data)));
		logger.info("");
		
		logger.info("");
		logger.info("========== ========== ========== ========== connect send : [{}]", req);
		HashMap<String, String> resData = new HashMap<String, String>();
		long time = System.currentTimeMillis();
		String message = "";
		UrlClient client = null;
		try {
			client = new UrlClient(startUrl, "POST", "application/x-www-form-urlencoded; charset=euc-kr");
			client.setTimeout(DN_CONNECT_TIMEOUT, DN_TIMEOUT);
			client.setDoInputOutput(true, true);
			message = client.connect(req);
			
			logger.debug("DANAL,VACCT recv : [{}]", message);
			if (message.indexOf("RETURNCODE") > -1) {
				resData = parseQueryString(message);
			} else if (message.indexOf("DATA=") > -1) {
				String decrypted = toDecrypt(urlDecode(message.split("=")[1]));
				resData = parseQueryString(decrypted);
				
				logger.debug("========== ");
				logger.debug("DANAL,VACCT data decrypted : {}", decrypted);
				logger.debug("========== ");
				logger.debug("DANAL,VACCT data resData : {}", resData);
				logger.debug("========== ");
			} else {
				logger.debug("DANAL,VACCT data error : {}", message);
			}

		} catch (Exception e) {
			message = "DANAL,VACCT NOTCONNECTED";
			resData.put("RETURNCODE", "XXXX");
			resData.put("RETURNMSG", message);
			logger.debug("DANAL,error : {}", e.getMessage());
		} finally {
			logger.debug("DANAL Elasped Time =[{} sec]", CommonUtil.parseDouble((System.currentTimeMillis() - time) / 1000));
			logger.debug("DANAL, res = [{}]", resData);
		}
		return resData;
	}
	public synchronized static HashMap<String,String> CallVaccount(HashMap<String,String> data) {
		logger.debug("========== ========== ========== ========== ========== CallVaccount()");
		String req = "CPID=" + CPID + "&DATA=" + urlEncode(toEncrypt(toQueryString(data)));
		logger.info("========== ========== ========== ========== CPID : " + CPID);
		logger.info("========== ========== ========== ========== send : [{}]", req);
		HashMap<String, String> resData = new HashMap<String, String>();
		long time = System.currentTimeMillis();
		String message = "";
		UrlClient client = null;
		try {
			logger.info("========== send URL : " + DN_TX_URL);
			client = new UrlClient(DN_TX_URL, "POST", "application/x-www-form-urlencoded; charset=euc-kr");
			client.setTimeout(DN_CONNECT_TIMEOUT, DN_TIMEOUT);
			client.setDoInputOutput(true, true);
			message = client.connect(req);
			
			logger.debug("DANAL VACCT CallVaccount message : ", message);
			if (message.indexOf("RETURNCODE") > -1) {
				resData = parseQueryString(message);
			} else if (message.indexOf("DATA=") > -1) {
				String decrypted = toDecrypt(urlDecode(message.split("=")[1]));
				resData = parseQueryString(decrypted);
			} else {
				logger.debug("DANAL,VACCT CallVaccount data error : {}", message);
			}
			
		} catch (Exception e) {
			message = "DANAL,VACCT CallVaccount NOTCONNECTED";
			resData.put("RETURNCODE", "XXXX");
			resData.put("RETURNMSG", message);
			logger.debug("danal,error : {}", e.getMessage());
		} finally {
			logger.debug("DANAL CallVaccount Elasped Time =[{} sec]", CommonUtil.parseDouble((System.currentTimeMillis() - time) / 1000));
			logger.debug("DANAL CallVaccount, res = [{}]", resData);
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
		logger.info("\n" + sbb);
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
		logger.info("\n" + sbb);
		return data;

	}
	
	
	
	
	public static String urlEncode(Object obj) {
		if (obj == null)
			return null;
		
		try {
			return URLEncoder.encode(obj.toString(), CHARSET);
		} catch (Exception e) {
			logger.warn("========== ========== ========== ========== urlEncode() - Exception : " + e);
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
			logger.warn("========== ========== ========== ========== urlDecode() - Exception : " + e);
			return obj.toString();
		}
	}

	
	public static String toEncrypt(String originalMsg)  {
		//logger.debug("========== ========== ========== ========== ========== toEncrypt originalMsg : " + originalMsg);
		//logger.debug("========== ========== ========== ========== ========== toDecrypt IVKEY : " + IVKEY);
		//logger.debug("========== ========== ========== ========== ========== toDecrypt CRYPTOKEY : " + CRYPTOKEY);
		String AESMode = "AES/CBC/PKCS5Padding";
		String SecetKeyAlgorithmString = "AES";
		//logger.debug("request : [{}]",originalMsg);
		IvParameterSpec ivspec = new IvParameterSpec(hexToByteArray(IVKEY));
		SecretKey keySpec = new SecretKeySpec(hexToByteArray(CRYPTOKEY), SecetKeyAlgorithmString);
		try {
			Cipher cipher = Cipher.getInstance(AESMode);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivspec);
			byte[] encrypted = cipher.doFinal(originalMsg.getBytes());
			return new String(Base64.encodeBase64(encrypted));
		} catch (Exception e) {
			logger.warn("========== ========== ========== ========== urlDecode() - Encrypt Exception : " + e);
			logger.warn("========== ========== ========== ========== Encrypt error : {}",e.getMessage());
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
			byte[] decrypted = cipher.doFinal(Base64.decodeBase64(originalMsg));
			String retValue = new String(decrypted);

			return retValue;
		} catch (Exception e) {
			logger.warn("========== ========== ========== ========== urlDecode() - Decrypt Exception : " + e);
			logger.warn("========== ========== ========== ========== Decrypt error : {}",e.getMessage());
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
			logger.warn("========== ========== ========== ========== urlDecode() - Decrypt UnsupportedEncodingException : " + e);
			logger.warn("========== ========== ========== ========== UnsupportedEncodingException error : {}",e.getMessage());
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
			logger.warn("========== ========== ========== ========== disableSslVerification() - NoSuchAlgorithmException : " + e);
			logger.warn("========== ========== ========== ========== NoSuchAlgorithmException error : {}",e.getMessage());
			e.printStackTrace();
		} catch (KeyManagementException e) {
			logger.warn("========== ========== ========== ========== disableSslVerification() - KeyManagementException : " + e);
			logger.warn("========== ========== ========== ========== KeyManagementException error : {}",e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void sendMsgDanalAuth(RoutingContext rc, String code, String statusMessage, String message, String startUrl, String startParams, String ciUrl, String color) {
		logger.info("========== ========== ========== ========== ========== sendMsgDanalAuth()");
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
			logger.warn("========== ========== ========== ========== sendMsgDanalAuth() - Exception : " + e);
			logger.warn("========== ========== ========== ========== Exception error : {}",e.getMessage());
			e.printStackTrace();
		}
	}
	
}
