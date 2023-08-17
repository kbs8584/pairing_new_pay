package com.pgmate.pay.proc;


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public abstract class Proc {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.proc.Proc.class );
	private long startTime	= System.nanoTime();
	
	protected SharedMap<String,Object> sharedMap 		= null;
	protected TrxDAO trxDAO									= null;
	protected SharedMap<String,Object> mchtTmnMap 	= null;
	protected SharedMap<String,Object> mchtMap 		= null;
	protected SharedMap<String,Object> mchtMngMap 	= null;
	
	protected Response response								= null;
	protected Request request									= null;
	protected RoutingContext rc									= null;
	
	// 2022-05-16 즉시취소를 위한 결제데이터 관리
	public static HashMap<String, Object> trxIdPayMap = new HashMap<String, Object>();
	
	public abstract void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject);
	public abstract void valid();
	
	public abstract void valid2(Request request, SharedMap<String, Object> sharedMap, SharedMap<String, Object> sales_mchtMap, SharedMap<String, Object> sales_mchtTmnMap, SharedMap<String, Object> sales_mchtMngMap);
	
	protected synchronized void set(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) {
		logger.info("========== ========== ========== ========== ========== set() - BEGIN ");
		this.rc						= rc;
		this.request				= request;
		this.sharedMap         = sharedMap;
		this.mchtTmnMap		= sharedObject.get("mchtTmn");
		this.mchtMap			= sharedObject.get("mcht");
		this.mchtMngMap		= sharedObject.get("mchtMng");
		this.response			= new Response();
		this.trxDAO				= new TrxDAO();
		logger.info("========== set() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID));
		logger.info("========== set() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtMap mchtId : " + mchtMap.getString("mchtId"));
		if (mchtTmnMap.get("van").equals("SMARTRO")) {
			//logger.info("========== set() - van : SMARTRO");
			sharedMap.put("van", "SMARTRO");
		} else {
			
		}
		
		BigDecimal rate = new BigDecimal(mchtMngMap.getString("rate"));
		BigDecimal data = new BigDecimal("0");
		rate.stripTrailingZeros().toPlainString();		
		int compareRate = rate.compareTo(data); 
		logger.info("");
		/*
		if (rate < 0.001) {
			logger.info("mcht mng not set rate : {}", mchtMngMap.getDouble("rate"));
			response.result = ResultUtil.getResult("9999", "설정오류", "가맹점 정산 정보 미설정 오류");
			return;
		}
		logger.info("========== set() - mcht rate : " + rate);
		*/
		
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
				logger.info("========== set() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | request is null , payLoad : {}",sharedMap.getString(PAYUNIT.PAYLOAD));
				response.result = ResultUtil.getResult("9999", "요청 정보 없음","요청 데이터가 없습니다.");
				return;
			}
		}
		
		if (!mchtTmnMap.isEquals("status", "사용")) {
			logger.info("========== set() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtTmnMap status : " + mchtTmnMap.getString("status"));
			if (!sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_REFUND)) {
				response.result = ResultUtil.getResult("9999","설정오류","사용가능한 터미널이 아닙니다.");
			}
		}
		
		if (mchtTmnMap.getLong("vanIdx") == 0) {
			logger.info("========== set() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtTmnMap vanIdx : " + mchtTmnMap.getLong("vanIdx"));
			response.result = ResultUtil.getResult("9999","설정오류","라우팅을 찾을 수 없습니다.");
		}

		valid();
		logger.info("========== ========== ========== ========== ========== set() - END ");
	}
	
	protected synchronized void setResponse() {
		logger.info("========== ========== ========== ========== ========== setResponse() - BEGIN");
		String res = GsonUtil.toJsonExcludeStrategies(response,true);
		if (sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_PAY)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_OPEN)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_CLOSE)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_PATCH)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_AUTH)) {
			logger.info("========== setResponse() - trxDAO.updateTrxIO");
			trxDAO.updateTrxIO(sharedMap, res);
			logger.info("========== setResponse() - trxDAO.updateTrxReq");
			trxDAO.updateTrxReq(sharedMap);
			logger.info("========== setResponse() - trxDAO.updateTrxPAY");
			trxDAO.updateTrxPay(sharedMap);
			
		}
		
		if (sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_PAY)) {
			if (response.result.resultCd.equals("0000")) {
				// 2022-05-16 즉시취소를 위한 결제데이터 관리 - 결제번호, 현재시간
				String trxPay_trxId = sharedMap.getString("trxId");
			//	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			//	Date now = new Date();
			//	String nowTime = sdf.format(now);
				int trxIdCnt = 5;
				System.out.println();
				System.out.println("========== 즉쉬취소 결제데이터 관리 업데이트");
				System.out.println("========== 결제번호 trxPay_trxId : " + trxPay_trxId);
				//System.out.println("========== 저장시간 nowTime : " + nowTime);
				//trxIdPayMap.put(trxPay_trxId, nowTime);
				System.out.println("========== 저장카운트 trxIdCnt : " + trxIdCnt);
				trxIdPayMap.put(trxPay_trxId, trxIdCnt);
				System.out.println("========== 즉쉬취소 결제데이터 : " + trxIdPayMap.toString());
				System.out.println();
			}
		}
		
		/*
		if(!sharedMap.isEquals(PAYUNIT.ACCEPTLANGUAGE, "ko")){
			logger.info("convert language : {} ", sharedMap.getString(PAYUNIT.ACCEPTLANGUAGE));
			response.result = ResultMap.convert(response.result, sharedMap.getString(PAYUNIT.ACCEPTLANGUAGE));
			res = GsonUtil.toJsonExcludeStrategies(response,true);
		}
		*/
		
		VertXMessage.set200(rc, res);
		logger.info("estimatedTime : {}",TimeUnit.MILLISECONDS.convert(System.nanoTime()- startTime, TimeUnit.NANOSECONDS));
	//	sharedMap 		= null;	
		mchtTmnMap	= null;
		mchtMap			= null;
		mchtMngMap	= null;
		trxDAO 			= null;
		response			= null;
		request			= null;
		
		logger.info("========== ========== ========== ========== ========== setResponse() - END");
	}
	
	public Proc() {

	}

	protected synchronized void set2(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) {
		logger.info("========== ========== ========== ========== ========== set2() - BEGIN ");
		this.rc						= rc;
		this.request				= request;
		this.sharedMap         = sharedMap;
		this.mchtTmnMap		= sharedObject.get("mchtTmn");
		this.mchtMap			= sharedObject.get("mcht");
		this.mchtMngMap		= sharedObject.get("mchtMng");
		this.response			= new Response();
		this.trxDAO				= new TrxDAO();
		logger.info("========== set2() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtId : " + sharedMap.getString(PAYUNIT.MCHTID));
		logger.info("========== set2() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtMap mchtId : " + mchtMap.getString("mchtId"));
		if (mchtTmnMap.get("van").equals("SMARTRO")) {
			//logger.info("========== set() - van : SMARTRO");
			sharedMap.put("van", "SMARTRO");
		} else {
			
		}
		BigDecimal rate = new BigDecimal(mchtMngMap.getString("rate"));
		BigDecimal data = new BigDecimal("0");
		rate.stripTrailingZeros().toPlainString();		
		int compareRate = rate.compareTo(data); 
		/*
		if (rate < 0.001) {
			logger.info("mcht mng not set rate : {}", mchtMngMap.getDouble("rate"));
			response.result = ResultUtil.getResult("9999", "설정오류", "가맹점 정산 정보 미설정 오류");
			return;
		}
		logger.info("========== set() - mcht rate : " + rate);
		*/
		
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
				logger.info("========== set2() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | request is null , payLoad : {}",sharedMap.getString(PAYUNIT.PAYLOAD));
				response.result = ResultUtil.getResult("9999", "요청 정보 없음","요청 데이터가 없습니다.");
				return;
			}
		}
		
		if (!mchtTmnMap.isEquals("status", "사용")) {
			logger.info("========== set2() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtTmnMap status : " + mchtTmnMap.getString("status"));
			if (!sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_REFUND)) {
				response.result = ResultUtil.getResult("9999","설정오류","사용가능한 터미널이 아닙니다.");
			}
		}
		
		if (mchtTmnMap.getLong("vanIdx") == 0) {
			logger.info("========== set2() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID) + " | mchtTmnMap vanIdx : " + mchtTmnMap.getLong("vanIdx"));
			response.result = ResultUtil.getResult("9999","설정오류","라우팅을 찾을 수 없습니다.");
		}
		valid();
		logger.info("========== ========== ========== ========== ========== set2() - END ");
	}
	
	protected synchronized void setResponse2(RoutingContext rc, SharedMap sharedMap) {
		logger.info("========== ========== ========== ========== ========== setResponse2() - BEGIN");
		logger.info("========== setResponse2() - trxId : "+ sharedMap.getString(PAYUNIT.TRX_ID));
		String res = GsonUtil.toJsonExcludeStrategies(response,true);
		if (sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_PAY)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_OPEN)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_CLOSE)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_PATCH)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_AUTH)) {
			logger.info("========== setResponse2() - trxId : " + sharedMap.getString(PAYUNIT.TRX_ID) + " - updateTrxIO() ");
			trxDAO.updateTrxIO(sharedMap, res);
			logger.info("========== setResponse2() - trxId : " + sharedMap.getString(PAYUNIT.TRX_ID) + " - updateTrxReq() ");
			trxDAO.updateTrxReq(sharedMap);
			logger.info("========== setResponse2() - trxId : " + sharedMap.getString(PAYUNIT.TRX_ID) + " - updateTrxPAY() ");
			trxDAO.updateTrxPay(sharedMap);
			
		}
		
		if (sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_PAY)) {
			if (response.result.resultCd.equals("0000")) {
				// 2022-05-16 즉시취소를 위한 결제데이터 관리 - 결제번호, 현재시간
				String trxPay_trxId = sharedMap.getString("trxId");
			//	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			//	Date now = new Date();
			//	String nowTime = sdf.format(now);
				int trxIdCnt = 5;
				System.out.println("========== 즉쉬취소 결제데이터 관리 업데이트");
				System.out.println("========== 결제번호 trxPay_trxId : " + trxPay_trxId);
				//System.out.println("========== 저장시간 nowTime : " + nowTime);
				//trxIdPayMap.put(trxPay_trxId, nowTime);
				System.out.println("========== 저장카운트 trxIdCnt : " + trxIdCnt);
				trxIdPayMap.put(trxPay_trxId, trxIdCnt);
				System.out.println("========== 즉쉬취소 결제데이터 : " + trxIdPayMap.toString());
			}
		}
		
		/*
		if(!sharedMap.isEquals(PAYUNIT.ACCEPTLANGUAGE, "ko")){
			logger.info("convert language : {} ", sharedMap.getString(PAYUNIT.ACCEPTLANGUAGE));
			response.result = ResultMap.convert(response.result, sharedMap.getString(PAYUNIT.ACCEPTLANGUAGE));
			res = GsonUtil.toJsonExcludeStrategies(response,true);
		}
		*/
		
		VertXMessage.set200(rc, res);
		logger.info("estimatedTime : {}",TimeUnit.MILLISECONDS.convert(System.nanoTime()- startTime, TimeUnit.NANOSECONDS));
	//	sharedMap 		= null;	
	//	mchtTmnMap	= null;
	//	mchtMap			= null;
	//	mchtMngMap	= null;
	//	trxDAO 			= null;
	//	response			= null;
	//	request			= null;
		
		logger.info("========== ========== ========== ========== ========== setResponse2() - END");
	}
}
