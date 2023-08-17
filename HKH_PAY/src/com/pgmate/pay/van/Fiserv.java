package com.pgmate.pay.van;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;




/**
 * @author Administrator
 *
 */
public class Fiserv implements Van {

	private static Logger logger 					= LoggerFactory.getLogger( com.pgmate.pay.van.Fiserv.class ); 
	private static final String FISERV_URL 			= "https://ps.firstpay.co.kr/jsp/common/pay.jsp";
	private static final int FISERV_CONNECT_TIMEOUT = 5000;
	private static final int FISERV_READ_TIMEOUT	= 17000;
	private static final String PUBMOUDLUS			= "C11F996FFDEC987FD0931B987698C37CB7773B06593C794B2A3B3FE3449596F862EE757D2387F8EEB6FEBC055F55F8F870BA79934A7E9C5E6341B8FE1EF90E5106F9DA5C1FEA2B3B8290668F8AC09E5B11BD104D24338B4FCF2DF9EC1098AA2598CA3C5DE2D4E19DC6337B42D956A46AF5BEA7339EEBAB16B85B93B28984162C4BF8288A0C06663AC0D34FD4E025FF05B858626FBC7A97B42C43D05A3558DC6753F81720CC543BB376AF9BE4E6111F9DF4422622183D0E53D1A44E5B63775BC279DCB5978D1E157DBC78612CB11FA623F7310F98D87D64AEB10A6B22E13829E69258BE3DC4C2D79F235B8A4BF8EBC536E08A4F514B0C3D8C83EF14C6C41C4237";
	private static final String PUBEXPONENT			= "0010001";
	private static PublicKey publicKey				= null;
	
	private String vanId	= "";
	private String cryptoKey= "";	
	private String VAN		= "";
	
	public Fiserv(){
		
	}
	
