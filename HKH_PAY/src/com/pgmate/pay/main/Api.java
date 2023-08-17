package com.pgmate.pay.main;

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.util.validation.StringValidator;
import com.pgmate.lib.util.xml.XmlUtil;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.Proc;
import com.pgmate.pay.proc.ProcAuth;
import com.pgmate.pay.proc.ProcBillPay;
import com.pgmate.pay.proc.ProcBillUser;
import com.pgmate.pay.proc.ProcBillWidget;
import com.pgmate.pay.proc.ProcCashCoocon;
import com.pgmate.pay.proc.ProcEcho;
import com.pgmate.pay.proc.ProcGet;
import com.pgmate.pay.proc.ProcPay;
import com.pgmate.pay.proc.ProcPay3DHook;
import com.pgmate.pay.proc.ProcPay3DWidget;
import com.pgmate.pay.proc.ProcRefund;
import com.pgmate.pay.proc.ProcWebHook;
import com.pgmate.pay.proc.ProcWebHookAllat;
import com.pgmate.pay.proc.ProcWebHookDaou;
import com.pgmate.pay.proc.ProcWebHookNice;
import com.pgmate.pay.proc.ProcWebHookPairing;
import com.pgmate.pay.proc.ProcWebHookSmartro;
import com.pgmate.pay.proc.ProcWebHookSpcn;
import com.pgmate.pay.proc.ProcWidget;
import com.pgmate.pay.proc.VactClose;
import com.pgmate.pay.proc.VactGet;
import com.pgmate.pay.proc.VactOpen;
import com.pgmate.pay.proc.VactPatch;
import com.pgmate.pay.proc.VactStatus;
import com.pgmate.pay.util.APIComp;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.TemplateUtil;
import com.pgmate.pay.van.Danal;
import com.pgmate.pay.van.DanalVacct;
import com.pgmate.pay.van.SmartroVacct;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class Api {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.main.Api.class);
	//public static SharedMap<String, Object> sharedMap = new SharedMap<String, Object>();
	//private SharedMap<String, SharedMap<String, Object>> sharedObject = new SharedMap<String, SharedMap<String, Object>>();
	//private Request request = null;
	private TrxDAO trxDAO = new TrxDAO();

	public Api() {
		// TODO Auto-generated constructor stub
	}

	public synchronized void apiHandler(RoutingContext rc) {
		logger.info("========== ========== ========== ========== ========== apiHandler() - BEGIN");
		SharedMap<String, Object> sharedMap = new SharedMap<String, Object>();
		SharedMap<String, SharedMap<String, Object>> sharedObject = new SharedMap<String, SharedMap<String, Object>>();
		Request request = (Request) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc).trim(), Request.class);
		// 접속 URI 확인
		String uri = CommonUtil.nToB(rc.request().uri());
		logger.info("========== apiHandler() -  uri : {}, method : {}, ip : {}", uri, VertXUtil.getMethod(rc), VertXUtil.getXRealIp(rc));

		// 외부업체 결제 연동 API
		//if (uri.startsWith("/api/comp/callBack")) {
		if (uri.equals("/api/comp/callBack")) {
			APIComp.APICompCallBack(rc, uri);
			return;
		} else if (uri.equals("/api/comp/pay")) {
			APIComp.APICompPay(rc, uri);
			return;
		} else if (uri.equals("/api/comp/refund")) {
			APIComp.APICompRefund(rc, uri);
			return;
		}
		// 2022-08-05 - 외부업체 결제 연동 API - 인증결제
		if (uri.equals("/api/comp/callBackAuth")) {
			APIComp.APICompCallBackAuth(rc, uri);
			return;
		} else if (uri.equals("/api/comp/payAuth")) {
			APIComp.APICompPayAuth(rc, uri);
			return;
		}
		
		// 가상계좌 - 다날
		if (uri.startsWith("/api/vacct/danal/cancel")) {
			DanalVacct.vacctPayNoti(rc, uri);
			return;
		} else if (uri.startsWith("/api/vacct/danal/noti")) {
			DanalVacct.vacctPayNoti(rc, uri);
			return;
		} else if (uri.startsWith("/api/vacct/danal/return")) {
			DanalVacct.vacctAuthReturn(rc, uri);
			return;
		} else if (uri.startsWith("/api/vacct/danal")) {
			DanalVacct.vacctPay(rc, uri);
			return;
		// 가상계좌 - 스마트로 발급
		} else if (uri.startsWith("/api/vacct/smartro/auth")) {
			SmartroVacct.vacct(rc, uri);
			return;
		// 가상계좌 - 스마트로 입금 통보
		} else if (uri.startsWith("/api/vacct/smartro/noti")) {
			SmartroVacct.vacctNoti(rc, uri);
			return;
		// 가상계좌 - 스마트로 입금 통보
		} else if (uri.startsWith("/api/vacct/smartro/refund")) {
			SmartroVacct.vacct(rc, uri);
			return;
		}
		
		// 카드 - 다날 인증결제
		if (uri.startsWith("/api/pay/danal/redirect")) {
			// 웹 간편결제 페이지 이동처리 
			logger.info("========== 간편결제 이동처리 START");
			String encData = StringEscapeUtils.unescapeJava(VertXUtil.getBodyAsString(rc).split("=")[1]);
			logger.info("========== 간편결제 이동처리 encData : " + encData);
			// 세션아이디 가져오기
			String decrypted = Danal.toDecrypt((String) Danal.urlDecode((Object) encData));
			HashMap<String, String> resData = new HashMap<String, String>();
			resData = Danal.parseQueryString(decrypted);
			DAO dao = new DAO();
			dao.setDebug(true);
			dao.initRecord();
			dao.setColumns("*");
			dao.setTable("PG_TRX_DANAL");
			dao.addWhere("vanTrxId", resData.get("TID"));
			SharedMap<String, Object> selectMap = dao.search().getRowFirst();
			String session_userId = selectMap.getString("userId");
			String mchtId = selectMap.getString("mchtId");
			logger.info("========== 간편결제 이동처리 session_userId : " + session_userId);
			logger.info("========== 간편결제 이동처리 mchtId : " + mchtId);
			
			//TemplateUtil.redirectPayDanal(rc, "http://10.10.11.20:8090/HH/pay/sugipay/redirect.jsp", encData);
			String sumData = encData + "," + session_userId + "," + mchtId;
			TemplateUtil.redirectPayDanal(rc, "http://10.10.11.20:8090/HH/pay/sugipay/form", sumData);
			//TemplateUtil.redirectPayDanal(rc, "http://10.10.11.20:8090/HH/pay/sugipay/danal/redirect", sumData);
			logger.info("========== 간편결제 이동처리 END");
			return;
			//TemplateUtil.redirect3D(rc, "http://10.10.11.20:8090/HH/pay/sugipay/form", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
		}
		
		// 시스템 거래 메세지 생성 : msg_ 및 기록
		//String trxId = TrxDAO.getTrxId();
		String trxId = "";
		//String trxId = TrxDAO.getTrxId();
		Request req_chk = (Request) GsonUtil.fromJson(VertXUtil.getBodyAsString(rc).trim(), Request.class);
		if (req_chk != null) {
			//logger.info("========== ========== ========== ========== ========== url pay/auth - req_chk NOT NULL");
			if (req_chk.pay != null) {
				if (CommonUtil.isNullOrSpace(req_chk.pay.trxId)) {
					trxId = TrxDAO.getTrxId();
					//String trxId = "1";
					//logger.info("========== trxId null space : " + trxId);
				} else {
					//logger.info("========== ========== ========== ========== ========== url pay/auth - req_chk.pay NOT NULL trxId : " + req_chk.pay.trxId);
					trxId = req_chk.pay.trxId;
				}
			} else {
				trxId = TrxDAO.getTrxId();
				//String trxId = "1";
				//logger.info("========== trxId null space : " + trxId);
			}
		} else {
			trxId = TrxDAO.getTrxId();
			//String trxId = "1";
			//logger.info("========== trxId null space : " + trxId);
		}
		logger.info("========== apiHandler() ========== trixId : " + trxId);
		// 다날 인증결제
		if (uri.startsWith("/api/pay/danal/auth")) {
			Danal.auth(rc, uri, trxId);
			return;
		} else if (uri.startsWith("/api/pay/danal/return")) {
			Danal.authReturn(rc, uri, trxId);
			//Danal.authReturn(rc, uri);
			return;
		}
		
		try {
			// 기본 접속 정보 저장
			sharedMap.put(PAYUNIT.TRX_ID, trxId);
			sharedMap.put(PAYUNIT.URI, uri); // URI
			sharedMap.put(PAYUNIT.METHOD, VertXUtil.getMethod(rc).toString()); // POST,GET,PUT
			sharedMap.put(PAYUNIT.REMOTEIP, VertXUtil.getXRealIp(rc)); // 접속 IP
			sharedMap.put(PAYUNIT.CONTENTTYPE, VertXUtil.getContentType(rc)); // CONTENTS TYPE
			sharedMap.put(PAYUNIT.PAYLOAD, VertXUtil.getBodyAsString(rc).trim()); // POST, PUT DATA Part
			sharedMap.put(PAYUNIT.USERAGENT, VertXUtil.getUserAgent(rc)); // USER AGENT
			sharedMap.put(PAYUNIT.ACCEPTLANGUAGE, VertXUtil.getAcceptLanguage(rc)); // ACCEPT LANGUAGE
			sharedMap.put(PAYUNIT.RESPONSE_TYPE, PAYUNIT.RESPONSE_DEFAULT); // 기본 응답 유형 설정
			sharedMap.put(PAYUNIT.HOST, VertXUtil.getSchemeHost(rc)); // HOST정보
			sharedMap.put(PAYUNIT.REG_DATE, CommonUtil.getCurrentDate("yyyyMMddHHmmss")); // 시스템 시간

			if (sharedMap.like(PAYUNIT.HOST, "api")) {
				sharedMap.put(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE); // LIVE 환경
			} else {
				sharedMap.put(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_DEMO); // DEMO 환경
			}

			// 인증에 대한 필터링 및 URI 및 METHOD 필터링 // 실패시 바로 응답 후 종료 처리
			//if (!authorization(rc) || !uriMethodFilter(rc)) {
			if (!authorization(rc, request, sharedMap, sharedObject) || !uriMethodFilter(rc, sharedMap)) {
				logger.info("========== apiHandler() - ========== trixId : " + trxId + " -  authorization FALSE ERROR : {},{},{}", trxId, rc.response().getStatusCode(), rc.response().getStatusMessage());
				return;
			}
		} catch (Exception e) {
			logger.info("========== apiHandler() - ERROR : {},{}", trxId, CommonUtil.getExceptionMessage(e));
			VertXMessage.set500(rc);
			return;
		}
		
		//daou get방식 설정 > 추후 vertx처리
		if (uri.indexOf("api/webhooks/daou") > -1) {
			sharedMap.put(PAYUNIT.PAYLOAD, uri.substring(uri.indexOf("?") + 1));
			logger.debug("/api/webhooks/daou params : {}", sharedMap.get(PAYUNIT.PAYLOAD));
		}
		// PAYMENT 로 통신 처리
		try {
			logger.info("========== apiHandler() - trxId : " + sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - Proc process SET");
			Proc process = null;
			if (uri.startsWith(PAYUNIT.API_PAY)) {
				process = new ProcPay();
			} else if (uri.startsWith(PAYUNIT.API_REFUND)) {
				process = new ProcRefund();
			} else if (uri.startsWith(PAYUNIT.API_ECHO)) {
				process = new ProcEcho();
			} else if (uri.startsWith(PAYUNIT.API_GET)) {
				process = new ProcGet();
			} else if (uri.startsWith(PAYUNIT.API_WIDGET)) {
				process = new ProcWidget();
			} else if (uri.startsWith(PAYUNIT.API_3D_WIDGET)) {
				process = new ProcPay3DWidget();
			} else if (uri.startsWith(PAYUNIT.API_3D_HOOK)) {
				process = new ProcPay3DHook();
			} else if (uri.equals(PAYUNIT.API_CASH) || uri.equals(PAYUNIT.API_CASH_CC)) {
				process = new ProcCashCoocon();
			} else if (uri.startsWith(PAYUNIT.API_VACT_GET)) {
				process = new VactGet();
			} else if (uri.startsWith(PAYUNIT.API_VACT_OPEN)) {
				process = new VactOpen();
			} else if (uri.startsWith(PAYUNIT.API_VACT_CLOSE)) {
				process = new VactClose();
			} else if (uri.startsWith(PAYUNIT.API_VACT_STATUS)) {
				process = new VactStatus();
			} else if (uri.startsWith(PAYUNIT.API_VACT_PATCH)) {
				process = new VactPatch();
			} else if (uri.startsWith(PAYUNIT.API_BILL_PAY)) {
				process = new ProcBillPay();
			} else if (uri.startsWith(PAYUNIT.API_BILL_USER)) {
				process = new ProcBillUser();
			} else if (uri.startsWith(PAYUNIT.API_AUTH)) {
				process = new ProcAuth();
			} else {
				if (uri.startsWith(PAYUNIT.API_WEBHOOK_DANAL)) {
					sharedMap.put("van", "DANAL");
					ProcWebHook webHook = new ProcWebHook();
					webHook.exec(rc, sharedMap);
				} else if (uri.startsWith(PAYUNIT.API_WEBHOOK_NICE)) {
					sharedMap.put("van", "NICE");
					ProcWebHookNice webHook = new ProcWebHookNice();
					webHook.exec(rc, sharedMap);
				} else if (uri.startsWith(PAYUNIT.API_WEBHOOK_DAOU)) {
					sharedMap.put("van", "DAOU");
					ProcWebHookDaou webHook = new ProcWebHookDaou();
					webHook.exec(rc, sharedMap);
				} else if (uri.startsWith(PAYUNIT.API_WEBHOOK_PAIRING)) {
					sharedMap.put("van", "UNKNOWN");
					ProcWebHookPairing webHook = new ProcWebHookPairing();
					webHook.exec(rc, sharedMap);
				} else if (uri.startsWith(PAYUNIT.API_WEBHOOK_ALLAT)) {
					sharedMap.put("van", "ALLAT");
					ProcWebHookAllat webHook = new ProcWebHookAllat();
					webHook.exec(rc, sharedMap);
				} else if (uri.startsWith(PAYUNIT.API_BILL_WIDGET)) {
					ProcBillWidget widget = new ProcBillWidget();
					widget.exec(rc, sharedMap);
				} else if (uri.startsWith(PAYUNIT.API_WEBHOOK_SMARTRO)) {
					ProcWebHookSmartro webHook = new ProcWebHookSmartro();
					webHook.exec(rc, sharedMap);
				} else if (uri.startsWith(PAYUNIT.API_WEBHOOK_SPCN)) {
					ProcWebHookSpcn webHook = new ProcWebHookSpcn();
					webHook.exec(rc, sharedMap);
				} else {
					logger.info("========== apiHandler() - PROCESS NOT FOUND : {}", CommonUtil.toString(sharedMap.get(PAYUNIT.URI)));
					VertXMessage.set404(rc);
				}
			}

			if (process != null) {
				process.exec(rc, request, sharedMap, sharedObject);
			}
		} catch (Exception e) {
			logger.info("========== apiHandler() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - Exception : [{}], {}", trxId, CommonUtil.getExceptionMessage(e));
			Api.sendMsg2(rc, sharedMap.get("vanResultCd"), "Error", sharedMap.get("vanResultMsg"));
			//Api.sendMsg2(rc, sharedMap.get("code"), sharedMap.get("status"), sharedMap.get("msg"));
		}
		logger.info("========== ========== ========== ========== ========== apiHandler END");
	}

	//private synchronized boolean authorization(RoutingContext rc) {
	private synchronized boolean authorization(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) {
		logger.info("========== ========== ========== ========== ========== authorization() - BEGIN");
		logger.info("========== authorization() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID));
		// DB,REDIS 살아있는지 체크
		DAO dao = new DAO();
		if (dao.dbPing() == false) {
			logger.debug("========== authorization ERROR : DB CONNECTION ERROR");
			VertXMessage.set500(rc);
			return false;
		}

		// POST 또는 PUT
		if (VertXUtil.isMethod(rc, HttpMethod.POST) || VertXUtil.isMethod(rc, HttpMethod.PUT)) {
			try {
				//syntax();
				syntax(request, sharedMap);
			} catch (Exception e) {
				VertXMessage.set400(rc);
				return false;
			}
		}

		// 인증과 관계 없이 수신할 수 있는 경우
		if (StringValidator.isInclude(sharedMap.getString(PAYUNIT.URI), PAYUNIT.IGNORE_AUTHRORISATION)) {
			return true;
		}

		// JSON,XML 만 수신처리
		String contentsType = sharedMap.getString(PAYUNIT.CONTENTTYPE).toLowerCase();
		if (contentsType.startsWith("application/json") || contentsType.equalsIgnoreCase(VertXMessage.CONTENT_XML)) {
			
		} else {
			logger.debug("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== authorization() - invalid content-type : " + contentsType);
			if (sharedMap.getString(PAYUNIT.URI).indexOf("widget") > -1) {
				
			} else {
				VertXMessage.set400(rc);
				return false;
			}
		}
		
		// Authorization
		SharedMap<String, Object> mchtTmn = null;
		String authorization = VertXUtil.getHeader(rc, HttpHeaders.AUTHORIZATION);
		////2018.03.16 WIGET GET  요청은 KEY 로 정보를 취득한다.
		if (authorization.equals("") && (sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_WIDGET) || sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_3D_WIDGET))) {
			String widgetType = sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_WIDGET) ? PAYUNIT.API_WIDGET : PAYUNIT.API_3D_WIDGET; 
			String key = sharedMap.getString(PAYUNIT.URI).replaceAll(widgetType+"/", "");
			SharedMap<String,Object> tempMap = PAYUNIT.cacheMap.get(key);
			if (tempMap != null) {
				authorization = tempMap.getString("authorization");
			}
		}
		if (CommonUtil.isNullOrSpace(authorization)) {
			logger.info("========== authorization() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - CommonUtil.isNullOrSpace - tmnId");
			String tmnId = "";
			if (request != null) { // 추가
				if (request.pay != null) {
					tmnId = request.pay.tmnId;
				} else if (request.refund != null) {
					tmnId = request.refund.tmnId;
				} else {
				}
				mchtTmn = trxDAO.getMchtTmnByTmnId(tmnId);
			}
			if (mchtTmn != null) {
//				logger.debug("========== authorization() - authorized by getMchtTmnByTmnId() : " + tmnId);
				logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== authorization() - authorized by getMchtTmnByTmnId() : " + tmnId + " payKey : " + mchtTmn.getString("payKey"));
				authorization = mchtTmn.getString("payKey");
			}
		}
		// 2021-12-28 가맹점명으로 결제키 조회
		if (CommonUtil.isNullOrSpace(authorization)) {
	//		logger.debug("========== authorization() - CommonUtil.isNullOrSpace - mchtId");
			logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== authorization() - CommonUtil.isNullOrSpace - authorization : " + authorization);
			String mchtId = "";
			if (request != null) { // 추가
				if (request.pay != null) {
					mchtId = request.pay.mchtId;
				} else if (request.refund != null) {
					mchtId = request.refund.tmnId;
				} else {
				}
				mchtTmn = trxDAO.getMchtTmnByMchtId(mchtId);
			}
			if (mchtTmn != null) {
				//logger.debug("========== authorization() - authorized by getMchtTmnByMchtId() mchtId : {}", mchtId);
				logger.info("========== authorization() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - getMchtTmnByMchtId() : " + mchtId + " payKey : " + mchtTmn.getString("payKey"));
				authorization = mchtTmn.getString("payKey");
			}
		}
		// Authorization Header 값 비교
		if (authorization.startsWith(PAYUNIT.KEYINITIAL)) {
			mchtTmn = trxDAO.getMchtTmnByPayKey(authorization);
			if (mchtTmn == null) {
				logger.info("========== authorization() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - getMchtTmnByMchtId() : payKey NULL");
				VertXMessage.set401(rc);
				return false;
			}
		} else {
			//logger.debug("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== authorization() - Unauthorized {} : [{}] , invalid authorization key ", authorization);
			logger.info("========== authorization() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - payKey NULL");
			VertXMessage.set401(rc);
			return false;
		}
		// 단말기 등록 정보 비교
		if (mchtTmn != null) {
			if (mchtTmn.containsKey("mchtId") && mchtTmn.isEquals("status", "사용")) {
				if (mchtTmn.getLong("activeDate") > CommonUtil.parseLong(CommonUtil.getCurrentDate("yyyyMMdd"))) {
					//logger.debug("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== Unauthorized : [{}] , activeDate: [{}]", authorization, mchtTmn.getString("activeDate"));
					logger.info("========== authorization() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - mchtTmn activeDate : " + mchtTmn.getString("activeDate") + " > today");
					VertXMessage.set401(rc);
					return false;
				}
			} else {
				//logger.debug("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + "========== authorization() - Unauthorized : [{}] , status : [{}]", authorization, mchtTmn.getString("status"));
				logger.info("========== authorization() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - mchtTmn status : " + mchtTmn.getString("status"));
				VertXMessage.set401(rc);
				return false;
			}
		}

		SharedMap<String, Object> mcht = trxDAO.getMchtByMchtId(mchtTmn.getString("mchtId"));
		SharedMap<String, Object> mchtMng = trxDAO.getMchtMngByMchtId(mchtTmn.getString("mchtId"));

		if (mcht == null) {
			logger.info("========== authorization() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - mcht NULL");
			VertXMessage.set401(rc);
			return false;
		} else if (mchtMng == null) {
			if (mcht == null || mchtMng == null) {
				logger.info("========== authorization() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - mchtMng NULL");
				VertXMessage.set401(rc);
				return false;
		} else if (mcht.isEquals("status", "예비") || mcht.isEquals("status", "폐기")) {
			logger.debug("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== authorization() - Unauthorized : [{}] , mcht status: [{}]", authorization, mcht.getString("status"));
			logger.info("========== authorization() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - mcht status : " + mcht.getString("status"));
			VertXMessage.set401(rc);
			return false;
		}
			/*
			if (mchtMng.isEquals("payStatus", "중지")) {
				logger.debug("Unauthorized : [{}] , mchtMng payStatus: [{}]", authorization, mchtMng.getString("payStatus"));
				VertXMessage.set401(rc);
				return false;
			}*/
		}
		sharedMap.put(PAYUNIT.MCHTID, mchtTmn.getString("mchtId"));
		sharedMap.put("tmnId", mchtTmn.getString("tmnId"));
		sharedObject.put("mcht", mcht);
		sharedObject.put("mchtTmn", mchtTmn);
		sharedObject.put("mchtMng", mchtMng);
		logger.info("sharedObject - mcht : " + mcht.size());
		logger.info("sharedObject - mchtTmn : " + mchtTmn.size());
		logger.info("sharedObject - mchtMng : " + mchtMng.size());
		
		// 2021-11-23 VAN사 정보
		SharedMap<String, Object> mchtVan = trxDAO.getMchtTmnByVanIdx2(mchtTmn.getInt("vanIdx"), mchtTmn.getString("mchtId"));
		sharedObject.put("mchtVan", mchtVan);
		// 2022-08-25 PG_VAN 정보
		SharedMap<String, Object> mchtVanId = trxDAO.getVanByVanIdx2(mchtTmn.getString("vanIdx"));
		sharedObject.put("mchtVanId", mchtVanId);
		
		logger.info("========== ========== ========== ========== ========== authorization() - END");
		return true;
	}

	private boolean uriMethodFilter(RoutingContext rc, SharedMap<String, Object> sharedMap) {

		if ("POST,GET,PUT".indexOf(sharedMap.getString(PAYUNIT.METHOD)) < 0) {
			logger.debug("Method Not Allowed : {}", VertXUtil.getMethod(rc));
			VertXMessage.set405(rc);
			return false;
		}

		return true;
	}

	/**
	 * payLoad 의 데이터 파싱 처리 및 syntax error 확인
	 * 
	 * @param payment
	 * @return
	 * @throws Exception
	 */
	//private void syntax() throws Exception {
	private void syntax(Request request, SharedMap<String, Object> sharedMap) throws Exception {
		logger.info("========== ========== ========== ==========  ========== syntax() - BEGIN");
		logger.info("========== syntax() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID));
		
		String contentsType = sharedMap.getString(PAYUNIT.CONTENTTYPE);
		String requestBody = sharedMap.getString(PAYUNIT.PAYLOAD);
	//	logger.info("========== syntax() - contentsType : " + contentsType);
	//	logger.info("========== syntax() - requestBody : " + requestBody);
		
		if (CommonUtil.isNullOrSpace(requestBody)) {
			logger.info("========== syntax() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - CommonUtil.isNullOrSpace - requestBody NULL");
			return;
		}

		try {
			// WEBHOOK CONTENT TYPE 정해지면 수정.
			if (contentsType.toLowerCase().startsWith(VertXMessage.CONTENT_JSON)) {
				request = (Request) GsonUtil.fromJson(requestBody, Request.class);
				sharedMap.put(PAYUNIT.PAYLOAD, GsonUtil.toJson(request));
			} else if (contentsType.toLowerCase().startsWith(VertXMessage.CONTENT_XML)) {
				request = (Request) XmlUtil.fromXml(new Request(), requestBody);
				sharedMap.put(PAYUNIT.PAYLOAD, GsonUtil.toJson(request));
			} else {
				sharedMap.put(PAYUNIT.PAYLOAD, requestBody);
			}
		} catch (Exception e) {
			logger.info("========== syntax() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID) + " - Exception : " + CommonUtil.getExceptionMessage(e));
			throw new Exception("syntax error : {}" + e.getMessage());
		} finally {
			//logger.info("========== sharedMap PAYUNIT.TRX_ID : " + sharedMap.get(PAYUNIT.TRX_ID) + " ========== syntax() - request : trxId: {}", sharedMap.get(PAYUNIT.TRX_ID));
		}
		logger.info("========== ========== ========== ==========  ========== syntax() - END");
		return;
	}

	public static void sendMsg(RoutingContext rc, Object object, Object object2, Object object3) {
		logger.info("========== ========== ========== ========== ========== sendMsg()");
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put("code", object);
		responseMap.put("statusMessage", object2);
		responseMap.put("message", object3);

		try {
			VertXMessage.set200(rc, "application/json", GsonUtil.toJson(responseMap), "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void sendMsg2(RoutingContext rc, Object object, Object object2, Object object3) {
		logger.info("========== ========== ========== ========== ========== sendMsg2()");
	//	logger.info("========== ========== ========== ========== code : \t" + object);
	//	logger.info("========== ========== ========== ========== statusMessage : \t" + object2);
	//	logger.info("========== ========== ========== ========== message : \t" + object3);
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put("code", object);
		responseMap.put("status", object2);
		responseMap.put("msg", object3);
		
		try {
			VertXMessage.set200(rc, "application/json", GsonUtil.toJson(responseMap), "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void sendMsgCallBack(RoutingContext rc, String code, String statusMessage, String message, String EncData) {
		logger.info("========== ========== ========== ========== ========== sendMsgCallBack()");
	//	logger.info("========== ========== ========== ========== code : \t" + code);
	//	logger.info("========== ========== ========== ========== statusMessage : \t" + statusMessage);
	//	logger.info("========== ========== ========== ========== message : \t" + message);
	//	logger.info("========== ========== ========== ========== EncData : \t" + EncData);
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put("code", code);
		responseMap.put("statusMessage", statusMessage);
		responseMap.put("message", message);
		responseMap.put("EncData", EncData);
		
		try {
			VertXMessage.set200(rc, "application/json", GsonUtil.toJson(responseMap), "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void sendMsgCallBack2(RoutingContext rc, String code, String statusMessage, String message, String EncData) {
		logger.info("========== ========== ========== ========== ========== sendMsgCallBack2()");
		//logger.info("========== ========== ========== ========== code : \t" + code);
		//logger.info("========== ========== ========== ========== statusMessage : \t" + statusMessage);
		//logger.info("========== ========== ========== ========== message : \t" + message);
		//logger.info("========== ========== ========== ========== EncData : \t" + EncData);
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put("code", code);
		responseMap.put("status", statusMessage);
		responseMap.put("msg", message);
		responseMap.put("EncData", EncData);
		
		try {
			VertXMessage.set200(rc, "application/json", GsonUtil.toJson(responseMap), "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void sendMsgPay(RoutingContext rc, String code, String statusMessage, String message, String trxId) {
		logger.info("========== ========== ========== ========== ========== sendMsgPay()");
	//	logger.info("========== ========== ========== ========== code : \t" + code);
	//	logger.info("========== ========== ========== ========== statusMessage : \t" + statusMessage);
	//	logger.info("========== ========== ========== ========== message : \t" + message);
	//	logger.info("========== ========== ========== ========== trxId : \t" + trxId);
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put("code", code);
		responseMap.put("statusMessage", statusMessage);
		responseMap.put("message", message);
		responseMap.put("trxId", trxId);
		
		try {
			VertXMessage.set200(rc, "application/json", GsonUtil.toJson(responseMap), "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void sendMsgPay2(RoutingContext rc, String code, String statusMessage, String message, String trxId) {
		logger.info("========== ========== ========== ========== ========== sendMsgPay2()");
	//	logger.info("========== ========== ========== ========== code : \t" + code);
	//	logger.info("========== ========== ========== ========== statusMessage : \t" + statusMessage);
	//	logger.info("========== ========== ========== ========== message : \t" + message);
	//	logger.info("========== ========== ========== ========== trxId : \t" + trxId);
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put("code", code);
		responseMap.put("status", statusMessage);
		responseMap.put("msg", message);
		responseMap.put("trxId", trxId);
		
		try {
			VertXMessage.set200(rc, "application/json", GsonUtil.toJson(responseMap), "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void sendMsgPayStr(RoutingContext rc, String code, String statusMessage, String message, String data) {
		logger.info("========== ========== ========== ========== ========== sendMsgPayStr()");
	//	logger.info("========== ========== ========== ========== code : \t" + code);
	//	logger.info("========== ========== ========== ========== statusMessage : \t" + statusMessage);
	//	logger.info("========== ========== ========== ========== message : \t" + message);
	//	logger.info("========== ========== ========== ========== data : \t" + data);
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put("code", code);
		responseMap.put("status", statusMessage);
		responseMap.put("msg", message);
		responseMap.put("data", data);
		
		try {
			VertXMessage.set200(rc, "application/json", GsonUtil.toJson(responseMap), "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void sendMsgPayMap(RoutingContext rc, String code, String statusMessage, String message, JSONObject data) {
		logger.info("========== ========== ========== ========== ========== sendMsgPayMap()");
		//logger.info("========== ========== ========== ========== code : \t" + code);
		//logger.info("========== ========== ========== ========== statusMessage : \t" + statusMessage);
		//logger.info("========== ========== ========== ========== message : \t" + message);
		//logger.info("========== ========== ========== ========== data : \t" + data);
		LinkedHashMap<String, Object> responseMap = new LinkedHashMap<String, Object>();
		responseMap.put("code", code);
		responseMap.put("status", statusMessage);
		responseMap.put("msg", message);
		responseMap.put("data", data);
		
		try {
			VertXMessage.set200(rc, "application/json", GsonUtil.toJson(responseMap), "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
