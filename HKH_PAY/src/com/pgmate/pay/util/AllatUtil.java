package com.pgmate.pay.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.dao.TrxDAO;

/**
 * @author Administrator
 *
 */
public class AllatUtil {


	public static String ALLAT_RECEIPT = "http://www.allatpay.com/servlet/AllatBizPop/member/pop_card_receipt.jsp";
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.util.AllatUtil.class ); 
	
	public AllatUtil() {
	}
	
	
	public String getCard(String vanId,String cryptoKey,String trackId,String amount){
		long epochTime 	= System.currentTimeMillis();
		String hash 		= getHash(vanId+cryptoKey+trackId+amount+epochTime);	
		String value = "shop_id="+vanId+"&order_number="+trackId+"&hash_value="+hash+"&current_time="+epochTime;
		return connect(value);
	}
	
	
	public SharedMap<String,Object> getCard(SharedMap<String,Object> sharedMap){
		SharedMap<String,Object> vanMap= new TrxDAO().getVanByVanId("ALLAT",sharedMap.getString("shop_id"));
		
		long epochTime 	= System.currentTimeMillis();
		String hash 		= getHash(sharedMap.getString("shop_id")+vanMap.getString("cryptoKey")+sharedMap.getString("order_no")+sharedMap.getString("amt")+epochTime);	
		String value = "shop_id="+sharedMap.getString("shop_id")+"&order_number="+sharedMap.getString("order_no")+"&hash_value="+hash+"&current_time="+epochTime;
		sharedMap.put("card_no", connect(value));
		
		return sharedMap;
	
	}
	
	private String getHash(String text){
		StringBuffer buf = new StringBuffer();
		try{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(text.getBytes("euc-kr"));
			byte[] digest = md.digest();
			
			for( int i = 0; i < digest.length; i++ ){
				if((0xff & digest[i]) < 0x10)
					buf.append("0" + Integer.toHexString(0xff & digest[i]));
				else
					buf.append(Integer.toHexString(0xff & digest[i]));
			}

			
		}catch(Exception e){
		}
		return buf.toString();
	}
	
	
	private String connect(String msg){
		long time = System.currentTimeMillis();
		String card = "444444xxxxxx4444";
		StringBuffer result = new StringBuffer();
		URL url = null;
		HttpURLConnection conn = null;
		
		try {
			
			url = new URL(AllatUtil.ALLAT_RECEIPT);
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(10000);
			
			byte[] reqbuf = msg.getBytes(Charset.forName("euc-kr"));
			
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Connection", "close");
			conn.setRequestProperty("Content-length", CommonUtil.toString(reqbuf.length));
			
			OutputStream os = conn.getOutputStream();
			os.write(reqbuf);
			os.flush();
			os.close();
			
			int httpCode = conn.getResponseCode();
			BufferedReader in = null;
			
			if(httpCode ==200){
				in = new BufferedReader(new InputStreamReader( conn.getInputStream(),"euc-kr"));
			}else{
				in = new BufferedReader(new InputStreamReader( conn.getErrorStream(),"euc-kr"));
				result.append("NETWORKERROR:");
			}
			
			String line;
			while ((line = in.readLine()) != null){
				result.append(line);
			}
			
			in.close();
			
			Document doc = Jsoup.parse(result.toString());
			Element div = doc.select("div.receipt_div1").first();
			if(div != null){
				String number = CommonUtil.leftTrim(div.html().split("<br>")[1].replaceAll("-", "").trim());
				if(number.length() > 14){
					card = number;
				}
			}
		
			
		} catch(Exception e) {
			result.append("NOTCONNECTED "+e.getMessage());
		}finally{
			logger.debug("allat Elasped Time =[{} sec]",CommonUtil.parseDouble((System.currentTimeMillis()-time)/1000) );
			//logger.debug("allat, res = [{}]",result.toString());
			conn.disconnect();
		}
		
		
		return card;
	}
	
	
	
	
	public static void main(String[] args){
		AllatUtil a = new AllatUtil();
		String allatTrackId = "";
		String shopId		= "";
		String amount = "39000";
		long epochTime 	= System.currentTimeMillis();
		
		System.out.println("card_no="+a.getCard(shopId, "f66715c9ae8a611716f534f753802439", allatTrackId, amount));
		
	}

}