	public Fiserv(SharedMap<String, Object> vanMap) {
		if(Fiserv.publicKey == null) {
			try {
				Fiserv.publicKey = Fiserv.getPublicKey(Fiserv.PUBMOUDLUS, Fiserv.PUBEXPONENT);
			}catch(Exception e) {
				logger.info("Fiserv publickey error "+e.getMessage());
			}
		}
		
		vanId 		=  vanMap.getString("vanId").trim();
		cryptoKey	= vanMap.getString("cryptoKey").trim();
		VAN = vanMap.getString("van");
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,Response response) {
		
		String item = "";
		try{
			if(response.pay.products != null && response.pay.products.size() > 0){
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
			}
		}catch(Exception e){}
		
		FiservPay pay = new FiservPay();
		pay.MxID		= vanId;
		pay.MxIssueNO	= response.pay.trxId;
		pay.MxIssueDate	= CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		pay.PayMethod	= "CC";
		pay.CcMode		= "10";
		pay.EncodeType	= "U";
		pay.TxCode		= "EC131000";
		pay.Amount		= CommonUtil.toString(response.pay.amount);
		pay.CcNO		= RsaEncData(response.pay.card.number,publicKey);
		
		pay.CcExpDate	= "20"+response.pay.card.expiry;
		if(response.pay.metadata != null){
			if(response.pay.metadata.isTrue("cardAuth")){
				pay.CcVfNO		= response.pay.metadata.getString("authDob");
				pay.CcVfValue	= response.pay.metadata.getString("authPw");
			}
			response.pay.metadata = null;
		}
		pay.Currency	= "KRW";
		pay.Tmode		= "WEB";
		pay.Installment	= CommonUtil.zerofill(response.pay.card.installment, 2);
		pay.CcNameOnCard= CommonUtil.nToB(response.pay.payerName,"username");
		pay.CcProdDesc	= item;
		pay.PhoneNO		= CommonUtil.nToB(response.pay.payerTel,"0215446872");
		pay.Email		= CommonUtil.nToB(response.pay.payerEmail,"pay@aynil.co.kr");
		pay.SpecVer		= "F101C000";
		
		pay.HashData	= Fiserv.Sha256data(pay.MxID+pay.MxIssueNO+pay.MxIssueDate+pay.Amount+pay.TxCode+cryptoKey.trim());
		String req 		= GsonUtil.toJson(pay);

		//logger.info("FISERV_URL : [{}]",FISERV_URL);
		//logger.info("PUBMOUDLUS : [{}]",PUBMOUDLUS);
		//logger.info("PUBEXPONENT : [{}]",PUBEXPONENT);
		logger.info("MxID : [{}]",vanId);
		//logger.info("Hash : [{}]",cryptoKey);
		FiservRes res = connect(req);
		
		response.pay.authCd = res.AuthNO;
		
		if(res.ReplyCode.equals("0000") || res.ReplyCode.equals("2000")){
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
		}else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString( res.ReplyCode),"승인실패",CommonUtil.toString(res.ReplyMessage));
		}
		sharedMap.put("van",VAN);
		sharedMap.put("vanId",vanId);
		sharedMap.put("vanTrxId",res.ReferenceNO);
		sharedMap.put("vanResultCd",CommonUtil.toString(res.ReplyCode));
		sharedMap.put("vanResultMsg",CommonUtil.toString(res.ReplyMessage));
		sharedMap.put("vanDate",CommonUtil.toString(res.MxIssueDate));	
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));
		
		
		
		return sharedMap;
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		
		FiservPay rfd = new FiservPay();
		rfd.MxID		= vanId;
		rfd.MxIssueNO	= payMap.getString("trxId");
		rfd.MxIssueDate	= payMap.getString("regDay")+payMap.getString("regTime");
		rfd.PayMethod	= "CC";
		rfd.CcMode		= "10";
		rfd.EncodeType	= "U";
		rfd.TxCode		= "EC131400";
		rfd.Amount		= CommonUtil.toString(response.refund.amount);
		rfd.SpecVer		= "F101C000";
		
		rfd.HashData	= Fiserv.Sha256data(rfd.MxID+rfd.MxIssueNO+rfd.MxIssueDate+rfd.Amount+rfd.TxCode+cryptoKey.trim());
		String req 		= GsonUtil.toJson(rfd);

		
		FiservRes res = connect(req);
		
		response.refund.authCd = res.AuthNO;
		
		if(res.ReplyCode.equals("0000") || res.ReplyCode.equals("2000")){
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		}else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString( res.ReplyCode),"취소실패",CommonUtil.toString(res.ReplyMessage));
		}
		sharedMap.put("van",payMap.getString("van"));
		sharedMap.put("vanId",vanId);
		sharedMap.put("vanTrxId",res.ReferenceNO);
		sharedMap.put("vanResultCd",CommonUtil.toString(res.ReplyCode));
		sharedMap.put("vanResultMsg",CommonUtil.toString(res.ReplyMessage));
		sharedMap.put("vanDate",CommonUtil.toString(res.MxIssueDate));	
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));
		

		return sharedMap;
	}
	
	
	public FiservRes connect(String req){

		
		logger.info("send : [{}]",req);
		FiservRes res = new FiservRes();
		long time = System.currentTimeMillis();
		String message = "";
		
		
		URL url = null;
		HttpsURLConnection conn = null;
		
		
		
		try {
		
			byte[] eucReq = req.getBytes("euc-kr");
			
			url = new URL(Fiserv.FISERV_URL);
			conn = (HttpsURLConnection)url.openConnection();
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=EUC-KR");
			conn.setRequestProperty("Content-Length", Integer.toString(eucReq.length));
			conn.setConnectTimeout(3000);  
			conn.setReadTimeout(17000); 
			
			OutputStream os = conn.getOutputStream();
			os.write(eucReq);
			os.flush();
			os.close();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			StringBuffer pageBuffer = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				pageBuffer.append(line);
			}
			
			message = pageBuffer.toString();
			logger.debug("recv : [{}]",message);
			message = URLDecoder.decode(message,"UTF-8");
		} catch(Exception e) {
			message = "({\"ReplyCode\":9998\"\",\"ReplyMessage\":\"거래요청 수신 중 오류 발생\"})";
			logger.debug("Fiserv,error : {}",e.getMessage());
		}finally{
			res = (FiservRes)GsonUtil.fromJson(message, FiservRes.class);
			logger.debug("Fiserv Elasped Time =[{} sec]",CommonUtil.parseDouble((System.currentTimeMillis()-time)/1000) );
			logger.debug("Fiserv, res = [{}]",message);
			
		}
		return res;
	}
	
	
	
	 
	public static String RsaEncData(String str, PublicKey pubkey){
			
		String rtnData = ""; 	//결과 DATA
		try{
			byte[] encMsg = encryptByPubKey(pubkey, str.getBytes());
			rtnData = Base64.encodeToString(encMsg);
		}catch(Exception e){
			logger.info("card number encode error "+e.getMessage());
			rtnData = ""; 
		}
		return rtnData;
	}
	
	
	public static byte[] encryptByPubKey(PublicKey key, byte[] plaintext) throws NoSuchAlgorithmException,NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(plaintext);
	}
	 
	public static PublicKey getPublicKey(String modStr, String expStr) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException  {	
			
		BigInteger modulus  = new BigInteger(modStr, 16);
		BigInteger exponent = new BigInteger(expStr, 16);
			
		RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(modulus, exponent);
		
		KeyFactory factory = KeyFactory.getInstance("RSA");
		return factory.generatePublic(publicSpec);
	}
	
	
	public static String Sha256data(String str){
		
		String rtnData = ""; 	//결과 DATA
		
		try{
			MessageDigest md = MessageDigest.getInstance("SHA-256"); 
			md.update(str.getBytes()); 
			byte byteData[] = md.digest();
			StringBuffer sb = new StringBuffer(); 
			for(int i = 0 ; i < byteData.length ; i++){
				sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
			}
			rtnData = sb.toString().toUpperCase();
			
		}catch(NoSuchAlgorithmException e){
            e.printStackTrace();                              
			rtnData = ""; 
		}
	
		return rtnData;
	}
		
	 

}
