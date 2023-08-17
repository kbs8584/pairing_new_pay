package com.pgmate.pay.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.bean.Result;

/**
 * @author Administrator
 *
 */
public class APICall {
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.util.APICall.class ); 
	/**
	 * 
	 */
	public APICall() {
		// TODO Auto-generated constructor stub
	}
	
public Response comm(Request reqObj,String payKey, String C3URL){
		
		
		Response response = null;
		String reqJson = GsonUtil.toJson(reqObj);
		
		
		StringBuilder resBuf = new StringBuilder();
		
		URL url = null;
		HttpURLConnection conn = null;
		int code = 0;
		
		
		long time = System.currentTimeMillis();
		try {
			logger.info("BILL >> C3API ["+reqJson+"]");
			url = new URL(C3URL);
			conn = (HttpURLConnection)url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(60000);
			
			
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Authorization", payKey);
			conn.setRequestProperty("Connection", "close");
			
			
			OutputStream os = conn.getOutputStream();
			os.write(reqJson.getBytes("utf-8"));
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

			response = (Response)GsonUtil.fromJson(resBuf.toString().trim(),Response.class);
			
		} catch(Exception e) {
			logger.info("PAYMENT URL REQUEST ERROR =["+e.getMessage()+"]"+C3URL);
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
			logger.info("BILL << C3API ["+resBuf.toString()+"]");
			conn.disconnect();
		}
		return response;
		
	}

}
