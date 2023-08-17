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
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Refund;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;

public class PayTest {
	public static final String CONTENTS_JSON	= "application/json";
	public static final String METHOD_POST		= "POST";
	public static final String METHOD_GET		= "GET"; 
	public static final String METHOD_PUT		= "PUT";
	public static final String PAY_KEY		 	= "xxxx"; //개발테스트
	//public static final String PAY_KEY	= "pk_bbcd-e2d894-955-2a5af";
	
	public PayTest() {
	}
	
	public void executeEcho(){
		Response response = new Response();
		Request request = new Request();
		String reqJson = GsonUtil.toJson(request);
		try{
			
			response = (Response)GsonUtil.fromJson(test("echo",""),Response.class);
			System.out.println(response.toString());
		}catch(Exception e){
			
		}
	}
	
	public void executePay(){
		
		Response response = new Response();
		Request request = new Request();
		Pay pay = new Pay();
		pay.trxId = "";
		pay.trxType = "ONTR";
		pay.tmnId = "TMN000945";
		pay.trackId = "TRK5121542754";
		pay.amount = 1004;
		pay.udf1 = "User Define Field 1";
		pay.udf2 = "User Define Field 2";
		pay.payerName = "홍길동";
		pay.payerEmail = "test@test.com";
		pay.payerTel = "010-4444-2281";

		pay.webhookUrl = "http://xxxx:8080/test/webhook";
		
		pay.card		= new Card();
		pay.card.encTrackI = "acs15892657713bd94f2";
		pay.card.installment = 0;
	
		pay.products 	= new ArrayList<Product>();
		Product product = new Product();
		product.name="테스트";
		product.qty = 1;
		product.price = 5000;
		product.desc = "테스트상품구매";
		pay.products.add(product);
		
		pay.metadata = new SharedMap<String,String>();
		request.pay = pay;
		
		String reqJson = GsonUtil.toJson(request,true,"");
		System.out.println(reqJson);
		//System.out.println(XmlUtil.toXml(request,true,"utf-8"));
		try{
			response = (Response)GsonUtil.fromJson(test("pay",reqJson),Response.class);
			System.out.println(response.toString());
			//executeRefund(response);
		}catch(Exception e){
			
		}
	}
	
	public void executeRefund(Response response){
		
		Request request = new Request();
		Refund refund = new Refund();
		
		refund.trxType		= "ONTR";
		refund.tmnId		= "";
		refund.trackId		= GenKey.genInterMsgKeys("TEST");
		refund.amount		= 5000;
		refund.rootTrxId 	= response.pay.trxId;
		
		
		request.refund = refund;
		
		String reqJson = GsonUtil.toJson(request);
		try{
			response = (Response)GsonUtil.fromJson(test("refund",reqJson),Response.class);
			
		}catch(Exception e){
			
		}
	}
	
	
	public void executeRefund2(Response response){
		
		Request request = new Request();
		Refund refund = new Refund();
		
		refund.trxType		= "ONTR";
		refund.tmnId		= "";
		refund.trackId		= GenKey.genInterMsgKeys("TEST");

		refund.amount		= response.pay.amount;
		refund.rootTrackId	= response.pay.trackId;
		refund.rootTrxDay	= CommonUtil.getCurrentDate("yyyyMMdd");
		
		request.refund = refund;
		
		String reqJson = GsonUtil.toJson(request);
		try{
			response = (Response)GsonUtil.fromJson(test("refund",reqJson),Response.class);
			
		}catch(Exception e){
			
		}
	} 
	
	public void executeRefund3(String trxId){
		
		Request request = new Request();
		Refund refund = new Refund();
		
		refund.trxType		= "ONTR";
		refund.tmnId		= "";
		refund.trackId		= GenKey.genInterMsgKeys("TEST");

		refund.amount		= 1004;
		refund.rootTrxId		= trxId;
		
		request.refund = refund;
		
		String reqJson = GsonUtil.toJson(request);
		try{
			Response response = (Response)GsonUtil.fromJson(test("refund",reqJson),Response.class);
			
		} catch (Exception e) {
			
		}
	} 
	
	
	public void executeGet(String trxId){
		
		
		try{
			Response response = (Response)GsonUtil.fromJson(test("get/"+trxId,""),Response.class);
			
		}catch(Exception e){
			
		}
	} 
		
	public static String test(String paymentUrl,String request){
		paymentUrl = String.format("https://xxxx/api/%s",paymentUrl);
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
			conn.setRequestProperty("Authorization", PayTest.PAY_KEY);
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
	
	
	
	
	
	
	
	public static void main(String[] args){
		
		
		PayTest t = new PayTest();
		//t.executePay();
		t.executeRefund3("T200520324882");
		//t.executeGet("trmsg_201705201122_f26e-cb6d15-9c1-31686");
		/*for(int i = 0; i < 5 ; i++){
			t.executePay();
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println(e.getMessage());
			}
		}*/
	
		//t.executeRefund(null);
		
		/*
		Response response = new Response();
		System.out.println(XmlUtil.toXml(response,true, ""));
		*/
		
	}
}
