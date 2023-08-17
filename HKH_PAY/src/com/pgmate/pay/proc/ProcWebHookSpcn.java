package com.pgmate.pay.proc;

import java.io.UnsupportedEncodingException;
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
public class ProcWebHookSpcn {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.proc.ProcWebHookSpcn.class);
	private long startTime = System.nanoTime();
	private RoutingContext rc = null;
	private SharedMap<String, Object> sharedMap = null;
	private SharedMap<String, Object> requestMap = null;
	private SharedMap<String, Object> mchtTmnMap = null;
	private SharedMap<String, Object> mchtMap = null;
	private SharedMap<String, Object> rootTrxMap  	= null;
	private SharedMap<String, Object> trxMap  = null;
	private SharedMap<String, Object> whMap = null;

	private String response = "Fail";
	private String resMsg 	= "";
	private WHDAO whDAO 	= null;
	private boolean retry 	= false;

	public ProcWebHookSpcn() {
		whDAO = new WHDAO();
		requestMap = new SharedMap<String, Object>();
		whMap = new SharedMap<String, Object>();
	}

	public void exec(RoutingContext rc, SharedMap<String,Object> sharedMap) {
		this.rc = rc;
		this.sharedMap = sharedMap;
		
		if(sharedMap.getString(PAYUNIT.URI).indexOf("/retry/") > -1) {
			retry = true;
			logger.info("웹에서 재 처리한 데이터");
		}
		
		parseQueryString(sharedMap.getString(PAYUNIT.PAYLOAD));
		if(requestMap.isNullOrSpace("cmd")){
			setError();
			return;
		}
	
		
		
		if(requestMap.isEquals("requestFlag", "C")){
			requestMap.put("tmnId",requestMap.getString("vanCatId").substring(0, 10));
		}
		
		if(requestMap.isEquals("cmd", "0")){
			requestMap.put("trxType", "PAY");
		} else {
			requestMap.put("trxType", "REFUND");
		}
		
		logger.info("trxType: {}",requestMap.getString("trxType"));
		logger.info("cmd    : {}",requestMap.getString("cmd"));
		logger.info("mbrNo  : {}",requestMap.getString("mbrNo"));
		logger.info("mbrRefNo: {}",requestMap.getString("mbrRefNo"));
		logger.info("tmnId  : {}",requestMap.getString("tmnId"));
		logger.info("refNo  : {}",requestMap.getString("refNo"));
		logger.info("applNo : {}",requestMap.getString("applNo"));
		logger.info("cardNo : {}",requestMap.getString("cardNo"));
		logger.info("orgRefNo : {}",requestMap.getString("orgRefNo"));
		logger.info("orgTranDate : {}",requestMap.getString("orgTranDate"));
		logger.info("vanCatId : {}",requestMap.getString("vanCatId"));
		
		
		if(requestMap.isEquals("paymethod","CASH")){
			logger.info("현금영수증 거래는 거래취급안함.");
			response = "OK";
			setResponse();
			return;
		}
	
		//VAN 조회
		SharedMap<String,Object> vanMap = whDAO.getVanByTmnId(requestMap.getString("tmnId"));
		if(vanMap == null){
			vanMap = whDAO.getVanByVanId(requestMap.getString("mbrNo"));
		}
		
		if(vanMap == null){
			logger.info("등록된 Terminal 정보를 찾을 수 없습니다.");
			sharedMap.put("van", "SPCN");
			response = "Fail|미등록터미널";
			setResponse();
			return;
		}else{
			whMap.put("van", vanMap.getString("van"));
		}
		
		
		whMap.put("orgData"	, sharedMap.getString(PAYUNIT.PAYLOAD));	//원본 데이터 
		whMap.put("trxId"	, sharedMap.getString("trxId"));			//새로 채번된 거래번호
		whMap.put("tmnId"	, requestMap.getString("tmnId"));			//전달된 터미널 ID
		whMap.put("trxType"	, requestMap.getString("trxType"));
		whMap.put("reqData"	, requestMap.toJson());
		whMap.put("vanId"	, requestMap.getString("mbrNo"));
		whMap.put("vanTrxId", requestMap.getString("refNo"));
		
		
		if(!vanMap.isEquals("secondKey","OFF")){
			logger.info("ONLINE 통지 거래는 처리안함");
			response = "OK";
			setResponse();
			return;
		}
		
		
		
		
		
		
		if(requestMap.getString("trxType").equals("REFUND")){
			requestMap.put("amount", -requestMap.getLong("amount"));
		}else{
			requestMap.put("amount", requestMap.getLong("amount"));
		}
		logger.info("van    : {}",whMap.getString("van"));
		
		//VAN 정보만을 가져왔을 경우 다시 가져온다.
		if(vanMap.isNullOrSpace("tmnId")){
			vanMap = whDAO.getVanByTmnId(requestMap.getString("tmnId"));
		}
		
		logger.info("trxType: {}",requestMap.getString("trxType"));
		logger.info("cmd    : {}",requestMap.getString("cmd"));
		logger.info("mbrNo  : {}",requestMap.getString("mbrNo"));
		logger.info("mbrRefNo: {}",requestMap.getString("mbrRefNo"));
		logger.info("tmnId  : {}",requestMap.getString("tmnId"));
		logger.info("refNo  : {}",requestMap.getString("refNo"));
		logger.info("applNo : {}",requestMap.getString("applNo"));
		logger.info("cardNo : {}",requestMap.getString("cardNo"));
		logger.info("orgRefNo : {}",requestMap.getString("orgRefNo"));
		logger.info("orgTranDate : {}",requestMap.getString("orgTranDate"));
		logger.info("vanCatId : {}",requestMap.getString("vanCatId"));
		
		
		//DB 처리할 SharedMap 작성
		sharedMap.put("trxType"		, "WHTR");
		sharedMap.put("van"			, whMap.getString("van"));
		sharedMap.put("vanId"		, requestMap.getString("mbrNo"));
		sharedMap.put("trackId" 	, requestMap.getString("mbrRefNo"));
		sharedMap.put("vanTrxId"	, requestMap.getString("refNo"));
		sharedMap.put("vanResultCd"	, "0000");
		sharedMap.put("vanResultMsg", "정상");
		sharedMap.put("amount"		, requestMap.getString("amount"));
		sharedMap.put("regDate"		, "20"+requestMap.getString("tranDate")+requestMap.getString("tranTime"));
		sharedMap.put("payerName" 	, requestMap.getString("customerName"));
		sharedMap.put("payerEmail" 	, requestMap.getString("customerEmail"));
		sharedMap.put("payerTel" 	, requestMap.getString("customerTelNo"));
		
		
		if(requestMap.isEquals("trxType", "REFUND")){
			sharedMap.put("vanRootTrxId", requestMap.getString("orgRefNo"));
			sharedMap.put("rootTrnDay"	, "20"+requestMap.getString("orgTranDate"));
			if(sharedMap.isEquals("rootTrnDay", sharedMap.getString("regDate").substring(0,8))){
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
		response = "OK";
		
		
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
			sharedMap.put("authCd", CommonUtil.nToB(requestMap.getString("applNo")));	
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
		if(requestMap.isNullOrSpace("installNo")){
			sharedMap.put("installment","00");
		}else{
			sharedMap.put("installment",requestMap.getString("installNo"));
		}
		
		// 취소거래의 경우 원거래가 있는지 확인한다.
		if(requestMap.getString("trxType").equals("REFUND")) {
			rootTrxMap = whDAO.getTrxPayByVanTrxId(sharedMap.getString("rootTrnDay"),sharedMap.getString("van"), sharedMap.getString("vanRootTrxId"));
			if(rootTrxMap == null){
				logger.info("원거래를 찾을 수 없습니다. rootTrnDay : {}, rootTrxId: {}", sharedMap.getString("rootTrnDay"), sharedMap.getString("vanRootTrxId"));
				resMsg = "취소원거래를 찾을 수 없습니다. rootTrnDay : "+ sharedMap.getString("rootTrnDay") + ", rootTrackId:" + sharedMap.getString("vanRootTrxId");
				return false;
				
			}
			sharedMap.put("rootTrnDay",rootTrxMap.getString("regDay"));
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
			
			//1이면 전액, 2이면  부분 
			logger.info("SPNC 취소구분: {}",requestMap.getString("cmd"));
			
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
			

			sharedMap.put("bin"		, requestMap.getString("cardNo").substring(0,6));
			sharedMap.put("last4"	, requestMap.getString("cardNo").substring(requestMap.getString("cardNo").length()-4));
			
			sharedMap.put("cardId"	, sharedMap.getString(PAYUNIT.KEY_CARD));
			
			/*
			SharedMap<String,Object> binMap = whDAO.getDBIssuer(sharedMap.getString("bin"));
			
			sharedMap.put("issuer"	, binMap.getString("issuer"));
			sharedMap.put("acquirer", binMap.getString("acquirer"));
			sharedMap.put("cardType", binMap.getString("cardType"));
			*/
			
			sharedMap.put("issuer"	, getIssuerName(requestMap.getString("issueCompanyNo")));
			sharedMap.put("acquirer", getAcquirer(requestMap.getString("acqCompanyNo")));
			sharedMap.put("cardType", "신용");
			
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
		if(retry){
			VertXMessage.set200(rc, "text/html",response, "");
		}else{
			
			SharedMap<String,Object> responseMap = new SharedMap<String,Object>();
			if(whMap.like("resData","중복거래") || whMap.like("resData","기취소") ||  whMap.like("resData","미등록")){
				responseMap.put("resultCode","0000");
				VertXMessage.set200(rc, "application/json",GsonUtil.toJson(responseMap), "");
				
			}else{
				if(response.equals("OK")){
					responseMap.put("resultCode","0000");
				}else{
					responseMap.put("resultCode","9999");
					responseMap.put("message",response);
				}
				VertXMessage.set200(rc, "application/json",GsonUtil.toJson(responseMap), "");
			}
		}
		logger.info("estimatedTime : {}", TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));
		
		// WeebHook 정보를 저장.
		if(retry) {
			if(response.equals("OK")){
				whMap.put("terminal","O");
			}
			whDAO.updateTrxWH(whMap);
		} else {
			whDAO.insertTrxWH(whMap);
		}
		
		sharedMap = null;
	}
	
	public void parseQueryString(String str){
		//logger.debug("SMARTRO : [{}]", str);
		requestMap = new SharedMap<String,Object>();
		String[] st = str.split("&");

		for (int i = 0; i < st.length; i++) {
			int index = st[i].indexOf('=');
			if (index > 0){
				String key = st[i].substring(0, index);
				requestMap.put(key, changeCharset(urlDecode(st[i].substring(index + 1)),"utf-8"));
				logger.info("DATAS : {},[{}]",key,requestMap.getString(key));
			}
		}

	}
	
	
	 public String changeCharset(String str, String charset) {
	        try {
	            byte[] bytes = str.getBytes(charset);
	            return new String(bytes, charset);
	        } catch(UnsupportedEncodingException e) { }//Exception
	        return "";
	    }
	
	
	

	/*
	 *  urlDecode
	 */
	public String urlDecode(Object obj) {
		if (obj == null)
			return null;

		try {
			return URLDecoder.decode(obj.toString(), "UTF-8");
		} catch (Exception e) {
			return obj.toString();
		}
	}
	
	
	
	protected void setError() {
		VertXMessage.set500(rc);
		sharedMap = null;
	}
	
	
	public String getIssuerName(String cd){
		if(cd.equals("01")){
			return "비씨카드";
		} else if(cd.equals("02")) {
			return "신한카드";
		} else if(cd.equals("03")) {
			return "삼성카드";
		} else if(cd.equals("04")) {
			return "현대카드";
		} else if(cd.equals("05")) {
			return "롯데카드";
		} else if(cd.equals("06")) {
			return "해외JCB";
		} else if(cd.equals("07")) {
			return "국민카드";
		} else if(cd.equals("08")) {
			return "하나카드";
		} else if(cd.equals("09")) {
			return "해외카드";
		} else if(cd.equals("11")) {
			return "수협카드";
		} else if(cd.equals("12")) {
			return "농협카드";
		} else if(cd.equals("13")) {
			return "한미카드";
		} else if(cd.equals("15")) {
			return "씨티카드";
		} else if(cd.equals("21")) {
			return "신한카드";
		} else if(cd.equals("22")) {
			return "제주카드";
		} else if(cd.equals("23")) {
			return "광주카드";
		} else if(cd.equals("24")) {
			return "전북카드";
		} else if(cd.equals("26")) {
			return "신협카드";
		} else if(cd.equals("27")) {
			return "하나카드";
		} else{
			return "기타";
		}
	}
	
	
	public String getAcquirer(String acq){
		if(acq.equals("01")){
			return "비씨";
		}else if(acq.equals("02")){
			return "신한";
		}else if(acq.equals("03")){
			return "삼성";
		}else if(acq.equals("04")){
			return "현대";
		}else if(acq.equals("05")){
			return "롯데";
		}else if(acq.equals("07")){
			return "국민";
		}else if(acq.equals("08")){
			return "하나";
		}else if(acq.equals("12")){
			return "농협";
		}else{
			return "기타";
		}
	}

}
