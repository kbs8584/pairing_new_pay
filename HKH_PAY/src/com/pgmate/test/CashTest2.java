



package com.pgmate.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.util.xml.XmlUtil;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.Cash;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Refund;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;

/**
 * @author Administrator
 *
 */
public class CashTest2 {

	
	public static final String CONTENTS_JSON	= "application/json";
	public static final String METHOD_POST		= "POST";
	public static final String METHOD_GET		= "GET"; 
	public static final String METHOD_PUT		= "PUT";
	public static final String PAY_KEY		 	= ""; //개발테스트 
	
	public long amount = 1004;
	public String card = "xxxxxxxxxxxx";
	public String expire = "2211";
	
	

	public CashTest2() {
		
		
		
	}
	

	
	
	public void issueReceipt(){
		
		Response response = new Response();
		Request request = new Request();
		Cash cash = new Cash();
		cash.trxType = "CASH";
		cash.tmnId = "";
		cash.trackId	= "TEST_"+CommonUtil.getCurrentDate("HHmmss");
		cash.action ="승인거래";
		cash.identity="xxxxxxxx";
		cash.usage = "소득공제용"; //지출증빙용
		cash.taxType = "과세"; //비과세
		cash.supplyAmt= 913;
		cash.vatAmt = 91;
		cash.serviceAmt = 0;
		cash.amount = 1004;
		cash.custName="임주섭";
		cash.custTel="xxxxxx";
		cash.custEmail="xxxxxx@xxxxxx.net";
		cash.pdtName = "테스트상품";
 			
		request.cash = cash;
		
		String reqJson = GsonUtil.toJson(request,true,"");
		System.out.println(XmlUtil.toXml(request,true,"utf-8"));
		try{
			
			response = (Response)GsonUtil.fromJson(test("cash",reqJson),Response.class);
			System.out.println(response.toString());
			//revokeCash(response);
			
		}catch(Exception e){
			
		}
	}
	
	public void revokeCash(Response response){
		
		Request request = new Request();
		Cash cash = new Cash();
	
		
		cash.trxType		= "CASH";
		cash.trxId			= response.cash.trxId;
		cash.action  		= "취소거래";
		cash.trackId		= "TEST_"+CommonUtil.getCurrentDate("HHmmssS");
		
		
		request.cash = cash;
		
		String reqJson = GsonUtil.toJson(request);
		try{
			response = (Response)GsonUtil.fromJson(test("cash",reqJson),Response.class);
			
		}catch(Exception e){
			
		}
	}
	
	
		
	public static String test(String paymentUrl,String request){
		paymentUrl = String.format("https://WAS/api/%s",paymentUrl);
		StringBuilder result = new StringBuilder();
		URL url = null;
		HttpURLConnection conn = null;
		
		System.setProperty("https.protocols", "TLSv1.2");
		
		long time = System.currentTimeMillis();
		try {
			System.out.println("LOCAL >> PAYMENT ["+request+"]");
			url = new URL(paymentUrl);
			
			
			
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(60000);
			
			
			conn.setRequestProperty("Content-Type", CONTENTS_JSON);
			conn.setRequestProperty("Authorization", CashTest2.PAY_KEY);
			conn.setRequestProperty("Connection", "close");
			
			
			OutputStream os = conn.getOutputStream();
			os.write(request.getBytes("utf-8"));
			os.flush();
			os.close();
			
			
			BufferedReader br = new BufferedReader(new InputStreamReader( conn.getInputStream()));
			
			String line;
			while ((line = br.readLine()) != null)
				result.append(line+"\n");
			
			br.close();

		} catch(Exception e) {
			result.append("CONNECT ERROR ["+e.getMessage()+"] "+paymentUrl);
			System.out.println("PAYMENT URL REQUEST ERROR =["+e.getMessage()+"]");
			
		}finally{
			System.out.println("ElapsedTime : "+(long)(System.currentTimeMillis()-time)+"msec");
			System.out.println("LOCAL << PAYMENT ["+result.toString()+"]");
			conn.disconnect();
		}
		return result.toString();
		
	}
	
	
	public static String test2(String paymentUrl){
		paymentUrl = String.format("https://WAS/api/%s",paymentUrl);
		StringBuilder result = new StringBuilder();
		URL url = null;
		HttpURLConnection conn = null;
		
		System.setProperty("https.protocols", "TLSv1.2");
		
		long time = System.currentTimeMillis();
		try {
	
			url = new URL(paymentUrl);
			
			
			
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(false);
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(60000);
			
			
			conn.setRequestProperty("Content-Type", CONTENTS_JSON);
			//conn.setRequestProperty("Authorization", PayTest.PAY_KEY);
			conn.setRequestProperty("Connection", "close");
			
			
			
			BufferedReader br = new BufferedReader(new InputStreamReader( conn.getInputStream()));
			
			String line;
			while ((line = br.readLine()) != null)
				result.append(line+"\n");
			
			br.close();

		} catch(Exception e) {
			result.append("CONNECT ERROR ["+e.getMessage()+"] "+paymentUrl);
			System.out.println("PAYMENT URL REQUEST ERROR =["+e.getMessage()+"]");
			
		}finally{
			System.out.println("ElapsedTime : "+(long)(System.currentTimeMillis()-time)+"msec");
			System.out.println("LOCAL << PAYMENT ["+result.toString()+"]");
			conn.disconnect();
		}
		return result.toString();
		
	}
	
	
	
	
	public static void main(String[] args){
		//String a = "/api/o/redirect/checkout?cko-payment-token=pay_tok_e7b39c34-ba5f-42e4-96c9-53d0eba1febe";
		//System.out.println(a.indexOf("redirect"));
		
		CashTest2 t = new CashTest2();
		t.issueReceipt();
		
	}

}
