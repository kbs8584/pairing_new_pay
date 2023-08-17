package com.pgmate.pay.proc;

import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class VactOpen extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.VactOpen.class );
	private SharedMap<String,Object> vact = null; 


	public VactOpen() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		response.vact = request.vact;
		if(response.result != null){
			setResponse();
			return;
		}else{
			boolean execute = false;

			SharedMap<String,Object> ntsMap = null;

			if(vact.isEquals("vactType","임시")){
				execute = trxDAO.insertVactDtl(vact);
				//20190827 이메일 전송 추가 
				ntsMap = new SharedMap<String,Object>();
				ntsMap.put("tmnId",mchtTmnMap.getString("tmnId"));
				ntsMap.put("trackId",vact.getString("trackId"));
				ntsMap.put("payerEmail",vact.getString("payerEmail"));
				ntsMap.put("trxId",vact.getString("issueId"));
				if(mchtMngMap.isEquals("notiEmail", "사용") && !vact.isNullOrSpace("payerEmail")) {
					ntsMap.put("trxType","VACTISSUE");
					trxDAO.insertTrxNtsEmail(ntsMap);
				}
				
			}else{
				execute = trxDAO.updateVactDtl(vact);
			}
			
			if(execute){
				response.vact.issueId = vact.getString("issueId");
				response.vact.expireAt = vact.getString("expireAt");
				response.vact.status  = "발행";
				response.result = ResultUtil.getResult("0000", "정상","가상계좌가 발행되었습니다."+vact.getString("issueId"));
				
			}else{
				response.result = ResultUtil.getResult("9999", "발행오류","시스템 오류로 인한 가상계좌 발행 실패.");
			}

			/* 20191112 yhbae 발행 NTS 추가 */
			if(request.vact.webhookUrl != null && request.vact.webhookUrl.length() > 0 && ntsMap != null) {
				if(ntsMap.getString("trxId").length() > 13) {
					ntsMap.put("trxId", ntsMap.getString("trxId").substring(1,14));
				}
				ntsMap.put("trxType", "vissue");
				ntsMap.put("webHookUrl",request.vact.webhookUrl);
				ntsMap.put("regDay"		, CommonUtil.getCurrentDate("yyyyMMdd"));
				ntsMap.put("regTime"	, CommonUtil.getCurrentDate("HHmmss"));
				ntsMap.put("payLoad"	, GsonUtil.toJsonExcludeStrategies(response,true));
				ntsMap.put("resData", "");
				ntsMap.put("status", "전송실패");
				ntsMap.put("code", 0);
				ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
				trxDAO.insertTrxNTS(ntsMap);
			}

			setResponse();
			return;
		}
	}


	@Override
	public void valid() {
		
		
		if(request.vact == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌발행정보가 없습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.bankCd)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌발행은행이 지정되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.account)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌번호가 지정되지 않았습니다.");return;
		}
	
		if(CommonUtil.isNullOrSpace(request.vact.trackId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 주문번호가 입력되지 않았습니다.");return;
		}
		
		
		if(CommonUtil.isNullOrSpace(request.vact.amount)){
			request.vact.amount = "0";
			request.vact.oper = "ge";	// 기본 금액이 0 보다 클경우만으로 처리 
		}else{
			
			request.vact.oper = CommonUtil.nToB(request.vact.oper,"eq").toLowerCase();
			String[] opers = {"eq","le","lt","gt","ge"};	//기본 Operation
			boolean isMatch = false;
			for(String oper : opers){
				if(request.vact.oper.equals(oper)){
					isMatch = true; break;
				}
			}
			if(!isMatch){
				response.result = ResultUtil.getResult("9999", "필수값틀림","request.vact.oper 는 'eq','le','lt','gt','ge' 만 지원하며 기본값은 'eq' 입니다.");return;
			}
		}
		
		if(CommonUtil.parseLong(request.vact.amount) < 0){
			response.result = ResultUtil.getResult("9999", "필수값틀림","금액 포맷이 잘못되었거나 0 보다 작습니다.");return;
		}
		
		
		if(request.vact.udf1 == null){	request.vact.udf1=""; 		}
		if(request.vact.udf2 == null){	request.vact.udf2=""; 		}
		
		//TRX_IO  기록 
		trxDAO.insertTrxIO(sharedMap, request.vact);
		
		SharedMap<String,Object> mchtVactMngMap = trxDAO.getMchtMngVact(mchtMap.getString("mchtId"));
		
		if(mchtVactMngMap == null){
			response.result = ResultUtil.getResult("9999", "서비스미등록","가상계좌서비스를 사용하지 않는 가맹점입니다.");return;
		}else{
			if(!mchtVactMngMap.isEquals("status","사용")){
				response.result = ResultUtil.getResult("9999", "서비스사용이전","가상계좌서비스가 활성화 되지 않았습니다. 현재 상태"+mchtVactMngMap.getString("status"));return;
			}//startDay 에 대한 제어는 차후에 서비스 개시 이후 생각해보자 
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.holderName)){
			request.vact.holderName = mchtVactMngMap.getString("holderName");
		}
		
		logger.info("mchtId : {}, vactType : {}",mchtVactMngMap.getString("mchtId"),mchtVactMngMap.getString("vactType"));
		String issueId = trxDAO.isDuplicatedVactTrackId(sharedMap.getString(PAYUNIT.MCHTID),request.vact.trackId);
		
		if(!issueId.equals("")){
			logger.info("duplicated trackId : {}, issueId : {}",request.vact.trackId,issueId);
			response.result = ResultUtil.getResult("9999", "중복된 주문번호입니다.","가상계좌 발행원장에 이미 사용된 주문번호입니다.");return;
		}
		
		
		if(mchtVactMngMap.isEquals("issueType", "영구")){
			vact = trxDAO.getReadyVactDtl(request.vact.account, mchtMap.getString("mchtId"));
			if(vact == null){ 	//대기중인 가상계좌가 없고 
				vact = trxDAO.getNotIssueVactDtl(request.vact.account, mchtMap.getString("mchtId"));
				if(vact == null){ // 발행중인 가상계좌가 없을 경우 즉, 만료일 경우 
					vact = new SharedMap<String,Object>();
					vact.put("issueId", TrxDAO.getVactIssueId());
					vact.put("vactType","영구");
					vact.put("mchtId",mchtMap.getString("mchtId"));
					vact.put("account",request.vact.account);
				}else{
					response.result = ResultUtil.getResult("9999", "가상계좌없음","영구발급된 가상계좌가 없거나 이미 사용중인 계좌입니다.");return;
				}
			}else{
				if(!vact.isEquals("bankCd", request.vact.bankCd)){
					response.result = ResultUtil.getResult("9999", "은행코드틀림","가상계좌 발행은행과 요청된 은행코드가 다릅니다.");return;
				}
			}
			
			
		}else{
			List<String> getList = PAYUNIT.vactCacheMap.get("PG_VACT_BANK_"+request.vact.bankCd);
			if(getList == null){
				response.result = ResultUtil.getResult("9999", "가상계좌없음","임시 가상계좌 발급내역이 없거나 5분이 경과되어 삭제된 계좌입니다.");return;
			}else{
				boolean isExist = false;
				for(int i=0;i<getList.size() ; i++){
					String s = getList.get(i);
					if(s.equals(request.vact.account)){
						isExist = true;
						getList.remove(i);	//임시 발행 내역 삭제
						break;
					}
				}
				
				if(!isExist){
					response.result = ResultUtil.getResult("9999", "가상계좌없음","임시 가상계좌 발급내역이 없거나 5분이 경과되어 삭제된 계좌입니다.");return;
				}else{
					PAYUNIT.vactCacheMap.put("PG_VACT_BANK_"+request.vact.bankCd,getList); //임시 발행내역 삭제된거 업데이트
				}
			}
			
			//임시 발행 초기화
			vact = new SharedMap<String,Object>();
			vact.put("issueId", TrxDAO.getVactIssueId());
			vact.put("vactType","임시");
			vact.put("tmnId",mchtTmnMap.getString("tmnId")); // 20190920 추가
			vact.put("mchtId",mchtMap.getString("mchtId"));
			vact.put("account",request.vact.account);

		}

		//20190827 이메일 및 전화번호 추가 + 20190903 구매자 성명 추가
		vact.put("payerEmail",CommonUtil.nToB(request.vact.payerEmail));
		vact.put("payerTel",CommonUtil.nToB(request.vact.payerTel));
		vact.put("payerName",CommonUtil.nToB(request.vact.payerName));
		//20190920-yhbae 추가
		request.vact.tmnId 	= mchtTmnMap.getString("tmnId");

		//20190827 현금영수증 정보가 있을 경우 이에 대한 Validation check - vact 생성 후로 위치수정
		if(request.cashcc != null && mchtVactMngMap.isEquals("issueType", "임시")) {
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
				request.cashcc.amount = CommonUtil.parseLong(request.vact.amount);
			}
			request.cashcc.trxType 	= "issue";
			request.cashcc.tmnId 	= mchtTmnMap.getString("tmnId");
			request.cashcc.udf1		= "VACT";
			request.cashcc.udf2		= sharedMap.getString("trxId");
			if(CommonUtil.isNullOrSpace(request.cashcc.trackId)) {
				request.cashcc.trackId = request.vact.trackId;
			}
			
			
			vact.put("cash",GsonUtil.toJson(request.cashcc));
		}

		// 20190830(yhbae) 상품정보 추가
		if(request.vact.products != null){
			vact.put(PAYUNIT.KEY_PROD, GenKey.genKeys(CPKEY.PRODUCT, vact.getString("issueId")));
			trxDAO.insertProduct(vact.getString(PAYUNIT.KEY_PROD), request.vact.products, CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
		}

		
		
		//초기값 설정
		vact.put("status","발행");
		vact.put("holderName", request.vact.holderName);
		vact.put("amount", request.vact.amount);
		vact.put("oper", request.vact.oper);
		vact.put("trackId", request.vact.trackId);
		vact.put("udf1", request.vact.udf1);
		vact.put("udf2", request.vact.udf2);

		// 20190903(yhbae) expireAt 입력받아 사용하도록 추가
		if(request.vact.expireAt != null) {
			if(request.vact.expireAt.matches("[0-9]{8,10}")) {
				if(request.vact.expireAt.length() == 8) { vact.put("expireAt", request.vact.expireAt + "00"); }
				else { vact.put("expireAt", request.vact.expireAt); }
			} else {
				response.result = ResultUtil.getResult("9999", "옵션값오류","만료기간 포맷이 맞지 않습니다.");return;
			}
		} else {
			int expireSet = mchtVactMngMap.getInt("expireSet");
			if(expireSet == 365){
				vact.put("expireAt", CommonUtil.getOpDate(Calendar.YEAR, 1, CommonUtil.getCurrentDate("yyyyMMdd"))+"00");
			}else{
				vact.put("expireAt", CommonUtil.getOpDate(Calendar.DATE, expireSet+1, CommonUtil.getCurrentDate("yyyyMMdd"))+"00");
			}
		}
		
		logger.info("VACT OPEN INFO : [{}]",GsonUtil.toJson(vact,true,""));
		
	}

	@Override
	public void valid2(Request request, SharedMap<String, Object> sharedMap, SharedMap<String, Object> sales_mchtMap,
			SharedMap<String, Object> sales_mchtTmnMap, SharedMap<String, Object> sales_mchtMngMap) {
		// TODO Auto-generated method stub
		
	}

}
