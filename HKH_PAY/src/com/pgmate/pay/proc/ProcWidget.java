package com.pgmate.pay.proc;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcWidget extends Proc {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.proc.ProcWidget.class);

	public ProcWidget() {

	}

	@Override
	public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) {
		//set2(rc, request, sharedMap, sharedObject);
		//setResponse();
		
		// 2022-08-25 - set 로직 추가 - BEGIN
		logger.info("========== ========== ========== ========== ========== exec() - set2() - BEGIN ");
		SharedMap<String, Object> sales_mchtMap = sharedObject.get("mcht");
		SharedMap<String, Object> sales_mchtTmnMap = sharedObject.get("mchtTmn");
		SharedMap<String, Object> sales_mchtMngMap = sharedObject.get("mchtMng");
		
		//logger.info("sharedObject - sales_mchtMap : " + sales_mchtMap.toJson());
		//logger.info("sharedObject - sales_mchtTmnMap : " + sales_mchtTmnMap.toJson());
		//logger.info("sharedObject - sales_mchtMngMap : " + sales_mchtMngMap.toJson());
		this.response = new Response();
		this.trxDAO	= new TrxDAO();
		logger.info("========== exec() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID));
		logger.info("========== exec() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtMap mchtId : " + sales_mchtMap.getString("mchtId"));
		if (sales_mchtTmnMap.get("van").equals("SMARTRO")) {
			sharedMap.put("van", "SMARTRO");
		}
		BigDecimal rate = new BigDecimal(sales_mchtMngMap.getString("rate"));
		BigDecimal data = new BigDecimal("0");
		rate.stripTrailingZeros().toPlainString();		
		int compareRate = rate.compareTo(data); 
		if (compareRate < 0) {
			response.result = ResultUtil.getResult("9999", "설정오류", "가맹점 정산 정보 미설정 오류");
			return;
		} else if (compareRate == 0) {
		} else if (compareRate > 0) {
		}
		//2018.03.16 WIDGET 반영과 함께 적용 
		if (sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_GET) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_WIDGET )
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_ECHO ) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_3D_WIDGET )
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_CLOSE) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_STATUS)
				|| sharedMap.getString(PAYUNIT.URI).startsWith("/api/comp/pay")){			
		} else {
			if (request == null) {
				logger.info("========== exec() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | request is null , payLoad : {}",sharedMap.getString(PAYUNIT.PAYLOAD));
				response.result = ResultUtil.getResult("9999", "요청 정보 없음","요청 데이터가 없습니다.");
				return;
			}
		}
		if (!sales_mchtTmnMap.isEquals("status", "사용")) {
			logger.info("========== exec() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtTmnMap status : " + sales_mchtTmnMap.getString("status"));
			if (!sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_REFUND)) {
				response.result = ResultUtil.getResult("9999","설정오류","사용가능한 터미널이 아닙니다.");
			}
		}
		
		if (sales_mchtTmnMap.getLong("vanIdx") == 0) {
			logger.info("========== exec() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtTmnMap vanIdx : " + sales_mchtTmnMap.getLong("vanIdx"));
			response.result = ResultUtil.getResult("9999","설정오류","라우팅을 찾을 수 없습니다.");
		}
		valid2(request, sharedMap, sales_mchtMap, sales_mchtTmnMap, sales_mchtMngMap);
		logger.info("========== ========== ========== ========== ========== exec() - set2() - END ");
		// 2022-08-25 - set 로직 추가 - END
		setResponse2(rc, sharedMap);
		return;
	}

	@Override
	public void valid() {

		if (sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("GET")) {
			String key = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_WIDGET + "/", "");
			logger.info("========== valid() - call key : {}", key);
			if (!key.startsWith("key_")) {
				response.result = ResultUtil.getResult("9999", "요청 정보 없음", "Widget 정보 요청 실패 Invalid Key");
				return;
			} else {
				if (PAYUNIT.cacheMap.containsKey(key)) {
					response.result = ResultUtil.getResult("0000", "정상", "정상완료");
					response.widget = PAYUNIT.cacheMap.get(key);
					return;
				} else {
					response.result = ResultUtil.getResult("9999", "Expired 된 Key 입니다.");
					return;
				}
			}

		} else if (sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("POST")) {
			if (request.widget == null) {
				response.result = ResultUtil.getResult("9999", "요청 정보 없음", "요청 데이터가 없습니다.Widget  오류");
				return;
			} else {
				if (mchtTmnMap.isEquals("webPay", "사용")) {

					String widgetKey = "key_" + CommonUtil.toString(System.currentTimeMillis()) + UUID.randomUUID().toString().substring(0, 7);
					response.widget = new SharedMap<String, Object>();
					response.widget.put("key", widgetKey);
					// response.widget.put("apiMaxInstall", mchtTmnMap.getInt("apiMaxInstall") );
					response.widget.put("apiMaxInstall", mchtMngMap.getInt("maxInstall"));
					response.widget.put("nick", mchtMap.getString("nick"));
					response.widget.put("semiAuth", mchtTmnMap.getString("semiAuth"));
					response.widget.put("tmnId", mchtTmnMap.getString("tmnId"));
					response.widget.put("target", "PAIRINGSOLUTION");
					response.widget.put("routeUrl", "/form/payment/pairingsolution/index.html?token=" + widgetKey);
					// request.widget.put("apiMaxInstall",mchtTmnMap.getInt("apiMaxInstall") );
					
					request.widget.put("apiMaxInstall", mchtMngMap.getInt("maxInstall"));
					request.widget.put("nick", mchtMap.getString("nick"));
					request.widget.put("semiAuth", mchtTmnMap.getString("semiAuth"));
					request.widget.put("tmnId", mchtTmnMap.getString("tmnId"));
					request.widget.put("authorization", mchtTmnMap.getString("payKey"));

					logger.info("ProcWidget valid - save as key : {}", response.widget.getString("key"));
					PAYUNIT.cacheMap.put(response.widget.getString("key"), request.widget);

					response.result = ResultUtil.getResult("0000", "정상", "정상완료");
				} else {
					response.result = ResultUtil.getResult("9999", "호출실패", "온라인 결제폼을 사용하지 않는 가맹점입니다.관리자에 문의바랍니다.");
					return;
				}
			}
		} else {
			response.result = ResultUtil.getResult("9999", "호출실패", "Invalid request method");
			return;
		}
	}

	@Override
	public void valid2(Request request, SharedMap<String, Object> sharedMap, SharedMap<String, Object> sales_mchtMap, SharedMap<String, Object> sales_mchtTmnMap, SharedMap<String, Object> sales_mchtMngMap) {
		
		if (sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("GET")) {
			String key = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_WIDGET + "/", "");
			logger.info("========== valid2() - call key : {}", key);
			if (!key.startsWith("key_")) {
				response.result = ResultUtil.getResult("9999", "요청 정보 없음", "Widget 정보 요청 실패 Invalid Key");
				return;
			} else {
				if (PAYUNIT.cacheMap.containsKey(key)) {
					response.result = ResultUtil.getResult("0000", "정상", "정상완료");
					response.widget = PAYUNIT.cacheMap.get(key);
					return;
				} else {
					response.result = ResultUtil.getResult("9999", "Expired 된 Key 입니다.");
					return;
				}
			}

		} else if (sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("POST")) {
			if (request.widget == null) {
				response.result = ResultUtil.getResult("9999", "요청 정보 없음", "요청 데이터가 없습니다.Widget  오류");
				return;
			} else {
				if (sales_mchtTmnMap.isEquals("webPay", "사용")) {

					String widgetKey = "key_" + CommonUtil.toString(System.currentTimeMillis()) + UUID.randomUUID().toString().substring(0, 7);
					response.widget = new SharedMap<String, Object>();
					response.widget.put("key", widgetKey);
					// response.widget.put("apiMaxInstall", sales_mchtTmnMap.getInt("apiMaxInstall") );
					response.widget.put("apiMaxInstall", sales_mchtMngMap.getInt("maxInstall"));
					response.widget.put("nick", sales_mchtMap.getString("nick"));
					response.widget.put("semiAuth", sales_mchtTmnMap.getString("semiAuth"));
					response.widget.put("tmnId", sales_mchtTmnMap.getString("tmnId"));
					response.widget.put("target", "PAIRINGSOLUTION");
					response.widget.put("routeUrl", "/form/payment/pairingsolution/index.html?token=" + widgetKey);
					// request.widget.put("apiMaxInstall",sales_mchtTmnMap.getInt("apiMaxInstall") );
					request.widget.put("apiMaxInstall", sales_mchtMngMap.getInt("maxInstall"));
					request.widget.put("nick", sales_mchtMap.getString("nick"));
					request.widget.put("semiAuth", sales_mchtTmnMap.getString("semiAuth"));
					request.widget.put("tmnId", sales_mchtTmnMap.getString("tmnId"));
					request.widget.put("authorization", sales_mchtTmnMap.getString("payKey"));

					logger.info("========== ProcWidget valid - save as key : {}", response.widget.getString("key"));
					logger.info("ProcWidget valid - save as key : {}", response.widget.getString("key"));
					PAYUNIT.cacheMap.put(response.widget.getString("key"), request.widget);

					response.result = ResultUtil.getResult("0000", "정상", "정상완료");
				} else {
					response.result = ResultUtil.getResult("9999", "호출실패", "온라인 결제폼을 사용하지 않는 가맹점입니다.관리자에 문의바랍니다.");
					return;
				}
			}
		} else {
			response.result = ResultUtil.getResult("9999", "호출실패", "Invalid request method");
			return;
		}
		
	}

}
