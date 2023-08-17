package com.pgmate.pay.van;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class CashClient {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.van.CashClient.class);

	private SharedMap<String, Object> resMap = new SharedMap<String, Object>();
	private static String host 	= "SYSC3WAS";
	private static int port 	= 19234;
	private static int timeout  = 20000;
	/**
	 * 
	 */
	public CashClient() {
		// TODO Auto-generated constructor stub
	}
	
	
	
	
	public CashInter  comm(CashInter inter){
		
		Socket socket = null;
		OutputStream output = null;
		InputStream input = null;
		String reqJson = GsonUtil.toJson(inter);
		String resJson = "";
		long time = System.currentTimeMillis();
		try{
			socket = new Socket(host, port);
			socket.setSoTimeout(timeout);
			
			output = socket.getOutputStream();
			output.write(reqJson.getBytes("MS949"));
			output.flush();
			
			input = socket.getInputStream();
		
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			int bcount = 0;
			byte[] buf = new byte[2048];
			int read_retry_count = 0;
			while(true) {
				int n = input.read(buf);
			    if ( n > 0 ) { bcount += n; bout.write(buf,0,n); }
			    else if (n == -1) break;
			    else  { // n == 0
			if (++read_retry_count >= 5)
			  throw new IOException("inputstream-read-retry-count(5) exceed !");
			    }
			    if(input.available() == 0){ break; }
			}
			bout.flush();
			byte[] res = bout.toByteArray();
			bout.close();
			resJson = new String(res,"MS949");
			inter = (CashInter)GsonUtil.fromJson(resJson.trim(), CashInter.class);
			
		}catch(Exception e){
			inter.resultCd = "XXXX";
			inter.resultMsg = "현금서비스 시스템과의 통신장애 잠시후 재시도 바람.";
		}finally{
			logger.info("-> CASH : [{}]",reqJson);
			logger.info("<- CASH : [{}],{}",resJson,(System.currentTimeMillis()-time));
			
			try{
				if(input != null){ input.close();}
				if(output != null){ output.close();}
				if(socket != null){ socket.close();}
			}catch(Exception ex){
				
			}
		}
		
		return inter;
		
	}
	

}
