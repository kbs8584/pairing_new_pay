package com.pgmate.pay.van;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Refund;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.bean.Result;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;




/**
 * @author Administrator
 *
 */
public class Pairing implements Van {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.Pairing.class ); 
	public static final String CONTENTS_JSON	= "application/json";
	public static final String METHOD_POST		= "POST";
	public static final String METHOD_GET		= "GET";
	public String C3_URL			= "https://api-dev.pairingpayments.nett/api/%s";
	public String van 		= "";
	public String vanId		= "";
	public String payKey	= "";
	
	
	public Pairing(SharedMap<String, Object> tmnVanMap) {
		van		= tmnVanMap.getString("van").trim();
		vanId =  tmnVanMap.getString("vanId").trim();
		payKey	= tmnVanMap.getString("cryptoKey").trim();
		
		if(tmnVanMap.startsWith("van","KWON")){
			C3_URL			= "https://api-dev.pairingpayments.nett/api/%s";
		}else if(tmnVanMap.startsWith("van","PAIRING")){
			C3_URL			= "https://api-dev.pairingpayments.nett/api/%s";
		}
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,Response response) {
		
		Request request = new Request();
		Pay pay = new Pay();
		pay.trxType		= "ONTR";								//고정값
		pay.tmnId		= vanId;								//필수값아님. 터미널아이디 : PAIRING 에서 지정한 터미널 아이디
		pay.trackId		= response.pay.trxId;					//주문번호
		pay.amount		= response.pay.amount;					//거래금액
		pay.udf1		= response.pay.tmnId;					//가맹점 정의영역1
		pay.udf2		= response.pay.trackId;					//가맹점 정의영역2
		pay.payerName	= response.pay.payerName;				//결제자 이름
		pay.payerEmail	= response.pay.payerEmail;				//결제자 이메일
		pay.payerTel	= response.pay.payerTel;				//결제자 전화번호
		
		pay.card		= new Card();					
		pay.card.number = response.pay.card.number;				//카드번호 4242424242424242 는 테스트승인 카드
		pay.card.expiry = response.pay.card.expiry;				//유효기간 YYMM
		pay.card.installment = response.pay.card.installment;	//할부기간 
		
		//상품 수량 또는 종류에 따라 다중 입력 단. 1개의 상품은 반드시 입력 바람.
		pay.products 	= response.pay.products;
		pay.metadata  	= response.pay.metadata;
		
		request.pay = pay;
		
		Response res =  comm(METHOD_POST,"pay",request);
		
		if(res.result.resultCd.equals("XXXX")){
			try{Thread.sleep(1500);}catch(Exception e){}
			res =  comm(METHOD_GET,"get/"+response.pay.trxId,null);
		}
		
		
		response.result = res.result;
		response.pay.authCd = res.pay.authCd;
		logger.info("pairing result : {},{},{}",res.result.resultCd,res.result.resultMsg,res.result.advanceMsg);
		sharedMap.put("van",van);
		sharedMap.put("vanId",vanId);
		sharedMap.put("vanTrxId",res.pay.trxId);
		sharedMap.put("vanResultCd",res.result.resultCd);
		sharedMap.put("vanResultMsg",res.result.resultMsg);	
		sharedMap.put("vanDate",res.result.create);	
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),res.result.create);
		
		

		
		return sharedMap;
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		
		Request request = new Request();
		Refund refund = new Refund();
		
		refund.trxType		= "ONTR";
		refund.tmnId			= vanId;
		refund.trackId		= response.refund.trxId;
		refund.amount		= response.refund.amount;
		refund.rootTrxId 	= payMap.getString("vanTrxId");
		request.refund = refund;
		Response res = comm(METHOD_POST,"refund",request);
		if(res.result.resultCd.equals("0000") || res.result.resultMsg.equals("기취소오류")){
			response.refund.authCd = payMap.getString("authCd");
			if(CommonUtil.isNullOrSpace(response.refund.authCd)){
				response.refund.authCd = payMap.getString("authCd");
			}
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		}else{
			response.result = res.result;
			response.result.create = "yyyyMMddHHmmss";
		}
	
		logger.info("pairing result : {},{},{}",res.result.resultCd,res.result.resultMsg,res.result.advanceMsg);
		sharedMap.put("van",payMap.getString("van"));
		sharedMap.put("vanId",vanId);
		sharedMap.put("vanTrxId",res.refund.trxId);
		sharedMap.put("vanResultCd",res.result.resultCd);
		sharedMap.put("vanResultMsg",res.result.resultMsg);	
		sharedMap.put("vanDate",res.result.create);	
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),res.result.create);

		return sharedMap;
	}
	
	
	public Response comm(String method,String paymentUri,Request reqObj){
		Response response = null;
		String request = toJson(reqObj);
		
		
		StringBuilder resBuf = new StringBuilder();
		
		URL url = null;
		HttpsURLConnection conn = null;
		int code = 0;
		logger.debug("URL : ["+String.format(C3_URL,paymentUri)+"]");
		// JDK에 따라서 TLSv1.0,TLSv1.1,TLSv1.2 를 사용하시기 바랍니다. 
		
		
		long time = System.currentTimeMillis();
		try {
			System.out.println("LOCAL >> CRX ["+request+"]");
			url = new URL(String.format(C3_URL,paymentUri));
			conn = (HttpsURLConnection) url.openConnection();
			
			conn.setRequestMethod(method);
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(60000);
			
			
			conn.setRequestProperty("Content-Type", CONTENTS_JSON);
			conn.setRequestProperty("Authorization", payKey);
			conn.setRequestProperty("Connection", "close");
			
			
			OutputStream os = conn.getOutputStream();
			os.write(request.getBytes("utf-8"));
			os.flush();
			os.close();
			
			code = conn.getResponseCode();
			
			InputStream inputStream = null;
			if(code == HttpsURLConnection.HTTP_OK){
				inputStream = conn.getInputStream();
			}else{
				inputStream = conn.getErrorStream();
			}
			
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			int bcount = 0;
			byte[] buf = new byte[2048];
			int read_retry_count = 0;
			while(true) {
				int n = inputStream.read(buf);
			    if ( n > 0 ) { bcount += n; bout.write(buf,0,n); }
			    else if (n == -1) break;
			    else  { // n == 0
			if (++read_retry_count >= 5)
			  throw new IOException("inputstream-read-retry-count(5) exceed !");
			    }
			    if(inputStream.available() == 0){ break; }
			}
			bout.flush();
			byte[] res = bout.toByteArray();
			bout.close();
			
			if(res!=null){
				resBuf.append(new String(res));
			}

			response = fromJson(resBuf.toString().trim());
			
		} catch(Exception e) {
			logger.info("PAYMENT URL REQUEST ERROR =["+e.getMessage()+"]"+String.format(C3_URL,paymentUri));
			response = new Response();
			Result result = new Result();
			result.resultCd = "XXXX";
			result.resultMsg ="통신장애 또는 시스템 장애 :"+code;
			result.advanceMsg = e.getMessage();
			result.create = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
			response.result = result;
			response.pay = reqObj.pay;
			
		}finally{
			logger.info("ElapsedTime : "+(long)(System.currentTimeMillis()-time)+"msec");
			logger.info("LOCAL << CRX ["+resBuf.toString()+"]");
			conn.disconnect();
		}
		return response;
		
	}
	
	
	public String toJson(Object src){
		if(src == null){ return "";}
		return new GsonBuilder().setPrettyPrinting().create().toJson(src);
	}
	
	
	
	public Response fromJson(String json){
		Response obj = null;
		try{
			obj = new GsonBuilder().create().fromJson(json, Response.class);
		}catch(Exception e){
			e.printStackTrace();
		}
		return obj;
	}
	
	

}
