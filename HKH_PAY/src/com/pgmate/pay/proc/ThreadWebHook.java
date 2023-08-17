package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;

/**
 * @author Administrator
 *
 */
public class ThreadWebHook extends Thread {

	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ThreadWebHook.class );
	private Response response	= null;
	private TrxDAO trxDAO 	= null;
	private String webhookUrl			= "";
	
	
	public ThreadWebHook(String webhookUrl,Response response) {
		this.webhookUrl = webhookUrl;
		this.response 	= response;
		this.trxDAO 	= new TrxDAO();
		
	}
	
	
	public void run(){
		
		logger.info("WebHook   : {}",webhookUrl);
	
		SharedMap<String,Object> ntsMap = new SharedMap<String,Object>();	
		if(response.pay != null){
			ntsMap.put("trxId"		, response.pay.trxId);
			ntsMap.put("trxType"	, "pay");
			ntsMap.put("tmnId"		, response.pay.tmnId);
			ntsMap.put("trackId"	, response.pay.trackId);
			
		}else if(response.billPay != null){
			ntsMap.put("trxId"		, response.billPay.trxId);
			ntsMap.put("trxType"	, "bill");
			ntsMap.put("tmnId"		, response.billPay.tmnId);
			ntsMap.put("trackId"	, response.billPay.trackId);
			
		}else if(response.refund != null){
			ntsMap.put("trxId"		, response.refund.trxId);
			ntsMap.put("trxType"	, "refund");
			ntsMap.put("tmnId"		, response.refund.tmnId);
			ntsMap.put("trackId"	, response.refund.trackId);
		}else if(response.auth != null){
			ntsMap.put("trxId"		, response.auth.trxId);
			ntsMap.put("trxType"	, "auth");
			ntsMap.put("tmnId"		, response.auth.tmnId);
			ntsMap.put("trackId"	, response.auth.trackId);
		}else {}
		
		ntsMap.put("webHookUrl"	, webhookUrl);
		ntsMap.put("retry"		, 0);
		ntsMap.put("status"		, "대기");
		ntsMap.put("regDay"		, CommonUtil.getCurrentDate("yyyyMMdd"));
		ntsMap.put("regTime"	, CommonUtil.getCurrentDate("HHmmss"));
		ntsMap.put("payLoad"	, GsonUtil.toJsonExcludeStrategies(response,true));
		
		//20181114 변경 INSERT만 하는 것으로 
		ntsMap.put("resData", "");
		ntsMap.put("status"		, "전송실패");
		ntsMap.put("code", 0);
		ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
		trxDAO.insertTrxNTS(ntsMap);
		
		
		/*
		long time = System.currentTimeMillis();
		try {
			logger.info("WEBHOOK START");
			logger.info("trxId       : {}",ntsMap.getString("trxId"));
			
			String contentType = "application/x-www-form-urlencoded";
			
			
			UrlClient client = new UrlClient(ntsMap.getString("webHookUrl"), "POST", contentType);
			client.setTimeout(30000,30000);
			client.setDoInputOutput(true, true);
			String payload = "response="+URLEncoder.encode(ntsMap.getString("payLoad"),"UTF-8");
			ntsMap.put("resData", CommonUtil.cut(client.connect(payload),512));
			ntsMap.put("code", client.getHttpCode());
		
			ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
			if(ntsMap.getString("resData").indexOf("OK") > -1 || ntsMap.getString("resData").indexOf("result=0000") > -1 || client.getHttpCode() == 200){
				ntsMap.put("status"		, "전송완료");
			}else{
				ntsMap.put("status"		, "전송실패");
			}
			
		} catch(Exception e) {
			logger.info("WH MERCHANT URL REQUEST ERROR =["+e.getMessage()+"]");
			ntsMap.put("status","전송실패");
			ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
		}finally{
			logger.info("WH MERCHANT THREAD MERCHANT RESPONSE : [{}]"+CommonUtil.cut(ntsMap.getString("resData"),100)+"]");
			logger.info("WH MERCHANT THREAD Elasped Time : [{}]",(System.currentTimeMillis()-time)/1000);
			trxDAO.insertTrxNTS(ntsMap);
		}	*/
	}
	
	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
