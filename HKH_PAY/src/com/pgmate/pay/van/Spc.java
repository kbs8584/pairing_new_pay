package com.pgmate.pay.van;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.util.comm.UrlClient;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.bean.Result;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;





/**
 * @author Administrator
 *
 */
public class Spc implements Van {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.Spc.class ); 
	public static final String CONTENTS_JSON	= "application/json";
	public static final String METHOD_POST		= "POST";
	static final String SPC_CREDIT_URL 		= "https://relay.mainpay.co.kr%s";
	static final int SPC_CONNECT_TIMEOUT 	= 5000;
	static final int SPC_TIMEOUT 			= 30000;
	
	
	static final String ERC_NETWORK_ERROR 	= "-1";
	static final String ERM_NETWORK 		= "Network Error";
	static final String CHARSET 			= "UTF-8";
	
	
	private String CPID 					= "";
	private String CRYPTOKEY				= "";
	private String VAN						= "";
	private String SPC_URL 			= "";
	/*
	static {
	    disableSslVerification();
	}*/
	
	public Spc(){
		
	}
	
	public Spc(SharedMap<String, Object> vanMap) {
		CPID =  vanMap.getString("vanId").trim();
		CRYPTOKEY	= vanMap.getString("cryptoKey").trim();
		VAN	= vanMap.getString("van").trim();
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,Response response) {
		
		SPC_URL = "/v1/api/payments/payment/card-keyin/trans";
		
		String item = "";
		try{
			if(response.pay.products != null && response.pay.products.size() > 0){
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
			}
		}catch(Exception e){}
		
		HashMap<String,String> req = new HashMap<String,String>();
		
		// 결제 정보
		req.put("mbrNo", CPID);
		req.put("mbrRefNo", response.pay.trxId);
		
		req.put("cardNo", response.pay.card.number);
		req.put("expd", response.pay.card.expiry);
		req.put("amount", CommonUtil.toString(response.pay.amount));
		req.put("taxAmt", "0");
		req.put("feeAmt", "0");
		req.put("installment", CommonUtil.zerofill(response.pay.card.installment, 2));
		req.put("goodsName", item);
		req.put("customerName", CommonUtil.nToB(response.pay.payerName,"주식회사굿섬"));
		req.put("customerTelNo", CommonUtil.nToB(response.pay.payerTel,"0216701915"));
		
		
		String timestmp = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		
		String data = String.format("%s|%s|%s|%s|%s", CPID,response.pay.trxId,CommonUtil.toString(response.pay.amount),CRYPTOKEY,timestmp);
		String hashData = "";
		try {
			hashData = hash(data);
		}catch(Exception e) {
			logger.debug("spc, hash Exception  = [{}]",e.toString());
		}
		
		req.put("signature", hashData);
		req.put("clientType", "Online");
		req.put("timestamp", timestmp);
		
		
		if(response.pay.metadata != null){
			if(response.pay.metadata.isTrue("cardAuth")){
				req.put("keyinAuthType", "O");
				req.put("authType", "0");
				req.put("regNo", response.pay.metadata.getString("authDob"));
				req.put("passwd", response.pay.metadata.getString("authPw"));
			}else {
				req.put("keyinAuthType", "K");
			}
			response.pay.metadata = null;
		} 
		
		Map<String, Object> resD = comm(METHOD_POST, SPC_URL ,req);
		/*
		 * 응답값의 Data 부분 처리를 위하여 resData Map 추가
		 * resData map 에 거래 정보가 존재함
		 */
		Map<String, Object> resData = null;
		
		if(CommonUtil.toString(resD.get("resultCode")).equals("200")){
			
			String rstData = resD.get("data").toString();
			resData = fromJson(rstData);
			
			response.pay.authCd = CommonUtil.toString(CommonUtil.parseInt(resData.get("applNo")));
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
			
			sharedMap.put("vanTrxId",CommonUtil.toString(resData.get("refNo")));
			sharedMap.put("vanDate",CommonUtil.toString(resData.get("tranDate"))+CommonUtil.toString(resData.get("tranTime")));
			logger.debug("cardName : {}",CommonUtil.toString(resData.get("issueCompanyName")));
		}else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString(resD.get("resultCode")),"승인실패",CommonUtil.toString(resD.get("resultMessage")));
		}
		
		sharedMap.put("van",VAN);
		sharedMap.put("vanId",CPID);
		sharedMap.put("vanResultCd",CommonUtil.toString(resD.get("resultCode")));
		sharedMap.put("vanResultMsg",CommonUtil.toString(resD.get("resultMessage")));
		
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));
		
		
		return sharedMap;
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		
		SPC_URL = "/v1/api/payments/payment/card-keyin/cancel";
		
		HashMap<String,String> req = new HashMap<String,String>();
		
		logger.info("rootTrxId : [{}]", payMap.getString("vanTrxId"));
		
		req.put("mbrNo", CPID);
		req.put("mbrRefNo", response.refund.trxId);
		req.put("orgRefNo", payMap.getString("vanTrxId"));
		req.put("orgTranDate", CommonUtil.toString(payMap.getString("regDay").getBytes(), 2));
		req.put("amount", CommonUtil.toString(response.refund.amount));
		req.put("payType", "Keyin");
		
		String timestmp = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		
		String data = String.format("%s|%s|%s|%s|%s", CPID,response.refund.trxId,CommonUtil.toString(response.refund.amount),CRYPTOKEY,timestmp);
		String hashData = "";
		try {
			hashData = hash(data);
		}catch(Exception e) {
			logger.debug("spc, hash Exception  = [{}]",e.toString());
		}
		
		req.put("signature", hashData);
		req.put("timestamp", timestmp);
		
		Map<String, Object> resD = comm(METHOD_POST, SPC_URL ,req);
		Map<String, Object> resData = null;
		
		if(CommonUtil.toString(resD.get("resultCode")).equals("200")){
			
			String rstData = resD.get("data").toString();
			resData = fromJson(rstData);
			
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
			response.refund.authCd = CommonUtil.toString(CommonUtil.parseInt(resData.get("applNo")));
		}else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString(resD.get("resultCode")),"취소실패",CommonUtil.toString(resD.get("resultMessage")));
		}
		sharedMap.put("van",payMap.getString("van"));
		sharedMap.put("vanId",CPID);
		sharedMap.put("vanTrxId",CommonUtil.toString(resData.get("refNo")));
		sharedMap.put("vanResultCd",CommonUtil.toString(resD.get("resultCode")));
		sharedMap.put("vanResultMsg",CommonUtil.toString(resD.get("resultMessage")));
		sharedMap.put("vanDate",CommonUtil.toString(resData.get("tranDate"))+CommonUtil.toString(resData.get("tranTime")));	
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId\"),sharedMap.getString(\"vanDate\"));\n" + 
				"		logger.debug(\"cardName : {}\",CommonUtil.toString(resD.get(\"data\""),sharedMap.getString("vanDate"));

		return sharedMap;
	}
	
	/*
	 * 미사용 connection       
	 */
	public Map<String, Object> connect(HashMap<String,String> data){

		//String req = toJson(data);
		String req = toQueryString(data);
		logger.info("send : [{}]",req);
		long time = System.currentTimeMillis();
		String message = "";
		UrlClient client = null;
		
		Map<String, Object> retMap = null;
		
		try {
			client = new UrlClient(SPC_URL, "POST", "application/x-www-form-urlencoded; charset=utf-8");
			client.setTimeout(SPC_CONNECT_TIMEOUT,SPC_TIMEOUT);
			client.setDoInputOutput(true, true);
			
			message = client.connect(req);
			
			logger.debug("recv : [{}]",message);
			
			//resData = fromJson(message.toString().trim());
			retMap = fromJson(message);
			
		} catch(Exception e) {
			message = "NOTCONNECTED";
			
			retMap.put("resultCode", "XXXX");
			retMap.put("resultMessage", message);
			logger.debug("spc,error : {}",e.getMessage());
		}finally{
			logger.debug("spc Elasped Time =[{} sec]",CommonUtil.parseDouble((System.currentTimeMillis()-time)/1000) );
			logger.debug("spc, res result code = [{}] , result msg = [{}]",retMap.get("resultCode"), retMap.get("resultMessage"));
			logger.debug("spc, res data = [{}] ",CommonUtil.toString(retMap));
		}
		return retMap;
	}
	
	
	/*
	 * send 시 web queryString 방식으로 처리
	 * recv 시 Json 방식으로 처리
	 */
	public Map<String, Object> comm(String method,String paymentUri,HashMap<String,String> reqObj){
		Map<String, Object> retMap = null;
		//String request = toJson(reqObj);
		String request = toQueryString(reqObj);
		
		
		StringBuilder resBuf = new StringBuilder();
		
		URL url = null;
		HttpsURLConnection conn = null;
		int code = 0;
		logger.debug("URL : ["+String.format(SPC_CREDIT_URL,SPC_URL)+"]");
		// JDK에 따라서 TLSv1.0,TLSv1.1,TLSv1.2 를 사용하시기 바랍니다. 
		
		
		long time = System.currentTimeMillis();
		try {
			logger.info("\"LOCAL >> SPC  [{}]",request);
			url = new URL(String.format(SPC_CREDIT_URL,paymentUri));
			conn = (HttpsURLConnection) url.openConnection();
			
			conn.setRequestMethod(method);
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(60000);
			
			
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			//conn.setRequestProperty("Authorization", payKey);
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

			retMap = fromJson(resBuf.toString().trim());
			
		} catch(Exception e) {
			logger.info("PAYMENT URL REQUEST ERROR =["+e.getMessage()+"]"+String.format(SPC_CREDIT_URL,paymentUri));
			Result result = new Result();
			result.resultCd = "XXXX";
			result.resultMsg ="통신장애 또는 시스템 장애 :"+code;
			result.advanceMsg = e.getMessage();
			result.create = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
			retMap.put("resultCode", "XXXX");
			retMap.put("resultMessage", result.resultMsg);
			
		}finally{
			logger.info("ElapsedTime : "+(long)(System.currentTimeMillis()-time)+"msec");
			logger.info("LOCAL << SPC ["+resBuf.toString()+"]");
			conn.disconnect();
		}
		return retMap;
		
	}
	
	public String toQueryString(HashMap<String,String> data){
		StringBuilder sb = new StringBuilder();
		for( String key : data.keySet() ){
			sb.append(key+"="+urlEncode(changeCharset(CommonUtil.nToB(data.get(key)),"utf-8")));
			sb.append("&");
	    }
		if (sb.length() > 0){
			return sb.substring(0, sb.length() - 1);
		} else {
			return "";
		}
	}
	
	public String changeCharset(String str, String charset) {
        try {
            byte[] bytes = str.getBytes(charset);
            return new String(bytes, charset);
        } catch(UnsupportedEncodingException e) { }//Exception
        return "";
    }
	
	public String toJson(Object src){
		if(src == null){ return "";}
		return new GsonBuilder().setPrettyPrinting().create().toJson(src);
	}
	
	
	
	public Map<String, Object> fromJson(String json){
		Map<String, Object> obj = null;
		
		try{
			//obj = new GsonBuilder().create().fromJson(json, SpcResponse.class);
			obj = new Gson().fromJson(json, new TypeToken<HashMap<String, Object>>() {}.getType());
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return obj;
	}
	
	
	
	
	public String urlEncode(Object obj) {
		if (obj == null)
			return null;

		try {
			return URLEncoder.encode(obj.toString(), CHARSET);
		} catch (Exception e) {
			return obj.toString();
		}
	}

	/*
	 *  urlDecode
	 */
	public String urlDecode(Object obj) {
		if (obj == null)
			return null;

		try {
			return URLDecoder.decode(obj.toString(),CHARSET);
		} catch (Exception e) {
			return obj.toString();
		}
	}

	
	 private static void disableSslVerification() {
		try
		{
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
				}
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
				}
			};
		
			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			
			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
			    public boolean verify(String hostname, SSLSession session) {
			        return true;
			    }
			};
			
			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}
	 
	 
	 private static String hash(String data) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(data.getBytes("UTF-8"));
		byte[] hashbytes = md.digest();

		StringBuilder sbuilder = new StringBuilder();
		for(int i=0 ; i<hashbytes.length ; i++){

			sbuilder.append(String.format("%02x", hashbytes[i] & 0xff));
		}
		return sbuilder.toString();
	}

}
