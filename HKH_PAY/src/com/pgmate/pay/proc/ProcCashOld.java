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
public class ProcCashOld extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcCashOld.class );
	
	

	public ProcCashOld() {
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
		inter.trackId	= request.cash.trackId;
		inter.tmnId  	= request.cash.tmnId;
		inter.trxDate	= sharedMap.getString(PAYUNIT.REG_DATE);
		
		if(request.cash.action.equals("승인거래")) {
			inter.trxType  = "issue";
			inter.cashType = "0";
			inter.amount  = request.cash.amount;
			inter.service	= request.cash.serviceAmt;
			inter.vat     = request.cash.vatAmt;
			inter.supply  = request.cash.supplyAmt;
			if(request.cash.usage.equals("소득공제용")) {
				inter.usage = "0";
			}else {	//지출증빙용
				inter.usage = "1";
			}
			inter.identity = request.cash.identity;
			
			inter = cash.comm(inter);
			logger.info("응답:{},{}",inter.resultCd,inter.resultMsg);
			logger.info("응답일자:{}",inter.trxDate);
			
			response.cash = request.cash;
			response.cash.identity 	= inter.identity;	//마스크된 값 전달
			response.cash.authCd	= inter.authCd;
			
			response.result = ResultUtil.getResult(inter.resultCd, inter.resultMsg);
			response.result.create = inter.trxDate;
		}else{
			inter.trxType  = "revoke";
			inter.rootTrxId = request.cash.trxId;
			inter.amount  = request.cash.amount;
			inter.service	= request.cash.serviceAmt;
			inter.vat     = request.cash.vatAmt;
			inter.supply  = request.cash.supplyAmt;
			inter = cash.comm(inter);
			logger.info("응답:{},{}",inter.resultCd,inter.resultMsg);
			logger.info("응답일자:{}",inter.trxDate);
			
			response.cash = request.cash;
			response.cash.identity 	= inter.identity;	//마스크된 값 전달
			response.cash.authCd	= inter.authCd;
			response.cash.amount	= inter.amount;
			
			response.result = ResultUtil.getResult(inter.resultCd, inter.resultMsg);
			response.result.create = inter.trxDate;
					
		}
		
		
		setResponse();
		
		return;
	}


	@Override
	public void valid() {
		
		if(request.cash == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","현금영수증 발행정보가 없습니다..");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.cash.tmnId)){
			request.cash.tmnId = sharedMap.getString("tmnId");
		}
		
		if(CommonUtil.isNullOrSpace(request.cash.trackId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 주문번호가 입력되지 않았습니다.");return;
		}
		
		
		if(!(request.cash.action.equals("승인거래") || request.cash.action.equals("취소거래"))){
			response.result = ResultUtil.getResult("9999", "필수값없음","승인거래,취소거래 정보값 오류");return;
		}
		
		if(request.cash.action.equals("취소거래")){
			
			logger.info("cancel trxId : {}",request.cash.trxId);
			if(CommonUtil.isNullOrSpace(request.cash.trxId)){
				response.result = ResultUtil.getResult("9999", "필수값없음","원거래번호 없음");return;
			}
			
			SharedMap<String,Object> cashMap = trxDAO.getTrxCash2(request.cash.trxId);
			logger.info("test trxId : {}",request.cash.trxId);
			if(cashMap == null){
				response.result = ResultUtil.getResult("9999", "취소불가","원거래번호를 찾을 수 없습니다.");return;
			}
			if(cashMap.isEquals("status", "실패")) {
				response.result = ResultUtil.getResult("9999", "취소불가","현금영수증 발급 실패 거래입니다.");return;
			}else if(cashMap.isEquals("status", "오류실패")) {
				response.result = ResultUtil.getResult("9999", "취소불가","현금영수증 국세청 오류통지로 실패거래입니다.");return;
			}else {
				
			}
			
			
			
			
		}else{
			request.cash.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
			if(CommonUtil.isNullOrSpace(request.cash.identity)){
				response.result = ResultUtil.getResult("9999", "필수값없음","주민번호,사업자번호,휴대폰번호 값 없음.");return;
			}
			
			if(!(request.cash.usage.equals("지출증빙용") || request.cash.usage.equals("소득공제용"))){
				response.result = ResultUtil.getResult("9999", "필수값오류","지출증빙용,소득공제용 구분값 다름.");return;
			}
			
			if(request.cash.amount == 0){
				response.result = ResultUtil.getResult("9999", "필수값없음","합계 금액 없음");return;
			}
			
			
			
		}
		
		request.cash.tmnId = mchtTmnMap.getString("tmnId");
	}

	@Override
	public void valid2(Request request, SharedMap<String, Object> sharedMap, SharedMap<String, Object> sales_mchtMap,
			SharedMap<String, Object> sales_mchtTmnMap, SharedMap<String, Object> sales_mchtMngMap) {
		// TODO Auto-generated method stub
		
	}
	
	

	

}
