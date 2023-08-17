package com.pgmate.pay.proc;

import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.dao.WHDAO;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcWebHookCyrex {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.proc.ProcWebHookCyrex.class);
	
	private long startTime = System.nanoTime();
	private RoutingContext rc = null;
	private SharedMap<String, Object> sharedMap 	= null;
	private SharedMap<String, Object> requestMap 	= null;
	private SharedMap<String, Object> mchtTmnMap 	= null;
	private SharedMap<String, Object> mchtMap 		= null;
	private SharedMap<String, Object> rootTrxMap  	= null;
	private SharedMap<String, Object> whMap 		= null;
	private SharedMap<String, Object> vanMap 		= null;
	
	private String response = "Fail";
	private String resMsg = "";
	private WHDAO whDAO = null;
	private boolean retry = false;
	
	public ProcWebHookCyrex() {
		whDAO = new WHDAO();
		requestMap = new SharedMap<String, Object>();
		whMap = new SharedMap<String, Object>();
	}

	public void exec(RoutingContext rc,SharedMap<String,Object> sharedMap) {
		this.rc = rc;
		this.sharedMap = sharedMap;
		
		if(sharedMap.getString(PAYUNIT.URI).indexOf("/retry/") > -1) {
			retry = true;
			logger.info("웹에서 재 처리한 데이터");
		}
		
		//WHMAP 에 기록하여 추후 retry 에 사용한다.
		parseQueryString(sharedMap.getString(PAYUNIT.PAYLOAD));
		if(requestMap.isNullOrSpace("trxType")){
			setError();
			return;
		}
		
		logger.info("trxType: {}",requestMap.getString("trxType"));
		logger.info("mchtId : {}",requestMap.getString("mchtId"));
		logger.info("tmnId  : {}",requestMap.getString("tmnId"));
		logger.info("trackId: {}",requestMap.getString("trackId"));
		logger.info("trxId  : {}",requestMap.getString("trxId"));
		logger.info("authCd : {}",requestMap.getString("authCd"));
		logger.info("rootTrxId : {}",requestMap.getString("rootTrxId"));
		//VAN 조회
		SharedMap<String,Object> vanMap = whDAO.getVanByTmnId(requestMap.getString("tmnId"));
		if(vanMap == null){
			vanMap = whDAO.getVanByVanId(requestMap.getString("mchtId"));
		}
		
		if(vanMap == null){
			logger.info("등록된 Terminal 정보를 찾을 수 없습니다.");
			//기본 VAN 정보 설정
			if(sharedMap.startsWith(PAYUNIT.REMOTEIP, "203.245.13.")){
				whMap.put("van", "KWON1");
				logger.info("기본 VAN KWON1 으로 설정");
			}
			response = "Fail|미등록터미널";
			setResponse();
			return;
		}else{
			whMap.put("van", vanMap.getString("van"));
		}
		
		// 20201111 KWON수기결제 취소노티 처리 - 테스트 필요
		if(sharedMap.startsWith(PAYUNIT.REMOTEIP, "203.245.13.") && requestMap.getString("trxType").equals("REFUND")) {
			SharedMap<String,Object> tmnMap = whDAO.getTmnId(requestMap.getString("tmnId"));
			SharedMap<String,Object> tmnTrxMap = whDAO.getTmnIdByTrxId(requestMap.getString("rootTrxId"));
			
			if(tmnMap == null) {
				if(tmnTrxMap == null) {
					response = "Fail|미등록 터미널 및 원거래를 찾을 수 없습니다.";
					setResponse();
					return;
				} else {
					requestMap.put("tmnId", tmnTrxMap.getString("tmnId"));
				}
			}
		}
		
		whMap.put("orgData", sharedMap.getString(PAYUNIT.PAYLOAD));	//원본 데이터 
		whMap.put("trxId", sharedMap.getString("trxId"));			//새로 채번된 거래번호
		whMap.put("tmnId", requestMap.getString("tmnId"));			//전달된 터미널 ID
		whMap.put("trxType", requestMap.getString("trxType"));
		whMap.put("reqData", requestMap.toJson());
		whMap.put("vanId", requestMap.getString("mchtId"));
		whMap.put("vanTrxId", requestMap.getString("trxId"));
		
		// 마이너스 값으로 들어온 것 보정
		if(requestMap.getString("trxType").equals("REFUND")){
			requestMap.put("amount", -requestMap.getLong("amount"));
		}
		logger.info("van    : {}",whMap.getString("van"));
		//logger.info("requestMap : {}", requestMap.toJson());
		
		//DB 처리할 SharedMap 작성
		sharedMap.put("trxType"		, "WHTR");
		sharedMap.put("van"			, whMap.getString("van"));
		sharedMap.put("vanId"		, requestMap.getString("mchtId"));
		sharedMap.put("trackId" 	, requestMap.getString("trackId"));
		sharedMap.put("vanTrxId"	, requestMap.getString("trxId"));
		sharedMap.put("vanResultCd"	, "0000");
		sharedMap.put("vanResultMsg", "정상");
		sharedMap.put("amount"		, requestMap.getString("amount"));
		sharedMap.put("regDate"		, requestMap.getString("trxDate"));
		
		
		if(requestMap.isEquals("trxType", "REFUND")){
			sharedMap.put("vanRootTrxId", requestMap.getString("rootTrxId"));
			sharedMap.put("rootTrnDay"	, "20"+requestMap.getString("rootTrxId").substring(1,7));
			if(sharedMap.isEquals("rootTrnDay", requestMap.getString("trxDate").substring(0,8))){
				sharedMap.put("rfdType", "승인취소");
			}else{
				sharedMap.put("rfdType", "매입취소");
			}
		}

		if(!valid()) {
			response = "Fail|" + resMsg;
			setResponse();
			return;
		}
		response = "result=0000||OK";
		
		if(requestMap.getString("trxType").equals("REFUND")) { // valid 에서 만든 값
			/*(
			SharedMap<String, Object> adminRfd = whDAO.getAdminRfdByVanTrxId(requestMap.getString("vanTrxId"));
			
			if(adminRfd != null && adminRfd.size() > 0) {
				logger.debug("ADMIN REFUND REQUEST : {} / IDX : {}" + adminRfd.getString("idx"));
				whDAO.updateAdminRfd(adminRfd.getString("idx"), sharedMap.getString("trxId"), "0000");	
			}
			*/
			whDAO.insertTrxRFD(sharedMap, rootTrxMap);

			sharedMap.put("resultCd", "0000");
			sharedMap.put("resultMsg", "정상");
			sharedMap.put("advanceMsg", "정상취소");
			
			//당일 취소는 반드시 전액 취소만 가능하며 . 승인 취소로 업데이트 한다.
			if(rootTrxMap.isEquals("regDay",sharedMap.getString("regDate").substring(0,8))){
				whDAO.updateTrxPay(rootTrxMap.getString("trxId"));
			}
			
		} else {
			
			whDAO.insertTrxREQ(sharedMap);			
			sharedMap.put("authCd", CommonUtil.nToB(requestMap.getString("authCd")));	
			sharedMap.put("resultCd", "0000");
			sharedMap.put("resultMsg", "정상");
			sharedMap.put("advanceMsg", "정상승인");
			whDAO.insertTrxRES(sharedMap);
		}
		
		setResponse();
		
		return;
	}
	
	
	private boolean valid() {
		
		if(requestMap.getString("trxType").equals("PAY")){
			if (whDAO.isDuplicatedVanTrxId(sharedMap.getString("van"),sharedMap.getString("vanTrxId"))) {
				logger.info("중복거래,vanTrxId 일치 :{}", sharedMap.getString("vanTrxId"));
				resMsg = "중복거래 :" +sharedMap.getString("vanTrxId");
				return false;
			}
		}else{
			SharedMap<String,Object> rfdFinded = whDAO.getRfdByVanTrxId(sharedMap.getString("van"),sharedMap.getString("vanTrxId"));
			if(rfdFinded != null && rfdFinded.size() !=0){
				logger.info("중복거래,vanTrxId 일치 :{}", sharedMap.getString("vanTrxId"));
				resMsg = "중복거래 :" +sharedMap.getString("vanTrxId");
				if(rfdFinded.startsWith("trackId", "rfd_")){
					logger.info("웹관리자취소");
					resMsg = "중복거래,웹관리자취소";
				}
				return false;
			}else{
				logger.info("신규취소거래 : {}", sharedMap.getString("vanTrxId"));
			}
			
		}
		
		mchtTmnMap = whDAO.getVanByTmnId(requestMap.getString("tmnId"));
		if (mchtTmnMap == null || mchtTmnMap.isEmpty()) {
			logger.debug("터미널 아이디가 없습니다.:{}", requestMap.getString("tmnId"));
			resMsg = "미등록터미널,등록요망:" + CommonUtil.nToB(requestMap.getString("tmnId"));
			return false;
		}
		
		//등록된 tmnId 및 가맹점 ID SET
		sharedMap.put("tmnId"	, mchtTmnMap.getString("tmnId"));
		sharedMap.put("mchtId"	, mchtTmnMap.getString("mchtId"));
		sharedMap.put("taxType"	, mchtTmnMap.getString("taxType"));
		
		//할부기간 설정 
		if(requestMap.isNullOrSpace("installment")){
			sharedMap.put("installment","00");
		}else{
			sharedMap.put("installment",requestMap.getString("installment"));
		}
		
		// 취소거래의 경우 원거래가 있는지 확인한다.
		if(requestMap.getString("trxType").equals("REFUND")) {
			rootTrxMap = whDAO.getTrxPayByVanTrxId(sharedMap.getString("rootTrnDay"),sharedMap.getString("van"), sharedMap.getString("vanRootTrxId"));
			if(rootTrxMap == null){
				logger.info("원거래를 찾을 수 없습니다. rootTrnDay : {}, rootTrxId: {}", sharedMap.getString("rootTrnDay"), sharedMap.getString("vanRootTrxId"));
				resMsg = "취소원거래를 찾을 수 없습니다. rootTrnDay : "+ sharedMap.getString("rootTrnDay") + ", rootTrackId:" + sharedMap.getString("vanRootTrxId");
				return false;
				
			}
			logger.info("root TrxId: {}",rootTrxMap.getString("trxId"));
			logger.info("root amount: {}",rootTrxMap.getLong("amount"));
			logger.info("root regDay: {}",rootTrxMap.getString("regDay"));
			logger.info("root authCd: {}",rootTrxMap.getString("authCd"));
			logger.info("root trackId: {}",rootTrxMap.getString("trackId"));
			
			//원거래 취소 확인
			SharedMap<String,Object> rfdMap = whDAO.getTrxRfdByTrxId(rootTrxMap.getString("trxId"));
			if(rfdMap != null){
				if(rfdMap.isEquals("rfdAll", "전액") && rfdMap.isEquals("status", "완료")){
					logger.info("이미 취소된 거래입니다.");
					resMsg = "기취소거래,전액취소";
					if(rfdMap.startsWith("trackId", "rfd_")){
						logger.info("웹관리자취소");
						resMsg = "기취소거래,전액취소,웹관리자";
					}
					
					return false;
				}
			}
			
			SharedMap<String,Object> refundedMap = whDAO.getTrxRefundSumByTrxId(rootTrxMap.getString("trxId"));
			long refundedAmount = refundedMap.getLong("AMT");
			logger.info("기취소총액: {}",refundedAmount);
			
			if(rootTrxMap.getLong("amount") == -refundedAmount){
				logger.info("기취소거래,취소금액전액");
				resMsg = "기취소거래,취소금액전액";
				return false;
			}
			
			logger.info("취소요청:{},기취소금액:{}",sharedMap.getLong("amount") , -rootTrxMap.getLong("amount"));
			if(sharedMap.getLong("amount") == -rootTrxMap.getLong("amount")){
				sharedMap.put("rfdAll", "전액");
			}else{
				sharedMap.put("rfdAll", "부분");
			}
			
			logger.info("취소구분 : {}",sharedMap.getString("rfdAll"));
	
		} else {
			sharedMap.put(PAYUNIT.KEY_CARD, GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
			sharedMap.put(PAYUNIT.KEY_PROD, GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));
			

			sharedMap.put("bin"		, requestMap.getString("bin"));
			sharedMap.put("last4"	, requestMap.getString("last4"));
			sharedMap.put("issuer"	, requestMap.getString("issuer"));
			sharedMap.put("acquirer", requestMap.getString("acquirer"));
			sharedMap.put("cardId"	, sharedMap.getString(PAYUNIT.KEY_CARD));
			sharedMap.put("cardType", requestMap.getString("cardType"));
			
			Card card = new Card();
			card.number = sharedMap.getString("bin")+"xxxxxx"+sharedMap.getString("last4");
			if(sharedMap.startsWith("bin", "34") || sharedMap.startsWith("bin", "37")){
				card.number = sharedMap.getString("bin")+"xxxxx"+sharedMap.getString("last4");
			}
			card.last4 	= sharedMap.getString("last4");
			card.issuer	= sharedMap.getString("issuer");
			card.cardId	= sharedMap.getString("cardId");
			card.bin	= sharedMap.getString("bin");
			card.installment = CommonUtil.parseInt(sharedMap.getString("installment"));
			card.acquirer= sharedMap.getString("acquirer");
			card.cardType = sharedMap.getString("cardType");
			String encrypted = Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16)));
			whDAO.insertCard(sharedMap.getString(PAYUNIT.KEY_CARD), encrypted);
			
		}
		
		return true;
	}
	




	

	protected void setResponse() {
		whMap.put("resData", response);
		if(retry) {
			VertXMessage.set200(rc, "text/html",response, "");
		}else{
			if(whMap.like("resData","중복거래") || whMap.like("resData","기취소") ||  whMap.like("resData","미등록")){
				VertXMessage.set200(rc, "text/html","result=0000||OK", "");
			}else{
				VertXMessage.set200(rc, "text/html",response, "");
			}
		}
		logger.info("estimatedTime : {}", TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));
		
		// WeebHook 정보를 저장.
//		if(retry) {
//			if(response.equals("OK")){
//				whMap.put("terminal","O");
//			}
//			whDAO.updateTrxWH(whMap);
//		} else {
//			whDAO.insertTrxWH(whMap);
//		}
		
		sharedMap = null;
	}
	
	
	protected void setError() {
		VertXMessage.set500(rc);
		sharedMap = null;
	}
	

	
	
	public void parseQueryString(String str){
		if(requestMap == null){
			requestMap = new SharedMap<String,Object>();
		}
		try{
			String[] st = str.split("&");
			for (int i = 0; i < st.length; i++) {
				int index = st[i].indexOf('=');
				if (index > 0){
					String key = st[i].substring(0, index);
					try{
						requestMap.put(key, URLDecoder.decode(st[i].substring(index + 1),"utf-8"));
					}catch(Exception e){
						requestMap.put(key, "");
					}
				}
			}
		}catch(Exception e){
			logger.error(CommonUtil.getExceptionMessage(e));
		}
		
		
	
		
	}
	
	
	

	

	public static void main(String[] args){
		
		
		
	}
}
