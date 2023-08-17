package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import com.popbill.api.PopbillException;
import com.popbill.api.Response;
import com.popbill.api.cashbill.Cashbill;
import com.popbill.api.cashbill.CashbillInfo;
import com.popbill.api.cashbill.CashbillServiceImp;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcCash extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcCash.class );
	private long code = 1;
	private String message = "";
	
	

	public ProcCash() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		if(response.result != null){
			setResponse();
			return;
		}
		
		
		
		CashbillServiceImp cbs = new CashbillServiceImp();
		cbs.setLinkID("PAIRINGSOLUTION");
		cbs.setSecretKey("ctf4uzjhR/4AvpdRpPA3EP/tQGDPvRW1pFX9KK2D3QI=");
		cbs.setTest(false);
		
		if(request.cash.action.equals("승인거래")){
			SharedMap<String,Object> taxMap = trxDAO.getMchtTaxByTaxId(mchtTmnMap.getString("taxId"));
			trxDAO.insertTrxCash(sharedMap, request.cash,taxMap.getString("iden"));
			
			
			
			
			Cashbill cb = new Cashbill();
			cb.setMgtKey(sharedMap.getString("trxId"));
			cb.setTradeType("승인거래");  
			cb.setTradeUsage(request.cash.usage);//지출증빙용
			cb.setIdentityNum(request.cash.identity);//주민등록번호/휴대폰/카드번호
			cb.setTaxationType(request.cash.taxType);//과세/비과세
			cb.setSupplyCost(CommonUtil.toString(request.cash.supplyAmt));	//공급가액
			cb.setTax(CommonUtil.toString(request.cash.vatAmt));			//부가세
			cb.setServiceFee(CommonUtil.toString(request.cash.serviceAmt));		//봉사료
			cb.setTotalAmount(CommonUtil.toString(request.cash.amount)); //합계금액 봉사료+공급가액+_세액

			
			
			cb.setFranchiseCorpNum(taxMap.getString("iden"));
			cb.setFranchiseCorpName(taxMap.getString("compName"));
			cb.setFranchiseCEOName(taxMap.getString("ceoName"));
			cb.setFranchiseAddr(taxMap.getString("addr1")+" "+taxMap.getString("addr2"));
			cb.setFranchiseTEL(mchtMap.getString("tel1"));
			
			cb.setCustomerName(request.cash.custName);
			cb.setItemName(request.cash.pdtName);
			cb.setEmail(request.cash.custEmail);
			cb.setHp(request.cash.custTel);
			
			
			if(registIssue(cbs,taxMap.getString("iden"),cb)){
				CashbillInfo cbi = getInfo(cbs,taxMap.getString("iden"),sharedMap.getString("trxId"));
				if(cbi == null){
					logger.info("issue error : code : {},message : {}",code,message);
					trxDAO.insertTrxCashDtl(sharedMap.getString("trxId"), "장애", "-1", message+" "+code,"","",CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
					response.result = ResultUtil.getResult("9999", "시스템장애","제휴 발급처 정보 수신 장애 , 관리자 전화요망 "+message);
				}else{
					logger.info("거래번호 : {}",cbi.getItemKey());
					logger.info("관리번호 : {}",cbi.getMgtKey());
					logger.info("거래일자 : {}",cbi.getTradeDate());
					logger.info("발행일시 : {}",cbi.getIssueDT());
					logger.info("등록일시 : {}",cbi.getRegDT());
					logger.info("상태코드 : {}",cbi.getStateCode());
					logger.info("상태변경 : {}",cbi.getStateDT());
					
					trxDAO.insertTrxCashDtl(sharedMap.getString("trxId"), "접수",CommonUtil.toString(cbi.getStateCode()), getCodeMessage(cbi.getStateCode()),cbi.getNtsresultCode(),cbi.getNtsresultMessage(),cbi.getStateDT());
					trxDAO.updateTrxCash(sharedMap.getString("trxId"), cbi.getItemKey(),"",cbi.getTradeDate(), "");
					
					
					response.cash = request.cash;
					response.result = ResultUtil.getResult("0000", "현금영수증 정상 발급 신청");
				}
				
				
			}else{
				logger.info("issue error : code : {},message : {}",code,message);
				trxDAO.insertTrxCashDtl(sharedMap.getString("trxId"), "장애", "-1", message+" "+code,"","",CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
				response.result = ResultUtil.getResult("9999", "시스템장애","제휴 발급처 통신 장애 "+message);
			}
			
			
			
		}else{
			SharedMap<String,Object> cashMap = trxDAO.getTrxCash(request.cash.trxId);
			SharedMap<String,Object> cashDtlMap = trxDAO.getTrxCashDtl(request.cash.trxId);
			
			
			
			if(cashMap.isEquals("status", "승인")){
				if(cashDtlMap.isEquals("resultCd", "300") || cashDtlMap.isEquals("resultCd", "301") || cashDtlMap.isEquals("resultCd", "302") || cashDtlMap.isEquals("resultCd", "303")){
					
					CashbillInfo cbi = getInfo(cbs,cashMap.getString("issuer"),sharedMap.getString("trxId"));
					if(cbi != null){
						logger.info("거래번호 : {}",cbi.getItemKey());
						logger.info("관리번호 : {}",cbi.getMgtKey());
						logger.info("거래일자 : {}",cbi.getTradeDate());
						logger.info("발행일시 : {}",cbi.getIssueDT());
						logger.info("등록일시 : {}",cbi.getRegDT());
						logger.info("상태코드 : {}",cbi.getStateCode());
						logger.info("상태변경 : {}",cbi.getStateDT());
						
						trxDAO.updateTrxCashDtl(sharedMap.getString("trxId"));
						trxDAO.insertTrxCashDtl(sharedMap.getString("trxId"), "접수",CommonUtil.toString(cbi.getStateCode()), getCodeMessage(cbi.getStateCode()),cbi.getNtsresultCode(),cbi.getNtsresultMessage(),cbi.getStateDT());
						if(cbi.getConfirmNum() != null){
							if(!cbi.getConfirmNum().equals("null")){
								trxDAO.updateTrxCash(sharedMap.getString("trxId"), "",cbi.getConfirmNum(),"", "");
							}
						}
					}
					//정보 갱신
					cashMap = trxDAO.getTrxCash(request.cash.trxId);
					cashDtlMap = trxDAO.getTrxCashDtl(request.cash.trxId);
				}
				
				
				
				if(cashDtlMap.isEquals("resultCd", "300")){
					if(cancelIssue(cbs,cashMap.getString("issuer"), cashMap.getString("trxId"))){
						
						trxDAO.updateTrxCashRevoke(request.cash.trxId, "취소",TrxDAO.getCashId());
						trxDAO.updateTrxCashDtl(sharedMap.getString("trxId"));
						trxDAO.insertTrxCashDtl(sharedMap.getString("trxId"), "전송완료","400", " 전송완료","0000","정상취소",CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
						response.result = ResultUtil.getResult("0000", "취소성공","취소 처리가 완료되었습니다.");
					}else{
						response.result = ResultUtil.getResult("9999", "통신장애","통신장애 입니다. 재시도 바랍니다. "+message);
					}
				}else if(cashDtlMap.isEquals("resultCd", "301") || cashDtlMap.isEquals("resultCd", "302") || cashDtlMap.isEquals("resultCd", "303")){
					logger.info("취소 error : 전송중 거래 : {}",GsonUtil.toJson(cashDtlMap));					
					response.result = ResultUtil.getResult("9999", "취소실패","국세청 전송 중인 거래로 취소 불가합니다.발행일 기준 오후 5시 이전에 발행된 현금영수증은 다음날 오후 2시 이후 취소 가능합니다. ");
				}else if(cashDtlMap.isEquals("resultCd", "304")){
					String canTrxId = TrxDAO.getCashId();
					if(revokeIssue(cbs,cashMap.getString("issuer"),canTrxId,cashMap.getString("authCd"),cashMap.getString("issueDay"))){
						trxDAO.updateTrxCashRevoke(request.cash.trxId, "취소",canTrxId);
						trxDAO.updateTrxCashDtl(sharedMap.getString("trxId"));
						trxDAO.insertTrxCashDtl(sharedMap.getString("trxId"), "전송완료","400", " 전송완료","0000","정상취소",CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
						response.result = ResultUtil.getResult("0000", "취소성공","취소 접수가 완료되었습니다.익일 취소를 확인할 수 있습니다.");
					}else{
						response.result = ResultUtil.getResult("9999", "통신장애","통신장애 입니다. 재시도 바랍니다. "+message);
					}
					
				}else if(cashDtlMap.startsWith("resultCd", "305")){
					logger.info("취소 error : 미승인 거래 : {}",GsonUtil.toJson(cashDtlMap));					
					response.result = ResultUtil.getResult("9999", "취소실패","미승인 거래, 국세청에서 발급 오류를 발행한 상태 "+cashDtlMap.getString("ntsMsg"));
				}else if(cashDtlMap.isEquals("resultCd", "-1")){
					logger.info("취소 error : 미승인 거래 : {}",GsonUtil.toJson(cashDtlMap));					
					response.result = ResultUtil.getResult("9999", "취소실패","승인장애된 거래이므로 발급되지 않은 현금영수증입니다.");
				}
			}else{
				if(cashDtlMap.startsWith("resultCd", "4")){
					logger.info("취소 error : 기취소거래 : {}",GsonUtil.toJson(cashDtlMap));					
					response.result = ResultUtil.getResult("0000", "취소성공","취소 전송 중이거나 이미 취소된 거래");
				}
			}
			
			
		}
		
		
		setResponse();
		
		return;
	}


	@Override
	public void valid() {
		
		if(request.cash == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","현금영수증 발행정보가 없습니다x");return;
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
			
			if(trxDAO.getTrxCash(request.cash.trxId) == null){
				response.result = ResultUtil.getResult("9999", "취소불가","원거래번호를 찾을 수 없습니다.");return;
			}
			
			
		}else{
			request.cash.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
			if(CommonUtil.isNullOrSpace(request.cash.identity)){
				response.result = ResultUtil.getResult("9999", "필수값없음","주민번호,사업자번호,휴대폰번호 값 없음.");return;
			}
			
			if(!(request.cash.usage.equals("지출증빙용") || request.cash.usage.equals("소득공제용"))){
				response.result = ResultUtil.getResult("9999", "필수값오류","지출증빙용,소득공제용 구분값 다름.");return;
			}
			if(!(request.cash.taxType.equals("과세") || request.cash.taxType.equals("비과세"))){
				response.result = ResultUtil.getResult("9999", "필수값오류","과세,비과세 구분값 다름.");return;
			}
			
			if(request.cash.amount == 0){
				response.result = ResultUtil.getResult("9999", "필수값없음","합계 금액 없음");return;
			}
			
			if(request.cash.amount == 0){
				response.result = ResultUtil.getResult("9999", "금액오류","합계 금액 없음");return;
			}
			if(request.cash.supplyAmt == 0){
				response.result = ResultUtil.getResult("9999", "금액오류","공급가액 정보 없음");return;
			}
			
			
		}
		
		request.cash.tmnId = mchtTmnMap.getString("tmnId");
	}
	
	
	
	public boolean registIssue(CashbillServiceImp cbs,String idenNum,Cashbill cb){
		Response res = null;
		try {

			res = cbs.registIssue(idenNum, cb, "");
			code = res.getCode();
			message = res.getMessage();
		}catch (PopbillException pe) {
			code = pe.getCode();
			message = pe.getMessage();
		}
		
		if(code == 1){
			return true;
		}else{
			return false;
		}
	}
	
	public boolean cancelIssue(CashbillServiceImp cbs,String idenNum,String vanTrxId){
		Response res = null;
		try {
			logger.info("cancelIssue : {},{}",idenNum,vanTrxId);
			res = cbs.cancelIssue(idenNum, vanTrxId, "");
			code = res.getCode();
			message = res.getMessage();
		}catch (PopbillException pe) {
			code = pe.getCode();
			message = pe.getMessage();
		}
		
		if(code == 1){
			return true;
		}else{
			return false;
		}
	}
	
	
	public boolean revokeIssue(CashbillServiceImp cbs,String idenNum,String vanTrxId,String authCd,String issueDay){
		Response res = null;
		try {

			res = cbs.revokeRegistIssue(idenNum, vanTrxId, authCd, issueDay);
			code = res.getCode();
			message = res.getMessage();
		}catch (PopbillException pe) {
			code = pe.getCode();
			message = pe.getMessage();
		}
		
		if(code == 1){
			return true;
		}else{
			return false;
		}
	}
	
	
	public CashbillInfo getInfo(CashbillServiceImp cbs,String idenNum,String trxId){
		CashbillInfo cashbillInfo = null;
		try {
			cashbillInfo = cbs.getInfo(idenNum, trxId);
		} catch (PopbillException pe) {
			code = pe.getCode();
			message = pe.getMessage();
		}
		return cashbillInfo;
	}
	
	
	public String getCodeMessage(int c){
		String code = CommonUtil.toString(c);
		logger.info("############# : [{}]",code);
		if(code.endsWith("0")){
			return "접수";
		}else if(code.endsWith("4")){
			return "전송완료";
		}else if(code.endsWith("5")){
			return "전송실패";
		}else{
			return "전송중";
		}
	}

	@Override
	public void valid2(Request request, SharedMap<String, Object> sharedMap, SharedMap<String, Object> sales_mchtMap,
			SharedMap<String, Object> sales_mchtTmnMap, SharedMap<String, Object> sales_mchtMngMap) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	

}
