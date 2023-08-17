package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.van.CashClient;
import com.pgmate.pay.van.CashInter;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcCashCoocon extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcCashCoocon.class );
	
	

	public ProcCashCoocon() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		if(response.result != null){
			setResponse();
			return;
		}
	
		CashClient cash = new CashClient();
		CashInter inter = new CashInter();
		inter.trxId		= sharedMap.getString(PAYUNIT.TRX_ID);
		inter.trackId	= request.cashcc.trackId;
		inter.tmnId  	= request.cashcc.tmnId;
		inter.trxDate	= sharedMap.getString(PAYUNIT.REG_DATE);
		
		if(request.cashcc.trxType.equals("issue")) {
			inter.trxType  = "issue";
			inter.cashType = request.cashcc.cashType;
			inter.amount  = request.cashcc.amount;
			inter.service  = request.cashcc.service;
			inter.vat  = request.cashcc.vat;
			inter.supply  = request.cashcc.supply;
			inter.usage = request.cashcc.usage;
			inter.identity = request.cashcc.identity;
			
			inter = cash.comm(inter);
			logger.info("응답:{},{}",inter.resultCd,inter.resultMsg);
			logger.info("응답일자:{}",inter.trxDate);
			
			response.cashcc = request.cashcc;
			response.cashcc.identity 	= inter.identity;	//마스크된 값 전달
			response.cashcc.authCd	= inter.authCd;
			response.cashcc.trxDate = inter.trxDate;
			response.cashcc.trxId   = inter.trxId;
			
			response.result = ResultUtil.getResult(inter.resultCd, inter.resultMsg);
			response.result.create = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		}else{
			inter.trxType  = "revoke";
			inter.rootTrxId = request.cashcc.rootTrxId;
			inter.amount  = request.cashcc.amount;
			inter.service  = request.cashcc.service;
			inter.vat  = request.cashcc.vat;
			inter.supply  = request.cashcc.supply;
			inter = cash.comm(inter);
			logger.info("응답:{},{}",inter.resultCd,inter.resultMsg);
			logger.info("응답일자:{}",inter.trxDate);
			
			response.cashcc = request.cashcc;
			response.cashcc.identity 	= inter.identity;	//마스크된 값 전달
			response.cashcc.authCd	= inter.authCd;
			response.cashcc.trxDate = inter.trxDate;
			response.cashcc.trxId   = inter.trxId;
			
			response.result = ResultUtil.getResult(inter.resultCd, inter.resultMsg);
			response.result.create = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
			
					
		}
		
		
		setResponse();
		
		return;
	}


	@Override
	public void valid() {
		
		if(request.cashcc == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","현금영수증 발행정보가 없습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.cashcc.tmnId)){
			request.cashcc.tmnId = sharedMap.getString("tmnId");
		}
		
		if(CommonUtil.isNullOrSpace(request.cashcc.trackId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 주문번호가 입력되지 않았습니다.");return;
		}
		
		request.cashcc.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
		
		
		
		if(!(request.cashcc.trxType.equals("issue") || request.cashcc.trxType.equals("revoke"))){
			response.result = ResultUtil.getResult("9999", "필수값없음","승인거래,취소거래 정보값 오류");return;
		}
		
		if(request.cashcc.trxType.equals("revoke")){
			
			SharedMap<String,Object> cashMap = null;
			
			if(!CommonUtil.isNullOrSpace(request.cashcc.rootTrxId)) {
				cashMap = trxDAO.getTrxCash2(request.cashcc.rootTrxId);
			}else {
				if(CommonUtil.isNullOrSpace(request.cashcc.rootTrxDay) || CommonUtil.isNullOrSpace(request.cashcc.rootTrackId) || request.cashcc.amount ==0) {
					response.result = ResultUtil.getResult("9999", "취소불가","원거래일자,주문번호,금액 중 누락된 값이 있습니다.");return;
				}
				cashMap = trxDAO.getTrxCash2(mchtTmnMap.getString("tmnId"), request.cashcc.rootTrackId, request.cashcc.rootTrxDay);
				
			}
			
			if(cashMap == null || cashMap.isNullOrSpace("trxId")){
				response.result = ResultUtil.getResult("9999", "취소불가","원거래번호를 찾을 수 없습니다.");return;
			}else {
				request.cashcc.rootTrxId = cashMap.getString("trxId");
				request.cashcc.rootTrxDay = cashMap.getString("regDay");
				request.cashcc.rootTrackId = cashMap.getString("trackId");
				
			}
			if(cashMap.isEquals("status", "실패")) {
				response.result = ResultUtil.getResult("9999", "취소불가","현금영수증 발급 실패 거래입니다.");return;
			}else if(cashMap.isEquals("status", "오류실패")) {
				response.result = ResultUtil.getResult("9999", "취소불가","현금영수증 국세청 오류통지로 실패거래입니다.");return;
			}else {
				
			}
			
			
			
			
		}
		if(request.cashcc.trxType.equals("issue")){
			request.cashcc.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
			if(CommonUtil.isNullOrSpace(request.cashcc.identity)){
				response.result = ResultUtil.getResult("9999", "필수값없음","주민번호,사업자번호,휴대폰번호 값 없음.");return;
			}
			
			if(CommonUtil.isNullOrSpace(request.cashcc.usage)){
				response.result = ResultUtil.getResult("9999", "필수값없음","발급 구분값이 입력되지 않았습니다.");return;
			}
			
			if(CommonUtil.isNullOrSpace(request.cashcc.cashType)){
				response.result = ResultUtil.getResult("9999", "필수값없음","일반,도서공연비 구분이 입력되지 않았습니다.");return;
			}
			
			if(!(request.cashcc.usage.equals("0") || request.cashcc.usage.equals("1") || request.cashcc.usage.equals("2"))){
				response.result = ResultUtil.getResult("9999", "필수값오류","지출증빙용,소득공제용,자진발급 구분값 다름.");return;
			}
			
			if(request.cashcc.amount == 0){
				response.result = ResultUtil.getResult("9999", "필수값없음","합계 금액 값이 누락됨");return;
			}
			
			
			
			
		}
		
		request.cashcc.tmnId = mchtTmnMap.getString("tmnId");
	}

	@Override
	public void valid2(Request request, SharedMap<String, Object> sharedMap, SharedMap<String, Object> sales_mchtMap,
			SharedMap<String, Object> sales_mchtTmnMap, SharedMap<String, Object> sales_mchtMngMap) {
		// TODO Auto-generated method stub
		
	}
	
	

	

}
